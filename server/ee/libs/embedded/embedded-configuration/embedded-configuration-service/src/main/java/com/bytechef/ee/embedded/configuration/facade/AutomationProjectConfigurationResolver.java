/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectConfiguration;
import com.bytechef.ee.embedded.configuration.exception.AutomationProjectConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves which automation project configuration a connected user runs a project under: the one the user is assigned
 * to, or, for a user without an assignment, the self-serve pick.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@SuppressFBWarnings("EI")
public class AutomationProjectConfigurationResolver {

    public static final Comparator<AutomationProjectConfiguration> SELF_SERVE_ORDER = Comparator
        .comparingInt(AutomationProjectConfiguration::getProjectVersion)
        .thenComparing(AutomationProjectConfiguration::getId);

    private final AutomationProjectConfigurationService automationProjectConfigurationService;
    private final ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService;
    private final ConnectedUserProjectService connectedUserProjectService;
    private final EmbeddedPermissionEvaluator embeddedPermissionEvaluator;

    public AutomationProjectConfigurationResolver(
        AutomationProjectConfigurationService automationProjectConfigurationService,
        ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService,
        ConnectedUserProjectService connectedUserProjectService,
        EmbeddedPermissionEvaluator embeddedPermissionEvaluator) {

        this.automationProjectConfigurationService = automationProjectConfigurationService;
        this.connectedUserProjectConfigurationService = connectedUserProjectConfigurationService;
        this.connectedUserProjectService = connectedUserProjectService;
        this.embeddedPermissionEvaluator = embeddedPermissionEvaluator;
    }

    /**
     * Admin path: assigns the connected user to the configuration whatever its permission expression says.
     *
     * @throws ConfigurationException when the configuration belongs to a different project or environment
     */
    @Transactional
    public void assign(ConnectedUser connectedUser, long projectId, long configurationId) {
        AutomationProjectConfiguration configuration =
            automationProjectConfigurationService.getConfiguration(configurationId);

        if (configuration.getProjectId() != projectId ||
            configuration.getEnvironmentId() != connectedUser.getEnvironmentId()) {

            throw new ConfigurationException(
                "Configuration id=%s does not belong to project id=%s in environment id=%s".formatted(
                    configurationId, projectId, connectedUser.getEnvironmentId()),
                AutomationProjectConfigurationErrorType.CONFIGURATION_PROJECT_MISMATCH);
        }

        lockConnectedUser(connectedUser);

        connectedUserProjectConfigurationService.assign(connectedUser.getId(), projectId, configurationId);
    }

    /**
     * Read-only: returns the assigned configuration, whatever its state, or else the self-serve pick.
     */
    @Transactional(readOnly = true)
    public Optional<AutomationProjectConfiguration> resolve(ConnectedUser connectedUser, long projectId) {
        Optional<ConnectedUserProjectConfiguration> assignmentOptional =
            connectedUserProjectConfigurationService.fetchAssignment(connectedUser.getId(), projectId);

        if (assignmentOptional.isPresent()) {
            ConnectedUserProjectConfiguration assignment = assignmentOptional.get();

            return Optional.of(
                automationProjectConfigurationService.getConfiguration(assignment.getConfigurationId()));
        }

        return pickSelfServe(connectedUser, projectId);
    }

    /**
     * Returns the assigned configuration or, for a user without an assignment, assigns the self-serve pick and returns
     * it. Serializes on the connected user's row so concurrent first calls end up with one assignment.
     */
    @Transactional
    public Optional<AutomationProjectConfiguration> resolveAndAssign(
        ConnectedUser connectedUser, long projectId) {

        lockConnectedUser(connectedUser);

        Optional<ConnectedUserProjectConfiguration> assignmentOptional =
            connectedUserProjectConfigurationService.fetchAssignment(connectedUser.getId(), projectId);

        if (assignmentOptional.isPresent()) {
            ConnectedUserProjectConfiguration assignment = assignmentOptional.get();

            return Optional.of(
                automationProjectConfigurationService.getConfiguration(assignment.getConfigurationId()));
        }

        Optional<AutomationProjectConfiguration> pickOptional = pickSelfServe(connectedUser, projectId);

        while (pickOptional.isPresent()) {
            AutomationProjectConfiguration pick = pickOptional.get();

            if (isEnabled(pick)) {
                connectedUserProjectConfigurationService.assign(connectedUser.getId(), projectId, pick.getId());

                return pickOptional;
            }

            pickOptional = pickSelfServe(connectedUser, projectId);
        }

        return pickOptional;
    }

    private boolean isEnabled(AutomationProjectConfiguration configuration) {
        return automationProjectConfigurationService
            .getConfigurations(configuration.getProjectId(), configuration.getEnvironmentId())
            .stream()
            .anyMatch(currentConfiguration -> Objects.equals(currentConfiguration.getId(), configuration.getId()) &&
                currentConfiguration.isEnabled());
    }

    private void lockConnectedUser(ConnectedUser connectedUser) {
        connectedUserProjectService.lockConnectedUser(connectedUser.getExternalId(), connectedUser.getEnvironment());
    }

    private Optional<AutomationProjectConfiguration>
        pickSelfServe(ConnectedUser connectedUser, long projectId) {
        List<AutomationProjectConfiguration> configurations =
            automationProjectConfigurationService.getConfigurations(
                projectId, connectedUser.getEnvironmentId());

        return configurations.stream()
            .filter(AutomationProjectConfiguration::isEnabled)
            .filter(
                configuration -> embeddedPermissionEvaluator.evaluate(
                    configuration.getPermissionExpression(), connectedUser))
            .max(SELF_SERVE_ORDER);
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectConfiguration;
import com.bytechef.ee.embedded.configuration.exception.AutomationWorkflowProjectConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves which automation workflow project configuration a connected user runs a project under: the one the user is
 * assigned to, or, for a user without an assignment, the self-serve pick.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@SuppressFBWarnings("EI")
public class AutomationWorkflowProjectConfigurationResolver {

    public static final Comparator<AutomationWorkflowProjectConfiguration> SELF_SERVE_ORDER = Comparator
        .comparingInt(AutomationWorkflowProjectConfiguration::getProjectVersion)
        .thenComparing(AutomationWorkflowProjectConfiguration::getId);

    private final AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService;
    private final ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService;
    private final ConnectedUserProjectService connectedUserProjectService;
    private final EmbeddedPermissionEvaluator embeddedPermissionEvaluator;

    public AutomationWorkflowProjectConfigurationResolver(
        AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService,
        ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService,
        ConnectedUserProjectService connectedUserProjectService,
        EmbeddedPermissionEvaluator embeddedPermissionEvaluator) {

        this.automationWorkflowProjectConfigurationService = automationWorkflowProjectConfigurationService;
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
        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.getConfiguration(configurationId);

        if (configuration.getProjectId() != projectId ||
            configuration.getEnvironmentId() != connectedUser.getEnvironmentId()) {

            throw new ConfigurationException(
                "Configuration id=%s does not belong to project id=%s in environment id=%s".formatted(
                    configurationId, projectId, connectedUser.getEnvironmentId()),
                AutomationWorkflowProjectConfigurationErrorType.CONFIGURATION_PROJECT_MISMATCH);
        }

        lockConnectedUser(connectedUser);

        connectedUserProjectConfigurationService.assign(connectedUser.getId(), projectId, configurationId);
    }

    /**
     * Read-only: returns the assigned configuration, whatever its state, or else the self-serve pick.
     */
    @Transactional(readOnly = true)
    public Optional<AutomationWorkflowProjectConfiguration> resolve(ConnectedUser connectedUser, long projectId) {
        Optional<ConnectedUserProjectConfiguration> assignmentOptional =
            connectedUserProjectConfigurationService.fetchAssignment(connectedUser.getId(), projectId);

        if (assignmentOptional.isPresent()) {
            ConnectedUserProjectConfiguration assignment = assignmentOptional.get();

            return Optional.of(
                automationWorkflowProjectConfigurationService.getConfiguration(assignment.getConfigurationId()));
        }

        return pickSelfServe(connectedUser, projectId);
    }

    /**
     * Returns the assigned configuration or, for a user without an assignment, assigns the self-serve pick and returns
     * it. Serializes on the connected user's row so concurrent first calls end up with one assignment.
     */
    @Transactional
    public Optional<AutomationWorkflowProjectConfiguration> resolveAndAssign(
        ConnectedUser connectedUser, long projectId) {

        lockConnectedUser(connectedUser);

        Optional<ConnectedUserProjectConfiguration> assignmentOptional =
            connectedUserProjectConfigurationService.fetchAssignment(connectedUser.getId(), projectId);

        if (assignmentOptional.isPresent()) {
            ConnectedUserProjectConfiguration assignment = assignmentOptional.get();

            return Optional.of(
                automationWorkflowProjectConfigurationService.getConfiguration(assignment.getConfigurationId()));
        }

        Optional<AutomationWorkflowProjectConfiguration> pickOptional = pickSelfServe(connectedUser, projectId);

        if (pickOptional.isPresent()) {
            AutomationWorkflowProjectConfiguration pick = pickOptional.get();

            connectedUserProjectConfigurationService.assign(connectedUser.getId(), projectId, pick.getId());
        }

        return pickOptional;
    }

    private void lockConnectedUser(ConnectedUser connectedUser) {
        connectedUserProjectService.lockConnectedUser(connectedUser.getExternalId(), connectedUser.getEnvironment());
    }

    private Optional<AutomationWorkflowProjectConfiguration>
        pickSelfServe(ConnectedUser connectedUser, long projectId) {
        List<AutomationWorkflowProjectConfiguration> configurations =
            automationWorkflowProjectConfigurationService.getConfigurations(
                projectId, connectedUser.getEnvironmentId());

        return configurations.stream()
            .filter(AutomationWorkflowProjectConfiguration::isEnabled)
            .filter(
                configuration -> embeddedPermissionEvaluator.evaluate(
                    configuration.getPermissionExpression(), connectedUser))
            .max(SELF_SERVE_ORDER);
    }
}

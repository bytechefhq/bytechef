/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.security;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class EmbeddedPrincipalEnvironmentResolver {

    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationWorkflowService integrationWorkflowService;

    @SuppressFBWarnings("EI")
    public EmbeddedPrincipalEnvironmentResolver(
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceService integrationInstanceService, IntegrationWorkflowService integrationWorkflowService) {

        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationWorkflowService = integrationWorkflowService;
    }

    public Optional<Environment> fetchEnvironment(long principalId, Predicate<IntegrationWorkflow> workflowMatcher) {
        return fetchEnvironment(principalId, workflowMatcher, true);
    }

    public Optional<Environment> fetchInstanceEnvironment(
        long principalId, Predicate<IntegrationWorkflow> workflowMatcher) {

        return fetchEnvironment(principalId, workflowMatcher, false);
    }

    private Optional<Environment> fetchEnvironment(
        long principalId, Predicate<IntegrationWorkflow> workflowMatcher, boolean includeConfiguration) {

        Optional<Environment> instanceEnvironment = fetchInstanceCandidate(principalId, workflowMatcher);

        if (!includeConfiguration) {
            return instanceEnvironment;
        }

        Optional<Environment> configurationEnvironment = fetchConfigurationCandidate(principalId, workflowMatcher);

        if (instanceEnvironment.isPresent() && configurationEnvironment.isPresent()) {
            return instanceEnvironment.equals(configurationEnvironment) ? instanceEnvironment : Optional.empty();
        }

        return instanceEnvironment.isPresent() ? instanceEnvironment : configurationEnvironment;
    }

    private Optional<Environment> fetchInstanceCandidate(
        long principalId, Predicate<IntegrationWorkflow> workflowMatcher) {

        return integrationInstanceService.getIntegrationInstances(List.of(principalId))
            .stream()
            .findFirst()
            .map(IntegrationInstance::getIntegrationInstanceConfigurationId)
            .flatMap(configurationId -> fetchConfigurationCandidate(configurationId, workflowMatcher));
    }

    private Optional<Environment> fetchConfigurationCandidate(
        long configurationId, Predicate<IntegrationWorkflow> workflowMatcher) {

        return integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(List.of(configurationId))
            .stream()
            .findFirst()
            .filter(configuration -> belongsToIntegration(configuration, workflowMatcher))
            .map(IntegrationInstanceConfiguration::getEnvironment);
    }

    private boolean belongsToIntegration(
        IntegrationInstanceConfiguration configuration, Predicate<IntegrationWorkflow> workflowMatcher) {

        Long integrationId = configuration.getIntegrationId();

        if (Objects.isNull(integrationId)) {
            return false;
        }

        List<IntegrationWorkflow> integrationWorkflows = integrationWorkflowService.getIntegrationWorkflows(
            integrationId);

        return integrationWorkflows.stream()
            .anyMatch(workflowMatcher);
    }
}

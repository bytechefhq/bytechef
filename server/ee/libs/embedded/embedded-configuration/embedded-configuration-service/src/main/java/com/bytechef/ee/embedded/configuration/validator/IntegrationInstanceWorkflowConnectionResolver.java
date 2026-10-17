/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.validator;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.workflow.validator.WorkflowDeploymentConnectionResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
class IntegrationInstanceWorkflowConnectionResolver implements WorkflowDeploymentConnectionResolver {

    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationService integrationService;

    IntegrationInstanceWorkflowConnectionResolver(
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceService integrationInstanceService, IntegrationService integrationService) {

        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationService = integrationService;
    }

    @Override
    public List<WorkflowNodeConnection> getWorkflowNodeConnections(
        String workflowId, String workflowNodeName, long environmentId) {

        Environment[] environments = Environment.values();

        if (environmentId < 0 || environmentId >= environments.length) {
            return List.of();
        }

        List<WorkflowNodeConnection> workflowNodeConnections = new ArrayList<>();

        for (IntegrationInstanceConfiguration integrationInstanceConfiguration : integrationInstanceConfigurationService
            .getIntegrationInstanceConfigurations(environments[(int) environmentId], true)) {

            long integrationInstanceConfigurationId = Objects.requireNonNull(integrationInstanceConfiguration.getId());

            boolean workflowEnabled = false;

            for (IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow : integrationInstanceConfigurationWorkflowService
                .getIntegrationInstanceConfigurationWorkflows(integrationInstanceConfigurationId)) {

                if (!Objects.equals(integrationInstanceConfigurationWorkflow.getWorkflowId(), workflowId) ||
                    !integrationInstanceConfigurationWorkflow.isEnabled()) {

                    continue;
                }

                workflowEnabled = true;

                for (IntegrationInstanceConfigurationWorkflowConnection integrationInstanceConfigurationWorkflowConnection : integrationInstanceConfigurationWorkflow
                    .getConnections()) {

                    if (Objects.equals(
                        integrationInstanceConfigurationWorkflowConnection.getWorkflowNodeName(), workflowNodeName)) {

                        workflowNodeConnections.add(
                            new WorkflowNodeConnection(
                                integrationInstanceConfigurationWorkflowConnection.getConnectionId(),
                                integrationInstanceConfigurationWorkflowConnection.getWorkflowConnectionKey()));
                    }
                }
            }

            if (!workflowEnabled) {
                continue;
            }

            Integration integration = integrationService.getIntegration(
                Objects.requireNonNull(integrationInstanceConfiguration.getIntegrationId()));

            for (IntegrationInstance integrationInstance : integrationInstanceService.getIntegrationInstances(
                integrationInstanceConfigurationId)) {

                if (integrationInstance.isEnabled()) {
                    workflowNodeConnections.add(
                        new WorkflowNodeConnection(
                            integrationInstance.getConnectionId(), integration.getComponentName()));
                }
            }
        }

        return workflowNodeConnections;
    }
}

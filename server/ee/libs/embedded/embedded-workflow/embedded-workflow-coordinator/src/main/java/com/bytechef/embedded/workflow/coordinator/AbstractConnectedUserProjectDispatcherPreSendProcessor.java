/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.embedded.workflow.coordinator;

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public abstract class AbstractConnectedUserProjectDispatcherPreSendProcessor {

    protected final ConnectedUserProjectService connectedUserProjectService;
    protected final EnvironmentService environmentService;
    protected final IntegrationInstanceService integrationInstanceService;
    protected final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    protected AbstractConnectedUserProjectDispatcherPreSendProcessor(
        ConnectedUserProjectService connectedUserProjectService, EnvironmentService environmentService,
        IntegrationInstanceService integrationInstanceService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService) {

        this.connectedUserProjectService = connectedUserProjectService;
        this.environmentService = environmentService;
        this.integrationInstanceService = integrationInstanceService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
    }

    protected Map<String, Long> getConnectionIdMap(
        Long projectDeploymentId, String workflowId, String workflowNodeName) {

        List<ProjectDeploymentWorkflowConnection> projectDeploymentWorkflowConnections =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflowConnections(
                projectDeploymentId, workflowId, workflowNodeName);

        return MapUtils.toMap(
            projectDeploymentWorkflowConnections, ProjectDeploymentWorkflowConnection::getWorkflowConnectionKey,
            ProjectDeploymentWorkflowConnection::getConnectionId);
    }

    protected Map<String, Long> getConnectionIdMap(
        Long projectDeploymentId, String workflowId, String workflowNodeName, String componentName,
        long environmentId) {

        Map<String, Long> connectionIdMap = getConnectionIdMap(projectDeploymentId, workflowId, workflowNodeName);

        if (connectionIdMap.isEmpty()) {
            connectionIdMap = getIntegrationInstanceConnectionIdMap(
                projectDeploymentId, workflowNodeName, componentName, environmentId);
        }

        return connectionIdMap;
    }

    private Map<String, Long> getIntegrationInstanceConnectionIdMap(
        Long projectDeploymentId, String workflowNodeName, String componentName, long environmentId) {

        Optional<Long> connectedUserId = connectedUserProjectService.fetchConnectedUserId(projectDeploymentId);

        if (connectedUserId.isEmpty()) {
            return Map.of();
        }

        Environment environment = environmentService.getEnvironment(environmentId);

        Optional<IntegrationInstance> integrationInstance = integrationInstanceService.fetchIntegrationInstance(
            connectedUserId.get(), componentName, environment);

        return integrationInstance
            .map(curIntegrationInstance -> Map.of(workflowNodeName, curIntegrationInstance.getConnectionId()))
            .orElseGet(Map::of);
    }
}

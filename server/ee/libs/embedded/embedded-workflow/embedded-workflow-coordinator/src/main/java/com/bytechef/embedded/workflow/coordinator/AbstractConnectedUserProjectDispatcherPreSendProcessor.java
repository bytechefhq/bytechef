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
import java.util.List;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public abstract class AbstractConnectedUserProjectDispatcherPreSendProcessor {

    protected final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    protected AbstractConnectedUserProjectDispatcherPreSendProcessor(
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService) {

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
}

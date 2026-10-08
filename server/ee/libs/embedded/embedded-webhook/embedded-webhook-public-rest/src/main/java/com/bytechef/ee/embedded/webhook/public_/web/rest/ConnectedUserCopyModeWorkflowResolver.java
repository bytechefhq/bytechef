/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.webhook.public_.web.rest;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserCopyModeWorkflowResolver {
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectWorkflowService projectWorkflowService;

    ConnectedUserCopyModeWorkflowResolver(
        ProjectDeploymentService projectDeploymentService, ProjectWorkflowService projectWorkflowService) {
        this.projectDeploymentService = projectDeploymentService;
        this.projectWorkflowService = projectWorkflowService;
    }

    Optional<Resolved> resolve(ConnectedUserProjectWorkflow copyModeWorkflow, Environment environment) {
        ProjectWorkflow projectWorkflow = projectWorkflowService.getProjectWorkflow(
            copyModeWorkflow.getProjectWorkflowId());

        long projectDeploymentId = projectDeploymentService.getProjectDeploymentId(
            projectWorkflow.getProjectId(), environment);

        String workflowUuid = projectWorkflow.getUuidAsString();

        return projectWorkflowService.fetchProjectWorkflowWorkflowId(projectDeploymentId, workflowUuid)
            .map(workflowId -> new Resolved(projectDeploymentId, workflowUuid, workflowId));
    }

    record Resolved(long projectDeploymentId, String workflowUuid, String workflowId) {
    }
}

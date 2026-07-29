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
 * Resolves a connected user's copy-mode {@link ConnectedUserProjectWorkflow} row -- the user's own private copy of a
 * visual catalog template, as opposed to a shared reference to a code catalog workflow -- to the workflow id it must be
 * dispatched through, plus the {@code ProjectDeployment} id and the copy's own workflow uuid.
 *
 * <p>
 * Mirrors {@code ConnectedUserProjectFacadeImpl.enableProjectWorkflow(long, boolean)}'s copy-mode resolution: a copy's
 * {@code projectWorkflowId} points at a {@link ProjectWorkflow} owned by the connected user's own project (one project
 * per connected user per environment, provisioned by {@code ConnectedUserProjectWorkflowManager}), so that project's
 * currently active deployment -- not the row's (always-null, for copy mode) {@code projectDeploymentId} -- is what must
 * be resolved and used as the job principal.
 *
 * <p>
 * Shared by {@link RequestTriggerApiController} and {@link AppEventTriggerApiController} so the two controllers resolve
 * copy-mode rows the exact same way instead of forking the resolution subtly.
 *
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

    /**
     * Returns empty when the copy's own workflow has not (yet) been attached to its project's active deployment.
     */
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

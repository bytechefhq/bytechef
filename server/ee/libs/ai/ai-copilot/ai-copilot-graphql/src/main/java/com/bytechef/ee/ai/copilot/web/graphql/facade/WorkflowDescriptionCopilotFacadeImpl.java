/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.graphql.facade;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotGenerator;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotRequest;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotResult;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link WorkflowDescriptionCopilotFacade}. Carries the workflow-scope guard that used to sit in
 * {@code WorkflowDescriptionCopilotGraphQlController.generateWorkflowDescription}'s body, so it is enforced for every
 * caller of the facade rather than only the GraphQL entry point.
 *
 * <p>
 * The guard is written out rather than expressed as {@code @PreAuthorize} for the same reason as on
 * {@link PropertyCopilotFacadeImpl}: it resolves the owning project from the client-supplied workflow id first, and its
 * denial message names the workflow.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnCoordinator
@SuppressFBWarnings("EI")
class WorkflowDescriptionCopilotFacadeImpl implements WorkflowDescriptionCopilotFacade {

    private static final String WORKFLOW_VIEW_SCOPE = "WORKFLOW_VIEW";

    private final PermissionService permissionService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowDescriptionCopilotGenerator workflowDescriptionCopilotGenerator;

    WorkflowDescriptionCopilotFacadeImpl(
        PermissionService permissionService, ProjectWorkflowService projectWorkflowService,
        Optional<WorkflowDescriptionCopilotGenerator> workflowDescriptionCopilotGenerator) {

        this.permissionService = permissionService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowDescriptionCopilotGenerator = workflowDescriptionCopilotGenerator.orElse(null);
    }

    @Override
    public WorkflowDescriptionCopilotResult generateWorkflowDescription(WorkflowDescriptionCopilotRequest request) {
        if (workflowDescriptionCopilotGenerator == null) {
            throw new IllegalStateException("Workflow Description Copilot is not enabled");
        }

        // Authorize: the workflowId is client-supplied, so verify the current user may view the owning
        // project before reading its definition (IDOR / cross-tenant guard).
        long projectId = projectWorkflowService.getWorkflowProjectWorkflow(request.workflowId())
            .getProjectId();

        if (!permissionService.hasWorkspaceScopeForProject(projectId, WORKFLOW_VIEW_SCOPE)) {
            throw new AccessDeniedException("Access denied to workflow " + request.workflowId());
        }

        return workflowDescriptionCopilotGenerator.generate(request);
    }
}

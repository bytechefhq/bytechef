/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotGenerator;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotRequest;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotResult;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for the Workflow Description Copilot feature.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
@SuppressFBWarnings("EI")
public class WorkflowDescriptionCopilotGraphQlController {

    private static final String WORKFLOW_VIEW_SCOPE = "WORKFLOW_VIEW";

    private final PermissionService permissionService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowDescriptionCopilotGenerator workflowDescriptionCopilotGenerator;

    public WorkflowDescriptionCopilotGraphQlController(
        PermissionService permissionService, ProjectWorkflowService projectWorkflowService,
        Optional<WorkflowDescriptionCopilotGenerator> workflowDescriptionCopilotGenerator) {

        this.permissionService = permissionService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowDescriptionCopilotGenerator = workflowDescriptionCopilotGenerator.orElse(null);
    }

    @MutationMapping
    public GenerateWorkflowDescriptionPayload generateWorkflowDescription(
        @Argument GenerateWorkflowDescriptionInput input) {

        if (workflowDescriptionCopilotGenerator == null) {
            throw new IllegalStateException("Workflow Description Copilot is not enabled");
        }

        // Authorize: the workflowId is client-supplied, so verify the current user may view the owning
        // project before reading its definition (IDOR / cross-tenant guard).
        long projectId = projectWorkflowService.getWorkflowProjectWorkflow(input.workflowId())
            .getProjectId();

        if (!permissionService.hasWorkspaceScopeForProject(projectId, WORKFLOW_VIEW_SCOPE)) {
            throw new AccessDeniedException("Access denied to workflow " + input.workflowId());
        }

        WorkflowDescriptionCopilotResult result = workflowDescriptionCopilotGenerator.generate(
            new WorkflowDescriptionCopilotRequest(
                input.workflowId(), input.workflowNodeName(), input.environmentId()));

        return new GenerateWorkflowDescriptionPayload(result.value());
    }

    @SuppressFBWarnings("EI")
    public record GenerateWorkflowDescriptionInput(String workflowId, String workflowNodeName, long environmentId) {
    }

    @SuppressFBWarnings("EI")
    public record GenerateWorkflowDescriptionPayload(String value) {
    }
}

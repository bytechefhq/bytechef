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
import com.bytechef.ee.ai.copilot.property.PropertyCopilotGenerator;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotMode;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotRequest;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotResult;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for the Property Copilot feature.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
@SuppressFBWarnings("EI")
public class PropertyCopilotGraphQlController {

    private static final String WORKFLOW_VIEW_SCOPE = "WORKFLOW_VIEW";

    private final PermissionService permissionService;
    private final ProjectWorkflowService projectWorkflowService;
    private final PropertyCopilotGenerator propertyCopilotGenerator;

    public PropertyCopilotGraphQlController(
        PermissionService permissionService, ProjectWorkflowService projectWorkflowService,
        Optional<PropertyCopilotGenerator> propertyCopilotGenerator) {

        this.permissionService = permissionService;
        this.projectWorkflowService = projectWorkflowService;
        this.propertyCopilotGenerator = propertyCopilotGenerator.orElse(null);
    }

    @MutationMapping
    public GeneratePropertyValuePayload generatePropertyValue(@Argument GeneratePropertyValueInput input) {
        if (propertyCopilotGenerator == null) {
            throw new IllegalStateException("Property Copilot is not enabled");
        }

        // Authorize: the workflowId is client-supplied, so verify the current user may view the owning
        // project before reading its previous-step outputs/sample data (IDOR / cross-tenant guard).
        long projectId = projectWorkflowService.getWorkflowProjectWorkflow(input.workflowId())
            .getProjectId();

        if (!permissionService.hasWorkspaceScopeForProject(projectId, WORKFLOW_VIEW_SCOPE)) {
            throw new AccessDeniedException("Access denied to workflow " + input.workflowId());
        }

        PropertyCopilotResult result = propertyCopilotGenerator.generate(new PropertyCopilotRequest(
            input.prompt(), input.mode(), input.workflowId(), input.workflowNodeName(), input.propertyPath(),
            input.propertyType(), input.dynamic(), input.environmentId()));

        return new GeneratePropertyValuePayload(result.value(), result.valid(), result.message());
    }

    @SuppressFBWarnings("EI")
    public record GeneratePropertyValueInput(
        String prompt, PropertyCopilotMode mode, String workflowId, String workflowNodeName, String propertyPath,
        String propertyType, boolean dynamic, long environmentId) {
    }

    @SuppressFBWarnings("EI")
    public record GeneratePropertyValuePayload(String value, boolean valid, String message) {
    }
}

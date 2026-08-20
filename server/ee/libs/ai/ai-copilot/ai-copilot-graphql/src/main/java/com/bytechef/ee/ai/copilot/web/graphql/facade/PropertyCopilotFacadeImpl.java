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
import com.bytechef.ee.ai.copilot.property.PropertyCopilotGenerator;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotRequest;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotResult;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link PropertyCopilotFacade}. Carries the workflow-scope guard that used to sit in
 * {@code PropertyCopilotGraphQlController.generatePropertyValue}'s body, so it is enforced for every caller of the
 * facade rather than only the GraphQL entry point.
 *
 * <p>
 * The guard is written out rather than expressed as {@code @PreAuthorize}: it resolves the owning project from the
 * client-supplied workflow id first, and it denies with a workflow-naming {@link AccessDeniedException} whose message
 * the GraphQL error shape already depends on. A {@code hasPermission} annotation would replace that message with
 * Spring's generic "Access Denied", which is a wire-visible change rather than a relocation.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnCoordinator
@SuppressFBWarnings("EI")
class PropertyCopilotFacadeImpl implements PropertyCopilotFacade {

    private static final String WORKFLOW_VIEW_SCOPE = "WORKFLOW_VIEW";

    private final PermissionService permissionService;
    private final ProjectWorkflowService projectWorkflowService;
    private final PropertyCopilotGenerator propertyCopilotGenerator;

    PropertyCopilotFacadeImpl(
        PermissionService permissionService, ProjectWorkflowService projectWorkflowService,
        Optional<PropertyCopilotGenerator> propertyCopilotGenerator) {

        this.permissionService = permissionService;
        this.projectWorkflowService = projectWorkflowService;
        this.propertyCopilotGenerator = propertyCopilotGenerator.orElse(null);
    }

    @Override
    public PropertyCopilotResult generatePropertyValue(PropertyCopilotRequest request) {
        if (propertyCopilotGenerator == null) {
            throw new IllegalStateException("Property Copilot is not enabled");
        }

        // Authorize: the workflowId is client-supplied, so verify the current user may view the owning
        // project before reading its previous-step outputs/sample data (IDOR / cross-tenant guard).
        long projectId = projectWorkflowService.getWorkflowProjectWorkflow(request.workflowId())
            .getProjectId();

        if (!permissionService.hasWorkspaceScopeForProject(projectId, WORKFLOW_VIEW_SCOPE)) {
            throw new AccessDeniedException("Access denied to workflow " + request.workflowId());
        }

        return propertyCopilotGenerator.generate(request);
    }
}

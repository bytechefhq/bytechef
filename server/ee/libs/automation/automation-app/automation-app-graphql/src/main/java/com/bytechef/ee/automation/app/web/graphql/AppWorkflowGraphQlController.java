/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.app.dto.AppBindableWorkflowDTO;
import com.bytechef.ee.automation.app.dto.AppWorkflowBindingDTO;
import com.bytechef.ee.automation.app.facade.AppWorkflowFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller exposing the workflow bindings of an {@code App} for the admin UI.
 *
 * <p>
 * {@link AppWorkflowFacade} owns all authorization and lookups, so this controller carries no {@code @PreAuthorize}: it
 * only maps arguments and responses.
 *
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
class AppWorkflowGraphQlController {

    private final AppWorkflowFacade appWorkflowFacade;

    @SuppressFBWarnings("EI")
    AppWorkflowGraphQlController(AppWorkflowFacade appWorkflowFacade) {
        this.appWorkflowFacade = appWorkflowFacade;
    }

    @QueryMapping
    public List<AppWorkflowBindingResponse> appWorkflows(@Argument long appId) {
        return appWorkflowFacade.getWorkflowBindings(appId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @QueryMapping
    public List<AppBindableWorkflowResponse> appBindableWorkflows(@Argument long appId) {
        return appWorkflowFacade.getBindableWorkflows(appId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @MutationMapping
    public AppWorkflowBindingResponse bindAppWorkflow(@Argument long appId, @Argument AppWorkflowBindingInput input) {
        return toResponse(
            appWorkflowFacade.bindWorkflow(appId, input.workflowKey(), input.projectDeploymentId(),
                input.workflowUuid()));
    }

    @MutationMapping
    public boolean unbindAppWorkflow(@Argument long appId, @Argument String workflowKey) {
        appWorkflowFacade.unbindWorkflow(appId, workflowKey);

        return true;
    }

    private AppWorkflowBindingResponse toResponse(AppWorkflowBindingDTO binding) {
        return new AppWorkflowBindingResponse(
            binding.id(), binding.appId(), binding.workflowKey(), binding.projectDeploymentId(),
            binding.projectDeploymentName(), binding.workflowUuid(), binding.workflowName(), binding.available());
    }

    private AppBindableWorkflowResponse toResponse(AppBindableWorkflowDTO workflow) {
        return new AppBindableWorkflowResponse(
            workflow.projectDeploymentId(), workflow.projectDeploymentName(), workflow.projectName(),
            workflow.workflowUuid(), workflow.workflowName(), workflow.description());
    }

    public record AppWorkflowBindingInput(String workflowKey, long projectDeploymentId, String workflowUuid) {
    }

    public record AppWorkflowBindingResponse(
        long id, long appId, String workflowKey, long projectDeploymentId, @Nullable String projectDeploymentName,
        String workflowUuid, @Nullable String workflowName, boolean available) {
    }

    public record AppBindableWorkflowResponse(
        long projectDeploymentId, String projectDeploymentName, String projectName, String workflowUuid,
        String workflowName, @Nullable String description) {
    }
}

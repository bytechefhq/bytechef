/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.app.dto.AppBindableWorkflowDTO;
import com.bytechef.ee.automation.app.dto.AppWorkflowBindingDTO;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.facade.AppWorkflowFacade;
import com.bytechef.ee.automation.app.public_.web.rest.management.AppWorkflowBindingApi;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppBindableWorkflowModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppWorkflowBindingInputModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppWorkflowBindingModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.EnvironmentModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.service.EnvironmentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The App workflow binding management endpoints of the automation public API.
 *
 * <p>
 * {@link AppWorkflowFacade} owns all authorization and lookups, so this controller carries no {@code @PreAuthorize}: it
 * maps models to and from DTOs. Every route first requires the app to be in the request's environment through
 * {@link AppFacade}, before any permission check, and answers an app of another environment as not found.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.app.public_.web.rest.AppWorkflowBindingApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AppWorkflowBindingApiController extends AbstractAppApiController implements AppWorkflowBindingApi {

    private final AppFacade appFacade;
    private final AppWorkflowFacade appWorkflowFacade;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI2")
    public AppWorkflowBindingApiController(
        AppFacade appFacade, AppWorkflowFacade appWorkflowFacade, EnvironmentService environmentService) {

        this.appFacade = appFacade;
        this.appWorkflowFacade = appWorkflowFacade;
        this.environmentService = environmentService;
    }

    @Override
    public ResponseEntity<List<AppWorkflowBindingModel>> getAppWorkflowBindings(
        Long id, @Nullable EnvironmentModel xEnvironment) {

        checkAppEnvironment(id, xEnvironment);

        return ResponseEntity.ok(
            appWorkflowFacade.getWorkflowBindings(id)
                .stream()
                .map(this::toModel)
                .toList());
    }

    @Override
    public ResponseEntity<List<AppBindableWorkflowModel>> getAppBindableWorkflows(
        Long id, @Nullable EnvironmentModel xEnvironment) {

        checkAppEnvironment(id, xEnvironment);

        return ResponseEntity.ok(
            appWorkflowFacade.getBindableWorkflows(id)
                .stream()
                .map(this::toModel)
                .toList());
    }

    @Override
    public ResponseEntity<AppWorkflowBindingModel> bindAppWorkflow(
        Long id, String workflowKey, AppWorkflowBindingInputModel appWorkflowBindingInputModel,
        @Nullable EnvironmentModel xEnvironment) {

        checkAppEnvironment(id, xEnvironment);

        return ResponseEntity.ok(
            toModel(
                appWorkflowFacade.bindWorkflow(
                    id, workflowKey, appWorkflowBindingInputModel.getProjectDeploymentId(),
                    appWorkflowBindingInputModel.getWorkflowUuid())));
    }

    @Override
    public ResponseEntity<Void> unbindAppWorkflow(
        Long id, String workflowKey, @Nullable EnvironmentModel xEnvironment) {

        checkAppEnvironment(id, xEnvironment);

        appWorkflowFacade.unbindWorkflow(id, workflowKey);

        return ResponseEntity.noContent()
            .build();
    }

    private void checkAppEnvironment(long id, @Nullable EnvironmentModel xEnvironment) {
        AppEnvironmentScope.requireInEnvironment(appFacade, environmentService, id, xEnvironment);
    }

    private AppBindableWorkflowModel toModel(AppBindableWorkflowDTO appBindableWorkflowDTO) {
        return new AppBindableWorkflowModel()
            .projectDeploymentId(appBindableWorkflowDTO.projectDeploymentId())
            .projectDeploymentName(appBindableWorkflowDTO.projectDeploymentName())
            .projectName(appBindableWorkflowDTO.projectName())
            .workflowUuid(appBindableWorkflowDTO.workflowUuid())
            .workflowName(appBindableWorkflowDTO.workflowName())
            .description(appBindableWorkflowDTO.description());
    }

    private AppWorkflowBindingModel toModel(AppWorkflowBindingDTO binding) {
        return new AppWorkflowBindingModel()
            .id(binding.id())
            .appId(binding.appId())
            .workflowKey(binding.workflowKey())
            .projectDeploymentId(binding.projectDeploymentId())
            .projectDeploymentName(binding.projectDeploymentName())
            .workflowUuid(binding.workflowUuid())
            .workflowName(binding.workflowName())
            .available(binding.available());
    }
}

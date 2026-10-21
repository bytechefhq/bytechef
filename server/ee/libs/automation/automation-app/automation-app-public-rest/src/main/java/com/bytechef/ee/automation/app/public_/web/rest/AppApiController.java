/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.dto.AppComponentOperationsDTO;
import com.bytechef.ee.automation.app.dto.AppDTO;
import com.bytechef.ee.automation.app.dto.ForeignAppInstanceDTO;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.public_.web.rest.management.AppApi;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppComponentOperationsModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppInputModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppInstanceSecretKeyModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppInstanceStatusModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppStatusModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppStatusUpdateModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppUpdateModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppVisibilityModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.EnvironmentModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.ForeignAppInstanceModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The App management endpoints of the automation public API.
 *
 * <p>
 * {@link AppFacade} owns all authorization and lookups, so this controller carries no {@code @PreAuthorize}: it maps
 * models to and from DTOs, one facade call per operation. A field absent from an update means "keep the stored value"
 * (the facade owns that rule). An app spans environments, so its routes are not scoped to {@code X-Environment}; an app
 * lists only its instances of the request's environment. Every mutation is refused outside DEVELOPMENT before the
 * facade is called.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.app.public_.web.rest.AppApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AppApiController extends AbstractAppApiController implements AppApi {

    private final AppFacade appFacade;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI2")
    public AppApiController(AppFacade appFacade, EnvironmentService environmentService) {
        this.appFacade = appFacade;
        this.environmentService = environmentService;
    }

    @Override
    public ResponseEntity<List<AppModel>> getApps(Long workspaceId, @Nullable EnvironmentModel xEnvironment) {
        Environment environment = AppEnvironmentScope.getEnvironment(environmentService, xEnvironment);

        return ResponseEntity.ok(
            appFacade.getApps(workspaceId)
                .stream()
                .map(app -> toModel(app, environment))
                .toList());
    }

    @Override
    public ResponseEntity<AppInstanceSecretKeyModel> createApp(
        Long workspaceId, AppInputModel appInputModel, @Nullable EnvironmentModel xEnvironment) {

        Environment environment = AppEnvironmentScope.requireDevelopment(environmentService, xEnvironment);

        ResourceVisibility visibility = AppManagementModels.toVisibility(appInputModel.getVisibility());

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(
                AppManagementModels.toModel(
                    appFacade.createApp(
                        workspaceId, environment, appInputModel.getName(), appInputModel.getDescription(),
                        AppManagementModels.toHostingTarget(appInputModel.getHostingTarget()),
                        visibility == null ? ResourceVisibility.WORKSPACE : visibility,
                        AppManagementModels.toUserAuthMode(appInputModel.getUserAuthMode()),
                        AppManagementModels.toCategory(appInputModel.getCategory()),
                        Boolean.TRUE.equals(appInputModel.getPortalDiscoverable()))));
    }

    @Override
    public ResponseEntity<AppModel> getApp(Long id, @Nullable EnvironmentModel xEnvironment) {
        Environment environment = AppEnvironmentScope.getEnvironment(environmentService, xEnvironment);

        return ResponseEntity.ok(toModel(appFacade.getApp(id), environment));
    }

    @Override
    public ResponseEntity<AppModel> updateApp(
        Long id, AppUpdateModel appUpdateModel, @Nullable EnvironmentModel xEnvironment) {

        Environment environment = AppEnvironmentScope.requireDevelopment(environmentService, xEnvironment);

        AppDTO app = appFacade.updateApp(
            id, appUpdateModel.getName(), appUpdateModel.getDescription(),
            AppManagementModels.toCategory(appUpdateModel.getCategory()), appUpdateModel.getPortalDiscoverable());

        return ResponseEntity.ok(toModel(app, environment));
    }

    @Override
    public ResponseEntity<AppModel> updateAppStatus(
        Long id, AppStatusUpdateModel appStatusUpdateModel, @Nullable EnvironmentModel xEnvironment) {

        Environment environment = AppEnvironmentScope.requireDevelopment(environmentService, xEnvironment);

        AppStatusModel status = appStatusUpdateModel.getStatus();

        AppDTO app = appFacade.updateAppStatus(id, AppStatus.valueOf(status.name()));

        return ResponseEntity.ok(toModel(app, environment));
    }

    @Override
    public ResponseEntity<Void> deleteApp(Long id, @Nullable EnvironmentModel xEnvironment) {
        AppEnvironmentScope.requireDevelopment(environmentService, xEnvironment);

        appFacade.deleteApp(id);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<AppComponentOperationsModel> getAppComponentOperations(
        String componentName, Integer componentVersion) {

        AppComponentOperationsDTO appComponentOperationsDTO = appFacade.getComponentOperations(
            componentName, componentVersion);

        return ResponseEntity.ok(
            new AppComponentOperationsModel()
                .actionNames(appComponentOperationsDTO.actionNames())
                .toolNames(appComponentOperationsDTO.toolNames()));
    }

    private static AppModel toModel(AppDTO app, Environment environment) {
        ResourceVisibility visibility = app.visibility();

        return new AppModel()
            .id(app.id())
            .uuid(app.uuid())
            .name(app.name())
            .description(app.description())
            .workspaceId(app.workspaceId())
            .ownerUserId(app.ownerUserId())
            .categoryId(app.categoryId())
            .hasIcon(app.hasIcon())
            .status(AppStatusModel.valueOf(app.status()
                .name()))
            .visibility(AppVisibilityModel.valueOf(visibility.name()))
            .portalDiscoverable(app.portalDiscoverable())
            .instances(
                app.instances()
                    .stream()
                    .filter(appInstance -> appInstance.environment() == environment)
                    .map(AppManagementModels::toModel)
                    .toList())
            .foreignInstances(
                app.foreignInstances()
                    .stream()
                    .filter(foreignAppInstance -> foreignAppInstance.environment() == environment)
                    .map(AppApiController::toModel)
                    .toList())
            .createdBy(app.createdBy())
            .createdDate(AppManagementModels.toOffsetDateTime(app.createdDate()))
            .lastModifiedBy(app.lastModifiedBy())
            .lastModifiedDate(AppManagementModels.toOffsetDateTime(app.lastModifiedDate()))
            .version(app.version());
    }

    private static ForeignAppInstanceModel toModel(ForeignAppInstanceDTO foreignAppInstance) {
        Environment environment = foreignAppInstance.environment();

        return new ForeignAppInstanceModel()
            .id(foreignAppInstance.id())
            .name(foreignAppInstance.name())
            .workspaceId(foreignAppInstance.workspaceId())
            .environment(EnvironmentModel.valueOf(environment.name()))
            .status(AppInstanceStatusModel.valueOf(foreignAppInstance.status()
                .name()))
            .currentAppDeploymentId(foreignAppInstance.currentAppDeploymentId());
    }
}

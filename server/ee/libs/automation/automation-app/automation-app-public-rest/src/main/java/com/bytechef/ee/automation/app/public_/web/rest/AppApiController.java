/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.dto.AppBindableConnectionDTO;
import com.bytechef.ee.automation.app.dto.AppBindingDTO;
import com.bytechef.ee.automation.app.dto.AppComponentOperationsDTO;
import com.bytechef.ee.automation.app.dto.AppDTO;
import com.bytechef.ee.automation.app.dto.AppSecretKeyDTO;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.public_.web.rest.management.AppApi;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppBindableConnectionModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppBindingInputModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppBindingModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppComponentOperationsModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppHostingTargetModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppInputModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppSecretKeyModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppStatusModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppUpdateModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppVisibilityModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.EnvironmentModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
 * models to and from DTOs. A field absent from an update means "keep the stored value" (the facade owns that rule).
 * Every by-id route first requires the app to be in the request's environment, before any permission check, and answers
 * an app of another environment as not found.
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
        Environment environment = toEnvironment(xEnvironment);

        return ResponseEntity.ok(
            appFacade.getApps(workspaceId, environment.ordinal())
                .stream()
                .map(this::toModel)
                .toList());
    }

    @Override
    public ResponseEntity<AppSecretKeyModel> createApp(
        Long workspaceId, AppInputModel appInputModel, @Nullable EnvironmentModel xEnvironment) {

        AppVisibilityModel visibility = appInputModel.getVisibility();

        AppSecretKeyDTO appSecretKey = appFacade.createApp(
            workspaceId, toEnvironment(xEnvironment), appInputModel.getName(), appInputModel.getDescription(),
            AppHostingTarget.valueOf(appInputModel.getHostingTarget()
                .name()),
            visibility == null ? ResourceVisibility.WORKSPACE : toVisibility(visibility));

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(toModel(appSecretKey));
    }

    @Override
    public ResponseEntity<AppModel> getApp(Long id, @Nullable EnvironmentModel xEnvironment) {
        return ResponseEntity.ok(toModel(getAppInEnvironment(id, xEnvironment)));
    }

    @Override
    public ResponseEntity<AppModel> updateApp(
        Long id, AppUpdateModel appUpdateModel, @Nullable EnvironmentModel xEnvironment) {

        AppDTO app = getAppInEnvironment(id, xEnvironment);

        if (hasFieldChange(appUpdateModel)) {
            AppHostingTargetModel hostingTarget = appUpdateModel.getHostingTarget();
            AppVisibilityModel visibility = appUpdateModel.getVisibility();

            app = appFacade.updateApp(
                id, appUpdateModel.getName(), appUpdateModel.getDescription(),
                hostingTarget == null ? null : AppHostingTarget.valueOf(hostingTarget.name()),
                appUpdateModel.getUrl(), toInstant(appUpdateModel.getLastDeployedDate()),
                visibility == null ? null : toVisibility(visibility));
        }

        AppStatusModel status = appUpdateModel.getStatus();

        if (status != null) {
            app = appFacade.updateAppStatus(id, AppStatus.valueOf(status.name()));
        }

        return ResponseEntity.ok(toModel(app));
    }

    @Override
    public ResponseEntity<Void> deleteApp(Long id, @Nullable EnvironmentModel xEnvironment) {
        requireAppInEnvironment(id, xEnvironment);

        appFacade.deleteApp(id);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<AppSecretKeyModel> rotateAppSecretKey(Long id, @Nullable EnvironmentModel xEnvironment) {
        requireAppInEnvironment(id, xEnvironment);

        return ResponseEntity.ok(toModel(appFacade.rotateSecretKey(id)));
    }

    @Override
    public ResponseEntity<List<AppBindingModel>> getAppBindings(Long id, @Nullable EnvironmentModel xEnvironment) {
        requireAppInEnvironment(id, xEnvironment);

        return ResponseEntity.ok(
            appFacade.getBindings(id)
                .stream()
                .map(this::toModel)
                .toList());
    }

    @Override
    public ResponseEntity<List<AppBindableConnectionModel>> getAppBindableConnections(
        Long id, @Nullable String componentName, @Nullable EnvironmentModel xEnvironment) {

        requireAppInEnvironment(id, xEnvironment);

        return ResponseEntity.ok(
            appFacade.getBindableConnections(id, componentName)
                .stream()
                .map(this::toModel)
                .toList());
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

    @Override
    public ResponseEntity<AppBindingModel> bindAppConnection(
        Long id, String bindingKey, AppBindingInputModel appBindingInputModel,
        @Nullable EnvironmentModel xEnvironment) {

        requireAppInEnvironment(id, xEnvironment);

        Boolean proxyEnabled = appBindingInputModel.getProxyEnabled();

        return ResponseEntity.ok(
            toModel(
                appFacade.bindConnection(
                    id, bindingKey, appBindingInputModel.getConnectionId(),
                    orEmpty(appBindingInputModel.getActionNames()), orEmpty(appBindingInputModel.getToolNames()),
                    proxyEnabled != null && proxyEnabled, orEmpty(appBindingInputModel.getProxyAllowedMethods()))));
    }

    @Override
    public ResponseEntity<Void> unbindAppConnection(
        Long id, String bindingKey, @Nullable EnvironmentModel xEnvironment) {

        requireAppInEnvironment(id, xEnvironment);

        appFacade.unbindConnection(id, bindingKey);

        return ResponseEntity.noContent()
            .build();
    }

    private AppDTO getAppInEnvironment(long id, @Nullable EnvironmentModel xEnvironment) {
        return AppEnvironmentScope.getApp(appFacade, environmentService, id, xEnvironment);
    }

    private void requireAppInEnvironment(long id, @Nullable EnvironmentModel xEnvironment) {
        AppEnvironmentScope.requireInEnvironment(appFacade, environmentService, id, xEnvironment);
    }

    private static boolean hasFieldChange(AppUpdateModel appUpdateModel) {
        return appUpdateModel.getName() != null || appUpdateModel.getDescription() != null ||
            appUpdateModel.getHostingTarget() != null || appUpdateModel.getUrl() != null ||
            appUpdateModel.getLastDeployedDate() != null || appUpdateModel.getVisibility() != null;
    }

    private static List<String> orEmpty(@Nullable List<String> values) {
        return values == null ? List.of() : values;
    }

    private Environment toEnvironment(@Nullable EnvironmentModel environmentModel) {
        return environmentService.getEnvironment(environmentModel == null ? null : environmentModel.name());
    }

    private static @Nullable Instant toInstant(@Nullable OffsetDateTime offsetDateTime) {
        return offsetDateTime == null ? null : offsetDateTime.toInstant();
    }

    private static @Nullable OffsetDateTime toOffsetDateTime(@Nullable Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static ResourceVisibility toVisibility(AppVisibilityModel appVisibilityModel) {
        return ResourceVisibility.valueOf(appVisibilityModel.name());
    }

    private AppModel toModel(AppDTO app) {
        return new AppModel()
            .id(app.id())
            .uuid(app.uuid())
            .name(app.name())
            .description(app.description())
            .workspaceId(app.workspaceId())
            .environment(EnvironmentModel.valueOf(app.environment()
                .name()))
            .ownerUserId(app.ownerUserId())
            .status(AppStatusModel.valueOf(app.status()
                .name()))
            .visibility(AppVisibilityModel.valueOf(app.visibility()
                .name()))
            .hostingTarget(AppHostingTargetModel.valueOf(app.hostingTarget()
                .name()))
            .url(app.url())
            .lastDeployedDate(toOffsetDateTime(app.lastDeployedDate()))
            .secretKeyHint(app.secretKeyHint())
            .secretKeyLastUsedDate(toOffsetDateTime(app.secretKeyLastUsedDate()))
            .createdBy(app.createdBy())
            .createdDate(toOffsetDateTime(app.createdDate()))
            .lastModifiedBy(app.lastModifiedBy())
            .lastModifiedDate(toOffsetDateTime(app.lastModifiedDate()))
            .version(app.version());
    }

    private AppSecretKeyModel toModel(AppSecretKeyDTO appSecretKey) {
        return new AppSecretKeyModel()
            .app(toModel(appSecretKey.app()))
            .secretKey(appSecretKey.secretKey());
    }

    private AppBindableConnectionModel toModel(AppBindableConnectionDTO appBindableConnectionDTO) {
        return new AppBindableConnectionModel()
            .id(appBindableConnectionDTO.id())
            .name(appBindableConnectionDTO.name())
            .componentName(appBindableConnectionDTO.componentName())
            .connectionVersion(appBindableConnectionDTO.connectionVersion())
            .componentVersion(appBindableConnectionDTO.componentVersion())
            .usable(appBindableConnectionDTO.usable());
    }

    private AppBindingModel toModel(AppBindingDTO binding) {
        return new AppBindingModel()
            .id(binding.id())
            .appId(binding.appId())
            .bindingKey(binding.bindingKey())
            .connectionId(binding.connectionId())
            .connectionName(binding.connectionName())
            .componentName(binding.componentName())
            .connectionVersion(binding.connectionVersion())
            .actionNames(binding.actionNames())
            .toolNames(binding.toolNames())
            .proxyEnabled(binding.proxyEnabled())
            .proxyAllowedMethods(binding.proxyAllowedMethods());
    }
}

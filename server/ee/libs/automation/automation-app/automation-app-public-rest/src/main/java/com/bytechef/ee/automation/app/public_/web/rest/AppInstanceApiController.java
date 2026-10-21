/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.dto.AppBindableConnectionDTO;
import com.bytechef.ee.automation.app.dto.AppBindingDTO;
import com.bytechef.ee.automation.app.dto.AppDTO;
import com.bytechef.ee.automation.app.dto.AppInstanceDTO;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.facade.AppInstanceFacade;
import com.bytechef.ee.automation.app.public_.web.rest.management.AppInstanceApi;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppBindableConnectionModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppBindingInputModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppBindingModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppHostingTargetModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppInstanceDeploymentSettingsUpdateModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppInstanceInputModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppInstanceModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppInstanceSecretKeyModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppInstanceStatusModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppInstanceStatusUpdateModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppInstanceUpdateModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppInstanceUserAuthUpdateModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppUserAuthModeModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.EnvironmentModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The App instance management endpoints of the automation public API: instances, their secret keys and their connection
 * bindings.
 *
 * <p>
 * {@link AppInstanceFacade} and {@link AppFacade} own all authorization and lookups, so this controller carries no
 * {@code @PreAuthorize}: it maps models to and from DTOs, one facade call per operation. Every mutation is refused
 * outside DEVELOPMENT before any facade is called. Every by-id route then requires the instance to be in the request's
 * environment, before any permission check, and answers an instance of another environment as not found.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.app.public_.web.rest.AppInstanceApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AppInstanceApiController extends AbstractAppApiController implements AppInstanceApi {

    private final AppFacade appFacade;
    private final AppInstanceFacade appInstanceFacade;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI2")
    public AppInstanceApiController(
        AppFacade appFacade, AppInstanceFacade appInstanceFacade, EnvironmentService environmentService) {

        this.appFacade = appFacade;
        this.appInstanceFacade = appInstanceFacade;
        this.environmentService = environmentService;
    }

    @Override
    public ResponseEntity<List<AppInstanceModel>> getAppInstances(Long id, @Nullable EnvironmentModel xEnvironment) {
        Environment environment = AppEnvironmentScope.getEnvironment(environmentService, xEnvironment);

        AppDTO app = appFacade.getApp(id);

        return ResponseEntity.ok(
            app.instances()
                .stream()
                .filter(appInstance -> appInstance.environment() == environment)
                .map(AppManagementModels::toModel)
                .toList());
    }

    @Override
    public ResponseEntity<AppInstanceSecretKeyModel> createAppInstance(
        Long id, AppInstanceInputModel appInstanceInputModel, @Nullable EnvironmentModel xEnvironment) {

        Environment environment = AppEnvironmentScope.requireDevelopment(environmentService, xEnvironment);

        ResourceVisibility visibility = AppManagementModels.toVisibility(appInstanceInputModel.getVisibility());

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(
                AppManagementModels.toModel(
                    appInstanceFacade.createAppInstance(
                        id, appInstanceInputModel.getWorkspaceId(), environment, appInstanceInputModel.getName(),
                        AppManagementModels.toHostingTarget(appInstanceInputModel.getHostingTarget()),
                        visibility == null ? ResourceVisibility.WORKSPACE : visibility,
                        AppManagementModels.toUserAuthMode(appInstanceInputModel.getUserAuthMode()))));
    }

    @Override
    public ResponseEntity<AppInstanceModel> getAppInstance(Long id, @Nullable EnvironmentModel xEnvironment) {
        requireInEnvironment(id, AppEnvironmentScope.getEnvironment(environmentService, xEnvironment));

        return ResponseEntity.ok(AppManagementModels.toModel(appInstanceFacade.getAppInstance(id)));
    }

    @Override
    public ResponseEntity<AppInstanceModel> updateAppInstance(
        Long id, AppInstanceUpdateModel appInstanceUpdateModel, @Nullable EnvironmentModel xEnvironment) {

        requireDevelopmentAndInEnvironment(id, xEnvironment);

        AppHostingTargetModel hostingTarget = appInstanceUpdateModel.getHostingTarget();
        JsonNullable<Map<String, Object>> configuration = appInstanceUpdateModel.getConfiguration();
        String name = appInstanceUpdateModel.getName();
        String url = appInstanceUpdateModel.getUrl();

        AppInstanceDTO appInstance = appInstanceFacade.updateAppInstance(
            id, name, hostingTarget == null ? null : AppManagementModels.toHostingTarget(hostingTarget), url,
            configuration != null && configuration.isPresent() ? configuration.get() : null);

        return ResponseEntity.ok(AppManagementModels.toModel(appInstance));
    }

    @Override
    public ResponseEntity<AppInstanceModel> updateAppInstanceStatus(
        Long id, AppInstanceStatusUpdateModel appInstanceStatusUpdateModel, @Nullable EnvironmentModel xEnvironment) {

        requireDevelopmentAndInEnvironment(id, xEnvironment);

        AppInstanceStatusModel status = appInstanceStatusUpdateModel.getStatus();

        return ResponseEntity.ok(
            AppManagementModels.toModel(
                appInstanceFacade.updateAppInstanceStatus(id, AppInstanceStatus.valueOf(status.name()))));
    }

    @Override
    public ResponseEntity<AppInstanceModel> updateAppInstanceUserAuth(
        Long id, AppInstanceUserAuthUpdateModel appInstanceUserAuthUpdateModel,
        @Nullable EnvironmentModel xEnvironment) {

        requireDevelopmentAndInEnvironment(id, xEnvironment);

        AppUserAuthModeModel userAuthMode = appInstanceUserAuthUpdateModel.getUserAuthMode();

        return ResponseEntity.ok(
            AppManagementModels.toModel(
                appInstanceFacade.updateAppInstanceUserAuth(id, AppUserAuthMode.valueOf(userAuthMode.name()))));
    }

    @Override
    public ResponseEntity<AppInstanceModel> updateAppInstanceDeploymentSettings(
        Long id, AppInstanceDeploymentSettingsUpdateModel appInstanceDeploymentSettingsUpdateModel,
        @Nullable EnvironmentModel xEnvironment) {

        requireDevelopmentAndInEnvironment(id, xEnvironment);

        return ResponseEntity.ok(
            AppManagementModels.toModel(
                appInstanceFacade.updateAppInstanceDeploymentSettings(
                    id, appInstanceDeploymentSettingsUpdateModel.getConnectionId(),
                    appInstanceDeploymentSettingsUpdateModel.getSettings())));
    }

    @Override
    public ResponseEntity<Void> deleteAppInstance(Long id, @Nullable EnvironmentModel xEnvironment) {
        requireDevelopmentAndInEnvironment(id, xEnvironment);

        appInstanceFacade.deleteAppInstance(id);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<AppInstanceSecretKeyModel> rotateAppInstanceSecretKey(
        Long id, @Nullable EnvironmentModel xEnvironment) {

        requireDevelopmentAndInEnvironment(id, xEnvironment);

        return ResponseEntity.ok(AppManagementModels.toModel(appInstanceFacade.rotateSecretKey(id)));
    }

    @Override
    public ResponseEntity<List<AppBindingModel>> getAppInstanceBindings(
        Long id, @Nullable EnvironmentModel xEnvironment) {

        requireInEnvironment(id, AppEnvironmentScope.getEnvironment(environmentService, xEnvironment));

        return ResponseEntity.ok(
            appInstanceFacade.getBindings(id)
                .stream()
                .map(AppInstanceApiController::toModel)
                .toList());
    }

    @Override
    public ResponseEntity<List<AppBindableConnectionModel>> getAppInstanceBindableConnections(
        Long id, @Nullable String componentName, @Nullable EnvironmentModel xEnvironment) {

        requireInEnvironment(id, AppEnvironmentScope.getEnvironment(environmentService, xEnvironment));

        return ResponseEntity.ok(
            appInstanceFacade.getBindableConnections(id, componentName)
                .stream()
                .map(AppInstanceApiController::toModel)
                .toList());
    }

    @Override
    public ResponseEntity<AppBindingModel> bindAppInstanceConnection(
        Long id, String bindingKey, AppBindingInputModel appBindingInputModel,
        @Nullable EnvironmentModel xEnvironment) {

        requireDevelopmentAndInEnvironment(id, xEnvironment);

        Boolean proxyEnabled = appBindingInputModel.getProxyEnabled();

        return ResponseEntity.ok(
            toModel(
                appInstanceFacade.bindConnection(
                    id, bindingKey, appBindingInputModel.getConnectionId(),
                    AppManagementModels.orEmpty(appBindingInputModel.getActionNames()),
                    AppManagementModels.orEmpty(appBindingInputModel.getToolNames()),
                    proxyEnabled != null && proxyEnabled,
                    AppManagementModels.orEmpty(appBindingInputModel.getProxyAllowedMethods()))));
    }

    @Override
    public ResponseEntity<Void> unbindAppInstanceConnection(
        Long id, String bindingKey, @Nullable EnvironmentModel xEnvironment) {

        requireDevelopmentAndInEnvironment(id, xEnvironment);

        appInstanceFacade.unbindConnection(id, bindingKey);

        return ResponseEntity.noContent()
            .build();
    }

    private void requireDevelopmentAndInEnvironment(long id, @Nullable EnvironmentModel xEnvironment) {
        requireInEnvironment(id, AppEnvironmentScope.requireDevelopment(environmentService, xEnvironment));
    }

    private void requireInEnvironment(long id, Environment environment) {
        AppEnvironmentScope.requireInEnvironment(appInstanceFacade, id, environment);
    }

    private static AppBindableConnectionModel toModel(AppBindableConnectionDTO appBindableConnectionDTO) {
        return new AppBindableConnectionModel()
            .id(appBindableConnectionDTO.id())
            .name(appBindableConnectionDTO.name())
            .componentName(appBindableConnectionDTO.componentName())
            .connectionVersion(appBindableConnectionDTO.connectionVersion())
            .componentVersion(appBindableConnectionDTO.componentVersion())
            .usable(appBindableConnectionDTO.usable());
    }

    private static AppBindingModel toModel(AppBindingDTO binding) {
        return new AppBindingModel()
            .id(binding.id())
            .appInstanceId(binding.appInstanceId())
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

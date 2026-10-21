/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.dto.AppBindableConnectionDTO;
import com.bytechef.ee.automation.app.dto.AppBindingDTO;
import com.bytechef.ee.automation.app.dto.AppDTO;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.facade.AppInstanceFacade;
import com.bytechef.ee.automation.app.web.graphql.AppGraphQlMapper.AppInstanceResponse;
import com.bytechef.ee.automation.app.web.graphql.AppGraphQlMapper.AppInstanceSecretKeyResponse;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller exposing app instances, their secret keys and their connection bindings for the admin UI.
 *
 * <p>
 * {@link AppInstanceFacade} and {@link AppFacade} own all authorization and lookups, so this controller carries no
 * {@code @PreAuthorize}: it only maps arguments and responses, and every operation makes one facade call.
 *
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
class AppInstanceGraphQlController {

    private final AppFacade appFacade;
    private final AppInstanceFacade appInstanceFacade;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    AppInstanceGraphQlController(
        AppFacade appFacade, AppInstanceFacade appInstanceFacade, EnvironmentService environmentService) {

        this.appFacade = appFacade;
        this.appInstanceFacade = appInstanceFacade;
        this.environmentService = environmentService;
    }

    @QueryMapping
    public AppInstanceResponse appInstance(@Argument long id) {
        return AppGraphQlMapper.toResponse(appInstanceFacade.getAppInstance(id));
    }

    @QueryMapping
    public List<AppInstanceResponse> appInstances(@Argument long appId) {
        AppDTO app = appFacade.getApp(appId);

        return app.instances()
            .stream()
            .map(AppGraphQlMapper::toResponse)
            .toList();
    }

    @QueryMapping
    public List<AppInstanceResponse> hostedAppInstances(@Argument long workspaceId, @Argument long environmentId) {
        Environment environment = environmentService.getEnvironment(environmentId);

        return appInstanceFacade.getHostedAppInstances(workspaceId, environment)
            .stream()
            .map(AppGraphQlMapper::toResponse)
            .toList();
    }

    @QueryMapping
    public List<AppBindingResponse> appBindings(@Argument long appInstanceId) {
        return appInstanceFacade.getBindings(appInstanceId)
            .stream()
            .map(AppInstanceGraphQlController::toResponse)
            .toList();
    }

    @QueryMapping
    public List<AppBindableConnectionResponse> appBindableConnections(@Argument long appInstanceId) {
        return appInstanceFacade.getBindableConnections(appInstanceId, null)
            .stream()
            .map(AppInstanceGraphQlController::toResponse)
            .toList();
    }

    @MutationMapping
    public AppInstanceSecretKeyResponse createAppInstance(
        @Argument long appId, @Argument long workspaceId, @Argument long environmentId,
        @Argument AppInstanceInput input) {

        Environment environment = environmentService.getEnvironment(environmentId);
        ResourceVisibility visibility = AppGraphQlMapper.parseVisibility(input.visibility());

        return AppGraphQlMapper.toResponse(
            appInstanceFacade.createAppInstance(
                appId, workspaceId, environment, input.name(), input.hostingTarget(),
                visibility == null ? ResourceVisibility.WORKSPACE : visibility, input.userAuthMode()));
    }

    @MutationMapping
    public AppInstanceResponse updateAppInstance(@Argument long id, @Argument AppInstanceUpdateInput input) {
        return AppGraphQlMapper.toResponse(
            appInstanceFacade.updateAppInstance(id, input.name(), input.hostingTarget(), input.url()));
    }

    @MutationMapping
    public AppInstanceResponse updateAppInstanceConfiguration(@Argument long id, @Argument Object configuration) {
        return AppGraphQlMapper.toResponse(appInstanceFacade.updateAppInstanceConfiguration(id, configuration));
    }

    @MutationMapping
    public AppInstanceResponse updateAppInstanceDeploymentSettings(
        @Argument long id, @Argument AppInstanceDeploymentSettingsInput input) {

        return AppGraphQlMapper.toResponse(
            appInstanceFacade.updateAppInstanceDeploymentSettings(
                id, input.deploymentConnectionId(), input.deploymentSettings()));
    }

    @MutationMapping
    public AppInstanceResponse updateAppInstanceStatus(@Argument long id, @Argument AppInstanceStatus status) {
        return AppGraphQlMapper.toResponse(appInstanceFacade.updateAppInstanceStatus(id, status));
    }

    @MutationMapping
    public AppInstanceResponse updateAppInstanceUserAuth(
        @Argument long id, @Argument AppUserAuthMode userAuthMode) {

        return AppGraphQlMapper.toResponse(appInstanceFacade.updateAppInstanceUserAuth(id, userAuthMode));
    }

    @MutationMapping
    public boolean deleteAppInstance(@Argument long id) {
        appInstanceFacade.deleteAppInstance(id);

        return true;
    }

    @MutationMapping
    public AppInstanceSecretKeyResponse rotateAppInstanceSecretKey(@Argument long id) {
        return AppGraphQlMapper.toResponse(appInstanceFacade.rotateSecretKey(id));
    }

    @MutationMapping
    public AppBindingResponse bindAppConnection(@Argument long appInstanceId, @Argument AppBindingInput input) {
        return toResponse(
            appInstanceFacade.bindConnection(
                appInstanceId, input.bindingKey(), input.connectionId(), input.actionNames(), input.toolNames(),
                input.proxyEnabled(), input.proxyAllowedMethods()));
    }

    @MutationMapping
    public boolean unbindAppConnection(@Argument long appInstanceId, @Argument String bindingKey) {
        appInstanceFacade.unbindConnection(appInstanceId, bindingKey);

        return true;
    }

    private static AppBindingResponse toResponse(AppBindingDTO binding) {
        return new AppBindingResponse(
            binding.id(), binding.appInstanceId(), binding.bindingKey(), binding.connectionId(),
            binding.connectionName(), binding.componentName(), binding.connectionVersion(), binding.componentVersion(),
            binding.actionNames(), binding.toolNames(), binding.proxyEnabled(), binding.proxyAllowedMethods());
    }

    private static AppBindableConnectionResponse toResponse(AppBindableConnectionDTO connection) {
        return new AppBindableConnectionResponse(
            connection.id(), connection.name(), connection.componentName(), connection.connectionVersion(),
            connection.componentVersion());
    }

    public record AppInstanceInput(
        String name, AppHostingTarget hostingTarget, @Nullable AppUserAuthMode userAuthMode,
        @Nullable String visibility) {
    }

    @SuppressFBWarnings("EI")
    public record AppInstanceDeploymentSettingsInput(
        @Nullable Long deploymentConnectionId, @Nullable Map<String, ?> deploymentSettings) {
    }

    public record AppInstanceUpdateInput(
        @Nullable String name, @Nullable AppHostingTarget hostingTarget, @Nullable String url) {
    }

    @SuppressFBWarnings("EI")
    public record AppBindingInput(
        String bindingKey, long connectionId, List<String> actionNames, List<String> toolNames, boolean proxyEnabled,
        List<String> proxyAllowedMethods) {
    }

    @SuppressFBWarnings("EI")
    public record AppBindingResponse(
        long id, long appInstanceId, String bindingKey, long connectionId, String connectionName,
        String componentName, int connectionVersion, @Nullable Integer componentVersion, List<String> actionNames,
        List<String> toolNames, boolean proxyEnabled, List<String> proxyAllowedMethods) {
    }

    public record AppBindableConnectionResponse(
        long id, String name, String componentName, int connectionVersion, int componentVersion) {
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.dto.AppBindableConnectionDTO;
import com.bytechef.ee.automation.app.dto.AppBindingDTO;
import com.bytechef.ee.automation.app.dto.AppComponentOperationsDTO;
import com.bytechef.ee.automation.app.dto.AppDTO;
import com.bytechef.ee.automation.app.dto.AppSecretKeyDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller exposing {@code App}s for the admin UI.
 *
 * <p>
 * {@link AppFacade} owns all authorization and lookups, so this controller carries no {@code @PreAuthorize}: it only
 * maps arguments and responses. A null update argument means "keep the stored value" (the facade owns that rule), and
 * an unrecognised visibility string surfaces as {@code APP_VISIBILITY_UNSUPPORTED}. Returns are mapped to
 * controller-local response records because graphql-java's default {@code String} scalar coercion throws for
 * {@link Instant}.
 *
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
class AppGraphQlController {

    private final AppFacade appFacade;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    AppGraphQlController(AppFacade appFacade, EnvironmentService environmentService) {
        this.appFacade = appFacade;
        this.environmentService = environmentService;
    }

    @QueryMapping
    public AppResponse app(@Argument long id) {
        return toResponse(appFacade.getApp(id));
    }

    @QueryMapping
    public List<AppBindingResponse> appBindings(@Argument long appId) {
        return appFacade.getBindings(appId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @QueryMapping
    public List<AppResponse> apps(@Argument long workspaceId, @Argument long environmentId) {
        return appFacade.getApps(workspaceId, environmentId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @QueryMapping
    public List<AppBindableConnectionResponse> appBindableConnections(@Argument long appId) {
        return appFacade.getBindableConnections(appId, null)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @QueryMapping
    public AppComponentOperationsResponse appComponentOperations(
        @Argument String componentName, @Argument int componentVersion) {

        return toResponse(appFacade.getComponentOperations(componentName, componentVersion));
    }

    @MutationMapping
    public AppSecretKeyResponse createApp(
        @Argument long workspaceId, @Argument long environmentId, @Argument AppInput input) {

        Environment environment = environmentService.getEnvironment(environmentId);

        return toResponse(
            appFacade.createApp(
                workspaceId, environment, input.name(), input.description(), input.hostingTarget(),
                input.visibility() == null ? ResourceVisibility.WORKSPACE : parseVisibility(input.visibility())));
    }

    @MutationMapping
    public AppResponse updateApp(@Argument long id, @Argument AppInput input) {
        return toResponse(
            appFacade.updateApp(
                id, input.name(), input.description(), input.hostingTarget(), input.url(), null,
                parseVisibility(input.visibility())));
    }

    @MutationMapping
    public AppResponse updateAppStatus(@Argument long id, @Argument AppStatus status) {
        return toResponse(appFacade.updateAppStatus(id, status));
    }

    @MutationMapping
    public AppSecretKeyResponse rotateAppSecretKey(@Argument long id) {
        return toResponse(appFacade.rotateSecretKey(id));
    }

    @MutationMapping
    public boolean deleteApp(@Argument long id) {
        appFacade.deleteApp(id);

        return true;
    }

    @MutationMapping
    public AppBindingResponse bindAppConnection(@Argument long appId, @Argument AppBindingInput input) {
        return toResponse(
            appFacade.bindConnection(
                appId, input.bindingKey(), input.connectionId(), input.actionNames(), input.toolNames(),
                input.proxyEnabled(), input.proxyAllowedMethods()));
    }

    @MutationMapping
    public boolean unbindAppConnection(@Argument long appId, @Argument String bindingKey) {
        appFacade.unbindConnection(appId, bindingKey);

        return true;
    }

    private AppResponse toResponse(AppDTO app) {
        return new AppResponse(
            app.id(), app.uuid(), app.name(), app.description(), app.workspaceId(), app.environment()
                .ordinal(),
            app.ownerUserId(), app.status(), app.visibility()
                .name(),
            app.hostingTarget(), app.url(), toString(app.lastDeployedDate()), app.secretKeyHint(),
            toString(app.secretKeyLastUsedDate()), app.createdBy(), toString(app.createdDate()), app.lastModifiedBy(),
            toString(app.lastModifiedDate()));
    }

    private AppSecretKeyResponse toResponse(AppSecretKeyDTO appSecretKey) {
        return new AppSecretKeyResponse(toResponse(appSecretKey.app()), appSecretKey.secretKey());
    }

    private AppBindingResponse toResponse(AppBindingDTO binding) {
        return new AppBindingResponse(
            binding.id(), binding.appId(), binding.bindingKey(), binding.connectionId(), binding.connectionName(),
            binding.componentName(), binding.connectionVersion(), binding.componentVersion(), binding.actionNames(),
            binding.toolNames(), binding.proxyEnabled(), binding.proxyAllowedMethods());
    }

    private AppBindableConnectionResponse toResponse(AppBindableConnectionDTO connection) {
        return new AppBindableConnectionResponse(
            connection.id(), connection.name(), connection.componentName(), connection.connectionVersion(),
            connection.componentVersion());
    }

    private AppComponentOperationsResponse toResponse(AppComponentOperationsDTO operations) {
        return new AppComponentOperationsResponse(operations.actionNames(), operations.toolNames());
    }

    private static ResourceVisibility parseVisibility(String visibility) {
        if (visibility == null) {
            return null;
        }

        try {
            return ResourceVisibility.valueOf(visibility.trim()
                .toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new ConfigurationException(
                "App does not support %s visibility".formatted(visibility), AppErrorType.APP_VISIBILITY_UNSUPPORTED);
        }
    }

    private static String toString(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    public record AppInput(
        String name, String description, AppHostingTarget hostingTarget, String url, String visibility) {
    }

    @SuppressFBWarnings("EI")
    public record AppBindingInput(
        String bindingKey, long connectionId, List<String> actionNames, List<String> toolNames, boolean proxyEnabled,
        List<String> proxyAllowedMethods) {
    }

    public record AppResponse(
        long id, String uuid, String name, String description, long workspaceId, long environmentId,
        long ownerUserId, AppStatus status, String visibility, AppHostingTarget hostingTarget, String url,
        String lastDeployedDate, String secretKeyHint, String secretKeyLastUsedDate, String createdBy,
        String createdDate, String lastModifiedBy, String lastModifiedDate) {
    }

    public record AppSecretKeyResponse(AppResponse app, String secretKey) {

        @Override
        public String toString() {
            return "AppSecretKeyResponse[app=" + app + ", secretKey=[REDACTED]]";
        }
    }

    @SuppressFBWarnings("EI")
    public record AppBindingResponse(
        long id, long appId, String bindingKey, long connectionId, String connectionName, String componentName,
        int connectionVersion, @Nullable Integer componentVersion, List<String> actionNames, List<String> toolNames,
        boolean proxyEnabled, List<String> proxyAllowedMethods) {
    }

    public record AppBindableConnectionResponse(
        long id, String name, String componentName, int connectionVersion, int componentVersion) {
    }

    @SuppressFBWarnings("EI")
    public record AppComponentOperationsResponse(List<String> actionNames, List<String> toolNames) {
    }
}

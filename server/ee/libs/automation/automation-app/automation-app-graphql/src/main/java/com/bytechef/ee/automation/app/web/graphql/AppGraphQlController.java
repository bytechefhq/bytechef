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
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.dto.AppCategoryDTO;
import com.bytechef.ee.automation.app.dto.AppComponentOperationsDTO;
import com.bytechef.ee.automation.app.dto.AppDTO;
import com.bytechef.ee.automation.app.dto.AppTagDTO;
import com.bytechef.ee.automation.app.dto.ForeignAppInstanceDTO;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.web.graphql.AppGraphQlMapper.AppInstanceResponse;
import com.bytechef.ee.automation.app.web.graphql.AppGraphQlMapper.AppInstanceSecretKeyResponse;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
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
 * an unrecognised visibility string on create surfaces as {@code APP_VISIBILITY_UNSUPPORTED}. Returns are mapped to
 * controller-local response records because graphql-java's default {@code String} scalar coercion throws for
 * {@link java.time.Instant}.
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
    public List<AppCategoryResponse> appCategories(@Argument long workspaceId) {
        return appFacade.getAppCategories(workspaceId)
            .stream()
            .map(category -> new AppCategoryResponse(Objects.requireNonNull(category.id()), category.name()))
            .toList();
    }

    @QueryMapping
    public List<AppTagResponse> appTags(@Argument long workspaceId) {
        return appFacade.getAppTags(workspaceId)
            .stream()
            .map(tag -> new AppTagResponse(Objects.requireNonNull(tag.id()), tag.name()))
            .toList();
    }

    @QueryMapping
    public List<AppResponse> apps(@Argument long workspaceId) {
        return appFacade.getApps(workspaceId)
            .stream()
            .map(AppGraphQlController::toResponse)
            .toList();
    }

    @QueryMapping
    public boolean appUserAuthAvailable(@Argument long workspaceId) {
        return appFacade.isUserAuthAvailable(workspaceId);
    }

    @QueryMapping
    public AppComponentOperationsResponse appComponentOperations(
        @Argument String componentName, @Argument int componentVersion) {

        AppComponentOperationsDTO operations = appFacade.getComponentOperations(componentName, componentVersion);

        return new AppComponentOperationsResponse(operations.actionNames(), operations.toolNames());
    }

    @MutationMapping
    public AppInstanceSecretKeyResponse createApp(
        @Argument long workspaceId, @Argument long environmentId, @Argument AppInput input) {

        Environment environment = environmentService.getEnvironment(environmentId);
        ResourceVisibility visibility = AppGraphQlMapper.parseVisibility(input.visibility());

        return AppGraphQlMapper.toResponse(
            appFacade.createApp(
                workspaceId, environment, input.name(), input.description(), input.hostingTarget(),
                visibility == null ? ResourceVisibility.WORKSPACE : visibility, input.userAuthMode(),
                toCategory(input.category()), Boolean.TRUE.equals(input.portalDiscoverable())));
    }

    @MutationMapping
    public AppResponse updateApp(@Argument long id, @Argument AppUpdateInput input) {
        return toResponse(
            appFacade.updateApp(
                id, input.name(), input.description(), toCategory(input.category()), input.portalDiscoverable()));
    }

    @MutationMapping
    public AppResponse updateAppStatus(@Argument long id, @Argument AppStatus status) {
        return toResponse(appFacade.updateAppStatus(id, status));
    }

    @MutationMapping
    public AppResponse updateAppTags(@Argument long id, @Argument List<AppTagInput> tags) {
        return toResponse(
            appFacade.updateAppTags(
                id, tags.stream()
                    .map(tag -> new AppTagDTO(tag.id(), tag.name()))
                    .toList()));
    }

    @MutationMapping
    public boolean deleteApp(@Argument long id) {
        appFacade.deleteApp(id);

        return true;
    }

    private static @Nullable AppCategoryDTO toCategory(@Nullable AppCategoryInput category) {
        if (category == null) {
            return null;
        }

        return new AppCategoryDTO(category.id(), category.name());
    }

    private static AppResponse toResponse(AppDTO app) {
        return new AppResponse(
            app.id(), app.uuid(), app.name(), app.description(), app.workspaceId(), app.ownerUserId(),
            app.categoryId(), app.tagIds(), app.hasIcon(), app.status(), app.visibility()
                .name(),
            app.portalDiscoverable(),
            app.instances()
                .stream()
                .map(AppGraphQlMapper::toResponse)
                .toList(),
            app.foreignInstances()
                .stream()
                .map(AppGraphQlController::toResponse)
                .toList(),
            app.createdBy(), AppGraphQlMapper.toDateString(app.createdDate()), app.lastModifiedBy(),
            AppGraphQlMapper.toDateString(app.lastModifiedDate()), app.version());
    }

    private static ForeignAppInstanceResponse toResponse(ForeignAppInstanceDTO foreignAppInstance) {
        return new ForeignAppInstanceResponse(
            foreignAppInstance.id(), foreignAppInstance.name(), foreignAppInstance.workspaceId(),
            foreignAppInstance.environment()
                .ordinal(),
            foreignAppInstance.status(), foreignAppInstance.currentAppDeploymentId());
    }

    public record AppCategoryInput(@Nullable Long id, @Nullable String name) {
    }

    public record AppCategoryResponse(long id, String name) {
    }

    public record AppTagInput(@Nullable Long id, String name) {
    }

    public record AppTagResponse(long id, String name) {
    }

    public record AppInput(
        String name, @Nullable String description, AppHostingTarget hostingTarget,
        @Nullable AppUserAuthMode userAuthMode, @Nullable String visibility, @Nullable AppCategoryInput category,
        @Nullable Boolean portalDiscoverable) {
    }

    public record AppUpdateInput(
        @Nullable String name, @Nullable String description, @Nullable AppCategoryInput category,
        @Nullable Boolean portalDiscoverable) {
    }

    @SuppressFBWarnings("EI")
    public record AppResponse(
        long id, String uuid, String name, @Nullable String description, long workspaceId, long ownerUserId,
        @Nullable Long categoryId, List<Long> tagIds, boolean hasIcon, AppStatus status, String visibility,
        boolean portalDiscoverable,
        List<AppInstanceResponse> instances, List<ForeignAppInstanceResponse> foreignInstances, String createdBy,
        @Nullable String createdDate, String lastModifiedBy, @Nullable String lastModifiedDate, int version) {
    }

    public record ForeignAppInstanceResponse(
        long id, String name, long workspaceId, long environmentId, AppInstanceStatus status,
        @Nullable Long currentAppDeploymentId) {
    }

    @SuppressFBWarnings("EI")
    public record AppComponentOperationsResponse(List<String> actionNames, List<String> toolNames) {
    }
}

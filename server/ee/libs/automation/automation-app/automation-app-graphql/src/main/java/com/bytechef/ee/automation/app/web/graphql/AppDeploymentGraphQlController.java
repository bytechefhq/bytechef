/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.app.domain.AppDeploymentProviderType;
import com.bytechef.ee.automation.app.domain.AppDeploymentStatus;
import com.bytechef.ee.automation.app.dto.AppDeploymentDTO;
import com.bytechef.ee.automation.app.dto.AppVersionDTO;
import com.bytechef.ee.automation.app.facade.AppDeploymentFacade;
import com.bytechef.ee.automation.app.web.graphql.AppGraphQlController.AppResponse;
import com.bytechef.ee.automation.app.web.graphql.AppGraphQlMapper.AppInstanceResponse;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller exposing the versions of an app and the recorded deployments of its instances for the admin UI.
 *
 * <p>
 * {@link AppDeploymentFacade} owns all authorization and lookups, so this controller carries no {@code @PreAuthorize}:
 * it only maps arguments and responses, and every operation makes at most one facade call. A deployment's provider
 * metadata is not exposed.
 *
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
class AppDeploymentGraphQlController {

    private final AppDeploymentFacade appDeploymentFacade;

    @SuppressFBWarnings("EI")
    AppDeploymentGraphQlController(AppDeploymentFacade appDeploymentFacade) {
        this.appDeploymentFacade = appDeploymentFacade;
    }

    @QueryMapping
    public List<AppVersionResponse> appVersions(@Argument long appId, @Argument @Nullable Integer limit) {
        return toVersionResponses(appDeploymentFacade.getVersions(appId, resolveLimit(limit)));
    }

    @QueryMapping
    public List<AppVersionResponse> appInstanceVersions(
        @Argument long appInstanceId, @Argument @Nullable Integer limit) {

        return toVersionResponses(appDeploymentFacade.getVersionsForInstance(appInstanceId, resolveLimit(limit)));
    }

    @QueryMapping
    public List<AppDeploymentResponse> appDeployments(
        @Argument long appInstanceId, @Argument @Nullable Integer limit) {

        return toDeploymentResponses(appDeploymentFacade.getDeployments(appInstanceId, resolveLimit(limit)));
    }

    @QueryMapping
    public AppDeploymentResponse appDeployment(@Argument long appInstanceId, @Argument long appDeploymentId) {
        return toResponse(appDeploymentFacade.getDeployment(appInstanceId, appDeploymentId));
    }

    @SchemaMapping(typeName = "App", field = "versions")
    public List<AppVersionResponse> versions(AppResponse app, @Argument @Nullable Integer limit) {
        return toVersionResponses(appDeploymentFacade.getVersions(app.id(), resolveLimit(limit)));
    }

    @SchemaMapping(typeName = "AppInstance", field = "deployments")
    public List<AppDeploymentResponse> deployments(
        AppInstanceResponse appInstance, @Argument @Nullable Integer limit) {

        return toDeploymentResponses(appDeploymentFacade.getDeployments(appInstance.id(), resolveLimit(limit)));
    }

    @SchemaMapping(typeName = "AppInstance", field = "currentDeployment")
    public @Nullable AppDeploymentResponse currentDeployment(AppInstanceResponse appInstance) {
        if (appInstance.currentAppDeploymentId() == null) {
            return null;
        }

        return appDeploymentFacade.getCurrentDeployment(appInstance.id())
            .map(AppDeploymentGraphQlController::toResponse)
            .orElse(null);
    }

    @SchemaMapping(typeName = "AppInstance", field = "deploymentProviderAvailable")
    public boolean deploymentProviderAvailable(AppInstanceResponse appInstance) {
        return appDeploymentFacade.isDeploymentProviderAvailable(appInstance.hostingTarget());
    }

    @MutationMapping
    public AppDeploymentResponse cancelAppDeployment(@Argument long appInstanceId, @Argument long appDeploymentId) {
        return toResponse(appDeploymentFacade.cancel(appInstanceId, appDeploymentId));
    }

    @MutationMapping
    public AppVersionResponse createAppVersion(@Argument long appId, @Argument AppVersionInput input) {
        return toResponse(
            appDeploymentFacade.createVersion(
                appId, input.name(), input.gitRef(), input.gitCommitSha(), input.description()));
    }

    @MutationMapping
    public AppDeploymentResponse deployAppInstance(@Argument long appInstanceId, @Argument long appVersionId) {
        return toResponse(appDeploymentFacade.deploy(appInstanceId, appVersionId));
    }

    @MutationMapping
    public AppDeploymentResponse recordAppDeployment(
        @Argument long appInstanceId, @Argument AppDeploymentRecordInput input) {

        return toResponse(
            appDeploymentFacade.recordDeployment(appInstanceId, input.versionName(), input.url(),
                input.gitCommitSha()));
    }

    @MutationMapping
    public AppDeploymentResponse rollbackAppInstance(@Argument long appInstanceId, @Argument long appVersionId) {
        return toResponse(appDeploymentFacade.rollback(appInstanceId, appVersionId));
    }

    private static int resolveLimit(@Nullable Integer limit) {
        return limit == null ? AppDeploymentFacade.DEFAULT_LIST_LIMIT : limit;
    }

    private static List<AppVersionResponse> toVersionResponses(List<AppVersionDTO> appVersions) {
        return appVersions.stream()
            .map(AppDeploymentGraphQlController::toResponse)
            .toList();
    }

    private static List<AppDeploymentResponse> toDeploymentResponses(List<AppDeploymentDTO> appDeployments) {
        return appDeployments.stream()
            .map(AppDeploymentGraphQlController::toResponse)
            .toList();
    }

    private static AppVersionResponse toResponse(AppVersionDTO appVersion) {
        return new AppVersionResponse(
            appVersion.id(), appVersion.appId(), appVersion.name(), appVersion.gitRef(), appVersion.gitCommitSha(),
            appVersion.description(), appVersion.createdBy(), AppGraphQlMapper.toDateString(appVersion.createdDate()));
    }

    private static AppDeploymentResponse toResponse(AppDeploymentDTO appDeployment) {
        return new AppDeploymentResponse(
            appDeployment.id(), appDeployment.appInstanceId(), toResponse(appDeployment.appVersion()),
            appDeployment.provider(), appDeployment.providerDeploymentId(), appDeployment.url(), appDeployment.status(),
            appDeployment.errorMessage(), appDeployment.current(), appDeployment.createdBy(),
            AppGraphQlMapper.toDateString(appDeployment.createdDate()),
            AppGraphQlMapper.toDateString(appDeployment.finishedDate()));
    }

    public record AppVersionInput(
        String name, @Nullable String gitRef, @Nullable String gitCommitSha, @Nullable String description) {
    }

    public record AppDeploymentRecordInput(String versionName, @Nullable String url, @Nullable String gitCommitSha) {
    }

    public record AppVersionResponse(
        long id, long appId, String name, @Nullable String gitRef, @Nullable String gitCommitSha,
        @Nullable String description, String createdBy, @Nullable String createdDate) {
    }

    public record AppDeploymentResponse(
        long id, long appInstanceId, AppVersionResponse appVersion, AppDeploymentProviderType provider,
        @Nullable String providerDeploymentId, @Nullable String url, AppDeploymentStatus status,
        @Nullable String errorMessage, boolean current, String createdBy, @Nullable String createdDate,
        @Nullable String finishedDate) {
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.app.domain.AppDeploymentProviderType;
import com.bytechef.ee.automation.app.domain.AppDeploymentStatus;
import com.bytechef.ee.automation.app.dto.AppDeploymentDTO;
import com.bytechef.ee.automation.app.dto.AppVersionDTO;
import com.bytechef.ee.automation.app.facade.AppDeploymentFacade;
import com.bytechef.ee.automation.app.facade.AppInstanceFacade;
import com.bytechef.ee.automation.app.public_.web.rest.management.AppDeploymentApi;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppDeploymentModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppDeploymentProviderModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppDeploymentRecordInputModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppDeploymentStatusModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppDeploymentTriggerInputModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppVersionInputModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppVersionModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.EnvironmentModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The App version and deployment endpoints of the automation public API.
 *
 * <p>
 * {@link AppDeploymentFacade} owns all authorization and lookups, so this controller carries no {@code @PreAuthorize}:
 * it maps models to and from DTOs, one facade call per operation. These are the management operations allowed in every
 * environment, so none of them requires DEVELOPMENT. A version belongs to an app, which spans environments, so the
 * app-keyed version routes are not scoped to {@code X-Environment}. An instance route, the instance-keyed version read
 * included, requires the instance to be in the request's environment before the guarded facade call, and answers an
 * instance of another environment as not found.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.app.public_.web.rest.AppDeploymentApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AppDeploymentApiController extends AbstractAppApiController implements AppDeploymentApi {

    private final AppDeploymentFacade appDeploymentFacade;
    private final AppInstanceFacade appInstanceFacade;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI2")
    public AppDeploymentApiController(
        AppDeploymentFacade appDeploymentFacade, AppInstanceFacade appInstanceFacade,
        EnvironmentService environmentService) {

        this.appDeploymentFacade = appDeploymentFacade;
        this.appInstanceFacade = appInstanceFacade;
        this.environmentService = environmentService;
    }

    @Override
    public ResponseEntity<List<AppVersionModel>> getAppVersions(
        Long id, @Nullable EnvironmentModel xEnvironment, @Nullable Integer limit, @Nullable String name) {

        if (name != null) {
            Optional<AppVersionDTO> appVersion = appDeploymentFacade.fetchVersion(id, name);

            return ResponseEntity.ok(
                appVersion.stream()
                    .map(AppDeploymentApiController::toModel)
                    .toList());
        }

        return ResponseEntity.ok(
            appDeploymentFacade.getVersions(id, resolveLimit(limit))
                .stream()
                .map(AppDeploymentApiController::toModel)
                .toList());
    }

    @Override
    public ResponseEntity<List<AppVersionModel>> getAppInstanceVersions(
        Long id, @Nullable EnvironmentModel xEnvironment, @Nullable Integer limit, @Nullable String name) {

        requireInEnvironment(id, xEnvironment);

        if (name != null) {
            Optional<AppVersionDTO> appVersion = appDeploymentFacade.fetchVersionForInstance(id, name);

            return ResponseEntity.ok(
                appVersion.stream()
                    .map(AppDeploymentApiController::toModel)
                    .toList());
        }

        return ResponseEntity.ok(
            appDeploymentFacade.getVersionsForInstance(id, resolveLimit(limit))
                .stream()
                .map(AppDeploymentApiController::toModel)
                .toList());
    }

    @Override
    public ResponseEntity<AppVersionModel> createAppVersion(
        Long id, AppVersionInputModel appVersionInputModel, @Nullable EnvironmentModel xEnvironment) {

        AppVersionDTO appVersion = appDeploymentFacade.createVersion(
            id, appVersionInputModel.getName(), appVersionInputModel.getGitRef(),
            appVersionInputModel.getGitCommitSha(), appVersionInputModel.getDescription());

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(toModel(appVersion));
    }

    @Override
    public ResponseEntity<List<AppDeploymentModel>> getAppInstanceDeployments(
        Long id, @Nullable EnvironmentModel xEnvironment, @Nullable Integer limit) {

        requireInEnvironment(id, xEnvironment);

        return ResponseEntity.ok(
            appDeploymentFacade.getDeployments(id, resolveLimit(limit))
                .stream()
                .map(AppDeploymentApiController::toModel)
                .toList());
    }

    @Override
    public ResponseEntity<AppDeploymentModel> recordAppInstanceDeployment(
        Long id, AppDeploymentRecordInputModel appDeploymentRecordInputModel,
        @Nullable EnvironmentModel xEnvironment) {

        requireInEnvironment(id, xEnvironment);

        AppDeploymentDTO appDeployment = appDeploymentFacade.recordDeployment(
            id, appDeploymentRecordInputModel.getVersionName(), appDeploymentRecordInputModel.getUrl(),
            appDeploymentRecordInputModel.getGitCommitSha());

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(toModel(appDeployment));
    }

    @Override
    public ResponseEntity<AppDeploymentModel> getAppInstanceDeployment(
        Long id, Long deploymentId, @Nullable EnvironmentModel xEnvironment) {

        requireInEnvironment(id, xEnvironment);

        return ResponseEntity.ok(toModel(appDeploymentFacade.getDeployment(id, deploymentId)));
    }

    @Override
    public ResponseEntity<AppDeploymentModel> deployAppInstance(
        Long id, AppDeploymentTriggerInputModel appDeploymentTriggerInputModel,
        @Nullable EnvironmentModel xEnvironment) {

        requireInEnvironment(id, xEnvironment);

        AppDeploymentDTO appDeployment = appDeploymentFacade.deploy(
            id, appDeploymentTriggerInputModel.getAppVersionId());

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(toModel(appDeployment));
    }

    @Override
    public ResponseEntity<AppDeploymentModel> rollbackAppInstance(
        Long id, AppDeploymentTriggerInputModel appDeploymentTriggerInputModel,
        @Nullable EnvironmentModel xEnvironment) {

        requireInEnvironment(id, xEnvironment);

        AppDeploymentDTO appDeployment = appDeploymentFacade.rollback(
            id, appDeploymentTriggerInputModel.getAppVersionId());

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(toModel(appDeployment));
    }

    @Override
    public ResponseEntity<AppDeploymentModel> cancelAppInstanceDeployment(
        Long id, Long deploymentId, @Nullable EnvironmentModel xEnvironment) {

        requireInEnvironment(id, xEnvironment);

        return ResponseEntity.ok(toModel(appDeploymentFacade.cancel(id, deploymentId)));
    }

    private void requireInEnvironment(long appInstanceId, @Nullable EnvironmentModel xEnvironment) {
        Environment environment = AppEnvironmentScope.getEnvironment(environmentService, xEnvironment);

        AppEnvironmentScope.requireInEnvironment(appInstanceFacade, appInstanceId, environment);
    }

    private static int resolveLimit(@Nullable Integer limit) {
        return limit == null ? AppDeploymentFacade.DEFAULT_LIST_LIMIT : limit;
    }

    private static AppVersionModel toModel(AppVersionDTO appVersion) {
        return new AppVersionModel()
            .id(appVersion.id())
            .appId(appVersion.appId())
            .name(appVersion.name())
            .gitRef(appVersion.gitRef())
            .gitCommitSha(appVersion.gitCommitSha())
            .description(appVersion.description())
            .createdBy(appVersion.createdBy())
            .createdDate(AppManagementModels.toOffsetDateTime(appVersion.createdDate()));
    }

    private static AppDeploymentModel toModel(AppDeploymentDTO appDeployment) {
        AppDeploymentProviderType provider = appDeployment.provider();
        AppDeploymentStatus status = appDeployment.status();

        return new AppDeploymentModel()
            .id(appDeployment.id())
            .appInstanceId(appDeployment.appInstanceId())
            .appVersion(toModel(appDeployment.appVersion()))
            .provider(AppDeploymentProviderModel.valueOf(provider.name()))
            .providerDeploymentId(appDeployment.providerDeploymentId())
            .url(appDeployment.url())
            .status(AppDeploymentStatusModel.valueOf(status.name()))
            .errorMessage(appDeployment.errorMessage())
            .current(appDeployment.current())
            .createdBy(appDeployment.createdBy())
            .createdDate(AppManagementModels.toOffsetDateTime(appDeployment.createdDate()))
            .finishedDate(AppManagementModels.toOffsetDateTime(appDeployment.finishedDate()));
    }
}

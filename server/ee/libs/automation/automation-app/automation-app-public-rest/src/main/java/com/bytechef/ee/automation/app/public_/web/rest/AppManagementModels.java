/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.dto.AppCategoryDTO;
import com.bytechef.ee.automation.app.dto.AppInstanceDTO;
import com.bytechef.ee.automation.app.dto.AppSecretKeyDTO;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppCategoryModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppHostingTargetModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppInstanceModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppInstanceSecretKeyModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppInstanceStatusModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppStatusModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppUserAuthModeModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppVisibilityModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.EnvironmentModel;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class AppManagementModels {

    private AppManagementModels() {
    }

    static List<String> orEmpty(@Nullable List<String> values) {
        return values == null ? List.of() : values;
    }

    static @Nullable AppCategoryDTO toCategory(@Nullable AppCategoryModel appCategoryModel) {
        if (appCategoryModel == null) {
            return null;
        }

        return new AppCategoryDTO(appCategoryModel.getId(), appCategoryModel.getName());
    }

    static AppHostingTarget toHostingTarget(AppHostingTargetModel appHostingTargetModel) {
        return AppHostingTarget.valueOf(appHostingTargetModel.name());
    }

    static AppInstanceModel toModel(AppInstanceDTO appInstance) {
        AppInstanceDTO.AppSummary app = appInstance.app();
        Environment environment = appInstance.environment();
        ResourceVisibility visibility = appInstance.visibility();

        return new AppInstanceModel()
            .id(appInstance.id())
            .uuid(appInstance.uuid())
            .appId(app.id())
            .appName(app.name())
            .appHasIcon(app.hasIcon())
            .appStatus(AppStatusModel.valueOf(app.status()
                .name()))
            .workspaceId(appInstance.workspaceId())
            .environment(EnvironmentModel.valueOf(environment.name()))
            .name(appInstance.name())
            .ownerUserId(appInstance.ownerUserId())
            .status(AppInstanceStatusModel.valueOf(appInstance.status()
                .name()))
            .visibility(AppVisibilityModel.valueOf(visibility.name()))
            .hostingTarget(AppHostingTargetModel.valueOf(appInstance.hostingTarget()
                .name()))
            .url(appInstance.url())
            .secretKeyHint(appInstance.secretKeyHint())
            .secretKeyLastUsedDate(toOffsetDateTime(appInstance.secretKeyLastUsedDate()))
            .userAuthMode(AppUserAuthModeModel.valueOf(appInstance.userAuthMode()
                .name()))
            ._configuration(new HashMap<>(appInstance.configuration()))
            .currentAppDeploymentId(appInstance.currentAppDeploymentId())
            .deploymentConnectionId(appInstance.deploymentConnectionId())
            .deploymentSettings(new HashMap<>(appInstance.deploymentSettings()))
            .createdBy(appInstance.createdBy())
            .createdDate(toOffsetDateTime(appInstance.createdDate()))
            .lastModifiedBy(appInstance.lastModifiedBy())
            .lastModifiedDate(toOffsetDateTime(appInstance.lastModifiedDate()))
            .version(appInstance.version());
    }

    static AppInstanceSecretKeyModel toModel(AppSecretKeyDTO appSecretKey) {
        return new AppInstanceSecretKeyModel()
            .appInstance(toModel(appSecretKey.appInstance()))
            .secretKey(appSecretKey.secretKey());
    }

    static @Nullable OffsetDateTime toOffsetDateTime(@Nullable Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    static @Nullable AppUserAuthMode toUserAuthMode(@Nullable AppUserAuthModeModel appUserAuthModeModel) {
        return appUserAuthModeModel == null ? null : AppUserAuthMode.valueOf(appUserAuthModeModel.name());
    }

    static @Nullable ResourceVisibility toVisibility(@Nullable AppVisibilityModel appVisibilityModel) {
        return appVisibilityModel == null ? null : ResourceVisibility.valueOf(appVisibilityModel.name());
    }
}

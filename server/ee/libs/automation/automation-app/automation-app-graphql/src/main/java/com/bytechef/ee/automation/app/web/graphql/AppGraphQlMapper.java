/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.web.graphql;

import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.dto.AppInstanceDTO;
import com.bytechef.ee.automation.app.dto.AppSecretKeyDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class AppGraphQlMapper {

    private AppGraphQlMapper() {
    }

    static @Nullable ResourceVisibility parseVisibility(@Nullable String visibility) {
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

    static @Nullable String toDateString(@Nullable Instant instant) {
        return instant == null ? null : instant.toString();
    }

    static AppInstanceResponse toResponse(AppInstanceDTO appInstance) {
        AppInstanceDTO.AppSummary app = appInstance.app();
        ResourceVisibility visibility = appInstance.visibility();

        return new AppInstanceResponse(
            appInstance.id(), appInstance.uuid(),
            new AppSummaryResponse(
                app.id(), app.name(), app.description(), app.categoryId(), app.hasIcon(), app.status()),
            appInstance.workspaceId(), appInstance.environment()
                .ordinal(),
            appInstance.name(), appInstance.ownerUserId(), appInstance.status(), visibility.name(),
            appInstance.hostingTarget(), appInstance.url(), appInstance.secretKeyHint(),
            toDateString(appInstance.secretKeyLastUsedDate()), appInstance.userAuthMode(), appInstance.configuration(),
            appInstance.currentAppDeploymentId(), appInstance.deploymentConnectionId(),
            appInstance.deploymentSettings(), appInstance.createdBy(), toDateString(appInstance.createdDate()),
            appInstance.lastModifiedBy(), toDateString(appInstance.lastModifiedDate()), appInstance.version());
    }

    static AppInstanceSecretKeyResponse toResponse(AppSecretKeyDTO appSecretKey) {
        return new AppInstanceSecretKeyResponse(toResponse(appSecretKey.appInstance()), appSecretKey.secretKey());
    }

    @SuppressFBWarnings("EI")
    public record AppInstanceResponse(
        long id, String uuid, AppSummaryResponse app, long workspaceId, long environmentId, String name,
        long ownerUserId, AppInstanceStatus status, String visibility, AppHostingTarget hostingTarget,
        @Nullable String url, @Nullable String secretKeyHint, @Nullable String secretKeyLastUsedDate,
        AppUserAuthMode userAuthMode, Map<String, ?> configuration, @Nullable Long currentAppDeploymentId,
        @Nullable Long deploymentConnectionId, Map<String, ?> deploymentSettings, String createdBy,
        @Nullable String createdDate, String lastModifiedBy, @Nullable String lastModifiedDate,
        int version) {
    }

    public record AppInstanceSecretKeyResponse(AppInstanceResponse appInstance, String secretKey) {

        @Override
        public String toString() {
            return "AppInstanceSecretKeyResponse[appInstance=" + appInstance + ", secretKey=[REDACTED]]";
        }
    }

    public record AppSummaryResponse(
        long id, String name, @Nullable String description, @Nullable Long categoryId, boolean hasIcon,
        AppStatus status) {
    }
}

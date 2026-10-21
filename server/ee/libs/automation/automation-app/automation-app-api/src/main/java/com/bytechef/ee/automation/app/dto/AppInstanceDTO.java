/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The management view of an app instance with a read-only summary of its app. It never carries the secret key hash,
 * only the hint (the key's last characters). An instance seen from the app's owning workspace when it lives in another
 * workspace is a {@link ForeignAppInstanceDTO} instead.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AppInstanceDTO(
    long id, String uuid, AppSummary app, long workspaceId, Environment environment, String name, long ownerUserId,
    AppInstanceStatus status, ResourceVisibility visibility, AppHostingTarget hostingTarget, @Nullable String url,
    @Nullable String secretKeyHint, @Nullable Instant secretKeyLastUsedDate, AppUserAuthMode userAuthMode,
    Map<String, ?> configuration, @Nullable Long currentAppDeploymentId, @Nullable Long deploymentConnectionId,
    Map<String, ?> deploymentSettings, String createdBy, Instant createdDate,
    String lastModifiedBy,
    Instant lastModifiedDate, int version) {

    public static AppInstanceDTO of(App app, AppInstance appInstance) {
        return new AppInstanceDTO(
            appInstance.getId(), appInstance.getUuid(), AppSummary.of(app), appInstance.getWorkspaceId(),
            appInstance.getEnvironment(), appInstance.getName(), appInstance.getOwnerUserId(), appInstance.getStatus(),
            appInstance.getVisibility(), appInstance.getHostingTarget(), appInstance.getUrl(),
            appInstance.getSecretKeyHint(), appInstance.getSecretKeyLastUsedDate(), appInstance.getUserAuthMode(),
            appInstance.getConfiguration(), appInstance.getCurrentAppDeploymentId(),
            appInstance.getDeploymentConnectionId(), appInstance.getDeploymentSettings(),
            appInstance.getCreatedBy(), appInstance.getCreatedDate(), appInstance.getLastModifiedBy(),
            appInstance.getLastModifiedDate(), appInstance.getVersion());
    }

    /**
     * The app fields an instance shows; they are changed through the app, never through the instance.
     */
    public record AppSummary(
        long id, String name, @Nullable String description, @Nullable Long categoryId, boolean hasIcon,
        AppStatus status) {

        public static AppSummary of(App app) {
            return new AppSummary(
                app.getId(), app.getName(), app.getDescription(), app.getCategoryId(), app.getIcon() != null,
                app.getStatus());
        }
    }
}

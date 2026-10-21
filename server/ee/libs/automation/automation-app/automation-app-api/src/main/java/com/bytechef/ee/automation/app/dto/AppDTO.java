/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The management view of an app. It never carries the secret key hash; only the hint (the key's last characters).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AppDTO(
    long id, String uuid, String name, @Nullable String description, long workspaceId, Environment environment,
    long ownerUserId, AppStatus status, ResourceVisibility visibility, AppHostingTarget hostingTarget,
    @Nullable String url, @Nullable Instant lastDeployedDate, String secretKeyHint,
    @Nullable Instant secretKeyLastUsedDate, String createdBy, Instant createdDate, String lastModifiedBy,
    Instant lastModifiedDate, int version) {

    public static AppDTO of(App app) {
        return new AppDTO(
            app.getId(), app.getUuid(), app.getName(), app.getDescription(),
            Objects.requireNonNull(app.getWorkspaceId(), "workspaceId"),
            app.getEnvironment(), app.getOwnerUserId(), app.getStatus(), app.getVisibility(), app.getHostingTarget(),
            app.getUrl(), app.getLastDeployedDate(), app.getSecretKeyHint(), app.getSecretKeyLastUsedDate(),
            app.getCreatedBy(), app.getCreatedDate(), app.getLastModifiedBy(), app.getLastModifiedDate(),
            app.getVersion());
    }
}

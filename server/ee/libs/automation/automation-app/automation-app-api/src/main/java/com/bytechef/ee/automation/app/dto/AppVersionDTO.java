/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

import com.bytechef.ee.automation.app.domain.AppVersion;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AppVersionDTO(
    long id, long appId, String name, @Nullable String gitRef, @Nullable String gitCommitSha,
    @Nullable String description, String createdBy, Instant createdDate) {

    public static AppVersionDTO of(AppVersion appVersion) {
        return new AppVersionDTO(
            appVersion.getId(), appVersion.getAppId(), appVersion.getName(), appVersion.getGitRef(),
            appVersion.getGitCommitSha(), appVersion.getDescription(), appVersion.getCreatedBy(),
            appVersion.getCreatedDate());
    }
}

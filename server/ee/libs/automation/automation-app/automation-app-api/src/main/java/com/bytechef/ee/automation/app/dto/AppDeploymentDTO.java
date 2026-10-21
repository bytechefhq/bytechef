/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

import com.bytechef.ee.automation.app.domain.AppDeployment;
import com.bytechef.ee.automation.app.domain.AppDeploymentProviderType;
import com.bytechef.ee.automation.app.domain.AppDeploymentStatus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AppDeploymentDTO(
    long id, long appInstanceId, AppVersionDTO appVersion, AppDeploymentProviderType provider,
    @Nullable String providerDeploymentId, @Nullable String url, AppDeploymentStatus status,
    @Nullable String errorMessage, Map<String, ?> metadata, boolean current, String createdBy, Instant createdDate,
    @Nullable Instant finishedDate) {

    public static AppDeploymentDTO of(AppDeployment appDeployment, AppVersionDTO appVersion, boolean current) {
        return new AppDeploymentDTO(
            appDeployment.getId(), appDeployment.getAppInstanceId(), appVersion, appDeployment.getProvider(),
            appDeployment.getProviderDeploymentId(), appDeployment.getUrl(), appDeployment.getStatus(),
            appDeployment.getErrorMessage(), appDeployment.getMetadata(), current, appDeployment.getCreatedBy(),
            appDeployment.getCreatedDate(), appDeployment.getFinishedDate());
    }
}

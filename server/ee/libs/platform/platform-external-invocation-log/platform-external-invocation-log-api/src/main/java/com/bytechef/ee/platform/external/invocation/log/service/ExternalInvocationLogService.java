/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.external.invocation.log.service;

import com.bytechef.ee.platform.external.invocation.log.domain.ExternalInvocationLog;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationSource;
import com.bytechef.platform.external.invocation.ExternalInvocationStatus;
import java.time.Instant;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ExternalInvocationLogService {

    ExternalInvocationLog create(ExternalInvocationLog externalInvocationLog);

    int deleteExternalInvocationLogsCreatedBefore(Instant instant);

    Optional<ExternalInvocationLog> fetchLastFailedExternalInvocationLog(PlatformType type, int environment);

    boolean hasExternalInvocationLog(PlatformType type, int environment, ExternalInvocationStatus status);

    Page<ExternalInvocationLog> getExternalInvocationLogs(
        PlatformType type, @Nullable Integer environment, @Nullable ExternalInvocationSource source,
        @Nullable ExternalInvocationStatus status,
        @Nullable Long mcpServerId, @Nullable Long a2aServerId, @Nullable Long connectedUserId,
        @Nullable Long integrationInstanceId,
        @Nullable Instant startDate, @Nullable Instant endDate, int pageNumber);

    Page<ExternalInvocationLog> getWorkspaceExternalInvocationLogs(
        PlatformType type, long workspaceId, int environment, @Nullable ExternalInvocationSource source,
        @Nullable ExternalInvocationStatus status, @Nullable Long mcpServerId, @Nullable Long a2aServerId,
        @Nullable Long connectedUserId,
        @Nullable Long integrationInstanceId, @Nullable Instant startDate, @Nullable Instant endDate,
        int pageNumber);
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.external.invocation.log.remote.client.service;

import com.bytechef.ee.platform.external.invocation.log.domain.ExternalInvocationLog;
import com.bytechef.ee.platform.external.invocation.log.service.ExternalInvocationLogService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationSource;
import com.bytechef.platform.external.invocation.ExternalInvocationStatus;
import java.time.Instant;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteExternalInvocationLogServiceClient implements ExternalInvocationLogService {

    @Override
    public ExternalInvocationLog create(ExternalInvocationLog externalInvocationLog) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int deleteExternalInvocationLogsCreatedBefore(Instant instant) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<ExternalInvocationLog> fetchLastFailedExternalInvocationLog(PlatformType type, int environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasExternalInvocationLog(PlatformType type, int environment, ExternalInvocationStatus status) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Page<ExternalInvocationLog> getExternalInvocationLogs(
        PlatformType type, @Nullable Integer environment, @Nullable ExternalInvocationSource source,
        @Nullable ExternalInvocationStatus status, @Nullable Long mcpServerId, @Nullable Long a2aServerId,
        @Nullable Long connectedUserId, @Nullable Long integrationInstanceId, @Nullable Instant startDate,
        @Nullable Instant endDate, int pageNumber) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Page<ExternalInvocationLog> getWorkspaceExternalInvocationLogs(
        PlatformType type, long workspaceId, int environment, @Nullable ExternalInvocationSource source,
        @Nullable ExternalInvocationStatus status, @Nullable Long mcpServerId, @Nullable Long a2aServerId,
        @Nullable Long connectedUserId, @Nullable Long integrationInstanceId, @Nullable Instant startDate,
        @Nullable Instant endDate, int pageNumber) {

        throw new UnsupportedOperationException();
    }
}

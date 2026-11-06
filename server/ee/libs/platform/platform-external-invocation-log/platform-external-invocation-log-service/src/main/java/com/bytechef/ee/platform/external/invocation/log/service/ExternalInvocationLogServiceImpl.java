/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.external.invocation.log.service;

import com.bytechef.ee.platform.external.invocation.log.domain.ExternalInvocationLog;
import com.bytechef.ee.platform.external.invocation.log.repository.ExternalInvocationLogRepository;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationOutcome;
import com.bytechef.platform.external.invocation.ExternalInvocationSurface;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
public class ExternalInvocationLogServiceImpl implements ExternalInvocationLogService {

    private static final int DEFAULT_PAGE_SIZE = 50;

    private final ExternalInvocationLogRepository externalInvocationLogRepository;

    @SuppressFBWarnings("EI2")
    public ExternalInvocationLogServiceImpl(ExternalInvocationLogRepository externalInvocationLogRepository) {
        this.externalInvocationLogRepository = externalInvocationLogRepository;
    }

    @Override
    @Transactional
    public ExternalInvocationLog create(ExternalInvocationLog externalInvocationLog) {
        return externalInvocationLogRepository.save(externalInvocationLog);
    }

    @Override
    @Transactional
    public int deleteExternalInvocationLogsCreatedBefore(Instant instant) {
        return externalInvocationLogRepository.deleteByCreatedDateBefore(instant);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExternalInvocationLog> getExternalInvocationLogs(
        PlatformType type, @Nullable Integer environment, @Nullable ExternalInvocationSurface surface,
        @Nullable ExternalInvocationOutcome outcome,
        @Nullable Long mcpServerId, @Nullable Long a2aServerId, @Nullable Long connectedUserId,
        @Nullable Long integrationInstanceId,
        @Nullable Instant startDate, @Nullable Instant endDate, int pageNumber) {

        return findPage(
            type, null, environment, surface, outcome, mcpServerId, a2aServerId, connectedUserId, integrationInstanceId,
            startDate,
            endDate, pageNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExternalInvocationLog> getWorkspaceExternalInvocationLogs(
        PlatformType type, long workspaceId, int environment, @Nullable ExternalInvocationSurface surface,
        @Nullable ExternalInvocationOutcome outcome, @Nullable Long mcpServerId, @Nullable Long a2aServerId,
        @Nullable Long connectedUserId,
        @Nullable Long integrationInstanceId, @Nullable Instant startDate, @Nullable Instant endDate,
        int pageNumber) {

        return findPage(
            type, workspaceId, environment, surface, outcome, mcpServerId, a2aServerId, connectedUserId,
            integrationInstanceId,
            startDate, endDate, pageNumber);
    }

    private Page<ExternalInvocationLog> findPage(
        PlatformType type, @Nullable Long workspaceId, @Nullable Integer environment,
        @Nullable ExternalInvocationSurface surface,
        @Nullable ExternalInvocationOutcome outcome, @Nullable Long mcpServerId, @Nullable Long a2aServerId,
        @Nullable Long connectedUserId,
        @Nullable Long integrationInstanceId, @Nullable Instant startDate, @Nullable Instant endDate,
        int pageNumber) {

        Integer surfaceOrdinal = surface == null ? null : surface.ordinal();
        Integer outcomeOrdinal = outcome == null ? null : outcome.ordinal();

        PageRequest pageRequest = PageRequest.of(pageNumber, DEFAULT_PAGE_SIZE);

        List<ExternalInvocationLog> content = externalInvocationLogRepository.findFiltered(
            type.ordinal(), workspaceId, environment, surfaceOrdinal, outcomeOrdinal, mcpServerId, a2aServerId,
            connectedUserId,
            integrationInstanceId, startDate, endDate, pageRequest.getPageSize(), pageRequest.getOffset());

        long total = externalInvocationLogRepository.countFiltered(
            type.ordinal(), workspaceId, environment, surfaceOrdinal, outcomeOrdinal, mcpServerId, a2aServerId,
            connectedUserId,
            integrationInstanceId, startDate, endDate);

        return new PageImpl<>(content, pageRequest, total);
    }
}

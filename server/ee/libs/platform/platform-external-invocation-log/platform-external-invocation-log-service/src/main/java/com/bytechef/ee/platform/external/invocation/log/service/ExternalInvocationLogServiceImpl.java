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
import com.bytechef.platform.external.invocation.ExternalInvocationSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

    private static final List<Integer> NON_FAILED_OUTCOMES = List.of(
        ExternalInvocationOutcome.SUCCESS.ordinal(), ExternalInvocationOutcome.APPROVAL_REQUIRED.ordinal(),
        ExternalInvocationOutcome.SUSPENDED.ordinal());

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
    public Optional<ExternalInvocationLog> fetchLastFailedExternalInvocationLog(PlatformType type, int environment) {
        return externalInvocationLogRepository.findLastFailed(type.ordinal(), environment, NON_FAILED_OUTCOMES);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasExternalInvocationLog(PlatformType type, int environment, ExternalInvocationOutcome outcome) {
        return externalInvocationLogRepository.existsByTypeAndEnvironmentAndOutcome(
            type.ordinal(), environment, outcome.ordinal());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExternalInvocationLog> getExternalInvocationLogs(
        PlatformType type, @Nullable Integer environment, @Nullable ExternalInvocationSource source,
        @Nullable ExternalInvocationOutcome outcome,
        @Nullable Long mcpServerId, @Nullable Long a2aServerId, @Nullable Long connectedUserId,
        @Nullable Long integrationInstanceId,
        @Nullable Instant startDate, @Nullable Instant endDate, int pageNumber) {

        return findPage(
            type, null, environment, source, outcome, mcpServerId, a2aServerId, connectedUserId, integrationInstanceId,
            startDate,
            endDate, pageNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExternalInvocationLog> getWorkspaceExternalInvocationLogs(
        PlatformType type, long workspaceId, int environment, @Nullable ExternalInvocationSource source,
        @Nullable ExternalInvocationOutcome outcome, @Nullable Long mcpServerId, @Nullable Long a2aServerId,
        @Nullable Long connectedUserId,
        @Nullable Long integrationInstanceId, @Nullable Instant startDate, @Nullable Instant endDate,
        int pageNumber) {

        return findPage(
            type, workspaceId, environment, source, outcome, mcpServerId, a2aServerId, connectedUserId,
            integrationInstanceId,
            startDate, endDate, pageNumber);
    }

    private Page<ExternalInvocationLog> findPage(
        PlatformType type, @Nullable Long workspaceId, @Nullable Integer environment,
        @Nullable ExternalInvocationSource source,
        @Nullable ExternalInvocationOutcome outcome, @Nullable Long mcpServerId, @Nullable Long a2aServerId,
        @Nullable Long connectedUserId,
        @Nullable Long integrationInstanceId, @Nullable Instant startDate, @Nullable Instant endDate,
        int pageNumber) {

        Integer sourceOrdinal = source == null ? null : source.ordinal();
        Integer outcomeOrdinal = outcome == null ? null : outcome.ordinal();

        PageRequest pageRequest = PageRequest.of(pageNumber, DEFAULT_PAGE_SIZE);

        List<ExternalInvocationLog> content = externalInvocationLogRepository.findFiltered(
            type.ordinal(), workspaceId, environment, sourceOrdinal, outcomeOrdinal, mcpServerId, a2aServerId,
            connectedUserId,
            integrationInstanceId, startDate, endDate, pageRequest.getPageSize(), pageRequest.getOffset());

        long total = externalInvocationLogRepository.countFiltered(
            type.ordinal(), workspaceId, environment, sourceOrdinal, outcomeOrdinal, mcpServerId, a2aServerId,
            connectedUserId,
            integrationInstanceId, startDate, endDate);

        return new PageImpl<>(content, pageRequest, total);
    }
}

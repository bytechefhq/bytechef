/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log;

import com.bytechef.ee.platform.tool.invocation.log.repository.ToolInvocationLogRepository;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
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
public class ToolInvocationLogServiceImpl implements ToolInvocationLogService {

    private static final int DEFAULT_PAGE_SIZE = 50;

    private final ToolInvocationLogRepository toolInvocationLogRepository;

    @SuppressFBWarnings("EI2")
    public ToolInvocationLogServiceImpl(ToolInvocationLogRepository toolInvocationLogRepository) {
        this.toolInvocationLogRepository = toolInvocationLogRepository;
    }

    @Override
    @Transactional
    public ToolInvocationLog create(ToolInvocationLog toolInvocationLog) {
        return toolInvocationLogRepository.save(toolInvocationLog);
    }

    @Override
    @Transactional
    public int deleteToolInvocationLogsCreatedBefore(Instant instant) {
        return toolInvocationLogRepository.deleteByCreatedDateBefore(instant);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ToolInvocationLog> getToolInvocationLogs(
        @Nullable ToolExecutionSurface surface, @Nullable ToolExecutionOutcome outcome, @Nullable Long mcpServerId,
        @Nullable Long connectedUserId, @Nullable Long integrationInstanceId, @Nullable Instant startDate,
        @Nullable Instant endDate, int pageNumber) {

        Integer surfaceOrdinal = surface == null ? null : surface.ordinal();
        Integer outcomeOrdinal = outcome == null ? null : outcome.ordinal();

        PageRequest pageRequest = PageRequest.of(pageNumber, DEFAULT_PAGE_SIZE);

        List<ToolInvocationLog> content = toolInvocationLogRepository.findFiltered(
            surfaceOrdinal, outcomeOrdinal, mcpServerId, connectedUserId, integrationInstanceId, startDate, endDate,
            pageRequest.getPageSize(), pageRequest.getOffset());

        long total = toolInvocationLogRepository.countFiltered(
            surfaceOrdinal, outcomeOrdinal, mcpServerId, connectedUserId, integrationInstanceId, startDate, endDate);

        return new PageImpl<>(content, pageRequest, total);
    }
}

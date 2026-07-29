/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLog;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller exposing read-only access to the tool invocation log.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class ToolInvocationLogGraphQlController {

    private final ToolInvocationLogService toolInvocationLogService;

    @SuppressFBWarnings("EI")
    public ToolInvocationLogGraphQlController(ToolInvocationLogService toolInvocationLogService) {
        this.toolInvocationLogService = toolInvocationLogService;
    }

    @QueryMapping(name = "toolInvocationLogs")
    public ToolInvocationLogPage toolInvocationLogs(
        @Argument @Nullable String surface, @Argument @Nullable String outcome, @Argument @Nullable Long mcpServerId,
        @Argument @Nullable Long connectedUserId, @Argument @Nullable Long integrationInstanceId,
        @Argument @Nullable Long fromDate, @Argument @Nullable Long toDate, @Argument @Nullable Integer page) {

        Page<ToolInvocationLog> toolInvocationLogPage = toolInvocationLogService.getToolInvocationLogs(
            toSurface(surface), toOutcome(outcome), mcpServerId, connectedUserId, integrationInstanceId,
            toInstant(fromDate), toInstant(toDate), page == null ? 0 : page);

        List<ToolInvocationLogItem> content = toolInvocationLogPage.getContent()
            .stream()
            .map(ToolInvocationLogGraphQlController::toItem)
            .toList();

        return new ToolInvocationLogPage(
            content, toolInvocationLogPage.getNumber(), toolInvocationLogPage.getSize(),
            toolInvocationLogPage.getTotalElements(), toolInvocationLogPage.getTotalPages());
    }

    private static @Nullable ToolExecutionSurface toSurface(@Nullable String surface) {
        return surface == null ? null : ToolExecutionSurface.valueOf(surface);
    }

    private static @Nullable ToolExecutionOutcome toOutcome(@Nullable String outcome) {
        return outcome == null ? null : ToolExecutionOutcome.valueOf(outcome);
    }

    private static @Nullable Instant toInstant(@Nullable Long epochMillis) {
        return epochMillis == null ? null : Instant.ofEpochMilli(epochMillis);
    }

    private static ToolInvocationLogItem toItem(ToolInvocationLog toolInvocationLog) {
        Instant createdDate = toolInvocationLog.getCreatedDate();

        return new ToolInvocationLogItem(
            toolInvocationLog.getId(),
            toolInvocationLog.getSurface()
                .name(),
            toolInvocationLog.getKind()
                .name(),
            toolInvocationLog.getToolName(), toolInvocationLog.getComponentName(),
            toolInvocationLog.getComponentVersion(), toolInvocationLog.getOperationName(),
            toolInvocationLog.getConnectionId(), toolInvocationLog.getEnvironment(),
            toolInvocationLog.getWorkspaceId(), toolInvocationLog.getExternalUserId(),
            toolInvocationLog.getConnectedUserId(), toolInvocationLog.getIntegrationInstanceId(),
            toolInvocationLog.getMcpServerId(), toolInvocationLog.getJobId(),
            toolInvocationLog.getOutcome()
                .name(),
            toolInvocationLog.getErrorType(), toolInvocationLog.getErrorMessage(),
            toolInvocationLog.getDurationMs(), createdDate == null ? null : createdDate.toEpochMilli());
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    record ToolInvocationLogItem(
        Long id, String surface, String kind, String toolName, String componentName, Integer componentVersion,
        String operationName, Long connectionId, Integer environment, Long workspaceId, String externalUserId,
        Long connectedUserId, Long integrationInstanceId, Long mcpServerId, Long jobId, String outcome,
        String errorType, String errorMessage, int durationMs, Long createdDate) {
    }

    record ToolInvocationLogPage(
        List<ToolInvocationLogItem> content, int number, int size, long totalElements, int totalPages) {
    }
}

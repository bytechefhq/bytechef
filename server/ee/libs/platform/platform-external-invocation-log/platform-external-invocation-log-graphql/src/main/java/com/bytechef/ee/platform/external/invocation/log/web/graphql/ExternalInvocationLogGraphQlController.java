/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.external.invocation.log.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.platform.external.invocation.log.domain.ExternalInvocationLog;
import com.bytechef.ee.platform.external.invocation.log.dto.ExternalInvocationLogDTO;
import com.bytechef.ee.platform.external.invocation.log.facade.ExternalInvocationLogFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationOutcome;
import com.bytechef.platform.external.invocation.ExternalInvocationSurface;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class ExternalInvocationLogGraphQlController {

    private final ExternalInvocationLogFacade externalInvocationLogFacade;

    @SuppressFBWarnings("EI")
    public ExternalInvocationLogGraphQlController(ExternalInvocationLogFacade externalInvocationLogFacade) {
        this.externalInvocationLogFacade = externalInvocationLogFacade;
    }

    @QueryMapping(name = "externalInvocationLogs")
    public ExternalInvocationLogPage externalInvocationLogs(
        @Argument PlatformType type, @Argument @Nullable Long environmentId, @Argument @Nullable String surface,
        @Argument @Nullable String outcome,
        @Argument @Nullable Long mcpServerId, @Argument @Nullable Long a2aServerId,
        @Argument @Nullable Long connectedUserId,
        @Argument @Nullable Long integrationInstanceId, @Argument @Nullable Long fromDate,
        @Argument @Nullable Long toDate, @Argument @Nullable Integer page) {

        return toPage(
            externalInvocationLogFacade.getExternalInvocationLogs(
                type, environmentId, toSurface(surface), toOutcome(outcome), mcpServerId, a2aServerId, connectedUserId,
                integrationInstanceId, toInstant(fromDate), toInstant(toDate), page == null ? 0 : page));
    }

    @QueryMapping(name = "workspaceExternalInvocationLogs")
    public ExternalInvocationLogPage workspaceExternalInvocationLogs(
        @Argument PlatformType type, @Argument long workspaceId, @Argument long environmentId,
        @Argument @Nullable String surface,
        @Argument @Nullable String outcome, @Argument @Nullable Long mcpServerId, @Argument @Nullable Long a2aServerId,
        @Argument @Nullable Long connectedUserId, @Argument @Nullable Long integrationInstanceId,
        @Argument @Nullable Long fromDate, @Argument @Nullable Long toDate, @Argument @Nullable Integer page) {

        return toPage(
            externalInvocationLogFacade.getWorkspaceExternalInvocationLogs(
                type, workspaceId, environmentId, toSurface(surface), toOutcome(outcome), mcpServerId, a2aServerId,
                connectedUserId,
                integrationInstanceId, toInstant(fromDate), toInstant(toDate), page == null ? 0 : page));
    }

    private static ExternalInvocationLogPage toPage(Page<ExternalInvocationLogDTO> externalInvocationLogPage) {
        List<ExternalInvocationLogItem> content = externalInvocationLogPage.getContent()
            .stream()
            .map(ExternalInvocationLogGraphQlController::toItem)
            .toList();

        return new ExternalInvocationLogPage(
            content, externalInvocationLogPage.getNumber(), externalInvocationLogPage.getSize(),
            externalInvocationLogPage.getTotalElements(), externalInvocationLogPage.getTotalPages());
    }

    private static @Nullable ExternalInvocationSurface toSurface(@Nullable String surface) {
        return surface == null ? null : ExternalInvocationSurface.valueOf(surface);
    }

    private static @Nullable ExternalInvocationOutcome toOutcome(@Nullable String outcome) {
        return outcome == null ? null : ExternalInvocationOutcome.valueOf(outcome);
    }

    private static @Nullable Instant toInstant(@Nullable Long epochMillis) {
        return epochMillis == null ? null : Instant.ofEpochMilli(epochMillis);
    }

    private static ExternalInvocationLogItem toItem(ExternalInvocationLogDTO externalInvocationLogDTO) {
        ExternalInvocationLog externalInvocationLog = externalInvocationLogDTO.externalInvocationLog();
        Instant createdDate = externalInvocationLog.getCreatedDate();

        return new ExternalInvocationLogItem(
            externalInvocationLog.getId(),
            externalInvocationLog.getSurface()
                .name(),
            externalInvocationLog.getKind()
                .name(),
            externalInvocationLog.getToolName(), externalInvocationLog.getComponentName(),
            externalInvocationLog.getComponentVersion(), externalInvocationLog.getOperationName(),
            externalInvocationLog.getConnectionId(), externalInvocationLog.getEnvironment(),
            externalInvocationLog.getWorkspaceId(), externalInvocationLog.getExternalUserId(),
            externalInvocationLog.getConnectedUserId(), externalInvocationLog.getIntegrationInstanceId(),
            externalInvocationLog.getMcpServerId(), externalInvocationLog.getA2aServerId(),
            externalInvocationLog.getJobId(),
            externalInvocationLogDTO.workflowName(),
            externalInvocationLog.getOutcome()
                .name(),
            externalInvocationLog.getErrorType(), externalInvocationLog.getErrorMessage(),
            externalInvocationLog.getDurationMs(), createdDate == null ? null : createdDate.toEpochMilli());
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    record ExternalInvocationLogItem(
        Long id, String surface, String kind, String toolName, String componentName, Integer componentVersion,
        String operationName, Long connectionId, Integer environment, Long workspaceId, String externalUserId,
        Long connectedUserId, Long integrationInstanceId, Long mcpServerId, Long a2aServerId, Long jobId,
        String workflowName, String outcome, String errorType, String errorMessage, int durationMs, Long createdDate) {
    }

    record ExternalInvocationLogPage(
        List<ExternalInvocationLogItem> content, int number, int size, long totalElements, int totalPages) {
    }
}

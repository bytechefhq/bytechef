/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log.facade;

import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLog;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class ToolInvocationLogFacadeImpl implements ToolInvocationLogFacade {

    private static final String IS_PLATFORM_USER =
        "!T(com.bytechef.platform.security.web.authentication.NonPlatformUserAuthentication).isCurrent()";

    private final ToolInvocationLogService toolInvocationLogService;

    @SuppressFBWarnings("EI")
    public ToolInvocationLogFacadeImpl(ToolInvocationLogService toolInvocationLogService) {
        this.toolInvocationLogService = toolInvocationLogService;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public Page<ToolInvocationLog> getToolInvocationLogs(
        @Nullable ToolExecutionSurface surface, @Nullable ToolExecutionOutcome outcome, @Nullable Long mcpServerId,
        @Nullable Long connectedUserId, @Nullable Long integrationInstanceId, @Nullable Instant startDate,
        @Nullable Instant endDate, int pageNumber) {

        return toolInvocationLogService.getToolInvocationLogs(
            surface, outcome, mcpServerId, connectedUserId, integrationInstanceId, startDate, endDate, pageNumber);
    }

    @Override
    @PreAuthorize(IS_PLATFORM_USER + " and hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_VIEW')")
    public Page<ToolInvocationLog> getWorkspaceToolInvocationLogs(
        long workspaceId, long environmentId, @Nullable ToolExecutionSurface surface,
        @Nullable ToolExecutionOutcome outcome, @Nullable Long mcpServerId, @Nullable Long connectedUserId,
        @Nullable Long integrationInstanceId, @Nullable Instant startDate, @Nullable Instant endDate,
        int pageNumber) {

        return toolInvocationLogService.getWorkspaceToolInvocationLogs(
            workspaceId, Math.toIntExact(environmentId), surface, outcome, mcpServerId, connectedUserId,
            integrationInstanceId, startDate, endDate, pageNumber);
    }
}

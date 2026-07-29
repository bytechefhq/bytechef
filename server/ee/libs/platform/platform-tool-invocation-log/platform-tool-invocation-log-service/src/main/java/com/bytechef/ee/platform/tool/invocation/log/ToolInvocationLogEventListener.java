/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.tool.execution.ToolExecutionEvent;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Persists a {@link com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLog} row for each
 * {@link ToolExecutionEvent} published by the CE tool-execution seam. Runs off the caller's thread on the
 * tenant-propagating async executor and is best-effort — a persistence failure is logged and swallowed so it can never
 * affect the originating tool call.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ToolInvocationLogEventListener {

    private static final Logger log = LoggerFactory.getLogger(ToolInvocationLogEventListener.class);

    private final ToolInvocationLogService toolInvocationLogService;

    @SuppressFBWarnings("EI2")
    public ToolInvocationLogEventListener(ToolInvocationLogService toolInvocationLogService) {
        this.toolInvocationLogService = toolInvocationLogService;
    }

    @EventListener
    @Async
    public void onToolExecutionEvent(ToolExecutionEvent event) {
        try {
            toolInvocationLogService.create(toToolInvocationLog(event));
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to persist tool invocation log for surface={} tool={}: {}", event.surface(), event.toolName(),
                exception.getMessage());
        }
    }

    private static ToolInvocationLog toToolInvocationLog(ToolExecutionEvent event) {
        ToolInvocationLog toolInvocationLog = new ToolInvocationLog();

        String tenantId = event.tenantId() == null ? TenantContext.getCurrentTenantId() : event.tenantId();

        toolInvocationLog.setTenantId(tenantId);
        toolInvocationLog.setSurface(event.surface());
        toolInvocationLog.setKind(event.kind());
        toolInvocationLog.setToolName(event.toolName());
        toolInvocationLog.setComponentName(event.componentName());
        toolInvocationLog.setComponentVersion(event.componentVersion());
        toolInvocationLog.setOperationName(event.operationName());
        toolInvocationLog.setConnectionId(event.connectionId());
        toolInvocationLog.setEnvironment(event.environment());
        toolInvocationLog.setWorkspaceId(event.workspaceId());
        toolInvocationLog.setExternalUserId(event.externalUserId());
        toolInvocationLog.setConnectedUserId(event.connectedUserId());
        toolInvocationLog.setIntegrationInstanceId(event.integrationInstanceId());
        toolInvocationLog.setMcpServerId(event.mcpServerId());
        toolInvocationLog.setJobId(event.jobId());
        toolInvocationLog.setOutcome(event.outcome());
        toolInvocationLog.setErrorType(event.errorType());
        toolInvocationLog.setErrorMessage(event.errorMessage());
        toolInvocationLog.setDurationMs((int) event.durationMs());
        toolInvocationLog.setCreatedDate(Instant.now());

        return toolInvocationLog;
    }
}

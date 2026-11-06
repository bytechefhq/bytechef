/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.external.invocation.log.listener;

import com.bytechef.ee.platform.external.invocation.log.domain.ExternalInvocationLog;
import com.bytechef.ee.platform.external.invocation.log.service.ExternalInvocationLogService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.external.invocation.ExternalInvocationEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ExternalInvocationLogEventListener {

    private static final Logger log = LoggerFactory.getLogger(ExternalInvocationLogEventListener.class);

    private static final int MAX_ERROR_MESSAGE_LENGTH = 512;
    private static final int MAX_NAME_LENGTH = 256;

    private final ExternalInvocationLogService externalInvocationLogService;

    @SuppressFBWarnings("EI2")
    public ExternalInvocationLogEventListener(ExternalInvocationLogService externalInvocationLogService) {
        this.externalInvocationLogService = externalInvocationLogService;
    }

    @EventListener
    @Async("workerExecutor")
    public void onExternalInvocationEvent(ExternalInvocationEvent event) {
        try {
            externalInvocationLogService.create(toExternalInvocationLog(event));
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to persist external invocation log for surface={} tool={}: {}", event.surface(),
                event.toolName(),
                exception.getMessage());
        }
    }

    private static ExternalInvocationLog toExternalInvocationLog(ExternalInvocationEvent event) {
        ExternalInvocationLog externalInvocationLog = new ExternalInvocationLog();

        externalInvocationLog.setType(event.type());
        externalInvocationLog.setSurface(event.surface());
        externalInvocationLog.setKind(event.kind());
        externalInvocationLog.setToolName(truncate(event.toolName(), MAX_NAME_LENGTH));
        externalInvocationLog.setComponentName(truncate(event.componentName(), MAX_NAME_LENGTH));
        externalInvocationLog.setComponentVersion(event.componentVersion());
        externalInvocationLog.setOperationName(truncate(event.operationName(), MAX_NAME_LENGTH));
        externalInvocationLog.setConnectionId(event.connectionId());
        externalInvocationLog.setEnvironment(event.environment());
        externalInvocationLog.setWorkspaceId(event.workspaceId());
        externalInvocationLog.setExternalUserId(event.externalUserId());
        externalInvocationLog.setConnectedUserId(event.connectedUserId());
        externalInvocationLog.setIntegrationInstanceId(event.integrationInstanceId());
        externalInvocationLog.setMcpServerId(event.mcpServerId());
        externalInvocationLog.setA2aServerId(event.a2aServerId());
        externalInvocationLog.setJobId(event.jobId());
        externalInvocationLog.setOutcome(event.outcome());
        externalInvocationLog.setErrorType(truncate(event.errorType(), MAX_NAME_LENGTH));
        externalInvocationLog.setErrorMessage(truncate(event.errorMessage(), MAX_ERROR_MESSAGE_LENGTH));
        externalInvocationLog.setDurationMs((int) event.durationMs());
        externalInvocationLog.setCreatedDate(Instant.now());

        return externalInvocationLog;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}

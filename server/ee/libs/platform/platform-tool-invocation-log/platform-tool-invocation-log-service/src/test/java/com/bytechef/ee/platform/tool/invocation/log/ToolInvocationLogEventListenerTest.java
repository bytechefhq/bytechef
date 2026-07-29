/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.tool.execution.ToolExecutionEvent;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ToolInvocationLogEventListenerTest {

    private ToolInvocationLogEventListener toolInvocationLogEventListener;
    private ToolInvocationLogService toolInvocationLogService;

    @BeforeEach
    void beforeEach() {
        toolInvocationLogService = mock(ToolInvocationLogService.class);
        toolInvocationLogEventListener = new ToolInvocationLogEventListener(toolInvocationLogService);
    }

    @Test
    void testMapsEventToLogRow() {
        ToolExecutionEvent event = ToolExecutionEvent
            .builder(ToolExecutionSurface.MCP_EMBEDDED, ToolExecutionKind.COMPONENT, "slack_sendMessage")
            .tenantId("tenant1")
            .componentName("slack")
            .componentVersion(1)
            .operationName("sendMessage")
            .connectionId(7L)
            .environment(2)
            .externalUserId("user-1")
            .integrationInstanceId(9L)
            .mcpServerId(3L)
            .outcome(ToolExecutionOutcome.SUCCESS)
            .durationMs(42)
            .build();

        toolInvocationLogEventListener.onToolExecutionEvent(event);

        ArgumentCaptor<ToolInvocationLog> captor = ArgumentCaptor.forClass(ToolInvocationLog.class);

        verify(toolInvocationLogService).create(captor.capture());

        ToolInvocationLog toolInvocationLog = captor.getValue();

        assertThat(toolInvocationLog.getTenantId()).isEqualTo("tenant1");
        assertThat(toolInvocationLog.getSurface()).isEqualTo(ToolExecutionSurface.MCP_EMBEDDED);
        assertThat(toolInvocationLog.getKind()).isEqualTo(ToolExecutionKind.COMPONENT);
        assertThat(toolInvocationLog.getToolName()).isEqualTo("slack_sendMessage");
        assertThat(toolInvocationLog.getComponentName()).isEqualTo("slack");
        assertThat(toolInvocationLog.getComponentVersion()).isEqualTo(1);
        assertThat(toolInvocationLog.getOperationName()).isEqualTo("sendMessage");
        assertThat(toolInvocationLog.getConnectionId()).isEqualTo(7L);
        assertThat(toolInvocationLog.getEnvironment()).isEqualTo(2);
        assertThat(toolInvocationLog.getExternalUserId()).isEqualTo("user-1");
        assertThat(toolInvocationLog.getIntegrationInstanceId()).isEqualTo(9L);
        assertThat(toolInvocationLog.getMcpServerId()).isEqualTo(3L);
        assertThat(toolInvocationLog.getOutcome()).isEqualTo(ToolExecutionOutcome.SUCCESS);
        assertThat(toolInvocationLog.getDurationMs()).isEqualTo(42);
        assertThat(toolInvocationLog.getCreatedDate()).isNotNull();
    }

    @Test
    void testPersistenceFailureIsSwallowed() {
        when(toolInvocationLogService.create(any(ToolInvocationLog.class)))
            .thenThrow(new RuntimeException("db down"));

        ToolExecutionEvent event = ToolExecutionEvent
            .builder(ToolExecutionSurface.EMBEDDED_API_ACTION, ToolExecutionKind.COMPONENT, "slack_sendMessage")
            .tenantId("tenant1")
            .outcome(ToolExecutionOutcome.ERROR)
            .build();

        assertThatCode(() -> toolInvocationLogEventListener.onToolExecutionEvent(event))
            .doesNotThrowAnyException();
    }
}

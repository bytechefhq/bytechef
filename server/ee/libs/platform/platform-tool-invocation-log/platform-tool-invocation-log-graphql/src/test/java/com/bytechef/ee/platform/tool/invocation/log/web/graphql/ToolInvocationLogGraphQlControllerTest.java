/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLog;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogService;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ToolInvocationLogGraphQlControllerTest {

    private ToolInvocationLogGraphQlController toolInvocationLogGraphQlController;
    private ToolInvocationLogService toolInvocationLogService;

    @BeforeEach
    void beforeEach() {
        toolInvocationLogService = mock(ToolInvocationLogService.class);
        toolInvocationLogGraphQlController = new ToolInvocationLogGraphQlController(toolInvocationLogService);
    }

    @Test
    void testToolInvocationLogsMapsEnumsAndDates() {
        ToolInvocationLog toolInvocationLog = new ToolInvocationLog();

        toolInvocationLog.setId(1L);
        toolInvocationLog.setSurface(ToolExecutionSurface.MCP_EMBEDDED);
        toolInvocationLog.setKind(ToolExecutionKind.COMPONENT);
        toolInvocationLog.setToolName("slack_sendMessage");
        toolInvocationLog.setOutcome(ToolExecutionOutcome.SUCCESS);
        toolInvocationLog.setDurationMs(42);
        toolInvocationLog.setCreatedDate(Instant.ofEpochMilli(1_000L));

        when(
            toolInvocationLogService.getToolInvocationLogs(
                eq(ToolExecutionSurface.MCP_EMBEDDED), eq(ToolExecutionOutcome.SUCCESS), eq(3L), isNull(), isNull(),
                isNull(), isNull(), eq(0)))
                    .thenReturn(new PageImpl<>(List.of(toolInvocationLog), PageRequest.of(0, 50), 1));

        ToolInvocationLogGraphQlController.ToolInvocationLogPage page =
            toolInvocationLogGraphQlController.toolInvocationLogs(
                "MCP_EMBEDDED", "SUCCESS", 3L, null, null, null, null, 0);

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).hasSize(1);

        ToolInvocationLogGraphQlController.ToolInvocationLogItem item = page.content()
            .getFirst();

        assertThat(item.surface()).isEqualTo("MCP_EMBEDDED");
        assertThat(item.kind()).isEqualTo("COMPONENT");
        assertThat(item.outcome()).isEqualTo("SUCCESS");
        assertThat(item.toolName()).isEqualTo("slack_sendMessage");
        assertThat(item.durationMs()).isEqualTo(42);
        assertThat(item.createdDate()).isEqualTo(1_000L);
    }

    @Test
    void testToolInvocationLogsPassesNullFilters() {
        when(
            toolInvocationLogService.getToolInvocationLogs(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(0)))
                    .thenReturn(new PageImpl<>(List.of()));

        toolInvocationLogGraphQlController.toolInvocationLogs(null, null, null, null, null, null, null, null);

        verify(toolInvocationLogService).getToolInvocationLogs(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(0));
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.tool.invocation.log.config.ToolInvocationLogIntTestConfiguration;
import com.bytechef.ee.platform.tool.invocation.log.repository.ToolInvocationLogRepository;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ToolInvocationLogIntTestConfiguration.class)
class ToolInvocationLogServiceIntTest {

    @Autowired
    private ToolInvocationLogRepository toolInvocationLogRepository;

    @Autowired
    private ToolInvocationLogService toolInvocationLogService;

    @AfterEach
    void afterEach() {
        toolInvocationLogRepository.deleteAll();
    }

    @Test
    void testCreateAndGetFilteredBySurface() {
        create(ToolExecutionSurface.MCP_AUTOMATION, ToolExecutionOutcome.SUCCESS, 10L, Instant.now());
        create(ToolExecutionSurface.MCP_EMBEDDED, ToolExecutionOutcome.ERROR, 20L, Instant.now());

        Page<ToolInvocationLog> page = toolInvocationLogService.getToolInvocationLogs(
            ToolExecutionSurface.MCP_EMBEDDED, null, null, null, null, null, null, 0);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()
            .getFirst()
            .getSurface()).isEqualTo(ToolExecutionSurface.MCP_EMBEDDED);
        assertThat(page.getContent()
            .getFirst()
            .getMcpServerId()).isEqualTo(20L);
    }

    @Test
    void testGetFilteredByOutcomeAndMcpServer() {
        create(ToolExecutionSurface.MCP_AUTOMATION, ToolExecutionOutcome.SUCCESS, 10L, Instant.now());
        create(ToolExecutionSurface.MCP_AUTOMATION, ToolExecutionOutcome.ERROR, 10L, Instant.now());
        create(ToolExecutionSurface.MCP_AUTOMATION, ToolExecutionOutcome.ERROR, 99L, Instant.now());

        Page<ToolInvocationLog> page = toolInvocationLogService.getToolInvocationLogs(
            null, ToolExecutionOutcome.ERROR, 10L, null, null, null, null, 0);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()
            .getFirst()
            .getOutcome()).isEqualTo(ToolExecutionOutcome.ERROR);
    }

    @Test
    void testRetentionDeletesOldRows() {
        create(ToolExecutionSurface.MCP_AUTOMATION, ToolExecutionOutcome.SUCCESS, 1L,
            Instant.now()
                .minus(120, ChronoUnit.DAYS));
        create(ToolExecutionSurface.MCP_AUTOMATION, ToolExecutionOutcome.SUCCESS, 2L, Instant.now());

        int deleted = toolInvocationLogService.deleteToolInvocationLogsCreatedBefore(
            Instant.now()
                .minus(90, ChronoUnit.DAYS));

        assertThat(deleted).isEqualTo(1);
        assertThat(toolInvocationLogRepository.count()).isEqualTo(1);
    }

    private void create(
        ToolExecutionSurface surface, ToolExecutionOutcome outcome, long mcpServerId, Instant createdDate) {

        ToolInvocationLog toolInvocationLog = new ToolInvocationLog();

        toolInvocationLog.setTenantId("public");
        toolInvocationLog.setSurface(surface);
        toolInvocationLog.setKind(ToolExecutionKind.COMPONENT);
        toolInvocationLog.setToolName("slack_sendMessage");
        toolInvocationLog.setMcpServerId(mcpServerId);
        toolInvocationLog.setOutcome(outcome);
        toolInvocationLog.setDurationMs(5);
        toolInvocationLog.setCreatedDate(createdDate);

        toolInvocationLogService.create(toolInvocationLog);
    }
}

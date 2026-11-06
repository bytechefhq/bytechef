/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.external.invocation.log;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.external.invocation.log.config.ExternalInvocationLogIntTestConfiguration;
import com.bytechef.ee.platform.external.invocation.log.domain.ExternalInvocationLog;
import com.bytechef.ee.platform.external.invocation.log.repository.ExternalInvocationLogRepository;
import com.bytechef.ee.platform.external.invocation.log.service.ExternalInvocationLogService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationKind;
import com.bytechef.platform.external.invocation.ExternalInvocationOutcome;
import com.bytechef.platform.external.invocation.ExternalInvocationSurface;
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
@SpringBootTest(classes = ExternalInvocationLogIntTestConfiguration.class)
class ExternalInvocationLogServiceIntTest {

    @Autowired
    private ExternalInvocationLogRepository externalInvocationLogRepository;

    @Autowired
    private ExternalInvocationLogService externalInvocationLogService;

    @AfterEach
    void afterEach() {
        externalInvocationLogRepository.deleteAll();
    }

    @Test
    void testCreateAndGetFilteredBySurface() {
        create(ExternalInvocationSurface.MCP_AUTOMATION, ExternalInvocationOutcome.SUCCESS, 10L, Instant.now());
        create(ExternalInvocationSurface.MCP_EMBEDDED, ExternalInvocationOutcome.ERROR, 20L, Instant.now());

        Page<ExternalInvocationLog> page = externalInvocationLogService.getExternalInvocationLogs(
            PlatformType.EMBEDDED, null, ExternalInvocationSurface.MCP_EMBEDDED, null, null, null, null, null, null,
            null,
            0);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()
            .getFirst()
            .getSurface()).isEqualTo(ExternalInvocationSurface.MCP_EMBEDDED);
        assertThat(page.getContent()
            .getFirst()
            .getMcpServerId()).isEqualTo(20L);
    }

    @Test
    void testGetFilteredByOutcomeAndMcpServer() {
        create(ExternalInvocationSurface.MCP_AUTOMATION, ExternalInvocationOutcome.SUCCESS, 10L, Instant.now());
        create(ExternalInvocationSurface.MCP_AUTOMATION, ExternalInvocationOutcome.ERROR, 10L, Instant.now());
        create(ExternalInvocationSurface.MCP_AUTOMATION, ExternalInvocationOutcome.ERROR, 99L, Instant.now());

        Page<ExternalInvocationLog> page = externalInvocationLogService.getExternalInvocationLogs(
            PlatformType.AUTOMATION, null, null, ExternalInvocationOutcome.ERROR, 10L, null, null, null, null, null, 0);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()
            .getFirst()
            .getOutcome()).isEqualTo(ExternalInvocationOutcome.ERROR);
    }

    @Test
    void testGetFilteredByA2aServer() {
        createA2aSkillCall(5L);
        createA2aSkillCall(6L);
        create(ExternalInvocationSurface.MCP_AUTOMATION, ExternalInvocationOutcome.SUCCESS, 5L, Instant.now());

        Page<ExternalInvocationLog> page = externalInvocationLogService.getExternalInvocationLogs(
            PlatformType.AUTOMATION, null, null, null, null, 5L, null, null, null, null, 0);

        assertThat(page.getContent()).singleElement()
            .satisfies(externalInvocationLog -> {
                assertThat(externalInvocationLog.getSurface()).isEqualTo(ExternalInvocationSurface.A2A);
                assertThat(externalInvocationLog.getA2aServerId()).isEqualTo(5L);
                assertThat(externalInvocationLog.getMcpServerId()).isNull();
            });
    }

    @Test
    void testGetFilteredByEnvironmentExcludesRowsOfOtherEnvironments() {
        createInEnvironment(ExternalInvocationSurface.MCP_EMBEDDED, 2);
        createInEnvironment(ExternalInvocationSurface.EMBEDDED_API_ACTION, 2);
        createInEnvironment(ExternalInvocationSurface.MCP_EMBEDDED, 0);
        create(ExternalInvocationSurface.MCP_EMBEDDED, ExternalInvocationOutcome.SUCCESS, 10L, Instant.now());

        Page<ExternalInvocationLog> page = externalInvocationLogService.getExternalInvocationLogs(
            PlatformType.EMBEDDED, 2, null, null, null, null, null, null, null, null, 0);

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(ExternalInvocationLog::getEnvironment)
            .containsOnly(2);

        Page<ExternalInvocationLog> unfilteredPage = externalInvocationLogService.getExternalInvocationLogs(
            PlatformType.EMBEDDED, null, null, null, null, null, null, null, null, null, 0);

        assertThat(unfilteredPage.getTotalElements()).isEqualTo(4);
    }

    @Test
    void testWorkspaceQueryReturnsOnlyThatWorkspacesRowsInThatEnvironment() {
        createInWorkspace(7L, 2, 5L);
        createInWorkspace(7L, 0, 5L);
        createInWorkspace(8L, 2, 6L);
        create(ExternalInvocationSurface.MCP_EMBEDDED, ExternalInvocationOutcome.SUCCESS, 5L, Instant.now());

        Page<ExternalInvocationLog> page = externalInvocationLogService.getWorkspaceExternalInvocationLogs(
            PlatformType.AUTOMATION, 7L, 2, null, null, null, null, null, null, null, null, 0);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(ExternalInvocationLog::getWorkspaceId)
            .containsOnly(7L);
        assertThat(page.getContent()).extracting(ExternalInvocationLog::getEnvironment)
            .containsOnly(2);
    }

    @Test
    void testWorkspaceQueryNamingAnotherWorkspacesMcpServerMatchesNothing() {
        createInWorkspace(8L, 2, 6L);

        Page<ExternalInvocationLog> page = externalInvocationLogService.getWorkspaceExternalInvocationLogs(
            PlatformType.AUTOMATION, 7L, 2, null, null, 6L, null, null, null, null, null, 0);

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void testTenantWideQueryForAutomationExcludesEmbeddedRows() {
        create(ExternalInvocationSurface.MCP_AUTOMATION, ExternalInvocationOutcome.SUCCESS, 10L, Instant.now());
        create(ExternalInvocationSurface.MCP_EMBEDDED, ExternalInvocationOutcome.SUCCESS, 20L, Instant.now());

        Page<ExternalInvocationLog> page = externalInvocationLogService.getExternalInvocationLogs(
            PlatformType.AUTOMATION, null, null, null, null, null, null, null, null, null, 0);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(ExternalInvocationLog::getType)
            .containsExactly(PlatformType.AUTOMATION);
        assertThat(page.getContent()).extracting(ExternalInvocationLog::getMcpServerId)
            .containsExactly(10L);
    }

    @Test
    void testWorkspaceQueryForAutomationExcludesEmbeddedRowsOfTheSameWorkspace() {
        createInWorkspace(7L, 2, 5L);

        ExternalInvocationLog embeddedExternalInvocationLog = new ExternalInvocationLog();

        embeddedExternalInvocationLog.setType(PlatformType.EMBEDDED);
        embeddedExternalInvocationLog.setSurface(ExternalInvocationSurface.MCP_EMBEDDED);
        embeddedExternalInvocationLog.setKind(ExternalInvocationKind.COMPONENT);
        embeddedExternalInvocationLog.setWorkspaceId(7L);
        embeddedExternalInvocationLog.setEnvironment(2);
        embeddedExternalInvocationLog.setMcpServerId(6L);
        embeddedExternalInvocationLog.setOutcome(ExternalInvocationOutcome.SUCCESS);
        embeddedExternalInvocationLog.setCreatedDate(Instant.now());

        externalInvocationLogService.create(embeddedExternalInvocationLog);

        Page<ExternalInvocationLog> page = externalInvocationLogService.getWorkspaceExternalInvocationLogs(
            PlatformType.AUTOMATION, 7L, 2, null, null, null, null, null, null, null, null, 0);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(ExternalInvocationLog::getMcpServerId)
            .containsExactly(5L);
    }

    @Test
    void testRetentionDeletesOldRows() {
        create(ExternalInvocationSurface.MCP_AUTOMATION, ExternalInvocationOutcome.SUCCESS, 1L,
            Instant.now()
                .minus(120, ChronoUnit.DAYS));
        create(ExternalInvocationSurface.MCP_AUTOMATION, ExternalInvocationOutcome.SUCCESS, 2L, Instant.now());

        int deleted = externalInvocationLogService.deleteExternalInvocationLogsCreatedBefore(
            Instant.now()
                .minus(90, ChronoUnit.DAYS));

        assertThat(deleted).isEqualTo(1);
        assertThat(externalInvocationLogRepository.count()).isEqualTo(1);
    }

    private void createInEnvironment(ExternalInvocationSurface surface, int environment) {
        ExternalInvocationLog externalInvocationLog = new ExternalInvocationLog();

        externalInvocationLog.setType(PlatformType.EMBEDDED);
        externalInvocationLog.setSurface(surface);
        externalInvocationLog.setKind(ExternalInvocationKind.COMPONENT);
        externalInvocationLog.setToolName("slack_sendMessage");
        externalInvocationLog.setEnvironment(environment);
        externalInvocationLog.setOutcome(ExternalInvocationOutcome.SUCCESS);
        externalInvocationLog.setDurationMs(5);
        externalInvocationLog.setCreatedDate(Instant.now());

        externalInvocationLogService.create(externalInvocationLog);
    }

    private void createA2aSkillCall(long a2aServerId) {
        ExternalInvocationLog externalInvocationLog = new ExternalInvocationLog();

        externalInvocationLog.setType(PlatformType.AUTOMATION);
        externalInvocationLog.setSurface(ExternalInvocationSurface.A2A);
        externalInvocationLog.setKind(ExternalInvocationKind.WORKFLOW);
        externalInvocationLog.setToolName("Summarize");
        externalInvocationLog.setA2aServerId(a2aServerId);
        externalInvocationLog.setOutcome(ExternalInvocationOutcome.SUCCESS);
        externalInvocationLog.setDurationMs(5);
        externalInvocationLog.setCreatedDate(Instant.now());

        externalInvocationLogService.create(externalInvocationLog);
    }

    private void createInWorkspace(long workspaceId, int environment, long mcpServerId) {
        ExternalInvocationLog externalInvocationLog = new ExternalInvocationLog();

        externalInvocationLog.setType(PlatformType.AUTOMATION);
        externalInvocationLog.setSurface(ExternalInvocationSurface.MCP_AUTOMATION);
        externalInvocationLog.setKind(ExternalInvocationKind.COMPONENT);
        externalInvocationLog.setToolName("slack_sendMessage");
        externalInvocationLog.setWorkspaceId(workspaceId);
        externalInvocationLog.setEnvironment(environment);
        externalInvocationLog.setMcpServerId(mcpServerId);
        externalInvocationLog.setOutcome(ExternalInvocationOutcome.SUCCESS);
        externalInvocationLog.setDurationMs(5);
        externalInvocationLog.setCreatedDate(Instant.now());

        externalInvocationLogService.create(externalInvocationLog);
    }

    private void create(
        ExternalInvocationSurface surface, ExternalInvocationOutcome outcome, long mcpServerId, Instant createdDate) {

        ExternalInvocationLog externalInvocationLog = new ExternalInvocationLog();

        externalInvocationLog.setType(
            surface == ExternalInvocationSurface.MCP_AUTOMATION ? PlatformType.AUTOMATION : PlatformType.EMBEDDED);
        externalInvocationLog.setSurface(surface);
        externalInvocationLog.setKind(ExternalInvocationKind.COMPONENT);
        externalInvocationLog.setToolName("slack_sendMessage");
        externalInvocationLog.setMcpServerId(mcpServerId);
        externalInvocationLog.setOutcome(outcome);
        externalInvocationLog.setDurationMs(5);
        externalInvocationLog.setCreatedDate(createdDate);

        externalInvocationLogService.create(externalInvocationLog);
    }
}

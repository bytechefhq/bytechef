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
import com.bytechef.platform.external.invocation.ExternalInvocationSource;
import com.bytechef.platform.external.invocation.ExternalInvocationStatus;
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
    void testCreateAndGetFilteredBySource() {
        create(PlatformType.AUTOMATION, ExternalInvocationStatus.SUCCESS, 10L, Instant.now());
        create(PlatformType.EMBEDDED, ExternalInvocationStatus.ERROR, 20L, Instant.now());

        Page<ExternalInvocationLog> page = externalInvocationLogService.getExternalInvocationLogs(
            PlatformType.EMBEDDED, null, ExternalInvocationSource.MCP_SERVER, null, null, null, null, null, null,
            null,
            0);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()
            .getFirst()
            .getSource()).isEqualTo(ExternalInvocationSource.MCP_SERVER);
        assertThat(page.getContent()
            .getFirst()
            .getMcpServerId()).isEqualTo(20L);
    }

    @Test
    void testGetFilteredByStatusAndMcpServer() {
        create(PlatformType.AUTOMATION, ExternalInvocationStatus.SUCCESS, 10L, Instant.now());
        create(PlatformType.AUTOMATION, ExternalInvocationStatus.ERROR, 10L, Instant.now());
        create(PlatformType.AUTOMATION, ExternalInvocationStatus.ERROR, 99L, Instant.now());

        Page<ExternalInvocationLog> page = externalInvocationLogService.getExternalInvocationLogs(
            PlatformType.AUTOMATION, null, null, ExternalInvocationStatus.ERROR, 10L, null, null, null, null, null, 0);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()
            .getFirst()
            .getStatus()).isEqualTo(ExternalInvocationStatus.ERROR);
    }

    @Test
    void testGetFilteredByA2aServer() {
        createA2aSkillCall(5L);
        createA2aSkillCall(6L);
        create(PlatformType.AUTOMATION, ExternalInvocationStatus.SUCCESS, 5L, Instant.now());

        Page<ExternalInvocationLog> page = externalInvocationLogService.getExternalInvocationLogs(
            PlatformType.AUTOMATION, null, null, null, null, 5L, null, null, null, null, 0);

        assertThat(page.getContent()).singleElement()
            .satisfies(externalInvocationLog -> {
                assertThat(externalInvocationLog.getSource()).isEqualTo(ExternalInvocationSource.A2A_SERVER);
                assertThat(externalInvocationLog.getA2aServerId()).isEqualTo(5L);
                assertThat(externalInvocationLog.getMcpServerId()).isNull();
            });
    }

    @Test
    void testGetFilteredByEnvironmentExcludesRowsOfOtherEnvironments() {
        createInEnvironment(ExternalInvocationSource.MCP_SERVER, 2);
        createInEnvironment(ExternalInvocationSource.API_ACTION, 2);
        createInEnvironment(ExternalInvocationSource.MCP_SERVER, 0);
        create(PlatformType.EMBEDDED, ExternalInvocationStatus.SUCCESS, 10L, Instant.now());

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
        create(PlatformType.EMBEDDED, ExternalInvocationStatus.SUCCESS, 5L, Instant.now());

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
        create(PlatformType.AUTOMATION, ExternalInvocationStatus.SUCCESS, 10L, Instant.now());
        create(PlatformType.EMBEDDED, ExternalInvocationStatus.SUCCESS, 20L, Instant.now());

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
        embeddedExternalInvocationLog.setSource(ExternalInvocationSource.MCP_SERVER);
        embeddedExternalInvocationLog.setKind(ExternalInvocationKind.COMPONENT);
        embeddedExternalInvocationLog.setWorkspaceId(7L);
        embeddedExternalInvocationLog.setEnvironment(2);
        embeddedExternalInvocationLog.setMcpServerId(6L);
        embeddedExternalInvocationLog.setStatus(ExternalInvocationStatus.SUCCESS);
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
        create(PlatformType.AUTOMATION, ExternalInvocationStatus.SUCCESS, 1L,
            Instant.now()
                .minus(120, ChronoUnit.DAYS));
        create(PlatformType.AUTOMATION, ExternalInvocationStatus.SUCCESS, 2L, Instant.now());

        int deleted = externalInvocationLogService.deleteExternalInvocationLogsCreatedBefore(
            Instant.now()
                .minus(90, ChronoUnit.DAYS));

        assertThat(deleted).isEqualTo(1);
        assertThat(externalInvocationLogRepository.count()).isEqualTo(1);
    }

    @Test
    void testHasExternalInvocationLogMatchesTypeEnvironmentAndStatus() {
        createWithEnvironment(PlatformType.EMBEDDED, 0, ExternalInvocationStatus.ERROR, Instant.now());
        createWithEnvironment(PlatformType.AUTOMATION, 0, ExternalInvocationStatus.SUCCESS, Instant.now());
        createWithEnvironment(PlatformType.EMBEDDED, 2, ExternalInvocationStatus.SUCCESS, Instant.now());

        assertThat(
            externalInvocationLogService.hasExternalInvocationLog(
                PlatformType.EMBEDDED, 0, ExternalInvocationStatus.SUCCESS)).isFalse();
        assertThat(
            externalInvocationLogService.hasExternalInvocationLog(
                PlatformType.EMBEDDED, 2, ExternalInvocationStatus.SUCCESS)).isTrue();
    }

    @Test
    void testFetchLastFailedExternalInvocationLogReturnsLatestFailureInEnvironment() {
        Instant now = Instant.now();

        createWithEnvironment(
            PlatformType.EMBEDDED, 0, ExternalInvocationStatus.ERROR, now.minus(2, ChronoUnit.MINUTES));
        createWithEnvironment(
            PlatformType.EMBEDDED, 0, ExternalInvocationStatus.CONNECTION_REQUIRED, now.minus(1, ChronoUnit.MINUTES));
        createWithEnvironment(PlatformType.EMBEDDED, 0, ExternalInvocationStatus.SUCCESS, now);
        createWithEnvironment(PlatformType.EMBEDDED, 0, ExternalInvocationStatus.APPROVAL_REQUIRED, now);
        createWithEnvironment(PlatformType.EMBEDDED, 0, ExternalInvocationStatus.SUSPENDED, now);
        createWithEnvironment(PlatformType.AUTOMATION, 0, ExternalInvocationStatus.TIMEOUT, now);
        createWithEnvironment(PlatformType.EMBEDDED, 2, ExternalInvocationStatus.ERROR, now);

        assertThat(externalInvocationLogService.fetchLastFailedExternalInvocationLog(PlatformType.EMBEDDED, 0))
            .get()
            .extracting(ExternalInvocationLog::getStatus)
            .isEqualTo(ExternalInvocationStatus.CONNECTION_REQUIRED);
        assertThat(externalInvocationLogService.fetchLastFailedExternalInvocationLog(PlatformType.EMBEDDED, 1))
            .isEmpty();
    }

    private void createWithEnvironment(
        PlatformType type, int environment, ExternalInvocationStatus status, Instant createdDate) {

        ExternalInvocationLog externalInvocationLog = new ExternalInvocationLog();

        externalInvocationLog.setType(type);
        externalInvocationLog.setSource(ExternalInvocationSource.MCP_SERVER);
        externalInvocationLog.setKind(ExternalInvocationKind.COMPONENT);
        externalInvocationLog.setInvocationName("slack_sendMessage");
        externalInvocationLog.setEnvironment(environment);
        externalInvocationLog.setStatus(status);
        externalInvocationLog.setDurationMs(5);
        externalInvocationLog.setCreatedDate(createdDate);

        externalInvocationLogService.create(externalInvocationLog);
    }

    private void createInEnvironment(ExternalInvocationSource source, int environment) {
        ExternalInvocationLog externalInvocationLog = new ExternalInvocationLog();

        externalInvocationLog.setType(PlatformType.EMBEDDED);
        externalInvocationLog.setSource(source);
        externalInvocationLog.setKind(ExternalInvocationKind.COMPONENT);
        externalInvocationLog.setInvocationName("slack_sendMessage");
        externalInvocationLog.setEnvironment(environment);
        externalInvocationLog.setStatus(ExternalInvocationStatus.SUCCESS);
        externalInvocationLog.setDurationMs(5);
        externalInvocationLog.setCreatedDate(Instant.now());

        externalInvocationLogService.create(externalInvocationLog);
    }

    private void createA2aSkillCall(long a2aServerId) {
        ExternalInvocationLog externalInvocationLog = new ExternalInvocationLog();

        externalInvocationLog.setType(PlatformType.AUTOMATION);
        externalInvocationLog.setSource(ExternalInvocationSource.A2A_SERVER);
        externalInvocationLog.setKind(ExternalInvocationKind.WORKFLOW);
        externalInvocationLog.setInvocationName("Summarize");
        externalInvocationLog.setA2aServerId(a2aServerId);
        externalInvocationLog.setStatus(ExternalInvocationStatus.SUCCESS);
        externalInvocationLog.setDurationMs(5);
        externalInvocationLog.setCreatedDate(Instant.now());

        externalInvocationLogService.create(externalInvocationLog);
    }

    private void createInWorkspace(long workspaceId, int environment, long mcpServerId) {
        ExternalInvocationLog externalInvocationLog = new ExternalInvocationLog();

        externalInvocationLog.setType(PlatformType.AUTOMATION);
        externalInvocationLog.setSource(ExternalInvocationSource.MCP_SERVER);
        externalInvocationLog.setKind(ExternalInvocationKind.COMPONENT);
        externalInvocationLog.setInvocationName("slack_sendMessage");
        externalInvocationLog.setWorkspaceId(workspaceId);
        externalInvocationLog.setEnvironment(environment);
        externalInvocationLog.setMcpServerId(mcpServerId);
        externalInvocationLog.setStatus(ExternalInvocationStatus.SUCCESS);
        externalInvocationLog.setDurationMs(5);
        externalInvocationLog.setCreatedDate(Instant.now());

        externalInvocationLogService.create(externalInvocationLog);
    }

    private void create(
        PlatformType type, ExternalInvocationStatus status, long mcpServerId, Instant createdDate) {

        ExternalInvocationLog externalInvocationLog = new ExternalInvocationLog();

        externalInvocationLog.setType(type);
        externalInvocationLog.setSource(ExternalInvocationSource.MCP_SERVER);
        externalInvocationLog.setKind(ExternalInvocationKind.COMPONENT);
        externalInvocationLog.setInvocationName("slack_sendMessage");
        externalInvocationLog.setMcpServerId(mcpServerId);
        externalInvocationLog.setStatus(status);
        externalInvocationLog.setDurationMs(5);
        externalInvocationLog.setCreatedDate(createdDate);

        externalInvocationLogService.create(externalInvocationLog);
    }
}

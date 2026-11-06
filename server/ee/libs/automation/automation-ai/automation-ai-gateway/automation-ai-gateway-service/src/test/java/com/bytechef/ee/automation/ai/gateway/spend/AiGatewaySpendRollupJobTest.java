/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.spend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewaySpendService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewaySpendSummary;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewaySpendService;
import com.bytechef.ee.platform.ai.llm.usage.AiLlmUsage;
import com.bytechef.ee.platform.ai.llm.usage.LlmUsageSource;
import com.bytechef.ee.platform.ai.llm.usage.service.AiLlmUsageService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiGatewaySpendRollupJobTest {

    private final Instant periodStart = Instant.parse("2026-07-20T10:00:00Z");
    private final Instant periodEnd = periodStart.plus(1, ChronoUnit.HOURS);

    @Mock
    private AiGatewaySpendService aiGatewaySpendService;

    @Mock
    private AiLlmUsageService aiLlmUsageService;

    @Mock
    private WorkspaceAiGatewaySpendService workspaceAiGatewaySpendService;

    private AiGatewaySpendRollupJob job;

    @BeforeEach
    void beforeEach() {
        job = new AiGatewaySpendRollupJob(aiGatewaySpendService, aiLlmUsageService, workspaceAiGatewaySpendService);

        when(aiLlmUsageService.findDistinctWorkspaceIds()).thenReturn(List.of(1L));
        when(workspaceAiGatewaySpendService.getSpendSummariesByWorkspaceId(anyLong(), any(), any()))
            .thenReturn(List.of());
        when(aiGatewaySpendService.getSpendSummariesWithoutWorkspace(any(), any())).thenReturn(List.of());
    }

    @Test
    void testRollupWritesWorkspaceLessConnectedUserUsageIntoAWorkspaceLessSummary() {
        AiLlmUsage connectedUserUsage = usage("gpt-4o", "openai", new BigDecimal("0.04"), 100, 20);

        connectedUserUsage.setUserId(5L);

        when(aiLlmUsageService.getConnectedUserRequestLogsWithoutWorkspace(periodStart, periodEnd))
            .thenReturn(List.of(connectedUserUsage));

        job.rollUp(periodStart, periodEnd);

        ArgumentCaptor<AiGatewaySpendSummary> summaryCaptor = ArgumentCaptor.forClass(AiGatewaySpendSummary.class);

        verify(aiGatewaySpendService).create(summaryCaptor.capture());

        AiGatewaySpendSummary summary = summaryCaptor.getValue();

        assertThat(summary.getConnectedUserId()).isEqualTo(5L);
        assertThat(summary.getWorkspaceId()).isNull();
        assertThat(summary.getTotalCost()).isEqualByComparingTo(new BigDecimal("0.04"));

        verify(workspaceAiGatewaySpendService, never()).createInWorkspace(any(), anyLong());
    }

    @Test
    void testWorkspaceLessRollupSkipsAnAlreadyRolledUpHour() {
        AiLlmUsage connectedUserUsage = usage("gpt-4o", "openai", new BigDecimal("0.04"), 100, 20);

        connectedUserUsage.setUserId(5L);

        when(aiLlmUsageService.getConnectedUserRequestLogsWithoutWorkspace(periodStart, periodEnd))
            .thenReturn(List.of(connectedUserUsage));
        when(aiGatewaySpendService.getSpendSummariesWithoutWorkspace(periodStart, periodEnd))
            .thenReturn(List.of(new AiGatewaySpendSummary(periodStart, periodEnd)));

        job.rollUp(periodStart, periodEnd);

        verify(aiGatewaySpendService, never()).create(any());
    }

    @Test
    void testWorkspaceLessRollupDropsRowsThatAreNotGatewayConnectedUserUsage() {
        AiLlmUsage aiHubUsage = usage("gpt-4o", "openai", new BigDecimal("0.04"), 100, 20);

        aiHubUsage.setSource(LlmUsageSource.AI_HUB);
        aiHubUsage.setUserId(5L);

        when(aiLlmUsageService.getConnectedUserRequestLogsWithoutWorkspace(periodStart, periodEnd))
            .thenReturn(List.of(aiHubUsage));

        job.rollUp(periodStart, periodEnd);

        verify(aiGatewaySpendService, never()).create(any());
    }

    @Test
    void testWorkspaceRollupFailureDoesNotStopTheWorkspaceLessRollup() {
        AiLlmUsage connectedUserUsage = usage("gpt-4o", "openai", new BigDecimal("0.04"), 100, 20);

        connectedUserUsage.setUserId(5L);

        when(aiLlmUsageService.getRequestLogsByWorkspace(1L, periodStart, periodEnd))
            .thenThrow(new IllegalStateException("workspace 1 is broken"));
        when(aiLlmUsageService.getConnectedUserRequestLogsWithoutWorkspace(periodStart, periodEnd))
            .thenReturn(List.of(connectedUserUsage));

        job.rollUp(periodStart, periodEnd);

        verify(aiGatewaySpendService).create(any());
    }

    @Test
    void testAggregatesUsageRowsPerModelGroup() {
        AiLlmUsage firstUsage = usage("gpt-4o", "openai", new BigDecimal("0.01"), 100, 20);
        AiLlmUsage secondUsage = usage("gpt-4o", "openai", new BigDecimal("0.02"), 200, 40);
        AiLlmUsage otherModelUsage = usage("claude-sonnet-5", "anthropic", new BigDecimal("0.05"), 50, 10);

        when(aiLlmUsageService.getRequestLogsByWorkspace(1L, periodStart, periodEnd))
            .thenReturn(List.of(firstUsage, secondUsage, otherModelUsage));

        job.rollUp(periodStart, periodEnd);

        ArgumentCaptor<AiGatewaySpendSummary> summaryCaptor = ArgumentCaptor.forClass(AiGatewaySpendSummary.class);

        verify(workspaceAiGatewaySpendService, org.mockito.Mockito.times(2))
            .createInWorkspace(summaryCaptor.capture(), eq(1L));

        List<AiGatewaySpendSummary> summaries = summaryCaptor.getAllValues();

        AiGatewaySpendSummary gptSummary = summaries.stream()
            .filter(summary -> "gpt-4o".equals(summary.getModel()))
            .findFirst()
            .orElseThrow();

        assertThat(gptSummary.getRequestCount()).isEqualTo(2);
        assertThat(gptSummary.getTotalInputTokens()).isEqualTo(300);
        assertThat(gptSummary.getTotalOutputTokens()).isEqualTo(60);
        assertThat(gptSummary.getTotalCost()).isEqualByComparingTo(new BigDecimal("0.03"));
        assertThat(gptSummary.getPeriodStart()).isEqualTo(periodStart);
        assertThat(gptSummary.getPeriodEnd()).isEqualTo(periodEnd);
    }

    @Test
    void testAlreadyRolledUpWindowIsSkipped() {
        AiGatewaySpendSummary existingSummary = new AiGatewaySpendSummary(periodStart, periodEnd);

        when(workspaceAiGatewaySpendService.getSpendSummariesByWorkspaceId(1L, periodStart, periodEnd))
            .thenReturn(List.of(existingSummary));

        job.rollUp(periodStart, periodEnd);

        verify(workspaceAiGatewaySpendService, never()).createInWorkspace(any(), anyLong());
    }

    @Test
    void testEmptyWindowWritesNothing() {
        when(aiLlmUsageService.getRequestLogsByWorkspace(1L, periodStart, periodEnd)).thenReturn(List.of());

        job.rollUp(periodStart, periodEnd);

        verify(workspaceAiGatewaySpendService, never()).createInWorkspace(any(), anyLong());
    }

    @Test
    void testRollupAttributesSpendToTheResolvedConnectedUser() {
        AiLlmUsage connectedUserUsage = usage("gpt-4o", "openai", new BigDecimal("0.01"), 100, 20);

        connectedUserUsage.setUserId(5L);

        when(aiLlmUsageService.getRequestLogsByWorkspace(1L, periodStart, periodEnd))
            .thenReturn(List.of(connectedUserUsage));

        job.rollUp(periodStart, periodEnd);

        ArgumentCaptor<AiGatewaySpendSummary> summaryCaptor = ArgumentCaptor.forClass(AiGatewaySpendSummary.class);

        verify(workspaceAiGatewaySpendService).createInWorkspace(summaryCaptor.capture(), eq(1L));

        assertThat(summaryCaptor.getValue()
            .getConnectedUserId()).isEqualTo(5L);
    }

    @Test
    void testRollupDoesNotDeriveTheConnectedUserFromTheApiKey() {
        AiLlmUsage usage = usage("gpt-4o", "openai", new BigDecimal("0.01"), 100, 20);

        usage.setApiKeyId(99L);
        usage.setUserId(null);

        when(aiLlmUsageService.getRequestLogsByWorkspace(1L, periodStart, periodEnd))
            .thenReturn(List.of(usage));

        job.rollUp(periodStart, periodEnd);

        ArgumentCaptor<AiGatewaySpendSummary> summaryCaptor = ArgumentCaptor.forClass(AiGatewaySpendSummary.class);

        verify(workspaceAiGatewaySpendService).createInWorkspace(summaryCaptor.capture(), eq(1L));

        assertThat(summaryCaptor.getValue()
            .getConnectedUserId()).isNull();
    }

    @Test
    void testRollupDoesNotAttributeAiHubUsageToAConnectedUser() {
        AiLlmUsage aiHubUsage = usage("gpt-4o", "openai", new BigDecimal("0.01"), 100, 20);

        aiHubUsage.setSource(LlmUsageSource.AI_HUB);
        aiHubUsage.setUserId(5L);

        when(aiLlmUsageService.getRequestLogsByWorkspace(1L, periodStart, periodEnd))
            .thenReturn(List.of(aiHubUsage));

        job.rollUp(periodStart, periodEnd);

        ArgumentCaptor<AiGatewaySpendSummary> summaryCaptor = ArgumentCaptor.forClass(AiGatewaySpendSummary.class);

        verify(workspaceAiGatewaySpendService).createInWorkspace(summaryCaptor.capture(), eq(1L));

        assertThat(summaryCaptor.getValue()
            .getConnectedUserId()).isNull();
    }

    @Test
    void testRollupAttributesSpendForAGatewaySourcedUsageRow() {
        AiLlmUsage gatewaySourcedUsage = AiLlmUsage.forSuccess(
            "req-gateway-sourced", "gpt-4o", "gpt-4o", "openai", 200, 100, 20, 500, new BigDecimal("0.01"));

        gatewaySourcedUsage.setUserId(5L);

        when(aiLlmUsageService.getRequestLogsByWorkspace(1L, periodStart, periodEnd))
            .thenReturn(List.of(gatewaySourcedUsage));

        job.rollUp(periodStart, periodEnd);

        ArgumentCaptor<AiGatewaySpendSummary> summaryCaptor = ArgumentCaptor.forClass(AiGatewaySpendSummary.class);

        verify(workspaceAiGatewaySpendService).createInWorkspace(summaryCaptor.capture(), eq(1L));

        assertThat(summaryCaptor.getValue()
            .getConnectedUserId()).isEqualTo(5L);
    }

    @Test
    void testRollupSplitsSpendByConnectedUserWithinTheSameApiKey() {
        AiLlmUsage firstCustomerUsage = usage("gpt-4o", "openai", new BigDecimal("0.01"), 100, 20);
        AiLlmUsage secondCustomerUsage = usage("gpt-4o", "openai", new BigDecimal("0.02"), 200, 40);

        firstCustomerUsage.setApiKeyId(1L);
        firstCustomerUsage.setUserId(5L);
        secondCustomerUsage.setApiKeyId(1L);
        secondCustomerUsage.setUserId(6L);

        when(aiLlmUsageService.getRequestLogsByWorkspace(1L, periodStart, periodEnd))
            .thenReturn(List.of(firstCustomerUsage, secondCustomerUsage));

        job.rollUp(periodStart, periodEnd);

        ArgumentCaptor<AiGatewaySpendSummary> summaryCaptor = ArgumentCaptor.forClass(AiGatewaySpendSummary.class);

        verify(workspaceAiGatewaySpendService, org.mockito.Mockito.times(2))
            .createInWorkspace(summaryCaptor.capture(), eq(1L));

        List<Long> connectedUserIds = summaryCaptor.getAllValues()
            .stream()
            .map(AiGatewaySpendSummary::getConnectedUserId)
            .toList();

        assertThat(connectedUserIds).containsExactlyInAnyOrder(5L, 6L);
    }

    private static AiLlmUsage usage(
        String model, String provider, BigDecimal cost, int inputTokens, int outputTokens) {

        AiLlmUsage aiLlmUsage = new AiLlmUsage("req-" + model + "-" + cost, model);

        aiLlmUsage.setRoutedModel(model);
        aiLlmUsage.setRoutedProvider(provider);
        aiLlmUsage.setCost(cost);
        aiLlmUsage.setInputTokens(inputTokens);
        aiLlmUsage.setOutputTokens(outputTokens);

        return aiLlmUsage;
    }
}

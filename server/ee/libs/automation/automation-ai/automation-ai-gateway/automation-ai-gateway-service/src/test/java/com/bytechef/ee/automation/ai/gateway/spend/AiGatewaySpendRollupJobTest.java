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
import com.bytechef.ee.platform.ai.llm.usage.AiLlmUsage;
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
    private AiLlmUsageService aiLlmUsageService;

    @Mock
    private WorkspaceAiGatewaySpendService workspaceAiGatewaySpendService;

    private AiGatewaySpendRollupJob job;

    @BeforeEach
    void beforeEach() {
        job = new AiGatewaySpendRollupJob(aiLlmUsageService, workspaceAiGatewaySpendService);

        when(aiLlmUsageService.findDistinctWorkspaceIds()).thenReturn(List.of(1L));
        when(workspaceAiGatewaySpendService.getSpendSummariesByWorkspaceId(anyLong(), any(), any()))
            .thenReturn(List.of());
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

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.spend;

import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewaySpendService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewaySpendSummary;
import com.bytechef.ee.platform.ai.llm.usage.AiLlmUsage;
import com.bytechef.ee.platform.ai.llm.usage.service.AiLlmUsageService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hourly rollup of raw {@code ai_llm_usage} rows into {@code ai_gateway_spend_summary} — the writer the budget checker
 * and spend dashboards read from (the read stack existed without any producer before this job). Each run aggregates the
 * previous full hour per workspace, grouped by (provider, model, apiKeyId, projectId), and writes through
 * {@link WorkspaceAiGatewaySpendService#createInWorkspace} so the mandatory workspace membership row exists.
 *
 * <p>
 * Idempotency: a workspace-hour that already has a summary row with the same period start is skipped, so restarts don't
 * double-count. In an HA deployment two nodes can race past that check between read and write; the window is one
 * scheduler tick and the duplicate would only overstate spend (never understate a budget), so a distributed lock is
 * deliberately not introduced yet.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiGatewaySpendRollupJob {

    private static final Logger log = LoggerFactory.getLogger(AiGatewaySpendRollupJob.class);

    private static final String UNKNOWN = "unknown";

    private final AiLlmUsageService aiLlmUsageService;
    private final WorkspaceAiGatewaySpendService workspaceAiGatewaySpendService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiGatewaySpendRollupJob(
        AiLlmUsageService aiLlmUsageService, WorkspaceAiGatewaySpendService workspaceAiGatewaySpendService) {

        this.aiLlmUsageService = aiLlmUsageService;
        this.workspaceAiGatewaySpendService = workspaceAiGatewaySpendService;
    }

    @Scheduled(cron = "0 5 * * * *")
    public void rollUpPreviousHour() {
        Instant periodEnd = Instant.now()
            .truncatedTo(ChronoUnit.HOURS);

        rollUp(periodEnd.minus(1, ChronoUnit.HOURS), periodEnd);
    }

    void rollUp(Instant periodStart, Instant periodEnd) {
        for (Long workspaceId : aiLlmUsageService.findDistinctWorkspaceIds()) {
            try {
                rollUpWorkspace(workspaceId, periodStart, periodEnd);
            } catch (RuntimeException exception) {
                // One broken workspace must not stop the rollup for the rest.
                log.warn("Spend rollup failed for workspace {}", workspaceId, exception);
            }
        }
    }

    private void rollUpWorkspace(long workspaceId, Instant periodStart, Instant periodEnd) {
        boolean alreadyRolledUp = workspaceAiGatewaySpendService
            .getSpendSummariesByWorkspaceId(workspaceId, periodStart, periodEnd)
            .stream()
            .anyMatch(summary -> periodStart.equals(summary.getPeriodStart()));

        if (alreadyRolledUp) {
            return;
        }

        List<AiLlmUsage> usages = aiLlmUsageService.getRequestLogsByWorkspace(workspaceId, periodStart, periodEnd);

        if (usages.isEmpty()) {
            return;
        }

        Map<GroupKey, List<AiLlmUsage>> groups = usages.stream()
            .collect(Collectors.groupingBy(AiGatewaySpendRollupJob::toGroupKey));

        for (Map.Entry<GroupKey, List<AiLlmUsage>> group : groups.entrySet()) {
            AiGatewaySpendSummary summary = new AiGatewaySpendSummary(periodStart, periodEnd);

            GroupKey groupKey = group.getKey();

            summary.setProvider(groupKey.provider());
            summary.setModel(groupKey.model());
            summary.setApiKeyId(groupKey.apiKeyId());
            summary.setProjectId(groupKey.projectId());

            List<AiLlmUsage> groupUsages = group.getValue();

            summary.setRequestCount(groupUsages.size());
            summary.setTotalInputTokens(
                groupUsages.stream()
                    .map(AiLlmUsage::getInputTokens)
                    .filter(Objects::nonNull)
                    .mapToLong(Integer::longValue)
                    .sum());
            summary.setTotalOutputTokens(
                groupUsages.stream()
                    .map(AiLlmUsage::getOutputTokens)
                    .filter(Objects::nonNull)
                    .mapToLong(Integer::longValue)
                    .sum());
            // Null costs (cancelled streams with unknown usage) are excluded from the sum rather than treated as $0;
            // the row still contributes to request/token counts.
            summary.setTotalCost(
                groupUsages.stream()
                    .map(AiLlmUsage::getCost)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            workspaceAiGatewaySpendService.createInWorkspace(summary, workspaceId);
        }

        if (log.isDebugEnabled()) {
            log.debug(
                "Rolled up {} usage rows into {} spend summaries for workspace {} ({} - {})",
                usages.size(), groups.size(), workspaceId, periodStart, periodEnd);
        }
    }

    private static GroupKey toGroupKey(AiLlmUsage usage) {
        String model = usage.getRoutedModel() != null ? usage.getRoutedModel() : usage.getRequestedModel();

        return new GroupKey(
            usage.getRoutedProvider() != null ? usage.getRoutedProvider() : UNKNOWN,
            model != null ? model : UNKNOWN,
            usage.getApiKeyId(), usage.getProjectId());
    }

    private record GroupKey(String provider, String model, Long apiKeyId, Long projectId) {
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.spend;

import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewaySpendService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewaySpendSummary;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewaySpendService;
import com.bytechef.ee.platform.ai.llm.usage.AiLlmUsage;
import com.bytechef.ee.platform.ai.llm.usage.LlmUsageSource;
import com.bytechef.ee.platform.ai.llm.usage.service.AiLlmUsageService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
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

    private final AiGatewaySpendService aiGatewaySpendService;
    private final AiLlmUsageService aiLlmUsageService;
    private final WorkspaceAiGatewaySpendService workspaceAiGatewaySpendService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiGatewaySpendRollupJob(
        AiGatewaySpendService aiGatewaySpendService, AiLlmUsageService aiLlmUsageService,
        WorkspaceAiGatewaySpendService workspaceAiGatewaySpendService) {

        this.aiGatewaySpendService = aiGatewaySpendService;
        this.aiLlmUsageService = aiLlmUsageService;
        this.workspaceAiGatewaySpendService = workspaceAiGatewaySpendService;
    }

    @Scheduled(cron = "0 5 * * * *")
    public void rollUpPreviousHour() {
        Instant periodEnd = Instant.now()
            .truncatedTo(ChronoUnit.HOURS);

        rollUp(periodEnd.minus(1, ChronoUnit.HOURS), periodEnd);
    }

    public void rollUp(Instant periodStart, Instant periodEnd) {
        for (Long workspaceId : aiLlmUsageService.findDistinctWorkspaceIds()) {
            try {
                rollUpWorkspace(workspaceId, periodStart, periodEnd);
            } catch (RuntimeException exception) {
                log.warn("Spend rollup failed for workspace {}", workspaceId, exception);
            }
        }

        try {
            rollUpConnectedUsersWithoutWorkspace(periodStart, periodEnd);
        } catch (RuntimeException exception) {
            log.warn("Spend rollup failed for workspace-less connected-user usage", exception);
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

        List<AiGatewaySpendSummary> summaries = toSummaries(usages, periodStart, periodEnd);

        for (AiGatewaySpendSummary summary : summaries) {
            workspaceAiGatewaySpendService.createInWorkspace(summary, workspaceId);
        }

        if (log.isDebugEnabled()) {
            log.debug(
                "Rolled up {} usage rows into {} spend summaries for workspace {} ({} - {})",
                usages.size(), summaries.size(), workspaceId, periodStart, periodEnd);
        }
    }

    private void rollUpConnectedUsersWithoutWorkspace(Instant periodStart, Instant periodEnd) {
        boolean alreadyRolledUp = aiGatewaySpendService.getSpendSummariesWithoutWorkspace(periodStart, periodEnd)
            .stream()
            .anyMatch(summary -> periodStart.equals(summary.getPeriodStart()));

        if (alreadyRolledUp) {
            return;
        }

        List<AiLlmUsage> usages = aiLlmUsageService
            .getConnectedUserRequestLogsWithoutWorkspace(periodStart, periodEnd)
            .stream()
            .filter(usage -> connectedUserIdOf(usage) != null)
            .toList();

        if (usages.isEmpty()) {
            return;
        }

        List<AiGatewaySpendSummary> summaries = toSummaries(usages, periodStart, periodEnd);

        for (AiGatewaySpendSummary summary : summaries) {
            aiGatewaySpendService.create(summary);
        }

        if (log.isDebugEnabled()) {
            log.debug(
                "Rolled up {} workspace-less connected-user usage rows into {} spend summaries ({} - {})",
                usages.size(), summaries.size(), periodStart, periodEnd);
        }
    }

    private static List<AiGatewaySpendSummary> toSummaries(
        List<AiLlmUsage> usages, Instant periodStart, Instant periodEnd) {

        Map<GroupKey, List<AiLlmUsage>> groups = usages.stream()
            .collect(Collectors.groupingBy(AiGatewaySpendRollupJob::toGroupKey));

        List<AiGatewaySpendSummary> summaries = new ArrayList<>(groups.size());

        for (Map.Entry<GroupKey, List<AiLlmUsage>> group : groups.entrySet()) {
            AiGatewaySpendSummary summary = new AiGatewaySpendSummary(periodStart, periodEnd);

            GroupKey groupKey = group.getKey();

            summary.setProvider(groupKey.provider());
            summary.setModel(groupKey.model());
            summary.setApiKeyId(groupKey.apiKeyId());
            summary.setProjectId(groupKey.projectId());
            summary.setConnectedUserId(groupKey.connectedUserId());

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
            summary.setTotalCost(
                groupUsages.stream()
                    .map(AiLlmUsage::getCost)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            summaries.add(summary);
        }

        return summaries;
    }

    private static GroupKey toGroupKey(AiLlmUsage usage) {
        String model = usage.getRoutedModel() != null ? usage.getRoutedModel() : usage.getRequestedModel();

        return new GroupKey(
            usage.getRoutedProvider() != null ? usage.getRoutedProvider() : UNKNOWN,
            model != null ? model : UNKNOWN,
            usage.getApiKeyId(), usage.getProjectId(), connectedUserIdOf(usage));
    }

    private static @Nullable Long connectedUserIdOf(AiLlmUsage usage) {
        LlmUsageSource source = usage.getSource();

        return source == null || source == LlmUsageSource.AI_GATEWAY ? usage.getUserId() : null;
    }

    private record GroupKey(String provider, String model, Long apiKeyId, Long projectId, Long connectedUserId) {
    }
}

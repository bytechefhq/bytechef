/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import com.bytechef.ee.platform.ai.llm.usage.AiLlmUsage;
import com.bytechef.ee.platform.ai.llm.usage.Money;
import com.bytechef.ee.platform.ai.llm.usage.repository.AiLlmUsageRepository;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertCondition;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertEvent;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertEventStatus;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertRule;
import com.bytechef.ee.platform.ai.observability.metrics.AiObservabilityMetrics;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityAlertEventService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityAlertRuleService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityNotificationDispatcher;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.scheduler.event.AlertEvaluationEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
public class AiObservabilityAlertEvaluator {

    private static final Logger log = LoggerFactory.getLogger(AiObservabilityAlertEvaluator.class);

    private final AiObservabilityMetrics aiObservabilityMetrics;
    private final AiLlmUsageRepository aiGatewayRequestLogRepository;
    private final AiObservabilityAlertEventService aiObservabilityAlertEventService;
    private final AiObservabilityAlertRuleService aiObservabilityAlertRuleService;
    private final WorkspaceAiObservabilityAlertRuleService workspaceAiObservabilityAlertRuleService;
    private final AiObservabilityNotificationDispatcher aiObservabilityNotificationDispatcher;

    AiObservabilityAlertEvaluator(
        AiObservabilityMetrics aiObservabilityMetrics,
        AiLlmUsageRepository aiGatewayRequestLogRepository,
        AiObservabilityAlertEventService aiObservabilityAlertEventService,
        AiObservabilityAlertRuleService aiObservabilityAlertRuleService,
        WorkspaceAiObservabilityAlertRuleService workspaceAiObservabilityAlertRuleService,
        AiObservabilityNotificationDispatcher aiObservabilityNotificationDispatcher) {

        this.aiObservabilityMetrics = aiObservabilityMetrics;
        this.aiGatewayRequestLogRepository = aiGatewayRequestLogRepository;
        this.aiObservabilityAlertEventService = aiObservabilityAlertEventService;
        this.aiObservabilityAlertRuleService = aiObservabilityAlertRuleService;
        this.workspaceAiObservabilityAlertRuleService = workspaceAiObservabilityAlertRuleService;
        this.aiObservabilityNotificationDispatcher = aiObservabilityNotificationDispatcher;
    }

    @EventListener
    public void onAlertEvaluation(AlertEvaluationEvent alertEvaluationEvent) {
        long alertRuleId = alertEvaluationEvent.alertRuleId();

        AiObservabilityAlertRule alertRule = aiObservabilityAlertRuleService.getAlertRule(alertRuleId);

        if (!alertRule.isEnabled()) {
            return;
        }

        Instant snoozedUntil = alertRule.getSnoozedUntil();

        if (snoozedUntil != null && snoozedUntil.isAfter(Instant.now())) {
            return;
        }

        // Let exceptions propagate so the surrounding @Transactional rolls back. Spring's
        // ApplicationEventMulticaster logs unhandled listener exceptions; catching here would
        // silently commit partial state (e.g. alert event created but notifications not dispatched).
        evaluate(alertRule);
    }

    /**
     * Computes the current metric value for the given alert rule over its configured time window, without creating an
     * alert event or dispatching notifications. Useful as a dry-run / preview (e.g., "Test" button in the UI).
     *
     * @param alertRule the alert rule to evaluate
     * @return the current metric value, or {@code null} if no data is available
     */
    @Transactional(readOnly = true)
    public BigDecimal evaluateMetric(AiObservabilityAlertRule alertRule) {
        Instant windowEnd = Instant.now();
        Instant windowStart = windowEnd.minus(alertRule.getWindowMinutes(), ChronoUnit.MINUTES);

        List<AiLlmUsage> requestLogs =
            aiGatewayRequestLogRepository.findAllByWorkspaceIdAndCreatedDateBetween(
                workspaceAiObservabilityAlertRuleService.getWorkspaceId(alertRule.getId()), windowStart, windowEnd);

        if (requestLogs.isEmpty()) {
            return null;
        }

        return computeMetric(alertRule, requestLogs);
    }

    private void evaluate(AiObservabilityAlertRule alertRule) {
        Instant windowEnd = Instant.now();
        Instant windowStart = windowEnd.minus(alertRule.getWindowMinutes(), ChronoUnit.MINUTES);

        List<AiLlmUsage> requestLogs =
            aiGatewayRequestLogRepository.findAllByWorkspaceIdAndCreatedDateBetween(
                workspaceAiObservabilityAlertRuleService.getWorkspaceId(alertRule.getId()), windowStart, windowEnd);

        if (requestLogs.isEmpty()) {
            return;
        }

        BigDecimal metricValue = computeMetric(alertRule, requestLogs);

        if (metricValue == null) {
            return;
        }

        boolean breached = evaluateCondition(alertRule.getCondition(), metricValue, alertRule.getThreshold());

        if (!breached) {
            resolveIfOpen(alertRule, metricValue);

            return;
        }

        if (isCooldownActive(alertRule)) {
            return;
        }

        String message = String.format(
            "Alert '%s': %s %s threshold %s (actual: %s) over %d minute window",
            alertRule.getName(),
            alertRule.getMetric()
                .name(),
            alertRule.getCondition()
                .name()
                .toLowerCase()
                .replace('_', ' '),
            alertRule.getThreshold()
                .toPlainString(),
            metricValue.toPlainString(),
            alertRule.getWindowMinutes());

        AiObservabilityAlertEvent alertEvent = new AiObservabilityAlertEvent(
            alertRule.getId(), metricValue, message);

        AiObservabilityAlertEvent savedAlertEvent = aiObservabilityAlertEventService.create(alertEvent);

        aiObservabilityMetrics.incrementAlertTriggered(alertRule.getMetric()
            .name());

        aiObservabilityNotificationDispatcher.dispatch(alertRule, savedAlertEvent);
    }

    private BigDecimal computeMetric(
        AiObservabilityAlertRule alertRule, List<AiLlmUsage> requestLogs) {

        return switch (alertRule.getMetric()) {
            case ERROR_RATE -> computeErrorRate(requestLogs);
            case LATENCY_P95 -> computeLatencyP95(requestLogs);
            case COST -> computeTotalCost(requestLogs);
            case TOKEN_USAGE -> computeTotalTokens(requestLogs);
            case REQUEST_VOLUME -> BigDecimal.valueOf(requestLogs.size());
        };
    }

    private BigDecimal computeErrorRate(List<AiLlmUsage> requestLogs) {
        long totalCount = requestLogs.size();
        long errorCount = requestLogs.stream()
            .filter(requestLog -> requestLog.getStatus() != null && requestLog.getStatus() >= 400)
            .count();

        if (totalCount == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(errorCount)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal computeLatencyP95(List<AiLlmUsage> requestLogs) {
        List<Integer> latencies = requestLogs.stream()
            .map(AiLlmUsage::getLatencyMs)
            .filter(latencyMs -> latencyMs != null)
            .sorted()
            .toList();

        if (latencies.isEmpty()) {
            return null;
        }

        int p95Index = (int) Math.ceil(latencies.size() * 0.95) - 1;

        p95Index = Math.max(0, Math.min(p95Index, latencies.size() - 1));

        return BigDecimal.valueOf(latencies.get(p95Index));
    }

    /**
     * Sums per-request costs as {@link Money}. Reducing over {@code Money::add} rejects cross-currency summation at
     * call-time instead of silently producing a meaningless number; {@code AiLlmUsage.getCostAsMoney()} defaults to USD
     * today, so the type guarantee buys a future-proofing win at zero current cost.
     */
    private BigDecimal computeTotalCost(List<AiLlmUsage> requestLogs) {
        return requestLogs.stream()
            .map(AiLlmUsage::getCostAsMoney)
            .filter(costMoney -> costMoney != null)
            .reduce(Money.usd(BigDecimal.ZERO), Money::add)
            .amount();
    }

    private BigDecimal computeTotalTokens(List<AiLlmUsage> requestLogs) {
        int totalTokens = requestLogs.stream()
            .mapToInt(requestLog -> {
                int inputTokens = requestLog.getInputTokens() != null ? requestLog.getInputTokens() : 0;
                int outputTokens = requestLog.getOutputTokens() != null ? requestLog.getOutputTokens() : 0;

                return inputTokens + outputTokens;
            })
            .sum();

        return BigDecimal.valueOf(totalTokens);
    }

    private boolean evaluateCondition(
        AiObservabilityAlertCondition condition, BigDecimal metricValue, BigDecimal threshold) {

        int comparison = metricValue.compareTo(threshold);

        return switch (condition) {
            case GREATER_THAN -> comparison > 0;
            case LESS_THAN -> comparison < 0;
            case EQUALS -> comparison == 0;
        };
    }

    private void resolveIfOpen(AiObservabilityAlertRule alertRule, BigDecimal currentValue) {
        Optional<AiObservabilityAlertEvent> latestEventOptional =
            aiObservabilityAlertEventService.getLatestEventByRule(alertRule.getId());

        if (latestEventOptional.isEmpty()) {
            return;
        }

        AiObservabilityAlertEvent latestEvent = latestEventOptional.get();

        if (latestEvent.getStatus() != AiObservabilityAlertEventStatus.TRIGGERED) {
            return;
        }

        latestEvent.setStatus(AiObservabilityAlertEventStatus.RESOLVED);
        latestEvent.setResolvedDate(Instant.now());

        AiObservabilityAlertEvent resolvedEvent = aiObservabilityAlertEventService.update(latestEvent);

        log.info(
            "Alert rule '{}' resolved: metric {} returned to {} (threshold: {})",
            alertRule.getName(), alertRule.getMetric(), currentValue.toPlainString(),
            alertRule.getThreshold()
                .toPlainString());

        aiObservabilityNotificationDispatcher.dispatch(alertRule, resolvedEvent);
    }

    private boolean isCooldownActive(AiObservabilityAlertRule alertRule) {
        Optional<AiObservabilityAlertEvent> latestEvent =
            aiObservabilityAlertEventService.getLatestEventByRule(alertRule.getId());

        if (latestEvent.isEmpty()) {
            return false;
        }

        Instant cooldownEnd = latestEvent.get()
            .getCreatedDate()
            .plus(alertRule.getCooldownMinutes(), ChronoUnit.MINUTES);

        return Instant.now()
            .isBefore(cooldownEnd);
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.budget;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayBudget;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayBudgetEnforcementMode;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayBudgetPeriod;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewaySpendSummary;
import com.bytechef.ee.automation.ai.gateway.domain.Money;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayBudgetService;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewaySpendService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
public class AiGatewayBudgetChecker {

    private static final Logger logger = LoggerFactory.getLogger(AiGatewayBudgetChecker.class);

    private static final long HARD_BLOCKED_TTL_MS = 5 * 60 * 1000;
    private static final String HARD_BLOCKED_CACHE = "ai-gateway-hard-blocked";

    private final AiGatewayBudgetService aiGatewayBudgetService;
    private final AiGatewaySpendService aiGatewaySpendService;
    private final CacheManager cacheManager;

    public AiGatewayBudgetChecker(
        AiGatewayBudgetService aiGatewayBudgetService,
        AiGatewaySpendService aiGatewaySpendService,
        CacheManager cacheManager) {

        this.aiGatewayBudgetService = aiGatewayBudgetService;
        this.aiGatewaySpendService = aiGatewaySpendService;
        this.cacheManager = cacheManager;
    }

    public BudgetCheckResult checkBudget(long workspaceId) {
        BudgetCheckResult cachedResult = getHardBlockedCache().get(workspaceId, BudgetCheckResult.class);

        if (cachedResult != null) {
            if (System.currentTimeMillis() - cachedResult.cachedAtMs() < HARD_BLOCKED_TTL_MS) {
                return cachedResult;
            }

            getHardBlockedCache().evict(workspaceId);
        }

        Optional<AiGatewayBudget> budgetOptional = aiGatewayBudgetService.getBudgetByWorkspaceId(workspaceId);

        if (budgetOptional.isEmpty() || !budgetOptional.get()
            .isEnabled()) {
            return BudgetCheckResult.allowed();
        }

        AiGatewayBudget budget = budgetOptional.get();

        if (budget.getAmount() == null || budget.getAmount()
            .compareTo(BigDecimal.ZERO) <= 0) {

            return BudgetCheckResult.rejected(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(100));
        }

        Money budgetMoney = budget.getAmountAsMoney();
        Instant periodStart = calculatePeriodStart(budget.getPeriod(), budget.getWeekStartsOn());
        Instant periodEnd = Instant.now();

        Money currentSpendMoney = sumSpend(workspaceId, periodStart, periodEnd);

        BigDecimal currentSpend = currentSpendMoney.amount();

        BigDecimal usagePercentage = currentSpend
            .multiply(BigDecimal.valueOf(100))
            .divide(budget.getAmount(), 2, RoundingMode.HALF_UP);

        boolean overBudget = currentSpendMoney.compareTo(budgetMoney) >= 0;
        boolean overThreshold =
            usagePercentage.compareTo(BigDecimal.valueOf(budget.getAlertThreshold())) >= 0;

        if (overBudget && budget.getEnforcementMode() == AiGatewayBudgetEnforcementMode.HARD) {
            BudgetCheckResult rejectedResult = BudgetCheckResult.rejected(
                currentSpend, budget.getAmount(), usagePercentage);

            getHardBlockedCache().put(workspaceId, rejectedResult);

            return rejectedResult;
        }

        if (overBudget || overThreshold) {
            return BudgetCheckResult.warning(currentSpend, budget.getAmount(), usagePercentage);
        }

        return BudgetCheckResult.allowed();
    }

    public void recordSpendAndEnforce(long workspaceId) {
        Optional<AiGatewayBudget> budgetOptional = aiGatewayBudgetService.getBudgetByWorkspaceId(workspaceId);

        if (budgetOptional.isEmpty() || !budgetOptional.get()
            .isEnabled()) {

            getHardBlockedCache().evict(workspaceId);

            return;
        }

        AiGatewayBudget budget = budgetOptional.get();

        if (budget.getEnforcementMode() != AiGatewayBudgetEnforcementMode.HARD) {
            return;
        }

        if (budget.getAmount() == null || budget.getAmount()
            .compareTo(BigDecimal.ZERO) <= 0) {

            return;
        }

        Money budgetMoney = budget.getAmountAsMoney();
        Instant periodStart = calculatePeriodStart(budget.getPeriod(), budget.getWeekStartsOn());

        Money currentSpendMoney = sumSpend(workspaceId, periodStart, Instant.now());

        BigDecimal currentSpend = currentSpendMoney.amount();

        BigDecimal usagePercentage = currentSpend
            .multiply(BigDecimal.valueOf(100))
            .divide(budget.getAmount(), 2, RoundingMode.HALF_UP);

        if (currentSpendMoney.compareTo(budgetMoney) >= 0) {
            BudgetCheckResult blockedResult = BudgetCheckResult.rejected(
                currentSpend, budget.getAmount(), usagePercentage);

            getHardBlockedCache().put(workspaceId, blockedResult);

            logger.warn("Workspace {} is now hard-blocked: spend ${} exceeds budget ${}",
                workspaceId, currentSpend, budget.getAmount());
        } else {
            getHardBlockedCache().evict(workspaceId);
        }
    }

    /**
     * @param weekStartsOn 1 = Monday (ISO), 7 = Sunday — matches {@link java.time.DayOfWeek#getValue()}. Applied only
     *                     to {@link AiGatewayBudgetPeriod#WEEKLY}; ignored otherwise.
     */
    private Instant calculatePeriodStart(AiGatewayBudgetPeriod period, int weekStartsOn) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        return switch (period) {
            case DAILY -> now.truncatedTo(ChronoUnit.DAYS)
                .toInstant();
            case WEEKLY -> {
                int currentDow = now.getDayOfWeek()
                    .getValue();
                // Days to roll back so the current instant lands on the configured week-start day. weekStartsOn is
                // 1..7; fall back to Monday if it's out of range (shouldn't happen given domain validation).
                int normalizedStart = (weekStartsOn >= 1 && weekStartsOn <= 7) ? weekStartsOn : 1;
                int daysToSubtract = (currentDow - normalizedStart + 7) % 7;

                yield now.minusDays(daysToSubtract)
                    .truncatedTo(ChronoUnit.DAYS)
                    .toInstant();
            }
            case MONTHLY -> now.withDayOfMonth(1)
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant();
            case QUARTERLY ->
                now.withMonth((now.getMonthValue() - 1) / 3 * 3 + 1)
                    .withDayOfMonth(1)
                    .truncatedTo(ChronoUnit.DAYS)
                    .toInstant();
            case YEARLY -> now.withDayOfYear(1)
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant();
        };
    }

    public void evictHardBlockedCache(long workspaceId) {
        getHardBlockedCache().evict(workspaceId);
    }

    /**
     * Aggregates spend-summary totals as {@link Money} so mixed-currency rows would throw at add-time instead of
     * silently summing into a misleading total. The current schema assumes all-USD budgets; if a provider starts
     * emitting non-USD summaries the reject path here is preferable to an arithmetic-but-wrong number.
     */
    private Money sumSpend(long workspaceId, Instant periodStart, Instant periodEnd) {
        return aiGatewaySpendService.getSpendSummariesByWorkspaceId(workspaceId, periodStart, periodEnd)
            .stream()
            .map(AiGatewaySpendSummary::getTotalCostAsMoney)
            .reduce(Money.usd(BigDecimal.ZERO), Money::add);
    }

    private Cache getHardBlockedCache() {
        Cache cache = cacheManager.getCache(HARD_BLOCKED_CACHE);

        if (cache == null) {
            throw new IllegalStateException(
                "Required cache '" + HARD_BLOCKED_CACHE + "' is not configured in the CacheManager");
        }

        return cache;
    }

    public record BudgetCheckResult(
        boolean requestAllowed, boolean thresholdWarning, BigDecimal currentSpend, BigDecimal budgetAmount,
        BigDecimal usagePercentage, long cachedAtMs) {

        public static BudgetCheckResult allowed() {
            return new BudgetCheckResult(
                true, false, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, System.currentTimeMillis());
        }

        public static BudgetCheckResult warning(
            BigDecimal currentSpend, BigDecimal budgetAmount, BigDecimal usagePercentage) {

            return new BudgetCheckResult(
                true, true, currentSpend, budgetAmount, usagePercentage, System.currentTimeMillis());
        }

        public static BudgetCheckResult rejected(
            BigDecimal currentSpend, BigDecimal budgetAmount, BigDecimal usagePercentage) {

            return new BudgetCheckResult(
                false, false, currentSpend, budgetAmount, usagePercentage, System.currentTimeMillis());
        }
    }
}

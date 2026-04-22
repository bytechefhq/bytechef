/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.budget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.gateway.budget.AiGatewayBudgetChecker.BudgetCheckResult;
import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayBudgetService;
import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewaySpendService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayBudget;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayBudgetEnforcementMode;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayBudgetPeriod;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewaySpendSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiGatewayBudgetCheckerTest {

    @Mock
    private WorkspaceAiGatewayBudgetService workspaceAiGatewayBudgetService;

    @Mock
    private WorkspaceAiGatewaySpendService workspaceAiGatewaySpendService;

    @Mock
    private CacheManager cacheManager;

    private AiGatewayBudgetChecker aiGatewayBudgetChecker;

    @BeforeEach
    void setUp() {
        Cache hardBlockedCache = new ConcurrentMapCache("ai-gateway-hard-blocked");

        org.mockito.Mockito.lenient()
            .when(cacheManager.getCache("ai-gateway-hard-blocked"))
            .thenReturn(hardBlockedCache);

        aiGatewayBudgetChecker = new AiGatewayBudgetChecker(
            workspaceAiGatewayBudgetService, workspaceAiGatewaySpendService, cacheManager);
    }

    @Test
    void testCheckBudgetNoBudgetConfigured() {
        when(workspaceAiGatewayBudgetService.getBudgetByWorkspaceId(1L)).thenReturn(Optional.empty());

        BudgetCheckResult result = aiGatewayBudgetChecker.checkBudget(1L);

        assertTrue(result.requestAllowed());
        assertFalse(result.thresholdWarning());
        assertEquals(BigDecimal.ZERO, result.currentSpend());
        assertEquals(BigDecimal.ZERO, result.budgetAmount());
    }

    @Test
    void testCheckBudgetDisabledBudget() {
        AiGatewayBudget budget = new AiGatewayBudget(
            new BigDecimal("100"), AiGatewayBudgetPeriod.MONTHLY,
            AiGatewayBudgetEnforcementMode.HARD);

        budget.setEnabled(false);

        when(workspaceAiGatewayBudgetService.getBudgetByWorkspaceId(1L)).thenReturn(Optional.of(budget));

        BudgetCheckResult result = aiGatewayBudgetChecker.checkBudget(1L);

        assertTrue(result.requestAllowed());
        assertFalse(result.thresholdWarning());
    }

    @Test
    void testCheckBudgetZeroAmount() {
        AiGatewayBudget budget = new AiGatewayBudget(
            BigDecimal.ONE, AiGatewayBudgetPeriod.MONTHLY,
            AiGatewayBudgetEnforcementMode.HARD);

        ReflectionTestUtils.setField(budget, "amount", BigDecimal.ZERO);

        when(workspaceAiGatewayBudgetService.getBudgetByWorkspaceId(1L)).thenReturn(Optional.of(budget));

        BudgetCheckResult result = aiGatewayBudgetChecker.checkBudget(1L);

        assertFalse(result.requestAllowed());
        assertEquals(BigDecimal.ZERO, result.currentSpend());
        assertEquals(BigDecimal.ZERO, result.budgetAmount());
        assertEquals(BigDecimal.valueOf(100), result.usagePercentage());
    }

    @Test
    void testCheckBudgetBelowThreshold() {
        AiGatewayBudget budget = new AiGatewayBudget(
            new BigDecimal("100"), AiGatewayBudgetPeriod.MONTHLY,
            AiGatewayBudgetEnforcementMode.HARD);

        when(workspaceAiGatewayBudgetService.getBudgetByWorkspaceId(1L)).thenReturn(Optional.of(budget));

        AiGatewaySpendSummary spendSummary = new AiGatewaySpendSummary(Instant.now(), Instant.now());

        spendSummary.setTotalCost(new BigDecimal("50"));

        when(workspaceAiGatewaySpendService.getSpendSummariesByWorkspaceId(any(Long.class), any(Instant.class),
            any(Instant.class)))
                .thenReturn(List.of(spendSummary));

        BudgetCheckResult result = aiGatewayBudgetChecker.checkBudget(1L);

        assertTrue(result.requestAllowed());
        assertFalse(result.thresholdWarning());
    }

    @Test
    void testCheckBudgetAboveThresholdSoftEnforcement() {
        AiGatewayBudget budget = new AiGatewayBudget(
            new BigDecimal("100"), AiGatewayBudgetPeriod.MONTHLY,
            AiGatewayBudgetEnforcementMode.SOFT);

        when(workspaceAiGatewayBudgetService.getBudgetByWorkspaceId(1L)).thenReturn(Optional.of(budget));

        AiGatewaySpendSummary spendSummary = new AiGatewaySpendSummary(Instant.now(), Instant.now());

        spendSummary.setTotalCost(new BigDecimal("85"));

        when(workspaceAiGatewaySpendService.getSpendSummariesByWorkspaceId(any(Long.class), any(Instant.class),
            any(Instant.class)))
                .thenReturn(List.of(spendSummary));

        BudgetCheckResult result = aiGatewayBudgetChecker.checkBudget(1L);

        assertTrue(result.requestAllowed());
        assertTrue(result.thresholdWarning());
        // currentSpend flows through Money which normalizes USD to 2 decimal places.
        assertEquals(new BigDecimal("85.00"), result.currentSpend());
        assertEquals(new BigDecimal("100"), result.budgetAmount());
    }

    @Test
    void testCheckBudgetOverBudgetHardEnforcement() {
        AiGatewayBudget budget = new AiGatewayBudget(
            new BigDecimal("100"), AiGatewayBudgetPeriod.MONTHLY,
            AiGatewayBudgetEnforcementMode.HARD);

        when(workspaceAiGatewayBudgetService.getBudgetByWorkspaceId(1L)).thenReturn(Optional.of(budget));

        AiGatewaySpendSummary spendSummary = new AiGatewaySpendSummary(Instant.now(), Instant.now());

        spendSummary.setTotalCost(new BigDecimal("110"));

        when(workspaceAiGatewaySpendService.getSpendSummariesByWorkspaceId(any(Long.class), any(Instant.class),
            any(Instant.class)))
                .thenReturn(List.of(spendSummary));

        BudgetCheckResult result = aiGatewayBudgetChecker.checkBudget(1L);

        assertFalse(result.requestAllowed());
        assertFalse(result.thresholdWarning());
        assertEquals(new BigDecimal("110.00"), result.currentSpend());
        assertEquals(new BigDecimal("100"), result.budgetAmount());
    }

    @Test
    void testCheckBudgetOverBudgetSoftEnforcement() {
        AiGatewayBudget budget = new AiGatewayBudget(
            new BigDecimal("100"), AiGatewayBudgetPeriod.MONTHLY,
            AiGatewayBudgetEnforcementMode.SOFT);

        when(workspaceAiGatewayBudgetService.getBudgetByWorkspaceId(1L)).thenReturn(Optional.of(budget));

        AiGatewaySpendSummary spendSummary = new AiGatewaySpendSummary(Instant.now(), Instant.now());

        spendSummary.setTotalCost(new BigDecimal("110"));

        when(workspaceAiGatewaySpendService.getSpendSummariesByWorkspaceId(any(Long.class), any(Instant.class),
            any(Instant.class)))
                .thenReturn(List.of(spendSummary));

        BudgetCheckResult result = aiGatewayBudgetChecker.checkBudget(1L);

        assertTrue(result.requestAllowed());
        assertTrue(result.thresholdWarning());
        assertEquals(new BigDecimal("110.00"), result.currentSpend());
        assertEquals(new BigDecimal("100"), result.budgetAmount());
    }

    @Test
    void testCalculatePeriodStartDaily() {
        AiGatewayBudget budget = new AiGatewayBudget(
            new BigDecimal("100"), AiGatewayBudgetPeriod.DAILY,
            AiGatewayBudgetEnforcementMode.HARD);

        when(workspaceAiGatewayBudgetService.getBudgetByWorkspaceId(1L)).thenReturn(Optional.of(budget));

        AiGatewaySpendSummary spendSummary = new AiGatewaySpendSummary(Instant.now(), Instant.now());

        spendSummary.setTotalCost(new BigDecimal("50"));

        when(workspaceAiGatewaySpendService.getSpendSummariesByWorkspaceId(any(Long.class), any(Instant.class),
            any(Instant.class)))
                .thenReturn(List.of(spendSummary));

        aiGatewayBudgetChecker.checkBudget(1L);

        Instant expectedPeriodStart = ZonedDateTime.now(ZoneOffset.UTC)
            .truncatedTo(ChronoUnit.DAYS)
            .toInstant();

        org.mockito.ArgumentCaptor<Instant> startCaptor = org.mockito.ArgumentCaptor.forClass(Instant.class);

        org.mockito.Mockito.verify(workspaceAiGatewaySpendService)
            .getSpendSummariesByWorkspaceId(any(Long.class), startCaptor.capture(), any(Instant.class));

        Instant capturedStart = startCaptor.getValue();

        assertEquals(expectedPeriodStart, capturedStart);
    }

    @Test
    void testCalculatePeriodStartQuarterly() {
        AiGatewayBudget budget = new AiGatewayBudget(
            new BigDecimal("100"), AiGatewayBudgetPeriod.QUARTERLY,
            AiGatewayBudgetEnforcementMode.HARD);

        when(workspaceAiGatewayBudgetService.getBudgetByWorkspaceId(1L)).thenReturn(Optional.of(budget));

        AiGatewaySpendSummary spendSummary = new AiGatewaySpendSummary(Instant.now(), Instant.now());

        spendSummary.setTotalCost(new BigDecimal("50"));

        when(workspaceAiGatewaySpendService.getSpendSummariesByWorkspaceId(any(Long.class), any(Instant.class),
            any(Instant.class)))
                .thenReturn(List.of(spendSummary));

        aiGatewayBudgetChecker.checkBudget(1L);

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        int quarterStartMonth = (now.getMonthValue() - 1) / 3 * 3 + 1;

        Instant expectedPeriodStart = now.withMonth(quarterStartMonth)
            .withDayOfMonth(1)
            .truncatedTo(ChronoUnit.DAYS)
            .toInstant();

        org.mockito.ArgumentCaptor<Instant> startCaptor = org.mockito.ArgumentCaptor.forClass(Instant.class);

        org.mockito.Mockito.verify(workspaceAiGatewaySpendService)
            .getSpendSummariesByWorkspaceId(any(Long.class), startCaptor.capture(), any(Instant.class));

        Instant capturedStart = startCaptor.getValue();

        assertEquals(expectedPeriodStart, capturedStart);
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewaySpendSummary;
import com.bytechef.ee.platform.ai.gateway.repository.AiGatewaySpendSummaryRepository;
import com.bytechef.ee.platform.ai.llm.usage.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiGatewaySpendServiceTest {

    private final Instant start = Instant.parse("2026-08-01T00:00:00Z");
    private final Instant end = Instant.parse("2026-09-01T00:00:00Z");

    @Mock
    private AiGatewaySpendSummaryRepository aiGatewaySpendSummaryRepository;

    private AiGatewaySpendService aiGatewaySpendService;

    @BeforeEach
    void setUp() {
        aiGatewaySpendService = new AiGatewaySpendServiceImpl(aiGatewaySpendSummaryRepository);
    }

    @Test
    void testGetTotalCostByConnectedUserIdSumsAcrossMatchingSummaries() {
        AiGatewaySpendSummary firstSummary = new AiGatewaySpendSummary(start, end);

        firstSummary.setTotalCost(new BigDecimal("12.50"));

        AiGatewaySpendSummary secondSummary = new AiGatewaySpendSummary(start, end);

        secondSummary.setTotalCost(new BigDecimal("7.25"));

        when(aiGatewaySpendSummaryRepository.findAllByConnectedUserIdAndPeriodStartBetween(5L, start, end))
            .thenReturn(List.of(firstSummary, secondSummary));

        Money total = aiGatewaySpendService.getTotalCostByConnectedUserId(5L, start, end);

        assertThat(total).isEqualTo(Money.usd(new BigDecimal("19.75")));
    }

    @Test
    void testGetTotalCostByConnectedUserIdExcludesNullCostRows() {
        AiGatewaySpendSummary summaryWithCost = new AiGatewaySpendSummary(start, end);

        summaryWithCost.setTotalCost(new BigDecimal("5.00"));

        AiGatewaySpendSummary summaryWithoutCost = new AiGatewaySpendSummary(start, end);

        when(aiGatewaySpendSummaryRepository.findAllByConnectedUserIdAndPeriodStartBetween(5L, start, end))
            .thenReturn(List.of(summaryWithCost, summaryWithoutCost));

        Money total = aiGatewaySpendService.getTotalCostByConnectedUserId(5L, start, end);

        assertThat(total).isEqualTo(Money.usd(new BigDecimal("5.00")));
    }

    @Test
    void testGetTotalCostByConnectedUserIdReturnsZeroRatherThanNullWhenNoRowsMatch() {
        when(aiGatewaySpendSummaryRepository.findAllByConnectedUserIdAndPeriodStartBetween(5L, start, end))
            .thenReturn(List.of());

        Money total = aiGatewaySpendService.getTotalCostByConnectedUserId(5L, start, end);

        assertThat(total).isEqualTo(Money.usd(BigDecimal.ZERO));
    }

    @Test
    void testGetTotalCostByConnectedUserIdThrowsOnCurrencyMismatchRatherThanSummingWrong() {
        AiGatewaySpendSummary usdSummary = new AiGatewaySpendSummary(start, end);

        usdSummary.setTotalCost(new BigDecimal("5.00"));

        AiGatewaySpendSummary eurSummary = new AiGatewaySpendSummary(start, end);

        eurSummary.setCurrency("EUR");
        eurSummary.setTotalCost(new BigDecimal("5.00"));

        when(aiGatewaySpendSummaryRepository.findAllByConnectedUserIdAndPeriodStartBetween(5L, start, end))
            .thenReturn(List.of(usdSummary, eurSummary));

        assertThatThrownBy(() -> aiGatewaySpendService.getTotalCostByConnectedUserId(5L, start, end))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Currency mismatch");
    }
}

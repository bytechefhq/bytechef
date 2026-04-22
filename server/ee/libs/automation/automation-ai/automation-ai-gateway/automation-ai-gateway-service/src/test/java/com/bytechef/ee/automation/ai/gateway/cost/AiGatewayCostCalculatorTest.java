/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModel;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class AiGatewayCostCalculatorTest {

    private AiGatewayCostCalculator aiGatewayCostCalculator;

    @BeforeEach
    void setUp() {
        aiGatewayCostCalculator = new AiGatewayCostCalculator();
    }

    @Test
    void testCalculateCostWithValidInputAndOutput() {
        AiGatewayModel model = new AiGatewayModel(1L, "gpt-4");

        model.setInputCostPerMTokens(new BigDecimal("30.00"));
        model.setOutputCostPerMTokens(new BigDecimal("60.00"));

        BigDecimal cost = aiGatewayCostCalculator.calculateCost(model, 1000, 500);

        // inputCost = 30.00 * 1000 / 1_000_000 = 0.030000
        // outputCost = 60.00 * 500 / 1_000_000 = 0.030000
        // total = 0.060000
        BigDecimal expectedCost = new BigDecimal("0.060000");

        assertEquals(expectedCost, cost);
    }

    @Test
    void testCalculateCostZeroTokens() {
        AiGatewayModel model = new AiGatewayModel(1L, "gpt-4");

        model.setInputCostPerMTokens(new BigDecimal("30.00"));
        model.setOutputCostPerMTokens(new BigDecimal("60.00"));

        BigDecimal cost = aiGatewayCostCalculator.calculateCost(model, 0, 0);

        assertEquals(new BigDecimal("0.000000"), cost);
    }

    @Test
    void testCalculateCostNullInputCostThrows() {
        AiGatewayModel model = new AiGatewayModel(1L, "gpt-4");

        model.setOutputCostPerMTokens(new BigDecimal("60.00"));

        assertThrows(
            IllegalStateException.class,
            () -> aiGatewayCostCalculator.calculateCost(model, 1000, 500));
    }

    @Test
    void testCalculateCostNullOutputCostThrows() {
        AiGatewayModel model = new AiGatewayModel(1L, "gpt-4");

        model.setInputCostPerMTokens(new BigDecimal("30.00"));

        assertThrows(
            IllegalStateException.class,
            () -> aiGatewayCostCalculator.calculateCost(model, 1000, 500));
    }
}

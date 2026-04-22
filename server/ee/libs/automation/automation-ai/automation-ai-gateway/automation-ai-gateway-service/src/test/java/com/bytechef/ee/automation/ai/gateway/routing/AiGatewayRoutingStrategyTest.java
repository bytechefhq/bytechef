/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModel;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModelDeployment;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRoutingStrategyType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class AiGatewayRoutingStrategyTest {

    @Test
    void testWeightedRandomWithSingleDeployment() {
        WeightedRandomRoutingStrategy strategy = new WeightedRandomRoutingStrategy();

        AiGatewayModelDeployment deployment = new AiGatewayModelDeployment(1L, 100L);
        AiGatewayRoutingContext context = new AiGatewayRoutingContext(Map.of());

        AiGatewayModelDeployment selected = strategy.selectDeployment(List.of(deployment), context);

        assertEquals(deployment, selected);
    }

    @Test
    void testCostOptimizedSelectsCheapest() {
        CostOptimizedRoutingStrategy strategy = new CostOptimizedRoutingStrategy();

        AiGatewayModelDeployment expensiveDeployment = new AiGatewayModelDeployment(1L, 100L);
        AiGatewayModelDeployment cheapDeployment = new AiGatewayModelDeployment(1L, 200L);

        AiGatewayModel expensiveModel = new AiGatewayModel(1L, "gpt-4");

        expensiveModel.setInputCostPerMTokens(new BigDecimal("30.00"));

        AiGatewayModel cheapModel = new AiGatewayModel(2L, "gpt-3.5-turbo");

        cheapModel.setInputCostPerMTokens(new BigDecimal("0.50"));

        Map<Long, AiGatewayModel> modelMap = Map.of(100L, expensiveModel, 200L, cheapModel);

        AiGatewayRoutingContext context = new AiGatewayRoutingContext(modelMap);

        AiGatewayModelDeployment selected = strategy.selectDeployment(
            List.of(expensiveDeployment, cheapDeployment), context);

        assertEquals(cheapDeployment, selected);
    }

    @Test
    void testPriorityFallbackSelectsLowestOrder() {
        PriorityFallbackRoutingStrategy strategy = new PriorityFallbackRoutingStrategy();

        AiGatewayModelDeployment highPriorityDeployment = new AiGatewayModelDeployment(1L, 100L);

        highPriorityDeployment.setPriorityOrder(1);

        AiGatewayModelDeployment lowPriorityDeployment = new AiGatewayModelDeployment(1L, 200L);

        lowPriorityDeployment.setPriorityOrder(10);

        AiGatewayRoutingContext context = new AiGatewayRoutingContext(Map.of());

        AiGatewayModelDeployment selected = strategy.selectDeployment(
            List.of(lowPriorityDeployment, highPriorityDeployment), context);

        assertEquals(highPriorityDeployment, selected);
    }

    @Test
    void testLatencyOptimizedSelectsLowestLatency() {
        LatencyOptimizedRoutingStrategy strategy = new LatencyOptimizedRoutingStrategy();

        AiGatewayModelDeployment slowDeployment = new AiGatewayModelDeployment(1L, 100L);
        AiGatewayModelDeployment fastDeployment = new AiGatewayModelDeployment(1L, 200L);

        Map<Long, Double> latencyMap = Map.of(100L, 500.0, 200L, 100.0);

        AiGatewayRoutingContext context = new AiGatewayRoutingContext(latencyMap, Map.of());

        AiGatewayModelDeployment selected = strategy.selectDeployment(
            List.of(slowDeployment, fastDeployment), context);

        assertEquals(fastDeployment, selected);
    }

    // SimpleRoutingStrategy tests

    @Test
    void testSimpleReturnsFirstDeployment() {
        SimpleRoutingStrategy strategy = new SimpleRoutingStrategy();

        AiGatewayModelDeployment firstDeployment = new AiGatewayModelDeployment(1L, 100L);
        AiGatewayModelDeployment secondDeployment = new AiGatewayModelDeployment(1L, 200L);

        AiGatewayRoutingContext context = new AiGatewayRoutingContext(Map.of());

        AiGatewayModelDeployment selected = strategy.selectDeployment(
            List.of(firstDeployment, secondDeployment), context);

        assertEquals(firstDeployment, selected);
    }

    // TagBasedRoutingStrategy tests

    @Test
    void testTagBasedSelectsHighestPriority() {
        TagBasedRoutingStrategy strategy = new TagBasedRoutingStrategy();

        AiGatewayModelDeployment highPriorityDeployment = new AiGatewayModelDeployment(1L, 100L);

        highPriorityDeployment.setPriorityOrder(1);

        AiGatewayModelDeployment lowPriorityDeployment = new AiGatewayModelDeployment(1L, 200L);

        lowPriorityDeployment.setPriorityOrder(10);

        AiGatewayRoutingContext context = new AiGatewayRoutingContext(Map.of());

        AiGatewayModelDeployment selected = strategy.selectDeployment(
            List.of(lowPriorityDeployment, highPriorityDeployment), context);

        assertEquals(highPriorityDeployment, selected);
    }

    @Test
    void testTagBasedEmptyDeploymentsThrowsException() {
        TagBasedRoutingStrategy strategy = new TagBasedRoutingStrategy();

        AiGatewayRoutingContext context = new AiGatewayRoutingContext(Map.of());

        assertThrows(IllegalArgumentException.class, () -> strategy.selectDeployment(List.of(), context));
    }

    // IntelligentRoutingStrategy tests

    @Test
    void testIntelligentCostLowComplexityRoutesCheapest() {
        IntelligentRoutingStrategy strategy = new IntelligentRoutingStrategy(
            AiGatewayRoutingStrategyType.INTELLIGENT_COST);

        AiGatewayModelDeployment cheapDeployment = new AiGatewayModelDeployment(1L, 100L);
        AiGatewayModelDeployment expensiveDeployment = new AiGatewayModelDeployment(1L, 200L);

        AiGatewayModel cheapModel = new AiGatewayModel(1L, "cheap-model");

        cheapModel.setOutputCostPerMTokens(new BigDecimal("0.50"));

        AiGatewayModel expensiveModel = new AiGatewayModel(2L, "expensive-model");

        expensiveModel.setOutputCostPerMTokens(new BigDecimal("30.00"));

        Map<Long, AiGatewayModel> modelMap = Map.of(100L, cheapModel, 200L, expensiveModel);

        AiGatewayRoutingContext context = new AiGatewayRoutingContext(
            Map.of(), modelMap, 0.2, Map.of(), Map.of());

        AiGatewayModelDeployment selected = strategy.selectDeployment(
            List.of(cheapDeployment, expensiveDeployment), context);

        assertEquals(cheapDeployment, selected);
    }

    @Test
    void testIntelligentCostHighComplexityRoutesMostCapable() {
        IntelligentRoutingStrategy strategy = new IntelligentRoutingStrategy(
            AiGatewayRoutingStrategyType.INTELLIGENT_COST);

        AiGatewayModelDeployment cheapDeployment = new AiGatewayModelDeployment(1L, 100L);
        AiGatewayModelDeployment expensiveDeployment = new AiGatewayModelDeployment(1L, 200L);

        AiGatewayModel cheapModel = new AiGatewayModel(1L, "cheap-model");

        cheapModel.setOutputCostPerMTokens(new BigDecimal("0.50"));

        AiGatewayModel expensiveModel = new AiGatewayModel(2L, "expensive-model");

        expensiveModel.setOutputCostPerMTokens(new BigDecimal("30.00"));

        Map<Long, AiGatewayModel> modelMap = Map.of(100L, cheapModel, 200L, expensiveModel);

        AiGatewayRoutingContext context = new AiGatewayRoutingContext(
            Map.of(), modelMap, 0.9, Map.of(), Map.of());

        AiGatewayModelDeployment selected = strategy.selectDeployment(
            List.of(cheapDeployment, expensiveDeployment), context);

        assertEquals(expensiveDeployment, selected);
    }

    @Test
    void testIntelligentBalancedBelowThresholdRoutesCheapest() {
        IntelligentRoutingStrategy strategy = new IntelligentRoutingStrategy(
            AiGatewayRoutingStrategyType.INTELLIGENT_BALANCED);

        AiGatewayModelDeployment cheapDeployment = new AiGatewayModelDeployment(1L, 100L);
        AiGatewayModelDeployment expensiveDeployment = new AiGatewayModelDeployment(1L, 200L);

        AiGatewayModel cheapModel = new AiGatewayModel(1L, "cheap-model");

        cheapModel.setOutputCostPerMTokens(new BigDecimal("0.50"));

        AiGatewayModel expensiveModel = new AiGatewayModel(2L, "expensive-model");

        expensiveModel.setOutputCostPerMTokens(new BigDecimal("30.00"));

        Map<Long, AiGatewayModel> modelMap = Map.of(100L, cheapModel, 200L, expensiveModel);

        AiGatewayRoutingContext context = new AiGatewayRoutingContext(
            Map.of(), modelMap, 0.4, Map.of(), Map.of());

        AiGatewayModelDeployment selected = strategy.selectDeployment(
            List.of(cheapDeployment, expensiveDeployment), context);

        assertEquals(cheapDeployment, selected);
    }

    @Test
    void testIntelligentBalancedAboveThresholdRoutesMostCapable() {
        IntelligentRoutingStrategy strategy = new IntelligentRoutingStrategy(
            AiGatewayRoutingStrategyType.INTELLIGENT_BALANCED);

        AiGatewayModelDeployment cheapDeployment = new AiGatewayModelDeployment(1L, 100L);
        AiGatewayModelDeployment expensiveDeployment = new AiGatewayModelDeployment(1L, 200L);

        AiGatewayModel cheapModel = new AiGatewayModel(1L, "cheap-model");

        cheapModel.setOutputCostPerMTokens(new BigDecimal("0.50"));

        AiGatewayModel expensiveModel = new AiGatewayModel(2L, "expensive-model");

        expensiveModel.setOutputCostPerMTokens(new BigDecimal("30.00"));

        Map<Long, AiGatewayModel> modelMap = Map.of(100L, cheapModel, 200L, expensiveModel);

        AiGatewayRoutingContext context = new AiGatewayRoutingContext(
            Map.of(), modelMap, 0.6, Map.of(), Map.of());

        AiGatewayModelDeployment selected = strategy.selectDeployment(
            List.of(cheapDeployment, expensiveDeployment), context);

        assertEquals(expensiveDeployment, selected);
    }

    @Test
    void testIntelligentQualityLowThresholdRoutesMostCapable() {
        IntelligentRoutingStrategy strategy = new IntelligentRoutingStrategy(
            AiGatewayRoutingStrategyType.INTELLIGENT_QUALITY);

        AiGatewayModelDeployment cheapDeployment = new AiGatewayModelDeployment(1L, 100L);
        AiGatewayModelDeployment expensiveDeployment = new AiGatewayModelDeployment(1L, 200L);

        AiGatewayModel cheapModel = new AiGatewayModel(1L, "cheap-model");

        cheapModel.setOutputCostPerMTokens(new BigDecimal("0.50"));

        AiGatewayModel expensiveModel = new AiGatewayModel(2L, "expensive-model");

        expensiveModel.setOutputCostPerMTokens(new BigDecimal("30.00"));

        Map<Long, AiGatewayModel> modelMap = Map.of(100L, cheapModel, 200L, expensiveModel);

        AiGatewayRoutingContext context = new AiGatewayRoutingContext(
            Map.of(), modelMap, 0.4, Map.of(), Map.of());

        AiGatewayModelDeployment selected = strategy.selectDeployment(
            List.of(cheapDeployment, expensiveDeployment), context);

        assertEquals(expensiveDeployment, selected);
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModelDeployment;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingStrategyType;
import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
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

        AiModel expensiveModel = new AiModel(1L, "gpt-4");

        expensiveModel.setInputCostPerMTokens(new BigDecimal("30.00"));

        AiModel cheapModel = new AiModel(2L, "gpt-3.5-turbo");

        cheapModel.setInputCostPerMTokens(new BigDecimal("0.50"));

        Map<Long, AiModel> modelMap = Map.of(100L, expensiveModel, 200L, cheapModel);

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

        AiModel cheapModel = new AiModel(1L, "cheap-model");

        cheapModel.setOutputCostPerMTokens(new BigDecimal("0.50"));

        AiModel expensiveModel = new AiModel(2L, "expensive-model");

        expensiveModel.setOutputCostPerMTokens(new BigDecimal("30.00"));

        Map<Long, AiModel> modelMap = Map.of(100L, cheapModel, 200L, expensiveModel);

        AiGatewayRoutingContext context = new AiGatewayRoutingContext(
            Map.of(), modelMap, 0.2, Map.of(), Map.of());

        AiGatewayModelDeployment selected = strategy.selectDeployment(
            List.of(cheapDeployment, expensiveDeployment), context);

        assertEquals(Long.valueOf(100L), selected.getModelId());
    }

    @Test
    void testIntelligentCostHighComplexityRoutesMostCapable() {
        IntelligentRoutingStrategy strategy = new IntelligentRoutingStrategy(
            AiGatewayRoutingStrategyType.INTELLIGENT_COST);

        AiGatewayModelDeployment cheapDeployment = new AiGatewayModelDeployment(1L, 100L);
        AiGatewayModelDeployment expensiveDeployment = new AiGatewayModelDeployment(1L, 200L);

        AiModel cheapModel = new AiModel(1L, "cheap-model");

        cheapModel.setOutputCostPerMTokens(new BigDecimal("0.50"));

        AiModel expensiveModel = new AiModel(2L, "expensive-model");

        expensiveModel.setOutputCostPerMTokens(new BigDecimal("30.00"));

        Map<Long, AiModel> modelMap = Map.of(100L, cheapModel, 200L, expensiveModel);

        AiGatewayRoutingContext context = new AiGatewayRoutingContext(
            Map.of(), modelMap, 0.9, Map.of(), Map.of());

        AiGatewayModelDeployment selected = strategy.selectDeployment(
            List.of(cheapDeployment, expensiveDeployment), context);

        assertEquals(Long.valueOf(200L), selected.getModelId());
    }

    @Test
    void testIntelligentBalancedBelowThresholdRoutesCheapest() {
        IntelligentRoutingStrategy strategy = new IntelligentRoutingStrategy(
            AiGatewayRoutingStrategyType.INTELLIGENT_BALANCED);

        AiGatewayModelDeployment cheapDeployment = new AiGatewayModelDeployment(1L, 100L);
        AiGatewayModelDeployment expensiveDeployment = new AiGatewayModelDeployment(1L, 200L);

        AiModel cheapModel = new AiModel(1L, "cheap-model");

        cheapModel.setOutputCostPerMTokens(new BigDecimal("0.50"));

        AiModel expensiveModel = new AiModel(2L, "expensive-model");

        expensiveModel.setOutputCostPerMTokens(new BigDecimal("30.00"));

        Map<Long, AiModel> modelMap = Map.of(100L, cheapModel, 200L, expensiveModel);

        AiGatewayRoutingContext context = new AiGatewayRoutingContext(
            Map.of(), modelMap, 0.4, Map.of(), Map.of());

        AiGatewayModelDeployment selected = strategy.selectDeployment(
            List.of(cheapDeployment, expensiveDeployment), context);

        assertEquals(Long.valueOf(100L), selected.getModelId());
    }

    @Test
    void testIntelligentBalancedAboveThresholdRoutesMostCapable() {
        IntelligentRoutingStrategy strategy = new IntelligentRoutingStrategy(
            AiGatewayRoutingStrategyType.INTELLIGENT_BALANCED);

        AiGatewayModelDeployment cheapDeployment = new AiGatewayModelDeployment(1L, 100L);
        AiGatewayModelDeployment expensiveDeployment = new AiGatewayModelDeployment(1L, 200L);

        AiModel cheapModel = new AiModel(1L, "cheap-model");

        cheapModel.setOutputCostPerMTokens(new BigDecimal("0.50"));

        AiModel expensiveModel = new AiModel(2L, "expensive-model");

        expensiveModel.setOutputCostPerMTokens(new BigDecimal("30.00"));

        Map<Long, AiModel> modelMap = Map.of(100L, cheapModel, 200L, expensiveModel);

        AiGatewayRoutingContext context = new AiGatewayRoutingContext(
            Map.of(), modelMap, 0.6, Map.of(), Map.of());

        AiGatewayModelDeployment selected = strategy.selectDeployment(
            List.of(cheapDeployment, expensiveDeployment), context);

        assertEquals(Long.valueOf(200L), selected.getModelId());
    }

    @Test
    void testIntelligentQualityLowThresholdRoutesMostCapable() {
        IntelligentRoutingStrategy strategy = new IntelligentRoutingStrategy(
            AiGatewayRoutingStrategyType.INTELLIGENT_QUALITY);

        AiGatewayModelDeployment cheapDeployment = new AiGatewayModelDeployment(1L, 100L);
        AiGatewayModelDeployment expensiveDeployment = new AiGatewayModelDeployment(1L, 200L);

        AiModel cheapModel = new AiModel(1L, "cheap-model");

        cheapModel.setOutputCostPerMTokens(new BigDecimal("0.50"));

        AiModel expensiveModel = new AiModel(2L, "expensive-model");

        expensiveModel.setOutputCostPerMTokens(new BigDecimal("30.00"));

        Map<Long, AiModel> modelMap = Map.of(100L, cheapModel, 200L, expensiveModel);

        AiGatewayRoutingContext context = new AiGatewayRoutingContext(
            Map.of(), modelMap, 0.4, Map.of(), Map.of());

        AiGatewayModelDeployment selected = strategy.selectDeployment(
            List.of(cheapDeployment, expensiveDeployment), context);

        assertEquals(Long.valueOf(200L), selected.getModelId());
    }

    @Test
    void testIntelligentBalancedThreeTiersMidScoreRoutesMiddleTier() {
        IntelligentRoutingStrategy strategy = new IntelligentRoutingStrategy(
            AiGatewayRoutingStrategyType.INTELLIGENT_BALANCED);

        AiGatewayModelDeployment basicDeployment = new AiGatewayModelDeployment(1L, 100L);
        AiGatewayModelDeployment standardDeployment = new AiGatewayModelDeployment(1L, 200L);
        AiGatewayModelDeployment frontierDeployment = new AiGatewayModelDeployment(1L, 300L);

        AiModel basicModel = new AiModel(1L, "basic-model");

        basicModel.setOutputCostPerMTokens(new BigDecimal("0.05"));

        AiModel standardModel = new AiModel(2L, "standard-model");

        standardModel.setOutputCostPerMTokens(new BigDecimal("1.75"));

        AiModel frontierModel = new AiModel(3L, "frontier-model");

        frontierModel.setOutputCostPerMTokens(new BigDecimal("6.00"));

        Map<Long, AiModel> modelMap = Map.of(100L, basicModel, 200L, standardModel, 300L, frontierModel);

        AiGatewayRoutingContext context = new AiGatewayRoutingContext(
            Map.of(), modelMap, 0.5, Map.of(), Map.of());

        AiGatewayModelDeployment selected = strategy.selectDeployment(
            List.of(basicDeployment, standardDeployment, frontierDeployment), context);

        assertEquals(Long.valueOf(200L), selected.getModelId());
    }

    @Test
    void testIntelligentQualityThreeTiersHighScoreRoutesFrontier() {
        IntelligentRoutingStrategy strategy = new IntelligentRoutingStrategy(
            AiGatewayRoutingStrategyType.INTELLIGENT_QUALITY);

        AiGatewayModelDeployment basicDeployment = new AiGatewayModelDeployment(1L, 100L);
        AiGatewayModelDeployment standardDeployment = new AiGatewayModelDeployment(1L, 200L);
        AiGatewayModelDeployment frontierDeployment = new AiGatewayModelDeployment(1L, 300L);

        AiModel basicModel = new AiModel(1L, "basic-model");

        basicModel.setOutputCostPerMTokens(new BigDecimal("0.05"));

        AiModel standardModel = new AiModel(2L, "standard-model");

        standardModel.setOutputCostPerMTokens(new BigDecimal("1.75"));

        AiModel frontierModel = new AiModel(3L, "frontier-model");

        frontierModel.setOutputCostPerMTokens(new BigDecimal("6.00"));

        Map<Long, AiModel> modelMap = Map.of(100L, basicModel, 200L, standardModel, 300L, frontierModel);

        AiGatewayRoutingContext context = new AiGatewayRoutingContext(
            Map.of(), modelMap, 0.95, Map.of(), Map.of());

        AiGatewayModelDeployment selected = strategy.selectDeployment(
            List.of(basicDeployment, standardDeployment, frontierDeployment), context);

        assertEquals(Long.valueOf(300L), selected.getModelId());
    }

    @Test
    void testIntelligentScoreOneRoutesMostCapableTier() {
        IntelligentRoutingStrategy strategy = new IntelligentRoutingStrategy(
            AiGatewayRoutingStrategyType.INTELLIGENT_COST);

        AiGatewayModelDeployment basicDeployment = new AiGatewayModelDeployment(1L, 100L);
        AiGatewayModelDeployment frontierDeployment = new AiGatewayModelDeployment(1L, 300L);

        AiModel basicModel = new AiModel(1L, "basic-model");

        basicModel.setOutputCostPerMTokens(new BigDecimal("0.05"));

        AiModel frontierModel = new AiModel(3L, "frontier-model");

        frontierModel.setOutputCostPerMTokens(new BigDecimal("6.00"));

        Map<Long, AiModel> modelMap = Map.of(100L, basicModel, 300L, frontierModel);

        AiGatewayRoutingContext context = new AiGatewayRoutingContext(
            Map.of(), modelMap, 1.0, Map.of(), Map.of());

        AiGatewayModelDeployment selected = strategy.selectDeployment(
            List.of(basicDeployment, frontierDeployment), context);

        assertEquals(Long.valueOf(300L), selected.getModelId());
    }

    @Test
    void testIntelligentBalancedFiveTiersMapsScoreAcrossAllTiers() {
        IntelligentRoutingStrategy strategy = new IntelligentRoutingStrategy(
            AiGatewayRoutingStrategyType.INTELLIGENT_BALANCED);

        AiGatewayModelDeployment basicDeployment = new AiGatewayModelDeployment(1L, 100L);
        AiGatewayModelDeployment efficientDeployment = new AiGatewayModelDeployment(1L, 200L);
        AiGatewayModelDeployment standardDeployment = new AiGatewayModelDeployment(1L, 300L);
        AiGatewayModelDeployment advancedDeployment = new AiGatewayModelDeployment(1L, 400L);
        AiGatewayModelDeployment frontierDeployment = new AiGatewayModelDeployment(1L, 500L);

        AiModel basicModel = new AiModel(1L, "basic-model");

        basicModel.setOutputCostPerMTokens(new BigDecimal("0.05"));

        AiModel efficientModel = new AiModel(2L, "efficient-model");

        efficientModel.setOutputCostPerMTokens(new BigDecimal("0.50"));

        AiModel standardModel = new AiModel(3L, "standard-model");

        standardModel.setOutputCostPerMTokens(new BigDecimal("1.75"));

        AiModel advancedModel = new AiModel(4L, "advanced-model");

        advancedModel.setOutputCostPerMTokens(new BigDecimal("3.00"));

        AiModel frontierModel = new AiModel(5L, "frontier-model");

        frontierModel.setOutputCostPerMTokens(new BigDecimal("6.00"));

        Map<Long, AiModel> modelMap = Map.of(
            100L, basicModel, 200L, efficientModel, 300L, standardModel, 400L, advancedModel, 500L, frontierModel);

        List<AiGatewayModelDeployment> deployments = List.of(
            basicDeployment, efficientDeployment, standardDeployment, advancedDeployment, frontierDeployment);

        // BALANCED is linear: floor(score * 5) selects the tier index. 0.1 -> BASIC(0), 0.5 -> STANDARD(2),
        // 0.9 -> FRONTIER(4) — proving the score spreads across all five present tiers, not just the extremes.
        assertEquals(
            Long.valueOf(100L),
            strategy.selectDeployment(deployments, fiveTierContext(modelMap, 0.1))
                .getModelId());
        assertEquals(
            Long.valueOf(300L),
            strategy.selectDeployment(deployments, fiveTierContext(modelMap, 0.5))
                .getModelId());
        assertEquals(
            Long.valueOf(500L),
            strategy.selectDeployment(deployments, fiveTierContext(modelMap, 0.9))
                .getModelId());
    }

    private static AiGatewayRoutingContext fiveTierContext(Map<Long, AiModel> modelMap, double score) {
        return new AiGatewayRoutingContext(Map.of(), modelMap, score, Map.of(), Map.of());
    }
}

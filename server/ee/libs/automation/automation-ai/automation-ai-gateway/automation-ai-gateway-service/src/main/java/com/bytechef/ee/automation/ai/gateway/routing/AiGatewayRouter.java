/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.routing;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModelDeployment;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRoutingStrategyType;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiGatewayRouter {

    private static final AiGatewayRoutingStrategy COST_OPTIMIZED_STRATEGY =
        new CostOptimizedRoutingStrategy();
    private static final AiGatewayRoutingStrategy INTELLIGENT_BALANCED_STRATEGY =
        new IntelligentRoutingStrategy(AiGatewayRoutingStrategyType.INTELLIGENT_BALANCED);
    private static final AiGatewayRoutingStrategy INTELLIGENT_COST_STRATEGY =
        new IntelligentRoutingStrategy(AiGatewayRoutingStrategyType.INTELLIGENT_COST);
    private static final AiGatewayRoutingStrategy INTELLIGENT_QUALITY_STRATEGY =
        new IntelligentRoutingStrategy(AiGatewayRoutingStrategyType.INTELLIGENT_QUALITY);
    private static final AiGatewayRoutingStrategy LATENCY_OPTIMIZED_STRATEGY =
        new LatencyOptimizedRoutingStrategy();
    private static final AiGatewayRoutingStrategy PRIORITY_FALLBACK_STRATEGY =
        new PriorityFallbackRoutingStrategy();
    private static final AiGatewayRoutingStrategy SIMPLE_STRATEGY = new SimpleRoutingStrategy();
    private static final AiGatewayRoutingStrategy TAG_BASED_STRATEGY = new TagBasedRoutingStrategy();
    private static final AiGatewayRoutingStrategy WEIGHTED_RANDOM_STRATEGY =
        new WeightedRandomRoutingStrategy();

    public AiGatewayModelDeployment route(
        AiGatewayRoutingStrategyType strategyType,
        List<AiGatewayModelDeployment> deployments,
        AiGatewayRoutingContext context) {

        List<AiGatewayModelDeployment> enabledDeployments = deployments.stream()
            .filter(AiGatewayModelDeployment::isEnabled)
            .toList();

        if (enabledDeployments.isEmpty()) {
            throw new IllegalStateException("No enabled deployments available");
        }

        AiGatewayRoutingStrategy strategy = switch (strategyType) {
            case COST_OPTIMIZED -> COST_OPTIMIZED_STRATEGY;
            case INTELLIGENT_BALANCED -> INTELLIGENT_BALANCED_STRATEGY;
            case INTELLIGENT_COST -> INTELLIGENT_COST_STRATEGY;
            case INTELLIGENT_QUALITY -> INTELLIGENT_QUALITY_STRATEGY;
            case LATENCY_OPTIMIZED -> LATENCY_OPTIMIZED_STRATEGY;
            case PRIORITY_FALLBACK -> PRIORITY_FALLBACK_STRATEGY;
            case SIMPLE -> SIMPLE_STRATEGY;
            case TAG_BASED -> TAG_BASED_STRATEGY;
            case WEIGHTED_RANDOM -> WEIGHTED_RANDOM_STRATEGY;
        };

        return strategy.selectDeployment(enabledDeployments, context);
    }
}

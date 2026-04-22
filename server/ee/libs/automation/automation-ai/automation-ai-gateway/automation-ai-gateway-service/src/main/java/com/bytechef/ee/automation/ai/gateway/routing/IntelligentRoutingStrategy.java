/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.routing;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModel;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModelDeployment;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRoutingStrategyType;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Threshold-based routing that classifies each request by estimated prompt complexity and routes it to either the
 * cheapest or most capable (expensive) model. Complexity is scored as one of three discrete values (0.2 for simple, 0.5
 * for moderate, 0.8 for complex prompts) and compared against a threshold that varies by axis. Requests at or above the
 * threshold go to the capable (expensive) model; below go to the cheap model.
 * <ul>
 * <li>INTELLIGENT_COST: threshold 0.7 — only complex prompts go to expensive models</li>
 * <li>INTELLIGENT_BALANCED: threshold 0.5 — moderate and complex prompts go to capable models</li>
 * <li>INTELLIGENT_QUALITY: threshold 0.3 — most prompts go to capable models</li>
 * </ul>
 *
 * This is a per-request binary decision, not a probabilistic traffic split. The actual distribution of traffic across
 * models depends on the complexity distribution of incoming prompts.
 *
 * @version ee
 */
class IntelligentRoutingStrategy implements AiGatewayRoutingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(IntelligentRoutingStrategy.class);

    private final AiGatewayRoutingStrategyType axis;

    IntelligentRoutingStrategy(AiGatewayRoutingStrategyType axis) {
        this.axis = axis;
    }

    @Override
    public AiGatewayModelDeployment selectDeployment(
        List<AiGatewayModelDeployment> deployments, AiGatewayRoutingContext context) {

        if (deployments.size() == 1) {
            return deployments.getFirst();
        }

        List<AiGatewayModelDeployment> sortedByCost = deployments.stream()
            .sorted(Comparator.comparing(deployment -> {
                AiGatewayModel model = context.modelMap()
                    .get(deployment.getModelId());

                if (model == null || model.getOutputCostPerMTokens() == null) {
                    logger.debug("No pricing data for deployment (modelId={}), deprioritizing in intelligent routing",
                        deployment.getModelId());

                    return BigDecimal.valueOf(Long.MAX_VALUE);
                }

                return model.getOutputCostPerMTokens();
            }))
            .toList();

        double complexityScore = context.promptComplexityScore();

        double cheapThreshold = switch (axis) {
            case INTELLIGENT_COST -> 0.7;
            case INTELLIGENT_BALANCED -> 0.5;
            case INTELLIGENT_QUALITY -> 0.3;
            default -> throw new IllegalArgumentException(
                "IntelligentRoutingStrategy does not support axis: " + axis);
        };

        if (complexityScore < cheapThreshold) {
            return sortedByCost.getFirst();
        }

        return sortedByCost.getLast();
    }
}

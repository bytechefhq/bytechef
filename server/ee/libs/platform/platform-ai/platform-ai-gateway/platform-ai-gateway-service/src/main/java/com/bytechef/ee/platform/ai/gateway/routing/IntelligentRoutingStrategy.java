/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.routing;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModelDeployment;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModelTier;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingStrategyType;
import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntelligentRoutingStrategy implements AiGatewayRoutingStrategy {

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

        List<AiGatewayModelTier> presentTiers = deployments.stream()
            .map(deployment -> tierOf(deployment, context))
            .distinct()
            .sorted(Comparator.comparingInt(Enum::ordinal))
            .toList();

        double effective = transform(context.promptComplexityScore());

        int index = (int) Math.floor(effective * presentTiers.size());

        index = Math.max(0, Math.min(index, presentTiers.size() - 1));

        AiGatewayModelTier selectedTier = presentTiers.get(index);

        return deployments.stream()
            .filter(deployment -> tierOf(deployment, context) == selectedTier)
            .min(Comparator
                .comparing(
                    (AiGatewayModelDeployment deployment) -> outputCost(deployment, context),
                    Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AiGatewayModelDeployment::getModelId))
            .orElseGet(deployments::getFirst);
    }

    private double transform(double score) {
        return switch (axis) {
            case INTELLIGENT_COST -> score * score;
            case INTELLIGENT_BALANCED -> score;
            case INTELLIGENT_QUALITY -> 1.0 - (1.0 - score) * (1.0 - score);
            default -> throw new IllegalArgumentException(
                "IntelligentRoutingStrategy does not support axis: " + axis);
        };
    }

    private static AiGatewayModelTier tierOf(AiGatewayModelDeployment deployment, AiGatewayRoutingContext context) {
        return AiGatewayModelTier.classify(outputCost(deployment, context));
    }

    private static BigDecimal outputCost(AiGatewayModelDeployment deployment, AiGatewayRoutingContext context) {
        AiModel model = context.modelMap()
            .get(deployment.getModelId());

        if (model == null) {
            return null;
        }

        return model.getOutputCostPerMTokens();
    }
}

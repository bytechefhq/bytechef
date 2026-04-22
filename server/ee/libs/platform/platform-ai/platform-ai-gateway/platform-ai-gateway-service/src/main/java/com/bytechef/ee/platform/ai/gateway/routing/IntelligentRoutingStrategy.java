/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.routing;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModel;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModelDeployment;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModelTier;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingStrategyType;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Tier-based intelligent routing. Classifies each candidate deployment's model into an {@link AiGatewayModelTier} cost
 * tier, then maps the request's 0.0–1.0 prompt complexity score onto the tiers actually present in the policy. The
 * per-axis score transform skews the mapping:
 * <ul>
 * <li>INTELLIGENT_COST: complexity squared — most prompts go to cheaper tiers</li>
 * <li>INTELLIGENT_BALANCED: complexity — linear spread across tiers</li>
 * <li>INTELLIGENT_QUALITY: 1 - (1 - complexity)squared — most prompts go to capable tiers</li>
 * </ul>
 * With exactly two present tiers these transforms reproduce the legacy 0.7 / 0.5 / 0.3 thresholds.
 *
 * @version ee
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

        // AiGatewayModelTier is declared capability-ascending, so ordinal sort yields cheap -> capable order.
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
        AiGatewayModel model = context.modelMap()
            .get(deployment.getModelId());

        if (model == null) {
            return null;
        }

        return model.getOutputCostPerMTokens();
    }
}

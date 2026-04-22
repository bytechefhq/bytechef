/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.routing;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModel;
import java.util.Map;

/**
 * @version ee
 */
public record AiGatewayRoutingContext(
    Map<Long, Double> averageLatencyByModelId,
    Map<Long, AiGatewayModel> modelMap,
    double promptComplexityScore,
    Map<Long, String> providerTypeByModelId,
    Map<String, String> tags) {

    public AiGatewayRoutingContext(
        Map<Long, Double> averageLatencyByModelId,
        Map<Long, AiGatewayModel> modelMap,
        double promptComplexityScore,
        Map<Long, String> providerTypeByModelId,
        Map<String, String> tags) {

        this.averageLatencyByModelId = Map.copyOf(averageLatencyByModelId);
        this.modelMap = Map.copyOf(modelMap);
        this.promptComplexityScore = promptComplexityScore;
        this.providerTypeByModelId = Map.copyOf(providerTypeByModelId);
        this.tags = Map.copyOf(tags);
    }

    public AiGatewayRoutingContext(Map<Long, AiGatewayModel> modelMap) {
        this(Map.of(), modelMap, 0.5, Map.of(), Map.of());
    }

    public AiGatewayRoutingContext(
        Map<Long, Double> averageLatencyByModelId, Map<Long, AiGatewayModel> modelMap) {

        this(averageLatencyByModelId, modelMap, 0.5, Map.of(), Map.of());
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.routing;

import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import java.util.Map;

/**
 * @version ee
 */
public record AiGatewayRoutingContext(
    Map<Long, Double> averageLatencyByModelId,
    Map<Long, AiModel> modelMap,
    double promptComplexityScore,
    Map<Long, String> providerTypeByModelId,
    Map<String, String> tags) {

    public AiGatewayRoutingContext(
        Map<Long, Double> averageLatencyByModelId,
        Map<Long, AiModel> modelMap,
        double promptComplexityScore,
        Map<Long, String> providerTypeByModelId,
        Map<String, String> tags) {

        this.averageLatencyByModelId = Map.copyOf(averageLatencyByModelId);
        this.modelMap = Map.copyOf(modelMap);
        this.promptComplexityScore = promptComplexityScore;
        this.providerTypeByModelId = Map.copyOf(providerTypeByModelId);
        this.tags = Map.copyOf(tags);
    }

    public AiGatewayRoutingContext(Map<Long, AiModel> modelMap) {
        this(Map.of(), modelMap, 0.5, Map.of(), Map.of());
    }

    public AiGatewayRoutingContext(
        Map<Long, Double> averageLatencyByModelId, Map<Long, AiModel> modelMap) {

        this(averageLatencyByModelId, modelMap, 0.5, Map.of(), Map.of());
    }
}

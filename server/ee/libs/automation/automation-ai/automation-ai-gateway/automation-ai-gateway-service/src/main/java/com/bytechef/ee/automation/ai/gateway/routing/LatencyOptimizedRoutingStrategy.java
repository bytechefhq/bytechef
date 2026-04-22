/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.routing;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModelDeployment;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Selects the deployment with the lowest observed average latency from recent request logs. Falls back to first
 * deployment if no latency data is available.
 *
 * @version ee
 */
class LatencyOptimizedRoutingStrategy implements AiGatewayRoutingStrategy {

    @Override
    public AiGatewayModelDeployment selectDeployment(
        List<AiGatewayModelDeployment> deployments, AiGatewayRoutingContext context) {

        Map<Long, Double> latencyMap = context.averageLatencyByModelId();

        if (latencyMap.isEmpty()) {
            return deployments.getFirst();
        }

        return deployments.stream()
            .min(Comparator.comparingDouble(deployment -> {
                Double latency = latencyMap.get(deployment.getModelId());

                if (latency == null) {
                    return Double.MAX_VALUE;
                }

                return latency;
            }))
            .orElse(deployments.getFirst());
    }
}

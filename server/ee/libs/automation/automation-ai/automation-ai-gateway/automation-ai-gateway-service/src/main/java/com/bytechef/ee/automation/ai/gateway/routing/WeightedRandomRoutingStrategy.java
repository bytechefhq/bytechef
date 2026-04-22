/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.routing;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModelDeployment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Selects a deployment proportional to its weight using weighted random selection.
 *
 * @version ee
 */
@SuppressFBWarnings({
    "EI", "PREDICTABLE_RANDOM"
})
class WeightedRandomRoutingStrategy implements AiGatewayRoutingStrategy {

    @Override
    public AiGatewayModelDeployment selectDeployment(
        List<AiGatewayModelDeployment> deployments, AiGatewayRoutingContext context) {

        int totalWeight = deployments.stream()
            .mapToInt(AiGatewayModelDeployment::getWeight)
            .sum();

        if (totalWeight <= 0) {
            return deployments.getFirst();
        }

        int threshold = ThreadLocalRandom.current()
            .nextInt(totalWeight);

        for (AiGatewayModelDeployment deployment : deployments) {
            threshold -= deployment.getWeight();

            if (threshold < 0) {
                return deployment;
            }
        }

        return deployments.getLast();
    }
}

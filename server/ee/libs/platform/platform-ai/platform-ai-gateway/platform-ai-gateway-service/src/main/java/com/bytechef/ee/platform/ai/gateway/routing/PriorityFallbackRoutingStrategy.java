/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.routing;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModelDeployment;
import java.util.Comparator;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PriorityFallbackRoutingStrategy implements AiGatewayRoutingStrategy {

    @Override
    public AiGatewayModelDeployment selectDeployment(
        List<AiGatewayModelDeployment> deployments, AiGatewayRoutingContext context) {
        return deployments.stream()
            .min(Comparator.comparingInt(AiGatewayModelDeployment::getPriorityOrder))
            .orElse(deployments.getFirst());
    }
}

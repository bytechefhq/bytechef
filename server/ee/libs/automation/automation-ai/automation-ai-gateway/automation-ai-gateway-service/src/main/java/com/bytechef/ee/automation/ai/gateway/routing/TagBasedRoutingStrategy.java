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

/**
 * Selects the highest-priority deployment. Policy matching by tags is handled at the facade level before this strategy
 * is invoked.
 *
 * @version ee
 */
class TagBasedRoutingStrategy implements AiGatewayRoutingStrategy {

    @Override
    public AiGatewayModelDeployment selectDeployment(
        List<AiGatewayModelDeployment> deployments, AiGatewayRoutingContext context) {

        return deployments.stream()
            .sorted(Comparator.comparingInt(AiGatewayModelDeployment::getPriorityOrder))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No deployments available"));
    }
}

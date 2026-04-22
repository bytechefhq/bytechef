/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.routing;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModelDeployment;
import java.util.List;

/**
 * Selects the first deployment from the list. Enabled-only filtering is performed upstream by the router.
 *
 * @version ee
 */
class SimpleRoutingStrategy implements AiGatewayRoutingStrategy {

    @Override
    public AiGatewayModelDeployment selectDeployment(
        List<AiGatewayModelDeployment> deployments, AiGatewayRoutingContext context) {

        return deployments.getFirst();
    }
}

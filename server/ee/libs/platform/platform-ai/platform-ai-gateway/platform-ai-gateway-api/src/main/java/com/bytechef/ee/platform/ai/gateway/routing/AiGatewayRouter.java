/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.routing;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModelDeployment;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingStrategyType;
import java.util.List;

/**
 * @version ee
 */
public interface AiGatewayRouter {

    AiGatewayModelDeployment route(
        AiGatewayRoutingStrategyType strategyType,
        List<AiGatewayModelDeployment> deployments,
        AiGatewayRoutingContext context);
}

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
 * @version ee
 */
public interface AiGatewayRoutingStrategy {

    AiGatewayModelDeployment selectDeployment(
        List<AiGatewayModelDeployment> deployments, AiGatewayRoutingContext context);
}

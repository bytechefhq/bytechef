/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingPolicy;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewayRoutingPolicyFacade {

    AiGatewayRoutingPolicy getRoutingPolicy(long id);

    List<AiGatewayRoutingPolicy> getRoutingPolicies();

    AiGatewayRoutingPolicy create(AiGatewayRoutingPolicy policy);

    void delete(long id);

    AiGatewayRoutingPolicy update(AiGatewayRoutingPolicy policy);
}

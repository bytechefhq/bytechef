/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRoutingPolicy;
import java.util.Collection;
import java.util.List;

/**
 * @version ee
 */
public interface AiGatewayRoutingPolicyService {

    AiGatewayRoutingPolicy create(AiGatewayRoutingPolicy policy);

    void delete(long id);

    AiGatewayRoutingPolicy getRoutingPolicy(long id);

    AiGatewayRoutingPolicy getRoutingPolicyByName(String name);

    List<AiGatewayRoutingPolicy> getRoutingPolicies();

    List<AiGatewayRoutingPolicy> getRoutingPolicies(Collection<Long> ids);

    AiGatewayRoutingPolicy update(AiGatewayRoutingPolicy policy);
}

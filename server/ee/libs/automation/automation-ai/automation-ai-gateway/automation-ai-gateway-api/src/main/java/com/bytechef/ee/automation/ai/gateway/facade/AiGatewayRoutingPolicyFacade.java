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
 * Facade for managing AI LLM Gateway routing policies. Hosts the authorization guards so they apply to every caller of
 * the facade rather than only the GraphQL entry point, and keeps them off the shared
 * {@code AiGatewayRoutingPolicyService} which the gateway data plane relies on.
 *
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

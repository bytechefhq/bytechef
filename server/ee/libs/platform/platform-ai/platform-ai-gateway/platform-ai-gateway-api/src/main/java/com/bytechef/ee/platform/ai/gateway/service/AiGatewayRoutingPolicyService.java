/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingPolicy;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewayRoutingPolicyService {

    AiGatewayRoutingPolicy create(AiGatewayRoutingPolicy policy);

    void delete(long id);

    List<AiGatewayRoutingPolicy> getDefaultRoutingPolicies();

    AiGatewayRoutingPolicy getRoutingPolicy(long id);

    AiGatewayRoutingPolicy getRoutingPolicyByName(String name);

    List<AiGatewayRoutingPolicy> getRoutingPolicies();

    List<AiGatewayRoutingPolicy> getRoutingPolicies(Collection<Long> ids);

    List<AiGatewayRoutingPolicy> getRoutingPoliciesByWorkspaceId(long workspaceId);

    AiGatewayRoutingPolicy update(AiGatewayRoutingPolicy policy);

    void updateWorkspaceId(long id, @Nullable Long workspaceId);
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingPolicy;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceAiGatewayRoutingPolicyService {

    List<AiGatewayRoutingPolicy> getWorkspaceRoutingPolicies(Long workspaceId);

    void assignRoutingPolicyToWorkspace(Long routingPolicyId, Long workspaceId);

    void removeRoutingPolicyFromWorkspace(Long routingPolicyId);
}

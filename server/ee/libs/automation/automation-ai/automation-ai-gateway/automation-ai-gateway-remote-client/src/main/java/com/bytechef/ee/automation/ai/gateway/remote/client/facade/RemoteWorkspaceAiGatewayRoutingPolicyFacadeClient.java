/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.remote.client.facade;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRoutingPolicy;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRoutingStrategyType;
import com.bytechef.ee.automation.ai.gateway.facade.WorkspaceAiGatewayRoutingPolicyFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteWorkspaceAiGatewayRoutingPolicyFacadeClient
    implements WorkspaceAiGatewayRoutingPolicyFacade {

    @Override
    public AiGatewayRoutingPolicy createWorkspaceRoutingPolicy(
        Long workspaceId, String name, AiGatewayRoutingStrategyType strategy,
        String fallbackModel, String config) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteWorkspaceRoutingPolicy(Long workspaceId, Long routingPolicyId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AiGatewayRoutingPolicy> getWorkspaceRoutingPolicies(Long workspaceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AiGatewayRoutingPolicy updateWorkspaceRoutingPolicy(
        Long workspaceId, Long id, String name, AiGatewayRoutingStrategyType strategy,
        String fallbackModel, String config, Boolean enabled) {

        throw new UnsupportedOperationException();
    }
}

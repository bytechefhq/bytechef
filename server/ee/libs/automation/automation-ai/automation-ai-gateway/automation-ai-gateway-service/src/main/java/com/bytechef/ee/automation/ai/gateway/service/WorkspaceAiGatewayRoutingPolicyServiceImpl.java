/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingPolicy;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayRoutingPolicyService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link WorkspaceAiGatewayRoutingPolicyService} interface.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class WorkspaceAiGatewayRoutingPolicyServiceImpl implements WorkspaceAiGatewayRoutingPolicyService {

    private final AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService;

    public WorkspaceAiGatewayRoutingPolicyServiceImpl(AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService) {
        this.aiGatewayRoutingPolicyService = aiGatewayRoutingPolicyService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayRoutingPolicy> getWorkspaceRoutingPolicies(Long workspaceId) {
        return aiGatewayRoutingPolicyService.getRoutingPoliciesByWorkspaceId(workspaceId);
    }

    @Override
    public void assignRoutingPolicyToWorkspace(Long routingPolicyId, Long workspaceId) {
        aiGatewayRoutingPolicyService.updateWorkspaceId(routingPolicyId, workspaceId);
    }

    @Override
    public void removeRoutingPolicyFromWorkspace(Long routingPolicyId) {
        aiGatewayRoutingPolicyService.updateWorkspaceId(routingPolicyId, null);
    }
}

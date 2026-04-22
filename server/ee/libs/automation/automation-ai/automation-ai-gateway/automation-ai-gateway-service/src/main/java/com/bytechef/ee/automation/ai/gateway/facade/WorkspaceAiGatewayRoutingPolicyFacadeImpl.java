/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRoutingPolicy;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRoutingStrategyType;
import com.bytechef.ee.automation.ai.gateway.domain.WorkspaceAiGatewayRoutingPolicy;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayRoutingPolicyService;
import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayRoutingPolicyService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link WorkspaceAiGatewayRoutingPolicyFacade} interface that handles workspace AI LLM Gateway
 * routing policy operations.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class WorkspaceAiGatewayRoutingPolicyFacadeImpl implements WorkspaceAiGatewayRoutingPolicyFacade {

    private final AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService;
    private final WorkspaceAiGatewayRoutingPolicyService workspaceAiGatewayRoutingPolicyService;

    @SuppressFBWarnings("EI")
    public WorkspaceAiGatewayRoutingPolicyFacadeImpl(
        AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService,
        WorkspaceAiGatewayRoutingPolicyService workspaceAiGatewayRoutingPolicyService) {

        this.aiGatewayRoutingPolicyService = aiGatewayRoutingPolicyService;
        this.workspaceAiGatewayRoutingPolicyService = workspaceAiGatewayRoutingPolicyService;
    }

    @Override
    public AiGatewayRoutingPolicy createWorkspaceRoutingPolicy(
        Long workspaceId, String name, AiGatewayRoutingStrategyType strategy,
        String fallbackModel, String config) {

        AiGatewayRoutingPolicy policy = new AiGatewayRoutingPolicy(name, strategy);

        policy.setFallbackModel(fallbackModel);
        policy.setConfig(config);

        policy = aiGatewayRoutingPolicyService.create(policy);

        workspaceAiGatewayRoutingPolicyService.assignRoutingPolicyToWorkspace(policy.getId(), workspaceId);

        return policy;
    }

    @Override
    public void deleteWorkspaceRoutingPolicy(Long workspaceId, Long routingPolicyId) {
        verifyWorkspaceOwnership(workspaceId, routingPolicyId);

        workspaceAiGatewayRoutingPolicyService.removeRoutingPolicyFromWorkspace(routingPolicyId);

        aiGatewayRoutingPolicyService.delete(routingPolicyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayRoutingPolicy> getWorkspaceRoutingPolicies(Long workspaceId) {
        List<Long> routingPolicyIds =
            workspaceAiGatewayRoutingPolicyService.getWorkspaceRoutingPolicies(workspaceId)
                .stream()
                .map(WorkspaceAiGatewayRoutingPolicy::getRoutingPolicyId)
                .toList();

        if (routingPolicyIds.isEmpty()) {
            return List.of();
        }

        return aiGatewayRoutingPolicyService.getRoutingPolicies(routingPolicyIds);
    }

    @Override
    public AiGatewayRoutingPolicy updateWorkspaceRoutingPolicy(
        Long workspaceId, Long id, String name, AiGatewayRoutingStrategyType strategy,
        String fallbackModel, String config, Boolean enabled) {

        verifyWorkspaceOwnership(workspaceId, id);

        AiGatewayRoutingPolicy policy = aiGatewayRoutingPolicyService.getRoutingPolicy(id);

        if (name != null) {
            policy.setName(name);
        }

        if (strategy != null) {
            policy.setStrategy(strategy);
        }

        if (fallbackModel != null) {
            policy.setFallbackModel(fallbackModel);
        }

        if (config != null) {
            policy.setConfig(config);
        }

        if (enabled != null) {
            policy.setEnabled(enabled);
        }

        return aiGatewayRoutingPolicyService.update(policy);
    }

    private void verifyWorkspaceOwnership(Long workspaceId, Long routingPolicyId) {
        boolean owned = workspaceAiGatewayRoutingPolicyService.getWorkspaceRoutingPolicies(workspaceId)
            .stream()
            .anyMatch(workspaceRoutingPolicy -> workspaceRoutingPolicy.getRoutingPolicyId()
                .equals(routingPolicyId));

        if (!owned) {
            throw new IllegalArgumentException(
                "Routing policy " + routingPolicyId + " does not belong to workspace " + workspaceId);
        }
    }
}

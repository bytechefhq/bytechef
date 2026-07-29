/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayRoutingPolicyService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingPolicy;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingStrategyType;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayRoutingPolicyService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
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
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public void deleteWorkspaceRoutingPolicy(Long workspaceId, Long routingPolicyId) {
        verifyWorkspaceOwnership(workspaceId, routingPolicyId);

        workspaceAiGatewayRoutingPolicyService.removeRoutingPolicyFromWorkspace(routingPolicyId);

        aiGatewayRoutingPolicyService.delete(routingPolicyId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public List<AiGatewayRoutingPolicy> getWorkspaceRoutingPolicies(Long workspaceId) {
        return workspaceAiGatewayRoutingPolicyService.getWorkspaceRoutingPolicies(workspaceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
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
            .anyMatch(workspaceRoutingPolicy -> Objects.equals(workspaceRoutingPolicy.getId(), routingPolicyId));

        if (!owned) {
            throw new IllegalArgumentException(
                "Routing policy " + routingPolicyId + " does not belong to workspace " + workspaceId);
        }
    }
}

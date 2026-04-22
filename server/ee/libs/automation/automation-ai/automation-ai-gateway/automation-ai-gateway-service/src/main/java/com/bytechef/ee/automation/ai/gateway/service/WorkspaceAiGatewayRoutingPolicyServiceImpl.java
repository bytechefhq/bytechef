/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.WorkspaceAiGatewayRoutingPolicy;
import com.bytechef.ee.automation.ai.gateway.repository.WorkspaceAiGatewayRoutingPolicyRepository;
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

    private final WorkspaceAiGatewayRoutingPolicyRepository workspaceAiGatewayRoutingPolicyRepository;

    public WorkspaceAiGatewayRoutingPolicyServiceImpl(
        WorkspaceAiGatewayRoutingPolicyRepository workspaceAiGatewayRoutingPolicyRepository) {

        this.workspaceAiGatewayRoutingPolicyRepository = workspaceAiGatewayRoutingPolicyRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceAiGatewayRoutingPolicy> getWorkspaceRoutingPolicies(Long workspaceId) {
        return workspaceAiGatewayRoutingPolicyRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    public void assignRoutingPolicyToWorkspace(Long routingPolicyId, Long workspaceId) {
        WorkspaceAiGatewayRoutingPolicy existing =
            workspaceAiGatewayRoutingPolicyRepository.findByWorkspaceIdAndRoutingPolicyId(
                workspaceId, routingPolicyId);

        if (existing == null) {
            WorkspaceAiGatewayRoutingPolicy workspaceAiGatewayRoutingPolicy =
                new WorkspaceAiGatewayRoutingPolicy(routingPolicyId, workspaceId);

            workspaceAiGatewayRoutingPolicyRepository.save(workspaceAiGatewayRoutingPolicy);
        }
    }

    @Override
    public void removeRoutingPolicyFromWorkspace(Long routingPolicyId) {
        List<WorkspaceAiGatewayRoutingPolicy> existingRelationships =
            workspaceAiGatewayRoutingPolicyRepository.findByRoutingPolicyId(routingPolicyId);

        if (!existingRelationships.isEmpty()) {
            workspaceAiGatewayRoutingPolicyRepository.deleteAll(existingRelationships);
        }
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.WorkspaceAiGatewayRoutingPolicy;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * Repository interface for managing {@link WorkspaceAiGatewayRoutingPolicy} entities.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceAiGatewayRoutingPolicyRepository
    extends ListCrudRepository<WorkspaceAiGatewayRoutingPolicy, Long> {

    /**
     * Find all workspace AI LLM Gateway routing policy relationships by workspace ID.
     *
     * @param workspaceId the workspace ID
     * @return list of workspace AI LLM Gateway routing policy relationships
     */
    List<WorkspaceAiGatewayRoutingPolicy> findAllByWorkspaceId(Long workspaceId);

    /**
     * Find all workspace AI LLM Gateway routing policy relationships by routing policy ID.
     *
     * @param routingPolicyId the routing policy ID
     * @return list of workspace AI LLM Gateway routing policy relationships
     */
    List<WorkspaceAiGatewayRoutingPolicy> findByRoutingPolicyId(Long routingPolicyId);

    /**
     * Find workspace AI LLM Gateway routing policy relationship by workspace ID and routing policy ID.
     *
     * @param workspaceId     the workspace ID
     * @param routingPolicyId the routing policy ID
     * @return workspace AI LLM Gateway routing policy relationship if found
     */
    WorkspaceAiGatewayRoutingPolicy findByWorkspaceIdAndRoutingPolicyId(Long workspaceId, Long routingPolicyId);
}

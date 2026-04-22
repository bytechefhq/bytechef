/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRoutingPolicy;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRoutingStrategyType;
import java.util.List;

/**
 * Defines the interface for managing and retrieving AI LLM Gateway routing policies associated with specific
 * workspaces.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceAiGatewayRoutingPolicyFacade {

    /**
     * Creates a new AI LLM Gateway routing policy and assigns it to the specified workspace.
     *
     * @param workspaceId   the workspace ID to assign the routing policy to
     * @param name          the name of the routing policy
     * @param strategy      the routing strategy type
     * @param fallbackModel the fallback model name
     * @param config        the routing policy configuration
     * @return the created routing policy
     */
    AiGatewayRoutingPolicy createWorkspaceRoutingPolicy(
        Long workspaceId, String name, AiGatewayRoutingStrategyType strategy,
        String fallbackModel, String config);

    /**
     * Deletes an AI LLM Gateway routing policy and removes it from all workspaces.
     *
     * @param workspaceId     the workspace ID that owns the routing policy
     * @param routingPolicyId the ID of the routing policy to delete
     */
    void deleteWorkspaceRoutingPolicy(Long workspaceId, Long routingPolicyId);

    /**
     * Retrieves all AI LLM Gateway routing policies associated with the specified workspace.
     *
     * @param workspaceId the workspace ID
     * @return a list of routing policies associated with the workspace
     */
    List<AiGatewayRoutingPolicy> getWorkspaceRoutingPolicies(Long workspaceId);

    /**
     * Updates an existing AI LLM Gateway routing policy.
     *
     * @param workspaceId   the workspace ID that owns the routing policy
     * @param id            the ID of the routing policy to update
     * @param name          the new name
     * @param strategy      the new routing strategy type
     * @param fallbackModel the new fallback model name
     * @param config        the new configuration
     * @param enabled       whether the routing policy is enabled
     * @return the updated routing policy
     */
    AiGatewayRoutingPolicy updateWorkspaceRoutingPolicy(
        Long workspaceId, Long id, String name, AiGatewayRoutingStrategyType strategy,
        String fallbackModel, String config, Boolean enabled);
}

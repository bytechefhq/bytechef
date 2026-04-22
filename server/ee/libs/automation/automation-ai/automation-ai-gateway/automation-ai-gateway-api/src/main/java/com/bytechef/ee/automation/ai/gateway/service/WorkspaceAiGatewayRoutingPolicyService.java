/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.WorkspaceAiGatewayRoutingPolicy;
import java.util.List;

/**
 * Service interface for managing workspace AI LLM Gateway routing policy relationships.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceAiGatewayRoutingPolicyService {

    /**
     * Gets AI LLM Gateway routing policies filtered by workspace ID.
     *
     * @param workspaceId the workspace ID to filter by
     * @return a list of workspace AI LLM Gateway routing policy relationships in the specified workspace
     */
    List<WorkspaceAiGatewayRoutingPolicy> getWorkspaceRoutingPolicies(Long workspaceId);

    /**
     * Assigns an AI LLM Gateway routing policy to a workspace.
     *
     * @param routingPolicyId the routing policy ID
     * @param workspaceId     the workspace ID
     */
    void assignRoutingPolicyToWorkspace(Long routingPolicyId, Long workspaceId);

    /**
     * Removes an AI LLM Gateway routing policy from all workspaces.
     *
     * @param routingPolicyId the routing policy ID
     */
    void removeRoutingPolicyFromWorkspace(Long routingPolicyId);
}

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
 * Service interface for the workspace scoping of AI LLM Gateway routing policies. A routing policy belongs to at most
 * one workspace, recorded in its own nullable {@code workspace_id} column.
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
     * @return a list of AI LLM Gateway routing policies in the specified workspace
     */
    List<AiGatewayRoutingPolicy> getWorkspaceRoutingPolicies(Long workspaceId);

    /**
     * Assigns an AI LLM Gateway routing policy to a workspace.
     *
     * @param routingPolicyId the routing policy ID
     * @param workspaceId     the workspace ID
     */
    void assignRoutingPolicyToWorkspace(Long routingPolicyId, Long workspaceId);

    /**
     * Removes an AI LLM Gateway routing policy from its workspace.
     *
     * @param routingPolicyId the routing policy ID
     */
    void removeRoutingPolicyFromWorkspace(Long routingPolicyId);
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModel;
import java.util.List;

/**
 * Defines the interface for managing and retrieving AI LLM Gateway models associated with specific workspaces. Models
 * are implicitly workspace-scoped through their provider relationship.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceAiGatewayModelFacade {

    /**
     * Creates a new AI LLM Gateway model and verifies the provider belongs to the specified workspace.
     *
     * @param workspaceId          the workspace ID to verify provider ownership
     * @param providerId           the provider ID for the model
     * @param name                 the name of the model
     * @param alias                the alias for the model
     * @param contextWindow        the context window size
     * @param inputCostPerMTokens  the input cost per million tokens
     * @param outputCostPerMTokens the output cost per million tokens
     * @param capabilities         the model capabilities
     * @return the created model
     */
    AiGatewayModel createWorkspaceModel(
        Long workspaceId, Long providerId, String name, String alias, Integer contextWindow,
        Double inputCostPerMTokens, Double outputCostPerMTokens, String capabilities);

    /**
     * Deletes an AI LLM Gateway model after verifying workspace ownership through the provider relationship.
     *
     * @param workspaceId the workspace ID to verify ownership
     * @param modelId     the ID of the model to delete
     */
    void deleteWorkspaceModel(Long workspaceId, Long modelId);

    /**
     * Retrieves all AI LLM Gateway models for providers associated with the specified workspace.
     *
     * @param workspaceId the workspace ID
     * @return a list of models belonging to workspace providers
     */
    List<AiGatewayModel> getWorkspaceModels(Long workspaceId);

    /**
     * Updates an existing AI LLM Gateway model.
     *
     * @param id                   the ID of the model to update
     * @param name                 the new name
     * @param alias                the new alias
     * @param contextWindow        the new context window size
     * @param inputCostPerMTokens  the new input cost per million tokens
     * @param outputCostPerMTokens the new output cost per million tokens
     * @param capabilities         the new capabilities
     * @param enabled              whether the model is enabled
     * @return the updated model
     */
    AiGatewayModel updateWorkspaceModel(
        Long id, String name, String alias, Integer contextWindow,
        Double inputCostPerMTokens, Double outputCostPerMTokens, String capabilities, Boolean enabled,
        Long defaultRoutingPolicyId);
}

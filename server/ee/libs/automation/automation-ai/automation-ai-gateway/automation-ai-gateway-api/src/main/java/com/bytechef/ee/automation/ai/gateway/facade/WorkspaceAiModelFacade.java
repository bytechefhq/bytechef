/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceAiModelFacade {

    AiModel createWorkspaceModel(
        Long workspaceId, Long providerId, String name, String alias, Integer contextWindow,
        Double inputCostPerMTokens, Double outputCostPerMTokens, String capabilities, Long defaultRoutingPolicyId);

    void deleteWorkspaceModel(Long workspaceId, Long modelId);

    List<AiModel> getWorkspaceModels(Long workspaceId);

    AiModel unpinWorkspaceModel(Long workspaceId, Long modelId);

    AiModel updateWorkspaceModel(
        Long workspaceId, Long modelId, String name, String alias, Integer contextWindow,
        Double inputCostPerMTokens, Double outputCostPerMTokens, String capabilities, Boolean enabled,
        Long defaultRoutingPolicyId);
}

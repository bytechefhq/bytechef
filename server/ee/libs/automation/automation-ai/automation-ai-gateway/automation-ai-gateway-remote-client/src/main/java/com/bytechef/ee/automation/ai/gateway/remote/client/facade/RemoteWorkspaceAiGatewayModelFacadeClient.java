/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.remote.client.facade;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModel;
import com.bytechef.ee.automation.ai.gateway.facade.WorkspaceAiGatewayModelFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteWorkspaceAiGatewayModelFacadeClient implements WorkspaceAiGatewayModelFacade {

    @Override
    public AiGatewayModel createWorkspaceModel(
        Long workspaceId, Long providerId, String name, String alias, Integer contextWindow,
        Double inputCostPerMTokens, Double outputCostPerMTokens, String capabilities) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteWorkspaceModel(Long workspaceId, Long modelId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AiGatewayModel> getWorkspaceModels(Long workspaceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AiGatewayModel updateWorkspaceModel(
        Long id, String name, String alias, Integer contextWindow,
        Double inputCostPerMTokens, Double outputCostPerMTokens, String capabilities, Boolean enabled,
        Long defaultRoutingPolicyId) {

        throw new UnsupportedOperationException();
    }
}

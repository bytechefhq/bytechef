/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.remote.client.facade;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.automation.ai.gateway.dto.ProviderConnectionResult;
import com.bytechef.ee.automation.ai.gateway.facade.WorkspaceAiGatewayProviderFacade;
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
public class RemoteWorkspaceAiGatewayProviderFacadeClient implements WorkspaceAiGatewayProviderFacade {

    @Override
    public AiGatewayProvider createWorkspaceProvider(
        String name, AiGatewayProviderType type, String apiKey, String baseUrl, String config, Long workspaceId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteWorkspaceProvider(Long workspaceId, Long providerId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AiGatewayProvider> getWorkspaceProviders(Long workspaceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AiGatewayProvider updateWorkspaceProvider(
        Long workspaceId, Long id, String name, String apiKey, String baseUrl, Boolean enabled, String config) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ProviderConnectionResult testWorkspaceProviderConnection(Long workspaceId, Long providerId) {
        throw new UnsupportedOperationException();
    }
}

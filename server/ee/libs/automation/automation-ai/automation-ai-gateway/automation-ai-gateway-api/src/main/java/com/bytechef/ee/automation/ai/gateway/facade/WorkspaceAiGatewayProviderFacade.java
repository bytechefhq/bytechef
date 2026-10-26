/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.platform.ai.gateway.dto.ProviderConnectionResult;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceAiGatewayProviderFacade {

    AiGatewayProvider createWorkspaceProvider(
        String name, AiGatewayProviderType type, String apiKey, String baseUrl, String config, Long workspaceId);

    void deleteWorkspaceProvider(Long workspaceId, Long providerId);

    List<AiGatewayProvider> getWorkspaceProviders(Long workspaceId);

    AiGatewayProvider updateWorkspaceProvider(
        Long workspaceId, Long id, String name, String apiKey, String baseUrl, Boolean enabled, String config);

    ProviderConnectionResult testWorkspaceProviderConnection(Long workspaceId, Long providerId);
}

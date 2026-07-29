/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link WorkspaceAiGatewayProviderService} interface.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class WorkspaceAiGatewayProviderServiceImpl implements WorkspaceAiGatewayProviderService {

    private final AiGatewayProviderService aiGatewayProviderService;

    public WorkspaceAiGatewayProviderServiceImpl(AiGatewayProviderService aiGatewayProviderService) {
        this.aiGatewayProviderService = aiGatewayProviderService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayProvider> getWorkspaceProviders(Long workspaceId) {
        return aiGatewayProviderService.getProvidersByWorkspaceId(workspaceId);
    }

    @Override
    public void assignProviderToWorkspace(Long providerId, Long workspaceId) {
        aiGatewayProviderService.updateWorkspaceId(providerId, workspaceId);
    }

    @Override
    public void removeProviderFromWorkspace(Long providerId) {
        aiGatewayProviderService.updateWorkspaceId(providerId, null);
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.WorkspaceAiGatewayProvider;
import com.bytechef.ee.automation.ai.gateway.repository.WorkspaceAiGatewayProviderRepository;
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

    private final WorkspaceAiGatewayProviderRepository workspaceAiGatewayProviderRepository;

    public WorkspaceAiGatewayProviderServiceImpl(
        WorkspaceAiGatewayProviderRepository workspaceAiGatewayProviderRepository) {

        this.workspaceAiGatewayProviderRepository = workspaceAiGatewayProviderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceAiGatewayProvider> getWorkspaceProviders(Long workspaceId) {
        return workspaceAiGatewayProviderRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    public void assignProviderToWorkspace(Long providerId, Long workspaceId) {
        WorkspaceAiGatewayProvider existing =
            workspaceAiGatewayProviderRepository.findByWorkspaceIdAndProviderId(workspaceId, providerId);

        if (existing == null) {
            WorkspaceAiGatewayProvider workspaceAiGatewayProvider =
                new WorkspaceAiGatewayProvider(providerId, workspaceId);

            workspaceAiGatewayProviderRepository.save(workspaceAiGatewayProvider);
        }
    }

    @Override
    public void removeProviderFromWorkspace(Long providerId) {
        List<WorkspaceAiGatewayProvider> existingRelationships =
            workspaceAiGatewayProviderRepository.findByProviderId(providerId);

        if (!existingRelationships.isEmpty()) {
            workspaceAiGatewayProviderRepository.deleteAll(existingRelationships);
        }
    }
}

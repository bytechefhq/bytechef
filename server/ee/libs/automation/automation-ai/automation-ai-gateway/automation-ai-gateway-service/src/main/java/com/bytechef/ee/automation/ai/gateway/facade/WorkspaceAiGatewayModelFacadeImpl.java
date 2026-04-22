/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModel;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayModelService;
import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayProviderService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link WorkspaceAiGatewayModelFacade} interface that handles workspace AI LLM Gateway model
 * operations. Models are implicitly workspace-scoped through their provider relationship.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class WorkspaceAiGatewayModelFacadeImpl implements WorkspaceAiGatewayModelFacade {

    private final AiGatewayModelService aiGatewayModelService;
    private final WorkspaceAiGatewayProviderService workspaceAiGatewayProviderService;

    @SuppressFBWarnings("EI")
    public WorkspaceAiGatewayModelFacadeImpl(
        AiGatewayModelService aiGatewayModelService,
        WorkspaceAiGatewayProviderService workspaceAiGatewayProviderService) {

        this.aiGatewayModelService = aiGatewayModelService;
        this.workspaceAiGatewayProviderService = workspaceAiGatewayProviderService;
    }

    @Override
    public AiGatewayModel createWorkspaceModel(
        Long workspaceId, Long providerId, String name, String alias, Integer contextWindow,
        Double inputCostPerMTokens, Double outputCostPerMTokens, String capabilities) {

        workspaceAiGatewayProviderService.getWorkspaceProviders(workspaceId)
            .stream()
            .filter(workspaceProvider -> workspaceProvider.getProviderId()
                .equals(providerId))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "Provider " + providerId + " does not belong to workspace " + workspaceId));

        AiGatewayModel model = new AiGatewayModel(providerId, name);

        model.setAlias(alias);
        model.setContextWindow(contextWindow);

        if (inputCostPerMTokens != null) {
            model.setInputCostPerMTokens(BigDecimal.valueOf(inputCostPerMTokens));
        }

        if (outputCostPerMTokens != null) {
            model.setOutputCostPerMTokens(BigDecimal.valueOf(outputCostPerMTokens));
        }

        model.setCapabilities(capabilities);

        return aiGatewayModelService.create(model);
    }

    @Override
    public void deleteWorkspaceModel(Long workspaceId, Long modelId) {
        verifyWorkspaceOwnership(workspaceId, modelId);

        aiGatewayModelService.delete(modelId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayModel> getWorkspaceModels(Long workspaceId) {
        return workspaceAiGatewayProviderService.getWorkspaceProviders(workspaceId)
            .stream()
            .flatMap(
                workspaceProvider -> aiGatewayModelService.getModelsByProviderId(
                    workspaceProvider.getProviderId())
                    .stream())
            .toList();
    }

    @Override
    public AiGatewayModel updateWorkspaceModel(
        Long id, String name, String alias, Integer contextWindow,
        Double inputCostPerMTokens, Double outputCostPerMTokens, String capabilities, Boolean enabled,
        Long defaultRoutingPolicyId) {

        AiGatewayModel model = aiGatewayModelService.getModel(id);

        if (name != null) {
            model.setName(name);
        }

        if (alias != null) {
            model.setAlias(alias);
        }

        if (contextWindow != null) {
            model.setContextWindow(contextWindow);
        }

        if (inputCostPerMTokens != null) {
            model.setInputCostPerMTokens(BigDecimal.valueOf(inputCostPerMTokens));
        }

        if (outputCostPerMTokens != null) {
            model.setOutputCostPerMTokens(BigDecimal.valueOf(outputCostPerMTokens));
        }

        if (capabilities != null) {
            model.setCapabilities(capabilities);
        }

        // Allow null to clear the override (i.e. "inherit from workspace/system default") since omitting the field
        // would leave the prior value sticking. Different from name/alias/etc. where null means "don't change".
        model.setDefaultRoutingPolicyId(defaultRoutingPolicyId);

        if (enabled != null) {
            model.setEnabled(enabled);
        }

        return aiGatewayModelService.update(model);
    }

    private void verifyWorkspaceOwnership(Long workspaceId, Long modelId) {
        AiGatewayModel model = aiGatewayModelService.getModel(modelId);

        boolean owned = workspaceAiGatewayProviderService.getWorkspaceProviders(workspaceId)
            .stream()
            .anyMatch(
                workspaceProvider -> workspaceProvider.getProviderId()
                    .equals(model.getProviderId()));

        if (!owned) {
            throw new IllegalArgumentException(
                "Model " + modelId + " does not belong to workspace " + workspaceId);
        }
    }
}

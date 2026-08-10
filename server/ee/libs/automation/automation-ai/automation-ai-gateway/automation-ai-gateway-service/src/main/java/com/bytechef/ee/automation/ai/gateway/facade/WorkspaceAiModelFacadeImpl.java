/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayProviderService;
import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import com.bytechef.ee.platform.ai.model.catalog.service.AiModelService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link WorkspaceAiModelFacade} interface that handles workspace AI LLM Gateway model
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
class WorkspaceAiModelFacadeImpl implements WorkspaceAiModelFacade {

    private final AiModelService aiModelService;
    private final WorkspaceAiGatewayProviderService workspaceAiGatewayProviderService;

    @SuppressFBWarnings("EI")
    public WorkspaceAiModelFacadeImpl(
        AiModelService aiModelService,
        WorkspaceAiGatewayProviderService workspaceAiGatewayProviderService) {

        this.aiModelService = aiModelService;
        this.workspaceAiGatewayProviderService = workspaceAiGatewayProviderService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiModel createWorkspaceModel(
        Long workspaceId, Long providerId, String name, String alias, Integer contextWindow,
        Double inputCostPerMTokens, Double outputCostPerMTokens, String capabilities,
        Long defaultRoutingPolicyId) {

        workspaceAiGatewayProviderService.getWorkspaceProviders(workspaceId)
            .stream()
            .filter(workspaceProvider -> Objects.equals(workspaceProvider.getId(), providerId))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "Provider " + providerId + " does not belong to workspace " + workspaceId));

        AiModel model = new AiModel(providerId, name);

        model.setAlias(alias);
        model.setContextWindow(contextWindow);

        if (inputCostPerMTokens != null) {
            model.setInputCostPerMTokens(BigDecimal.valueOf(inputCostPerMTokens));
        }

        if (outputCostPerMTokens != null) {
            model.setOutputCostPerMTokens(BigDecimal.valueOf(outputCostPerMTokens));
        }

        model.setCapabilities(capabilities);

        // Null is a valid value meaning "inherit from workspace/system default", same as the update path.
        model.setDefaultRoutingPolicyId(defaultRoutingPolicyId);

        return aiModelService.create(model);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void deleteWorkspaceModel(Long workspaceId, Long modelId) {
        verifyWorkspaceOwnership(workspaceId, modelId);

        aiModelService.delete(modelId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')")
    public List<AiModel> getWorkspaceModels(Long workspaceId) {
        return workspaceAiGatewayProviderService.getWorkspaceProviders(workspaceId)
            .stream()
            .flatMap(
                workspaceProvider -> aiModelService.getModelsByProviderId(workspaceProvider.getId())
                    .stream())
            .toList();
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiModel unpinWorkspaceModel(Long workspaceId, Long modelId) {
        verifyWorkspaceOwnership(workspaceId, modelId);

        return aiModelService.unpin(modelId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiModel updateWorkspaceModel(
        Long workspaceId, Long modelId, String name, String alias, Integer contextWindow,
        Double inputCostPerMTokens, Double outputCostPerMTokens, String capabilities, Boolean enabled,
        Long defaultRoutingPolicyId) {

        verifyWorkspaceOwnership(workspaceId, modelId);

        AiModel model = aiModelService.getModel(modelId);

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

        return aiModelService.update(model);
    }

    private void verifyWorkspaceOwnership(Long workspaceId, Long modelId) {
        AiModel model = aiModelService.getModel(modelId);

        boolean owned = workspaceAiGatewayProviderService.getWorkspaceProviders(workspaceId)
            .stream()
            .anyMatch(workspaceProvider -> Objects.equals(workspaceProvider.getId(), model.getProviderId()));

        if (!owned) {
            throw new IllegalArgumentException(
                "Model " + modelId + " does not belong to workspace " + workspaceId);
        }
    }
}

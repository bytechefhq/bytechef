/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.facade.WorkspaceAiModelFacade;
import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing workspace-scoped AI LLM Gateway models.
 *
 * <p>
 * Authorization is enforced on {@link WorkspaceAiModelFacade}, not here, so the guards apply to every caller of the
 * facade rather than only this GraphQL entry point: reads require the caller-bound {@code AI_GATEWAY_VIEW} workspace
 * permission, writes require the {@code ADMIN} authority, and every write additionally verifies the target model
 * belongs to the given workspace.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class WorkspaceAiModelGraphQlController {

    private final WorkspaceAiModelFacade workspaceAiModelFacade;

    @SuppressFBWarnings("EI")
    WorkspaceAiModelGraphQlController(
        WorkspaceAiModelFacade workspaceAiModelFacade) {

        this.workspaceAiModelFacade = workspaceAiModelFacade;
    }

    @QueryMapping
    public List<AiModel> workspaceAiModels(@Argument Long workspaceId) {
        return workspaceAiModelFacade.getWorkspaceModels(workspaceId);
    }

    @MutationMapping
    public AiModel createWorkspaceAiModel(
        @Argument CreateWorkspaceAiModelInput input) {

        return workspaceAiModelFacade.createWorkspaceModel(
            input.workspaceId(),
            input.providerId(),
            input.name(),
            input.alias(),
            input.contextWindow(),
            input.inputCostPerMTokens(),
            input.outputCostPerMTokens(),
            input.capabilities(),
            input.defaultRoutingPolicyId());
    }

    @MutationMapping
    public boolean deleteWorkspaceAiModel(@Argument Long workspaceId, @Argument Long modelId) {
        workspaceAiModelFacade.deleteWorkspaceModel(workspaceId, modelId);

        return true;
    }

    @MutationMapping
    public AiModel unpinWorkspaceAiModel(@Argument Long workspaceId, @Argument Long modelId) {
        return workspaceAiModelFacade.unpinWorkspaceModel(workspaceId, modelId);
    }

    @MutationMapping
    public AiModel updateWorkspaceAiModel(
        @Argument Long workspaceId, @Argument Long id, @Argument UpdateAiModelInput input) {

        return workspaceAiModelFacade.updateWorkspaceModel(
            workspaceId,
            id,
            input.name(),
            input.alias(),
            input.contextWindow(),
            input.inputCostPerMTokens(),
            input.outputCostPerMTokens(),
            input.capabilities(),
            input.enabled(),
            input.defaultRoutingPolicyId());
    }

    @SuppressFBWarnings("EI")
    public record CreateWorkspaceAiModelInput(
        String alias, String capabilities, Integer contextWindow, Long defaultRoutingPolicyId,
        Double inputCostPerMTokens, String name, Double outputCostPerMTokens, Long providerId, Long workspaceId) {
    }

    @SuppressFBWarnings("EI")
    public record UpdateAiModelInput(
        String alias, String capabilities, Integer contextWindow, Long defaultRoutingPolicyId, Boolean enabled,
        Double inputCostPerMTokens, String name, Double outputCostPerMTokens) {
    }
}

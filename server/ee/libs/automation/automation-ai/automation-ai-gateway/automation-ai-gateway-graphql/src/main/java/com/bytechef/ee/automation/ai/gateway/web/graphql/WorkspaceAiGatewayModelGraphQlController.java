/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModel;
import com.bytechef.ee.automation.ai.gateway.facade.WorkspaceAiGatewayModelFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing workspace-scoped AI LLM Gateway models.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class WorkspaceAiGatewayModelGraphQlController {

    private final WorkspaceAiGatewayModelFacade workspaceAiGatewayModelFacade;

    @SuppressFBWarnings("EI")
    WorkspaceAiGatewayModelGraphQlController(
        WorkspaceAiGatewayModelFacade workspaceAiGatewayModelFacade) {

        this.workspaceAiGatewayModelFacade = workspaceAiGatewayModelFacade;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public List<AiGatewayModel> workspaceAiGatewayModels(@Argument Long workspaceId) {
        return workspaceAiGatewayModelFacade.getWorkspaceModels(workspaceId);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public AiGatewayModel createWorkspaceAiGatewayModel(
        @Argument CreateWorkspaceAiGatewayModelInput input) {

        return workspaceAiGatewayModelFacade.createWorkspaceModel(
            input.workspaceId(),
            input.providerId(),
            input.name(),
            input.alias(),
            input.contextWindow(),
            input.inputCostPerMTokens(),
            input.outputCostPerMTokens(),
            input.capabilities());
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public boolean deleteWorkspaceAiGatewayModel(@Argument Long workspaceId, @Argument Long modelId) {
        workspaceAiGatewayModelFacade.deleteWorkspaceModel(workspaceId, modelId);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public AiGatewayModel updateWorkspaceAiGatewayModel(
        @Argument Long id, @Argument UpdateAiGatewayModelInput input) {

        return workspaceAiGatewayModelFacade.updateWorkspaceModel(
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
    public record CreateWorkspaceAiGatewayModelInput(
        String alias, String capabilities, Integer contextWindow, Double inputCostPerMTokens, String name,
        Double outputCostPerMTokens, Long providerId, Long workspaceId) {
    }

    @SuppressFBWarnings("EI")
    public record UpdateAiGatewayModelInput(
        String alias, String capabilities, Integer contextWindow, Long defaultRoutingPolicyId, Boolean enabled,
        Double inputCostPerMTokens, String name, Double outputCostPerMTokens) {
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRoutingPolicy;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRoutingStrategyType;
import com.bytechef.ee.automation.ai.gateway.facade.WorkspaceAiGatewayRoutingPolicyFacade;
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
 * GraphQL controller for managing workspace-scoped AI LLM Gateway routing policies.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class WorkspaceAiGatewayRoutingPolicyGraphQlController {

    private final WorkspaceAiGatewayRoutingPolicyFacade workspaceAiGatewayRoutingPolicyFacade;

    @SuppressFBWarnings("EI")
    WorkspaceAiGatewayRoutingPolicyGraphQlController(
        WorkspaceAiGatewayRoutingPolicyFacade workspaceAiGatewayRoutingPolicyFacade) {

        this.workspaceAiGatewayRoutingPolicyFacade = workspaceAiGatewayRoutingPolicyFacade;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public List<AiGatewayRoutingPolicy> workspaceAiGatewayRoutingPolicies(@Argument Long workspaceId) {
        return workspaceAiGatewayRoutingPolicyFacade.getWorkspaceRoutingPolicies(workspaceId);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public AiGatewayRoutingPolicy createWorkspaceAiGatewayRoutingPolicy(
        @Argument CreateWorkspaceAiGatewayRoutingPolicyInput input) {

        return workspaceAiGatewayRoutingPolicyFacade.createWorkspaceRoutingPolicy(
            input.workspaceId(),
            input.name(),
            input.strategy(),
            input.fallbackModel(),
            input.config());
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public boolean deleteWorkspaceAiGatewayRoutingPolicy(
        @Argument Long workspaceId, @Argument Long routingPolicyId) {

        workspaceAiGatewayRoutingPolicyFacade.deleteWorkspaceRoutingPolicy(workspaceId, routingPolicyId);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public AiGatewayRoutingPolicy updateWorkspaceAiGatewayRoutingPolicy(
        @Argument Long workspaceId, @Argument Long id, @Argument UpdateAiGatewayRoutingPolicyInput input) {

        return workspaceAiGatewayRoutingPolicyFacade.updateWorkspaceRoutingPolicy(
            workspaceId,
            id,
            input.name(),
            input.strategy(),
            input.fallbackModel(),
            input.config(),
            input.enabled());
    }

    @SuppressFBWarnings("EI")
    public record CreateWorkspaceAiGatewayRoutingPolicyInput(
        String config, String fallbackModel, String name, AiGatewayRoutingStrategyType strategy,
        Long workspaceId) {
    }

    @SuppressFBWarnings("EI")
    public record UpdateAiGatewayRoutingPolicyInput(
        String config, Boolean enabled, String fallbackModel, String name,
        AiGatewayRoutingStrategyType strategy) {
    }
}

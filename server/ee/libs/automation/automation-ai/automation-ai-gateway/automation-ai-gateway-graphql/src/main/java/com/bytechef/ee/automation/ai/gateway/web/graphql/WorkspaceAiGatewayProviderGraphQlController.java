/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.automation.ai.gateway.dto.ProviderConnectionResult;
import com.bytechef.ee.automation.ai.gateway.facade.WorkspaceAiGatewayProviderFacade;
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
 * GraphQL controller for managing workspace AI LLM Gateway provider relationships.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class WorkspaceAiGatewayProviderGraphQlController {

    private final WorkspaceAiGatewayProviderFacade workspaceAiGatewayProviderFacade;

    @SuppressFBWarnings("EI")
    WorkspaceAiGatewayProviderGraphQlController(
        WorkspaceAiGatewayProviderFacade workspaceAiGatewayProviderFacade) {

        this.workspaceAiGatewayProviderFacade = workspaceAiGatewayProviderFacade;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public List<AiGatewayProvider> workspaceAiGatewayProviders(@Argument Long workspaceId) {
        return workspaceAiGatewayProviderFacade.getWorkspaceProviders(workspaceId);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public AiGatewayProvider createWorkspaceAiGatewayProvider(
        @Argument CreateWorkspaceAiGatewayProviderInput input) {

        return workspaceAiGatewayProviderFacade.createWorkspaceProvider(
            input.name(),
            input.type(),
            input.apiKey(),
            input.baseUrl(),
            input.config(),
            input.workspaceId());
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public boolean deleteWorkspaceAiGatewayProvider(
        @Argument Long workspaceId, @Argument Long providerId) {

        workspaceAiGatewayProviderFacade.deleteWorkspaceProvider(workspaceId, providerId);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public ProviderConnectionResult testWorkspaceAiGatewayProviderConnection(
        @Argument Long workspaceId, @Argument Long providerId) {

        return workspaceAiGatewayProviderFacade.testWorkspaceProviderConnection(workspaceId, providerId);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public AiGatewayProvider updateWorkspaceAiGatewayProvider(
        @Argument Long workspaceId, @Argument Long id, @Argument UpdateAiGatewayProviderInput input) {

        return workspaceAiGatewayProviderFacade.updateWorkspaceProvider(
            workspaceId,
            id,
            input.name(),
            input.apiKey(),
            input.baseUrl(),
            input.enabled(),
            input.config());
    }

    @SuppressFBWarnings("EI")
    public record CreateWorkspaceAiGatewayProviderInput(
        String apiKey, String baseUrl, String config, String name, AiGatewayProviderType type, Long workspaceId) {
    }

    @SuppressFBWarnings("EI")
    public record UpdateAiGatewayProviderInput(
        String apiKey, String baseUrl, String config, Boolean enabled, String name, AiGatewayProviderType type) {
    }
}

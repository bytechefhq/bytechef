/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModel;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayModelService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing AI LLM Gateway models.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiGatewayModelGraphQlController {

    private final AiGatewayModelService aiGatewayModelService;

    @SuppressFBWarnings("EI")
    AiGatewayModelGraphQlController(AiGatewayModelService aiGatewayModelService) {
        this.aiGatewayModelService = aiGatewayModelService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayModel aiGatewayModel(@Argument long id) {
        return aiGatewayModelService.getModel(id);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiGatewayModel> aiGatewayModels() {
        return aiGatewayModelService.getModels();
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiGatewayModel> aiGatewayModelsByProvider(@Argument long providerId) {
        return aiGatewayModelService.getModelsByProviderId(providerId);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayModel createAiGatewayModel(@Argument CreateAiGatewayModelInput input) {
        AiGatewayModel model = new AiGatewayModel(input.providerId(), input.name());

        model.setAlias(input.alias());
        model.setContextWindow(input.contextWindow());

        if (input.inputCostPerMTokens() != null) {
            model.setInputCostPerMTokens(BigDecimal.valueOf(input.inputCostPerMTokens()));
        }

        if (input.outputCostPerMTokens() != null) {
            model.setOutputCostPerMTokens(BigDecimal.valueOf(input.outputCostPerMTokens()));
        }

        model.setCapabilities(input.capabilities());
        model.setDefaultRoutingPolicyId(input.defaultRoutingPolicyId());

        return aiGatewayModelService.create(model);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean deleteAiGatewayModel(@Argument long id) {
        aiGatewayModelService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayModel updateAiGatewayModel(
        @Argument long id, @Argument UpdateAiGatewayModelInput input) {

        AiGatewayModel model = aiGatewayModelService.getModel(id);

        if (input.name() != null) {
            model.setName(input.name());
        }

        if (input.alias() != null) {
            model.setAlias(input.alias());
        }

        if (input.contextWindow() != null) {
            model.setContextWindow(input.contextWindow());
        }

        if (input.inputCostPerMTokens() != null) {
            model.setInputCostPerMTokens(BigDecimal.valueOf(input.inputCostPerMTokens()));
        }

        if (input.outputCostPerMTokens() != null) {
            model.setOutputCostPerMTokens(BigDecimal.valueOf(input.outputCostPerMTokens()));
        }

        if (input.capabilities() != null) {
            model.setCapabilities(input.capabilities());
        }

        if (input.defaultRoutingPolicyId() != null) {
            model.setDefaultRoutingPolicyId(input.defaultRoutingPolicyId());
        }

        if (input.enabled() != null) {
            model.setEnabled(input.enabled());
        }

        return aiGatewayModelService.update(model);
    }

    @SuppressFBWarnings("EI")
    public record CreateAiGatewayModelInput(
        Long providerId, String name, String alias, Integer contextWindow, Double inputCostPerMTokens,
        Double outputCostPerMTokens, String capabilities, Long defaultRoutingPolicyId) {
    }

    @SuppressFBWarnings("EI")
    public record UpdateAiGatewayModelInput(
        String name, String alias, Integer contextWindow, Double inputCostPerMTokens, Double outputCostPerMTokens,
        String capabilities, Boolean enabled, Long defaultRoutingPolicyId) {
    }
}

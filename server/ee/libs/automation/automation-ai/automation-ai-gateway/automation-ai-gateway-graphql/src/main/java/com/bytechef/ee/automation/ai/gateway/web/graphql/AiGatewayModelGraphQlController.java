/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayModelFacade;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing AI LLM Gateway models.
 *
 * <p>
 * Authorization is enforced on {@link AiGatewayModelFacade}, not here.
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

    private final AiGatewayModelFacade aiGatewayModelFacade;

    @SuppressFBWarnings("EI")
    AiGatewayModelGraphQlController(AiGatewayModelFacade aiGatewayModelFacade) {
        this.aiGatewayModelFacade = aiGatewayModelFacade;
    }

    @QueryMapping
    public AiGatewayModel aiGatewayModel(@Argument long id) {
        return aiGatewayModelFacade.getModel(id);
    }

    @QueryMapping
    public List<AiGatewayModel> aiGatewayModels() {
        return aiGatewayModelFacade.getModels();
    }

    @QueryMapping
    public List<AiGatewayModel> aiGatewayModelsByProvider(@Argument long providerId) {
        return aiGatewayModelFacade.getModelsByProviderId(providerId);
    }

    @MutationMapping
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

        return aiGatewayModelFacade.create(model);
    }

    @MutationMapping
    public boolean deleteAiGatewayModel(@Argument long id) {
        aiGatewayModelFacade.delete(id);

        return true;
    }

    @MutationMapping
    public AiGatewayModel updateAiGatewayModel(
        @Argument long id, @Argument UpdateAiGatewayModelInput input) {

        AiGatewayModel model = aiGatewayModelFacade.getModel(id);

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

        return aiGatewayModelFacade.update(model);
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

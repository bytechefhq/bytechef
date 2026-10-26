/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.facade.AiModelFacade;
import com.bytechef.ee.platform.ai.gateway.catalog.AiModelCatalogReconciler;
import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiModelGraphQlController {

    private static final Logger log = LoggerFactory.getLogger(AiModelGraphQlController.class);

    private final AiModelCatalogReconciler aiModelCatalogReconciler;
    private final AiModelFacade aiModelFacade;

    @SuppressFBWarnings("EI")
    AiModelGraphQlController(
        AiModelCatalogReconciler aiModelCatalogReconciler, AiModelFacade aiModelFacade) {
        this.aiModelCatalogReconciler = aiModelCatalogReconciler;
        this.aiModelFacade = aiModelFacade;
    }

    @QueryMapping
    public AiModel aiModel(@Argument long id) {
        return aiModelFacade.getModel(id);
    }

    @QueryMapping
    public List<AiModel> aiModels() {
        return aiModelFacade.getModels();
    }

    @QueryMapping
    public List<AiModel> aiModelsByProvider(@Argument long providerId) {
        return aiModelFacade.getModelsByProviderId(providerId);
    }

    @BatchMapping(typeName = "AiModel", field = "catalogManaged")
    public Map<AiModel, Boolean> catalogManaged(List<AiModel> aiModels) {
        Set<Long> managedModelIds = resolveCatalogManagedModelIds(aiModels);

        return aiModels.stream()
            .collect(Collectors.toMap(Function.identity(), model -> managedModelIds.contains(model.getId())));
    }

    private Set<Long> resolveCatalogManagedModelIds(List<AiModel> aiModels) {
        try {
            return aiModelCatalogReconciler.catalogManagedModelIds(aiModels);
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to resolve catalogManaged for {} model(s); defaulting to false", aiModels.size(),
                exception);

            return Set.of();
        }
    }

    @MutationMapping
    public AiModel createAiModel(@Argument CreateAiModelInput input) {
        AiModel model = new AiModel(input.providerId(), input.name());

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

        return aiModelFacade.create(model);
    }

    @MutationMapping
    public boolean deleteAiModel(@Argument long id) {
        aiModelFacade.delete(id);

        return true;
    }

    @MutationMapping
    public boolean reconcileAiModelCatalog() {
        aiModelFacade.reconcileCatalog();

        return true;
    }

    @MutationMapping
    public AiModel unpinAiModel(@Argument long id) {
        return aiModelFacade.unpin(id);
    }

    @MutationMapping
    public AiModel updateAiModel(
        @Argument long id, @Argument UpdateAiModelInput input) {
        AiModel model = aiModelFacade.getModel(id);

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

        return aiModelFacade.update(model);
    }

    @SuppressFBWarnings("EI")
    public record CreateAiModelInput(
        Long providerId, String name, String alias, Integer contextWindow, Double inputCostPerMTokens,
        Double outputCostPerMTokens, String capabilities, Long defaultRoutingPolicyId) {
    }

    @SuppressFBWarnings("EI")
    public record UpdateAiModelInput(
        String name, String alias, Integer contextWindow, Double inputCostPerMTokens, Double outputCostPerMTokens,
        String capabilities, Boolean enabled, Long defaultRoutingPolicyId) {
    }
}

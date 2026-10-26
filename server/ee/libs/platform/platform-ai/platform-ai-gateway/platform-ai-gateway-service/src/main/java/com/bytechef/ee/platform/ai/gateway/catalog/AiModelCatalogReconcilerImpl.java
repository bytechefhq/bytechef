/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.catalog;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import com.bytechef.ee.platform.ai.model.catalog.service.AiModelService;
import com.bytechef.platform.ai.model.catalog.CatalogModel;
import com.bytechef.platform.ai.model.catalog.CatalogModel.Status;
import com.bytechef.platform.ai.model.catalog.Cost;
import com.bytechef.platform.ai.model.catalog.Modalities.Modality;
import com.bytechef.platform.ai.model.catalog.ModelCatalog;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI2")
public class AiModelCatalogReconcilerImpl implements AiModelCatalogReconciler {

    private static final Logger log = LoggerFactory.getLogger(AiModelCatalogReconcilerImpl.class);

    private final AiModelService aiModelService;
    private final AiGatewayProviderService aiGatewayProviderService;
    private final ModelCatalog modelCatalog;

    public AiModelCatalogReconcilerImpl(
        AiModelService aiModelService, AiGatewayProviderService aiGatewayProviderService,
        ModelCatalog modelCatalog) {
        this.aiModelService = aiModelService;
        this.aiGatewayProviderService = aiGatewayProviderService;
        this.modelCatalog = modelCatalog;
    }

    @Override
    public Set<Long> catalogManagedModelIds(Collection<AiModel> models) {
        if (models.isEmpty()) {
            return Set.of();
        }

        Set<Long> providerIds = models.stream()
            .map(AiModel::getProviderId)
            .collect(Collectors.toSet());

        Map<Long, AiGatewayProvider> providersById = aiGatewayProviderService.getProviders(providerIds)
            .stream()
            .collect(Collectors.toMap(AiGatewayProvider::getId, provider -> provider));

        Set<Long> managedModelIds = new HashSet<>();

        for (AiModel model : models) {
            AiGatewayProvider provider = providersById.get(model.getProviderId());

            if (provider == null || !provider.isEnabled()) {
                continue;
            }

            String providerId = AiModelsDevProviderIds.resolveProviderId(provider.getType());

            if (modelCatalog.fetchModel(providerId, model.getName())
                .isPresent()) {
                managedModelIds.add(model.getId());
            }
        }

        return managedModelIds;
    }

    @Override
    public void reconcile() {
        for (AiGatewayProvider provider : aiGatewayProviderService.getEnabledProviders()) {
            try {
                reconcileProvider(provider);
            } catch (RuntimeException exception) {
                log.warn(
                    "models.dev reconcile failed for provider '{}' (id={}); continuing", provider.getName(),
                    provider.getId(), exception);
            }
        }
    }

    private void reconcileProvider(AiGatewayProvider provider) {
        String providerId = AiModelsDevProviderIds.resolveProviderId(provider.getType());

        List<CatalogModel> catalogModels = modelCatalog.getModels(providerId);

        if (catalogModels.isEmpty()) {
            return;
        }

        Map<String, AiModel> existingModels = new HashMap<>();

        for (AiModel model : aiModelService.getModelsByProviderId(provider.getId())) {
            existingModels.put(model.getName(), model);
        }

        for (CatalogModel catalogModel : catalogModels) {
            try {
                reconcileModel(provider, existingModels.get(catalogModel.id()), catalogModel);
            } catch (RuntimeException exception) {
                log.warn(
                    "models.dev reconcile failed for provider '{}' (id={}) model '{}'; continuing",
                    provider.getName(), provider.getId(), catalogModel.id(), exception);
            }
        }
    }

    private void reconcileModel(
        AiGatewayProvider provider, @Nullable AiModel existingModel,
        CatalogModel catalogModel) {
        if (existingModel == null) {
            if (insertable(catalogModel)) {
                aiModelService.create(toNewModel(provider.getId(), catalogModel));
            }

            return;
        }

        if (existingModel.isCatalogPinned() || upToDate(existingModel, catalogModel)) {
            return;
        }

        applyCatalogValues(existingModel, catalogModel);

        aiModelService.updateFromCatalog(existingModel);
    }

    private static boolean upToDate(AiModel model, CatalogModel catalogModel) {
        return sameDecimal(model.getInputCostPerMTokens(), baseRate(catalogModel.cost(), true))
            && sameDecimal(model.getOutputCostPerMTokens(), baseRate(catalogModel.cost(), false))
            && Objects.equals(model.getContextWindow(), contextWindow(catalogModel))
            && Objects.equals(model.getCapabilities(), CapabilitiesEncoder.encode(catalogModel));
    }

    private static boolean sameDecimal(@Nullable BigDecimal existingValue, @Nullable BigDecimal value) {
        if (existingValue == null || value == null) {
            return existingValue == value;
        }

        return existingValue.compareTo(value) == 0;
    }

    private static boolean insertable(CatalogModel catalogModel) {
        return catalogModel.modalities()
            .output()
            .contains(Modality.TEXT) && catalogModel.status() != Status.DEPRECATED;
    }

    private static AiModel toNewModel(Long providerId, CatalogModel catalogModel) {
        AiModel model = new AiModel(providerId, catalogModel.id());

        applyCatalogValues(model, catalogModel);

        return model;
    }

    private static void applyCatalogValues(AiModel model, CatalogModel catalogModel) {
        model.setCapabilities(CapabilitiesEncoder.encode(catalogModel));
        model.setContextWindow(contextWindow(catalogModel));
        model.setInputCostPerMTokens(baseRate(catalogModel.cost(), true));
        model.setOutputCostPerMTokens(baseRate(catalogModel.cost(), false));
    }

    private static @Nullable Integer contextWindow(CatalogModel catalogModel) {
        Integer context = catalogModel.limit()
            .context();

        return context != null && context > 0 ? context : null;
    }

    private static @Nullable BigDecimal baseRate(@Nullable Cost cost, boolean input) {
        if (cost == null) {
            return null;
        }

        return input ? cost.input() : cost.output();
    }
}

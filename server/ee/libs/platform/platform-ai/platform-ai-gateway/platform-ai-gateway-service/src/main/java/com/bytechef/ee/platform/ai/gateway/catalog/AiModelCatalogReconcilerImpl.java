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
 * Populates {@code ai_model} rows from the models.dev catalog.
 *
 * <p>
 * Insert is filtered and update is not, deliberately. Without the filter, enabling a provider would insert video and
 * image generation models the gateway cannot route, plus every deprecated model upstream still lists. But a deprecated
 * model a deployment is <em>already routing to</em> costs real money, so an existing row keeps being repriced no matter
 * what upstream marks it.
 *
 * <p>
 * Rows the catalog does not know — Azure deployment names, fine-tunes, models newer than the bundled snapshot — are
 * skipped silently and keep whatever rates were entered by hand.
 *
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

        // One batched query for every distinct provider referenced by the list, rather than one lookup per model
        // row — the model list this feeds a GraphQL response for can be large, and per-row provider fetches would
        // turn a single query into an N+1.
        Map<Long, AiGatewayProvider> providersById = aiGatewayProviderService.getProviders(providerIds)
            .stream()
            .collect(Collectors.toMap(AiGatewayProvider::getId, provider -> provider));

        Set<Long> managedModelIds = new HashSet<>();

        for (AiModel model : models) {
            AiGatewayProvider provider = providersById.get(model.getProviderId());

            // A disabled provider is never swept by reconcile() (see getEnabledProviders() below), so a row on one
            // must never be reported managed — the reconciler will not touch its pricing again until re-enabled.
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

    /**
     * A single model's catalog write, isolated in its own try/catch by the caller so that one model upstream publishes
     * with an unexpected shape never costs the rest of the provider's sweep — see the class Javadoc on
     * {@link #reconcile()} for the provider-level version of the same guarantee.
     */
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

    /**
     * Skips rows the catalog would write identically. Without this the daily sweep rewrites every row, bumping
     * {@code last_modified_date} and the optimistic-locking version across the whole table and destroying the audit
     * signal for when a model's pricing genuinely moved.
     */
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

    /**
     * The gateway routes text completions. A model that cannot emit text is not routable through it, and a deprecated
     * model is not something to newly adopt — but see the class Javadoc for why neither condition applies to updates.
     */
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

    /**
     * Writes exactly the four catalog-owned fields. {@code alias}, {@code enabled}, and {@code defaultRoutingPolicyId}
     * belong to the administrator and are never touched here.
     */
    private static void applyCatalogValues(AiModel model, CatalogModel catalogModel) {
        model.setCapabilities(CapabilitiesEncoder.encode(catalogModel));
        model.setContextWindow(contextWindow(catalogModel));
        model.setInputCostPerMTokens(baseRate(catalogModel.cost(), true));
        model.setOutputCostPerMTokens(baseRate(catalogModel.cost(), false));
    }

    /**
     * Normalizes a non-positive {@code limit.context} to {@code null}. Upstream publishes {@code "context": 0} for a
     * handful of models — mostly image and audio models with no meaningful context window — and
     * {@link AiModel#setContextWindow(Integer)} rejects non-positive values outright. Both the insert path and
     * {@link #upToDate} must run every catalog value through this same normalization, or a freshly-inserted row with a
     * null context window would never compare equal to the catalog's raw zero and get rewritten on every sweep.
     */
    private static @Nullable Integer contextWindow(CatalogModel catalogModel) {
        Integer context = catalogModel.limit()
            .context();

        return context != null && context > 0 ? context : null;
    }

    /**
     * Returns the base-tier rate. For the 23 tiered models upstream publishes, above-threshold traffic is under-costed
     * against this rate — a known limitation of the gateway's two flat cost columns, which could not express tiering
     * before this reconciler existed either.
     */
    private static @Nullable BigDecimal baseRate(@Nullable Cost cost, boolean input) {
        if (cost == null) {
            return null;
        }

        return input ? cost.input() : cost.output();
    }
}

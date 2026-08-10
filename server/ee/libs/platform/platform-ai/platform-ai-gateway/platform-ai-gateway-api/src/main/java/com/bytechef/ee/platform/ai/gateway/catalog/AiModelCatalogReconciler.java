/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.catalog;

import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import java.util.Collection;
import java.util.Set;

/**
 * Populates {@code ai_model} rows from the models.dev catalog.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiModelCatalogReconciler {

    /**
     * Reports which of the given rows have a matching entry in the models.dev catalog for their provider + model id,
     * independent of {@link AiModel#isCatalogPinned()}. This is the signal the UI needs to tell an "Unmanaged" row (no
     * catalog entry, e.g. an Azure deployment name or a fine-tune) from a "Catalog" row (unpinned, catalog has an
     * entry, the reconciler maintains it) — a distinction {@code catalogPinned} alone cannot express.
     *
     * <p>
     * A row on a disabled provider is never reported managed, matching {@link #reconcile()}, which only sweeps
     * {@code getEnabledProviders()} — a provider an admin has disabled will never again be repriced by this reconciler,
     * so calling it "Catalog" would be a false maintenance claim.
     *
     * <p>
     * Resolves every row's provider in a single batched lookup rather than one round-trip per row, so calling this with
     * a large model list costs one query, not N.
     *
     * @param models the rows to check against the catalog
     * @return the ids of the given models for which the catalog currently has a matching entry
     */
    Set<Long> catalogManagedModelIds(Collection<AiModel> models);

    void reconcile();
}

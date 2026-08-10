/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.ai.model.catalog.service;

import com.bytechef.platform.ai.model.catalog.CatalogModel;
import com.bytechef.platform.ai.model.catalog.CatalogProvider;
import com.bytechef.platform.ai.model.catalog.ModelCatalog;
import com.bytechef.platform.ai.model.catalog.modelsdev.ModelsDevSnapshotLoader;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Serves catalog reads from an in-memory map that {@link com.bytechef.platform.ai.model.catalog.modelsdev
 * .ModelsDevRefresher} may replace wholesale.
 *
 * <p>
 * The bundled snapshot is parsed <em>lazily</em>, on first read rather than in the constructor. The document is 3.6 MB
 * and most deployments will not touch the catalog on their first request; server startup is already sensitive to
 * classpath-wide work, and paying a multi-megabyte parse on every boot for a feature that may go unused is the wrong
 * trade.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class ModelCatalogImpl implements ModelCatalog {

    private final ModelsDevSnapshotLoader modelsDevSnapshotLoader;

    private volatile @Nullable Catalog catalog;

    public ModelCatalogImpl(ModelsDevSnapshotLoader modelsDevSnapshotLoader) {
        this.modelsDevSnapshotLoader = modelsDevSnapshotLoader;
    }

    @Override
    public Optional<CatalogModel> fetchModel(String providerId, String modelId) {
        CatalogProvider provider = getCatalog().providers()
            .get(providerId);

        if (provider == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
            provider.models()
                .get(modelId));
    }

    @Override
    public Instant getLoadedAt() {
        return getCatalog().loadedAt();
    }

    @Override
    public List<CatalogModel> getModels(String providerId) {
        CatalogProvider provider = getCatalog().providers()
            .get(providerId);

        if (provider == null) {
            return List.of();
        }

        return List.copyOf(
            provider.models()
                .values());
    }

    @Override
    public List<CatalogProvider> getProviders() {
        return List.copyOf(
            getCatalog().providers()
                .values());
    }

    /**
     * Replaces the whole catalog atomically. Callers must never install a partial or empty map — a read served from
     * half a catalog is indistinguishable from a model genuinely not existing.
     *
     * <p>
     * Synchronized on the same monitor as the lazy load in {@link #getCatalog()}: without it, a thread already inside
     * that synchronized block — parsing the bundled snapshot — could finish after a concurrent {@code replaceCatalog}
     * and overwrite fresh, just-fetched data with the stale bundled snapshot.
     */
    public synchronized void replaceCatalog(Map<String, CatalogProvider> providers, Instant loadedAt) {
        catalog = new Catalog(Map.copyOf(providers), loadedAt);
    }

    private Catalog getCatalog() {
        Catalog currentCatalog = catalog;

        if (currentCatalog == null) {
            synchronized (this) {
                currentCatalog = catalog;

                if (currentCatalog == null) {
                    currentCatalog = new Catalog(modelsDevSnapshotLoader.load(), Instant.now());

                    catalog = currentCatalog;
                }
            }
        }

        return currentCatalog;
    }

    private record Catalog(Map<String, CatalogProvider> providers, Instant loadedAt) {
    }
}

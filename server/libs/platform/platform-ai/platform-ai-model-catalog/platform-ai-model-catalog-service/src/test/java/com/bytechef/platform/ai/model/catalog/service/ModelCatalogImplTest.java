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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.ai.model.catalog.CatalogModel;
import com.bytechef.platform.ai.model.catalog.CatalogModel.Status;
import com.bytechef.platform.ai.model.catalog.CatalogProvider;
import com.bytechef.platform.ai.model.catalog.Limit;
import com.bytechef.platform.ai.model.catalog.Modalities;
import com.bytechef.platform.ai.model.catalog.modelsdev.ModelsDevSnapshotLoader;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModelCatalogImplTest {

    @Mock
    private ModelsDevSnapshotLoader modelsDevSnapshotLoader;

    private static CatalogModel model(String id) {
        return new CatalogModel(
            id, id, null, null, false, false, false, false, false, false, null, null, null, Status.ACTIVE,
            new Modalities(List.of(), List.of()), new Limit(null, null, null), null);
    }

    private static Map<String, CatalogProvider> providers(String providerId, String modelId) {
        return Map.of(providerId, new CatalogProvider(providerId, providerId, null, Map.of(modelId, model(modelId))));
    }

    @Test
    void testFetchModelReturnsModel() {
        when(modelsDevSnapshotLoader.load()).thenReturn(providers("acme", "acme-large"));

        ModelCatalogImpl modelCatalog = new ModelCatalogImpl(modelsDevSnapshotLoader);

        assertThat(modelCatalog.fetchModel("acme", "acme-large")).isPresent();
    }

    @Test
    void testFetchModelReturnsEmptyForUnknownProviderOrModel() {
        when(modelsDevSnapshotLoader.load()).thenReturn(providers("acme", "acme-large"));

        ModelCatalogImpl modelCatalog = new ModelCatalogImpl(modelsDevSnapshotLoader);

        assertThat(modelCatalog.fetchModel("nope", "acme-large")).isEmpty();
        assertThat(modelCatalog.fetchModel("acme", "nope")).isEmpty();
    }

    @Test
    void testGetModelsReturnsEmptyListForUnknownProvider() {
        when(modelsDevSnapshotLoader.load()).thenReturn(providers("acme", "acme-large"));

        ModelCatalogImpl modelCatalog = new ModelCatalogImpl(modelsDevSnapshotLoader);

        assertThat(modelCatalog.getModels("nope")).isEmpty();
        assertThat(modelCatalog.getModels("acme")).hasSize(1);
    }

    @Test
    void testSnapshotIsLoadedLazilyAndOnlyOnce() {
        when(modelsDevSnapshotLoader.load()).thenReturn(providers("acme", "acme-large"));

        ModelCatalogImpl modelCatalog = new ModelCatalogImpl(modelsDevSnapshotLoader);

        verify(modelsDevSnapshotLoader, times(0)).load();

        modelCatalog.getProviders();
        modelCatalog.getProviders();

        verify(modelsDevSnapshotLoader, times(1)).load();
    }

    @Test
    void testReplaceCatalogSwapsContentAndAdvancesLoadedAt() {
        when(modelsDevSnapshotLoader.load()).thenReturn(providers("acme", "acme-large"));

        ModelCatalogImpl modelCatalog = new ModelCatalogImpl(modelsDevSnapshotLoader);

        Instant before = modelCatalog.getLoadedAt();
        Instant refreshedAt = before.plusSeconds(60);

        modelCatalog.replaceCatalog(providers("beta", "beta-small"), refreshedAt);

        assertThat(modelCatalog.fetchModel("acme", "acme-large")).isEmpty();
        assertThat(modelCatalog.fetchModel("beta", "beta-small")).isPresent();
        assertThat(modelCatalog.getLoadedAt()).isEqualTo(refreshedAt);
    }

    /**
     * Regression test for {@code replaceCatalog} not sharing {@code getCatalog()}'s monitor. Without
     * {@code synchronized} on {@code replaceCatalog}, a thread already inside the lazy-load's synchronized block can
     * finish its (slow) bundled-snapshot parse <em>after</em> a concurrent {@code replaceCatalog} call and overwrite
     * fresh data with the stale snapshot. This drives that exact interleaving: a lazy load is blocked mid-parse, a
     * concurrent {@code replaceCatalog} is proven to block behind it rather than racing ahead, and only once the lazy
     * load is allowed to finish does {@code replaceCatalog} proceed and win — leaving the fresh data installed last, as
     * it must.
     */
    @Test
    void testReplaceCatalogDuringConcurrentLazyLoadInstallsFreshDataLast() throws InterruptedException {
        CountDownLatch lazyLoadStarted = new CountDownLatch(1);
        CountDownLatch releaseLazyLoad = new CountDownLatch(1);

        when(modelsDevSnapshotLoader.load()).thenAnswer(invocation -> {
            lazyLoadStarted.countDown();

            assertThat(releaseLazyLoad.await(5, TimeUnit.SECONDS))
                .as("test setup: releaseLazyLoad must be counted down by the main thread")
                .isTrue();

            return providers("stale", "stale-model");
        });

        ModelCatalogImpl modelCatalog = new ModelCatalogImpl(modelsDevSnapshotLoader);

        Thread lazyLoadThread = new Thread(modelCatalog::getProviders, "lazy-load");

        lazyLoadThread.start();

        assertThat(lazyLoadStarted.await(5, TimeUnit.SECONDS))
            .as("the lazy load must have entered the synchronized block before replaceCatalog is attempted")
            .isTrue();

        CountDownLatch replaceCatalogReturned = new CountDownLatch(1);
        Instant freshLoadedAt = Instant.now()
            .plusSeconds(3600);
        Thread replaceCatalogThread = new Thread(() -> {
            modelCatalog.replaceCatalog(providers("fresh", "fresh-model"), freshLoadedAt);

            replaceCatalogReturned.countDown();
        }, "replace-catalog");

        replaceCatalogThread.start();

        assertThat(replaceCatalogReturned.await(200, TimeUnit.MILLISECONDS))
            .as("replaceCatalog must block behind the in-progress lazy load's monitor, not race ahead of it")
            .isFalse();

        releaseLazyLoad.countDown();

        lazyLoadThread.join(5000);
        replaceCatalogThread.join(5000);

        assertThat(modelCatalog.fetchModel("stale", "stale-model")).isEmpty();
        assertThat(modelCatalog.fetchModel("fresh", "fresh-model")).isPresent();
        assertThat(modelCatalog.getLoadedAt()).isEqualTo(freshLoadedAt);
    }
}

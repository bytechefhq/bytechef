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

package com.bytechef.platform.ai.model.catalog.modelsdev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.platform.ai.model.catalog.CatalogProvider;
import com.bytechef.platform.ai.model.catalog.service.ModelCatalogImpl;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModelsDevRefresherTest {

    private static final String VALID_BODY = """
        {"acme": {"id": "acme", "name": "Acme", "models": {
            "acme-large": {"id": "acme-large", "name": "Acme Large",
                           "modalities": {"input": ["text"], "output": ["text"]},
                           "limit": {"context": 100}, "cost": {"input": 1, "output": 2}}}}}
        """;

    @Mock
    private ModelCatalogImpl modelCatalog;

    @Test
    void testRefreshInstallsFetchedCatalog() {
        ModelsDevRefresher refresher = new ModelsDevRefresher(
            modelCatalog, () -> VALID_BODY.getBytes(StandardCharsets.UTF_8));

        refresher.refresh();

        verify(modelCatalog).replaceCatalog(any(), any(Instant.class));
    }

    @Test
    void testRefreshKeepsCurrentCatalogWhenFetchThrows() {
        ModelsDevRefresher refresher = new ModelsDevRefresher(modelCatalog, () -> {
            throw new IllegalStateException("connection reset");
        });

        refresher.refresh();

        verify(modelCatalog, never()).replaceCatalog(any(), any(Instant.class));
    }

    @Test
    void testRefreshKeepsCurrentCatalogWhenBodyIsMalformed() {
        ModelsDevRefresher refresher = new ModelsDevRefresher(
            modelCatalog, () -> "<html>502 Bad Gateway</html>".getBytes(StandardCharsets.UTF_8));

        refresher.refresh();

        verify(modelCatalog, never()).replaceCatalog(any(), any(Instant.class));
    }

    @Test
    void testRefreshKeepsCurrentCatalogWhenBodyParsesToZeroProviders() {
        ModelsDevRefresher refresher = new ModelsDevRefresher(
            modelCatalog, () -> "{}".getBytes(StandardCharsets.UTF_8));

        refresher.refresh();

        verify(modelCatalog, never()).replaceCatalog(any(), any(Instant.class));
    }

    /**
     * A realistic upstream error envelope: syntactically valid JSON, so {@code readTree} never throws, and non-empty at
     * the top level, so the zero-provider guard never fires either. {@link ModelsDevParser} builds one
     * {@link CatalogProvider} per top-level key regardless of its shape — {@code "error"} has no {@code models} object,
     * so it parses to a provider with zero models. Only a guard on total model count catches this; a guard on provider
     * count, as covered by {@link #testRefreshKeepsCurrentCatalogWhenBodyParsesToZeroProviders}, does not.
     */
    @Test
    void testRefreshKeepsCurrentCatalogWhenBodyParsesToContentFreeProviders() {
        ModelsDevRefresher refresher = new ModelsDevRefresher(
            modelCatalog, () -> "{\"error\": \"unavailable\"}".getBytes(StandardCharsets.UTF_8));

        refresher.refresh();

        verify(modelCatalog, never()).replaceCatalog(any(), any(Instant.class));
    }

    @Test
    void testFetchedCatalogRetainsParsedContent() {
        ModelsDevRefresher refresher = new ModelsDevRefresher(
            modelCatalog, () -> VALID_BODY.getBytes(StandardCharsets.UTF_8));

        refresher.refresh();

        org.mockito.ArgumentCaptor<Map<String, CatalogProvider>> captor =
            org.mockito.ArgumentCaptor.forClass(Map.class);

        verify(modelCatalog).replaceCatalog(captor.capture(), any(Instant.class));

        assertThat(captor.getValue()).containsKey("acme");
    }
}

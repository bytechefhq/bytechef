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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.ai.model.catalog.CatalogProvider;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Guards the shipped snapshot itself. A bad {@code refreshModelsDevSnapshot} commit — a truncated download, an error
 * page saved as JSON, an upstream rename of a provider id — fails here rather than reaching a deployment.
 */
class ModelsDevSnapshotLoaderTest {

    @Test
    void testBundledSnapshotParses() {
        Map<String, CatalogProvider> providers = new ModelsDevSnapshotLoader().load();

        assertThat(providers).hasSizeGreaterThan(100);
    }

    @Test
    void testBundledSnapshotContainsEveryGatewayMappedProvider() {
        Map<String, CatalogProvider> providers = new ModelsDevSnapshotLoader().load();

        assertThat(providers).containsKeys(
            "anthropic", "azure", "cohere", "deepseek", "google", "groq", "mistral", "openai");
    }

    @Test
    void testBundledSnapshotHasPricedModels() {
        Map<String, CatalogProvider> providers = new ModelsDevSnapshotLoader().load();

        assertThat(providers.get("anthropic")
            .models()).isNotEmpty();
        assertThat(
            providers.get("anthropic")
                .models()
                .values()
                .stream()
                .anyMatch(model -> model.cost() != null)).isTrue();
    }

    /**
     * The spec's error table requires a bad bundled snapshot to "fail fast at first access." A snapshot that parses to
     * zero models in total — one or more syntactically valid provider entries, none of them carrying any models, e.g.
     * an upstream error envelope saved in place of the real document — must not install an empty catalog that makes
     * every model look uncatalogued. Exercises {@code parseAndValidate} directly rather than {@code load()}: the
     * bundled classpath resource is fixed, so this is the only seam that lets the test supply content-free bytes.
     */
    @Test
    void testContentFreeSnapshotFailsFast() {
        String contentFreeJson = "{\"error\": \"unavailable\"}";

        assertThatThrownBy(
            () -> ModelsDevSnapshotLoader.parseAndValidate(
                new ByteArrayInputStream(contentFreeJson.getBytes(StandardCharsets.UTF_8))))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("zero models");
    }

    @Test
    void testEmptyDocumentFailsFast() {
        assertThatThrownBy(
            () -> ModelsDevSnapshotLoader.parseAndValidate(
                new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8))))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("zero models");
    }
}

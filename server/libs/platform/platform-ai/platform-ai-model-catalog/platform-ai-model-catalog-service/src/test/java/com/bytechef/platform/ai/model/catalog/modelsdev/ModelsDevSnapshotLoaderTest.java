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

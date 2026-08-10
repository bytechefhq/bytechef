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

import com.bytechef.platform.ai.model.catalog.CatalogModel;
import com.bytechef.platform.ai.model.catalog.CatalogModel.Status;
import com.bytechef.platform.ai.model.catalog.CatalogProvider;
import com.bytechef.platform.ai.model.catalog.Cost;
import com.bytechef.platform.ai.model.catalog.Modalities.Modality;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModelsDevParserTest {

    private static Map<String, CatalogProvider> parseFixture() {
        try (InputStream inputStream = ModelsDevParserTest.class.getResourceAsStream(
            "/model-catalog/test-api.json")) {

            return ModelsDevParser.parse(inputStream);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    void testParseReadsProviderMetadata() {
        CatalogProvider provider = parseFixture().get("acme");

        assertThat(provider.id()).isEqualTo("acme");
        assertThat(provider.name()).isEqualTo("Acme");
        assertThat(provider.doc()).isEqualTo("https://acme.example/docs");
        assertThat(provider.models()).hasSize(4);
    }

    @Test
    void testParseReadsCostAsBigDecimal() {
        CatalogModel model = parseFixture().get("acme")
            .models()
            .get("acme-large");

        assertThat(model.cost()).isNotNull();
        assertThat(model.cost()
            .input()).isEqualByComparingTo(new BigDecimal("3"));
        assertThat(model.cost()
            .output()).isEqualByComparingTo(new BigDecimal("15"));
        assertThat(model.cost()
            .cacheRead()).isEqualByComparingTo(new BigDecimal("0.003625"));
    }

    @Test
    void testParseReadsTiersAndIgnoresContextOver200k() {
        CatalogModel model = parseFixture().get("acme")
            .models()
            .get("acme-large");
        Cost cost = model.cost();

        assertThat(cost).isNotNull();
        assertThat(cost.tiers()).hasSize(1);
        assertThat(cost.tiers()
            .getFirst()
            .contextSize()).isEqualTo(200000);
        assertThat(cost.tiers()
            .getFirst()
            .input()).isEqualByComparingTo(new BigDecimal("6"));
    }

    @Test
    void testParseReadsDatesAndLimits() {
        CatalogModel model = parseFixture().get("acme")
            .models()
            .get("acme-large");

        assertThat(model.releaseDate()).isEqualTo(LocalDate.of(2025, 6, 1));
        assertThat(model.lastUpdated()).isEqualTo(LocalDate.of(2025, 9, 15));
        assertThat(model.limit()
            .context()).isEqualTo(200000);
        assertThat(model.limit()
            .output()).isEqualTo(64000);
        assertThat(model.limit()
            .input()).isNull();
    }

    @Test
    void testParseDropsUnknownModality() {
        CatalogModel model = parseFixture().get("acme")
            .models()
            .get("acme-large");

        assertThat(model.modalities()
            .input()).containsExactly(Modality.TEXT, Modality.IMAGE);
    }

    @Test
    void testParseModelWithoutCostYieldsNullCost() {
        CatalogModel model = parseFixture().get("acme")
            .models()
            .get("acme-free");

        assertThat(model.cost()).isNull();
        assertThat(model.openWeights()).isTrue();
        assertThat(model.structuredOutput()).isFalse();
    }

    @Test
    void testParseReadsDeprecatedStatus() {
        CatalogModel model = parseFixture().get("acme")
            .models()
            .get("acme-old");

        assertThat(model.status()).isEqualTo(Status.DEPRECATED);
    }

    @Test
    void testParseMapsUnknownStatusToActive() {
        CatalogModel model = parseFixture().get("acme")
            .models()
            .get("acme-vision");

        assertThat(model.status()).isEqualTo(Status.ACTIVE);
        assertThat(model.modalities()
            .output()).containsExactly(Modality.IMAGE);
    }

    /**
     * Documents how complete the leniency is: a provider whose value is a bare string, not an object, still yields a
     * usable (empty) entry rather than aborting the parse or dropping its healthy siblings. That is the correct outcome
     * on a refresh path, where one malformed upstream contribution must never blank the catalog.
     */
    @Test
    void testParseToleratesGarbageProviderAndKeepsSiblings() {
        String json = """
            {"good": {"id": "good", "name": "Good", "models": {}},
             "bad": "this-should-be-an-object"}
            """;

        Map<String, CatalogProvider> providers = ModelsDevParser.parse(
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        assertThat(providers).containsOnlyKeys("good", "bad");
        assertThat(providers.get("bad")
            .models()).isEmpty();
    }

    /**
     * {@code Map.copyOf} was used to freeze both the provider map and each provider's model map, but its iteration
     * order is unspecified and salt-randomized per JVM run — reshuffling upstream's order on every restart. The models
     * are built in a {@link java.util.LinkedHashMap} for exactly this reason; freezing must preserve that order rather
     * than discard it.
     */
    @Test
    void testParsePreservesModelInsertionOrder() {
        CatalogProvider provider = parseFixture().get("acme");

        assertThat(provider.models()
            .keySet()).containsExactly("acme-large", "acme-free", "acme-old", "acme-vision");
    }

    @Test
    void testParsePreservesProviderInsertionOrder() {
        String json = """
            {"zulu": {"id": "zulu", "name": "Zulu", "models": {}},
             "alpha": {"id": "alpha", "name": "Alpha", "models": {}},
             "mike": {"id": "mike", "name": "Mike", "models": {}}}
            """;

        Map<String, CatalogProvider> providers = ModelsDevParser.parse(
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        assertThat(providers.keySet()).containsExactly("zulu", "alpha", "mike");
    }

    @Test
    void testParseReturnsUnmodifiableMaps() {
        Map<String, CatalogProvider> providers = parseFixture();

        assertThatThrownBy(() -> providers.put("new-provider", null))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> providers.get("acme")
            .models()
            .put("new-model", null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testParseSkipsModelWhoseFieldThrows() {
        String json = """
            {"acme": {"id": "acme", "name": "Acme", "models": {
                "good": {"id": "good", "name": "Good", "modalities": {"input": ["text"], "output": ["text"]},
                         "limit": {"context": 100}},
                "bad": {"id": "bad", "name": "Bad", "cost": {"input": 1e999999999}}
            }}}
            """;

        Map<String, CatalogProvider> providers = ModelsDevParser.parse(
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        assertThat(providers.get("acme")
            .models()).containsOnlyKeys("good");
    }
}

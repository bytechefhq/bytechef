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

import com.bytechef.platform.ai.model.catalog.CatalogModel;
import com.bytechef.platform.ai.model.catalog.CatalogModel.Status;
import com.bytechef.platform.ai.model.catalog.CatalogProvider;
import com.bytechef.platform.ai.model.catalog.Cost;
import com.bytechef.platform.ai.model.catalog.CostTier;
import com.bytechef.platform.ai.model.catalog.Limit;
import com.bytechef.platform.ai.model.catalog.Modalities;
import com.bytechef.platform.ai.model.catalog.Modalities.Modality;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Parses the models.dev {@code api.json} document into {@link CatalogProvider} records.
 *
 * <p>
 * Parsing is deliberately lenient. models.dev is community-maintained: new capability flags, modalities, and status
 * values appear without notice, and a single malformed contribution reaches the published document before anyone
 * notices. A strict parse would let any of those blank the entire catalog on the next scheduled refresh, so unknown
 * vocabulary is dropped and an unparseable model is skipped while its siblings survive.
 *
 * @author Ivica Cardic
 */
public final class ModelsDevParser {

    private static final Logger log = LoggerFactory.getLogger(ModelsDevParser.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ModelsDevParser() {
    }

    public static Map<String, CatalogProvider> parse(InputStream inputStream) {
        JsonNode rootJsonNode = OBJECT_MAPPER.readTree(inputStream);

        Map<String, CatalogProvider> providers = new LinkedHashMap<>();

        for (Map.Entry<String, JsonNode> providerEntry : rootJsonNode.properties()) {
            String providerId = providerEntry.getKey();

            try {
                providers.put(providerId, parseProvider(providerId, providerEntry.getValue()));
            } catch (RuntimeException exception) {
                log.debug("Skipping unparseable models.dev provider '{}'", providerId, exception);
            }
        }

        // LinkedHashMap, not Map.copyOf: preserving the document's upstream order is deliberate — Map.copyOf's
        // iteration order is unspecified and salt-randomized per JVM run, which would make provider- and model-list
        // ordering nondeterministic for every future consumer of this map, not just this one.
        return Collections.unmodifiableMap(providers);
    }

    private static CatalogProvider parseProvider(String providerId, JsonNode providerJsonNode) {
        Map<String, CatalogModel> models = new LinkedHashMap<>();

        JsonNode modelsJsonNode = providerJsonNode.path("models");

        for (Map.Entry<String, JsonNode> modelEntry : modelsJsonNode.properties()) {
            String modelId = modelEntry.getKey();

            try {
                models.put(modelId, parseModel(modelId, modelEntry.getValue()));
            } catch (RuntimeException exception) {
                log.debug("Skipping unparseable models.dev model '{}/{}'", providerId, modelId, exception);
            }
        }

        return new CatalogProvider(
            text(providerJsonNode, "id", providerId), text(providerJsonNode, "name", providerId),
            nullableText(providerJsonNode, "doc"), Collections.unmodifiableMap(models));
    }

    private static CatalogModel parseModel(String modelId, JsonNode modelJsonNode) {
        return new CatalogModel(
            text(modelJsonNode, "id", modelId), text(modelJsonNode, "name", modelId),
            nullableText(modelJsonNode, "description"), nullableText(modelJsonNode, "family"),
            bool(modelJsonNode, "attachment"), bool(modelJsonNode, "reasoning"), bool(modelJsonNode, "tool_call"),
            bool(modelJsonNode, "structured_output"), bool(modelJsonNode, "temperature"),
            bool(modelJsonNode, "open_weights"), nullableText(modelJsonNode, "knowledge"),
            date(modelJsonNode, "release_date"), date(modelJsonNode, "last_updated"),
            Status.fromWireValue(nullableText(modelJsonNode, "status")),
            parseModalities(modelJsonNode.path("modalities")), parseLimit(modelJsonNode.path("limit")),
            parseCost(modelJsonNode.path("cost")));
    }

    private static Modalities parseModalities(JsonNode modalitiesJsonNode) {
        return new Modalities(
            parseModalityList(modalitiesJsonNode.path("input")),
            parseModalityList(modalitiesJsonNode.path("output")));
    }

    private static List<Modality> parseModalityList(JsonNode modalityListJsonNode) {
        List<Modality> modalities = new ArrayList<>();

        for (JsonNode modalityJsonNode : modalityListJsonNode) {
            Modality modality = Modality.fromWireValue(modalityJsonNode.asString(null));

            if (modality != null) {
                modalities.add(modality);
            }
        }

        return List.copyOf(modalities);
    }

    private static Limit parseLimit(JsonNode limitJsonNode) {
        return new Limit(
            integer(limitJsonNode, "context"), integer(limitJsonNode, "input"), integer(limitJsonNode, "output"));
    }

    private static @Nullable Cost parseCost(JsonNode costJsonNode) {
        if (costJsonNode.isMissingNode() || costJsonNode.isNull()) {
            return null;
        }

        return new Cost(
            decimal(costJsonNode, "input"), decimal(costJsonNode, "output"), decimal(costJsonNode, "cache_read"),
            decimal(costJsonNode, "cache_write"), decimal(costJsonNode, "reasoning"),
            decimal(costJsonNode, "input_audio"), decimal(costJsonNode, "output_audio"),
            parseTiers(costJsonNode.path("tiers")));
    }

    /**
     * Reads the structured {@code cost.tiers} array and deliberately ignores the flat {@code cost.context_over_200k}
     * key that upstream publishes alongside it. Both describe the same thing, but the flat key hardcodes a threshold
     * into its own name and cannot describe a model tiered at 272k tokens — which several GPT-5.x entries are.
     */
    private static List<CostTier> parseTiers(JsonNode tiersJsonNode) {
        List<CostTier> tiers = new ArrayList<>();

        for (JsonNode tierJsonNode : tiersJsonNode) {
            tiers.add(
                new CostTier(
                    integer(tierJsonNode.path("tier"), "size"), decimal(tierJsonNode, "input"),
                    decimal(tierJsonNode, "output"), decimal(tierJsonNode, "cache_read"),
                    decimal(tierJsonNode, "cache_write")));
        }

        return List.copyOf(tiers);
    }

    private static boolean bool(JsonNode jsonNode, String fieldName) {
        return jsonNode.path(fieldName)
            .asBoolean(false);
    }

    /**
     * Reads a numeric rate through its text form so the {@link BigDecimal} carries the published digits exactly. Going
     * via {@code double} would round rates such as {@code 0.003625} before they ever reach a cost calculation.
     */
    private static @Nullable BigDecimal decimal(JsonNode jsonNode, String fieldName) {
        JsonNode valueJsonNode = jsonNode.path(fieldName);

        if (!valueJsonNode.isNumber()) {
            return null;
        }

        return new BigDecimal(valueJsonNode.asString());
    }

    private static @Nullable LocalDate date(JsonNode jsonNode, String fieldName) {
        String value = nullableText(jsonNode, fieldName);

        if (value == null) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static @Nullable Integer integer(JsonNode jsonNode, String fieldName) {
        JsonNode valueJsonNode = jsonNode.path(fieldName);

        if (!valueJsonNode.isNumber()) {
            return null;
        }

        return valueJsonNode.asInt();
    }

    private static @Nullable String nullableText(JsonNode jsonNode, String fieldName) {
        JsonNode valueJsonNode = jsonNode.path(fieldName);

        if (!valueJsonNode.isString()) {
            return null;
        }

        return valueJsonNode.asString();
    }

    private static String text(JsonNode jsonNode, String fieldName, String fallback) {
        String value = nullableText(jsonNode, fieldName);

        return value == null ? fallback : value;
    }
}

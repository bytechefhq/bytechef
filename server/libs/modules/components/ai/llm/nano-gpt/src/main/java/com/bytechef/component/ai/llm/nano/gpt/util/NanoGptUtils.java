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

package com.bytechef.component.ai.llm.nano.gpt.util;

import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.TypeReference;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Marko Kriskovic
 */
public class NanoGptUtils {

    private NanoGptUtils() {
    }

    public static ActionDefinition.OptionsFunction<String> getNanoGptChatModels() {
        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> {
            ModelsResponse response = context.http(http -> http.get("/models"))
                .configuration(Context.Http.responseType(Context.Http.ResponseType.JSON))
                .queryParameter("detailed", "true")
                .execute()
                .getBody(new TypeReference<>() {});

            return response.data()
                .stream()
                .sorted(Comparator
                    .comparingDouble(NanoGptModel::totalCost)
                    .thenComparing(model -> model.name() != null ? model.name() : model.id()))
                .map(model -> option(
                    formatModelLabel(model),
                    model.id(),
                    model.description()))
                .collect(Collectors.toList());
        };
    }

    public static ActionDefinition.OptionsFunction<String> getNanoGptEmbeddingModels() {
        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> {
            EmbeddingModelsResponse response = context.http(http -> http.get("/embedding-models"))
                .configuration(Context.Http.responseType(Context.Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            return response.data()
                .stream()
                .sorted(Comparator
                    .comparingDouble(NanoGptEmbeddingModel::pricePerMillionOrZero)
                    .thenComparing(model -> model.name() != null ? model.name() : model.id()))
                .map(model -> option(
                    formatEmbeddingModelLabel(model),
                    model.id(),
                    model.description()))
                .collect(Collectors.toList());
        };
    }

    public static ActionDefinition.OptionsFunction<String> getNanoGptImageModels() {
        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> {
            ImageModelsResponse response = context.http(http -> http.get("/image-models"))
                .configuration(Context.Http.responseType(Context.Http.ResponseType.JSON))
                .queryParameter("detailed", "true")
                .execute()
                .getBody(new TypeReference<>() {});

            return response.data()
                .stream()
                .sorted(Comparator
                    .comparingDouble(NanoGptImageModel::minPriceOrZero)
                    .thenComparing(model -> model.name() != null ? model.name() : model.id()))
                .map(model -> option(
                    formatImageModelLabel(model),
                    model.id(),
                    model.description()))
                .collect(Collectors.toList());
        };
    }

    public static ActionDefinition.OptionsFunction<String> getNanoGptSpeechModels() {
        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) ->
            fetchAudioModels("tts", context);
    }

    public static ActionDefinition.OptionsFunction<String> getNanoGptTranscriptionModels() {
        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) ->
            fetchAudioModels("stt", context);
    }

    private static List<com.bytechef.component.definition.Option<String>> fetchAudioModels(
            String type, Context context) {

        AudioModelsResponse response = context.http(http -> http.get("/audio-models"))
            .configuration(Context.Http.responseType(Context.Http.ResponseType.JSON))
            .queryParameter("detailed", "true")
            .queryParameter("type", type)
            .execute()
            .getBody(new TypeReference<>() {});

        return response.data()
            .stream()
            .sorted(Comparator
                .comparingDouble((NanoGptAudioModel model) -> model.priceOrZero(type))
                .thenComparing(model -> model.name() != null ? model.name() : model.id()))
            .map(model -> option(
                formatAudioModelLabel(model, type),
                model.id(),
                model.description()))
            .collect(Collectors.toList());
    }

    private static String formatEmbeddingModelLabel(NanoGptEmbeddingModel model) {
        String displayName = model.name() != null ? model.name() : model.id();

        if (model.pricing() == null) {
            return displayName;
        }

        return displayName + " - $" + formatCost(model.pricing().per_million_tokens()) + " per 1M tokens";
    }

    private static String formatImageModelLabel(NanoGptImageModel model) {
        String displayName = model.name() != null ? model.name() : model.id();

        if (model.pricing() == null || model.pricing().per_image() == null || model.pricing()
            .per_image()
            .isEmpty()) {
            return displayName;
        }

        return displayName + " - from $" + formatCost(model.minPriceOrZero()) + " per image";
    }

    private static String formatAudioModelLabel(NanoGptAudioModel model, String type) {
        String displayName = model.name() != null ? model.name() : model.id();

        if (model.pricing() == null) {
            return displayName;
        }

        if ("tts".equals(type) && model.pricing().per_thousand_chars() != null) {
            return displayName + " - $" + formatCost(model.pricing().per_thousand_chars()) + " per 1K chars";
        }

        if ("stt".equals(type) && model.pricing().per_minute() != null) {
            return displayName + " - $" + formatCost(model.pricing().per_minute()) + " per min";
        }

        return displayName;
    }

    private static String formatModelLabel(NanoGptModel model) {
        String displayName = model.name() != null ? model.name() : model.id();

        if (model.pricing() == null) {
            return displayName;
        }

        Pricing pricing = model.pricing();

        return displayName + " - $" + formatCost(pricing.promptOrZero()) + "/$"
            + formatCost(pricing.completionOrZero()) + " per 1M tokens";
    }

    private static String formatCost(double cost) {
        return new BigDecimal(String.format("%.10f", cost))
            .stripTrailingZeros()
            .toPlainString();
    }

    private record Pricing(Double prompt, Double completion, String currency, String unit) {

        double promptOrZero() {
            return prompt != null ? prompt : 0.0;
        }

        double completionOrZero() {
            return completion != null ? completion : 0.0;
        }

        double totalCost() {
            return promptOrZero() + completionOrZero();
        }
    }

    private record NanoGptModel(String id, String name, String description, Pricing pricing) {

        double totalCost() {
            return pricing != null ? pricing.totalCost() : 0.0;
        }
    }

    private record ModelsResponse(List<NanoGptModel> data) {
    }

    private record EmbeddingPricing(double per_million_tokens, String currency) {
    }

    private record NanoGptEmbeddingModel(String id, String name, String description, Integer dimensions,
        Boolean supports_dimensions, Integer max_tokens, EmbeddingPricing pricing) {

        double pricePerMillionOrZero() {
            return pricing != null ? pricing.per_million_tokens() : 0.0;
        }
    }

    private record EmbeddingModelsResponse(List<NanoGptEmbeddingModel> data) {
    }

    private record ImagePricing(Map<String, Double> per_image, String currency) {
    }

    private record NanoGptImageModel(String id, String name, String description, ImagePricing pricing) {

        double minPriceOrZero() {
            if (pricing == null || pricing.per_image() == null || pricing.per_image()
                .isEmpty()) {
                return 0.0;
            }

            return pricing.per_image()
                .values()
                .stream()
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0.0);
        }
    }

    private record ImageModelsResponse(List<NanoGptImageModel> data) {
    }

    private record AudioPricing(Double per_thousand_chars, Double per_minute, Double per_second, String currency) {
    }

    private record NanoGptAudioModel(String id, String name, String description, AudioPricing pricing) {

        double priceOrZero(String type) {
            if (pricing == null) {
                return 0.0;
            }

            if ("tts".equals(type)) {
                return pricing.per_thousand_chars() != null ? pricing.per_thousand_chars() : 0.0;
            }

            return pricing.per_minute() != null ? pricing.per_minute() : 0.0;
        }
    }

    private record AudioModelsResponse(List<NanoGptAudioModel> data) {
    }
}

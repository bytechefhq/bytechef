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

package com.bytechef.component.ai.llm.router.open.router.util;

import static com.bytechef.component.ai.llm.router.constant.RouterConstants.SUPPORTED_PARAMETERS;
import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.TypeReference;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Marko Kriskovic
 */
public class OpenRouterUtils {
    public static ActionDefinition.OptionsFunction<String> getOpenRouterModels(String outputType) {
        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> {
            List<String> supportedParametersArray = inputParameters.getList(SUPPORTED_PARAMETERS, String.class);
            String supportedParametersString = supportedParametersArray != null && !supportedParametersArray.isEmpty()
                ? String.join(",", supportedParametersArray)
                : null;

            Context.Http.Executor executor = context.http(http -> http.get("/models"))
                .configuration(Context.Http.responseType(Context.Http.ResponseType.JSON))
                .queryParameter("output_modalities", outputType);

            if (supportedParametersString != null) {
                executor = executor.queryParameter("supported_parameters", supportedParametersString);
            }

            ModelsResponse response = executor.execute()
                .getBody(new TypeReference<>() {});

            return response.data()
                .stream()
                .sorted(Comparator.comparingDouble((OpenRouterModel model) -> model.pricing()
                    .totalCost())
                    .thenComparing(OpenRouterModel::name))
                .map(model -> option(model.name() + " - $" + formatCost(model.pricing()
                    .totalCost()), model.id(), model.description()))
                .collect(Collectors.toList());
        };
    }

    public static ActionDefinition.OptionsFunction<String> getOpenRouterEmbeddingModels() {
        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> {
            ModelsResponse response = context.http(http -> http.get("/embeddings/models"))
                .configuration(Context.Http.responseType(Context.Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            return response.data()
                .stream()
                .sorted(Comparator.comparingDouble((OpenRouterModel model) -> model.pricing()
                    .totalCost())
                    .thenComparing(OpenRouterModel::name))
                .map(model -> option(model.name() + " - $" + formatCost(model.pricing()
                    .totalCost()), model.id(), model.description()))
                .collect(Collectors.toList());
        };
    }

    private static String formatCost(double cost) {
        return new BigDecimal(String.format("%.10f", cost))
            .stripTrailingZeros()
            .toPlainString();
    }

    private record Pricing(String completion, String prompt, String audio, String audio_output, String image,
        String image_output, String image_token, String input_audio_cache, String input_cache_read,
        String input_cache_write, String internal_reasoning, String request, String web_search) {

        double totalCost() {
            return parseOrZero(completion) + parseOrZero(prompt) + parseOrZero(audio) + parseOrZero(audio_output)
                + parseOrZero(image) + parseOrZero(image_output) + parseOrZero(image_token)
                + parseOrZero(input_audio_cache) + parseOrZero(input_cache_read) + parseOrZero(input_cache_write)
                + parseOrZero(internal_reasoning) + parseOrZero(request) + parseOrZero(web_search);
        }

        private static double parseOrZero(String value) {
            return value == null ? 0 : Double.parseDouble(value);
        }
    }

    private record OpenRouterModel(String name, String id, String description, Pricing pricing,
        List<String> supported_parameters) {
    }

    private record ModelsResponse(List<OpenRouterModel> data) {
    }
}

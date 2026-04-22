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

package com.bytechef.component.ai.llm.open.router.util;

import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.SUPPORTED_PARAMETERS;
import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.TypeReference;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Marko Kriskovic
 */
public class OpenRouterUtils {
    public static ActionDefinition.OptionsFunction<String> getOpenRouterModels() {
        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> {
            List<String> supportedParametersArray = inputParameters.getList(SUPPORTED_PARAMETERS, String.class);
            String supportedParametersString = supportedParametersArray != null && !supportedParametersArray.isEmpty()
                ? String.join(",", supportedParametersArray)
                : null;

            Context.Http.Executor executor = context.http(http -> http.get("/models"))
                .configuration(Context.Http.responseType(Context.Http.ResponseType.JSON))
                .queryParameter("output_modalities", "text");

            if (supportedParametersString != null) {
                executor = executor.queryParameter("supported_parameters", supportedParametersString);
            }

            ModelsResponse response = executor.execute()
                .getBody(new TypeReference<>() {});

            return response.data()
                .stream()
                .filter((model) -> Double.parseDouble(model.pricing.completion) >= 0)
                .sorted(Comparator.comparingDouble((OpenRouterModel model) -> Double.parseDouble(model.pricing()
                    .completion()))
                    .thenComparing(OpenRouterModel::name))
                .map(model -> option(model.name() + " - $" + model.pricing()
                    .completion(), model.id(), model.description()))
                .collect(Collectors.toList());
        };
    }

    private record Pricing(String completion, String prompt, String request) {
    }

    private record OpenRouterModel(String name, String id, String description, Pricing pricing,
        List<String> supported_parameters) {
    }

    private record ModelsResponse(List<OpenRouterModel> data) {
    }
}

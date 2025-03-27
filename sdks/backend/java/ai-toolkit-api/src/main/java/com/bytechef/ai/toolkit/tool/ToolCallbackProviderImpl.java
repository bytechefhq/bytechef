/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.ai.toolkit.tool;

import com.bytechef.ai.toolkit.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

/**
 * @author Ivica Cardic
 */
class ToolCallbackProviderImpl implements ToolCallbackProvider {

    private final String externalUserId;
    private final String apiKey;
    private final Environment environment;
    // TODO Convert to HttpClient
    private final RestClient restClient;

    public ToolCallbackProviderImpl(
        String externalUserId, String apiKey, Environment environment, RestClient restClient) {

        this.externalUserId = externalUserId;
        this.apiKey = apiKey;
        this.environment = environment;
        this.restClient = restClient;
    }

    @Override
    @SuppressFBWarnings("NP")
    public ToolCallback[] getToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        List<ToolModel> toolModels = restClient.get()
            // TODO
            .uri("http://localhost:9555/api/embedded/v1/tools?externalUserId=%s".formatted(externalUserId))
            .header("Authorization", "Bearer %s".formatted(apiKey))
            .header("X-Environment", environment.name())
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, List<ToolModel>>>() {})
            .entrySet()
            .stream()
            .flatMap(entry -> entry.getValue()
                .stream())
            .toList();

        for (ToolModel toolModel : toolModels) {
            FunctionModel functionModel = toolModel.function();

            FunctionToolCallback.Builder<Map<String, Object>, Object> builder = FunctionToolCallback
                .builder(
                    functionModel.name(),
                    getToolCallbackFunction(functionModel.name(), externalUserId, apiKey, environment))
                .inputType(Map.class)
                .inputSchema(functionModel.parameters());

            if (functionModel.description() != null) {
                builder.description(functionModel.description());
            }

            toolCallbacks.add(builder.build());
        }

        return toolCallbacks.toArray(new ToolCallback[0]);
    }

    private Function<Map<String, Object>, Object> getToolCallbackFunction(
        String toolName, String externalUserId, String apiKey, Environment environment) {

        return parameters -> restClient.post()
            // TODO
            .uri("http://localhost:9555/api/embedded/v1/tools?externalUserId=%s".formatted(externalUserId))
            .header("Authorization", "Bearer %s".formatted(apiKey))
            .header("X-Environment", environment.name())
            .body(
                Map.of(
                    "name", toolName,
                    "parameters", parameters))
            .retrieve()
            .body(Object.class);
    }

    record ToolModel(FunctionModel function, String type) {
    }

    record FunctionModel(String name, String description, String parameters) {
    }
}

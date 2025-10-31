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

package com.bytechef.ai.toolkit.tool;

import com.bytechef.ai.toolkit.Environment;
import com.bytechef.ai.toolkit.ToolClient;
import com.bytechef.ai.toolkit.model.FunctionModel;
import com.bytechef.ai.toolkit.model.ToolModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;

/**
 * @author Ivica Cardic
 */
class ToolCallbackProviderImpl implements ToolCallbackProvider {

    private final String apiKey;
    private final String baseUrl;
    private final Environment environment;
    private final String externalUserId;

    public ToolCallbackProviderImpl(
        String apiKey, String baseUrl, Environment environment, String externalUserId) {

        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.environment = environment;
        this.externalUserId = externalUserId;
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        ToolClient toolClient = new ToolClient(apiKey, baseUrl, environment);

        List<ToolModel> toolModels = toolClient.getTools(externalUserId)
            .values()
            .stream()
            .flatMap(List::stream)
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

        return parameters -> {
            ToolClient toolClient = new ToolClient(apiKey, baseUrl, environment);

            return toolClient.executeTool(externalUserId, toolName, parameters);
        };
    }
}

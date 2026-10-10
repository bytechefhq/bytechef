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

package com.bytechef.component.ai.agent.utils.cluster;

import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import java.util.List;
import org.springaicommunity.agent.tools.BraveWebSearchTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * @author Ivica Cardic
 */
public class AiAgentUtilsBraveWebSearchTool {

    public static final String BRAVE_API_KEY = "braveApiKey";
    public static final String RESULT_COUNT = "resultCount";

    public static final ClusterElementDefinition<ToolCallbackProviderFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ToolCallbackProviderFunction>clusterElement("braveWebSearchTool")
            .title("Brave Web Search Tool")
            .description("Web search with domain filtering using the Brave Search API.")
            .type(TOOLS)
            .properties(
                integer(RESULT_COUNT)
                    .label("Result Count")
                    .description("Number of search results returned per query. Leave empty for the default of 10.")
                    .minValue(1)
                    .maxValue(20)
                    .advancedOption(true))
            .object(() -> AiAgentUtilsBraveWebSearchTool::apply);

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        String apiKey = connectionParameters.getString(BRAVE_API_KEY);

        if (apiKey == null || apiKey.isBlank()) {
            return ToolCallbackProvider.from(List.of());
        }

        BraveWebSearchTool.Builder builder = BraveWebSearchTool.builder(apiKey);

        Integer resultCount = inputParameters.getInteger(RESULT_COUNT);

        if (resultCount != null) {
            builder.resultCount(resultCount);
        }

        return ToolCallbackProvider.from(ToolCallbacks.from(builder.build()));
    }
}

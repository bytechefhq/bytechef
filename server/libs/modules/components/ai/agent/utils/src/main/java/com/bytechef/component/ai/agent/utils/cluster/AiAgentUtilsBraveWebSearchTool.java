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

import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agent.tools.BraveWebSearchTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * Provides web search with domain filtering using the Brave Search API.
 *
 * @author Ivica Cardic
 */
public class AiAgentUtilsBraveWebSearchTool {

    private static final Logger log = LoggerFactory.getLogger(AiAgentUtilsBraveWebSearchTool.class);

    public static final String BRAVE_API_KEY = "braveApiKey";

    public static final ClusterElementDefinition<ToolCallbackProviderFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ToolCallbackProviderFunction>clusterElement("braveWebSearchTool")
            .title("Brave Web Search Tool")
            .description("Web search with domain filtering using the Brave Search API.")
            .type(TOOLS)
            .object(() -> AiAgentUtilsBraveWebSearchTool::apply);

    /**
     * Reads the API key under this element's own {@link #BRAVE_API_KEY} first, then under
     * {@link Authorization#API_TOKEN} — the key {@code BraveConnection} actually stores it under. Both are checked
     * because this element lives on {@code aiAgentUtils}, which has no connection of its own, so whatever connection
     * reaches it was wired by something else and may use either name.
     * <p>
     * A missing key is logged rather than swallowed. Returning an empty provider silently made a misconfigured agent
     * look like one that simply chose not to search.
     */
    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        String apiKey = connectionParameters.getString(BRAVE_API_KEY);

        if (apiKey == null || apiKey.isBlank()) {
            apiKey = connectionParameters.getString(Authorization.API_TOKEN);
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn(
                "No Brave API key found under '{}' or '{}' — the web search tool is registered but will expose no " +
                    "tools. Use the brave component's own webSearch tool instead, which carries its connection.",
                BRAVE_API_KEY, Authorization.API_TOKEN);

            return ToolCallbackProvider.from(List.of());
        }

        BraveWebSearchTool braveWebSearchTool = BraveWebSearchTool.builder(apiKey)
            .build();

        return ToolCallbackProvider.from(ToolCallbacks.from(braveWebSearchTool));
    }
}

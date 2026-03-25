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

import static com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeToolFunction.CLAUDE_CODE_TOOLS;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeToolFunction;
import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.agent.tools.BraveWebSearchTool;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * Provides web search with domain filtering using the Brave Search API.
 *
 * @author Ivica Cardic
 */
public class AgentUtilsBraveWebSearchTool {

    public static final String BRAVE_API_KEY = "braveApiKey";

    public static final ClusterElementDefinition<ClaudeCodeToolFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ClaudeCodeToolFunction>clusterElement("braveWebSearchTool")
            .title("Brave Web Search Tool")
            .description("Web search with domain filtering using the Brave Search API.")
            .type(CLAUDE_CODE_TOOLS)
            .object(() -> AgentUtilsBraveWebSearchTool::apply);

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters, Path workingDirectory,
        @Nullable ChatModel chatModel) {

        String apiKey = connectionParameters.getString(BRAVE_API_KEY);

        if (apiKey == null || apiKey.isBlank()) {
            return ToolCallbackProvider.from(List.of());
        }

        BraveWebSearchTool braveWebSearchTool = BraveWebSearchTool.builder(apiKey)
            .build();

        return ToolCallbackProvider.from(ToolCallbacks.from(braveWebSearchTool));
    }
}

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
import org.jspecify.annotations.Nullable;
import org.springaicommunity.agent.tools.GlobTool;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * Provides fast file pattern matching for finding files by name patterns with glob syntax.
 *
 * @author Ivica Cardic
 */
public class AiAgentUtilsGlobTool {

    public static final ClusterElementDefinition<ClaudeCodeToolFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ClaudeCodeToolFunction>clusterElement("globTool")
            .title("Glob Tool")
            .description("Fast file pattern matching tool for finding files by name patterns with glob syntax.")
            .type(CLAUDE_CODE_TOOLS)
            .object(() -> AiAgentUtilsGlobTool::apply);

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters, Path workingDirectory,
        @Nullable ChatModel chatModel) {

        GlobTool globTool = GlobTool.builder()
            .workingDirectory(workingDirectory)
            .build();

        return ToolCallbackProvider.from(ToolCallbacks.from(globTool));
    }
}

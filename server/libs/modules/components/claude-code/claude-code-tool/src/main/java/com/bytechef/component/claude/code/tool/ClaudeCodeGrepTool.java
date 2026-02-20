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

package com.bytechef.component.claude.code.tool;

import static com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeToolFunction.CLAUDE_CODE_TOOLS;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeToolFunction;
import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.agent.tools.GrepTool;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

/**
 * Provides a pure Java grep implementation for code search with regex, glob filtering, and multiple output modes.
 *
 * @author Ivica Cardic
 */
public class ClaudeCodeGrepTool {

    public static final ClusterElementDefinition<ClaudeCodeToolFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ClaudeCodeToolFunction>clusterElement("grepTool")
            .title("Grep Tool")
            .description("Pure Java grep implementation for code search with regex, glob filtering, "
                + "and multiple output modes.")
            .type(CLAUDE_CODE_TOOLS)
            .object(() -> ClaudeCodeGrepTool::apply);

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static List<ToolCallback> apply(
        Parameters inputParameters, Parameters connectionParameters, Path workingDirectory,
        @Nullable ChatModel chatModel) {

        GrepTool grepTool = GrepTool.builder()
            .workingDirectory(workingDirectory)
            .build();

        return List.of(ToolCallbacks.from(grepTool));
    }
}

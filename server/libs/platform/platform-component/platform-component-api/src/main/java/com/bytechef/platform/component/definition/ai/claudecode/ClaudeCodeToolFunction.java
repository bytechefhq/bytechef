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

package com.bytechef.platform.component.definition.ai.claudecode;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.Parameters;
import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

/**
 * Functional interface for Claude Code built-in tools. Each implementation wraps a spring-ai-agent-utils tool and
 * returns a list of ToolCallbacks that will be registered on the ChatClient.
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface ClaudeCodeToolFunction {

    ClusterElementType CLAUDE_CODE_TOOLS = new ClusterElementType(
        "CLAUDE_CODE_TOOLS", "claudeCodeTools", "Claude Code Tools", true, false);

    /**
     * @param inputParameters      The input parameters for tool configuration.
     * @param connectionParameters The connection parameters (e.g., API keys).
     * @param workingDirectory     The temporary working directory for file operations.
     * @param chatModel            The resolved ChatModel for tools that need AI capabilities (e.g., SmartWebFetchTool).
     *                             May be null for tools that don't need it.
     * @return A list of ToolCallbacks to register on the ChatClient.
     * @throws Exception If an error occurs during tool initialization.
     */
    List<ToolCallback> apply(
        Parameters inputParameters, Parameters connectionParameters, Path workingDirectory,
        @Nullable ChatModel chatModel) throws Exception;
}

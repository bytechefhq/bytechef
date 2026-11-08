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

package com.bytechef.component.ai.agent.utils.cluster.workspace;

import com.bytechef.commons.util.JsonUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * @author Ivica Cardic
 */
public class WorkspacePathToolCallback implements ToolCallback {

    public static final String FILES_DESCRIPTION =
        "Only files inside it are accessible. A relative path is resolved against the workspace.";

    private final ToolCallback delegate;
    private final Set<String> pathArgumentNames;
    private final Set<String> patternArgumentNames;
    private final ToolDefinition toolDefinition;
    private final AgentWorkspace workspace;

    public WorkspacePathToolCallback(
        ToolCallback delegate, AgentWorkspace workspace, String workspaceDescription, Set<String> pathArgumentNames,
        Set<String> patternArgumentNames) {

        ToolDefinition delegateToolDefinition = delegate.getToolDefinition();

        this.delegate = delegate;
        this.pathArgumentNames = Set.copyOf(pathArgumentNames);
        this.patternArgumentNames = Set.copyOf(patternArgumentNames);
        this.toolDefinition = ToolDefinition.builder()
            .name(delegateToolDefinition.name())
            .description(
                workspace.displayText(delegateToolDefinition.description()) + "\n\nThe agent workspace is " +
                    workspace.modelRoot() + ". " + workspaceDescription)
            .inputSchema(delegateToolDefinition.inputSchema())
            .build();
        this.workspace = workspace;
    }

    public static ToolCallbackProvider wrap(
        ToolCallback[] toolCallbacks, AgentWorkspace workspace, String workspaceDescription,
        Set<String> pathArgumentNames, Set<String> patternArgumentNames) {

        return ToolCallbackProvider.from(wrapAll(
            toolCallbacks, toolCallback -> new WorkspacePathToolCallback(
                toolCallback, workspace, workspaceDescription, pathArgumentNames, patternArgumentNames)));
    }

    @Override
    public String call(String toolInput) {
        return workspace.displayText(delegate.call(toHostArguments(toolInput)));
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        return workspace.displayText(delegate.call(toHostArguments(toolInput), toolContext));
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    private String toHostArguments(String toolInput) {
        if (pathArgumentNames.isEmpty() && patternArgumentNames.isEmpty()) {
            return toolInput;
        }

        Map<String, Object> arguments = new HashMap<>(JsonUtils.readMap(toolInput));

        boolean changed = replaceArguments(arguments, pathArgumentNames, workspace::toHostPath);

        changed |= replaceArguments(arguments, patternArgumentNames, workspace::toHostPattern);

        return changed ? JsonUtils.write(arguments) : toolInput;
    }

    private static boolean replaceArguments(
        Map<String, Object> arguments, Set<String> argumentNames, UnaryOperator<String> mapper) {

        boolean changed = false;

        for (String argumentName : argumentNames) {
            if (arguments.get(argumentName) instanceof String value && !value.isBlank()) {
                arguments.put(argumentName, mapper.apply(value));

                changed = true;
            }
        }

        return changed;
    }

    private static List<ToolCallback> wrapAll(
        ToolCallback[] toolCallbacks, UnaryOperator<ToolCallback> wrapper) {

        return Arrays.stream(toolCallbacks)
            .map(wrapper)
            .toList();
    }
}

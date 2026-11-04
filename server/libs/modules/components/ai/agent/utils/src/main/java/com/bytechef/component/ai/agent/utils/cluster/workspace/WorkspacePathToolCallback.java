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
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * @author Ivica Cardic
 */
public class WorkspacePathToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final String pathArgumentName;
    private final ToolDefinition toolDefinition;
    private final Path workspaceRoot;

    public WorkspacePathToolCallback(ToolCallback delegate, String pathArgumentName, Path workspaceRoot) {
        ToolDefinition delegateToolDefinition = delegate.getToolDefinition();

        this.delegate = delegate;
        this.pathArgumentName = pathArgumentName;
        this.toolDefinition = ToolDefinition.builder()
            .name(delegateToolDefinition.name())
            .description(
                delegateToolDefinition.description() + "\n\nThe agent workspace is " + workspaceRoot +
                    ". Only files inside it are accessible. A relative " + pathArgumentName +
                    " is resolved against the workspace.")
            .inputSchema(delegateToolDefinition.inputSchema())
            .build();
        this.workspaceRoot = workspaceRoot;
    }

    @Override
    public String call(String toolInput) {
        return delegate.call(resolvePath(toolInput));
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        return delegate.call(resolvePath(toolInput), toolContext);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    private String resolvePath(String toolInput) {
        Map<String, Object> arguments = new HashMap<>(JsonUtils.readMap(toolInput));

        if (!(arguments.get(pathArgumentName) instanceof String path) || path.isBlank()) {
            return toolInput;
        }

        Path filePath = Path.of(path.strip());

        if (filePath.isAbsolute()) {
            return toolInput;
        }

        arguments.put(pathArgumentName, String.valueOf(workspaceRoot.resolve(filePath)));

        return JsonUtils.write(arguments);
    }
}

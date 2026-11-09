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
import com.bytechef.platform.ai.workspace.AgentWorkspaceQuota;
import java.nio.file.Path;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * @author Ivica Cardic
 */
public class FileWriteQuotaToolCallback implements ToolCallback {

    static final String EDIT_TOOL_NAME = "Edit";
    static final String WRITE_TOOL_NAME = "Write";

    private static final String CONTENT = "content";
    private static final String FILE_PATH = "filePath";
    private static final String NEW_STRING = "new_string";
    private static final String OLD_STRING = "old_string";
    private static final String REPLACE_ALL = "replace_all";

    private final ToolCallback delegate;
    private final AgentWorkspaceQuota quota;

    public FileWriteQuotaToolCallback(ToolCallback delegate, AgentWorkspaceQuota quota) {
        this.delegate = delegate;
        this.quota = quota;
    }

    public static ToolCallback wrap(ToolCallback toolCallback, AgentWorkspaceQuota quota) {
        ToolDefinition toolDefinition = toolCallback.getToolDefinition();

        String toolName = toolDefinition.name();

        if (!quota.isLimited() || !(WRITE_TOOL_NAME.equals(toolName) || EDIT_TOOL_NAME.equals(toolName))) {
            return toolCallback;
        }

        return new FileWriteQuotaToolCallback(toolCallback, quota);
    }

    @Override
    public String call(String toolInput) {
        String fullMessage = checkQuota(toolInput);

        return fullMessage == null ? delegate.call(toolInput) : fullMessage;
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        String fullMessage = checkQuota(toolInput);

        return fullMessage == null ? delegate.call(toolInput, toolContext) : fullMessage;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    private @Nullable String checkQuota(String toolInput) {
        Map<String, ?> arguments = JsonUtils.readMap(toolInput);

        ToolDefinition toolDefinition = delegate.getToolDefinition();

        Path file = getFile(arguments);

        if (WRITE_TOOL_NAME.equals(toolDefinition.name())) {
            return quota.checkWrite(quota.getWriteRequiredBytes(file, getString(arguments, CONTENT)));
        }

        return quota.checkWrite(
            quota.getEditRequiredBytes(
                file, getString(arguments, OLD_STRING), getString(arguments, NEW_STRING),
                Boolean.TRUE.equals(arguments.get(REPLACE_ALL))));
    }

    private @Nullable Path getFile(Map<String, ?> arguments) {
        if (!(arguments.get(FILE_PATH) instanceof String filePath) || filePath.isBlank()) {
            return null;
        }

        Path root = quota.root();

        try {
            return root.resolve(filePath.strip());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static @Nullable String getString(Map<String, ?> arguments, String key) {
        return arguments.get(key) instanceof String text ? text : null;
    }
}

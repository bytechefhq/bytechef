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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
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

        long requiredBytes = getRequiredBytes(arguments);

        if (requiredBytes <= 0) {
            return null;
        }

        long usedBytes = quota.getUsedBytes();

        if (usedBytes + requiredBytes <= quota.maxBytes()) {
            return null;
        }

        return quota.getFullMessage(usedBytes, requiredBytes);
    }

    private long getRequiredBytes(Map<String, ?> arguments) {
        ToolDefinition toolDefinition = delegate.getToolDefinition();

        Path file = getFile(arguments);

        if (WRITE_TOOL_NAME.equals(toolDefinition.name())) {
            return getByteCount(arguments.get(CONTENT)) - getFileSize(file);
        }

        long growth = getByteCount(arguments.get(NEW_STRING)) - getByteCount(arguments.get(OLD_STRING));

        if (growth <= 0) {
            return 0;
        }

        if (Boolean.TRUE.equals(arguments.get(REPLACE_ALL))) {
            return growth * countOccurrences(file, arguments.get(OLD_STRING));
        }

        return growth;
    }

    private static long countOccurrences(@Nullable Path file, @Nullable Object oldString) {
        if (file == null || !(oldString instanceof String text) || text.isEmpty()) {
            return 1;
        }

        try {
            String fileContent = Files.readString(file, StandardCharsets.UTF_8);

            long count = 0;
            int index = fileContent.indexOf(text);

            while (index >= 0) {
                count++;

                index = fileContent.indexOf(text, index + text.length());
            }

            return Math.max(count, 1);
        } catch (IOException | RuntimeException exception) {
            return 1;
        }
    }

    private static long getByteCount(@Nullable Object value) {
        if (!(value instanceof String text)) {
            return 0;
        }

        return text.getBytes(StandardCharsets.UTF_8).length;
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

    private static long getFileSize(@Nullable Path file) {
        if (file == null || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            return 0;
        }

        try {
            return Files.size(file);
        } catch (IOException ioException) {
            return 0;
        }
    }
}

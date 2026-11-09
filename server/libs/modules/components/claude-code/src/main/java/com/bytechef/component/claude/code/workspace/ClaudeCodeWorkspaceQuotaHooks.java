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

package com.bytechef.component.claude.code.workspace;

import com.bytechef.platform.ai.workspace.AgentWorkspaceQuota;
import io.github.markpollack.claude.agent.sdk.hooks.HookRegistry;
import io.github.markpollack.claude.agent.sdk.types.control.HookInput;
import io.github.markpollack.claude.agent.sdk.types.control.HookInput.PreToolUseInput;
import io.github.markpollack.claude.agent.sdk.types.control.HookOutput;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public final class ClaudeCodeWorkspaceQuotaHooks {

    static final String BASH_TOOL_NAME = "Bash";
    static final String BLOCK = "block";
    static final String EDIT_TOOL_NAME = "Edit";
    static final String FILE_WRITE_TOOL_NAMES = "Write|Edit|MultiEdit|NotebookEdit";
    static final String MULTI_EDIT_TOOL_NAME = "MultiEdit";
    static final String NOTEBOOK_EDIT_TOOL_NAME = "NotebookEdit";
    static final String WRITE_TOOL_NAME = "Write";

    private static final String COMMAND = "command";
    private static final String CONTENT = "content";
    private static final String DELETE_EDIT_MODE = "delete";
    private static final String EDIT_MODE = "edit_mode";
    private static final String EDITS = "edits";
    private static final String FILE_PATH = "file_path";
    private static final String NEW_SOURCE = "new_source";
    private static final String NEW_STRING = "new_string";
    private static final String NOTEBOOK_PATH = "notebook_path";
    private static final String OLD_STRING = "old_string";
    private static final String REPLACE_ALL = "replace_all";

    private ClaudeCodeWorkspaceQuotaHooks() {
    }

    public static HookRegistry create(AgentWorkspaceQuota quota) {
        HookRegistry hookRegistry = new HookRegistry();

        if (!quota.isLimited()) {
            return hookRegistry;
        }

        hookRegistry.registerPreToolUse(FILE_WRITE_TOOL_NAMES, hookInput -> checkFileWrite(quota, hookInput));
        hookRegistry.registerPreToolUse(BASH_TOOL_NAME, hookInput -> checkCommand(quota, hookInput));
        hookRegistry.registerPostToolUse(BASH_TOOL_NAME, hookInput -> reportOverLimit(quota));

        return hookRegistry;
    }

    static HookOutput checkCommand(AgentWorkspaceQuota quota, HookInput hookInput) {
        if (!(hookInput instanceof PreToolUseInput preToolUseInput)) {
            return HookOutput.allow();
        }

        long usedBytes = quota.getUsedBytes();

        if (usedBytes <= quota.maxBytes() ||
            AgentWorkspaceQuota.isCleanupCommand(getString(preToolUseInput.toolInput(), COMMAND))) {

            return HookOutput.allow();
        }

        return block(quota.getRefusedMessage(usedBytes));
    }

    static HookOutput checkFileWrite(AgentWorkspaceQuota quota, HookInput hookInput) {
        if (!(hookInput instanceof PreToolUseInput preToolUseInput)) {
            return HookOutput.allow();
        }

        Map<String, Object> toolInput = preToolUseInput.toolInput();
        String toolName = preToolUseInput.toolName();

        String filePathKey = NOTEBOOK_EDIT_TOOL_NAME.equals(toolName) ? NOTEBOOK_PATH : FILE_PATH;

        Path file = getFile(quota, getString(toolInput, filePathKey));

        if (file != null && isOutsideWorkspace(quota.root(), file)) {
            return HookOutput.allow();
        }

        long requiredBytes = switch (toolName) {
            case WRITE_TOOL_NAME -> quota.getWriteRequiredBytes(file, getString(toolInput, CONTENT));
            case EDIT_TOOL_NAME -> getEditRequiredBytes(quota, file, toolInput);
            case MULTI_EDIT_TOOL_NAME -> getMultiEditRequiredBytes(quota, file, toolInput);
            case NOTEBOOK_EDIT_TOOL_NAME -> getNotebookEditRequiredBytes(quota, toolInput);
            default -> 0;
        };

        String fullMessage = quota.checkWrite(requiredBytes);

        return fullMessage == null ? HookOutput.allow() : block(fullMessage);
    }

    static HookOutput reportOverLimit(AgentWorkspaceQuota quota) {
        long usedBytes = quota.getUsedBytes();

        if (usedBytes <= quota.maxBytes()) {
            return HookOutput.allow();
        }

        return block(quota.getOverLimitMessage(usedBytes));
    }

    private static HookOutput block(String reason) {
        return HookOutput.builder()
            .continueExecution(true)
            .decision(BLOCK)
            .reason(reason)
            .build();
    }

    private static long getEditRequiredBytes(AgentWorkspaceQuota quota, @Nullable Path file, Map<?, ?> edit) {
        return quota.getEditRequiredBytes(
            file, getString(edit, OLD_STRING), getString(edit, NEW_STRING), Boolean.TRUE.equals(edit.get(REPLACE_ALL)));
    }

    private static @Nullable Path getFile(AgentWorkspaceQuota quota, @Nullable String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }

        Path root = quota.root();

        try {
            Path file = root.resolve(filePath.strip());

            return file.normalize();
        } catch (InvalidPathException invalidPathException) {
            return null;
        }
    }

    private static long getMultiEditRequiredBytes(
        AgentWorkspaceQuota quota, @Nullable Path file, Map<String, Object> toolInput) {

        if (!(toolInput.get(EDITS) instanceof List<?> edits)) {
            return 0;
        }

        long requiredBytes = 0;

        for (Object edit : edits) {
            if (edit instanceof Map<?, ?> editMap) {
                requiredBytes += getEditRequiredBytes(quota, file, editMap);
            }
        }

        return requiredBytes;
    }

    private static long getNotebookEditRequiredBytes(AgentWorkspaceQuota quota, Map<String, Object> toolInput) {
        if (DELETE_EDIT_MODE.equals(getString(toolInput, EDIT_MODE))) {
            return 0;
        }

        return quota.getWriteRequiredBytes(null, getString(toolInput, NEW_SOURCE));
    }

    private static Path toRealPath(Path path) {
        Path existingPath = path;

        while (existingPath != null && !Files.exists(existingPath)) {
            existingPath = existingPath.getParent();
        }

        if (existingPath == null) {
            return path;
        }

        try {
            Path realExistingPath = existingPath.toRealPath();

            return realExistingPath.resolve(existingPath.relativize(path));
        } catch (IOException ioException) {
            return path;
        }
    }

    private static boolean isOutsideWorkspace(Path root, Path file) {
        if (file.startsWith(root)) {
            return false;
        }

        Path realFile = toRealPath(file);

        return !realFile.startsWith(toRealPath(root));
    }

    private static @Nullable String getString(Map<?, ?> map, String key) {
        return map.get(key) instanceof String text ? text : null;
    }
}

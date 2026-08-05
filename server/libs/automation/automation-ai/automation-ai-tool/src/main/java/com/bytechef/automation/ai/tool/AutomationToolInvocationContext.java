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

package com.bytechef.automation.ai.tool;

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;

/**
 * CE accessor for the workspace / environment / user / task scope that automation tool callbacks relocated out of
 * ai-hub read from Spring AI's {@link ToolContext}. The literal key strings intentionally match the keys written by the
 * EE {@code AiHubToolInvocationContext} (and by {@link WorkspaceScopedManagerToolCallback} on the management MCP
 * surface), so the AI-Hub / Copilot runtimes need no change to populate them — the same duplicate-key-in-lockstep
 * contract already documented on {@link WorkflowExecutionToolContextKeys}. If the EE key strings ever change, these
 * must change in lockstep.
 *
 * @author Ivica Cardic
 */
public record AutomationToolInvocationContext(
    @Nullable Long workspaceId, @Nullable Long userId, @Nullable Short sourceOrdinal, @Nullable String lastUserPrompt,
    @Nullable Long environmentId, @Nullable String threadId) {

    public static final String TOOL_CONTEXT_WORKSPACE_ID_KEY = "bytechef.assetFile.workspaceId";
    public static final String TOOL_CONTEXT_USER_ID_KEY = "bytechef.assetFile.userId";
    public static final String TOOL_CONTEXT_SOURCE_ORDINAL_KEY = "bytechef.assetFile.sourceOrdinal";
    public static final String TOOL_CONTEXT_LAST_USER_PROMPT_KEY = "bytechef.assetFile.lastUserPrompt";
    public static final String TOOL_CONTEXT_ENVIRONMENT_ID_KEY = "bytechef.assetFile.environmentId";
    public static final String TOOL_CONTEXT_THREAD_ID_KEY = "bytechef.assetFile.threadId";

    /**
     * Rehydrates the context from a {@link ToolContext}. Returns {@code null} when the tool context carries none of the
     * scope keys.
     */
    public static @Nullable AutomationToolInvocationContext fromToolContext(@Nullable ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }

        Map<String, Object> map = toolContext.getContext();

        if (map == null || map.isEmpty()) {
            return null;
        }

        Long workspaceId = asLong(map.get(TOOL_CONTEXT_WORKSPACE_ID_KEY));
        Long userId = asLong(map.get(TOOL_CONTEXT_USER_ID_KEY));
        Short sourceOrdinal = asShort(map.get(TOOL_CONTEXT_SOURCE_ORDINAL_KEY));
        String lastUserPrompt = asString(map.get(TOOL_CONTEXT_LAST_USER_PROMPT_KEY));
        Long environmentId = asLong(map.get(TOOL_CONTEXT_ENVIRONMENT_ID_KEY));
        String threadId = asString(map.get(TOOL_CONTEXT_THREAD_ID_KEY));

        if (workspaceId == null && userId == null && sourceOrdinal == null && lastUserPrompt == null
            && environmentId == null && threadId == null) {
            return null;
        }

        return new AutomationToolInvocationContext(
            workspaceId, userId, sourceOrdinal, lastUserPrompt, environmentId, threadId);
    }

    /**
     * Resolves the environment ordinal as an {@code int}, defaulting to {@code 0} (DEVELOPMENT) when the context is
     * absent or carries no {@code environmentId} — mirroring the legacy silent dev-bucketing behaviour.
     */
    public static int resolveEnvironmentOrDefault(@Nullable AutomationToolInvocationContext context) {
        Long environmentId = context == null ? null : context.environmentId();

        if (environmentId == null) {
            return 0;
        }

        return environmentId.intValue();
    }

    private static @Nullable Long asLong(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }

        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        return null;
    }

    private static @Nullable Short asShort(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number numberValue) {
            return numberValue.shortValue();
        }

        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Short.parseShort(stringValue);
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        return null;
    }

    private static @Nullable String asString(@Nullable Object value) {
        return value == null ? null : value.toString();
    }
}

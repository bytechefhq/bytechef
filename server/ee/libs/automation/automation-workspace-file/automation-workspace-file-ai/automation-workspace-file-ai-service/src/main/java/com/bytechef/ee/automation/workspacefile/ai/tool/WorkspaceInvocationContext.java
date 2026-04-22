/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workspacefile.ai.tool;

import java.util.HashMap;
import java.util.Map;
import org.springframework.ai.chat.model.ToolContext;

/**
 * Immutable snapshot of the workspace, agent-source, and originating user prompt for a single copilot invocation. The
 * values flow into the three workspace-file tool callbacks through two complementary channels: the
 * {@link AgUiToolContextWorkspaceContextProvider} thread-local for the common synchronous path, and a Spring AI
 * {@link ToolContext} map for reactive/tool-calling-manager paths that run on different threads.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record WorkspaceInvocationContext(Long workspaceId, Short sourceOrdinal, String lastUserPrompt) {

    /**
     * Rehydrates a {@link WorkspaceInvocationContext} from Spring AI's {@link ToolContext}. Returns {@code null} when
     * the tool context does not carry the workspace keys.
     */
    public static WorkspaceInvocationContext fromToolContext(ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }

        Map<String, Object> map = toolContext.getContext();

        if (map == null || map.isEmpty()) {
            return null;
        }

        Long workspaceId = asLong(map.get(AgUiToolContextWorkspaceContextProvider.TOOL_CONTEXT_WORKSPACE_ID_KEY));
        Short sourceOrdinal = asShort(
            map.get(AgUiToolContextWorkspaceContextProvider.TOOL_CONTEXT_SOURCE_ORDINAL_KEY));
        String lastUserPrompt = asString(
            map.get(AgUiToolContextWorkspaceContextProvider.TOOL_CONTEXT_LAST_USER_PROMPT_KEY));

        if (workspaceId == null && sourceOrdinal == null && lastUserPrompt == null) {
            return null;
        }

        return new WorkspaceInvocationContext(workspaceId, sourceOrdinal, lastUserPrompt);
    }

    /**
     * Serialises this context into a map suitable for Spring AI's
     * {@code ChatClient.ChatClientRequestSpec.toolContext(Map)}.
     */
    public Map<String, Object> toToolContext() {
        Map<String, Object> map = new HashMap<>();

        if (workspaceId != null) {
            map.put(AgUiToolContextWorkspaceContextProvider.TOOL_CONTEXT_WORKSPACE_ID_KEY, workspaceId);
        }

        if (sourceOrdinal != null) {
            map.put(AgUiToolContextWorkspaceContextProvider.TOOL_CONTEXT_SOURCE_ORDINAL_KEY, sourceOrdinal);
        }

        if (lastUserPrompt != null) {
            map.put(AgUiToolContextWorkspaceContextProvider.TOOL_CONTEXT_LAST_USER_PROMPT_KEY, lastUserPrompt);
        }

        return map;
    }

    private static Long asLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Long longValue) {
            return longValue;
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

    private static Short asShort(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Short shortValue) {
            return shortValue;
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

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }
}

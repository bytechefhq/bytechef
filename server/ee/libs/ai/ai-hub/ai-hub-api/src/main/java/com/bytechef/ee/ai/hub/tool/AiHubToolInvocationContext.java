/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.automation.ai.tool.AutomationToolInvocationContext;
import java.util.HashMap;
import java.util.Map;
import org.springframework.ai.chat.model.ToolContext;

/**
 * Immutable snapshot of the workspace, agent-source, and originating user prompt for a single agent invocation. The
 * agent serialises this onto Spring AI's {@link ToolContext} via {@link #toToolContext()} when constructing the chat
 * request; tool callbacks rehydrate it via {@link #fromToolContext(ToolContext)}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AiHubToolInvocationContext(
    Long workspaceId, Long userId, Short sourceOrdinal, String lastUserPrompt, Long environmentId,
    String threadId, Long ownerUserId) {

    public static final String TOOL_CONTEXT_WORKSPACE_ID_KEY =
        AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY;
    public static final String TOOL_CONTEXT_USER_ID_KEY =
        AutomationToolInvocationContext.TOOL_CONTEXT_USER_ID_KEY;
    public static final String TOOL_CONTEXT_SOURCE_ORDINAL_KEY =
        AutomationToolInvocationContext.TOOL_CONTEXT_SOURCE_ORDINAL_KEY;
    public static final String TOOL_CONTEXT_LAST_USER_PROMPT_KEY =
        AutomationToolInvocationContext.TOOL_CONTEXT_LAST_USER_PROMPT_KEY;
    public static final String TOOL_CONTEXT_ENVIRONMENT_ID_KEY =
        AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY;
    public static final String TOOL_CONTEXT_THREAD_ID_KEY =
        AutomationToolInvocationContext.TOOL_CONTEXT_THREAD_ID_KEY;
    public static final String TOOL_CONTEXT_OWNER_USER_ID_KEY = "bytechef.automationTool.ownerUserId";

    /**
     * Convenience overload that leaves {@code threadId} and {@code ownerUserId} as {@code null}. Kept for tests that
     * don't carry a thread id. Production code should pass both explicitly via the canonical 7-arg constructor.
     */
    public AiHubToolInvocationContext(
        Long workspaceId, Long userId, Short sourceOrdinal, String lastUserPrompt, Long environmentId) {

        this(workspaceId, userId, sourceOrdinal, lastUserPrompt, environmentId, null, null);
    }

    /**
     * Convenience overload that leaves {@code ownerUserId} as {@code null}. Kept for the many existing call sites that
     * predate {@code ownerUserId} and have no chat-owner value to supply. Production code that resolves a chat should
     * prefer the canonical 7-arg constructor so tool callbacks can tell the chat owner apart from the current sender.
     */
    public AiHubToolInvocationContext(
        Long workspaceId, Long userId, Short sourceOrdinal, String lastUserPrompt, Long environmentId,
        String threadId) {

        this(workspaceId, userId, sourceOrdinal, lastUserPrompt, environmentId, threadId, null);
    }

    /**
     * Rehydrates a {@link AiHubToolInvocationContext} from Spring AI's {@link ToolContext}. Returns {@code null} when
     * the tool context does not carry the workspace keys.
     */
    public static AiHubToolInvocationContext fromToolContext(ToolContext toolContext) {
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
        Long ownerUserId = asLong(map.get(TOOL_CONTEXT_OWNER_USER_ID_KEY));

        if (workspaceId == null && userId == null && sourceOrdinal == null && lastUserPrompt == null
            && environmentId == null && threadId == null && ownerUserId == null) {
            return null;
        }

        return new AiHubToolInvocationContext(workspaceId, userId, sourceOrdinal, lastUserPrompt, environmentId,
            threadId, ownerUserId);
    }

    /**
     * Resolves the environment ordinal as an {@code int}, defaulting to {@code 0} (DEVELOPMENT) when the context is
     * absent or carries no {@code environmentId}. Mirrors how legacy callers behaved before environment was introduced
     * — silent dev-bucketing rather than a hard error so existing flows do not break before the chat surface wires the
     * environment selector through. Once every chat surface populates {@code environmentId}, this default can become a
     * tool-error so a missing context surfaces immediately.
     */
    public static int resolveEnvironmentOrDefault(AiHubToolInvocationContext context) {
        if (context == null || context.environmentId() == null) {
            return 0;
        }

        return context.environmentId()
            .intValue();
    }

    /**
     * Serialises this context into a map suitable for Spring AI's
     * {@code ChatClient.ChatClientRequestSpec.toolContext(Map)}.
     */
    public Map<String, Object> toToolContext() {
        Map<String, Object> map = new HashMap<>();

        if (workspaceId != null) {
            map.put(TOOL_CONTEXT_WORKSPACE_ID_KEY, workspaceId);
        }

        if (userId != null) {
            map.put(TOOL_CONTEXT_USER_ID_KEY, userId);
        }

        if (sourceOrdinal != null) {
            map.put(TOOL_CONTEXT_SOURCE_ORDINAL_KEY, sourceOrdinal);
        }

        if (lastUserPrompt != null) {
            map.put(TOOL_CONTEXT_LAST_USER_PROMPT_KEY, lastUserPrompt);
        }

        if (environmentId != null) {
            map.put(TOOL_CONTEXT_ENVIRONMENT_ID_KEY, environmentId);
        }

        if (threadId != null) {
            map.put(TOOL_CONTEXT_THREAD_ID_KEY, threadId);
        }

        if (ownerUserId != null) {
            map.put(TOOL_CONTEXT_OWNER_USER_ID_KEY, ownerUserId);
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

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

    public AiHubToolInvocationContext(
        Long workspaceId, Long userId, Short sourceOrdinal, String lastUserPrompt, Long environmentId) {

        this(workspaceId, userId, sourceOrdinal, lastUserPrompt, environmentId, null, null);
    }

    public AiHubToolInvocationContext(
        Long workspaceId, Long userId, Short sourceOrdinal, String lastUserPrompt, Long environmentId,
        String threadId) {

        this(workspaceId, userId, sourceOrdinal, lastUserPrompt, environmentId, threadId, null);
    }

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
     * (silent dev-bucketing rather than a hard error so existing flows do not break before the chat surface wires the
     * environment selector through). Once every chat surface populates {@code environmentId}, this default can become a
     * tool-error so a missing context surfaces immediately.
     */
    public static int resolveEnvironmentOrDefault(AiHubToolInvocationContext context) {
        if (context == null || context.environmentId() == null) {
            return 0;
        }

        return context.environmentId()
            .intValue();
    }

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

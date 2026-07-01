/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.util;

import com.agui.core.state.State;
import com.bytechef.ee.ai.copilot.tool.AgentToolInvocationContext;
import com.bytechef.platform.ai.tool.TaskTools;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;

/**
 * Maps per-run agent {@link State} entries into the Spring AI {@code ToolContext} map handed to copilot tools.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class CopilotToolContextUtils {

    private CopilotToolContextUtils() {
    }

    public static Map<String, Object> toToolContext(@Nullable State state) {
        if (state == null) {
            return Map.of();
        }

        Map<String, Object> toolContext = new HashMap<>();

        Object allowedComponentNames = state.get(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY);

        if (allowedComponentNames != null) {
            toolContext.put(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, allowedComponentNames);
        }

        Long workspaceId = asLong(state.get("workspaceId"));
        Long userId = asLong(state.get(CopilotStateKeys.STATE_AUTHENTICATED_USER_ID));
        Long environmentId = asLong(state.get("environmentId"));
        String tenantId = asString(state.get(CopilotStateKeys.STATE_TENANT_ID));
        Authentication authentication = state.get(CopilotStateKeys.STATE_AUTHENTICATION) instanceof Authentication value
            ? value : null;

        // An embedded run carries a connected-user Authentication (no backing platform user) and is authorized by the
        // embedded request layer, so its @PreAuthorize-gated tools must skip the platform automation RBAC check. This
        // mirrors WorkflowEditorSpringAIAgent, which bypasses the workflow-scope gate on the same STATE_AUTHENTICATION
        // signal — but the request thread's skip-checks ThreadLocal does not reach the tool-execution worker threads,
        // so the flag is carried through the tool context and re-armed by RehydrateContextToolCallback.
        boolean skipAutomationAuthorization = authentication != null;

        toolContext.putAll(
            AgentToolInvocationContext.builder()
                .workspaceId(workspaceId)
                .userId(userId)
                .environmentId(environmentId)
                .tenantId(tenantId)
                .authentication(authentication)
                .skipAutomationAuthorization(skipAutomationAuthorization)
                .build()
                .toToolContext());

        return toolContext;
    }

    private static @Nullable Long asLong(@Nullable Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String string && !string.isBlank()) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        return null;
    }

    private static @Nullable String asString(@Nullable Object value) {
        return value instanceof String string && !string.isBlank() ? string : null;
    }
}

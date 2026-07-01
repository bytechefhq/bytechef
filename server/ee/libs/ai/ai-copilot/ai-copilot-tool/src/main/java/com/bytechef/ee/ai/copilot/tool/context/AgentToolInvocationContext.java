/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool.context;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.security.core.Authentication;

/**
 * Surface-neutral invocation context for shared component-interaction tool callbacks (connection / property options).
 * Carries the workspace, user, and environment a tool needs, plus an optional conversation id (used by AI Hub's
 * artifact recorder; null for the Copilot surface) and an optional captured {@link Authentication} the tool wrapper
 * restores on the worker thread. Both AI Hub and the Copilot panel populate it into their agent {@code ToolContext};
 * the shared tools read only this type, so they don't depend on any one surface.
 *
 * <p>
 * New callers should prefer {@link #builder()} over the positional constructors: with three adjacent {@code Long} ids
 * (workspace, user, environment) the named setters remove the risk of silently transposing them.
 * </p>
 *
 * <p>
 * {@code authentication} is a live in-process object rather than a serialisable id: surfaces whose principal has no
 * backing platform user (the embedded API-key principal) cannot be reconstructed from a user id, so the request
 * thread's {@link Authentication} is captured and carried directly. It is therefore never read from a client-supplied
 * field and never serialised across the wire.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
// The Authentication is intentionally shared by reference (the same principal must be restored on the worker thread),
// so exposing it through the canonical constructor and accessor is by design rather than a leak.
@SuppressFBWarnings({
    "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
})
public record AgentToolInvocationContext(
    @Nullable Long workspaceId, @Nullable Long userId, @Nullable Long environmentId,
    @Nullable String conversationId, @Nullable String tenantId, @Nullable Authentication authentication,
    boolean skipAutomationAuthorization) {

    public static final String TOOL_CONTEXT_WORKSPACE_ID_KEY = "bytechef.agentTool.workspaceId";
    public static final String TOOL_CONTEXT_USER_ID_KEY = "bytechef.agentTool.userId";
    public static final String TOOL_CONTEXT_ENVIRONMENT_ID_KEY = "bytechef.agentTool.environmentId";
    public static final String TOOL_CONTEXT_CONVERSATION_ID_KEY = "bytechef.agentTool.conversationId";
    public static final String TOOL_CONTEXT_TENANT_ID_KEY = "bytechef.agentTool.tenantId";
    public static final String TOOL_CONTEXT_AUTHENTICATION_KEY = "bytechef.agentTool.authentication";
    public static final String TOOL_CONTEXT_SKIP_AUTHORIZATION_KEY = "bytechef.agentTool.skipAutomationAuthorization";

    /**
     * Convenience constructor for callers that carry a captured authentication but do not request the embedded
     * automation-authorization bypass.
     */
    public AgentToolInvocationContext(
        @Nullable Long workspaceId, @Nullable Long userId, @Nullable Long environmentId,
        @Nullable String conversationId, @Nullable String tenantId, @Nullable Authentication authentication) {

        this(workspaceId, userId, environmentId, conversationId, tenantId, authentication, false);
    }

    /**
     * Convenience constructor for the common case of no captured authentication (e.g. AI Hub and the userId-based
     * in-editor Copilot path, where security context is rehydrated from the user id instead).
     */
    public AgentToolInvocationContext(
        @Nullable Long workspaceId, @Nullable Long userId, @Nullable Long environmentId,
        @Nullable String conversationId, @Nullable String tenantId) {

        this(workspaceId, userId, environmentId, conversationId, tenantId, null, false);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static @Nullable AgentToolInvocationContext fromToolContext(@Nullable ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }

        Map<String, Object> map = toolContext.getContext();

        if (map == null || map.isEmpty()) {
            return null;
        }

        Long workspaceId = asLong(map.get(TOOL_CONTEXT_WORKSPACE_ID_KEY));
        Long userId = asLong(map.get(TOOL_CONTEXT_USER_ID_KEY));
        Long environmentId = asLong(map.get(TOOL_CONTEXT_ENVIRONMENT_ID_KEY));
        String conversationId = asString(map.get(TOOL_CONTEXT_CONVERSATION_ID_KEY));
        String tenantId = asString(map.get(TOOL_CONTEXT_TENANT_ID_KEY));
        Authentication authentication = map.get(TOOL_CONTEXT_AUTHENTICATION_KEY) instanceof Authentication value
            ? value : null;
        boolean skipAutomationAuthorization = asBoolean(map.get(TOOL_CONTEXT_SKIP_AUTHORIZATION_KEY));

        if (workspaceId == null && userId == null && environmentId == null && conversationId == null
            && tenantId == null && authentication == null && !skipAutomationAuthorization) {
            return null;
        }

        return new AgentToolInvocationContext(
            workspaceId, userId, environmentId, conversationId, tenantId, authentication, skipAutomationAuthorization);
    }

    public int resolveEnvironmentOrDefault() {
        return environmentId == null ? 0 : environmentId.intValue();
    }

    public Map<String, Object> toToolContext() {
        Map<String, Object> map = new HashMap<>();

        if (workspaceId != null) {
            map.put(TOOL_CONTEXT_WORKSPACE_ID_KEY, workspaceId);
        }

        if (userId != null) {
            map.put(TOOL_CONTEXT_USER_ID_KEY, userId);
        }

        if (environmentId != null) {
            map.put(TOOL_CONTEXT_ENVIRONMENT_ID_KEY, environmentId);
        }

        if (conversationId != null) {
            map.put(TOOL_CONTEXT_CONVERSATION_ID_KEY, conversationId);
        }

        if (tenantId != null) {
            map.put(TOOL_CONTEXT_TENANT_ID_KEY, tenantId);
        }

        if (authentication != null) {
            map.put(TOOL_CONTEXT_AUTHENTICATION_KEY, authentication);
        }

        if (skipAutomationAuthorization) {
            map.put(TOOL_CONTEXT_SKIP_AUTHORIZATION_KEY, Boolean.TRUE);
        }

        return map;
    }

    private static boolean asBoolean(@Nullable Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }

        return value instanceof String string && Boolean.parseBoolean(string);
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
        return value instanceof String string ? string : null;
    }

    /**
     * Transposition-safe builder for {@link AgentToolInvocationContext}. Preferred over the positional constructors for
     * new callers because the workspace / user / environment ids share the {@code Long} type and are easy to swap by
     * accident in a positional call.
     */
    public static final class Builder {

        private @Nullable Long workspaceId;
        private @Nullable Long userId;
        private @Nullable Long environmentId;
        private @Nullable String conversationId;
        private @Nullable String tenantId;
        private @Nullable Authentication authentication;
        private boolean skipAutomationAuthorization;

        private Builder() {
        }

        public Builder workspaceId(@Nullable Long workspaceId) {
            this.workspaceId = workspaceId;

            return this;
        }

        public Builder userId(@Nullable Long userId) {
            this.userId = userId;

            return this;
        }

        public Builder environmentId(@Nullable Long environmentId) {
            this.environmentId = environmentId;

            return this;
        }

        public Builder conversationId(@Nullable String conversationId) {
            this.conversationId = conversationId;

            return this;
        }

        public Builder tenantId(@Nullable String tenantId) {
            this.tenantId = tenantId;

            return this;
        }

        public Builder authentication(@Nullable Authentication authentication) {
            this.authentication = authentication;

            return this;
        }

        public Builder skipAutomationAuthorization(boolean skipAutomationAuthorization) {
            this.skipAutomationAuthorization = skipAutomationAuthorization;

            return this;
        }

        public AgentToolInvocationContext build() {
            return new AgentToolInvocationContext(
                workspaceId, userId, environmentId, conversationId, tenantId, authentication,
                skipAutomationAuthorization);
        }
    }
}

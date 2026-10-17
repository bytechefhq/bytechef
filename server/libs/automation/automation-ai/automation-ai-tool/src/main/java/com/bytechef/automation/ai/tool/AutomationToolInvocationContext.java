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

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
public record AutomationToolInvocationContext(@Nullable Long workspaceId, @Nullable Long environmentId) {

    private static final Logger log = LoggerFactory.getLogger(AutomationToolInvocationContext.class);

    public static final String TOOL_CONTEXT_WORKSPACE_ID_KEY = "bytechef.automationTool.workspaceId";
    public static final String TOOL_CONTEXT_ENVIRONMENT_ID_KEY = "bytechef.automationTool.environmentId";
    public static final String TOOL_CONTEXT_USER_ID_KEY = "bytechef.automationTool.userId";
    public static final String TOOL_CONTEXT_SOURCE_ORDINAL_KEY = "bytechef.automationTool.sourceOrdinal";
    public static final String TOOL_CONTEXT_LAST_USER_PROMPT_KEY = "bytechef.automationTool.lastUserPrompt";
    public static final String TOOL_CONTEXT_THREAD_ID_KEY = "bytechef.automationTool.threadId";

    public static final String WORKSPACE_ID_DESCRIPTION =
        "The workspace ID. Optional inside a Copilot chat, which defaults to the chat's workspace; otherwise " +
            "required - call listWorkspaces to find it.";

    public static @Nullable AutomationToolInvocationContext fromToolContext(@Nullable ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }

        Map<String, Object> map = toolContext.getContext();

        if (map == null || map.isEmpty()) {
            return null;
        }

        Long workspaceId = asLong(map.get(TOOL_CONTEXT_WORKSPACE_ID_KEY));
        Long environmentId = asLong(map.get(TOOL_CONTEXT_ENVIRONMENT_ID_KEY));

        if (workspaceId == null && environmentId == null) {
            return null;
        }

        return new AutomationToolInvocationContext(workspaceId, environmentId);
    }

    public static int resolveEnvironmentOrDefault(@Nullable AutomationToolInvocationContext context) {
        Long environmentId = context == null ? null : context.environmentId();

        if (environmentId == null) {
            return 0;
        }

        return environmentId.intValue();
    }

    public static @Nullable Long resolveWorkspaceId(
        @Nullable Long requestedWorkspaceId, @Nullable ToolContext toolContext, WorkspaceFacade workspaceFacade) {

        AutomationToolInvocationContext context = fromToolContext(toolContext);

        Long contextWorkspaceId = context == null ? null : context.workspaceId();

        if (requestedWorkspaceId == null) {
            return contextWorkspaceId;
        }

        if (contextWorkspaceId != null) {
            if (!requestedWorkspaceId.equals(contextWorkspaceId)) {
                throw new WorkspaceNotAccessibleException(
                    "Workspace " + requestedWorkspaceId + " is not the workspace " + contextWorkspaceId +
                        " this conversation is scoped to.");
            }

            return contextWorkspaceId;
        }

        List<Workspace> workspaces = workspaceFacade.getMyWorkspaces();

        boolean accessible = workspaces.stream()
            .anyMatch(workspace -> requestedWorkspaceId.equals(workspace.getId()));

        if (!accessible) {
            throw new WorkspaceNotAccessibleException(
                "Workspace " + requestedWorkspaceId + " is not accessible to the caller.");
        }

        return requestedWorkspaceId;
    }

    public static long resolveEnvironmentId(@Nullable String requestedEnvironment, @Nullable ToolContext toolContext) {
        if (requestedEnvironment != null && !requestedEnvironment.isBlank()) {
            return toEnvironment(requestedEnvironment).ordinal();
        }

        Object contextEnvironmentValue = getContextValue(toolContext, TOOL_CONTEXT_ENVIRONMENT_ID_KEY);

        if (contextEnvironmentValue == null) {
            return Environment.DEVELOPMENT.ordinal();
        }

        Long contextEnvironmentId = asLong(contextEnvironmentValue);

        if (contextEnvironmentId == null || contextEnvironmentId < 0 ||
            contextEnvironmentId >= Environment.values().length) {

            throw new UnknownEnvironmentException(
                "The environment of this conversation could not be determined (invalid environment id '" +
                    contextEnvironmentValue + "'). Supply environment (DEVELOPMENT, STAGING, or PRODUCTION) " +
                    "explicitly.");
        }

        return contextEnvironmentId;
    }

    public static Map<String, Object> toForwardedContext(
        @Nullable ToolContext toolContext, long workspaceId, long environmentId) {

        Map<String, Object> forwardedContext = new HashMap<>();

        if (toolContext != null) {
            forwardedContext.putAll(toolContext.getContext());
        }

        forwardedContext.put(TOOL_CONTEXT_WORKSPACE_ID_KEY, workspaceId);
        forwardedContext.put(TOOL_CONTEXT_ENVIRONMENT_ID_KEY, environmentId);
        forwardedContext.putAll(
            AgentToolInvocationContext.builder()
                .workspaceId(workspaceId)
                .environmentId(environmentId)
                .build()
                .toToolContext());

        return forwardedContext;
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
                log.warn("Ignoring malformed numeric tool context value '{}'", stringValue);

                return null;
            }
        }

        Class<?> valueClass = value.getClass();

        log.warn("Ignoring tool context value of unsupported type {}", valueClass.getName());

        return null;
    }

    private static @Nullable Object getContextValue(@Nullable ToolContext toolContext, String key) {
        if (toolContext == null) {
            return null;
        }

        Map<String, Object> map = toolContext.getContext();

        return map == null ? null : map.get(key);
    }

    private static Environment toEnvironment(String requestedEnvironment) {
        for (Environment environment : Environment.values()) {
            if (environment.name()
                .equals(requestedEnvironment.toUpperCase(Locale.ROOT))) {
                return environment;
            }
        }

        throw new UnknownEnvironmentException(
            "Unknown environment '" + requestedEnvironment + "'. Supported: DEVELOPMENT, STAGING, PRODUCTION");
    }
}

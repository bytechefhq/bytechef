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

package com.bytechef.ai.copilot.util;

import com.agui.core.state.State;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.ai.tool.AutomationToolInvocationContext;
import com.bytechef.commons.util.NumberUtils;
import com.bytechef.commons.util.StringUtils;
import com.bytechef.platform.ai.tool.TaskTools;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

/**
 * Maps per-run agent {@link State} entries into the Spring AI {@code ToolContext} map handed to copilot tools.
 *
 * @author Ivica Cardic
 */
public final class CopilotToolContextUtils {

    private static final Logger log = LoggerFactory.getLogger(CopilotToolContextUtils.class);

    private CopilotToolContextUtils() {
    }

    /**
     * The {@code llmProvider}/{@code llmModel} keys populated here MUST stay consistent with
     * {@code CopilotChatClientResolver}, this surface's {@code OverrideChatClientResolver} — a mismatch would run a
     * delegate subagent on a different model than the one its caller resolved for this turn.
     */
    public static Map<String, Object> toToolContext(@Nullable State state) {
        if (state == null) {
            return Map.of();
        }

        Map<String, Object> toolContext = new HashMap<>();

        Object allowedComponentNames = state.get(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY);

        if (allowedComponentNames != null) {
            toolContext.put(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, allowedComponentNames);
        }

        Long workspaceId = NumberUtils.asLong(state.get(CopilotConstants.STATE_WORKSPACE_ID));
        Long userId = NumberUtils.asLong(state.get(CopilotConstants.STATE_AUTHENTICATED_USER_ID));
        Long environmentId = NumberUtils.asLong(state.get(CopilotConstants.STATE_ENVIRONMENT_ID));
        String tenantId = StringUtils.asString(state.get(CopilotConstants.STATE_TENANT_ID));
        Authentication authentication = state.get(CopilotConstants.STATE_AUTHENTICATION) instanceof Authentication value
            ? value : null;

        // The carried Authentication is an embedded connected-user principal (no backing platform user); both
        // producers of STATE_AUTHENTICATION -- the embedded connected-user copilot controller and
        // CopilotWorkflowGeneratorImpl, itself reached only from ConnectedUserProjectFacadeImpl -- are the embedded
        // path. It is carried so RehydrateContextToolCallback can restore it on the tool worker, NOT so anything can
        // be skipped there: paired with the tenantId below it lets ConnectedUserResourceMembershipResolver recognise
        // the caller on that worker, and ResourceMembershipDecider then answers each resource-scoped check from the
        // connected user's own membership. An earlier revision also carried a resource-scoped skip mode alongside it,
        // needed only while the tenant failed to reach those threads and the resolver could not answer at all.

        String llmProvider = StringUtils.asString(state.get(CopilotConstants.STATE_USER_SELECTED_LLM_PROVIDER));
        String llmModel = StringUtils.asString(state.get(CopilotConstants.STATE_USER_SELECTED_LLM_MODEL));

        if (llmProvider == null || llmModel == null) {
            if ((llmProvider == null) != (llmModel == null)) {
                log.warn(
                    "Copilot user-selected LLM half-set (provider={}, model={}); not propagating a model override "
                        + "into the tool context",
                    llmProvider, llmModel);
            }

            llmProvider = null;
            llmModel = null;
        }

        toolContext.putAll(
            AgentToolInvocationContext.builder()
                .workspaceId(workspaceId)
                .userId(userId)
                .environmentId(environmentId)
                .tenantId(tenantId)
                .authentication(authentication)
                .llmProvider(llmProvider)
                .llmModel(llmModel)
                .build()
                .toToolContext());

        // Two workspace-id key families exist, and this surface has to populate both. The automation tools
        // (project deployments, asset files, …) read AutomationToolInvocationContext's keys, while the
        // data-table/knowledge-base/context-store tools read AgentToolInvocationContext's. Writing only the
        // latter is why deployment tools opened from a copilot panel failed with "Workspace context
        // unavailable - open this chat from the AI Hub of a workspace" — the AI Hub surface populates both
        // (AiHubSpringAIAgent#toolContext), as does the management MCP surface
        // (WorkspaceScopedSubAgentToolCallback). environmentId matters as much as workspaceId here:
        // AutomationToolInvocationContext.resolveEnvironmentOrDefault silently buckets a missing one to
        // DEVELOPMENT, so an omitted key reads as a wrong answer rather than an error.
        putIfNotNull(toolContext, AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, workspaceId);
        putIfNotNull(toolContext, AutomationToolInvocationContext.TOOL_CONTEXT_USER_ID_KEY, userId);
        putIfNotNull(toolContext, AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, environmentId);

        return toolContext;
    }

    private static void putIfNotNull(Map<String, Object> toolContext, String key, @Nullable Object value) {
        if (value != null) {
            toolContext.put(key, value);
        }
    }
}

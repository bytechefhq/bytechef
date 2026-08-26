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

import static org.assertj.core.api.Assertions.assertThat;

import com.agui.core.state.State;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.ai.tool.AutomationToolInvocationContext;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.platform.ai.tool.TaskTools;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class CopilotToolContextUtilsTest {

    /**
     * Two workspace-id key families reach tools from this one surface: the automation tools (project deployments, asset
     * files) read {@link AutomationToolInvocationContext}'s keys while the data-table/knowledge-base tools read
     * {@link AgentToolInvocationContext}'s. Populating only the latter is what made deployment tools opened from a
     * copilot panel answer "Workspace context unavailable - open this chat from the AI Hub of a workspace".
     */
    @Test
    void testToToolContextPopulatesBothWorkspaceIdKeyFamilies() {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put(CopilotConstants.STATE_WORKSPACE_ID, "7");
        stateMap.put(CopilotConstants.STATE_ENVIRONMENT_ID, "2");
        stateMap.put(CopilotConstants.STATE_AUTHENTICATED_USER_ID, "42");

        Map<String, Object> toolContext = CopilotToolContextUtils.toToolContext(new State(stateMap));

        assertThat(toolContext)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 7L)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 7L)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_USER_ID_KEY, 42L)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 2L);

        assertThat(AutomationToolInvocationContext.fromToolContext(new ToolContext(toolContext)))
            .isNotNull()
            .extracting(AutomationToolInvocationContext::workspaceId)
            .isEqualTo(7L);
    }

    /**
     * resolveEnvironmentOrDefault silently buckets a missing environment to DEVELOPMENT, so an absent key reads as a
     * confidently wrong answer rather than an error — the automation keys must stay absent, not zero, when unset.
     */
    @Test
    void testToToolContextOmitsAutomationKeysThatHaveNoState() {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put(CopilotConstants.STATE_WORKSPACE_ID, "7");

        Map<String, Object> toolContext = CopilotToolContextUtils.toToolContext(new State(stateMap));

        assertThat(toolContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 7L)
            .doesNotContainKey(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY)
            .doesNotContainKey(AutomationToolInvocationContext.TOOL_CONTEXT_USER_ID_KEY);
    }

    @Test
    void testToToolContextCopiesAllowedComponentNames() {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put("workflowId", "wf-1");
        stateMap.put(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, Set.of("slack", "logger"));

        Map<String, Object> toolContext = CopilotToolContextUtils.toToolContext(new State(stateMap));

        assertThat(toolContext)
            .containsEntry(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, Set.of("slack", "logger"));
    }

    @Test
    void testToToolContextWithoutAllowedComponentNamesIsEmpty() {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put("workflowId", "wf-1");

        Map<String, Object> toolContext = CopilotToolContextUtils.toToolContext(new State(stateMap));

        assertThat(toolContext).isEmpty();
    }

    @Test
    void testToToolContextWithNullStateIsEmpty() {
        assertThat(CopilotToolContextUtils.toToolContext(null)).isEmpty();
    }

    @Test
    void testEmitsAgentToolInvocationContextKeys() {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put(CopilotConstants.STATE_AUTHENTICATED_USER_ID, 42L);
        stateMap.put("workspaceId", 7L);
        stateMap.put("environmentId", "2");

        Map<String, Object> toolContext = CopilotToolContextUtils.toToolContext(new State(stateMap));

        assertThat(toolContext)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_USER_ID_KEY, 42L)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 7L)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 2L)
            .doesNotContainKey(AgentToolInvocationContext.TOOL_CONTEXT_CONVERSATION_ID_KEY);
    }

    @Test
    void testEmitsCapturedAuthentication() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("captured-user", "");

        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put(CopilotConstants.STATE_AUTHENTICATION, authentication);
        stateMap.put(CopilotConstants.STATE_TENANT_ID, "acme");

        Map<String, Object> toolContext = CopilotToolContextUtils.toToolContext(new State(stateMap));

        assertThat(toolContext)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_AUTHENTICATION_KEY, authentication)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_TENANT_ID_KEY, "acme")
            // The Authentication is carried so RehydrateContextToolCallback can restore the connected-user principal
            // on the tool worker -- paired with the tenant id, that is what lets the membership resolver govern it
            // there. Nothing is carried to skip a check: ticket 1051 Stage 4 removed the resource-scoped skip mode
            // this context used to carry alongside it.
            .doesNotContainKey("bytechef.agentTool.skipAutomationAuthorization");
    }

    @Test
    void testCarriesNoSkipModeForAConnectedUserRunOnAPooledWorker() throws Exception {
        // Reproduces the real topology. In production the tool context is built on a ForkJoinPool worker
        // (LocalAgent.runAgent dispatches through CompletableFuture.runAsync), NOT on the servlet thread. A connected
        // user's run carries an Authentication and a tenant id and nothing else that touches authorization: the
        // resource-scoped skip mode this used to carry is gone, so no thread can produce one by accident.
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put(CopilotConstants.STATE_AUTHENTICATION, new UsernamePasswordAuthenticationToken("cu", ""));

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        try {
            Map<String, Object> toolContext = executorService.submit(
                () -> CopilotToolContextUtils.toToolContext(new State(stateMap)))
                .get(10, TimeUnit.SECONDS);

            assertThat(toolContext).doesNotContainKey("bytechef.agentTool.skipAutomationAuthorization");
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void testCarriesNoSkipModeEvenWhenTheThreadLocalIsArmed() throws Throwable {
        // A caller that happens to be inside callSkippingChecks when the tool context is built must not leak that
        // into the context and re-arm it on a worker. Nothing reads the thread-local here, and this says so.
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put(CopilotConstants.STATE_AUTHENTICATION, new UsernamePasswordAuthenticationToken("cu", ""));

        Map<String, Object> unarmed = CopilotToolContextUtils.toToolContext(new State(stateMap));

        Map<String, Object> armed = AutomationAuthorizationContext.callSkippingChecks(
            () -> CopilotToolContextUtils.toToolContext(new State(stateMap)));

        assertThat(armed).isEqualTo(unarmed);
        assertThat(armed).doesNotContainKey("bytechef.agentTool.skipAutomationAuthorization");
    }

    @Test
    void testEmitsNeutralKeysAlongsideAllowedComponentNames() {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, Set.of("slack"));
        stateMap.put("workspaceId", 7L);

        Map<String, Object> toolContext = CopilotToolContextUtils.toToolContext(new State(stateMap));

        assertThat(toolContext)
            .containsEntry(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, Set.of("slack"))
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 7L);
    }

    @Test
    void testPropagatesTheUserSelectedModel() {
        State state = new State();

        state.set(CopilotConstants.STATE_ENVIRONMENT_ID, 1L);
        state.set(CopilotConstants.STATE_USER_SELECTED_LLM_PROVIDER, "anthropic");
        state.set(CopilotConstants.STATE_USER_SELECTED_LLM_MODEL, "claude-opus-4");

        Map<String, Object> toolContext = CopilotToolContextUtils.toToolContext(state);

        assertThat(toolContext)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_LLM_PROVIDER_KEY, "anthropic")
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_LLM_MODEL_KEY, "claude-opus-4");
    }

    @Test
    void testOmitsTheModelKeysWhenHalfSet() {
        State state = new State();

        state.set(CopilotConstants.STATE_ENVIRONMENT_ID, 1L);
        state.set(CopilotConstants.STATE_USER_SELECTED_LLM_PROVIDER, "anthropic");

        Map<String, Object> toolContext = CopilotToolContextUtils.toToolContext(state);

        assertThat(toolContext)
            .doesNotContainKey(AgentToolInvocationContext.TOOL_CONTEXT_LLM_PROVIDER_KEY)
            .doesNotContainKey(AgentToolInvocationContext.TOOL_CONTEXT_LLM_MODEL_KEY);
    }
}

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
import com.bytechef.platform.ai.tool.TaskTools;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class CopilotToolContextUtilsTest {

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
            // An embedded run (captured Authentication, no platform user) must skip platform automation RBAC on the
            // tool-execution worker threads.
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_SKIP_AUTHORIZATION_KEY, Boolean.TRUE);
    }

    @Test
    void testDoesNotSkipAutomationAuthorizationWithoutCapturedAuthentication() {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put(CopilotConstants.STATE_AUTHENTICATED_USER_ID, 42L);
        stateMap.put("workspaceId", 7L);

        Map<String, Object> toolContext = CopilotToolContextUtils.toToolContext(new State(stateMap));

        assertThat(toolContext)
            .doesNotContainKey(AgentToolInvocationContext.TOOL_CONTEXT_SKIP_AUTHORIZATION_KEY);
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
}

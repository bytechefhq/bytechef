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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agui.core.state.State;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class CopilotToolContextUtilsTest {

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
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 2L);

        assertThat(AutomationToolInvocationContext.fromToolContext(new ToolContext(toolContext)))
            .isNotNull()
            .extracting(AutomationToolInvocationContext::workspaceId)
            .isEqualTo(7L);
    }

    @Test
    void testToToolContextOmitsAutomationKeysThatHaveNoState() {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put(CopilotConstants.STATE_WORKSPACE_ID, "7");

        Map<String, Object> toolContext = CopilotToolContextUtils.toToolContext(new State(stateMap));

        assertThat(toolContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 7L)
            .doesNotContainKey(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY);
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
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_SKIP_AUTHORIZATION_KEY, Boolean.TRUE);
    }

    @Test
    void testDoesNotSkipAutomationAuthorizationForAPlatformUser() {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put(CopilotConstants.STATE_AUTHENTICATED_USER_ID, 42L);
        stateMap.put(CopilotConstants.STATE_AUTHENTICATION, new UsernamePasswordAuthenticationToken("user", ""));

        Map<String, Object> toolContext = CopilotToolContextUtils.toToolContext(new State(stateMap));

        assertThat(toolContext).doesNotContainKey(AgentToolInvocationContext.TOOL_CONTEXT_SKIP_AUTHORIZATION_KEY);
    }

    @Test
    void testConnectedUserToolCallSkipsChecksOnAPooledWorker() throws Exception {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put(CopilotConstants.STATE_AUTHENTICATION, new UsernamePasswordAuthenticationToken("cu", ""));

        assertThat(callProbeOnPooledWorker(CopilotToolContextUtils.toToolContext(new State(stateMap)))).isTrue();
    }

    @Test
    void testPlatformUserToolCallDoesNotSkipChecksOnAPooledWorker() throws Throwable {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put(CopilotConstants.STATE_AUTHENTICATED_USER_ID, 42L);

        Map<String, Object> toolContext = AutomationAuthorizationContext.callSkippingChecks(
            () -> CopilotToolContextUtils.toToolContext(new State(stateMap)));

        assertThat(toolContext).doesNotContainKey(AgentToolInvocationContext.TOOL_CONTEXT_SKIP_AUTHORIZATION_KEY);
        assertThat(callProbeOnPooledWorker(toolContext)).isFalse();
    }

    private static boolean callProbeOnPooledWorker(Map<String, Object> toolContext) throws Exception {
        AtomicBoolean skipChecksSeenInside = new AtomicBoolean();

        ToolCallback probe = new ToolCallback() {

            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder()
                    .name("probe")
                    .description("probe")
                    .inputSchema("{\"type\":\"object\"}")
                    .build();
            }

            @Override
            public String call(String toolInput) {
                return call(toolInput, null);
            }

            @Override
            public String call(String toolInput, @Nullable ToolContext toolContext) {
                skipChecksSeenInside.set(AutomationAuthorizationContext.isSkipChecks());

                return "ok";
            }
        };

        SecurityContextRehydrator securityContextRehydrator = mock(SecurityContextRehydrator.class);

        when(securityContextRehydrator.withUserSecurityContext(any(), any())).thenAnswer(
            invocation -> invocation.<Supplier<?>>getArgument(1)
                .get());

        ToolCallback wrapped = RehydrateContextToolCallback.wrap(probe, securityContextRehydrator);

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        try {
            executorService.submit(() -> wrapped.call("{}", new ToolContext(toolContext)))
                .get(10, TimeUnit.SECONDS);
        } finally {
            executorService.shutdownNow();
        }

        return skipChecksSeenInside.get();
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

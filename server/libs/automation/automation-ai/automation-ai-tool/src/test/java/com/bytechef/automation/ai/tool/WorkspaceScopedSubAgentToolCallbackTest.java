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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.agent.tool.CoreAgentType;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @author Ivica Cardic
 */
class WorkspaceScopedSubAgentToolCallbackTest {

    private ContextCapturingDelegate delegate;
    private WorkspaceScopedSubAgentToolCallback toolCallback;
    private final WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

    @BeforeEach
    void beforeEach() {
        when(workspaceFacade.getMyWorkspaces()).thenReturn(
            List.of(new Workspace(42L, "Workspace 42"), new Workspace(43L, "Workspace 43")));

        delegate = new ContextCapturingDelegate();

        toolCallback = new WorkspaceScopedSubAgentToolCallback(delegate, workspaceFacade);
    }

    @Test
    void testToolDefinitionKeepsDelegateNameAndExtendsSchema() {
        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("unknown");
        assertThat(toolCallback.getToolDefinition()
            .inputSchema()).contains("workspaceId")
                .contains("environment");
    }

    @Test
    void testBlankRequestReturnsError() {
        String result = toolCallback.call("{\"request\": \" \"}");

        assertThat(result)
            .contains("error")
            .contains("request is required");
        assertThat(delegate.capturedContext).isNull();
    }

    @Test
    void testExplicitWorkspaceIdIsForwardedToDelegateContext() {
        String result = toolCallback.call("{\"request\": \"list servers\", \"workspaceId\": 42}");

        assertThat(result).isEqualTo("done");
        assertThat(delegate.capturedContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 42L)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 42L);
        assertThat(delegate.capturedInput).contains("list servers");
    }

    @Test
    void testDefaultsEnvironmentToDevelopmentWhenOmitted() {
        toolCallback.call("{\"request\": \"list servers\", \"workspaceId\": 42}");

        assertThat(delegate.capturedContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 0L)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 0L);
    }

    @Test
    void testExplicitEnvironmentIsForwardedToDelegateContext() {
        String result = toolCallback.call(
            "{\"request\": \"list servers\", \"workspaceId\": 42, \"environment\": \"PRODUCTION\"}");

        assertThat(result).isEqualTo("done");
        assertThat(delegate.capturedContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 2L)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 2L);
    }

    @Test
    void testExplicitEnvironmentIsCaseInsensitive() {
        String result = toolCallback.call(
            "{\"request\": \"list servers\", \"workspaceId\": 42, \"environment\": \"staging\"}");

        assertThat(result).isEqualTo("done");
        assertThat(delegate.capturedContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 1L);
    }

    @Test
    void testUnknownEnvironmentReturnsError() {
        String result = toolCallback.call(
            "{\"request\": \"list servers\", \"workspaceId\": 42, \"environment\": \"NOPE\"}");

        assertThat(result).contains("error")
            .contains("Unknown environment")
            .contains("NOPE");
        assertThat(delegate.capturedContext).isNull();
    }

    @Test
    void testForwardsWorkspaceIdUnderAgentToolInvocationContextKeyToo() {
        String result = toolCallback.call("{\"request\": \"list servers\", \"workspaceId\": 42}");

        assertThat(result).isEqualTo("done");
        assertThat(delegate.capturedContext)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 42L);
    }

    @Test
    void testChatWorkspaceIsUsedWhenNoWorkspaceIdIsGiven() {
        String result = toolCallback.call(
            "{\"request\": \"list servers\"}",
            new ToolContext(Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 7L)));

        assertThat(result).isEqualTo("done");
        assertThat(delegate.capturedContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 7L)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 7L);
    }

    @Test
    void testExplicitWorkspaceIdOtherThanTheChatWorkspaceIsRejected() {
        String result = toolCallback.call(
            "{\"request\": \"list servers\", \"workspaceId\": 42}",
            new ToolContext(Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 7L)));

        assertThat(result).contains("error")
            .contains("Workspace 42 is not the workspace 7 this conversation is scoped to.");
        assertThat(delegate.capturedContext).isNull();
    }

    @Test
    void testMalformedChatEnvironmentReturnsErrorInsteadOfDefaulting() {
        String result = toolCallback.call(
            "{\"request\": \"list servers\"}",
            new ToolContext(
                Map.of(
                    AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 7L,
                    AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, "not-a-number")));

        assertThat(result).contains("error")
            .contains("environment of this conversation could not be determined");
        assertThat(delegate.capturedContext).isNull();
    }

    @Test
    void testExplicitWorkspaceIdTheCallerIsNotAMemberOfIsRejected() {
        String result = toolCallback.call("{\"request\": \"list servers\", \"workspaceId\": 99}");

        assertThat(result).contains("error")
            .contains("Workspace 99 is not accessible to the caller.");
        assertThat(delegate.capturedContext).isNull();
    }

    @Test
    void testMissingWorkspacePointsToListWorkspaces() {
        String result = toolCallback.call("{\"request\": \"list servers\"}");

        assertThat(result).contains("error")
            .contains("listWorkspaces");
        assertThat(delegate.capturedContext).isNull();
    }

    @Test
    void testWrapsPlainToolCallbackDelegate() {
        PlainCopilotDelegate copilotDelegate = new PlainCopilotDelegate();

        WorkspaceScopedSubAgentToolCallback copilotToolCallback =
            new WorkspaceScopedSubAgentToolCallback(copilotDelegate, workspaceFacade);

        String result = copilotToolCallback.call("{\"request\": \"list tables\", \"workspaceId\": 43}");

        assertThat(result).isEqualTo("delegated");
        assertThat(copilotToolCallback.getToolDefinition()
            .name()).isEqualTo("data_table_agent");
        assertThat(copilotToolCallback.getToolDefinition()
            .inputSchema()).contains("workspaceId");
        assertThat(copilotDelegate.capturedContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 43L)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 43L);
    }

    private static final class ContextCapturingDelegate implements ToolCallback {

        private Map<String, Object> capturedContext;
        private String capturedInput;

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name(CoreAgentType.UNKNOWN.key())
                .description("Manages project deployments.")
                .inputSchema(
                    "{\"type\": \"object\", \"properties\": {\"request\": {\"type\": \"string\"}}, \"required\": [\"request\"]}")
                .build();
        }

        @Override
        public String call(String toolInput) {
            return call(toolInput, null);
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            capturedInput = toolInput;
            capturedContext = toolContext == null ? null : toolContext.getContext();

            return "done";
        }
    }

    private static final class PlainCopilotDelegate implements ToolCallback {

        private Map<String, Object> capturedContext;

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name("data_table_agent")
                .description("Delegates data table work.")
                .inputSchema("{\"type\": \"object\", \"properties\": {\"request\": {\"type\": \"string\"}}}")
                .build();
        }

        @Override
        public String call(String toolInput) {
            return call(toolInput, null);
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            capturedContext = toolContext == null ? null : toolContext.getContext();

            return "delegated";
        }
    }
}

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

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.service.WorkspaceService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 *
 * @author Ivica Cardic
 */
class WorkspaceScopedSubAgentToolCallbackTest {

    private ContextCapturingDelegate delegate;
    private WorkspaceService workspaceService;
    private WorkspaceScopedSubAgentToolCallback toolCallback;

    @BeforeEach
    void beforeEach() {
        delegate = new ContextCapturingDelegate();
        workspaceService = mock(WorkspaceService.class);
        toolCallback = new WorkspaceScopedSubAgentToolCallback(delegate, workspaceService);
    }

    @Test
    void testToolDefinitionKeepsDelegateNameAndExtendsSchema() {
        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("mcp_agent");
        assertThat(toolCallback.getToolDefinition()
            .inputSchema()).contains("workspaceId")
                .contains("environment");
    }

    @Test
    void testBlankRequestReturnsError() {
        String result = toolCallback.call("{\"request\": \" \"}");

        assertThat(result).contains("error");
        assertThat(result).contains("request is required");
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

    /**
     * Every delegate wrapped by this class gets the FILES source ordinal written unconditionally — the key is a no-op
     * for delegates that never create asset files, but the {@code asset_file_agent} delegate cannot get the correct
     * attribution any other way on this surface (see {@link WorkspaceScopedSubAgentToolCallback} class Javadoc).
     */
    @Test
    void testForwardsSourceOrdinalUnconditionally() {
        toolCallback.call("{\"request\": \"list servers\", \"workspaceId\": 42}");

        assertThat(delegate.capturedContext)
            .containsEntry(
                AutomationToolInvocationContext.TOOL_CONTEXT_SOURCE_ORDINAL_KEY,
                AutomationToolInvocationContext.SOURCE_ORDINAL_FILES);
    }

    /**
     * Without an explicit environment, {@code resolveEnvironmentOrDefault} treats a missing key as DEVELOPMENT — but
     * this wrapper must write that default explicitly rather than leaving the key absent, or a caller building
     * {@link AutomationToolInvocationContext} straight from the forwarded map (rather than through the defaulting
     * accessor) would see no environment at all.
     */
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

    /**
     * Two workspace-id key families exist in this codebase: {@link AutomationToolInvocationContext} (read by asset-file
     * / automation tools) and {@link AgentToolInvocationContext} (read by ALL data-table, knowledge-base and EE
     * context-store tool callbacks). The AI Hub chat surface populates both (see
     * {@code AiHubSpringAIAgent#toolContext}), so this wrapper must too — otherwise those delegates' inner tools still
     * see no workspace and fail with "Workspace context unavailable".
     */
    @Test
    void testForwardsWorkspaceIdUnderAgentToolInvocationContextKeyToo() {
        String result = toolCallback.call("{\"request\": \"list servers\", \"workspaceId\": 42}");

        assertThat(result).isEqualTo("done");
        assertThat(delegate.capturedContext)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 42L);
    }

    @Test
    void testSingleWorkspaceIsAutoSelected() {
        Workspace workspace = new Workspace();

        workspace.setId(7L);
        workspace.setName("Main");

        when(workspaceService.getWorkspaces()).thenReturn(List.of(workspace));

        String result = toolCallback.call("{\"request\": \"list servers\"}");

        assertThat(result).isEqualTo("done");
        assertThat(delegate.capturedContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 7L)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 7L);
    }

    @Test
    void testMultipleWorkspacesReturnCandidateList() {
        Workspace firstWorkspace = new Workspace();

        firstWorkspace.setId(1L);
        firstWorkspace.setName("Alpha");

        Workspace secondWorkspace = new Workspace();

        secondWorkspace.setId(2L);
        secondWorkspace.setName("Beta");

        when(workspaceService.getWorkspaces()).thenReturn(List.of(firstWorkspace, secondWorkspace));

        String result = toolCallback.call("{\"request\": \"list servers\"}");

        assertThat(result).contains("workspace_required");
        assertThat(result).contains("Alpha");
        assertThat(result).contains("Beta");
        assertThat(delegate.capturedContext).isNull();
    }

    @Test
    void testWrapsPlainToolCallbackDelegate() {
        PlainCopilotDelegate copilotDelegate = new PlainCopilotDelegate();

        WorkspaceScopedSubAgentToolCallback copilotToolCallback =
            new WorkspaceScopedSubAgentToolCallback(copilotDelegate, workspaceService);

        String result = copilotToolCallback.call("{\"request\": \"list tables\", \"workspaceId\": 9}");

        assertThat(result).isEqualTo("delegated");
        assertThat(copilotToolCallback.getToolDefinition()
            .name()).isEqualTo("data_table_agent");
        assertThat(copilotToolCallback.getToolDefinition()
            .inputSchema()).contains("workspaceId");
        assertThat(copilotDelegate.capturedContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 9L)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 9L);
    }

    /**
     * Fake delegate that records the forwarded input and ToolContext and answers directly, so the wrapper's behaviour
     * is observable without stubbing the specialist's ChatClient chain.
     */
    private static final class ContextCapturingDelegate extends SubAgentToolCallback {

        private Map<String, Object> capturedContext;
        private String capturedInput;

        ContextCapturingDelegate() {
            super(AutomationSubAgentType.MCP_AGENT, mock(ChatClient.class), "Manages MCP servers.");
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            capturedInput = toolInput;
            capturedContext = toolContext == null ? null : toolContext.getContext();

            return "done";
        }
    }

    /**
     * Fake copilot-domain delegate: a plain {@link ToolCallback} with the same {@code {request}} input schema the real
     * delegates declare, proving the wrapper no longer requires a {@link SubAgentToolCallback}.
     */
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

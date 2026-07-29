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

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.service.WorkspaceService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;

/**
 *
 * @author Ivica Cardic
 */
class WorkspaceScopedManagerToolCallbackTest {

    private ContextCapturingDelegate delegate;
    private WorkspaceService workspaceService;
    private WorkspaceScopedManagerToolCallback toolCallback;

    @BeforeEach
    void beforeEach() {
        delegate = new ContextCapturingDelegate();
        workspaceService = mock(WorkspaceService.class);
        toolCallback = new WorkspaceScopedManagerToolCallback(delegate, workspaceService);
    }

    @Test
    void testToolDefinitionKeepsDelegateNameAndExtendsSchema() {
        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("mcp_manager");
        assertThat(toolCallback.getToolDefinition()
            .inputSchema()).contains("workspaceId");
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
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 42L);
        assertThat(delegate.capturedInput).contains("list servers");
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
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 7L);
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

    /**
     * Fake delegate that records the forwarded input and ToolContext and answers directly, so the wrapper's behaviour
     * is observable without stubbing the specialist's ChatClient chain.
     */
    private static final class ContextCapturingDelegate extends ManagerSubAgentToolCallback {

        private Map<String, Object> capturedContext;
        private String capturedInput;

        ContextCapturingDelegate() {
            super(ManagerAgentType.MCP_MANAGER, mock(ChatClient.class), "Manages MCP servers.");
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            capturedInput = toolInput;
            capturedContext = toolContext == null ? null : toolContext.getContext();

            return "done";
        }
    }
}

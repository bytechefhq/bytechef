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
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 *
 * @author Ivica Cardic
 */
class WorkspaceScopedFlatToolCallbackTest {

    private ContextCapturingDelegate delegate;
    private WorkspaceService workspaceService;
    private WorkspaceScopedFlatToolCallback toolCallback;

    @BeforeEach
    void beforeEach() {
        delegate = new ContextCapturingDelegate();
        workspaceService = mock(WorkspaceService.class);
        toolCallback = new WorkspaceScopedFlatToolCallback(delegate, workspaceService);
    }

    @Test
    void testToolDefinitionKeepsDelegateNameAndExtendsSchemaWithoutDroppingOriginalProperties() {
        ToolDefinition toolDefinition = toolCallback.getToolDefinition();

        assertThat(toolDefinition.name()).isEqualTo("createMcpServer");
        assertThat(toolDefinition.inputSchema())
            .contains("workspaceId")
            .contains("environment")
            .contains("name")
            .contains("enabled");
    }

    @Test
    void testExplicitWorkspaceIdIsForwardedAndOtherFieldsSurviveIntact() {
        String result =
            toolCallback.call("{\"name\": \"my server\", \"environment\": \"STAGING\", \"workspaceId\": 42}");

        assertThat(result).isEqualTo("done");
        assertThat(delegate.capturedContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 42L)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 1L);
        assertThat(delegate.capturedInput)
            .contains("my server")
            .doesNotContain("workspaceId")
            .doesNotContain("STAGING");
    }

    @Test
    void testDefaultsEnvironmentToDevelopmentWhenOmitted() {
        toolCallback.call("{\"name\": \"my server\", \"workspaceId\": 42}");

        assertThat(delegate.capturedContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 0L);
    }

    @Test
    void testUnknownEnvironmentReturnsError() {
        String result = toolCallback.call("{\"name\": \"my server\", \"workspaceId\": 42, \"environment\": \"NOPE\"}");

        assertThat(result).contains("error")
            .contains("Unknown environment")
            .contains("NOPE");
        assertThat(delegate.capturedContext).isNull();
    }

    @Test
    void testSingleWorkspaceIsAutoSelected() {
        Workspace workspace = new Workspace();

        workspace.setId(7L);
        workspace.setName("Main");

        when(workspaceService.getWorkspaces()).thenReturn(List.of(workspace));

        String result = toolCallback.call("{\"name\": \"my server\"}");

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

        String result = toolCallback.call("{\"name\": \"my server\"}");

        assertThat(result).contains("workspace_required")
            .contains("Alpha")
            .contains("Beta");
        assertThat(delegate.capturedContext).isNull();
    }

    @Test
    void testEmptyInputToolIsHandledWithoutError() {
        Workspace workspace = new Workspace();

        workspace.setId(9L);
        workspace.setName("Main");

        when(workspaceService.getWorkspaces()).thenReturn(List.of(workspace));

        String result = toolCallback.call("{}");

        assertThat(result).isEqualTo("done");
        assertThat(delegate.capturedContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 9L);
    }

    /**
     * Fake delegate that records the forwarded input and ToolContext and answers directly, so the wrapper's behaviour
     * is observable without a real facade.
     */
    private static final class ContextCapturingDelegate implements ToolCallback {

        private Map<String, Object> capturedContext;
        private String capturedInput;

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name("createMcpServer")
                .description("Create a new MCP server.")
                .inputSchema(
                    "{\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}, "
                        + "\"environment\": {\"type\": \"string\"}, \"enabled\": {\"type\": \"boolean\"}}, "
                        + "\"required\": [\"name\", \"environment\"]}")
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
}

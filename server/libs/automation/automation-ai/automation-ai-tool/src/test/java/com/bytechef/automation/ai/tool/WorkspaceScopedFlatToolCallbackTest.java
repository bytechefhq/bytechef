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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.WorkspaceService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class WorkspaceScopedFlatToolCallbackTest {

    private static final long USER_ID = 1L;

    private static final String CREATE_MCP_SERVER_SCHEMA =
        "{\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}, "
            + "\"environment\": {\"type\": \"string\", \"description\": \"Target environment\"}, "
            + "\"enabled\": {\"type\": \"boolean\"}}, \"required\": [\"name\", \"environment\"]}";

    private static final String NAME_ONLY_SCHEMA =
        "{\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}}}";

    private static final String ENVIRONMENT_FIELD = "environment";

    private final JsonMapper jsonMapper = new JsonMapper();
    private ContextCapturingDelegate delegate;
    private WorkspaceFacade workspaceFacade;
    private WorkspaceScopeResolver workspaceScopeResolver;
    private WorkspaceScopedFlatToolCallback toolCallback;

    @BeforeEach
    void beforeEach() {
        delegate = new ContextCapturingDelegate(CREATE_MCP_SERVER_SCHEMA);
        workspaceFacade = mock(WorkspaceFacade.class);

        UserService userService = mock(UserService.class);
        User user = new User();

        user.setId(USER_ID);

        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user));
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(
            List.of(workspace(42L, "Primary"), workspace(43L, "Secondary")));

        workspaceScopeResolver = new WorkspaceScopeResolver(
            userService, workspaceFacade, mock(WorkspaceService.class));

        toolCallback = new WorkspaceScopedFlatToolCallback(delegate, workspaceScopeResolver);
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
    void testToolDefinitionKeepsDelegateDeclaredEnvironmentPropertyUntouched() {
        JsonNode schemaNode = jsonMapper.readTree(toolCallback.getToolDefinition()
            .inputSchema());

        JsonNode environmentNode = schemaNode.path("properties")
            .path(ENVIRONMENT_FIELD);

        assertThat(environmentNode.path("description")
            .asString()).isEqualTo("Target environment");
        assertThat(schemaNode.path("required")
            .toString()).contains(ENVIRONMENT_FIELD);
        assertThat(schemaNode.path("properties")
            .has("workspaceId")).isTrue();
    }

    @Test
    void testToolDefinitionAddsEnvironmentWhenDelegateDoesNotDeclareIt() {
        WorkspaceScopedFlatToolCallback nameOnlyToolCallback = new WorkspaceScopedFlatToolCallback(
            new ContextCapturingDelegate(NAME_ONLY_SCHEMA), workspaceScopeResolver);

        JsonNode schemaNode = jsonMapper.readTree(nameOnlyToolCallback.getToolDefinition()
            .inputSchema());

        JsonNode propertiesNode = schemaNode.path("properties");

        assertThat(propertiesNode.has("workspaceId")).isTrue();
        assertThat(propertiesNode.path(ENVIRONMENT_FIELD)
            .path("description")
            .asString()).contains("defaults to DEVELOPMENT");
    }

    @Test
    void testExplicitWorkspaceIdIsStrippedAndDelegateDeclaredEnvironmentIsForwarded() {
        String result =
            toolCallback.call("{\"name\": \"my server\", \"environment\": \"STAGING\", \"workspaceId\": 42}");

        assertThat(result).isEqualTo("done");
        assertThat(delegate.capturedContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 42L)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 1L);
        assertThat(delegate.capturedInput)
            .contains("my server")
            .contains("STAGING")
            .doesNotContain("workspaceId");
    }

    @Test
    void testEnvironmentIsStrippedWhenDelegateDoesNotDeclareIt() {
        ContextCapturingDelegate nameOnlyDelegate = new ContextCapturingDelegate(NAME_ONLY_SCHEMA);

        WorkspaceScopedFlatToolCallback nameOnlyToolCallback =
            new WorkspaceScopedFlatToolCallback(nameOnlyDelegate, workspaceScopeResolver);

        String result =
            nameOnlyToolCallback.call("{\"name\": \"x\", \"environment\": \"STAGING\", \"workspaceId\": 42}");

        assertThat(result).isEqualTo("done");
        assertThat(nameOnlyDelegate.capturedContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 1L);
        assertThat(nameOnlyDelegate.capturedInput)
            .doesNotContain("STAGING")
            .doesNotContain("workspaceId");
    }

    @Test
    void testCreateMcpServerReceivesItsRequiredEnvironment() {
        WorkspaceMcpServerFacade workspaceMcpServerFacade = mock(WorkspaceMcpServerFacade.class);
        McpServer mcpServer = mock(McpServer.class);

        when(mcpServer.getId()).thenReturn(5L);
        when(mcpServer.getName()).thenReturn("my server");
        when(mcpServer.getType()).thenReturn(PlatformType.AUTOMATION);
        when(mcpServer.getEnvironment()).thenReturn(Environment.STAGING);
        when(workspaceMcpServerFacade.createWorkspaceMcpServer(
            eq("my server"), eq(PlatformType.AUTOMATION), eq(Environment.STAGING), eq(false), isNull(), eq(42L)))
                .thenReturn(mcpServer);

        WorkspaceScopedFlatToolCallback createMcpServerToolCallback = new WorkspaceScopedFlatToolCallback(
            new CreateMcpServerToolCallback(workspaceMcpServerFacade), workspaceScopeResolver);

        String result = createMcpServerToolCallback.call(
            "{\"name\": \"my server\", \"environment\": \"STAGING\", \"workspaceId\": 42}");

        assertThat(result).doesNotContain("error")
            .contains("\"mcpServerId\":5");

        verify(workspaceMcpServerFacade).createWorkspaceMcpServer(
            eq("my server"), eq(PlatformType.AUTOMATION), eq(Environment.STAGING), eq(false), isNull(), eq(42L));
    }

    @Test
    void testNonObjectInputReturnsError() {
        String result = toolCallback.call("[1, 2]");

        assertThat(result).contains("error")
            .contains("JSON object");
        assertThat(delegate.capturedContext).isNull();
    }

    @Test
    void testNonNumericWorkspaceIdReturnsError() {
        String result = toolCallback.call("{\"name\": \"my server\", \"workspaceId\": \"abc\"}");

        assertThat(result).contains("error")
            .contains("workspaceId must be")
            .doesNotContain("workspace_required");
        assertThat(delegate.capturedContext).isNull();
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
    void testExplicitForeignWorkspaceIdIsRejectedBeforeReachingDelegate() {
        String result = toolCallback.call("{\"name\": \"my server\", \"workspaceId\": 99}");

        assertThat(result).contains("error")
            .contains("not accessible");
        assertThat(delegate.capturedContext).isNull();
    }

    @Test
    void testSingleWorkspaceIsAutoSelected() {
        Workspace workspace = new Workspace();

        workspace.setId(7L);
        workspace.setName("Main");

        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(workspace));

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

        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(firstWorkspace, secondWorkspace));

        String result = toolCallback.call("{\"name\": \"my server\"}");

        assertThat(result).contains("workspace_required")
            .contains("Alpha")
            .contains("Beta");
        assertThat(delegate.capturedContext).isNull();
    }

    @Test
    void testForwardsAgentToolInvocationContextFamilyUnconditionally() {
        toolCallback.call("{\"name\": \"my server\", \"environment\": \"STAGING\", \"workspaceId\": 42}");

        assertThat(delegate.capturedContext)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 42L)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 1L);
    }

    @Test
    void testEmptyInputToolIsHandledWithoutError() {
        Workspace workspace = new Workspace();

        workspace.setId(9L);
        workspace.setName("Main");

        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(workspace));

        String result = toolCallback.call("{}");

        assertThat(result).isEqualTo("done");
        assertThat(delegate.capturedContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 9L);
    }

    private static Workspace workspace(long id, String name) {
        Workspace workspace = new Workspace();

        workspace.setId(id);
        workspace.setName(name);

        return workspace;
    }

    private static final class ContextCapturingDelegate implements ToolCallback {

        private final String inputSchema;
        private Map<String, Object> capturedContext;
        private String capturedInput;

        private ContextCapturingDelegate(String inputSchema) {
            this.inputSchema = inputSchema;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name("createMcpServer")
                .description("Create a new MCP server.")
                .inputSchema(inputSchema)
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

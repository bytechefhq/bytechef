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

package com.bytechef.ai.mcp.server.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.service.WorkspaceService;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import com.bytechef.server.ServerApplication;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ServerApplication.class,
    properties = {
        "bytechef.ai.mcp.server.enabled=true", "bytechef.ai.copilot.enabled=false",
        "bytechef.ai.vectorstore.pgvector.dimensions=768"
    })
@Import(PostgreSQLContainerConfiguration.class)
class ManagementMcpWorkflowAuthoringIntTest {

    private static final List<String> INTELLIGENT_TOOL_NAMES = List.of(
        "buildWorkflow", "importWorkflow", "authorSkill", "debugWorkflowExecution", "configureMcpServer");

    private static final String SETUP_LOGIN = "admin";
    private static final String SLACK_CHANNEL_ID = "C0123456789";
    private static final String SLACK_NODE_NAME = "sendChannelMessage_1";

    @MockitoSpyBean
    private ActionDefinitionFacade actionDefinitionFacade;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ManagementMcpServerConfiguration managementMcpServerConfiguration;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ProjectFacade projectFacade;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkspaceConnectionFacade workspaceConnectionFacade;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private WorkspaceUserService workspaceUserService;

    private String authorLogin;
    private long connectionId;
    private String connectionName;
    private String memberLogin;
    private long projectId;
    private Map<String, ToolCallback> toolCallbacks;
    private long workspaceId;

    @BeforeEach
    void beforeEach() {
        toolCallbacks = new LinkedHashMap<>();

        ToolCallbackProvider toolCallbackProvider = managementMcpServerConfiguration.toolCallbackProvider();

        for (ToolCallback toolCallback : toolCallbackProvider.getToolCallbacks()) {
            ToolDefinition toolDefinition = toolCallback.getToolDefinition();

            toolCallbacks.put(toolDefinition.name(), toolCallback);
        }

        authenticate(SETUP_LOGIN, AuthorityConstants.ADMIN);

        authorLogin = createUser(AuthorityConstants.ADMIN);
        memberLogin = createUser(AuthorityConstants.USER);

        workspaceId = createWorkspace();

        addWorkspaceUser(authorLogin, workspaceId);
        addWorkspaceUser(memberLogin, workspaceId);

        projectId = projectFacade.createProject(
            ProjectDTO.builder()
                .name("MCP Authoring " + UUID.randomUUID())
                .workspaceId(workspaceId)
                .build());

        connectionName = "Slack " + UUID.randomUUID();

        connectionId = createSlackConnection(workspaceId, connectionName);

        authenticate(authorLogin, AuthorityConstants.ADMIN);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCopilotDisabledServerHasNoIntelligentToolsAndInstructionsDoNotNameThem() {
        Set<String> toolNames = toolCallbacks.keySet();

        assertThat(toolNames)
            .doesNotContainAnyElementsOf(INTELLIGENT_TOOL_NAMES)
            .contains(
                "createProjectWorkflow", "updateWorkflow", "getWorkflow", "getWorkflowBuildInstructions",
                "listConnections", "lookupPropertyOptions", "saveWorkflowTestConnection", "publishProject");

        assertThat(applicationContext.containsBean("mcpAsyncServer")).isTrue();
        assertThat(applicationContext.containsBean("canonicalMcpAsyncServer")).isTrue();

        String instructions = ManagementMcpInstructions.build(toolNames);

        assertThat(instructions).contains("To author a workflow yourself", "lookupPropertyOptions");

        for (String intelligentToolName : INTELLIGENT_TOOL_NAMES) {
            assertThat(instructions).doesNotContain(intelligentToolName);
        }
    }

    @Test
    void testAuthorsAWorkflowWithoutAnAiModel() {
        ToolResult createResult = callTool(
            "createProjectWorkflow", Map.of("projectId", projectId, "name", "Notify Slack"));

        assertThat(createResult.error()).as(createResult.text())
            .isFalse();

        String workflowId = (String) JsonUtils.readMap(createResult.text())
            .get("workflow_id");

        assertThat(workflowId).isNotBlank();

        int createdVersion = getWorkflowVersion(workflowId);

        ToolResult invalidResult = callTool(
            "updateWorkflow",
            Map.of(
                "workflowId", workflowId, "definition", definition("slack/v1/doSomething"),
                "version", createdVersion));

        assertThat(invalidResult.error()).as(invalidResult.text())
            .isTrue();
        assertThat(invalidResult.text()).startsWith("Nothing was saved");
        assertThat(getWorkflowTasks(workflowId)).isEmpty();
        assertThat(getWorkflowVersion(workflowId)).isEqualTo(createdVersion);

        ToolResult validResult = callTool(
            "updateWorkflow",
            Map.of(
                "workflowId", workflowId, "definition", definition("slack/v1/sendChannelMessage"),
                "version", createdVersion));

        assertThat(validResult.error()).as(validResult.text())
            .isFalse();

        Map<String, ?> validResultMap = JsonUtils.readMap(validResult.text());

        assertThat(String.valueOf(validResultMap.get("warnings"))).contains("Missing required connection");
        assertThat(getWorkflowTasks(workflowId)).hasSize(1);

        ToolResult staleResult = callTool(
            "updateWorkflow",
            Map.of(
                "workflowId", workflowId, "definition", definition("slack/v1/sendChannelMessage"),
                "version", createdVersion));

        assertThat(staleResult.error()).as(staleResult.text())
            .isTrue();
        assertThat(staleResult.text()).contains("was modified after version");

        ToolResult listResult = callTool(
            "listConnections", Map.of("workspaceId", workspaceId, "componentName", "slack"));

        assertThat(listResult.error()).as(listResult.text())
            .isFalse();

        List<?> connections = JsonUtils.readList(listResult.text());

        assertThat(connections)
            .singleElement()
            .satisfies(connection -> {
                Map<?, ?> connectionMap = (Map<?, ?>) connection;

                assertThat(((Number) connectionMap.get("id")).longValue()).isEqualTo(connectionId);
                assertThat(connectionMap.get("name")).isEqualTo(connectionName);
                assertThat(connectionMap.get("environment")).isEqualTo(Environment.DEVELOPMENT.name());
            });

        ToolResult bindResult = callTool(
            "saveWorkflowTestConnection",
            Map.of(
                "workflowId", workflowId, "workflowNodeName", SLACK_NODE_NAME, "connectionKey", "slack",
                "connectionId", connectionId));

        assertThat(bindResult.error()).as(bindResult.text())
            .isFalse();
        assertThat(bindResult.text()).contains("has been bound to node '" + SLACK_NODE_NAME + "'");

        ToolResult publishResult = callTool("publishProject", Map.of("projectId", projectId));

        assertThat(publishResult.error()).as(publishResult.text())
            .isFalse();
        assertThat(((Number) JsonUtils.readMap(publishResult.text())
            .get("published_version")).intValue()).isPositive();
    }

    @Test
    void testListConnectionsRejectsAWorkspaceTheCallerIsNotAMemberOf() {
        authenticate(SETUP_LOGIN, AuthorityConstants.ADMIN);

        long otherWorkspaceId = createWorkspace();

        String otherConnectionName = "Hidden Slack " + UUID.randomUUID();

        createSlackConnection(otherWorkspaceId, otherConnectionName);

        authenticate(memberLogin, AuthorityConstants.USER);

        ToolResult ownResult = callTool(
            "listConnections", Map.of("workspaceId", workspaceId, "componentName", "slack"));

        assertThat(ownResult.error()).as(ownResult.text())
            .isFalse();
        assertThat(ownResult.text()).contains(connectionName);

        ToolResult listResult = callTool(
            "listConnections", Map.of("workspaceId", otherWorkspaceId, "componentName", "slack"));

        assertThat(listResult.error()).isTrue();
        assertThat(listResult.text())
            .contains("is not accessible")
            .doesNotContain(otherConnectionName);
    }

    @Test
    void testLookupPropertyOptionsResolvesOptionsWithAConnectionOfTheCallersWorkspace() {
        doReturn(List.of(new Option(ComponentDsl.option("general", SLACK_CHANNEL_ID))))
            .when(actionDefinitionFacade)
            .executeOptions(
                eq("slack"), anyInt(), eq("sendChannelMessage"), eq("channel"), anyMap(), anyList(), anyString(),
                eq(connectionId));

        ToolResult lookupResult = callTool("lookupPropertyOptions", slackChannelLookupInput(connectionId));

        assertThat(lookupResult.error()).as(lookupResult.text())
            .isFalse();
        assertThat(lookupResult.text()).contains(SLACK_CHANNEL_ID);

        verify(actionDefinitionFacade).executeOptions(
            eq("slack"), anyInt(), eq("sendChannelMessage"), eq("channel"), anyMap(), anyList(), anyString(),
            eq(connectionId));
    }

    @Test
    void testLookupPropertyOptionsRefusesAConnectionOfAnotherWorkspace() {
        authenticate(SETUP_LOGIN, AuthorityConstants.ADMIN);

        long otherWorkspaceId = createWorkspace();

        long foreignConnectionId = createSlackConnection(otherWorkspaceId, "Foreign Slack " + UUID.randomUUID());

        authenticate(authorLogin, AuthorityConstants.ADMIN);

        ToolResult lookupResult = callTool("lookupPropertyOptions", slackChannelLookupInput(foreignConnectionId));

        assertThat(lookupResult.error()).as(lookupResult.text())
            .isTrue();
        assertThat(lookupResult.text()).contains(
            "Connection " + foreignConnectionId + " is not a slack connection of workspace " + workspaceId,
            "listConnections");

        verify(actionDefinitionFacade, never()).executeOptions(
            anyString(), anyInt(), anyString(), anyString(), anyMap(), anyList(), anyString(), nullable(Long.class));
    }

    @Test
    void testUpdateWorkflowIsDeniedToAUserWithoutAccessToTheProject() {
        Class<? extends PermissionService> permissionServiceClass = permissionService.getClass();

        assertThat(permissionServiceClass.getName()).startsWith("com.bytechef.ee.");

        ToolResult createResult = callTool(
            "createProjectWorkflow", Map.of("projectId", projectId, "name", "Members Only"));

        assertThat(createResult.error()).as(createResult.text())
            .isFalse();

        String workflowId = (String) JsonUtils.readMap(createResult.text())
            .get("workflow_id");

        int version = getWorkflowVersion(workflowId);

        authenticate(SETUP_LOGIN, AuthorityConstants.ADMIN);

        String outsiderLogin = createUser(AuthorityConstants.USER);

        authenticate(outsiderLogin, AuthorityConstants.USER);

        ToolResult updateResult = callTool(
            "updateWorkflow",
            Map.of(
                "workflowId", workflowId, "definition", definition("slack/v1/sendChannelMessage"), "version",
                version));

        assertThat(updateResult.error()).isTrue();
        assertThat(updateResult.text()).contains("do not have access");

        authenticate(authorLogin, AuthorityConstants.ADMIN);

        assertThat(getWorkflowVersion(workflowId)).isEqualTo(version);
        assertThat(getWorkflowTasks(workflowId)).isEmpty();
    }

    @Test
    void testValidatorIsWired() {
        assertThat(applicationContext.getBeanProvider(WorkflowValidatorFacade.class)
            .getIfAvailable()).isNotNull();
    }

    private void addWorkspaceUser(String login, long workspaceId) {
        User user = userService.getUser(login);

        workspaceUserService.addWorkspaceUser(user.getId(), workspaceId, WorkspaceRole.ADMIN);
    }

    private ToolResult callTool(String name, Map<String, ?> input) {
        ToolCallback toolCallback = toolCallbacks.get(name);

        assertThat(toolCallback).as(name)
            .isNotNull();

        String text;

        try {
            text = toolCallback.call(
                JsonUtils.write(input),
                new ToolContext(
                    Map.of(McpToolUtils.TOOL_CONTEXT_MCP_EXCHANGE_KEY, mock(McpSyncServerExchange.class))));
        } catch (Exception exception) {
            return new ToolResult(exception.getMessage(), true);
        }

        if (isErrorEnvelope(text)) {
            Map<String, ?> errorMap = JsonUtils.readMap(text);

            return new ToolResult(String.valueOf(errorMap.get("error")), true);
        }

        return new ToolResult(text, false);
    }

    private long createSlackConnection(long workspaceId, String name) {
        return workspaceConnectionFacade.create(
            workspaceId,
            ConnectionDTO.builder()
                .authorizationType(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .componentName("slack")
                .connectionVersion(1)
                .environmentId(Environment.DEVELOPMENT.ordinal())
                .name(name)
                .parameters(Map.of("access_token", "test-token"))
                .build());
    }

    private Map<String, ?> slackChannelLookupInput(long lookupConnectionId) {
        return Map.of(
            "workspaceId", workspaceId, "kind", "ACTION", "componentName", "slack", "operationName",
            "sendChannelMessage", "propertyName", "channel", "connectionId", lookupConnectionId);
    }

    private String createUser(String authority) {
        String login = "mcp-" + UUID.randomUUID();

        AdminUserDTO adminUserDTO = new AdminUserDTO();

        adminUserDTO.setAuthorities(Set.of(authority));
        adminUserDTO.setEmail(login + "@example.com");
        adminUserDTO.setFirstName("Mcp");
        adminUserDTO.setLastName("Author");
        adminUserDTO.setLogin(login);

        User user = userService.create(adminUserDTO);

        return user.getLogin();
    }

    private long createWorkspace() {
        Workspace workspace = workspaceService.create(new Workspace("MCP Workspace " + UUID.randomUUID()));

        return workspace.getId();
    }

    private Map<String, ?> getWorkflow(String workflowId) {
        ToolResult workflowResult = callTool("getWorkflow", Map.of("workflowId", workflowId));

        assertThat(workflowResult.error()).as(workflowResult.text())
            .isFalse();

        return JsonUtils.readMap(workflowResult.text());
    }

    private List<?> getWorkflowTasks(String workflowId) {
        Map<String, ?> workflow = getWorkflow(workflowId);

        Map<String, ?> definition = JsonUtils.readMap((String) workflow.get("definition"));

        Object tasks = definition.get("tasks");

        return tasks == null ? List.of() : (List<?>) tasks;
    }

    private int getWorkflowVersion(String workflowId) {
        Map<String, ?> workflow = getWorkflow(workflowId);

        return ((Number) workflow.get("version")).intValue();
    }

    private static void authenticate(String login, String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(login, "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    private static String definition(String taskType) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("channel", SLACK_CHANNEL_ID);
        parameters.put("text", "Hello from an MCP client");

        Map<String, Object> task = new LinkedHashMap<>();

        task.put("label", "Send Channel Message");
        task.put("name", SLACK_NODE_NAME);
        task.put("parameters", parameters);
        task.put("type", taskType);

        Map<String, Object> definition = new LinkedHashMap<>();

        definition.put("label", "Notify Slack");
        definition.put("description", "");
        definition.put("inputs", List.of());
        definition.put(
            "triggers", List.of(Map.of("label", "Manual", "name", "trigger_1", "type", "manual/v1/manual")));
        definition.put("tasks", new ArrayList<>(List.of(task)));

        return JsonUtils.write(definition);
    }

    private static boolean isErrorEnvelope(String text) {
        if (text == null || !text.startsWith("{")) {
            return false;
        }

        Map<String, ?> map = JsonUtils.readMap(text);

        return map.size() == 1 && map.get("error") instanceof String;
    }

    private record ToolResult(String text, boolean error) {
    }
}

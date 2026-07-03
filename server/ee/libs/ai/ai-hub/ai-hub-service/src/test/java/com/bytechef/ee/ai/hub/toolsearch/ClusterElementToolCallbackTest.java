/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.ai.hub.tool.AiHubToolInvocationContext;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ClusterElementToolCallbackTest {

    private static final String TOOL_NAME = "slack_sendMessage";
    private static final String DESCRIPTION = "Send a message to a Slack channel";
    private static final String INPUT_SCHEMA = "{\"type\":\"object\",\"properties\":{}}";
    private static final String COMPONENT_NAME = "slack";
    private static final int COMPONENT_VERSION = 1;
    private static final String CLUSTER_ELEMENT_NAME = "sendMessage";

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionEchoesConstructorArgs() {
        ClusterElementToolCallback callback = newCallback(
            mock(ClusterElementDefinitionService.class), mock(ConnectionService.class));

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo(TOOL_NAME);
        assertThat(definition.description()).isEqualTo(DESCRIPTION);
        assertThat(definition.inputSchema()).isEqualTo(INPUT_SCHEMA);
    }

    @Test
    void testCallReturnsConnectionRequiredEnvelopeWhenNoConnection() throws Exception {
        // The "no connection in workspace" path is the most common failure mode for a freshly-discovered tool. It
        // must produce a structured envelope (not a tool-error string and not a generic exception) so the chat
        // surface can render the existing connect-button UX.
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);
        ConnectionService connectionService = mock(ConnectionService.class);

        when(connectionService.getConnections(eq(COMPONENT_NAME), isNull(), isNull(), eq(0L),
            eq(PlatformType.AUTOMATION)))
                .thenReturn(List.of());

        ClusterElementToolCallback callback = newCallback(clusterElementDefinitionService, connectionService);

        ToolContext toolContext = newWorkspaceContext(1L, 0L);

        String result = callback.call("{}", toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("connectionRequired")
            .asBoolean()).isTrue();
        assertThat(node.get("componentName")
            .asText()).isEqualTo(COMPONENT_NAME);
        assertThat(node.get("componentVersion")
            .asInt()).isEqualTo(COMPONENT_VERSION);

        // Critical: must NOT have called executeTool when no connection exists. A silent no-op call here would
        // bypass the connect-button UX and produce a confusing executor error downstream.
        verify(clusterElementDefinitionService, never()).executeTool(
            anyString(), anyInt(), anyString(), any(), any(), anyBoolean());
    }

    @Test
    void testCallReturnsConnectionAmbiguousEnvelopeOnMultipleMatches() throws Exception {
        // Multiple Slack workspaces connected. We must NOT silently pick the first — wrong target = wrong message
        // sent to wrong channel. The envelope lists the candidates so the LLM can ask the user which to use.
        ConnectionService connectionService = mock(ConnectionService.class);

        Connection connectionOne = mock(Connection.class);
        when(connectionOne.getId()).thenReturn(11L);
        when(connectionOne.getName()).thenReturn("Acme workspace");

        Connection connectionTwo = mock(Connection.class);
        when(connectionTwo.getId()).thenReturn(12L);
        when(connectionTwo.getName()).thenReturn("ByteChef workspace");

        when(connectionService.getConnections(
            eq(COMPONENT_NAME), isNull(), isNull(), eq(0L), eq(PlatformType.AUTOMATION)))
                .thenReturn(List.of(connectionOne, connectionTwo));

        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);

        ClusterElementToolCallback callback = newCallback(clusterElementDefinitionService, connectionService);

        String result = callback.call("{}", newWorkspaceContext(1L, 0L));

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("connectionAmbiguous")
            .asBoolean()).isTrue();
        assertThat(node.get("candidates")).hasSize(2);
        assertThat(node.get("candidates")
            .get(0)
            .get("id")
            .asLong()).isEqualTo(11L);
        assertThat(node.get("candidates")
            .get(1)
            .get("id")
            .asLong()).isEqualTo(12L);

        verify(clusterElementDefinitionService, never()).executeTool(
            anyString(), anyInt(), anyString(), any(), any(), anyBoolean());
    }

    @Test
    void testCallDispatchesToExecuteToolWhenConnectionResolves() throws Exception {
        // Happy path: one matching connection → dispatch through executeTool with parameters parsed from LLM JSON.
        // Pin the contract that LLM-supplied JSON args are passed AS-IS (not coerced or filtered) so the cluster
        // element executor can do its own type coercion downstream.
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);
        ConnectionService connectionService = mock(ConnectionService.class);

        Connection connection = mock(Connection.class);
        when(connection.getId()).thenReturn(99L);
        when(connection.getComponentName()).thenReturn(COMPONENT_NAME);
        when(connection.getConnectionVersion()).thenReturn(1);
        // Mockito generic-inference can't bridge Map<String, ?> across when()/thenReturn() captures; use doReturn()
        // which takes Object and sidesteps the wildcard capture mismatch.
        doReturn(Map.of("token", "xoxb-test")).when(connection)
            .getParameters();
        when(connection.getAuthorizationType()).thenReturn(AuthorizationType.BEARER_TOKEN);

        when(connectionService.getConnections(
            eq(COMPONENT_NAME), isNull(), isNull(), eq(0L), eq(PlatformType.AUTOMATION)))
                .thenReturn(List.of(connection));

        Object executeResult = Map.of("ok", true, "ts", "1701234567.123456");

        when(clusterElementDefinitionService.executeTool(
            eq(COMPONENT_NAME), eq(COMPONENT_VERSION), eq(CLUSTER_ELEMENT_NAME), any(), any(), eq(false)))
                .thenReturn(executeResult);

        ClusterElementToolCallback callback = newCallback(clusterElementDefinitionService, connectionService);

        String result = callback.call(
            "{\"channel\":\"#engineering\",\"text\":\"hello\"}", newWorkspaceContext(1L, 0L));

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("ok")
            .asBoolean()).isTrue();
        assertThat(node.get("ts")
            .asText()).isEqualTo("1701234567.123456");

        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<ComponentConnection> connectionCaptor = ArgumentCaptor.forClass(ComponentConnection.class);

        verify(clusterElementDefinitionService).executeTool(
            eq(COMPONENT_NAME), eq(COMPONENT_VERSION), eq(CLUSTER_ELEMENT_NAME),
            paramsCaptor.capture(), connectionCaptor.capture(), eq(false));

        assertThat(paramsCaptor.getValue()).containsEntry("channel", "#engineering");
        assertThat(paramsCaptor.getValue()).containsEntry("text", "hello");

        ComponentConnection passed = connectionCaptor.getValue();

        assertThat(passed.connectionId()).isEqualTo(99L);
        assertThat(passed.componentName()).isEqualTo(COMPONENT_NAME);
        assertThat(passed.authorizationType()).isEqualTo(AuthorizationType.BEARER_TOKEN);
    }

    @Test
    void testCallReturnsToolErrorWhenWorkspaceContextMissing() throws Exception {
        // Single-arg call() = ToolContext is null. Production path NPE'd before P3.2 fixes; this regression-pins
        // the structured-error response so a missing context never surfaces as a stack-trace tool result.
        ClusterElementToolCallback callback = newCallback(
            mock(ClusterElementDefinitionService.class), mock(ConnectionService.class));

        String result = callback.call("{}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("Workspace context unavailable");
    }

    @Test
    void testPinnedConnectionShortCircuitsWorkspaceLookup() throws Exception {
        // v2 task-attached tools pin the connection at attach time. The callback must use
        // getConnection(pinnedId) directly and skip the workspace-scoped lookup-and-disambiguate path
        // entirely — otherwise a workspace that grew a second connection of the same component after
        // attach would suddenly trigger the "ambiguous" envelope mid-task.
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);
        ConnectionService connectionService = mock(ConnectionService.class);

        Connection pinned = mock(Connection.class);
        when(pinned.getId()).thenReturn(42L);
        when(pinned.getComponentName()).thenReturn(COMPONENT_NAME);
        when(pinned.getConnectionVersion()).thenReturn(1);
        doReturn(Map.of("token", "xoxb-pinned")).when(pinned)
            .getParameters();
        when(pinned.getAuthorizationType()).thenReturn(AuthorizationType.BEARER_TOKEN);

        when(connectionService.getConnection(42L)).thenReturn(pinned);

        Object executeResult = Map.of("ok", true);

        when(clusterElementDefinitionService.executeTool(
            eq(COMPONENT_NAME), eq(COMPONENT_VERSION), eq(CLUSTER_ELEMENT_NAME), any(), any(), eq(false)))
                .thenReturn(executeResult);

        ClusterElementToolCallback callback = new ClusterElementToolCallback(
            TOOL_NAME, DESCRIPTION, INPUT_SCHEMA, COMPONENT_NAME, COMPONENT_VERSION, CLUSTER_ELEMENT_NAME,
            clusterElementDefinitionService, connectionService, 42L, Map.of());

        callback.call("{\"channel\":\"#engineering\"}", newWorkspaceContext(1L, 0L));

        verify(connectionService).getConnection(42L);
        // Critical: the workspace-scoped lookup must NOT be called when a pinned id is set. If it were, a
        // workspace with multiple Slack connections would still hit the ambiguity path despite the user
        // having already disambiguated at attach time.
        verify(connectionService, never()).getConnections(
            anyString(), any(), any(), any(Long.class), any(PlatformType.class));
    }

    @Test
    void testPinnedParametersMergeWithLlmArgsWithLlmTakingPrecedence() throws Exception {
        // v2 contract: pinned parameters are defaults, LLM-supplied keys override on conflict. Lets users
        // attach "always send to #engineering" and the assistant override per-call when the user says
        // "this time send to #urgent". Without LLM-takes-precedence the user's per-call intent gets ignored.
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);
        ConnectionService connectionService = mock(ConnectionService.class);

        Connection pinned = mock(Connection.class);
        when(pinned.getId()).thenReturn(42L);
        when(pinned.getComponentName()).thenReturn(COMPONENT_NAME);
        when(pinned.getConnectionVersion()).thenReturn(1);
        doReturn(Map.of()).when(pinned)
            .getParameters();
        when(pinned.getAuthorizationType()).thenReturn(AuthorizationType.BEARER_TOKEN);

        when(connectionService.getConnection(42L)).thenReturn(pinned);

        Object executeResult = Map.of("ok", true);

        when(clusterElementDefinitionService.executeTool(
            eq(COMPONENT_NAME), eq(COMPONENT_VERSION), eq(CLUSTER_ELEMENT_NAME), any(), any(), eq(false)))
                .thenReturn(executeResult);

        ClusterElementToolCallback callback = new ClusterElementToolCallback(
            TOOL_NAME, DESCRIPTION, INPUT_SCHEMA, COMPONENT_NAME, COMPONENT_VERSION, CLUSTER_ELEMENT_NAME,
            clusterElementDefinitionService, connectionService, 42L,
            Map.of("channel", "#engineering", "username", "bytechef-bot"));

        callback.call("{\"channel\":\"#urgent\"}", newWorkspaceContext(1L, 0L));

        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);

        verify(clusterElementDefinitionService).executeTool(
            eq(COMPONENT_NAME), eq(COMPONENT_VERSION), eq(CLUSTER_ELEMENT_NAME),
            paramsCaptor.capture(), any(), eq(false));

        Map<String, Object> sent = paramsCaptor.getValue();

        assertThat(sent).containsEntry("channel", "#urgent") // LLM override wins
            .containsEntry("username", "bytechef-bot"); // pinned default kept where LLM didn't override
    }

    @Test
    void testCallReturnsToolErrorOnInvalidJsonInput() throws Exception {
        ClusterElementToolCallback callback = newCallback(
            mock(ClusterElementDefinitionService.class), mock(ConnectionService.class));

        String result = callback.call("not-valid-json", newWorkspaceContext(1L, 0L));

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("Invalid tool input");
    }

    private ClusterElementToolCallback newCallback(
        ClusterElementDefinitionService clusterElementDefinitionService, ConnectionService connectionService) {

        return new ClusterElementToolCallback(
            TOOL_NAME, DESCRIPTION, INPUT_SCHEMA, COMPONENT_NAME, COMPONENT_VERSION, CLUSTER_ELEMENT_NAME,
            clusterElementDefinitionService, connectionService);
    }

    private static ToolContext newWorkspaceContext(long workspaceId, long environmentId) {
        return new ToolContext(
            new AiHubToolInvocationContext(workspaceId, 10L, (short) 0, "x", environmentId, "thread-1")
                .toToolContext());
    }
}

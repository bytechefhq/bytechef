/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.PropertyOptionsResolver;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.WorkspaceCopilotConnectionLister;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ClusterElementToolCallbackTest {

    private static final long WORKSPACE_ID = 7L;
    private static final long OTHER_WORKSPACE_ID = 8L;
    private static final long ENVIRONMENT_ID = 1L;
    private static final long USER_ID = 3L;

    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);
    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade =
        mock(ClusterElementDefinitionFacade.class);
    private final ConnectionDefinitionService connectionDefinitionService = mock(ConnectionDefinitionService.class);
    private final WorkspaceConnectionFacade workspaceConnectionFacade = mock(WorkspaceConnectionFacade.class);

    private ClusterElementToolCallback toolCallback;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        SecurityContextRehydrator securityContextRehydrator = mock(SecurityContextRehydrator.class);

        when(securityContextRehydrator.withUserSecurityContext(any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(1, Supplier.class)
                .get());

        toolCallback = new ClusterElementToolCallback(
            "slack_sendMessage", "Send a Slack message", "slack", 1, "sendMessage", clusterElementDefinitionService,
            clusterElementDefinitionFacade, connectionDefinitionService,
            new WorkspaceCopilotConnectionLister(
                workspaceConnectionFacade, new PropertyOptionsResolver(securityContextRehydrator)));
    }

    @Test
    void testRunsWithTheOnlyConnectionOfTheChatsWorkspaceAndEnvironment() {
        when(workspaceConnectionFacade.getConnections(WORKSPACE_ID, "slack", null, ENVIRONMENT_ID, null))
            .thenReturn(List.of(connection(11L, "Team Slack")));
        when(workspaceConnectionFacade.getConnections(eq(OTHER_WORKSPACE_ID), anyString(), any(), any(), any()))
            .thenReturn(List.of(connection(22L, "Other Slack")));
        when(clusterElementDefinitionFacade.executeTool(eq("slack"), eq(1), eq("sendMessage"), anyMap(), eq(11L)))
            .thenReturn(Map.of("ok", true));

        String result = toolCallback.call("{\"text\":\"hi\"}", toolContext(WORKSPACE_ID));

        assertThat(result).isEqualTo("{\"ok\":true}");
        verify(clusterElementDefinitionFacade).executeTool("slack", 1, "sendMessage", Map.of("text", "hi"), 11L);
        verify(clusterElementDefinitionFacade, never()).executeTool(
            anyString(), anyInt(), anyString(), anyMap(), eq(22L));
    }

    @Test
    void testAConnectionOfAnotherWorkspaceIsNeitherFoundNorUsed() {
        when(workspaceConnectionFacade.getConnections(WORKSPACE_ID, "slack", null, ENVIRONMENT_ID, null))
            .thenReturn(List.of());
        when(workspaceConnectionFacade.getConnections(eq(OTHER_WORKSPACE_ID), anyString(), any(), any(), any()))
            .thenReturn(List.of(connection(22L, "Other Slack")));

        String result = toolCallback.call("{}", toolContext(WORKSPACE_ID));

        assertThat(result).contains("\"connectionRequired\":true");
        verify(workspaceConnectionFacade, never()).getConnections(
            eq(OTHER_WORKSPACE_ID), anyString(), any(), any(), any());
        verifyNoInteractions(clusterElementDefinitionFacade);
    }

    @Test
    void testAPinnedConnectionThatWasDeletedAsksToReconnectInsteadOfUsingAnother() {
        when(workspaceConnectionFacade.getConnections(WORKSPACE_ID, "slack", null, ENVIRONMENT_ID, null))
            .thenReturn(List.of(connection(11L, "Team Slack")));

        String result = pinnedToolCallback(42L).call("{}", toolContext(WORKSPACE_ID));

        assertThat(result)
            .contains("\"connectionRequired\":true")
            .contains("reconnect");
        verifyNoInteractions(clusterElementDefinitionFacade);
    }

    @Test
    void testAPinnedConnectionThatStillExistsIsUsed() {
        when(workspaceConnectionFacade.getConnections(WORKSPACE_ID, "slack", null, ENVIRONMENT_ID, null))
            .thenReturn(List.of(connection(11L, "Team Slack"), connection(42L, "Pinned Slack")));
        when(clusterElementDefinitionFacade.executeTool(eq("slack"), eq(1), eq("sendMessage"), anyMap(), eq(42L)))
            .thenReturn(Map.of("ok", true));

        assertThat(pinnedToolCallback(42L).call("{}", toolContext(WORKSPACE_ID))).isEqualTo("{\"ok\":true}");
    }

    @Test
    void testTwoConnectionsAreReportedInsteadOfPickingOne() {
        when(workspaceConnectionFacade.getConnections(WORKSPACE_ID, "slack", null, ENVIRONMENT_ID, null))
            .thenReturn(List.of(connection(11L, "Team Slack"), connection(12L, "Ops Slack")));

        String result = toolCallback.call("{}", toolContext(WORKSPACE_ID));

        assertThat(result)
            .contains("\"connectionAmbiguous\":true")
            .contains("Team Slack")
            .contains("Ops Slack");
        verifyNoInteractions(clusterElementDefinitionFacade);
    }

    @Test
    void testAComponentWithoutAConnectionRunsWithoutLookingOneUp() {
        when(connectionDefinitionService.getConnectionDefinition("slack", 1))
            .thenThrow(new NoSuchElementException("no connection"));
        when(clusterElementDefinitionFacade.executeTool(eq("slack"), eq(1), eq("sendMessage"), anyMap(), isNull()))
            .thenReturn("done");

        assertThat(toolCallback.call("{}", toolContext(WORKSPACE_ID))).isEqualTo("\"done\"");
        verifyNoInteractions(workspaceConnectionFacade);
    }

    @Test
    void testACallWithoutAWorkspaceRunsNothing() {
        String result = toolCallback.call("{}", new ToolContext(Map.of()));

        assertThat(result).contains("\"error\"");
        verifyNoInteractions(workspaceConnectionFacade, clusterElementDefinitionFacade);
    }

    @Test
    void testInputSchemaIsGeneratedLazilyAndCached() {
        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getProperties()).thenReturn(List.of());
        when(clusterElementDefinitionService.getClusterElementDefinition("slack", 1, "sendMessage"))
            .thenReturn(clusterElementDefinition);

        verifyNoInteractions(clusterElementDefinitionService);

        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("slack_sendMessage");
        assertThat(toolCallback.getToolDefinition()
            .inputSchema()).isNotNull();

        verify(clusterElementDefinitionService, times(1)).getClusterElementDefinition("slack", 1, "sendMessage");
    }

    @Test
    void testInputSchemaDegradesToAnEmptyObjectWhenItCannotBeGenerated() {
        when(clusterElementDefinitionService.getClusterElementDefinition("slack", 1, "sendMessage"))
            .thenThrow(new IllegalStateException("malformed property tree"));

        assertThat(toolCallback.getToolDefinition()
            .inputSchema()).isEqualTo(ClusterElementToolCallback.EMPTY_INPUT_SCHEMA);
        assertThat(toolCallback.getToolDefinition()
            .inputSchema()).isEqualTo(ClusterElementToolCallback.EMPTY_INPUT_SCHEMA);

        verify(clusterElementDefinitionService, times(1)).getClusterElementDefinition("slack", 1, "sendMessage");
        verify(clusterElementDefinitionFacade, never()).executeTool(
            anyString(), anyInt(), anyString(), anyMap(), anyLong());
    }

    @SuppressWarnings("unchecked")
    private ClusterElementToolCallback pinnedToolCallback(long pinnedConnectionId) {
        SecurityContextRehydrator securityContextRehydrator = mock(SecurityContextRehydrator.class);

        when(securityContextRehydrator.withUserSecurityContext(any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(1, Supplier.class)
                .get());

        return new ClusterElementToolCallback(
            "slack_sendMessage", "Send a Slack message", "slack", 1, "sendMessage", clusterElementDefinitionService,
            clusterElementDefinitionFacade, connectionDefinitionService,
            new WorkspaceCopilotConnectionLister(
                workspaceConnectionFacade, new PropertyOptionsResolver(securityContextRehydrator)),
            pinnedConnectionId, Map.of(), false);
    }

    private static ToolContext toolContext(long workspaceId) {
        return new ToolContext(
            AgentToolInvocationContext.builder()
                .workspaceId(workspaceId)
                .userId(USER_ID)
                .environmentId(ENVIRONMENT_ID)
                .conversationId("thread-1")
                .build()
                .toToolContext());
    }

    private static ConnectionDTO connection(long id, String name) {
        return ConnectionDTO.builder()
            .id(id)
            .name(name)
            .active(true)
            .environmentId((int) ENVIRONMENT_ID)
            .status(ConnectionStatus.ACTIVE)
            .build();
    }
}

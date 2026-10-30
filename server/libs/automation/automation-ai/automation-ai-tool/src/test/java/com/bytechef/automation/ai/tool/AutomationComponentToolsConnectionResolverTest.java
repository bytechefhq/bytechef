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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
class AutomationComponentToolsConnectionResolverTest {

    private static final String COMPONENT_NAME = "testComponent";
    private static final int COMPONENT_VERSION = 1;
    private static final long WORKSPACE_ID = 7L;

    private final ConnectionService connectionService = mock(ConnectionService.class);
    private final WorkspaceConnectionService workspaceConnectionService = mock(WorkspaceConnectionService.class);
    private final AutomationComponentToolsConnectionResolver automationComponentToolsConnectionResolver =
        new AutomationComponentToolsConnectionResolver(connectionService, workspaceConnectionService);

    @Test
    void testResolvesNoConnectionWithoutAWorkspaceInTheToolContext() {
        assertThat(automationComponentToolsConnectionResolver.resolveConnectionId(
            COMPONENT_NAME, COMPONENT_VERSION, null)).isEmpty();
        assertThat(automationComponentToolsConnectionResolver.resolveConnectionId(
            COMPONENT_NAME, COMPONENT_VERSION, new ToolContext(Map.of("other", "value")))).isEmpty();

        verify(connectionService, never()).getConnections(anyString(), anyInt(), any(PlatformType.class));
    }

    @Test
    void testResolvesOnlyAConnectionOfTheToolContextWorkspace() {
        List<Connection> connections = List.of(connection(1L, 0), connection(2L, 0));

        when(connectionService.getConnections(COMPONENT_NAME, COMPONENT_VERSION, PlatformType.AUTOMATION))
            .thenReturn(connections);
        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
            .thenReturn(List.of(new WorkspaceConnection(2L, WORKSPACE_ID)));

        assertThat(automationComponentToolsConnectionResolver.resolveConnectionId(
            COMPONENT_NAME, COMPONENT_VERSION, toolContext(Map.of(
                AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID))))
                    .hasValue(2L);
    }

    @Test
    void testResolvesNoConnectionWhenOnlyAnotherWorkspaceHasOne() {
        List<Connection> connections = List.of(connection(1L, 0));

        when(connectionService.getConnections(COMPONENT_NAME, COMPONENT_VERSION, PlatformType.AUTOMATION))
            .thenReturn(connections);
        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID)).thenReturn(List.of());

        assertThat(automationComponentToolsConnectionResolver.resolveConnectionId(
            COMPONENT_NAME, COMPONENT_VERSION, toolContext(Map.of(
                AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID))))
                    .isEmpty();
    }

    @Test
    void testResolvesOnlyAConnectionOfTheToolContextEnvironment() {
        List<Connection> connections = List.of(connection(1L, 0), connection(2L, 2));

        when(connectionService.getConnections(COMPONENT_NAME, COMPONENT_VERSION, PlatformType.AUTOMATION))
            .thenReturn(connections);
        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
            .thenReturn(List.of(new WorkspaceConnection(1L, WORKSPACE_ID), new WorkspaceConnection(2L, WORKSPACE_ID)));

        assertThat(automationComponentToolsConnectionResolver.resolveConnectionId(
            COMPONENT_NAME, COMPONENT_VERSION, toolContext(Map.of(
                AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID,
                AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 2L))))
                    .hasValue(2L);
    }

    private static Connection connection(long id, int environmentId) {
        Connection connection = mock(Connection.class);

        when(connection.getId()).thenReturn(id);
        when(connection.getEnvironmentId()).thenReturn(environmentId);

        return connection;
    }

    private static ToolContext toolContext(Map<String, Object> context) {
        return new ToolContext(context);
    }
}

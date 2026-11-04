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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ConnectionToolsTest {

    private final WorkspaceConnectionFacade workspaceConnectionFacade = mock(WorkspaceConnectionFacade.class);
    private final ConnectionTools connectionTools = new ConnectionTools(workspaceConnectionFacade);

    @Test
    void testListsTheWorkspaceConnectionsOfTheContextEnvironment() {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .active(true)
            .componentName("slack")
            .connectionVersion(1)
            .environmentId(1)
            .id(5L)
            .name("Slack prod")
            .build();

        when(workspaceConnectionFacade.getConnections(7L, "slack", null, 1L, null)).thenReturn(List.of(connectionDTO));

        ToolContext toolContext = new ToolContext(
            Map.of(
                AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 7L,
                AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 1L));

        List<ConnectionTools.ConnectionInfo> connectionInfos = connectionTools.listConnections(
            "slack", toolContext);

        assertThat(connectionInfos).containsExactly(
            new ConnectionTools.ConnectionInfo(5L, "Slack prod", "slack", "STAGING", true));
    }

    @Test
    void testRequiresAWorkspaceInTheContext() {
        ToolContext toolContext = new ToolContext(Map.of());

        assertThatThrownBy(() -> connectionTools.listConnections(null, toolContext))
            .hasMessageContaining("workspaceId");

        verifyNoInteractions(workspaceConnectionFacade);
    }

    @Test
    void testWrappedToolRejectsAWorkspaceTheCallerIsNotAMemberOf() {
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        when(workspaceFacade.getMyWorkspaces()).thenReturn(List.of(new Workspace(1L, "Workspace 1")));

        ToolCallback[] toolCallbacks = ToolCallbacks.from(connectionTools);

        WorkspaceScopedFlatToolCallback toolCallback = new WorkspaceScopedFlatToolCallback(
            toolCallbacks[0], workspaceFacade);

        String result = toolCallback.call("{\"workspaceId\": 2}", new ToolContext(Map.of()));

        assertThat(result).contains("error")
            .contains("2");

        verifyNoInteractions(workspaceConnectionFacade);
    }

    @Test
    void testWrappedToolForwardsAMemberWorkspaceAndEnvironment() {
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        when(workspaceFacade.getMyWorkspaces()).thenReturn(List.of(new Workspace(1L, "Workspace 1")));

        ToolCallback[] toolCallbacks = ToolCallbacks.from(connectionTools);

        WorkspaceScopedFlatToolCallback toolCallback = new WorkspaceScopedFlatToolCallback(
            toolCallbacks[0], workspaceFacade);

        toolCallback.call("{\"workspaceId\": 1, \"environment\": \"PRODUCTION\"}", new ToolContext(Map.of()));

        verify(workspaceConnectionFacade).getConnections(1L, null, null, 2L, null);
    }
}

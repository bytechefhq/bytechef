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

package com.bytechef.automation.mcp.web.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.automation.mcp.web.graphql.WorkspaceMcpServerGraphQlController.CreateWorkspaceMcpServerInput;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.mcp.domain.McpServer;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link WorkspaceMcpServerGraphQlController}.
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceMcpServerGraphQlControllerTest {

    @Mock
    private WorkspaceMcpServerFacade workspaceMcpServerFacade;

    @InjectMocks
    private WorkspaceMcpServerGraphQlController workspaceMcpServerGraphQlController;

    @Test
    void testMcpServersByWorkspace() {
        // Given
        Long workspaceId = 1L;
        McpServer mcpServer1 = createMockMcpServer(100L, "Server 1");
        McpServer mcpServer2 = createMockMcpServer(200L, "Server 2");
        List<McpServer> expectedServers = Arrays.asList(mcpServer1, mcpServer2);

        when(workspaceMcpServerFacade.getWorkspaceMcpServers(workspaceId))
            .thenReturn(expectedServers);

        // When
        List<McpServer> result = workspaceMcpServerGraphQlController.workspaceMcpServers(workspaceId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Server 1", result.get(0)
            .getName());
        assertEquals("Server 2", result.get(1)
            .getName());
        assertEquals(expectedServers, result);

        verify(workspaceMcpServerFacade).getWorkspaceMcpServers(workspaceId);
    }

    @Test
    void testCreateMcpServerForWorkspace() {
        // Given
        String name = "Test Server";
        ModeType type = ModeType.AUTOMATION;
        long environmentId = Environment.DEVELOPMENT.ordinal();
        Boolean enabled = true;
        Long workspaceId = 1L;

        CreateWorkspaceMcpServerInput input = new CreateWorkspaceMcpServerInput(
            name, type, environmentId, enabled, workspaceId);

        McpServer createdServer = createMockMcpServer(100L, name);

        when(workspaceMcpServerFacade
            .createWorkspaceMcpServer(name, type, Environment.DEVELOPMENT, enabled, workspaceId))
                .thenReturn(createdServer);

        // When
        McpServer result = workspaceMcpServerGraphQlController.createWorkspaceMcpServer(input);

        // Then
        assertNotNull(result);
        assertEquals(createdServer, result);
        assertEquals(name, result.getName());
        assertEquals(100L, result.getId());

        verify(workspaceMcpServerFacade).createWorkspaceMcpServer(
            name, type, Environment.DEVELOPMENT, enabled, workspaceId);
    }

    @Test
    void testCreateMcpServerForWorkspaceWithNullEnabled() {
        // Given
        String name = "Test Server";
        ModeType type = ModeType.AUTOMATION;
        long environmentId = Environment.PRODUCTION.ordinal();
        Boolean enabled = null;
        Long workspaceId = 2L;

        CreateWorkspaceMcpServerInput input = new CreateWorkspaceMcpServerInput(
            name, type, environmentId, enabled, workspaceId);

        McpServer createdServer = createMockMcpServer(200L, name);

        when(workspaceMcpServerFacade
            .createWorkspaceMcpServer(name, type, Environment.PRODUCTION, enabled, workspaceId))
                .thenReturn(createdServer);

        // When
        McpServer result = workspaceMcpServerGraphQlController.createWorkspaceMcpServer(input);

        // Then
        assertNotNull(result);
        assertEquals(createdServer, result);
        assertEquals(name, result.getName());
        assertEquals(200L, result.getId());

        verify(workspaceMcpServerFacade).createWorkspaceMcpServer(
            name, type, Environment.PRODUCTION, enabled, workspaceId);
    }

    @Test
    void testCreateMcpServerForWorkspaceWithEmbeddedType() {
        // Given
        String name = "Embedded Server";
        ModeType type = ModeType.EMBEDDED;
        long environmentId = Environment.DEVELOPMENT.ordinal();
        Boolean enabled = false;
        Long workspaceId = 3L;

        CreateWorkspaceMcpServerInput input = new CreateWorkspaceMcpServerInput(
            name, type, environmentId, enabled, workspaceId);

        McpServer createdServer = createMockMcpServer(300L, name);

        when(workspaceMcpServerFacade.createWorkspaceMcpServer(
            name, type, Environment.DEVELOPMENT, enabled, workspaceId))
                .thenReturn(createdServer);

        // When
        McpServer result = workspaceMcpServerGraphQlController.createWorkspaceMcpServer(input);

        // Then
        assertNotNull(result);
        assertEquals(createdServer, result);
        assertEquals(name, result.getName());

        verify(workspaceMcpServerFacade).createWorkspaceMcpServer(
            name, type, Environment.DEVELOPMENT, enabled, workspaceId);
    }

    @Test
    void testDeleteWorkspaceMcpServer() {
        // Given
        Long mcpServerId = 100L;

        // When
        boolean result = workspaceMcpServerGraphQlController.deleteWorkspaceMcpServer(mcpServerId);

        // Then
        assertTrue(result);
        verify(workspaceMcpServerFacade).deleteWorkspaceMcpServer(mcpServerId);
    }

    @Test
    void testCreateMcpServerForWorkspaceInputRecord() {
        // Given
        String name = "Test Server";
        ModeType type = ModeType.AUTOMATION;
        long environmentId = 0L;
        Boolean enabled = true;
        Long workspaceId = 1L;

        // When
        CreateWorkspaceMcpServerInput input = new CreateWorkspaceMcpServerInput(
            name, type, environmentId, enabled, workspaceId);

        // Then
        assertEquals(name, input.name());
        assertEquals(type, input.type());
        assertEquals(environmentId, input.environmentId());
        assertEquals(enabled, input.enabled());
        assertEquals(workspaceId, input.workspaceId());
    }

    private McpServer createMockMcpServer(Long id, String name) {
        McpServer mcpServer = new McpServer(name, ModeType.AUTOMATION, Environment.DEVELOPMENT);

        mcpServer.setId(id);

        return mcpServer;
    }
}

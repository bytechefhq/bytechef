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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.automation.mcp.web.graphql.config.AutomationMcpGraphQlConfigurationSharedMocks;
import com.bytechef.automation.mcp.web.graphql.config.AutomationMcpGraphQlTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests for {@link WorkspaceMcpServerGraphQlController}.
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationMcpGraphQlTestConfiguration.class,
    WorkspaceMcpServerGraphQlController.class
})
@GraphQlTest(
    controllers = WorkspaceMcpServerGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath:graphql/"
    })
@AutomationMcpGraphQlConfigurationSharedMocks
class WorkspaceMcpServerGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private WorkspaceMcpServerFacade workspaceMcpServerFacade;

    @Test
    void testGetWorkspaceMcpServers() {
        // Given
        Long workspaceId = 1L;
        List<McpServer> expectedServers = List.of(
            createMockMcpServer(100L, "Server 1"),
            createMockMcpServer(200L, "Server 2"));

        when(workspaceMcpServerFacade.getWorkspaceMcpServers(workspaceId)).thenReturn(expectedServers);

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    workspaceMcpServers(workspaceId: "1") {
                        id
                        name
                        type
                        enabled
                    }
                }
                """)
            .execute()
            .path("workspaceMcpServers")
            .entityList(Object.class)
            .hasSize(2);

        verify(workspaceMcpServerFacade).getWorkspaceMcpServers(workspaceId);
    }

    @Test
    void testCreateWorkspaceMcpServer() {
        // Given
        String name = "Test Server";
        PlatformType type = PlatformType.AUTOMATION;
        Environment environment = Environment.DEVELOPMENT;
        Boolean enabled = true;
        Long workspaceId = 1L;

        McpServer createdServer = createMockMcpServer(100L, name);

        when(workspaceMcpServerFacade.createWorkspaceMcpServer(name, type, environment, enabled, workspaceId))
            .thenReturn(createdServer);

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    createWorkspaceMcpServer(input: {
                        name: "Test Server",
                        type: AUTOMATION,
                        environmentId: "0",
                        enabled: true,
                        workspaceId: "1"
                    }) {
                        id
                        name
                        type
                        enabled
                    }
                }
                """)
            .execute()
            .path("createWorkspaceMcpServer.id")
            .entity(String.class)
            .isEqualTo("100")
            .path("createWorkspaceMcpServer.name")
            .entity(String.class)
            .isEqualTo("Test Server");

        verify(workspaceMcpServerFacade).createWorkspaceMcpServer(name, type, environment, enabled, workspaceId);
    }

    @Test
    void testCreateWorkspaceMcpServerWithNullEnabled() {
        // Given
        McpServer createdServer = createMockMcpServer(200L, "Test Server");

        when(workspaceMcpServerFacade.createWorkspaceMcpServer(
            anyString(), any(PlatformType.class), any(Environment.class), isNull(), anyLong()))
                .thenReturn(createdServer);

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    createWorkspaceMcpServer(input: {
                        name: "Test Server",
                        type: AUTOMATION,
                        environmentId: "1",
                        workspaceId: "2"
                    }) {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("createWorkspaceMcpServer.id")
            .entity(String.class)
            .isEqualTo("200")
            .path("createWorkspaceMcpServer.name")
            .entity(String.class)
            .isEqualTo("Test Server");

        verify(workspaceMcpServerFacade).createWorkspaceMcpServer(
            anyString(), any(PlatformType.class), any(Environment.class), isNull(), anyLong());
    }

    @Test
    void testDeleteWorkspaceMcpServer() {
        // Given
        Long mcpServerId = 100L;

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    deleteWorkspaceMcpServer(mcpServerId: "100")
                }
                """)
            .execute()
            .path("deleteWorkspaceMcpServer")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(workspaceMcpServerFacade).deleteWorkspaceMcpServer(mcpServerId);
    }

    private McpServer createMockMcpServer(Long id, String name) {
        McpServer mcpServer = new McpServer(name, PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        mcpServer.setId(id);

        return mcpServer;
    }
}

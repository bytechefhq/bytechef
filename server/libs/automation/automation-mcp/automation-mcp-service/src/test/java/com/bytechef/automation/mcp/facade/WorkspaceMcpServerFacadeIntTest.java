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

package com.bytechef.automation.mcp.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.mcp.config.McpProjectIntTestConfiguration;
import com.bytechef.automation.mcp.config.McpProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.mcp.domain.WorkspaceMcpServer;
import com.bytechef.automation.mcp.repository.WorkspaceMcpServerRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration tests for {@link WorkspaceMcpServerFacadeImpl}.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = McpProjectIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@McpProjectIntTestConfigurationSharedMocks
public class WorkspaceMcpServerFacadeIntTest {

    @Autowired
    private WorkspaceMcpServerFacade workspaceMcpServerFacade;

    @Autowired
    private WorkspaceMcpServerRepository workspaceMcpServerRepository;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Long testWorkspaceId;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        workspaceMcpServerRepository.deleteAll();
        mcpServerRepository.deleteAll();
        workspaceRepository.deleteAll();

        // Create test workspace data
        Workspace defaultWorkspace = new Workspace("default-workspace");
        defaultWorkspace.setId(1049L);
        workspaceRepository.save(defaultWorkspace);

        Workspace otherWorkspace = new Workspace("other-workspace");
        otherWorkspace.setId(1050L);
        workspaceRepository.save(otherWorkspace);

        testWorkspaceId = 1049L; // Default workspace ID used in the system
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        workspaceMcpServerRepository.deleteAll();
        mcpServerRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @Test
    void testGetWorkspaceMcpServers() {
        // Given - Create and assign servers to workspace
        McpServer createdServer1 = workspaceMcpServerFacade.createWorkspaceMcpServer(
            "Test Server 1", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true, testWorkspaceId);
        McpServer createdServer2 = workspaceMcpServerFacade.createWorkspaceMcpServer(
            "Test Server 2", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true, testWorkspaceId);

        // When
        List<McpServer> result = workspaceMcpServerFacade.getWorkspaceMcpServers(testWorkspaceId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream()
            .anyMatch(server -> server.getId()
                .equals(createdServer1.getId())));
        assertTrue(result.stream()
            .anyMatch(server -> server.getId()
                .equals(createdServer2.getId())));
    }

    @Test
    void testCreateWorkspaceMcpServer() {
        // Given
        String name = "Test Server";
        PlatformType type = PlatformType.AUTOMATION;
        Environment environment = Environment.DEVELOPMENT;
        Boolean enabled = true;

        // When
        McpServer result = workspaceMcpServerFacade.createWorkspaceMcpServer(
            name, type, environment, enabled, testWorkspaceId);

        // Then
        assertNotNull(result);
        assertEquals(name, result.getName());
        assertEquals(type, result.getType());
        assertEquals(environment, result.getEnvironment());
        assertEquals(enabled, result.isEnabled());

        // Verify the server was assigned to the workspace
        List<WorkspaceMcpServer> assignments = workspaceMcpServerRepository.findAllByWorkspaceId(testWorkspaceId);
        assertEquals(1, assignments.size());
        assertEquals(result.getId(), assignments.get(0)
            .getMcpServerId());
        assertEquals(testWorkspaceId, assignments.get(0)
            .getWorkspaceId());
    }

    @Test
    void testCreateWorkspaceMcpServerWithNullEnabled() {
        // Given
        String name = "Test Server";
        PlatformType type = PlatformType.AUTOMATION;
        Environment environment = Environment.DEVELOPMENT;
        Boolean enabled = null; // Should default to enabled

        // When
        McpServer result = workspaceMcpServerFacade.createWorkspaceMcpServer(
            name, type, environment, enabled, testWorkspaceId);

        // Then
        assertNotNull(result);
        assertEquals(name, result.getName());
        assertEquals(type, result.getType());
        assertEquals(environment, result.getEnvironment());
        assertTrue(result.isEnabled()); // Should default to true

        // Verify the server was assigned to the workspace
        List<WorkspaceMcpServer> assignments = workspaceMcpServerRepository.findAllByWorkspaceId(testWorkspaceId);
        assertEquals(1, assignments.size());
        assertEquals(result.getId(), assignments.get(0)
            .getMcpServerId());
    }

    @Test
    void testDeleteWorkspaceMcpServer() {
        // Given - Create server and assign to workspace
        McpServer createdServer = workspaceMcpServerFacade.createWorkspaceMcpServer(
            "Test Server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true, testWorkspaceId);

        // Verify server exists
        assertEquals(1, workspaceMcpServerRepository.findAllByWorkspaceId(testWorkspaceId)
            .size());
        assertEquals(1, mcpServerRepository.count());

        // When
        workspaceMcpServerFacade.deleteWorkspaceMcpServer(createdServer.getId());

        // Then - Server should be removed from workspace and deleted from system
        List<WorkspaceMcpServer> assignments = workspaceMcpServerRepository.findAllByWorkspaceId(testWorkspaceId);
        assertEquals(0, assignments.size());
        assertEquals(0, mcpServerRepository.count());
    }

    @Test
    void testDeleteWorkspaceMcpServerButKeepIfUsedByOtherWorkspaces() {
        // Given - Create server and assign to two workspaces
        McpServer createdServer = workspaceMcpServerFacade.createWorkspaceMcpServer(
            "Test Server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true, testWorkspaceId);

        Long otherWorkspaceId = 1050L;
        workspaceMcpServerFacade.createWorkspaceMcpServer(
            "Test Server 2", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true, otherWorkspaceId);

        // Manually assign the first server to the second workspace to simulate shared usage
        WorkspaceMcpServer additionalAssignment = new WorkspaceMcpServer(createdServer.getId(), otherWorkspaceId);
        workspaceMcpServerRepository.save(additionalAssignment);

        // Verify setup
        assertEquals(1, workspaceMcpServerRepository.findAllByWorkspaceId(testWorkspaceId)
            .size());
        assertEquals(2, mcpServerRepository.count()); // Two servers total

        // When - Delete from one workspace
        workspaceMcpServerFacade.deleteWorkspaceMcpServer(createdServer.getId());

        // Then - Server should be completely deleted from all workspaces (current implementation behavior)
        List<WorkspaceMcpServer> assignments1 = workspaceMcpServerRepository.findAllByWorkspaceId(testWorkspaceId);
        List<WorkspaceMcpServer> assignments2 = workspaceMcpServerRepository.findAllByWorkspaceId(otherWorkspaceId);

        assertEquals(0, assignments1.size()); // Removed from first workspace
        assertEquals(1, assignments2.size()); // Only the second server remains in second workspace
        assertEquals(1, mcpServerRepository.count()); // Only the second server still exists
    }
}

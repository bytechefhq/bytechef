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

package com.bytechef.automation.mcp.service;

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
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration tests for {@link WorkspaceMcpServerServiceImpl}.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = McpProjectIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@McpProjectIntTestConfigurationSharedMocks
public class WorkspaceMcpServerServiceIntTest {

    @Autowired
    private McpServerService mcpServerService;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private WorkspaceMcpServerService workspaceMcpServerService;

    @Autowired
    private WorkspaceMcpServerRepository workspaceMcpServerRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private McpServer testMcpServer1;
    private McpServer testMcpServer2;
    private Long testWorkspaceId;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        workspaceMcpServerRepository.deleteAll();
        mcpServerRepository.deleteAll();
        workspaceRepository.deleteAll();

        workspaceRepository.save(new Workspace(Workspace.DEFAULT_WORKSPACE_ID, "Default workspace"));

        // Create test data
        testWorkspaceId = Workspace.DEFAULT_WORKSPACE_ID;
        testMcpServer1 =
            mcpServerService.create("Test Server 1", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true);
        testMcpServer2 =
            mcpServerService.create("Test Server 2", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true);
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        workspaceMcpServerRepository.deleteAll();
        mcpServerRepository.deleteAll();
    }

    @Test
    void testGetWorkspaceMcpServers() {
        // Given - Assign both servers to workspace
        workspaceMcpServerService.assignMcpServerToWorkspace(testMcpServer1.getId(), testWorkspaceId);
        workspaceMcpServerService.assignMcpServerToWorkspace(testMcpServer2.getId(), testWorkspaceId);

        // When
        List<WorkspaceMcpServer> result = workspaceMcpServerService.getWorkspaceMcpServers(testWorkspaceId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream()
            .anyMatch(workspaceServer -> workspaceServer.getMcpServerId()
                .equals(testMcpServer1.getId())));
        assertTrue(result.stream()
            .anyMatch(workspaceServer -> workspaceServer.getMcpServerId()
                .equals(testMcpServer2.getId())));
    }

    @Test
    void testAssignMcpServerToWorkspaceWhenNotExists() {
        // When
        workspaceMcpServerService.assignMcpServerToWorkspace(testMcpServer1.getId(), testWorkspaceId);

        // Then
        List<WorkspaceMcpServer> assignments = workspaceMcpServerRepository.findAllByWorkspaceId(testWorkspaceId);
        assertEquals(1, assignments.size());
        assertEquals(testMcpServer1.getId(), assignments.get(0)
            .getMcpServerId());
        assertEquals(testWorkspaceId, assignments.get(0)
            .getWorkspaceId());
    }

    @Test
    void testAssignMcpServerToWorkspaceWhenAlreadyExists() {
        // Given - Assign server first time
        workspaceMcpServerService.assignMcpServerToWorkspace(testMcpServer1.getId(), testWorkspaceId);

        // When - Assign same server again
        workspaceMcpServerService.assignMcpServerToWorkspace(testMcpServer1.getId(), testWorkspaceId);

        // Then - Should still have only one assignment
        List<WorkspaceMcpServer> assignments = workspaceMcpServerRepository.findAllByWorkspaceId(testWorkspaceId);
        assertEquals(1, assignments.size());
    }

    @Test
    void testRemoveMcpServerFromWorkspaceWhenExists() {
        // Given - Assign server to workspace first
        workspaceMcpServerService.assignMcpServerToWorkspace(testMcpServer1.getId(), testWorkspaceId);
        assertEquals(1, workspaceMcpServerRepository.findAllByWorkspaceId(testWorkspaceId)
            .size());

        // When
        workspaceMcpServerService.removeMcpServerFromWorkspace(testMcpServer1.getId());

        // Then
        List<WorkspaceMcpServer> assignments = workspaceMcpServerRepository.findAllByWorkspaceId(testWorkspaceId);
        assertEquals(0, assignments.size());
    }

    @Test
    void testRemoveMcpServerFromWorkspaceWhenNotExists() {
        // When - Try to remove non-existing assignment
        workspaceMcpServerService.removeMcpServerFromWorkspace(testMcpServer1.getId());

        // Then - Should not affect anything
        List<WorkspaceMcpServer> assignments = workspaceMcpServerRepository.findAllByWorkspaceId(testWorkspaceId);
        assertEquals(0, assignments.size());
    }

}

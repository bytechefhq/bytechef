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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.mcp.domain.McpProject;
import com.bytechef.automation.mcp.facade.McpProjectFacade;
import com.bytechef.automation.mcp.service.McpProjectService;
import com.bytechef.automation.mcp.service.McpProjectWorkflowService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit est for {@link McpProjectGraphQlController}. This test focuses on testing the GraphQL layer with mocked service
 * dependencies.
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
public class McpProjectGraphQlControllerTest {

    @Mock
    private McpProjectFacade mcpProjectFacade;

    @Mock
    private McpProjectService mcpProjectService;

    @Mock
    private McpProjectWorkflowService mcpProjectWorkflowService;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private McpProjectGraphQlController mcpProjectGraphQlController;

    @Test
    void testGetMcpProjectById() {
        // Given
        Long projectId = 1L;
        McpProject mockProject = createMockMcpProject(projectId, 123L, 1L);
        when(mcpProjectService.fetchMcpProject(projectId)).thenReturn(Optional.of(mockProject));

        // When
        McpProject result = mcpProjectGraphQlController.mcpProject(projectId);

        // Then
        assertNotNull(result);
        assertEquals(projectId, result.getId());
        assertEquals(123L, result.getProjectDeploymentId());
        assertEquals(1L, result.getMcpServerId());
        verify(mcpProjectService).fetchMcpProject(projectId);
    }

    @Test
    void testGetAllMcpProjects() {
        // Given
        List<McpProject> mockProjects = List.of(
            createMockMcpProject(1L, 123L, 1L),
            createMockMcpProject(2L, 456L, 2L));
        when(mcpProjectService.getMcpProjects()).thenReturn(mockProjects);

        // When
        List<McpProject> result = mcpProjectGraphQlController.mcpProjects();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(123L, result.get(0)
            .getProjectDeploymentId());
        assertEquals(456L, result.get(1)
            .getProjectDeploymentId());
        verify(mcpProjectService).getMcpProjects();
    }

    @Test
    void testGetMcpProjectsByServerId() {
        // Given
        Long serverId = 1L;
        List<McpProject> mockProjects = List.of(
            createMockMcpProject(1L, 123L, serverId),
            createMockMcpProject(2L, 456L, serverId));
        when(mcpProjectService.getMcpServerMcpProjects(serverId)).thenReturn(mockProjects);

        // When
        List<McpProject> result = mcpProjectGraphQlController.mcpProjectsByServerId(serverId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(serverId, result.get(0)
            .getMcpServerId());
        assertEquals(serverId, result.get(1)
            .getMcpServerId());
        verify(mcpProjectService).getMcpServerMcpProjects(serverId);
    }

    @Test
    void testCreateMcpProjectWithWorkflows() {
        // Given
        McpProjectGraphQlController.CreateMcpProjectInput input =
            new McpProjectGraphQlController.CreateMcpProjectInput(
                1L, 123L, 1, List.of("workflow1", "workflow2"));
        McpProject mockProject = createMockMcpProject(1L, 123L, 1L);
        when(mcpProjectFacade.createMcpProject(anyLong(), anyLong(), any(Integer.class), any()))
            .thenReturn(mockProject);

        // When
        McpProject result = mcpProjectGraphQlController.createMcpProject(input);

        // Then
        assertNotNull(result);
        assertEquals(123L, result.getProjectDeploymentId());
        assertEquals(1L, result.getMcpServerId());
        verify(mcpProjectFacade).createMcpProject(1L, 123L, 1, List.of("workflow1", "workflow2"));
    }

    @Test
    void testDeleteMcpProject() {
        // Given
        long projectId = 1L;

        // When
        boolean result = mcpProjectGraphQlController.deleteMcpProject(projectId);

        // Then
        assertTrue(result);
        verify(mcpProjectFacade).deleteMcpProject(projectId);
    }

    @Test
    void testGetProjectVersion() {
        // Given
        McpProject mockProject = createMockMcpProject(1L, 123L, 1L);
        ProjectDeployment mockDeployment = new ProjectDeployment();

        mockDeployment.setProjectVersion(2);

        when(projectDeploymentService.getProjectDeployment(123L)).thenReturn(mockDeployment);

        // When
        Integer result = mcpProjectGraphQlController.projectVersion(mockProject);

        // Then
        assertEquals(2, result);
        verify(projectDeploymentService).getProjectDeployment(123L);
    }

    private McpProject createMockMcpProject(Long id, Long projectDeploymentId, Long mcpServerId) {
        McpProject project = new McpProject(projectDeploymentId, mcpServerId);

        project.setId(id);
        project.setVersion(1);

        return project;
    }
}

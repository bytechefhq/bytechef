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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.mcp.domain.McpProject;
import com.bytechef.automation.mcp.facade.McpProjectFacade;
import com.bytechef.automation.mcp.service.McpProjectService;
import com.bytechef.automation.mcp.web.graphql.config.AutomationMcpGraphQlConfigurationSharedMocks;
import com.bytechef.automation.mcp.web.graphql.config.AutomationMcpGraphQlTestConfiguration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests for {@link McpProjectGraphQlController}.
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationMcpGraphQlTestConfiguration.class,
    McpProjectGraphQlController.class
})
@GraphQlTest(
    controllers = McpProjectGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath:graphql/"
    })
@AutomationMcpGraphQlConfigurationSharedMocks
class McpProjectGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private McpProjectFacade mcpProjectFacade;

    @Autowired
    private McpProjectService mcpProjectService;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Test
    void testGetMcpProjectById() {
        // Given
        Long projectId = 1L;
        McpProject mockProject = createMockMcpProject(projectId, 123L, 1L);

        when(mcpProjectService.fetchMcpProject(projectId)).thenReturn(Optional.of(mockProject));

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    mcpProject(id: "1") {
                        id
                        projectDeploymentId
                        mcpServerId
                    }
                }
                """)
            .execute()
            .path("mcpProject.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("mcpProject.projectDeploymentId")
            .entity(String.class)
            .isEqualTo("123")
            .path("mcpProject.mcpServerId")
            .entity(String.class)
            .isEqualTo("1");

        verify(mcpProjectService).fetchMcpProject(projectId);
    }

    @Test
    void testGetMcpProjectByIdNotFound() {
        // Given
        Long projectId = 1L;

        when(mcpProjectService.fetchMcpProject(projectId)).thenReturn(Optional.empty());

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    mcpProject(id: "1") {
                        id
                    }
                }
                """)
            .execute()
            .path("mcpProject")
            .valueIsNull();

        verify(mcpProjectService).fetchMcpProject(projectId);
    }

    @Test
    void testGetAllMcpProjects() {
        // Given
        List<McpProject> mockProjects = List.of(
            createMockMcpProject(1L, 123L, 1L),
            createMockMcpProject(2L, 456L, 2L));

        when(mcpProjectService.getMcpProjects()).thenReturn(mockProjects);

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    mcpProjects {
                        id
                        projectDeploymentId
                        mcpServerId
                    }
                }
                """)
            .execute()
            .path("mcpProjects")
            .entityList(Object.class)
            .hasSize(2);

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

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    mcpProjectsByServerId(mcpServerId: "1") {
                        id
                        projectDeploymentId
                        mcpServerId
                    }
                }
                """)
            .execute()
            .path("mcpProjectsByServerId")
            .entityList(Object.class)
            .hasSize(2);

        verify(mcpProjectService).getMcpServerMcpProjects(serverId);
    }

    @Test
    void testCreateMcpProject() {
        // Given
        McpProject mockProject = createMockMcpProject(1L, 123L, 1L);

        when(mcpProjectFacade.createMcpProject(anyLong(), anyLong(), anyInt(), any())).thenReturn(mockProject);

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    createMcpProject(input: {
                        mcpServerId: "1",
                        projectId: "123",
                        projectVersion: 1,
                        selectedWorkflowIds: ["workflow1", "workflow2"]
                    }) {
                        id
                        projectDeploymentId
                        mcpServerId
                    }
                }
                """)
            .execute()
            .path("createMcpProject.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("createMcpProject.mcpServerId")
            .entity(String.class)
            .isEqualTo("1");

        verify(mcpProjectFacade).createMcpProject(1L, 123L, 1, List.of("workflow1", "workflow2"));
    }

    @Test
    void testDeleteMcpProject() {
        // Given
        long projectId = 1L;

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    deleteMcpProject(id: "1")
                }
                """)
            .execute()
            .path("deleteMcpProject")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(mcpProjectFacade).deleteMcpProject(projectId);
    }

    @Test
    void testGetProjectVersion() {
        // Given
        Long projectId = 1L;
        McpProject mockProject = createMockMcpProject(projectId, 123L, 1L);
        ProjectDeployment mockDeployment = new ProjectDeployment();

        mockDeployment.setProjectVersion(2);

        when(mcpProjectService.fetchMcpProject(projectId)).thenReturn(Optional.of(mockProject));
        when(projectDeploymentService.getProjectDeployment(123L)).thenReturn(mockDeployment);

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    mcpProject(id: "1") {
                        id
                        projectVersion
                    }
                }
                """)
            .execute()
            .path("mcpProject.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("mcpProject.projectVersion")
            .entity(Integer.class)
            .isEqualTo(2);

        verify(projectDeploymentService).getProjectDeployment(123L);
    }

    private McpProject createMockMcpProject(Long id, Long projectDeploymentId, Long mcpServerId) {
        McpProject project = new McpProject(projectDeploymentId, mcpServerId);

        project.setId(id);
        project.setVersion(1);

        return project;
    }
}

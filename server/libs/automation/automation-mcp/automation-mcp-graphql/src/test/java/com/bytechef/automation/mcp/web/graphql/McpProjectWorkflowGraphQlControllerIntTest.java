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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.mcp.service.McpProjectWorkflowService;
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
 * Integration tests for {@link McpProjectWorkflowGraphQlController}.
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationMcpGraphQlTestConfiguration.class,
    McpProjectWorkflowGraphQlController.class
})
@GraphQlTest(
    controllers = McpProjectWorkflowGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath:graphql/"
    })
@AutomationMcpGraphQlConfigurationSharedMocks
class McpProjectWorkflowGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private McpProjectWorkflowService mcpProjectWorkflowService;

    @Test
    void testGetMcpProjectWorkflowById() {
        // Given
        Long workflowId = 1L;
        McpProjectWorkflow mockWorkflow = createMockMcpProjectWorkflow(workflowId, 1L, 123L);

        when(mcpProjectWorkflowService.fetchMcpProjectWorkflow(workflowId)).thenReturn(Optional.of(mockWorkflow));

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    mcpProjectWorkflow(id: "1") {
                        id
                        mcpProjectId
                        projectDeploymentWorkflowId
                    }
                }
                """)
            .execute()
            .path("mcpProjectWorkflow.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("mcpProjectWorkflow.mcpProjectId")
            .entity(Long.class)
            .isEqualTo(1L)
            .path("mcpProjectWorkflow.projectDeploymentWorkflowId")
            .entity(Long.class)
            .isEqualTo(123L);

        verify(mcpProjectWorkflowService).fetchMcpProjectWorkflow(workflowId);
    }

    @Test
    void testGetMcpProjectWorkflowByIdNotFound() {
        // Given
        long workflowId = 1L;

        when(mcpProjectWorkflowService.fetchMcpProjectWorkflow(workflowId)).thenReturn(Optional.empty());

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    mcpProjectWorkflow(id: "1") {
                        id
                    }
                }
                """)
            .execute()
            .path("mcpProjectWorkflow")
            .valueIsNull();

        verify(mcpProjectWorkflowService).fetchMcpProjectWorkflow(workflowId);
    }

    @Test
    void testGetAllMcpProjectWorkflows() {
        // Given
        List<McpProjectWorkflow> mockWorkflows = List.of(
            createMockMcpProjectWorkflow(1L, 1L, 123L),
            createMockMcpProjectWorkflow(2L, 2L, 456L));

        when(mcpProjectWorkflowService.getMcpProjectWorkflows()).thenReturn(mockWorkflows);

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    mcpProjectWorkflows {
                        id
                        mcpProjectId
                        projectDeploymentWorkflowId
                    }
                }
                """)
            .execute()
            .path("mcpProjectWorkflows")
            .entityList(Object.class)
            .hasSize(2);

        verify(mcpProjectWorkflowService).getMcpProjectWorkflows();
    }

    @Test
    void testGetMcpProjectWorkflowsByMcpProjectId() {
        // Given
        Long mcpProjectId = 1L;
        List<McpProjectWorkflow> mockWorkflows = List.of(
            createMockMcpProjectWorkflow(1L, mcpProjectId, 123L),
            createMockMcpProjectWorkflow(2L, mcpProjectId, 456L));

        when(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(mcpProjectId)).thenReturn(mockWorkflows);

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    mcpProjectWorkflowsByMcpProjectId(mcpProjectId: "1") {
                        id
                        mcpProjectId
                        projectDeploymentWorkflowId
                    }
                }
                """)
            .execute()
            .path("mcpProjectWorkflowsByMcpProjectId")
            .entityList(Object.class)
            .hasSize(2);

        verify(mcpProjectWorkflowService).getMcpProjectMcpProjectWorkflows(mcpProjectId);
    }

    @Test
    void testGetMcpProjectWorkflowsByProjectDeploymentWorkflowId() {
        // Given
        Long projectDeploymentWorkflowId = 123L;
        List<McpProjectWorkflow> mockWorkflows = List.of(
            createMockMcpProjectWorkflow(1L, 1L, projectDeploymentWorkflowId),
            createMockMcpProjectWorkflow(2L, 2L, projectDeploymentWorkflowId));

        when(mcpProjectWorkflowService.getProjectDeploymentWorkflowMcpProjectWorkflows(projectDeploymentWorkflowId))
            .thenReturn(mockWorkflows);

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    mcpProjectWorkflowsByProjectDeploymentWorkflowId(projectDeploymentWorkflowId: "123") {
                        id
                        mcpProjectId
                        projectDeploymentWorkflowId
                    }
                }
                """)
            .execute()
            .path("mcpProjectWorkflowsByProjectDeploymentWorkflowId")
            .entityList(Object.class)
            .hasSize(2);

        verify(mcpProjectWorkflowService).getProjectDeploymentWorkflowMcpProjectWorkflows(projectDeploymentWorkflowId);
    }

    @Test
    void testCreateMcpProjectWorkflow() {
        // Given
        McpProjectWorkflow mockWorkflow = createMockMcpProjectWorkflow(1L, 1L, 123L);

        when(mcpProjectWorkflowService.create(anyLong(), anyLong())).thenReturn(mockWorkflow);

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    createMcpProjectWorkflow(input: {
                        mcpProjectId: 1,
                        projectDeploymentWorkflowId: 123
                    }) {
                        id
                        mcpProjectId
                        projectDeploymentWorkflowId
                    }
                }
                """)
            .execute()
            .path("createMcpProjectWorkflow.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("createMcpProjectWorkflow.mcpProjectId")
            .entity(Long.class)
            .isEqualTo(1L);

        verify(mcpProjectWorkflowService).create(1L, 123L);
    }

    @Test
    void testUpdateMcpProjectWorkflow() {
        // Given
        Long workflowId = 1L;
        McpProjectWorkflow mockWorkflow = createMockMcpProjectWorkflow(workflowId, 2L, 456L);

        when(mcpProjectWorkflowService.update(anyLong(), anyLong(), anyLong())).thenReturn(mockWorkflow);

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    updateMcpProjectWorkflow(id: "1", input: {
                        mcpProjectId: 2,
                        projectDeploymentWorkflowId: 456
                    }) {
                        id
                        mcpProjectId
                        projectDeploymentWorkflowId
                    }
                }
                """)
            .execute()
            .path("updateMcpProjectWorkflow.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("updateMcpProjectWorkflow.mcpProjectId")
            .entity(Long.class)
            .isEqualTo(2L);

        verify(mcpProjectWorkflowService).update(workflowId, 2L, 456L);
    }

    @Test
    void testDeleteMcpProjectWorkflow() {
        // Given
        Long workflowId = 1L;

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    deleteMcpProjectWorkflow(id: "1")
                }
                """)
            .execute()
            .path("deleteMcpProjectWorkflow")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(mcpProjectWorkflowService).delete(workflowId);
    }

    private McpProjectWorkflow createMockMcpProjectWorkflow(
        Long id, Long mcpProjectId, Long projectDeploymentWorkflowId) {

        McpProjectWorkflow workflow = new McpProjectWorkflow(mcpProjectId, projectDeploymentWorkflowId);

        workflow.setId(id);
        workflow.setVersion(1);

        return workflow;
    }
}

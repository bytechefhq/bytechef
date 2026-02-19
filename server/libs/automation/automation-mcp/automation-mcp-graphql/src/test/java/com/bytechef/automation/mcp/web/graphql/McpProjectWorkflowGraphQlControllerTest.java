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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.mcp.service.McpProjectWorkflowService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test for {@link McpProjectWorkflowGraphQlController}. This test focuses on testing the GraphQL layer with mocked
 * service dependencies.
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
public class McpProjectWorkflowGraphQlControllerTest {

    @Mock
    private McpProjectWorkflowService mcpProjectWorkflowService;

    @InjectMocks
    private McpProjectWorkflowGraphQlController mcpProjectWorkflowGraphQlController;

    @Test
    void testGetMcpProjectWorkflowById() {
        // Given
        Long workflowId = 1L;
        McpProjectWorkflow mockWorkflow = createMockMcpProjectWorkflow(workflowId, 1L, 123L);

        when(mcpProjectWorkflowService.fetchMcpProjectWorkflow(workflowId)).thenReturn(Optional.of(mockWorkflow));

        // When
        McpProjectWorkflow result = mcpProjectWorkflowGraphQlController.mcpProjectWorkflow(workflowId);

        // Then
        assertNotNull(result);
        assertEquals(workflowId, result.getId());
        assertEquals(1L, result.getMcpProjectId());
        assertEquals(123L, result.getProjectDeploymentWorkflowId());
        verify(mcpProjectWorkflowService).fetchMcpProjectWorkflow(workflowId);
    }

    @Test
    void testGetMcpProjectWorkflowByIdNotFound() {
        // Given
        long workflowId = 1L;

        when(mcpProjectWorkflowService.fetchMcpProjectWorkflow(workflowId)).thenReturn(Optional.empty());

        // When
        McpProjectWorkflow result = mcpProjectWorkflowGraphQlController.mcpProjectWorkflow(workflowId);

        // Then
        assertNull(result);
        verify(mcpProjectWorkflowService).fetchMcpProjectWorkflow(workflowId);
    }

    @Test
    void testGetAllMcpProjectWorkflows() {
        // Given
        List<McpProjectWorkflow> mockWorkflows = List.of(
            createMockMcpProjectWorkflow(1L, 1L, 123L),
            createMockMcpProjectWorkflow(2L, 2L, 456L));

        when(mcpProjectWorkflowService.getMcpProjectWorkflows()).thenReturn(mockWorkflows);

        // When
        List<McpProjectWorkflow> result = mcpProjectWorkflowGraphQlController.mcpProjectWorkflows();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0)
            .getMcpProjectId());
        assertEquals(2L, result.get(1)
            .getMcpProjectId());
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

        // When
        List<McpProjectWorkflow> result =
            mcpProjectWorkflowGraphQlController.mcpProjectWorkflowsByMcpProjectId(mcpProjectId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(mcpProjectId, result.get(0)
            .getMcpProjectId());
        assertEquals(mcpProjectId, result.get(1)
            .getMcpProjectId());
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

        // When
        List<McpProjectWorkflow> result = mcpProjectWorkflowGraphQlController
            .mcpProjectWorkflowsByProjectDeploymentWorkflowId(projectDeploymentWorkflowId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(projectDeploymentWorkflowId, result.get(0)
            .getProjectDeploymentWorkflowId());
        assertEquals(projectDeploymentWorkflowId, result.get(1)
            .getProjectDeploymentWorkflowId());
        verify(mcpProjectWorkflowService).getProjectDeploymentWorkflowMcpProjectWorkflows(projectDeploymentWorkflowId);
    }

    @Test
    void testCreateMcpProjectWorkflow() {
        // Given
        Map<String, Object> input = Map.of(
            "mcpProjectId", "1",
            "projectDeploymentWorkflowId", "123");
        McpProjectWorkflow mockWorkflow = createMockMcpProjectWorkflow(1L, 1L, 123L);

        when(mcpProjectWorkflowService.create(anyLong(), anyLong())).thenReturn(mockWorkflow);

        // When
        McpProjectWorkflow result = mcpProjectWorkflowGraphQlController.createMcpProjectWorkflow(input);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getMcpProjectId());
        assertEquals(123L, result.getProjectDeploymentWorkflowId());
        verify(mcpProjectWorkflowService).create(1L, 123L);
    }

    @Test
    void testUpdateMcpProjectWorkflow() {
        // Given
        Long workflowId = 1L;
        Map<String, Object> input = Map.of(
            "mcpProjectId", "2",
            "projectDeploymentWorkflowId", "456");
        McpProjectWorkflow mockWorkflow = createMockMcpProjectWorkflow(workflowId, 2L, 456L);

        when(mcpProjectWorkflowService.update(anyLong(), anyLong(), anyLong())).thenReturn(mockWorkflow);

        // When
        McpProjectWorkflow result = mcpProjectWorkflowGraphQlController.updateMcpProjectWorkflow(workflowId, input);

        // Then
        assertNotNull(result);
        assertEquals(2L, result.getMcpProjectId());
        assertEquals(456L, result.getProjectDeploymentWorkflowId());
        verify(mcpProjectWorkflowService).update(workflowId, 2L, 456L);
    }

    @Test
    void testUpdateMcpProjectWorkflowPartialUpdate() {
        // Given
        Long workflowId = 1L;
        Map<String, Object> input = Map.of(
            "mcpProjectId", "2"
        // projectDeploymentWorkflowId is not provided
        );
        McpProjectWorkflow mockWorkflow = createMockMcpProjectWorkflow(workflowId, 2L, 123L);

        when(mcpProjectWorkflowService.update(eq(workflowId), eq(2L), eq(null))).thenReturn(mockWorkflow);

        // When
        McpProjectWorkflow result = mcpProjectWorkflowGraphQlController.updateMcpProjectWorkflow(workflowId, input);

        // Then
        assertNotNull(result);
        assertEquals(2L, result.getMcpProjectId());
        verify(mcpProjectWorkflowService).update(workflowId, 2L, null);
    }

    @Test
    void testDeleteMcpProjectWorkflow() {
        // Given
        Long workflowId = 1L;

        // When
        boolean result = mcpProjectWorkflowGraphQlController.deleteMcpProjectWorkflow(workflowId);

        // Then
        assertTrue(result);
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

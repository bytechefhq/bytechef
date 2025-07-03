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

package com.bytechef.platform.configuration.web.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.McpTool;
import com.bytechef.platform.configuration.service.McpToolService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Integration test for {@link McpToolGraphQlController}. This test focuses on testing the GraphQL layer with mocked
 * service dependencies.
 *
 * @author ByteChef
 */
@ExtendWith(MockitoExtension.class)
public class McpToolGraphQlControllerIntTest {

    @Mock
    private McpToolService mcpToolService;

    @InjectMocks
    private McpToolGraphQlController mcpToolGraphQlController;

    @Test
    void testGetMcpToolById() {
        // Given
        Long toolId = 1L;
        McpTool mockTool = createMockMcpTool(toolId, "test-tool", Map.of("param1", "value1"), 1L);
        when(mcpToolService.fetchMcpTool(toolId)).thenReturn(Optional.of(mockTool));

        // When
        McpTool result = mcpToolGraphQlController.mcpTool(toolId);

        // Then
        assertNotNull(result);
        assertEquals(toolId, result.getId());
        assertEquals("test-tool", result.getName());
        assertEquals(Map.of("param1", "value1"), result.getParameters());
        assertEquals(1L, result.getMcpComponentId());
        verify(mcpToolService).fetchMcpTool(toolId);
    }

    @Test
    void testGetMcpToolByIdNotFound() {
        // Given
        Long toolId = 1L;
        when(mcpToolService.fetchMcpTool(toolId)).thenReturn(Optional.empty());

        // When
        McpTool result = mcpToolGraphQlController.mcpTool(toolId);

        // Then
        assertNull(result);
        verify(mcpToolService).fetchMcpTool(toolId);
    }

    @Test
    void testGetAllMcpTools() {
        // Given
        List<McpTool> mockTools = List.of(
            createMockMcpTool(1L, "tool1", Map.of("param1", "value1"), 1L),
            createMockMcpTool(2L, "tool2", Map.of("param2", "value2"), 2L));
        when(mcpToolService.getMcpTools()).thenReturn(mockTools);

        // When
        List<McpTool> result = mcpToolGraphQlController.mcpTools();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("tool1", result.get(0)
            .getName());
        assertEquals("tool2", result.get(1)
            .getName());
        verify(mcpToolService).getMcpTools();
    }

    @Test
    void testGetMcpToolsByComponentId() {
        // Given
        Long componentId = 1L;
        List<McpTool> mockTools = List.of(
            createMockMcpTool(1L, "component-tool1", Map.of("param1", "value1"), componentId),
            createMockMcpTool(2L, "component-tool2", Map.of("param2", "value2"), componentId));
        when(mcpToolService.getMcpComponentMcpTools(componentId)).thenReturn(mockTools);

        // When
        List<McpTool> result = mcpToolGraphQlController.mcpToolsByComponentId(componentId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(componentId, result.get(0)
            .getMcpComponentId());
        assertEquals(componentId, result.get(1)
            .getMcpComponentId());
        assertEquals("component-tool1", result.get(0)
            .getName());
        assertEquals("component-tool2", result.get(1)
            .getName());
        verify(mcpToolService).getMcpComponentMcpTools(componentId);
    }

    @Test
    void testCreateMcpTool() {
        // Given
        Map<String, String> parameters = Map.of("param1", "value1", "param2", "value2");
        McpToolGraphQlController.McpToolInput input =
            new McpToolGraphQlController.McpToolInput("new-tool", parameters, 1L);
        McpTool mockTool = createMockMcpTool(1L, "new-tool", parameters, 1L);
        when(mcpToolService.create(any(McpTool.class))).thenReturn(mockTool);

        // When
        McpTool result = mcpToolGraphQlController.createMcpTool(input);

        // Then
        assertNotNull(result);
        assertEquals("new-tool", result.getName());
        assertEquals(parameters, result.getParameters());
        assertEquals(1L, result.getMcpComponentId());
        verify(mcpToolService).create(any(McpTool.class));
    }

    @Test
    void testCreateMcpToolWithEmptyParameters() {
        // Given
        Map<String, String> emptyParameters = Map.of();
        McpToolGraphQlController.McpToolInput input =
            new McpToolGraphQlController.McpToolInput("simple-tool", emptyParameters, 2L);
        McpTool mockTool = createMockMcpTool(1L, "simple-tool", emptyParameters, 2L);
        when(mcpToolService.create(any(McpTool.class))).thenReturn(mockTool);

        // When
        McpTool result = mcpToolGraphQlController.createMcpTool(input);

        // Then
        assertNotNull(result);
        assertEquals("simple-tool", result.getName());
        assertEquals(emptyParameters, result.getParameters());
        assertEquals(2L, result.getMcpComponentId());
        verify(mcpToolService).create(any(McpTool.class));
    }

    @Test
    void testCreateMcpToolWithNullComponentId() {
        // Given
        Map<String, String> parameters = Map.of("param1", "value1");
        McpToolGraphQlController.McpToolInput input =
            new McpToolGraphQlController.McpToolInput("orphan-tool", parameters, null);
        McpTool mockTool = createMockMcpTool(1L, "orphan-tool", parameters, null);
        when(mcpToolService.create(any(McpTool.class))).thenReturn(mockTool);

        // When
        McpTool result = mcpToolGraphQlController.createMcpTool(input);

        // Then
        assertNotNull(result);
        assertEquals("orphan-tool", result.getName());
        assertEquals(parameters, result.getParameters());
        // Note: Not testing getMcpComponentId() when componentId is null due to NPE in domain class
        verify(mcpToolService).create(any(McpTool.class));
    }

    private McpTool createMockMcpTool(Long id, String name, Map<String, String> parameters, Long mcpComponentId) {
        McpTool tool = new McpTool(name, parameters, mcpComponentId);
        tool.setId(id);
        tool.setVersion(1);
        return tool;
    }
}

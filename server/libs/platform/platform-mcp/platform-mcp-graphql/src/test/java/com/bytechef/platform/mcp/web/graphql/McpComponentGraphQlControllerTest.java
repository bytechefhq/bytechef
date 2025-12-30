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

package com.bytechef.platform.mcp.web.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpToolService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Integration test for {@link McpComponentGraphQlController}. This test focuses on testing the GraphQL layer with
 * mocked service dependencies.
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
public class McpComponentGraphQlControllerTest {

    @Mock
    private McpComponentService mcpComponentService;

    @Mock
    private McpServerFacade mcpServerFacade;

    @Mock
    private McpToolService mcpToolService;

    @InjectMocks
    private McpComponentGraphQlController mcpComponentGraphQlController;

    @Test
    void testGetMcpComponentById() {
        // Given
        Long componentId = 1L;

        McpComponent mockComponent = createMockMcpComponent(componentId, "test-component", 1);

        when(mcpComponentService.getMcpComponent(componentId)).thenReturn(mockComponent);

        // When
        McpComponent result = mcpComponentGraphQlController.mcpComponent(componentId);

        // Then
        assertNotNull(result);
        assertEquals(componentId, result.getId());
        assertEquals("test-component", result.getComponentName());
        assertEquals(1, result.getComponentVersion());
        verify(mcpComponentService).getMcpComponent(componentId);
    }

    @Test
    void testGetAllMcpComponents() {
        // Given
        List<McpComponent> mockComponents = List.of(
            createMockMcpComponent(1L, "component1", 1), createMockMcpComponent(2L, "component2", 2));

        when(mcpComponentService.getMcpComponents()).thenReturn(mockComponents);

        // When
        List<McpComponent> result = mcpComponentGraphQlController.mcpComponents();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("component1", result.get(0)
            .getComponentName());
        assertEquals("component2", result.get(1)
            .getComponentName());
        verify(mcpComponentService).getMcpComponents();
    }

    @Test
    void testGetMcpComponentsByServerId() {
        // Given
        long serverId = 1L;
        List<McpComponent> mockComponents = List.of(
            createMockMcpComponent(1L, "server-component1", 1), createMockMcpComponent(2L, "server-component2", 1));

        when(mcpComponentService.getMcpServerMcpComponents(serverId)).thenReturn(mockComponents);

        // When
        List<McpComponent> result = mcpComponentGraphQlController.mcpComponentsByServerId(serverId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("server-component1", result.get(0)
            .getComponentName());
        assertEquals("server-component2", result.get(1)
            .getComponentName());
        verify(mcpComponentService).getMcpServerMcpComponents(serverId);
    }

    @Test
    void testCreateMcpComponent() {
        // Given
        McpComponentGraphQlController.McpComponentInput input = new McpComponentGraphQlController.McpComponentInput(
            "new-component", 1, 1L, 1L);
        McpComponent mockComponent = createMockMcpComponent(1L, "new-component", 1);

        when(mcpComponentService.create(org.mockito.ArgumentMatchers.any(McpComponent.class)))
            .thenReturn(mockComponent);

        // When
        McpComponent result = mcpComponentGraphQlController.createMcpComponent(input);

        // Then
        assertNotNull(result);
        assertEquals("new-component", result.getComponentName());
        assertEquals(1, result.getComponentVersion());
        verify(mcpComponentService).create(org.mockito.ArgumentMatchers.any(McpComponent.class));
    }

    @Test
    void testDeleteMcpComponent() {
        // Given
        long componentId = 1L;

        // When
        boolean result = mcpComponentGraphQlController.deleteMcpComponent(componentId);

        // Then
        assertTrue(result);
        verify(mcpServerFacade).deleteMcpComponent(componentId);
    }

    @Test
    void testGetConnectionId() {
        // Given
        McpComponent mockComponent = createMockMcpComponent(1L, "test-component", 1);

        // When
        Long result = mcpComponentGraphQlController.connectionId(mockComponent);

        // Then
        assertEquals(mockComponent.getConnectionId(), result);
    }

    private McpComponent createMockMcpComponent(Long id, String componentName, int componentVersion) {
        McpComponent component = new McpComponent(componentName, componentVersion, 1L, 1L);

        component.setId(id);
        component.setVersion(1);

        return component;
    }
}

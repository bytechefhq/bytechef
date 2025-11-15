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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
public class McpServerGraphQlControllerTest {

    @Mock
    private McpServerFacade mcpServerFacade;

    @Mock
    private McpServerService mcpServerService;

    @Mock
    private ApplicationProperties applicationProperties;

    private McpServerGraphQlController mcpServerGraphQlController;

    @BeforeEach
    void setUp() {
        // McpServerGraphQlController constructor reads publicUrl from ApplicationProperties
        when(applicationProperties.getPublicUrl()).thenReturn("http://localhost:8080");

        // Manually construct controller after stubbing so the stub is used during construction
        mcpServerGraphQlController = new McpServerGraphQlController(
            applicationProperties, mcpServerFacade, mcpServerService);
    }

    @Test
    void testGetMcpServerById() {
        // Given
        Long serverId = 1L;
        McpServer mockServer = createMockMcpServer(
            serverId, "Test Server", ModeType.AUTOMATION, Environment.DEVELOPMENT, true);

        when(mcpServerService.getMcpServer(serverId)).thenReturn(mockServer);

        // When
        McpServer result = mcpServerGraphQlController.mcpServer(serverId);

        // Then
        assertNotNull(result);
        assertEquals(serverId, result.getId());
        assertEquals("Test Server", result.getName());
        assertEquals(ModeType.AUTOMATION, result.getType());
        assertEquals(Environment.DEVELOPMENT, result.getEnvironment());
        assertTrue(result.isEnabled());
        verify(mcpServerService).getMcpServer(serverId);
    }

    @Test
    void testGetMcpServers() {
        // Given
        List<McpServer> mockServers = List.of(
            createMockMcpServer(1L, "Server 1", ModeType.AUTOMATION, Environment.DEVELOPMENT, true),
            createMockMcpServer(2L, "Server 2", ModeType.AUTOMATION, Environment.PRODUCTION, false));

        when(mcpServerService.getMcpServers(ModeType.AUTOMATION, McpServerService.McpServerOrderBy.NAME_ASC))
            .thenReturn(mockServers);

        // When
        List<McpServer> result =
            mcpServerGraphQlController.mcpServers(ModeType.AUTOMATION, McpServerService.McpServerOrderBy.NAME_ASC);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Server 1", result.get(0)
            .getName());
        assertEquals("Server 2", result.get(1)
            .getName());
        verify(mcpServerService).getMcpServers(ModeType.AUTOMATION, McpServerService.McpServerOrderBy.NAME_ASC);
    }

    @Test
    void testCreateMcpServer() {
        // Given
        McpServerGraphQlController.McpServerInput input = new McpServerGraphQlController.McpServerInput(
            "New Server", ModeType.AUTOMATION, Environment.DEVELOPMENT.ordinal(), true);
        McpServer mockServer = createMockMcpServer(
            1L, "New Server", ModeType.AUTOMATION, Environment.DEVELOPMENT, true);

        when(mcpServerService.create(anyString(), any(ModeType.class), any(Environment.class), any(Boolean.class)))
            .thenReturn(mockServer);

        // When
        McpServer result = mcpServerGraphQlController.createMcpServer(input);

        // Then
        assertNotNull(result);
        assertEquals("New Server", result.getName());
        assertEquals(ModeType.AUTOMATION, result.getType());
        assertEquals(Environment.DEVELOPMENT, result.getEnvironment());
        assertTrue(result.isEnabled());
        verify(mcpServerService).create("New Server", ModeType.AUTOMATION, Environment.DEVELOPMENT, true);
    }

    @Test
    void testUpdateMcpServer() {
        // Given
        long serverId = 1L;
        McpServerGraphQlController.McpServerUpdateInput input =
            new McpServerGraphQlController.McpServerUpdateInput("Updated Server", false);
        McpServer mockServer =
            createMockMcpServer(serverId, "Updated Server", ModeType.AUTOMATION, Environment.DEVELOPMENT, false);

        when(mcpServerService.update(anyLong(), anyString(), any(Boolean.class))).thenReturn(mockServer);

        // When
        McpServer result = mcpServerGraphQlController.updateMcpServer(serverId, input);

        // Then
        assertNotNull(result);
        assertEquals("Updated Server", result.getName());
        assertEquals(false, result.isEnabled());
        verify(mcpServerService).update(serverId, "Updated Server", false);
    }

    @Test
    void testUpdateMcpServerTags() {
        // Given
        Long serverId = 1L;
        List<McpServerGraphQlController.TagInput> tagInputs = List.of(
            new McpServerGraphQlController.TagInput(1L, "tag1"),
            new McpServerGraphQlController.TagInput(2L, "tag2"));
        List<Tag> mockTags = List.of(
            createMockTag(1L, "tag1"),
            createMockTag(2L, "tag2"));

        when(mcpServerFacade.updateMcpServerTags(anyLong(), any())).thenReturn(mockTags);

        // When
        List<Tag> result = mcpServerGraphQlController.updateMcpServerTags(serverId, tagInputs);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("tag1", result.get(0)
            .getName());
        assertEquals("tag2", result.get(1)
            .getName());
        verify(mcpServerFacade).updateMcpServerTags(eq(serverId), any());
    }

    @Test
    void testDeleteMcpServer() {
        // Given
        long serverId = 1L;

        // When
        boolean result = mcpServerGraphQlController.deleteMcpServer(serverId);

        // Then
        assertTrue(result);
        verify(mcpServerFacade).deleteMcpServer(serverId);
    }

    @Test
    void testGetMcpComponents() {
        // Given
        List<McpServer> mockServers = List.of(
            createMockMcpServer(1L, "Server 1", ModeType.AUTOMATION, Environment.DEVELOPMENT, true));
        List<McpComponent> mockComponents = List.of(
            createMockMcpComponent(1L, "component1", 1));
        Map<McpServer, List<McpComponent>> mockComponentsMap = Map.of(mockServers.get(0), mockComponents);

        when(mcpServerFacade.getMcpServerMcpComponents(mockServers)).thenReturn(mockComponentsMap);

        // When
        Map<McpServer, List<McpComponent>> result = mcpServerGraphQlController.mcpComponents(mockServers);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(mockServers.get(0)));
        assertEquals(1, result.get(mockServers.get(0))
            .size());
        verify(mcpServerFacade).getMcpServerMcpComponents(mockServers);
    }

    @Test
    void testGetTags() {
        // Given
        List<McpServer> mockServers = List.of(
            createMockMcpServer(1L, "Server 1", ModeType.AUTOMATION, Environment.DEVELOPMENT, true));
        List<Tag> mockTags = List.of(
            createMockTag(1L, "tag1"));
        Map<McpServer, List<Tag>> mockTagsMap = Map.of(mockServers.get(0), mockTags);

        when(mcpServerFacade.getMcpServerTags(mockServers)).thenReturn(mockTagsMap);

        // When
        Map<McpServer, List<Tag>> result = mcpServerGraphQlController.tags(mockServers);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(mockServers.get(0)));
        assertEquals(1, result.get(mockServers.get(0))
            .size());
        verify(mcpServerFacade).getMcpServerTags(mockServers);
    }

    private McpServer createMockMcpServer(
        Long id, String name, ModeType type, Environment environment, Boolean enabled) {

        McpServer server = new McpServer(name, type, environment, enabled);

        server.setId(id);
        server.setVersion(1);

        return server;
    }

    private McpComponent createMockMcpComponent(Long id, String componentName, int componentVersion) {
        McpComponent component = new McpComponent(componentName, componentVersion, 1L, null);

        component.setId(id);
        component.setVersion(1);

        return component;
    }

    private Tag createMockTag(Long id, String name) {
        Tag tag = new Tag();

        tag.setId(id);
        tag.setName(name);

        return tag;
    }
}

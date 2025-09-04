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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
public class McpServerTagGraphQlControllerTest {

    @Mock
    private McpServerService mcpServerService;

    @Mock
    private TagService tagService;

    @InjectMocks
    private McpServerTagGraphQlController mcpServerTagGraphQlController;

    @Test
    void testGetMcpServerTagsWithValidType() {
        // Given
        ModeType type = ModeType.AUTOMATION;

        List<McpServer> mockServers = List.of(
            createMockMcpServerWithTags(1L, "Server 1", type, List.of(1L, 2L)),
            createMockMcpServerWithTags(2L, "Server 2", type, List.of(2L, 3L)));
        List<Tag> mockTags = List.of(
            createMockTag(1L, "tag1"),
            createMockTag(2L, "tag2"),
            createMockTag(3L, "tag3"));

        when(mcpServerService.getMcpServers(type)).thenReturn(mockServers);
        when(tagService.getTags(any())).thenReturn(mockTags);

        // When
        List<Tag> result = mcpServerTagGraphQlController.mcpServerTags(type);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("tag1", result.get(0)
            .getName());
        assertEquals("tag2", result.get(1)
            .getName());
        assertEquals("tag3", result.get(2)
            .getName());
        verify(mcpServerService).getMcpServers(type);
        verify(tagService).getTags(any());
    }

    @Test
    void testGetMcpServerTagsWithNullType() {
        // Given
        ModeType type = null;

        // When
        List<Tag> result = mcpServerTagGraphQlController.mcpServerTags(type);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        // Verify that no service calls are made when type is null
    }

    @Test
    void testGetMcpServerTagsWithNoServers() {
        // Given
        ModeType type = ModeType.AUTOMATION;
        List<McpServer> emptyServerList = List.of();

        when(mcpServerService.getMcpServers(type)).thenReturn(emptyServerList);

        // When
        List<Tag> result = mcpServerTagGraphQlController.mcpServerTags(type);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mcpServerService).getMcpServers(type);
        // tagService.getTags should not be called when there are no tag IDs
    }

    @Test
    void testGetMcpServerTagsWithServersButNoTags() {
        // Given
        ModeType type = ModeType.AUTOMATION;
        List<McpServer> mockServers = List.of(
            createMockMcpServerWithTags(1L, "Server 1", type, List.of()),
            createMockMcpServerWithTags(2L, "Server 2", type, List.of()));

        when(mcpServerService.getMcpServers(type)).thenReturn(mockServers);

        // When
        List<Tag> result = mcpServerTagGraphQlController.mcpServerTags(type);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mcpServerService).getMcpServers(type);
        // tagService.getTags should not be called when there are no tag IDs
    }

    @Test
    void testGetMcpServerTagsWithDifferentModeTypes() {
        // Given
        ModeType requestedType = ModeType.AUTOMATION;
        List<McpServer> mockServers = List.of(
            createMockMcpServerWithTags(1L, "Server 1", ModeType.AUTOMATION, List.of(1L)),
            createMockMcpServerWithTags(2L, "Server 2", ModeType.EMBEDDED, List.of(2L)) // Different type
        );
        List<Tag> mockTags = List.of(
            createMockTag(1L, "automation-tag"));

        when(mcpServerService.getMcpServers(requestedType)).thenReturn(mockServers);
        when(tagService.getTags(any())).thenReturn(mockTags);

        // When
        List<Tag> result = mcpServerTagGraphQlController.mcpServerTags(requestedType);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("automation-tag", result.get(0)
            .getName());
        verify(mcpServerService).getMcpServers(requestedType);
        verify(tagService).getTags(any());
    }

    private McpServer createMockMcpServerWithTags(Long id, String name, ModeType type, List<Long> tagIds) {
        McpServer server = new McpServer(name, type, Environment.DEVELOPMENT, true);
        server.setId(id);
        server.setVersion(1);
        server.setTagIds(tagIds);

        return server;
    }

    private Tag createMockTag(Long id, String name) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(name);
        return tag;
    }
}

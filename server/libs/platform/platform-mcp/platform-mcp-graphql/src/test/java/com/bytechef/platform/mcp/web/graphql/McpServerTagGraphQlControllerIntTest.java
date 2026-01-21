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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.web.graphql.config.McpGraphQlConfigurationSharedMocks;
import com.bytechef.platform.mcp.web.graphql.config.McpGraphQlTestConfiguration;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    McpGraphQlTestConfiguration.class,
    McpServerTagGraphQlController.class
})
@GraphQlTest(
    controllers = McpServerTagGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@McpGraphQlConfigurationSharedMocks
public class McpServerTagGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private McpServerService mcpServerService;

    @Autowired
    private TagService tagService;

    @Test
    void testGetMcpServerTagsWithValidType() {
        // Given
        List<McpServer> mockServers = List.of(
            createMockMcpServerWithTags(1L, "Server 1", PlatformType.AUTOMATION, List.of(1L, 2L)),
            createMockMcpServerWithTags(2L, "Server 2", PlatformType.AUTOMATION, List.of(2L, 3L)));
        List<Tag> mockTags = List.of(
            createMockTag(1L, "tag1"),
            createMockTag(2L, "tag2"),
            createMockTag(3L, "tag3"));

        when(mcpServerService.getMcpServers(PlatformType.AUTOMATION)).thenReturn(mockServers);
        when(tagService.getTags(any())).thenReturn(mockTags);

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    mcpServerTags(type: AUTOMATION) {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("mcpServerTags")
            .entityList(Object.class)
            .hasSize(3);

        verify(mcpServerService).getMcpServers(PlatformType.AUTOMATION);
        verify(tagService).getTags(any());
    }

    @Test
    void testGetMcpServerTagsWithNoServers() {
        // Given
        when(mcpServerService.getMcpServers(PlatformType.AUTOMATION)).thenReturn(List.of());

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    mcpServerTags(type: AUTOMATION) {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("mcpServerTags")
            .entityList(Object.class)
            .hasSize(0);

        verify(mcpServerService).getMcpServers(PlatformType.AUTOMATION);
    }

    @Test
    void testGetMcpServerTagsWithServersButNoTags() {
        // Given
        List<McpServer> mockServers = List.of(
            createMockMcpServerWithTags(1L, "Server 1", PlatformType.AUTOMATION, List.of()),
            createMockMcpServerWithTags(2L, "Server 2", PlatformType.AUTOMATION, List.of()));

        when(mcpServerService.getMcpServers(PlatformType.AUTOMATION)).thenReturn(mockServers);

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    mcpServerTags(type: AUTOMATION) {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("mcpServerTags")
            .entityList(Object.class)
            .hasSize(0);

        verify(mcpServerService).getMcpServers(PlatformType.AUTOMATION);
    }

    private McpServer createMockMcpServerWithTags(Long id, String name, PlatformType type, List<Long> tagIds) {
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

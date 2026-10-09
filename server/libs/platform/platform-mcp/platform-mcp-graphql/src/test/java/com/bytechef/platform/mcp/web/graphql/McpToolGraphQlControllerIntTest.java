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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.mcp.web.graphql.config.McpGraphQlConfigurationSharedMocks;
import com.bytechef.platform.mcp.web.graphql.config.McpGraphQlTestConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
    McpToolGraphQlController.class
})
@GraphQlTest(
    controllers = McpToolGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@McpGraphQlConfigurationSharedMocks
class McpToolGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private McpComponentService mcpComponentService;

    @Autowired
    private McpServerService mcpServerService;

    @Autowired
    private McpToolService mcpToolService;

    @BeforeEach
    void beforeEach() {
        when(mcpComponentService.getMcpComponent(anyLong())).thenReturn(createMcpComponent(1L));
        when(mcpServerService.getMcpServer(anyLong())).thenReturn(createMcpServer(PlatformType.AUTOMATION));
    }

    @Test
    void testGetMcpToolById() {
        // Given
        McpTool mockTool = createMockMcpTool(1L, "test-tool", Map.of("param1", "value1"), 1L);

        when(mcpToolService.fetchMcpTool(1L)).thenReturn(Optional.of(mockTool));

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    mcpTool(id: "1") {
                        id
                        name
                        mcpComponentId
                    }
                }
                """)
            .execute()
            .path("mcpTool.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("mcpTool.name")
            .entity(String.class)
            .isEqualTo("test-tool")
            .path("mcpTool.mcpComponentId")
            .entity(String.class)
            .isEqualTo("1");
    }

    @Test
    void testGetMcpToolByIdNotFound() {
        // Given
        when(mcpToolService.fetchMcpTool(1L)).thenReturn(Optional.empty());

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    mcpTool(id: "1") {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("mcpTool")
            .valueIsNull();
    }

    @Test
    void testGetAllMcpTools() {
        // Given
        List<McpTool> mockTools = List.of(
            createMockMcpTool(1L, "tool1", Map.of("param1", "value1"), 1L),
            createMockMcpTool(2L, "tool2", Map.of("param2", "value2"), 2L));

        when(mcpComponentService.getMcpComponents()).thenReturn(
            List.of(createMcpComponent(1L, 1L), createMcpComponent(2L, 1L)));
        when(mcpToolService.getMcpTools()).thenReturn(mockTools);

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    mcpTools {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("mcpTools")
            .entityList(Object.class)
            .hasSize(2);
    }

    @Test
    void testGetMcpToolsByComponentId() {
        // Given
        List<McpTool> mockTools = List.of(
            createMockMcpTool(1L, "component-tool1", Map.of("param1", "value1"), 1L),
            createMockMcpTool(2L, "component-tool2", Map.of("param2", "value2"), 1L));

        when(mcpToolService.getMcpComponentMcpTools(1L)).thenReturn(mockTools);

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    mcpToolsByComponentId(mcpComponentId: "1") {
                        id
                        name
                        mcpComponentId
                    }
                }
                """)
            .execute()
            .path("mcpToolsByComponentId")
            .entityList(Object.class)
            .hasSize(2);
    }

    @Test
    void testCreateMcpTool() {
        // Given
        Map<String, String> parameters = Map.of("param1", "value1", "param2", "value2");
        McpTool mockTool = createMockMcpTool(1L, "new-tool", parameters, 1L);

        when(mcpToolService.create(any(McpTool.class))).thenReturn(mockTool);

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    createMcpTool(input: {
                        name: "new-tool",
                        parameters: { param1: "value1", param2: "value2" },
                        mcpComponentId: "1"
                    }) {
                        id
                        name
                        mcpComponentId
                    }
                }
                """)
            .execute()
            .path("createMcpTool.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("createMcpTool.name")
            .entity(String.class)
            .isEqualTo("new-tool");

        verify(mcpToolService).create(any(McpTool.class));
    }

    @Test
    void testCreateMcpToolWithEmptyParameters() {
        // Given
        McpTool mockTool = createMockMcpTool(1L, "simple-tool", Map.of(), 2L);

        when(mcpToolService.create(any(McpTool.class))).thenReturn(mockTool);

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    createMcpTool(input: {
                        name: "simple-tool",
                        mcpComponentId: "2"
                    }) {
                        id
                        name
                        mcpComponentId
                    }
                }
                """)
            .execute()
            .path("createMcpTool.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("createMcpTool.name")
            .entity(String.class)
            .isEqualTo("simple-tool");

        verify(mcpToolService).create(any(McpTool.class));
    }

    @Test
    void testUpdateMcpTool() {
        McpTool mcpTool = createMockMcpTool(1L, "test-tool", Map.of("param1", "value1"), 1L);

        when(mcpToolService.fetchMcpTool(1L)).thenReturn(Optional.of(mcpTool));
        when(mcpToolService.update(any(McpTool.class))).thenReturn(mcpTool);

        this.graphQlTester
            .document("""
                mutation {
                    updateMcpTool(id: "1", input: {name: "test-tool", mcpComponentId: "1", version: 1}) {
                        id
                    }
                }
                """)
            .execute()
            .path("updateMcpTool.id")
            .entity(String.class)
            .isEqualTo("1");

        verify(mcpToolService).update(any(McpTool.class));
    }

    @Test
    void testDeleteMcpTool() {
        McpTool mcpTool = createMockMcpTool(1L, "test-tool", Map.of(), 1L);

        when(mcpToolService.fetchMcpTool(1L)).thenReturn(Optional.of(mcpTool));

        this.graphQlTester
            .document("""
                mutation {
                    deleteMcpTool(id: "1")
                }
                """)
            .execute()
            .path("deleteMcpTool")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(mcpToolService).delete(mcpTool);
    }

    @Test
    void testUpdateMcpToolEnabledIsNotExposed() {
        this.graphQlTester
            .document("""
                mutation {
                    updateMcpToolEnabled(id: "1", enabled: false) {
                        id
                    }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).hasSize(1));

        verify(mcpToolService, never()).updateEnabled(anyLong(), anyBoolean());
    }

    @Nested
    class EmbeddedMcpServerTest {

        private static final long EMBEDDED_MCP_COMPONENT_ID = 2L;
        private static final long EMBEDDED_MCP_SERVER_ID = 2L;

        @BeforeEach
        void beforeEach() {
            McpComponent embeddedMcpComponent = createMcpComponent(EMBEDDED_MCP_SERVER_ID);

            when(mcpComponentService.getMcpComponent(EMBEDDED_MCP_COMPONENT_ID)).thenReturn(embeddedMcpComponent);
            when(mcpServerService.getMcpServer(EMBEDDED_MCP_SERVER_ID)).thenReturn(
                createMcpServer(PlatformType.EMBEDDED));
            when(mcpToolService.fetchMcpTool(2L)).thenReturn(
                Optional.of(createMockMcpTool(2L, "embedded-tool", Map.of(), EMBEDDED_MCP_COMPONENT_ID)));
            when(mcpToolService.fetchMcpTool(1L)).thenReturn(
                Optional.of(createMockMcpTool(1L, "automation-tool", Map.of(), 1L)));
        }

        @Test
        void testMcpToolRejectsEmbeddedMcpTool() {
            assertRejected("""
                query {
                    mcpTool(id: "2") {
                        id
                    }
                }
                """);
        }

        @Test
        void testMcpToolsByComponentIdRejectsEmbeddedMcpComponent() {
            assertRejected("""
                query {
                    mcpToolsByComponentId(mcpComponentId: "2") {
                        id
                    }
                }
                """);

            verify(mcpToolService, never()).getMcpComponentMcpTools(anyLong());
        }

        @Test
        void testMcpToolsOmitsEmbeddedMcpTools() {
            McpServer embeddedMcpServer = createMcpServer(PlatformType.EMBEDDED);

            embeddedMcpServer.setId(EMBEDDED_MCP_SERVER_ID);

            when(mcpServerService.getMcpServers(PlatformType.EMBEDDED)).thenReturn(List.of(embeddedMcpServer));
            when(mcpComponentService.getMcpComponents()).thenReturn(
                List.of(createMcpComponent(1L, 1L),
                    createMcpComponent(EMBEDDED_MCP_COMPONENT_ID, EMBEDDED_MCP_SERVER_ID)));
            when(mcpToolService.getMcpTools()).thenReturn(
                List.of(
                    createMockMcpTool(1L, "automation-tool", Map.of(), 1L),
                    createMockMcpTool(2L, "embedded-tool", Map.of(), EMBEDDED_MCP_COMPONENT_ID)));

            graphQlTester.document("""
                query {
                    mcpTools {
                        id
                    }
                }
                """)
                .execute()
                .path("mcpTools[*].id")
                .entityList(String.class)
                .containsExactly("1");
        }

        @Test
        void testCreateMcpToolRejectsEmbeddedMcpComponent() {
            assertRejected("""
                mutation {
                    createMcpTool(input: {name: "tool", mcpComponentId: "2"}) {
                        id
                    }
                }
                """);

            verify(mcpToolService, never()).create(any(McpTool.class));
        }

        @Test
        void testUpdateMcpToolRejectsEmbeddedMcpTool() {
            assertRejected("""
                mutation {
                    updateMcpTool(id: "2", input: {name: "embedded-tool", mcpComponentId: "2"}) {
                        id
                    }
                }
                """);

            verify(mcpToolService, never()).update(any(McpTool.class));
        }

        @Test
        void testUpdateMcpToolRejectsMoveIntoEmbeddedMcpComponent() {
            assertRejected("""
                mutation {
                    updateMcpTool(id: "1", input: {name: "automation-tool", mcpComponentId: "2"}) {
                        id
                    }
                }
                """);

            verify(mcpToolService, never()).update(any(McpTool.class));
        }

        @Test
        void testDeleteMcpToolRejectsEmbeddedMcpTool() {
            assertRejected("""
                mutation {
                    deleteMcpTool(id: "2")
                }
                """);

            verify(mcpToolService, never()).delete(any(McpTool.class));
        }

        private void assertRejected(String document) {
            graphQlTester.document(document)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors).hasSize(1));
        }
    }

    private static McpComponent createMcpComponent(long mcpServerId) {
        return new McpComponent("component", 1, mcpServerId, null);
    }

    private static McpComponent createMcpComponent(long id, long mcpServerId) {
        McpComponent mcpComponent = createMcpComponent(mcpServerId);

        mcpComponent.setId(id);

        return mcpComponent;
    }

    private static McpServer createMcpServer(PlatformType type) {
        return new McpServer("Server", type, Environment.DEVELOPMENT, true);
    }

    private McpTool createMockMcpTool(Long id, String name, Map<String, String> parameters, long mcpComponentId) {
        McpTool tool = new McpTool(name, parameters, mcpComponentId);

        tool.setId(id);
        tool.setMcpComponentId(mcpComponentId);
        tool.setVersion(1);

        return tool;
    }
}

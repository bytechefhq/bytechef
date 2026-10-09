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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.mcp.web.graphql.config.McpGraphQlConfigurationSharedMocks;
import com.bytechef.platform.mcp.web.graphql.config.McpGraphQlTestConfiguration;
import java.util.List;
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
    McpComponentGraphQlController.class
})
@GraphQlTest(
    controllers = McpComponentGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@McpGraphQlConfigurationSharedMocks
public class McpComponentGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private McpComponentService mcpComponentService;

    @Autowired
    private McpServerFacade mcpServerFacade;

    @Autowired
    private McpServerService mcpServerService;

    @Autowired
    private McpToolService mcpToolService;

    @BeforeEach
    void beforeEach() {
        when(mcpComponentService.getMcpComponent(anyLong())).thenReturn(createMockMcpComponent(1L, "component", 1));
        when(mcpServerService.getMcpServer(anyLong())).thenReturn(createMcpServer(PlatformType.AUTOMATION));
    }

    @Test
    void testGetMcpComponentById() {
        // Given
        McpComponent mockComponent = createMockMcpComponent(1L, "test-component", 1);

        when(mcpComponentService.getMcpComponent(1L)).thenReturn(mockComponent);

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    mcpComponent(id: "1") {
                        id
                        componentName
                        componentVersion
                    }
                }
                """)
            .execute()
            .path("mcpComponent.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("mcpComponent.componentName")
            .entity(String.class)
            .isEqualTo("test-component")
            .path("mcpComponent.componentVersion")
            .entity(Integer.class)
            .isEqualTo(1);
    }

    @Test
    void testGetAllMcpComponents() {
        // Given
        List<McpComponent> mockComponents = List.of(
            createMockMcpComponent(1L, "component1", 1),
            createMockMcpComponent(2L, "component2", 2));

        when(mcpComponentService.getMcpComponents()).thenReturn(mockComponents);

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    mcpComponents {
                        id
                        componentName
                        componentVersion
                    }
                }
                """)
            .execute()
            .path("mcpComponents")
            .entityList(Object.class)
            .hasSize(2);
    }

    @Test
    void testGetMcpComponentsByServerId() {
        // Given
        List<McpComponent> mockComponents = List.of(
            createMockMcpComponent(1L, "server-component1", 1),
            createMockMcpComponent(2L, "server-component2", 1));

        when(mcpComponentService.getMcpServerMcpComponents(1L)).thenReturn(mockComponents);

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    mcpComponentsByServerId(mcpServerId: "1") {
                        id
                        componentName
                        componentVersion
                    }
                }
                """)
            .execute()
            .path("mcpComponentsByServerId")
            .entityList(Object.class)
            .hasSize(2);
    }

    @Test
    void testCreateMcpComponent() {
        // Given
        McpComponent mockComponent = createMockMcpComponent(1L, "new-component", 1);

        when(mcpComponentService.create(any(McpComponent.class))).thenReturn(mockComponent);

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    createMcpComponent(input: {
                        componentName: "new-component",
                        componentVersion: 1,
                        mcpServerId: "1",
                        connectionId: "1"
                    }) {
                        id
                        componentName
                        componentVersion
                    }
                }
                """)
            .execute()
            .path("createMcpComponent.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("createMcpComponent.componentName")
            .entity(String.class)
            .isEqualTo("new-component");

        verify(mcpComponentService).create(any(McpComponent.class));
    }

    @Test
    void testDeleteMcpComponent() {
        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    deleteMcpComponent(id: "1")
                }
                """)
            .execute()
            .path("deleteMcpComponent")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(mcpServerFacade).deleteMcpComponent(1L);
    }

    @Test
    void testUpdateMcpComponentWithTools() {
        McpComponent mcpComponent = createMockMcpComponent(1L, "component", 1);

        when(mcpServerFacade.update(any(McpComponent.class), any())).thenReturn(mcpComponent);

        this.graphQlTester
            .document("""
                mutation {
                    updateMcpComponentWithTools(id: "1", input: {
                        componentName: "component", componentVersion: 1, mcpServerId: "1", tools: [], version: 1
                    }) {
                        id
                    }
                }
                """)
            .execute()
            .path("updateMcpComponentWithTools.id")
            .entity(String.class)
            .isEqualTo("1");

        verify(mcpServerFacade).update(any(McpComponent.class), any());
    }

    @Nested
    class EmbeddedMcpServerTest {

        private static final long EMBEDDED_MCP_SERVER_ID = 2L;

        @BeforeEach
        void beforeEach() {
            McpComponent embeddedMcpComponent = new McpComponent("component", 1, EMBEDDED_MCP_SERVER_ID, null);

            embeddedMcpComponent.setId(2L);

            when(mcpComponentService.getMcpComponent(2L)).thenReturn(embeddedMcpComponent);
            when(mcpServerService.getMcpServer(EMBEDDED_MCP_SERVER_ID)).thenReturn(
                createMcpServer(PlatformType.EMBEDDED));
        }

        @Test
        void testMcpComponentRejectsEmbeddedMcpComponent() {
            assertRejected("""
                query {
                    mcpComponent(id: "2") {
                        id
                    }
                }
                """);
        }

        @Test
        void testMcpComponentsByServerIdRejectsEmbeddedMcpServer() {
            assertRejected("""
                query {
                    mcpComponentsByServerId(mcpServerId: "2") {
                        id
                    }
                }
                """);

            verify(mcpComponentService, never()).getMcpServerMcpComponents(anyLong());
        }

        @Test
        void testMcpComponentsOmitsEmbeddedMcpComponentsAndTheirTools() {
            McpServer embeddedMcpServer = createMcpServer(PlatformType.EMBEDDED);

            embeddedMcpServer.setId(EMBEDDED_MCP_SERVER_ID);

            McpComponent embeddedMcpComponent = new McpComponent("component", 1, EMBEDDED_MCP_SERVER_ID, null);

            embeddedMcpComponent.setId(2L);

            when(mcpServerService.getMcpServers(PlatformType.EMBEDDED)).thenReturn(List.of(embeddedMcpServer));
            when(mcpComponentService.getMcpComponents()).thenReturn(
                List.of(createMockMcpComponent(1L, "automation-component", 1), embeddedMcpComponent));

            graphQlTester.document("""
                query {
                    mcpComponents {
                        id
                        mcpTools {
                            id
                        }
                    }
                }
                """)
                .execute()
                .path("mcpComponents[*].id")
                .entityList(String.class)
                .containsExactly("1");

            verify(mcpToolService, never()).getMcpComponentMcpTools(2L);
        }

        @Test
        void testCreateMcpComponentRejectsEmbeddedMcpServer() {
            assertRejected("""
                mutation {
                    createMcpComponent(input: {componentName: "component", componentVersion: 1, mcpServerId: "2"}) {
                        id
                    }
                }
                """);

            verify(mcpComponentService, never()).create(any(McpComponent.class));
        }

        @Test
        void testCreateMcpComponentWithToolsRejectsEmbeddedMcpServer() {
            assertRejected("""
                mutation {
                    createMcpComponentWithTools(input: {
                        componentName: "component", componentVersion: 1, mcpServerId: "2", tools: []
                    }) {
                        id
                    }
                }
                """);

            verifyNoInteractions(mcpServerFacade);
        }

        @Test
        void testUpdateMcpComponentWithToolsRejectsEmbeddedMcpComponent() {
            assertRejected("""
                mutation {
                    updateMcpComponentWithTools(id: "2", input: {
                        componentName: "component", componentVersion: 1, mcpServerId: "2", tools: [], version: 1
                    }) {
                        id
                    }
                }
                """);

            verifyNoInteractions(mcpServerFacade);
        }

        @Test
        void testUpdateMcpComponentWithToolsRejectsMoveIntoEmbeddedMcpServer() {
            assertRejected("""
                mutation {
                    updateMcpComponentWithTools(id: "1", input: {
                        componentName: "component", componentVersion: 1, mcpServerId: "2", tools: [], version: 1
                    }) {
                        id
                    }
                }
                """);

            verifyNoInteractions(mcpServerFacade);
        }

        @Test
        void testDeleteMcpComponentRejectsEmbeddedMcpComponent() {
            assertRejected("""
                mutation {
                    deleteMcpComponent(id: "2")
                }
                """);

            verifyNoInteractions(mcpServerFacade);
        }

        private void assertRejected(String document) {
            graphQlTester.document(document)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors).hasSize(1));
        }
    }

    private static McpServer createMcpServer(PlatformType type) {
        return new McpServer("Server", type, Environment.DEVELOPMENT, true);
    }

    private McpComponent createMockMcpComponent(Long id, String componentName, int componentVersion) {
        McpComponent component = new McpComponent(componentName, componentVersion, 1L, 1L);

        component.setId(id);
        component.setVersion(1);

        return component;
    }
}

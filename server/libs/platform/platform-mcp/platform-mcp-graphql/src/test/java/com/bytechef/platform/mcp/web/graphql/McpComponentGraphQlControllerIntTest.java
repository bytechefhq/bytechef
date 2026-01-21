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

import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.web.graphql.config.McpGraphQlConfigurationSharedMocks;
import com.bytechef.platform.mcp.web.graphql.config.McpGraphQlTestConfiguration;
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

    private McpComponent createMockMcpComponent(Long id, String componentName, int componentVersion) {
        McpComponent component = new McpComponent(componentName, componentVersion, 1L, 1L);

        component.setId(id);
        component.setVersion(1);

        return component;
    }
}

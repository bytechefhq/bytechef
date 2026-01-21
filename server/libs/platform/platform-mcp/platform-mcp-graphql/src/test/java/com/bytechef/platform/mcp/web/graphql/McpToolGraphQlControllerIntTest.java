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

import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.mcp.web.graphql.config.McpGraphQlConfigurationSharedMocks;
import com.bytechef.platform.mcp.web.graphql.config.McpGraphQlTestConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
public class McpToolGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private McpToolService mcpToolService;

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

    private McpTool createMockMcpTool(Long id, String name, Map<String, String> parameters, long mcpComponentId) {
        McpTool tool = new McpTool(name, parameters, mcpComponentId);

        tool.setId(id);
        tool.setMcpComponentId(mcpComponentId);
        tool.setVersion(1);

        return tool;
    }
}

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.mcp.web.graphql.config.McpGraphQlConfigurationSharedMocks;
import com.bytechef.platform.mcp.web.graphql.config.McpGraphQlTestConfiguration;
import graphql.ErrorType;
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
class McpToolGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private McpServerFacade mcpServerFacade;

    @Autowired
    private McpToolService mcpToolService;

    @Test
    void testGetMcpToolById() {
        // Given
        McpTool mockTool = createMockMcpTool(1L, "test-tool", Map.of("param1", "value1"), 1L);

        when(mcpServerFacade.fetchMcpTool(1L)).thenReturn(Optional.of(mockTool));

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
        when(mcpServerFacade.fetchMcpTool(1L)).thenReturn(Optional.empty());

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

        when(mcpServerFacade.getMcpTools()).thenReturn(mockTools);

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

        when(mcpServerFacade.getMcpComponentMcpTools(1L)).thenReturn(mockTools);

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

        when(mcpServerFacade.createMcpTool(any(McpTool.class))).thenReturn(mockTool);

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

        verify(mcpServerFacade).createMcpTool(any(McpTool.class));
    }

    @Test
    void testCreateMcpToolWithEmptyParameters() {
        // Given
        McpTool mockTool = createMockMcpTool(1L, "simple-tool", Map.of(), 2L);

        when(mcpServerFacade.createMcpTool(any(McpTool.class))).thenReturn(mockTool);

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

        verify(mcpServerFacade).createMcpTool(any(McpTool.class));
    }

    @Test
    void testUpdateMcpTool() {
        McpTool mcpTool = createMockMcpTool(1L, "test-tool", Map.of("param1", "value1"), 1L);

        when(mcpServerFacade.fetchMcpTool(1L)).thenReturn(Optional.of(mcpTool));
        when(mcpServerFacade.updateMcpTool(any(McpTool.class))).thenReturn(mcpTool);

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

        verify(mcpServerFacade).updateMcpTool(any(McpTool.class));
    }

    @Test
    void testDeleteMcpTool() {
        McpTool mcpTool = createMockMcpTool(1L, "test-tool", Map.of(), 1L);

        when(mcpServerFacade.fetchMcpTool(1L)).thenReturn(Optional.of(mcpTool));

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

        verify(mcpServerFacade).deleteMcpTool(mcpTool);
    }

    @Test
    void testUpdateMcpToolEnabledIsNotInSchema() {
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
            .satisfy(errors -> assertThat(errors)
                .anySatisfy(error -> {
                    assertThat(error.getErrorType()).isEqualTo(ErrorType.ValidationError);
                    assertThat(error.getMessage()).contains("updateMcpToolEnabled");
                }));

        verifyNoInteractions(mcpToolService);
    }

    private McpTool createMockMcpTool(Long id, String name, Map<String, String> parameters, long mcpComponentId) {
        McpTool tool = new McpTool(name, parameters, mcpComponentId);

        tool.setId(id);
        tool.setMcpComponentId(mcpComponentId);
        tool.setVersion(1);

        return tool;
    }
}

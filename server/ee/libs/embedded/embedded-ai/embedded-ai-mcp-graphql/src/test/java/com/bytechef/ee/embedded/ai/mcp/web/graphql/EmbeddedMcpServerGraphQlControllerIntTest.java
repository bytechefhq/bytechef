/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.ai.mcp.facade.EmbeddedMcpServerFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.test.config.graphql.GraphQLScalarTypes;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    EmbeddedMcpServerGraphQlControllerIntTest.ScalarConfiguration.class,
    EmbeddedMcpServerGraphQlController.class
})
@GraphQlTest(
    controllers = EmbeddedMcpServerGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
class EmbeddedMcpServerGraphQlControllerIntTest {

    @MockitoBean
    private EmbeddedMcpServerFacade embeddedMcpServerFacade;

    @Autowired
    private GraphQlTester graphQlTester;

    @Nested
    class McpServerTest {

        @Test
        void testCreateEmbeddedMcpServerWithoutEnabled() {
            when(embeddedMcpServerFacade.createEmbeddedMcpServer("Server", Environment.DEVELOPMENT, null))
                .thenReturn(createMcpServer("Server"));

            graphQlTester.document("""
                mutation {
                    createEmbeddedMcpServer(input: {name: "Server", environmentId: "0"}) {
                        id
                        name
                    }
                }
                """)
                .execute()
                .path("createEmbeddedMcpServer.name")
                .entity(String.class)
                .isEqualTo("Server");

            verify(embeddedMcpServerFacade).createEmbeddedMcpServer("Server", Environment.DEVELOPMENT, null);
        }

        @Test
        void testUpdateEmbeddedMcpServer() {
            when(embeddedMcpServerFacade.updateEmbeddedMcpServer(1L, "Renamed", false))
                .thenReturn(createMcpServer("Renamed"));

            graphQlTester.document("""
                mutation {
                    updateEmbeddedMcpServer(id: "1", input: {name: "Renamed", enabled: false}) {
                        id
                        name
                    }
                }
                """)
                .execute()
                .path("updateEmbeddedMcpServer.name")
                .entity(String.class)
                .isEqualTo("Renamed");

            verify(embeddedMcpServerFacade).updateEmbeddedMcpServer(1L, "Renamed", false);
        }

        @Test
        void testUpdateEmbeddedMcpServerReportsDeniedAccess() {
            when(embeddedMcpServerFacade.updateEmbeddedMcpServer(1L, null, true))
                .thenThrow(new AccessDeniedException("denied"));

            graphQlTester.document("""
                mutation {
                    updateEmbeddedMcpServer(id: "1", input: {enabled: true}) {
                        id
                    }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors).hasSize(1));
        }

        @Test
        void testUpdateEmbeddedMcpServerTags() {
            Tag tag = new Tag("tag");

            tag.setId(5L);

            when(embeddedMcpServerFacade.updateEmbeddedMcpServerTags(eq(1L), anyList())).thenReturn(List.of(tag));

            graphQlTester.document("""
                mutation {
                    updateEmbeddedMcpServerTags(id: "1", tags: [{id: "5", name: "tag"}]) {
                        id
                        name
                    }
                }
                """)
                .execute()
                .path("updateEmbeddedMcpServerTags[0].name")
                .entity(String.class)
                .isEqualTo("tag");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Tag>> tagsArgumentCaptor = ArgumentCaptor.forClass(List.class);

            verify(embeddedMcpServerFacade).updateEmbeddedMcpServerTags(eq(1L), tagsArgumentCaptor.capture());

            assertThat(tagsArgumentCaptor.getValue())
                .singleElement()
                .satisfies(capturedTag -> {
                    assertThat(capturedTag.getId()).isEqualTo(5L);
                    assertThat(capturedTag.getName()).isEqualTo("tag");
                });
        }

        @Test
        void testUpdateEmbeddedMcpServerUrl() {
            when(embeddedMcpServerFacade.updateEmbeddedMcpServerSecretKey(1L)).thenReturn(createMcpServer("Server"));

            graphQlTester.document("""
                mutation {
                    updateEmbeddedMcpServerUrl(id: "1") {
                        id
                    }
                }
                """)
                .execute()
                .path("updateEmbeddedMcpServerUrl.id")
                .entity(String.class)
                .isEqualTo("1");

            verify(embeddedMcpServerFacade).updateEmbeddedMcpServerSecretKey(1L);
        }
    }

    @Nested
    class McpComponentTest {

        @Test
        void testEmbeddedMcpComponentsByServerId() {
            when(embeddedMcpServerFacade.getEmbeddedMcpServerMcpComponents(1L))
                .thenReturn(List.of(createMcpComponent()));

            graphQlTester.document("""
                query {
                    embeddedMcpComponentsByServerId(mcpServerId: "1") {
                        id
                        componentName
                    }
                }
                """)
                .execute()
                .path("embeddedMcpComponentsByServerId[0].componentName")
                .entity(String.class)
                .isEqualTo("gmail");

            verify(embeddedMcpServerFacade).getEmbeddedMcpServerMcpComponents(1L);
        }

        @Test
        void testEmbeddedMcpComponentsByServerIdReportsDeniedAccess() {
            when(embeddedMcpServerFacade.getEmbeddedMcpServerMcpComponents(1L))
                .thenThrow(new AccessDeniedException("denied"));

            graphQlTester.document("""
                query {
                    embeddedMcpComponentsByServerId(mcpServerId: "1") {
                        id
                    }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors).hasSize(1));
        }

        @Test
        void testCreateEmbeddedMcpComponent() {
            when(embeddedMcpServerFacade.createEmbeddedMcpComponent(any(), anyList()))
                .thenReturn(createMcpComponent());

            graphQlTester.document("""
                mutation {
                    createEmbeddedMcpComponent(input: {
                        componentName: "gmail", componentVersion: 1, mcpServerId: "1", connectionId: "7",
                        tools: [{name: "sendEmail", parameters: {to: "x"}}]
                    }) {
                        id
                    }
                }
                """)
                .execute()
                .path("createEmbeddedMcpComponent.id")
                .entity(String.class)
                .isEqualTo("2");

            ArgumentCaptor<McpComponent> mcpComponentArgumentCaptor = ArgumentCaptor.forClass(McpComponent.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<McpTool>> mcpToolsArgumentCaptor = ArgumentCaptor.forClass(List.class);

            verify(embeddedMcpServerFacade).createEmbeddedMcpComponent(
                mcpComponentArgumentCaptor.capture(), mcpToolsArgumentCaptor.capture());

            McpComponent mcpComponent = mcpComponentArgumentCaptor.getValue();

            assertThat(mcpComponent.getComponentName()).isEqualTo("gmail");
            assertThat(mcpComponent.getMcpServerId()).isEqualTo(1L);
            assertThat(mcpComponent.getConnectionId()).isEqualTo(7L);
            assertThat(mcpToolsArgumentCaptor.getValue())
                .singleElement()
                .satisfies(mcpTool -> {
                    assertThat(mcpTool.getName()).isEqualTo("sendEmail");

                    Map<String, ?> parameters = mcpTool.getParameters();

                    assertThat(parameters.get("to")).isEqualTo("x");
                });
        }

        @Test
        void testUpdateEmbeddedMcpComponent() {
            when(embeddedMcpServerFacade.updateEmbeddedMcpComponent(any(), anyList()))
                .thenReturn(createMcpComponent());

            graphQlTester.document("""
                mutation {
                    updateEmbeddedMcpComponent(id: "2", input: {
                        componentName: "gmail", componentVersion: 1, mcpServerId: "1", tools: [], version: 3
                    }) {
                        id
                    }
                }
                """)
                .execute()
                .path("updateEmbeddedMcpComponent.id")
                .entity(String.class)
                .isEqualTo("2");

            ArgumentCaptor<McpComponent> mcpComponentArgumentCaptor = ArgumentCaptor.forClass(McpComponent.class);

            verify(embeddedMcpServerFacade).updateEmbeddedMcpComponent(
                mcpComponentArgumentCaptor.capture(), eq(List.of()));

            McpComponent mcpComponent = mcpComponentArgumentCaptor.getValue();

            assertThat(mcpComponent.getId()).isEqualTo(2L);
            assertThat(mcpComponent.getMcpServerId()).isEqualTo(1L);
            assertThat(mcpComponent.getVersion()).isEqualTo(3);
        }

        @Test
        void testDeleteEmbeddedMcpComponent() {
            graphQlTester.document("""
                mutation {
                    deleteEmbeddedMcpComponent(id: "2")
                }
                """)
                .execute()
                .path("deleteEmbeddedMcpComponent")
                .entity(Boolean.class)
                .isEqualTo(true);

            verify(embeddedMcpServerFacade).deleteEmbeddedMcpComponent(2L);
        }
    }

    @Nested
    class McpToolTest {

        @Test
        void testEmbeddedMcpToolsByComponentId() {
            when(embeddedMcpServerFacade.getEmbeddedMcpComponentMcpTools(2L)).thenReturn(List.of(createMcpTool(true)));

            graphQlTester.document("""
                query {
                    embeddedMcpToolsByComponentId(mcpComponentId: "2") {
                        id
                        enabled
                    }
                }
                """)
                .execute()
                .path("embeddedMcpToolsByComponentId[0].enabled")
                .entity(Boolean.class)
                .isEqualTo(true);

            verify(embeddedMcpServerFacade).getEmbeddedMcpComponentMcpTools(2L);
        }

        @Test
        void testUpdateEmbeddedMcpTool() {
            when(embeddedMcpServerFacade.updateEmbeddedMcpTool(any())).thenReturn(createMcpTool(true));

            graphQlTester.document("""
                mutation {
                    updateEmbeddedMcpTool(id: "3", input: {
                        mcpComponentId: "2", name: "sendEmail", parameters: {to: "y"}, version: 4
                    }) {
                        id
                    }
                }
                """)
                .execute()
                .path("updateEmbeddedMcpTool.id")
                .entity(String.class)
                .isEqualTo("3");

            ArgumentCaptor<McpTool> mcpToolArgumentCaptor = ArgumentCaptor.forClass(McpTool.class);

            verify(embeddedMcpServerFacade).updateEmbeddedMcpTool(mcpToolArgumentCaptor.capture());

            McpTool mcpTool = mcpToolArgumentCaptor.getValue();

            assertThat(mcpTool.getId()).isEqualTo(3L);
            assertThat(mcpTool.getMcpComponentId()).isEqualTo(2L);
            Map<String, ?> parameters = mcpTool.getParameters();

            assertThat(parameters.get("to")).isEqualTo("y");
            assertThat(mcpTool.getVersion()).isEqualTo(4);
        }

        @Test
        void testUpdateEmbeddedMcpToolEnabled() {
            when(embeddedMcpServerFacade.updateEmbeddedMcpToolEnabled(3L, false)).thenReturn(createMcpTool(false));

            graphQlTester.document("""
                mutation {
                    updateEmbeddedMcpToolEnabled(id: "3", enabled: false) {
                        id
                        enabled
                    }
                }
                """)
                .execute()
                .path("updateEmbeddedMcpToolEnabled.enabled")
                .entity(Boolean.class)
                .isEqualTo(false);

            verify(embeddedMcpServerFacade).updateEmbeddedMcpToolEnabled(3L, false);
        }

        @Test
        void testDeleteEmbeddedMcpTool() {
            graphQlTester.document("""
                mutation {
                    deleteEmbeddedMcpTool(id: "3")
                }
                """)
                .execute()
                .path("deleteEmbeddedMcpTool")
                .entity(Boolean.class)
                .isEqualTo(true);

            verify(embeddedMcpServerFacade).deleteEmbeddedMcpTool(3L);
        }
    }

    private static McpComponent createMcpComponent() {
        McpComponent mcpComponent = new McpComponent("gmail", 1, 1L, null);

        mcpComponent.setId(2L);

        return mcpComponent;
    }

    private static McpServer createMcpServer(String name) {
        McpServer mcpServer = new McpServer(name, PlatformType.EMBEDDED, Environment.DEVELOPMENT, true);

        mcpServer.setId(1L);

        return mcpServer;
    }

    private static McpTool createMcpTool(boolean enabled) {
        McpTool mcpTool = new McpTool("sendEmail", Map.of(), 2L);

        mcpTool.setEnabled(enabled);
        mcpTool.setId(3L);

        return mcpTool;
    }

    @TestConfiguration
    static class ScalarConfiguration {

        @Bean
        RuntimeWiringConfigurer longScalarWiringConfigurer() {
            return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.longScalar());
        }

        @Bean
        RuntimeWiringConfigurer mapScalarWiringConfigurer() {
            return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.mapScalar());
        }
    }
}

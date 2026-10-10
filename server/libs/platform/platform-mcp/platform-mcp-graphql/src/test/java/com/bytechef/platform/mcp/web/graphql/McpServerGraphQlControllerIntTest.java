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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.web.graphql.config.McpGraphQlConfigurationSharedMocks;
import com.bytechef.platform.mcp.web.graphql.config.McpGraphQlTestConfiguration;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    McpGraphQlTestConfiguration.class,
    McpServerGraphQlController.class
})
@GraphQlTest(
    controllers = McpServerGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@McpGraphQlConfigurationSharedMocks
public class McpServerGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private McpServerFacade mcpServerFacade;

    @Autowired
    private McpServerService mcpServerService;

    @BeforeEach
    void beforeEach() {
        when(mcpServerService.getMcpServer(anyLong())).thenReturn(
            createMockMcpServer(1L, "Server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true));
    }

    @Test
    void testGetMcpServerById() {
        // Given
        McpServer mockServer = createMockMcpServer(
            1L, "Test Server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true);

        when(mcpServerService.getMcpServer(1L)).thenReturn(mockServer);

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    mcpServer(id: "1") {
                        id
                        name
                        type
                        enabled
                    }
                }
                """)
            .execute()
            .path("mcpServer.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("mcpServer.name")
            .entity(String.class)
            .isEqualTo("Test Server")
            .path("mcpServer.type")
            .entity(String.class)
            .isEqualTo("AUTOMATION")
            .path("mcpServer.enabled")
            .entity(Boolean.class)
            .isEqualTo(true);
    }

    @Test
    void testUrlMasksTheSecretKeyWhenTheCallerMayNotReadIt() {
        McpServer mockServer = createMockMcpServer(
            1L, "Test Server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true);

        when(mcpServerService.getMcpServer(1L)).thenReturn(mockServer);
        when(mcpServerService.getMcpServerSecretKey(1L)).thenThrow(new AccessDeniedException("Access Denied"));

        this.graphQlTester
            .document("""
                query {
                    mcpServer(id: "1") {
                        url
                    }
                }
                """)
            .execute()
            .path("mcpServer.url")
            .entity(String.class)
            .satisfies(url -> {
                assertThat(url).doesNotContain("secret-key-1");
                assertThat(url).endsWith("/mcp");
            });
    }

    @Test
    void testUrlCarriesTheSecretKeyForATenantAdmin() {
        McpServer mockServer = createMockMcpServer(
            1L, "Test Server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true);

        when(mcpServerService.getMcpServer(1L)).thenReturn(mockServer);
        when(mcpServerService.getMcpServerSecretKey(1L)).thenReturn("secret-key-1");

        this.graphQlTester
            .document("""
                query {
                    mcpServer(id: "1") {
                        url
                    }
                }
                """)
            .execute()
            .path("mcpServer.url")
            .entity(String.class)
            .satisfies(url -> assertThat(url).contains("secret-key-1"));
    }

    @Test
    void testUpdateMcpServer() {
        // Given
        McpServer mockServer = createMockMcpServer(
            1L, "Updated Server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, false);

        when(mcpServerService.update(1L, "Updated Server", false)).thenReturn(mockServer);

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    updateMcpServer(id: "1", input: {
                        name: "Updated Server",
                        enabled: false
                    }) {
                        id
                        name
                        enabled
                    }
                }
                """)
            .execute()
            .path("updateMcpServer.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("updateMcpServer.name")
            .entity(String.class)
            .isEqualTo("Updated Server")
            .path("updateMcpServer.enabled")
            .entity(Boolean.class)
            .isEqualTo(false);
    }

    @Test
    void testUpdateMcpServerAuthenticationRequired() {
        McpServer mockServer = createMockMcpServer(
            1L, "Test Server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true);

        when(mcpServerService.update(1L, "Test Server", true)).thenReturn(mockServer);

        McpServer updatedMockServer = createMockMcpServer(
            1L, "Test Server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true);

        updatedMockServer.setAuthenticationRequired(false);

        when(mcpServerService.update(argThat(server -> server != null && !server.isAuthenticationRequired())))
            .thenReturn(updatedMockServer);

        this.graphQlTester
            .document("""
                mutation {
                    updateMcpServer(id: "1", input: {
                        name: "Test Server",
                        enabled: true,
                        authenticationRequired: false
                    }) {
                        id
                        authenticationRequired
                    }
                }
                """)
            .execute()
            .path("updateMcpServer.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("updateMcpServer.authenticationRequired")
            .entity(Boolean.class)
            .isEqualTo(false);

        verify(mcpServerService).update(argThat(server -> server != null && !server.isAuthenticationRequired()));
    }

    @Test
    void testUpdateMcpServerRejectsInvariantViolation() {
        McpServer mockServer = createMockMcpServer(
            1L, "Test Server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true);

        when(mcpServerService.update(1L, "Test Server", true)).thenReturn(mockServer);

        when(mcpServerService.update(argThat(
            server -> server != null && server.isEnforceToolAuthorization() && !server.isAuthenticationRequired())))
                .thenThrow(new IllegalArgumentException(
                    "enforceToolAuthorization requires authenticationRequired to be enabled"));

        this.graphQlTester
            .document("""
                mutation {
                    updateMcpServer(id: "1", input: {
                        name: "Test Server",
                        enabled: true,
                        enforceToolAuthorization: true,
                        authenticationRequired: false
                    }) {
                        id
                    }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> {
                assertThat(errors).hasSize(1);

                assertThat(errors.get(0)
                    .getErrorType()).isEqualTo(ErrorType.INTERNAL_ERROR);
            })
            .path("updateMcpServer")
            .valueIsNull();
    }

    @Test
    void testUpdateMcpServerEnablesBothFlagsInSingleUpdate() {
        McpServer mockServer = createMockMcpServer(
            1L, "Test Server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true);

        mockServer.setAuthenticationRequired(false);
        mockServer.setEnforceToolAuthorization(false);

        when(mcpServerService.update(1L, "Test Server", true)).thenReturn(mockServer);

        McpServer updatedMockServer = createMockMcpServer(
            1L, "Test Server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true);

        updatedMockServer.setAuthenticationRequired(true);
        updatedMockServer.setEnforceToolAuthorization(true);

        when(mcpServerService.update(argThat(
            server -> server != null && server.isAuthenticationRequired() && server.isEnforceToolAuthorization())))
                .thenReturn(updatedMockServer);

        this.graphQlTester
            .document("""
                mutation {
                    updateMcpServer(id: "1", input: {
                        name: "Test Server",
                        enabled: true,
                        authenticationRequired: true,
                        enforceToolAuthorization: true
                    }) {
                        id
                        authenticationRequired
                        enforceToolAuthorization
                    }
                }
                """)
            .execute()
            .errors()
            .verify()
            .path("updateMcpServer.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("updateMcpServer.authenticationRequired")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("updateMcpServer.enforceToolAuthorization")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(mcpServerService).update(
            argThat(server -> server != null && server.isAuthenticationRequired()
                && server.isEnforceToolAuthorization()));
    }

    @Test
    void testUpdateMcpServerTags() {
        // Given
        List<Tag> mockTags = List.of(
            createMockTag(1L, "tag1"),
            createMockTag(2L, "tag2"));

        when(mcpServerFacade.updateMcpServerTags(anyLong(), any())).thenReturn(mockTags);

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    updateMcpServerTags(id: "1", tags: [
                        { id: "1", name: "tag1" },
                        { id: "2", name: "tag2" }
                    ]) {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("updateMcpServerTags")
            .entityList(Object.class)
            .hasSize(2);

        verify(mcpServerFacade).updateMcpServerTags(eq(1L), any());
    }

    @Test
    void testDeleteMcpServer() {
        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    deleteMcpServer(id: "1")
                }
                """)
            .execute()
            .path("deleteMcpServer")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(mcpServerFacade).deleteMcpServer(1L);
    }

    @Test
    void testUpdateMcpServerUrl() {
        McpServer mcpServer = createMockMcpServer(
            1L, "Server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true);

        when(mcpServerService.getMcpServer(1L)).thenReturn(mcpServer);
        when(mcpServerService.rotateSecretKey(1L)).thenReturn(mcpServer);

        this.graphQlTester
            .document("""
                mutation {
                    updateMcpServerUrl(id: "1")
                }
                """)
            .execute()
            .path("updateMcpServerUrl")
            .entity(String.class)
            .satisfies(url -> assertThat(url).endsWith("/api/automation/" + mcpServer.getSecretKey() + "/mcp"));

        verify(mcpServerService).rotateSecretKey(1L);
    }

    @Nested
    class EmbeddedMcpServerTest {

        @BeforeEach
        void beforeEach() {
            when(mcpServerService.getMcpServer(1L)).thenReturn(
                createMockMcpServer(1L, "Embedded", PlatformType.EMBEDDED, Environment.DEVELOPMENT, true));
        }

        @Test
        void testGetMcpServerByIdRejectsEmbeddedMcpServer() {
            assertRejected("""
                query {
                    mcpServer(id: "1") {
                        id
                        secretKey
                    }
                }
                """);
        }

        @Test
        void testGetMcpServersRejectsEmbeddedType() {
            assertRejected("""
                query {
                    mcpServers(type: EMBEDDED) {
                        id
                        secretKey
                    }
                }
                """);

            verify(mcpServerService, never()).getMcpServers(any());
        }

        @Test
        void testCreateMcpServerRejectsEmbeddedType() {
            assertRejected("""
                mutation {
                    createMcpServer(input: {name: "Embedded", type: EMBEDDED, environmentId: "0", enabled: true}) {
                        id
                    }
                }
                """);

            verify(mcpServerService, never()).create(anyString(), any(), any(), any());
        }

        @Test
        void testUpdateMcpServerRejectsEmbeddedMcpServer() {
            assertRejected("""
                mutation {
                    updateMcpServer(id: "1", input: {name: "Renamed", enabled: false}) {
                        id
                    }
                }
                """);

            verify(mcpServerService, never()).update(anyLong(), any(), any());
        }

        @Test
        void testUpdateMcpServerTagsRejectsEmbeddedMcpServer() {
            assertRejected("""
                mutation {
                    updateMcpServerTags(id: "1", tags: [{name: "tag"}]) {
                        id
                    }
                }
                """);

            verifyNoInteractions(mcpServerFacade);
        }

        @Test
        void testUpdateMcpServerUrlRejectsEmbeddedMcpServer() {
            assertRejected("""
                mutation {
                    updateMcpServerUrl(id: "1")
                }
                """);

            verify(mcpServerService, never()).rotateSecretKey(anyLong());
        }

        @Test
        void testDeleteMcpServerRejectsEmbeddedMcpServer() {
            assertRejected("""
                mutation {
                    deleteMcpServer(id: "1")
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

    private McpServer createMockMcpServer(
        Long id, String name, PlatformType type, Environment environment, Boolean enabled) {

        McpServer server = new McpServer(name, type, environment, enabled);

        server.setId(id);
        server.setVersion(1);

        return server;
    }

    private Tag createMockTag(Long id, String name) {
        Tag tag = new Tag();

        tag.setId(id);
        tag.setName(name);

        return tag;
    }
}

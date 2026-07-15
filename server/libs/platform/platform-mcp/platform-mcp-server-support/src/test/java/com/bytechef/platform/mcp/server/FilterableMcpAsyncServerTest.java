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

package com.bytechef.platform.mcp.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import io.modelcontextprotocol.spec.McpStreamableServerSession;
import io.modelcontextprotocol.spec.McpStreamableServerTransportProvider;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

/**
 * Verifies that the filtering core builds transport-agnostic handlers and can be attached to both the streamable and
 * the SSE session factories.
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class FilterableMcpAsyncServerTest {

    private static final String RESOURCE_URI = "ui://test/resource";

    @Mock
    private McpStreamableServerTransportProvider streamableTransportProvider;

    @Mock
    private McpServerTransportProvider sseTransportProvider;

    @Test
    void testAttachStreamableInstallsSessionFactory() {
        FilterableMcpAsyncServer server = newServer();

        server.attachStreamable(streamableTransportProvider);

        verify(streamableTransportProvider).setSessionFactory(any(McpStreamableServerSession.Factory.class));
    }

    @Test
    void testAttachSseInstallsSessionFactory() {
        FilterableMcpAsyncServer server = newServer();

        server.attachSse(sseTransportProvider);

        verify(sseTransportProvider).setSessionFactory(any(McpServerSession.Factory.class));
    }

    @Test
    void testResourcesListReturnsConfiguredResources() {
        FilterableMcpAsyncServer server = newServerWithResource();

        McpSchema.ListResourcesResult result = (McpSchema.ListResourcesResult) server
            .requestHandler(McpSchema.METHOD_RESOURCES_LIST)
            .handle(null, Map.of())
            .block();

        assertEquals(1, result.resources()
            .size());
        assertEquals(RESOURCE_URI, result.resources()
            .get(0)
            .uri());
    }

    @Test
    void testResourcesReadReturnsResourceContents() {
        FilterableMcpAsyncServer server = newServerWithResource();

        McpSchema.ReadResourceResult result = (McpSchema.ReadResourceResult) server
            .requestHandler(McpSchema.METHOD_RESOURCES_READ)
            .handle(null, Map.of("uri", RESOURCE_URI))
            .block();

        assertEquals(1, result.contents()
            .size());
    }

    @Test
    void testResourcesReadUnknownUriReturnsResourceNotFound() {
        FilterableMcpAsyncServer server = newServerWithResource();

        Mono<?> result = server.requestHandler(McpSchema.METHOD_RESOURCES_READ)
            .handle(null, Map.of("uri", "ui://test/missing"));

        McpError mcpError = assertThrows(McpError.class, result::block);

        assertEquals(McpSchema.ErrorCodes.RESOURCE_NOT_FOUND, mcpError.getJsonRpcError()
            .code()
            .intValue());
    }

    private static FilterableMcpAsyncServer newServer() {
        return new FilterableMcpAsyncServer(
            McpJsonDefaults.getMapper(), new McpSchema.Implementation("test", "1.0.0"),
            McpSchema.ServerCapabilities.builder()
                .tools(true)
                .build(),
            null, Duration.ofSeconds(10), McpJsonDefaults.getSchemaValidator(), false, exchange -> List.of(),
            List.of(), List.of("2024-11-05"));
    }

    private static FilterableMcpAsyncServer newServerWithResource() {
        return new FilterableMcpAsyncServer(
            McpJsonDefaults.getMapper(), new McpSchema.Implementation("test", "1.0.0"),
            McpSchema.ServerCapabilities.builder()
                .resources(false, false)
                .build(),
            null, Duration.ofSeconds(10), McpJsonDefaults.getSchemaValidator(), false, exchange -> List.of(),
            List.of(resourceSpecification(RESOURCE_URI)), List.of("2024-11-05"));
    }

    private static McpServerFeatures.AsyncResourceSpecification resourceSpecification(String uri) {
        McpSchema.Resource resource = McpSchema.Resource.builder()
            .uri(uri)
            .name("Test Resource")
            .mimeType("text/plain")
            .build();

        return new McpServerFeatures.AsyncResourceSpecification(
            resource,
            (exchange, request) -> Mono.just(new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(request.uri(), "text/plain", "hello")))));
    }
}

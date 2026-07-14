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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import io.modelcontextprotocol.spec.McpStreamableServerSession;
import io.modelcontextprotocol.spec.McpStreamableServerTransportProvider;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies that the filtering core builds transport-agnostic handlers and can be attached to both the streamable and
 * the SSE session factories.
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class FilterableMcpAsyncServerTest {

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

    private static FilterableMcpAsyncServer newServer() {
        return new FilterableMcpAsyncServer(
            McpJsonDefaults.getMapper(), new McpSchema.Implementation("test", "1.0.0"),
            McpSchema.ServerCapabilities.builder()
                .tools(true)
                .build(),
            null, Duration.ofSeconds(10), McpJsonDefaults.getSchemaValidator(), false, exchange -> List.of(),
            List.of(), List.of("2024-11-05"));
    }
}

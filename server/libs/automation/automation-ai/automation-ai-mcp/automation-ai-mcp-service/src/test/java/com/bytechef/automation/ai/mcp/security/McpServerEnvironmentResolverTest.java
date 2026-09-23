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

package com.bytechef.automation.ai.mcp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The resolver takes only the repository: reading through {@code McpServerService.getMcpServer}, which is itself
 * guarded by a check on the same server, recursed until the stack overflowed for every non-admin.
 *
 * @author Ivica Cardic
 */
class McpServerEnvironmentResolverTest {

    private static final long MCP_SERVER_ID = 9L;

    private final McpServerRepository mcpServerRepository = mock(McpServerRepository.class);
    private final McpServerEnvironmentResolver resolver = new McpServerEnvironmentResolver(mcpServerRepository);

    @Test
    void testReportsTheEnvironmentOfTheServer() {
        McpServer mcpServer = new McpServer();

        mcpServer.setEnvironment(Environment.STAGING);

        when(mcpServerRepository.findById(MCP_SERVER_ID)).thenReturn(Optional.of(mcpServer));

        assertThat(resolver.fetchEnvironment(MCP_SERVER_ID)).contains(Environment.STAGING);
    }

    @Test
    void testAnUnknownServerHasNoEnvironment() {
        when(mcpServerRepository.findById(MCP_SERVER_ID)).thenReturn(Optional.empty());

        assertThat(resolver.fetchEnvironment(MCP_SERVER_ID)).isEmpty();
    }
}

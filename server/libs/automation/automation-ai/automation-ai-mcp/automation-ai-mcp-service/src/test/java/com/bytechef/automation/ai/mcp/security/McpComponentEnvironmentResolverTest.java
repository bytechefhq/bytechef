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
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class McpComponentEnvironmentResolverTest {

    private static final long MCP_COMPONENT_ID = 3L;
    private static final long MCP_SERVER_ID = 9L;

    private final McpComponentRepository mcpComponentRepository = mock(McpComponentRepository.class);
    private final McpServerRepository mcpServerRepository = mock(McpServerRepository.class);

    @BeforeEach
    void setUp() {
        McpServer mcpServer = new McpServer();

        mcpServer.setEnvironment(Environment.PRODUCTION);

        when(mcpServerRepository.findById(MCP_SERVER_ID)).thenReturn(Optional.of(mcpServer));
        when(mcpComponentRepository.findById(MCP_COMPONENT_ID))
            .thenReturn(Optional.of(new McpComponent("component", 1, MCP_SERVER_ID, null)));
    }

    @Test
    void testMcpComponentReportsTheEnvironmentOfItsServer() {
        McpComponentEnvironmentResolver resolver =
            new McpComponentEnvironmentResolver(mcpComponentRepository, mcpServerRepository);

        assertThat(resolver.resourceType()).isEqualTo("McpComponent");
        assertThat(resolver.fetchEnvironment(MCP_COMPONENT_ID)).contains(Environment.PRODUCTION);
        assertThat(resolver.fetchEnvironment(MCP_COMPONENT_ID + 100)).isEmpty();
    }
}

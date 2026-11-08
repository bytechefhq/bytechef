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

package com.bytechef.component.mcp.client.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeMcpServerFunction;
import com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeMcpServerFunction.McpServer;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class McpClientClaudeCodeMcpServerTest {

    private final Context context = mock(Context.class);
    private final ConnectionDefinitionService connectionDefinitionService = mock(ConnectionDefinitionService.class);

    @Test
    void testApplyUsesTheConnectionCredentialsAsHeaders() throws Exception {
        when(connectionDefinitionService.executeAuthorizationApply(
            eq("mcpClient"), eq(1), eq(AuthorizationType.BEARER_TOKEN), anyMap(), any()))
                .thenReturn(ApplyResponse.ofHeaders(Map.of("Authorization", List.of("Bearer secret"))));

        McpServer mcpServer = getFunction().apply(
            MockParametersFactory.create(Map.of("serverName", "github")),
            MockParametersFactory.create(
                Map.of("url", "https://api.githubcopilot.com/mcp/", "transportType", "httpStreamable",
                    "token", "secret")),
            context);

        assertThat(mcpServer).isEqualTo(
            new McpServer(
                "github", McpServer.HTTP, "https://api.githubcopilot.com/mcp/",
                Map.of("Authorization", "Bearer secret")));
    }

    @Test
    void testApplyWithoutCredentialsSendsNoHeadersAndNamesTheServerAfterItsHost() throws Exception {
        McpServer mcpServer = getFunction().apply(
            MockParametersFactory.create(Map.of()),
            MockParametersFactory.create(Map.of("url", "https://mcp.example.com/sse", "transportType", "sse")),
            context);

        assertThat(mcpServer).isEqualTo(
            new McpServer("mcp_example_com", McpServer.SSE, "https://mcp.example.com/sse", Map.of()));

        verify(connectionDefinitionService, never()).executeAuthorizationApply(
            any(), any(int.class), any(), anyMap(), any());
    }

    @Test
    void testGetServerNameReplacesCharactersClaudeCodeDoesNotAccept() {
        assertThat(McpClientClaudeCodeMcpServer.getServerName(" My Server.1 ", "https://example.com"))
            .isEqualTo("My_Server_1");
    }

    private ClaudeCodeMcpServerFunction getFunction() throws Exception {
        McpClientClaudeCodeMcpServer mcpClientClaudeCodeMcpServer = new McpClientClaudeCodeMcpServer(
            connectionDefinitionService);

        ClusterElementDefinition<ClaudeCodeMcpServerFunction> clusterElementDefinition =
            mcpClientClaudeCodeMcpServer.getClusterElementDefinition();

        return clusterElementDefinition.getElement();
    }
}

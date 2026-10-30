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

package com.bytechef.automation.ai.mcp.server.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.tenant.domain.TenantKey;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AutomationMcpEndpointTest {

    private static final String MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of("acme"));

    private final McpServerService mcpServerService = mock(McpServerService.class);

    private final AutomationMcpEndpoint automationMcpEndpoint = new AutomationMcpEndpoint(mcpServerService);

    @Test
    void testMatchesAutomationPath() {
        assertThat(automationMcpEndpoint.matches("/api/automation/" + MCP_SERVER_SECRET_KEY + "/mcp")).isTrue();
        assertThat(automationMcpEndpoint.matches("/api/management/" + MCP_SERVER_SECRET_KEY + "/mcp")).isFalse();
        assertThat(automationMcpEndpoint.matches("/api/management/mcp")).isFalse();
    }

    @Test
    void testScope() {
        assertThat(automationMcpEndpoint.getScope()).isEqualTo("mcp:automation");
    }

    @Test
    void testResolveTenantIdFromSecretKey() {
        assertThat(automationMcpEndpoint.resolveTenantId("/api/automation/" + MCP_SERVER_SECRET_KEY + "/mcp"))
            .contains("acme");
    }

    @Test
    void testResolveTenantIdRejectsPathWithoutSecretKey() {
        assertThatThrownBy(() -> automationMcpEndpoint.resolveTenantId("/api/automation/mcp"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testResolveResourceNameReturnsMcpServerName() {
        McpServer mcpServer = mock(McpServer.class);

        when(mcpServer.getName()).thenReturn("Sales");
        when(mcpServerService.getMcpServer(MCP_SERVER_SECRET_KEY)).thenReturn(mcpServer);

        assertThat(
            automationMcpEndpoint.resolveResourceName(
                "https://bytechef.test/api/automation/" + MCP_SERVER_SECRET_KEY + "/mcp"))
                    .contains("Sales");
    }

    @Test
    void testResolveResourceNameIgnoresOtherResource() {
        assertThat(automationMcpEndpoint.resolveResourceName("https://bytechef.test/api/management/mcp")).isEmpty();

        verifyNoInteractions(mcpServerService);
    }

}

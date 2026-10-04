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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.tenant.domain.TenantKey;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * @author Ivica Cardic
 */
class AutomationMcpAuthenticationRequiredResolverTest {

    private static final String MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of("public"));

    private final McpServerService mcpServerService = mock(McpServerService.class);
    private final AutomationMcpAuthenticationRequiredResolver automationMcpAuthenticationRequiredResolver =
        new AutomationMcpAuthenticationRequiredResolver(mcpServerService);

    @Test
    void testAbstainsWhenRequestIsNotAnAutomationMcpEndpoint() {
        Optional<Boolean> authenticationRequired = automationMcpAuthenticationRequiredResolver
            .resolveAuthenticationRequired(mockRequest("/api/management/" + MCP_SERVER_SECRET_KEY + "/mcp"));

        assertThat(authenticationRequired).isEmpty();
    }

    @Test
    void testResolvesServerOptingOutOfAuthentication() {
        mockMcpServer(false);

        Optional<Boolean> authenticationRequired = automationMcpAuthenticationRequiredResolver
            .resolveAuthenticationRequired(mockRequest("/api/automation/" + MCP_SERVER_SECRET_KEY + "/mcp"));

        assertThat(authenticationRequired).hasValue(false);
    }

    @Test
    void testResolvesServerRequiringAuthentication() {
        mockMcpServer(true);

        Optional<Boolean> authenticationRequired = automationMcpAuthenticationRequiredResolver
            .resolveAuthenticationRequired(mockRequest("/api/automation/" + MCP_SERVER_SECRET_KEY + "/mcp"));

        assertThat(authenticationRequired).hasValue(true);
    }

    @Test
    void testRequiresAuthenticationWhenPathSecretIsNotATenantKey() {
        Optional<Boolean> authenticationRequired = automationMcpAuthenticationRequiredResolver
            .resolveAuthenticationRequired(mockRequest("/api/automation/not-a-tenant-key/mcp"));

        assertThat(authenticationRequired).hasValue(true);
    }

    @Test
    void testRequiresAuthenticationWhenServerCannotBeResolved() {
        when(mcpServerService.getMcpServer(MCP_SERVER_SECRET_KEY)).thenThrow(new IllegalArgumentException());

        Optional<Boolean> authenticationRequired = automationMcpAuthenticationRequiredResolver
            .resolveAuthenticationRequired(mockRequest("/api/automation/" + MCP_SERVER_SECRET_KEY + "/mcp"));

        assertThat(authenticationRequired).hasValue(true);
    }

    private void mockMcpServer(boolean authenticationRequired) {
        McpServer mcpServer = mock(McpServer.class);

        when(mcpServer.isAuthenticationRequired()).thenReturn(authenticationRequired);
        when(mcpServerService.getMcpServer(MCP_SERVER_SECRET_KEY)).thenReturn(mcpServer);
    }

    private static MockHttpServletRequest mockRequest(String servletPath) {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

        mockHttpServletRequest.setServletPath(servletPath);

        return mockHttpServletRequest;
    }
}

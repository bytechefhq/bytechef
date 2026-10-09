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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.tenant.domain.TenantKey;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * @author Ivica Cardic
 */
class AutomationMcpServerSecretVerifierTest {

    private static final String MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of("acme"));

    private final McpServerService mcpServerService = mock(McpServerService.class);

    private final AutomationMcpServerSecretVerifier automationMcpServerSecretVerifier =
        new AutomationMcpServerSecretVerifier(mcpServerService);

    @Test
    void testEnabledServerIsVerified() {
        mockMcpServer(true, MCP_SERVER_SECRET_KEY);

        assertThat(automationMcpServerSecretVerifier.verifyMcpServerSecret(automationRequest(MCP_SERVER_SECRET_KEY)))
            .contains(true);
    }

    @Test
    void testDisabledServerIsRejected() {
        mockMcpServer(false, MCP_SERVER_SECRET_KEY);

        assertThat(automationMcpServerSecretVerifier.verifyMcpServerSecret(automationRequest(MCP_SERVER_SECRET_KEY)))
            .contains(false);
    }

    @Test
    void testEmbeddedServerIsRejected() {
        mockMcpServer(true, MCP_SERVER_SECRET_KEY, PlatformType.EMBEDDED);

        assertThat(automationMcpServerSecretVerifier.verifyMcpServerSecret(automationRequest(MCP_SERVER_SECRET_KEY)))
            .contains(false);
    }

    @Test
    void testServerWithDifferentSecretKeyIsRejected() {
        mockMcpServer(true, String.valueOf(TenantKey.of("acme")));

        assertThat(automationMcpServerSecretVerifier.verifyMcpServerSecret(automationRequest(MCP_SERVER_SECRET_KEY)))
            .contains(false);
    }

    @Test
    void testUnknownServerIsRejected() {
        when(mcpServerService.getMcpServer(MCP_SERVER_SECRET_KEY)).thenThrow(new IllegalArgumentException());

        assertThat(automationMcpServerSecretVerifier.verifyMcpServerSecret(automationRequest(MCP_SERVER_SECRET_KEY)))
            .contains(false);
    }

    @Test
    void testOtherSurfaceIsNotClaimed() {
        String path = "/api/management/%s/mcp".formatted(MCP_SERVER_SECRET_KEY);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);

        request.setServletPath(path);

        assertThat(automationMcpServerSecretVerifier.verifyMcpServerSecret(request)).isEmpty();
        verifyNoInteractions(mcpServerService);
    }

    private void mockMcpServer(boolean enabled, String secretKey) {
        mockMcpServer(enabled, secretKey, PlatformType.AUTOMATION);
    }

    private void mockMcpServer(boolean enabled, String secretKey, PlatformType type) {
        McpServer mcpServer = mock(McpServer.class);

        when(mcpServer.getType()).thenReturn(type);
        when(mcpServer.isEnabled()).thenReturn(enabled);
        when(mcpServer.getSecretKey()).thenReturn(secretKey);
        when(mcpServerService.getMcpServer(MCP_SERVER_SECRET_KEY)).thenReturn(mcpServer);
    }

    private static MockHttpServletRequest automationRequest(String mcpServerSecretKey) {
        String path = "/api/automation/%s/mcp".formatted(mcpServerSecretKey);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);

        request.setServletPath(path);

        return request;
    }
}

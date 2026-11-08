/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication;

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
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedMcpServerSecretVerifierTest {

    private static final String MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of("acme"));

    private final McpServerService mcpServerService = mock(McpServerService.class);

    private final EmbeddedMcpServerSecretVerifier embeddedMcpServerSecretVerifier =
        new EmbeddedMcpServerSecretVerifier(mcpServerService);

    @Test
    void testEnabledServerIsVerified() {
        mockMcpServer(true, MCP_SERVER_SECRET_KEY, PlatformType.EMBEDDED);

        assertThat(embeddedMcpServerSecretVerifier.verifyMcpServerSecret(embeddedRequest(MCP_SERVER_SECRET_KEY)))
            .contains(true);
    }

    @Test
    void testDisabledServerIsRejected() {
        mockMcpServer(false, MCP_SERVER_SECRET_KEY, PlatformType.EMBEDDED);

        assertThat(embeddedMcpServerSecretVerifier.verifyMcpServerSecret(embeddedRequest(MCP_SERVER_SECRET_KEY)))
            .contains(false);
    }

    @Test
    void testAutomationServerIsRejected() {
        mockMcpServer(true, MCP_SERVER_SECRET_KEY, PlatformType.AUTOMATION);

        assertThat(embeddedMcpServerSecretVerifier.verifyMcpServerSecret(embeddedRequest(MCP_SERVER_SECRET_KEY)))
            .contains(false);
    }

    @Test
    void testServerWithDifferentSecretKeyIsRejected() {
        mockMcpServer(true, String.valueOf(TenantKey.of("acme")), PlatformType.EMBEDDED);

        assertThat(embeddedMcpServerSecretVerifier.verifyMcpServerSecret(embeddedRequest(MCP_SERVER_SECRET_KEY)))
            .contains(false);
    }

    @Test
    void testUnknownServerIsRejected() {
        when(mcpServerService.getMcpServer(MCP_SERVER_SECRET_KEY)).thenThrow(new IllegalArgumentException());

        assertThat(embeddedMcpServerSecretVerifier.verifyMcpServerSecret(embeddedRequest(MCP_SERVER_SECRET_KEY)))
            .contains(false);
    }

    @Test
    void testOtherSurfaceIsNotClaimed() {
        String path = "/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);

        request.setServletPath(path);

        assertThat(embeddedMcpServerSecretVerifier.verifyMcpServerSecret(request)).isEmpty();
        verifyNoInteractions(mcpServerService);
    }

    private void mockMcpServer(boolean enabled, String secretKey, PlatformType type) {
        McpServer mcpServer = mock(McpServer.class);

        when(mcpServer.getType()).thenReturn(type);
        when(mcpServer.isEnabled()).thenReturn(enabled);
        when(mcpServer.getSecretKey()).thenReturn(secretKey);
        when(mcpServerService.getMcpServer(MCP_SERVER_SECRET_KEY)).thenReturn(mcpServer);
    }

    private static MockHttpServletRequest embeddedRequest(String mcpServerSecretKey) {
        String path = "/api/embedded/%s/mcp".formatted(mcpServerSecretKey);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);

        request.setServletPath(path);

        return request;
    }
}

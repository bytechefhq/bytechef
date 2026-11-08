/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.mcp.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.security.web.mcp.McpServerSecretVerifier;
import com.bytechef.tenant.TenantContext;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * @version ee
 * @author Ivica Cardic
 */
class McpTenantSecretVerifierTest {

    private static final String MCP_PATH = "/api/management/secret/mcp";

    @Test
    void testVerifiesAgainstTheMcpPathInsideTheGivenTenant() {
        AtomicReference<String> verifiedServletPath = new AtomicReference<>();
        AtomicReference<String> verifiedTenantId = new AtomicReference<>();

        McpServerSecretVerifier mcpServerSecretVerifier = request -> {
            verifiedServletPath.set(request.getServletPath());
            verifiedTenantId.set(TenantContext.getCurrentTenantId());

            return Optional.of(true);
        };

        McpTenantSecretVerifier mcpTenantSecretVerifier = new McpTenantSecretVerifier(
            () -> List.of(mcpServerSecretVerifier));

        MockHttpServletRequest request = new MockHttpServletRequest(
            "GET", "/.well-known/oauth-protected-resource" + MCP_PATH);

        request.setServletPath("/.well-known/oauth-protected-resource" + MCP_PATH);

        assertThat(mcpTenantSecretVerifier.isVerified(request, "acme", MCP_PATH)).isTrue();
        assertThat(verifiedServletPath.get()).isEqualTo(MCP_PATH);
        assertThat(verifiedTenantId.get()).isEqualTo("acme");
    }

    @Test
    void testUsesTheFirstVerifierThatRecognisesThePath() {
        McpTenantSecretVerifier mcpTenantSecretVerifier = new McpTenantSecretVerifier(
            () -> List.of(request -> Optional.empty(), request -> Optional.of(false), request -> Optional.of(true)));

        assertThat(mcpTenantSecretVerifier.isVerified(new MockHttpServletRequest(), "acme", MCP_PATH)).isFalse();
    }

    @Test
    void testIsNotVerifiedWhenNoVerifierRecognisesThePath() {
        McpTenantSecretVerifier mcpTenantSecretVerifier = new McpTenantSecretVerifier(
            () -> List.of(request -> Optional.empty()));

        assertThat(mcpTenantSecretVerifier.isVerified(new MockHttpServletRequest(), "acme", MCP_PATH)).isFalse();
    }

    @Test
    void testIsNotVerifiedWhenAVerifierFails() {
        McpTenantSecretVerifier mcpTenantSecretVerifier = new McpTenantSecretVerifier(
            () -> List.of(request -> {
                throw new IllegalStateException("lookup failed");
            }));

        assertThat(mcpTenantSecretVerifier.isVerified(new MockHttpServletRequest(), "acme", MCP_PATH)).isFalse();
    }
}

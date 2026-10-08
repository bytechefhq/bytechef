/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.configurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpServerApiKeyAuthenticationToken;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.tenant.domain.TenantKey;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedMcpServerApiKeyAuthenticationConverterTest {

    private static final String MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of("tenant-a"));

    private final EmbeddedMcpServerApiKeyAuthenticationConverter embeddedMcpServerApiKeyAuthenticationConverter =
        new EmbeddedMcpServerApiKeyAuthenticationConverter(mock(SigningKeyService.class));

    @Test
    void testConvertWithoutAuthorizationHeaderYieldsTokenWithoutExternalUser() {
        HttpServletRequest httpServletRequest = mockRequest("/api/embedded/" + MCP_SERVER_SECRET_KEY + "/mcp", null);

        Authentication authentication = embeddedMcpServerApiKeyAuthenticationConverter.convert(httpServletRequest);

        assertThat(authentication).isInstanceOf(EmbeddedMcpServerApiKeyAuthenticationToken.class);

        EmbeddedMcpServerApiKeyAuthenticationToken embeddedMcpServerApiKeyAuthenticationToken =
            (EmbeddedMcpServerApiKeyAuthenticationToken) authentication;

        assertThat(embeddedMcpServerApiKeyAuthenticationToken.getExternalUserId()).isNull();
        assertThat(embeddedMcpServerApiKeyAuthenticationToken.getMcpServerSecretKey())
            .isEqualTo(MCP_SERVER_SECRET_KEY);
        assertThat(embeddedMcpServerApiKeyAuthenticationToken.getTenantId()).isEqualTo("tenant-a");
    }

    @Test
    void testConvertWithMalformedBearerTokenYieldsTokenWithoutExternalUser() {
        HttpServletRequest httpServletRequest = mockRequest(
            "/api/embedded/" + MCP_SERVER_SECRET_KEY + "/mcp", "Bearer not-a-jwt");

        Authentication authentication = embeddedMcpServerApiKeyAuthenticationConverter.convert(httpServletRequest);

        assertThat(authentication).isInstanceOf(EmbeddedMcpServerApiKeyAuthenticationToken.class);

        EmbeddedMcpServerApiKeyAuthenticationToken embeddedMcpServerApiKeyAuthenticationToken =
            (EmbeddedMcpServerApiKeyAuthenticationToken) authentication;

        assertThat(embeddedMcpServerApiKeyAuthenticationToken.getExternalUserId()).isNull();
        assertThat(embeddedMcpServerApiKeyAuthenticationToken.getTenantId()).isEqualTo("tenant-a");
    }

    @Test
    void testConvertRejectsPathSecretThatIsNotATenantKey() {
        HttpServletRequest httpServletRequest = mockRequest("/api/embedded/not-a-tenant-key/mcp", null);

        assertThatExceptionOfType(BadCredentialsException.class)
            .isThrownBy(() -> embeddedMcpServerApiKeyAuthenticationConverter.convert(httpServletRequest));
    }

    private static HttpServletRequest mockRequest(String servletPath, String authorization) {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);

        when(httpServletRequest.getServletPath()).thenReturn(servletPath);
        when(httpServletRequest.getHeader("Authorization")).thenReturn(authorization);

        return httpServletRequest;
    }
}

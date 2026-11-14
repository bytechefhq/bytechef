/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.configurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpServerApiKeyAuthenticationToken;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
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

    private static final String SIGNING_KEY_ID = String.valueOf(TenantKey.of("tenant-b"));

    private final SigningKeyService signingKeyService = mock(SigningKeyService.class);
    private final EmbeddedMcpServerApiKeyAuthenticationConverter embeddedMcpServerApiKeyAuthenticationConverter =
        new EmbeddedMcpServerApiKeyAuthenticationConverter(() -> List.of(Environment.values()), signingKeyService);

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
    void testConvertWithSignedBearerTokenRecordsSigningKeyUseInTheTokenTenant() throws NoSuchAlgorithmException {
        KeyPair keyPair = generateKeyPair();
        List<String> tenantIds = new ArrayList<>();

        when(signingKeyService.getPublicKey(SIGNING_KEY_ID, Environment.PRODUCTION.ordinal()))
            .thenReturn(keyPair.getPublic());

        doAnswer(invocation -> tenantIds.add(TenantContext.getCurrentTenantId()))
            .when(signingKeyService)
            .updateLastUsedDate(anyString(), anyLong());

        HttpServletRequest httpServletRequest = mockRequest(
            "/api/embedded/" + MCP_SERVER_SECRET_KEY + "/mcp", "Bearer " + createJwt(keyPair));

        Authentication authentication = embeddedMcpServerApiKeyAuthenticationConverter.convert(httpServletRequest);

        EmbeddedMcpServerApiKeyAuthenticationToken embeddedMcpServerApiKeyAuthenticationToken =
            (EmbeddedMcpServerApiKeyAuthenticationToken) authentication;

        assertThat(embeddedMcpServerApiKeyAuthenticationToken.getExternalUserId()).isEqualTo("user-a");

        verify(signingKeyService).updateLastUsedDate(SIGNING_KEY_ID, Environment.PRODUCTION.ordinal());

        assertThat(tenantIds).containsExactly("tenant-b");
    }

    @Test
    void testConvertWithWronglySignedBearerTokenDoesNotRecordSigningKeyUse() throws NoSuchAlgorithmException {
        KeyPair keyPair = generateKeyPair();

        when(signingKeyService.getPublicKey(SIGNING_KEY_ID, Environment.PRODUCTION.ordinal()))
            .thenReturn(generateKeyPair().getPublic());

        HttpServletRequest httpServletRequest = mockRequest(
            "/api/embedded/" + MCP_SERVER_SECRET_KEY + "/mcp", "Bearer " + createJwt(keyPair));

        Authentication authentication = embeddedMcpServerApiKeyAuthenticationConverter.convert(httpServletRequest);

        EmbeddedMcpServerApiKeyAuthenticationToken embeddedMcpServerApiKeyAuthenticationToken =
            (EmbeddedMcpServerApiKeyAuthenticationToken) authentication;

        assertThat(embeddedMcpServerApiKeyAuthenticationToken.getExternalUserId()).isNull();

        verify(signingKeyService, never()).updateLastUsedDate(anyString(), anyLong());
    }

    @Test
    void testConvertRejectsPathSecretThatIsNotATenantKey() {
        HttpServletRequest httpServletRequest = mockRequest("/api/embedded/not-a-tenant-key/mcp", null);

        assertThatExceptionOfType(BadCredentialsException.class)
            .isThrownBy(() -> embeddedMcpServerApiKeyAuthenticationConverter.convert(httpServletRequest));
    }

    private static String createJwt(KeyPair keyPair) {
        return Jwts.builder()
            .header()
            .keyId(SIGNING_KEY_ID)
            .and()
            .subject("user-a")
            .signWith(keyPair.getPrivate())
            .compact();
    }

    private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

        keyPairGenerator.initialize(2048);

        return keyPairGenerator.generateKeyPair();
    }

    private static HttpServletRequest mockRequest(String servletPath, String authorization) {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);

        when(httpServletRequest.getServletPath()).thenReturn(servletPath);
        when(httpServletRequest.getHeader("Authorization")).thenReturn(authorization);

        return httpServletRequest;
    }
}

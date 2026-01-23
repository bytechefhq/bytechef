/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.configurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.tenant.TenantContext;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedApiKeyAuthenticationConverterTest {

    private EmbeddedApiKeyAuthenticationConverter converter;
    private HttpServletRequest request;
    private SigningKeyService signingKeyService;

    @BeforeEach
    void setUp() {
        signingKeyService = mock(SigningKeyService.class);

        converter = new EmbeddedApiKeyAuthenticationConverter(signingKeyService);
        request = mock(HttpServletRequest.class);
    }

    @Test
    void testConvertWithNullAuthorizationHeaderReturnsNull() {
        when(request.getHeader("Authorization")).thenReturn(null);

        Authentication result = converter.convert(request);

        assertThat(result).isNull();
    }

    @Test
    void testConvertWithEmptyBearerTokenReturnsNull() {
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        Authentication result = converter.convert(request);

        assertThat(result).isNull();
    }

    @Test
    void testConvertWithBlankBearerTokenReturnsNull() {
        when(request.getHeader("Authorization")).thenReturn("Bearer    ");

        Authentication result = converter.convert(request);

        assertThat(result).isNull();
    }

    @Test
    void testConvertWithNonJwtTokenAndInternalUrlThrowsIllegalArgumentException() {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(request.getRequestURI()).thenReturn("/api/platform/internal/some-endpoint");

        assertThatThrownBy(() -> converter.convert(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("externalUserId parameter is required");
    }

    @Test
    void testConvertWithNonJwtTokenAndValidUriReturnsAuthentication() {
        String tenantId = "test-tenant";
        String tenantKey = EncodingUtils.base64EncodeToString(tenantId + ":randomData");
        String externalUserId = "user123";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + tenantKey);
        when(request.getHeader("X-ENVIRONMENT")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/embedded/v1/" + externalUserId + "/endpoint");

        Authentication result = converter.convert(request);

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(EmbeddedApiKeyAuthenticationToken.class);

        EmbeddedApiKeyAuthenticationToken token = (EmbeddedApiKeyAuthenticationToken) result;

        assertThat(token.getExternalUserId()).isEqualTo(externalUserId);
        assertThat(token.getTenantId()).isEqualTo(tenantId);
        assertThat(token.getEnvironmentId()).isEqualTo(Environment.PRODUCTION.ordinal());
    }

    @Test
    void testConvertWithNonJwtTokenAndDevelopmentEnvironmentReturnsAuthentication() {
        String tenantId = "test-tenant";
        String tenantKey = EncodingUtils.base64EncodeToString(tenantId + ":randomData");
        String externalUserId = "user456";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + tenantKey);
        when(request.getHeader("X-ENVIRONMENT")).thenReturn("DEVELOPMENT");
        when(request.getRequestURI()).thenReturn("/api/embedded/v2/" + externalUserId + "/workflow");

        Authentication result = converter.convert(request);

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(EmbeddedApiKeyAuthenticationToken.class);

        EmbeddedApiKeyAuthenticationToken token = (EmbeddedApiKeyAuthenticationToken) result;

        assertThat(token.getExternalUserId()).isEqualTo(externalUserId);
        assertThat(token.getTenantId()).isEqualTo(tenantId);
        assertThat(token.getEnvironmentId()).isEqualTo(Environment.DEVELOPMENT.ordinal());
    }

    @Test
    void testConvertWithNonJwtTokenAndStagingEnvironmentReturnsAuthentication() {
        String tenantId = "staging-tenant";
        String tenantKey = EncodingUtils.base64EncodeToString(tenantId + ":randomData");
        String externalUserId = "staging-user";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + tenantKey);
        when(request.getHeader("X-ENVIRONMENT")).thenReturn("staging");
        when(request.getRequestURI()).thenReturn("/api/embedded/v1/" + externalUserId + "/connections");

        Authentication result = converter.convert(request);

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(EmbeddedApiKeyAuthenticationToken.class);

        EmbeddedApiKeyAuthenticationToken token = (EmbeddedApiKeyAuthenticationToken) result;

        assertThat(token.getExternalUserId()).isEqualTo(externalUserId);
        assertThat(token.getTenantId()).isEqualTo(tenantId);
        assertThat(token.getEnvironmentId()).isEqualTo(Environment.STAGING.ordinal());
    }

    @Test
    void testConvertWithJwtTokenReturnsAuthentication() throws NoSuchAlgorithmException {
        String tenantId = "jwt-tenant";
        String externalUserId = "jwt-user";
        String keyId = EncodingUtils.base64EncodeToString(tenantId + ":keyId");

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

        keyPairGenerator.initialize(2048);

        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String jwtToken = Jwts.builder()
            .header()
            .keyId(keyId)
            .and()
            .subject(externalUserId)
            .signWith(keyPair.getPrivate())
            .compact();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwtToken);
        when(request.getHeader("X-ENVIRONMENT")).thenReturn("PRODUCTION");
        when(signingKeyService.getPublicKey(anyString(), anyLong())).thenReturn(keyPair.getPublic());

        Authentication result = TenantContext.callWithTenantId(tenantId, () -> converter.convert(request));

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(EmbeddedApiKeyAuthenticationToken.class);

        EmbeddedApiKeyAuthenticationToken token = (EmbeddedApiKeyAuthenticationToken) result;

        assertThat(token.getExternalUserId()).isEqualTo(externalUserId);
        assertThat(token.getTenantId()).isEqualTo(tenantId);
        assertThat(token.getEnvironmentId()).isEqualTo(Environment.PRODUCTION.ordinal());
    }

    @Test
    void testJwtTokenPatternMatchesValidJwt() {
        String validJwt = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyMTIzIn0.signature";

        assertThat(EmbeddedApiKeyAuthenticationConverter.JWT_TOKEN_PATTERN.matcher(validJwt)
            .find()).isTrue();
    }

    @Test
    void testJwtTokenPatternDoesNotMatchPlainToken() {
        String plainToken = "not-a-jwt-token";

        assertThat(EmbeddedApiKeyAuthenticationConverter.JWT_TOKEN_PATTERN.matcher(plainToken)
            .find()).isFalse();
    }

    @Test
    void testExternalUserIdPatternMatchesValidUri() {
        String validUri = "/api/embedded/v1/user123/endpoint";

        assertThat(EmbeddedApiKeyAuthenticationConverter.EXTERNAL_USER_ID_PATTERN.matcher(validUri)
            .matches())
                .isTrue();
    }

    @Test
    void testExternalUserIdPatternDoesNotMatchInvalidUri() {
        String invalidUri = "/api/platform/internal/some-endpoint";

        assertThat(EmbeddedApiKeyAuthenticationConverter.EXTERNAL_USER_ID_PATTERN.matcher(invalidUri)
            .matches())
                .isFalse();
    }
}

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
import com.bytechef.ee.embedded.security.service.JwtTokenService;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.BadCredentialsException;
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
        JwtTokenService jwtTokenService = mock(JwtTokenService.class);
        signingKeyService = mock(SigningKeyService.class);

        converter = new EmbeddedApiKeyAuthenticationConverter(jwtTokenService, signingKeyService);
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
        String tenantId = "jwt_tenant";
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

    /**
     * Pins the Critical-severity fix: a non-JWT token on a no-{@code {externalUserId}} "Frontend" path (e.g. the public
     * catalog listing at {@code /api/embedded/v1/automation/projects}) used to have {@code EXTERNAL_USER_ID_PATTERN}
     * incidentally capture the literal segment {@code "automation"} as if it were an externalUserId, which
     * {@code EmbeddedApiKeyAuthenticationProvider}'s get-or-create then turned into a phantom {@code ConnectedUser}
     * row. The converter must now reject before a token is even produced, so the provider (and its get-or-create) is
     * never reached -- see {@code EmbeddedApiKeyAuthenticationConverterProviderIntegrationTest} for the
     * no-ConnectedUser-created proof.
     *
     * <p>
     * Exercises {@link EmbeddedApiKeyAuthenticationConverter#convert(HttpServletRequest)} for EVERY entry in
     * {@link com.bytechef.ee.embedded.connected.user.constant.ConnectedUserConstants#FRONTEND_RESERVED_PATH_SEGMENTS},
     * not just a couple of samples, so a future addition to the allowlist that isn't wired through {@code convert()}
     * correctly is caught immediately.
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "app-events", "automation", "components", "connections", "external", "integration-instances", "integrations",
        "me", "unified", "workflows"
    })
    void testConvertWithNonJwtTokenAndReservedSegmentThrowsBadCredentialsException(String reservedSegment) {
        String tenantKey = EncodingUtils.base64EncodeToString("test-tenant" + ":randomData");

        when(request.getHeader("Authorization")).thenReturn("Bearer " + tenantKey);
        when(request.getHeader("X-ENVIRONMENT")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/embedded/v1/" + reservedSegment + "/probe");

        assertThatThrownBy(() -> converter.convert(request))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testConvertWithRetiredExternalPrefixRouteThrowsBadCredentialsException() {
        // Regression pin for the phantom-connected-user fix: the MCP integration-instance controllers briefly
        // mounted routes under /external/{externalUserId}/..., so the converter captured the literal "external" as
        // the user id and the provider's get-or-create minted a phantom ConnectedUser named "external". The routes
        // are normalized to the bare /{externalUserId}/ prefix and "external" is reserved -- a stale caller of the
        // old URL must be rejected HERE, before the provider can create any row.
        String tenantKey = EncodingUtils.base64EncodeToString("test-tenant" + ":randomData");

        when(request.getHeader("Authorization")).thenReturn("Bearer " + tenantKey);
        when(request.getHeader("X-ENVIRONMENT")).thenReturn(null);
        when(request.getRequestURI())
            .thenReturn("/api/embedded/v1/external/user123/integration-instances/1/mcp-tools/2/enable");

        assertThatThrownBy(() -> converter.convert(request))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testConvertWithNonJwtTokenAndMeFrontendRouteThrowsIllegalArgumentException() {
        // "/me" has no trailing path segment, so EXTERNAL_USER_ID_PATTERN never matches it in the first place --
        // this pins that pre-existing, already-safe behavior stays unchanged by the reserved-segment check.
        String tenantKey = EncodingUtils.base64EncodeToString("test-tenant" + ":randomData");

        when(request.getHeader("Authorization")).thenReturn("Bearer " + tenantKey);
        when(request.getHeader("X-ENVIRONMENT")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/embedded/v1/me");

        assertThatThrownBy(() -> converter.convert(request))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testConvertWithNonJwtTokenAndRealExternalUserIdPathIsUnchanged() {
        // Regression guard: a genuine server-to-server /{externalUserId}/ path must keep working, even though its
        // shape (first-segment-after-/v<n>/, followed by more segments) is otherwise identical to the Frontend case.
        String tenantId = "test-tenant";
        String tenantKey = EncodingUtils.base64EncodeToString(tenantId + ":randomData");
        String externalUserId = "real-external-user-42";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + tenantKey);
        when(request.getHeader("X-ENVIRONMENT")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/embedded/v1/" + externalUserId + "/automation/projects");

        Authentication result = converter.convert(request);

        assertThat(result).isNotNull();

        EmbeddedApiKeyAuthenticationToken token = (EmbeddedApiKeyAuthenticationToken) result;

        assertThat(token.getExternalUserId()).isEqualTo(externalUserId);
        assertThat(token.getTenantId()).isEqualTo(tenantId);
    }
}

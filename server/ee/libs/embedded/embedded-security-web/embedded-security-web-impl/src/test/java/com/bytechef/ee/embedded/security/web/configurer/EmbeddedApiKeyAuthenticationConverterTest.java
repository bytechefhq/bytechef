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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.service.ApiKeyService;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedApiKeyAuthenticationConverterTest {

    private static final String JWT_TENANT_ID = "jwt_tenant";

    private EmbeddedApiKeyAuthenticationConverter converter;
    private JwtTokenService jwtTokenService;
    private HttpServletRequest request;
    private SigningKeyService signingKeyService;

    @BeforeEach
    void setUp() {
        jwtTokenService = mock(JwtTokenService.class);
        signingKeyService = mock(SigningKeyService.class);

        converter = new EmbeddedApiKeyAuthenticationConverter(jwtTokenService, signingKeyService);
        request = mock(HttpServletRequest.class);
    }

    @NullSource
    @ParameterizedTest
    @ValueSource(strings = {
        "Bearer ", "Bearer    "
    })
    void testConvertWithMissingOrBlankBearerTokenReturnsNull(String authorizationHeader) {
        when(request.getHeader("Authorization")).thenReturn(authorizationHeader);

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
        Authentication result = convertWithJwt("jwt-user", "/api/embedded/v1/me");

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(EmbeddedApiKeyAuthenticationToken.class);

        EmbeddedApiKeyAuthenticationToken token = (EmbeddedApiKeyAuthenticationToken) result;

        assertThat(token.getExternalUserId()).isEqualTo("jwt-user");
        assertThat(token.getTenantId()).isEqualTo(JWT_TENANT_ID);
        assertThat(token.getEnvironmentId()).isEqualTo(Environment.PRODUCTION.ordinal());
    }

    @Test
    void testConvertWithJwtTokenOnAnotherExternalUserPathThrowsBadCredentialsException() {
        assertThatThrownBy(() -> convertWithJwt("user-a", "/api/embedded/v1/user-b/automation/workflows"))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testConvertWithJwtTokenOnAnotherExternalUserBarePathThrowsBadCredentialsException() {
        assertThatThrownBy(() -> convertWithJwt("user-a", "/api/embedded/v1/user-b"))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testConvertWithJwtTokenWithoutSubjectOnExternalUserPathThrowsBadCredentialsException() {
        assertThatThrownBy(() -> convertWithJwt(null, "/api/embedded/v1/user-b/automation/workflows"))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testConvertWithJwtTokenOnOwnExternalUserPathReturnsAuthentication() throws NoSuchAlgorithmException {
        Authentication result = convertWithJwt(
            "user-a", "/api/embedded/v1/user-a/automation/workflow-templates/uuid-1/provision");

        EmbeddedApiKeyAuthenticationToken token = (EmbeddedApiKeyAuthenticationToken) result;

        assertThat(token.getExternalUserId()).isEqualTo("user-a");
    }

    @Test
    void testConvertWithJwtTokenOnOwnPercentEncodedExternalUserPathReturnsAuthentication()
        throws NoSuchAlgorithmException {

        Authentication result = convertWithJwt(
            "user@example.com", "/api/embedded/v1/user%40example.com/automation/workflows");

        EmbeddedApiKeyAuthenticationToken token = (EmbeddedApiKeyAuthenticationToken) result;

        assertThat(token.getExternalUserId()).isEqualTo("user@example.com");
    }

    @Test
    void testConvertWithJwtTokenOnAnotherPercentEncodedExternalUserPathThrowsBadCredentialsException() {
        assertThatThrownBy(() -> convertWithJwt("user-a", "/api/embedded/v1/user%2Db/automation/workflows"))
            .isInstanceOf(BadCredentialsException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "app-events", "automation", "components", "connections", "external", "integration-instances", "integrations",
        "me", "unified", "workflows"
    })
    void testConvertWithJwtTokenOnReservedFrontendPathReturnsAuthentication(String reservedSegment)
        throws NoSuchAlgorithmException {

        Authentication result = convertWithJwt("user-a", "/api/embedded/v1/" + reservedSegment + "/probe");

        EmbeddedApiKeyAuthenticationToken token = (EmbeddedApiKeyAuthenticationToken) result;

        assertThat(token.getExternalUserId()).isEqualTo("user-a");
    }

    @Test
    void testConvertWithJwtTokenOnInternalPathReturnsAuthentication() throws NoSuchAlgorithmException {
        Authentication result = convertWithJwt("user-a", "/api/embedded/internal/connected-users/7/connections");

        EmbeddedApiKeyAuthenticationToken token = (EmbeddedApiKeyAuthenticationToken) result;

        assertThat(token.getExternalUserId()).isEqualTo("user-a");
    }

    @Test
    void testConvertWithServerMintedJwtTokenForAnotherEnvironmentThrowsBadCredentialsException()
        throws NoSuchAlgorithmException {

        String jwtToken = createServerMintedJwt("user-a", Environment.DEVELOPMENT.ordinal());

        assertThatThrownBy(() -> convertWithJwt(jwtToken, "/api/embedded/v1/connections", "PRODUCTION"))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testConvertWithServerMintedJwtTokenForTheRequestEnvironmentReturnsAuthentication()
        throws NoSuchAlgorithmException {

        String jwtToken = createServerMintedJwt("user-a", Environment.STAGING.ordinal());

        EmbeddedApiKeyAuthenticationToken token = (EmbeddedApiKeyAuthenticationToken) convertWithJwt(
            jwtToken, "/api/embedded/v1/connections", "STAGING");

        assertThat(token.getExternalUserId()).isEqualTo("user-a");
        assertThat(token.getEnvironmentId()).isEqualTo(Environment.STAGING.ordinal());
    }

    @Test
    void testConvertWithJwtTokenWithNonNumericEnvironmentClaimThrowsBadCredentialsException()
        throws NoSuchAlgorithmException {

        String jwtToken = createServerMintedJwt("user-a", "PRODUCTION");

        assertThatThrownBy(() -> convertWithJwt(jwtToken, "/api/embedded/v1/connections", "PRODUCTION"))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testConvertWithJwtTokenWithoutEnvironmentClaimUsesTheRequestEnvironment() throws NoSuchAlgorithmException {
        KeyPair keyPair = generateKeyPair();

        String jwtToken = Jwts.builder()
            .header()
            .keyId(EncodingUtils.base64EncodeToString(JWT_TENANT_ID + ":keyId"))
            .and()
            .subject("user-a")
            .signWith(keyPair.getPrivate())
            .compact();

        when(signingKeyService.getPublicKey(anyString(), anyLong())).thenReturn(keyPair.getPublic());

        EmbeddedApiKeyAuthenticationToken token = (EmbeddedApiKeyAuthenticationToken) convertWithJwt(
            jwtToken, "/api/embedded/v1/connections", "DEVELOPMENT");

        assertThat(token.getEnvironmentId()).isEqualTo(Environment.DEVELOPMENT.ordinal());
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
        String tenantKey = EncodingUtils.base64EncodeToString("test-tenant" + ":randomData");

        when(request.getHeader("Authorization")).thenReturn("Bearer " + tenantKey);
        when(request.getHeader("X-ENVIRONMENT")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/embedded/v1/me");

        assertThatThrownBy(() -> converter.convert(request))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testConvertWithNonJwtTokenAndRealExternalUserIdPathIsUnchanged() {
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

    @Test
    void testFrontendPathNeverReachesProviderSoNoConnectedUserIsCreated() {
        ApiKeyService apiKeyService = mock(ApiKeyService.class);
        ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
        String tenantKey = EncodingUtils.base64EncodeToString("test-tenant" + ":randomData");

        when(request.getHeader("Authorization")).thenReturn("Bearer " + tenantKey);
        when(request.getHeader("X-ENVIRONMENT")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/embedded/v1/automation/projects");

        assertThatThrownBy(() -> converter.convert(request))
            .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(connectedUserService);
        verifyNoInteractions(apiKeyService);
    }

    private Authentication convertWithJwt(String jwtToken, String requestUri, String environment) {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwtToken);
        when(request.getHeader("X-ENVIRONMENT")).thenReturn(environment);
        when(request.getRequestURI()).thenReturn(requestUri);

        return converter.convert(request);
    }

    private String createServerMintedJwt(String subject, Object environmentIdClaim) throws NoSuchAlgorithmException {
        String keyId = EncodingUtils.base64EncodeToString(JWT_TENANT_ID + ":server");

        KeyPair keyPair = generateKeyPair();

        when(jwtTokenService.getPublicKey(keyId)).thenReturn(keyPair.getPublic());

        return Jwts.builder()
            .header()
            .keyId(keyId)
            .and()
            .subject(subject)
            .claim(JwtTokenService.ENVIRONMENT_ID_CLAIM, environmentIdClaim)
            .claim("integrationId", 1L)
            .signWith(keyPair.getPrivate())
            .compact();
    }

    private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

        keyPairGenerator.initialize(2048);

        return keyPairGenerator.generateKeyPair();
    }

    private Authentication convertWithJwt(String subject, String requestUri) throws NoSuchAlgorithmException {
        String keyId = EncodingUtils.base64EncodeToString(JWT_TENANT_ID + ":keyId");

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

        keyPairGenerator.initialize(2048);

        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        JwtBuilder jwtBuilder = Jwts.builder()
            .header()
            .keyId(keyId)
            .and()
            .id("token-id");

        if (subject != null) {
            jwtBuilder = jwtBuilder.subject(subject);
        }

        String jwtToken = jwtBuilder.signWith(keyPair.getPrivate())
            .compact();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwtToken);
        when(request.getHeader("X-ENVIRONMENT")).thenReturn("PRODUCTION");
        when(request.getRequestURI()).thenReturn(requestUri);
        when(signingKeyService.getPublicKey(anyString(), anyLong())).thenReturn(keyPair.getPublic());

        return converter.convert(request);
    }
}

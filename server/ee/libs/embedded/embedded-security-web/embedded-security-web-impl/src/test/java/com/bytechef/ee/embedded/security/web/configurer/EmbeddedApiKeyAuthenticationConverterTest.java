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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedApiKeyAuthenticationConverterTest {

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
    void testConvertWithNonJwtTokenAndInternalUrlIsRefused() {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(request.getRequestURI()).thenReturn("/api/platform/internal/some-endpoint");

        assertThatThrownBy(() -> converter.convert(request))
            .isInstanceOf(BadCredentialsException.class);
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
    void testConvertWithNonJwtTokenTakesTheExternalUserIdOfAnExternalMcpInstanceRoute() {
        String tenantKey = EncodingUtils.base64EncodeToString("test-tenant:randomData");

        when(request.getHeader("Authorization")).thenReturn("Bearer " + tenantKey);
        when(request.getHeader("X-ENVIRONMENT")).thenReturn(null);
        when(request.getRequestURI())
            .thenReturn("/api/embedded/v1/external/user123/integration-instances/5/mcp-tools/7/enable");

        EmbeddedApiKeyAuthenticationToken token = (EmbeddedApiKeyAuthenticationToken) converter.convert(request);

        assertThat(token.getExternalUserId()).isEqualTo("user123");
    }

    @Test
    void testConvertWithNonJwtTokenKeepsAnExternalUserNamedExternal() {
        String tenantKey = EncodingUtils.base64EncodeToString("test-tenant:randomData");

        when(request.getHeader("Authorization")).thenReturn("Bearer " + tenantKey);
        when(request.getHeader("X-ENVIRONMENT")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/embedded/v1/external/integration-instances/5");

        EmbeddedApiKeyAuthenticationToken token = (EmbeddedApiKeyAuthenticationToken) converter.convert(request);

        assertThat(token.getExternalUserId()).isEqualTo("external");
    }

    @Test
    void testConvertWithNonJwtTokenDecodesAnEncodedExternalUserId() {
        String tenantKey = EncodingUtils.base64EncodeToString("test-tenant:randomData");

        when(request.getHeader("Authorization")).thenReturn("Bearer " + tenantKey);
        when(request.getHeader("X-ENVIRONMENT")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/embedded/v1/user%40example.com/tools");

        EmbeddedApiKeyAuthenticationToken token = (EmbeddedApiKeyAuthenticationToken) converter.convert(request);

        assertThat(token.getExternalUserId()).isEqualTo("user@example.com");
    }

    @Test
    void testConvertWithNonJwtTokenDecodesAnEncodedExternalUserIdOfAnExternalMcpInstanceRoute() {
        String tenantKey = EncodingUtils.base64EncodeToString("test-tenant:randomData");

        when(request.getHeader("Authorization")).thenReturn("Bearer " + tenantKey);
        when(request.getHeader("X-ENVIRONMENT")).thenReturn(null);
        when(request.getRequestURI())
            .thenReturn("/api/embedded/v1/external/user%40example.com/integration-instances/5/mcp-tools/7/enable");

        EmbeddedApiKeyAuthenticationToken token = (EmbeddedApiKeyAuthenticationToken) converter.convert(request);

        assertThat(token.getExternalUserId()).isEqualTo("user@example.com");
    }

    @Test
    void testConvertWithNonJwtTokenAcceptsAPathWithoutTrailingSegment() {
        assertThat(convertApiKeyRequest("/api/embedded/v1/alice")).isEqualTo("alice");
    }

    @Test
    void testConvertWithNonJwtTokenTakesTheFirstSegmentWhenExternalAppearsLater() {
        assertThat(convertApiKeyRequest("/api/embedded/v1/alice/automation/external/bob/integration-instances/1"))
            .isEqualTo("alice");
    }

    @Test
    void testConvertWithNonJwtTokenTakesTheFirstSegmentWhenALaterSegmentLooksLikeAVersion() {
        assertThat(convertApiKeyRequest("/api/embedded/v1/alice/v2/bob/tools")).isEqualTo("alice");
    }

    @Test
    void testConvertWithNonJwtTokenRespectsTheServletContextPath() {
        when(request.getContextPath()).thenReturn("/app");

        assertThat(convertApiKeyRequest("/app/api/embedded/v1/alice/tools")).isEqualTo("alice");
    }

    @Test
    void testConvertWithNonJwtTokenRefusesTheConnectedUserOnlyWebhookRoutes() {
        assertThatThrownBy(() -> convertApiKeyRequest("/api/embedded/v1/app-events"))
            .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> convertApiKeyRequest("/api/embedded/v1/workflows/workflow-uuid"))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testConvertWithNonJwtTokenKeepsExternalUsersNamedLikeTheWebhookRoutes() {
        assertThat(convertApiKeyRequest("/api/embedded/v1/workflows/connections/5")).isEqualTo("workflows");
        assertThat(convertApiKeyRequest("/api/embedded/v1/app-events/tools")).isEqualTo("app-events");
    }

    @Test
    void testConvertWithNonJwtTokenRefusesAMalformedEncodedExternalUserId() {
        assertThatThrownBy(() -> convertApiKeyRequest("/api/embedded/v1/%zz/tools"))
            .isInstanceOf(BadCredentialsException.class);
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

        assertThat(EmbeddedApiKeyAuthenticationConverter.API_KEY_PATH_PATTERN.matcher(validUri)
            .matches())
                .isTrue();
    }

    @Test
    void testExternalUserIdPatternDoesNotMatchInvalidUri() {
        String invalidUri = "/api/platform/internal/some-endpoint";

        assertThat(EmbeddedApiKeyAuthenticationConverter.API_KEY_PATH_PATTERN.matcher(invalidUri)
            .matches())
                .isFalse();
    }

    @Test
    void testJwtEnvironmentClaimWinsAndMismatchingHeaderIsRefused() throws NoSuchAlgorithmException {
        String jwt = issueBuilderJwt("ext-1", 0);

        MockHttpServletRequest noHeader = requestWithBearer(jwt, null);

        assertThat(((EmbeddedApiKeyAuthenticationToken) converter.convert(noHeader)).getEnvironmentId())
            .isEqualTo(0L);

        MockHttpServletRequest matching = requestWithBearer(jwt, "DEVELOPMENT");

        assertThat(((EmbeddedApiKeyAuthenticationToken) converter.convert(matching)).getEnvironmentId())
            .isEqualTo(0L);

        MockHttpServletRequest mismatching = requestWithBearer(jwt, "PRODUCTION");

        assertThatThrownBy(() -> converter.convert(mismatching)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testCustomerSignedJwtWithoutClaimKeepsHeaderEnvironment() throws NoSuchAlgorithmException {
        String jwt = issueCustomerJwt("ext-1", 1L, null);

        MockHttpServletRequest staging = requestWithBearer(jwt, "STAGING");

        assertThat(((EmbeddedApiKeyAuthenticationToken) converter.convert(staging)).getEnvironmentId())
            .isEqualTo(1L);
    }

    @Test
    void testCustomerSignedJwtClaimingAnotherEnvironmentThanItsKeyIsRefused() throws NoSuchAlgorithmException {
        String jwt = issueCustomerJwt("ext-1", 2L, 0);

        MockHttpServletRequest withoutHeader = requestWithBearer(jwt, null);

        assertThatThrownBy(() -> converter.convert(withoutHeader)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testCustomerSignedJwtClaimingItsKeyEnvironmentIsAccepted() throws NoSuchAlgorithmException {
        String jwt = issueCustomerJwt("ext-1", 2L, 2);

        MockHttpServletRequest withoutHeader = requestWithBearer(jwt, null);

        assertThat(((EmbeddedApiKeyAuthenticationToken) converter.convert(withoutHeader)).getEnvironmentId())
            .isEqualTo(2L);
    }

    private String issueBuilderJwt(String externalUserId, int environmentIdClaim) throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

        keyPairGenerator.initialize(2048);

        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String keyId = EncodingUtils.base64EncodeToString("builder-tenant" + ":builder-key-id");

        when(jwtTokenService.getPublicKey(keyId)).thenReturn(keyPair.getPublic());

        return Jwts.builder()
            .header()
            .keyId(keyId)
            .and()
            .subject(externalUserId)
            .claim("environmentId", environmentIdClaim)
            .signWith(keyPair.getPrivate())
            .compact();
    }

    private String issueCustomerJwt(String externalUserId, long keyEnvironmentId, Integer environmentIdClaim)
        throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

        keyPairGenerator.initialize(2048);

        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String keyId = EncodingUtils.base64EncodeToString("customer-tenant" + ":keyId");

        when(signingKeyService.getPublicKey(keyId, keyEnvironmentId)).thenReturn(keyPair.getPublic());

        return Jwts.builder()
            .header()
            .keyId(keyId)
            .and()
            .subject(externalUserId)
            .claim("environmentId", environmentIdClaim)
            .signWith(keyPair.getPrivate())
            .compact();
    }

    private static MockHttpServletRequest requestWithBearer(String jwt, String environmentHeader) {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

        mockHttpServletRequest.addHeader("Authorization", "Bearer " + jwt);

        if (environmentHeader != null) {
            mockHttpServletRequest.addHeader("X-ENVIRONMENT", environmentHeader);
        }

        return mockHttpServletRequest;
    }

    private String convertApiKeyRequest(String requestURI) {
        String tenantKey = EncodingUtils.base64EncodeToString("test-tenant:randomData");

        when(request.getHeader("Authorization")).thenReturn("Bearer " + tenantKey);
        when(request.getHeader("X-ENVIRONMENT")).thenReturn(null);
        when(request.getRequestURI()).thenReturn(requestURI);

        EmbeddedApiKeyAuthenticationToken token = (EmbeddedApiKeyAuthenticationToken) converter.convert(request);

        return token.getExternalUserId();
    }
}

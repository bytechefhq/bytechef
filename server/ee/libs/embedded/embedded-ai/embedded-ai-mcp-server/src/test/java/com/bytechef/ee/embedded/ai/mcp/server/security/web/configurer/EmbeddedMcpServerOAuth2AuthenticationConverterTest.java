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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpServerOAuth2AuthenticationToken;
import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpTrustedIssuerResolver;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantSecretVerifier;
import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtDecoderFactory;
import com.bytechef.tenant.domain.TenantKey;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class EmbeddedMcpServerOAuth2AuthenticationConverterTest {

    private static final String EMBEDDED_SCOPE = "mcp:embedded";
    private static final String EXTERNAL_USER_ID = "ext-user-1";
    private static final String ISSUER_URI = "https://idp.customer.test";
    private static final String TENANT_ID = "public";
    private static final String MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of(TENANT_ID));
    private static final String SERVLET_PATH = "/api/embedded/" + MCP_SERVER_SECRET_KEY + "/mcp";

    private final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);
    private final KeyPair keyPair = generateRsaKeyPair();
    private final KeyPair otherKeyPair = generateRsaKeyPair();
    private final AtomicBoolean secretVerified = new AtomicBoolean(true);

    @Test
    void testReturnsNullWhenNoAuthorizationHeader() {
        assertThat(converter(identityProvider(Map.of(), false)).convert(request(null))).isNull();
    }

    @Test
    void testReturnsNullForNonJwtBearer() {
        assertThat(converter(identityProvider(Map.of(), false)).convert(request("an-opaque-token-without-dots")))
            .isNull();
    }

    @Test
    void testReturnsNullForUntrustedIssuer() {
        String token = sign(claims("https://evil.example.com", EMBEDDED_SCOPE), keyPair);

        assertThat(converter(identityProvider(Map.of(), false)).convert(request(token))).isNull();
    }

    @Test
    void testReturnsNullWithoutIdentityProviderLookupWhenTheSecretIsNotVerified() {
        EmbeddedMcpServerOAuth2AuthenticationConverter converter = converter(identityProvider(Map.of(), false));

        secretVerified.set(false);

        String token = sign(claims(ISSUER_URI, EMBEDDED_SCOPE), keyPair);

        assertThat(converter.convert(request(token))).isNull();

        verify(identityProviderService, never()).getIdentityProviders();
    }

    @Test
    void testRejectsTrustedIssuerWithBadSignature() {
        String token = sign(claims(ISSUER_URI, EMBEDDED_SCOPE), otherKeyPair);

        EmbeddedMcpServerOAuth2AuthenticationConverter converter = converter(identityProvider(Map.of(), false));

        assertThatExceptionOfType(BadCredentialsException.class)
            .isThrownBy(() -> converter.convert(request(token)));
    }

    @Test
    void testRejectsTokenWithoutTheEmbeddedScope() {
        String token = sign(claims(ISSUER_URI, "openid email"), keyPair);

        EmbeddedMcpServerOAuth2AuthenticationConverter converter = converter(identityProvider(Map.of(), false));

        assertThatExceptionOfType(EmbeddedMcpInsufficientScopeException.class)
            .isThrownBy(() -> converter.convert(request(token)));
    }

    @Test
    void testRejectsTokenScopedForAnotherSurface() {
        String token = sign(claims(ISSUER_URI, "mcp:automation"), keyPair);

        EmbeddedMcpServerOAuth2AuthenticationConverter converter = converter(identityProvider(Map.of(), false));

        assertThatExceptionOfType(EmbeddedMcpInsufficientScopeException.class)
            .isThrownBy(() -> converter.convert(request(token)));
    }

    @Test
    void testConvertsValidToken() {
        String token = sign(claims(ISSUER_URI, EMBEDDED_SCOPE), keyPair);

        Authentication authentication = converter(identityProvider(Map.of(), false)).convert(request(token));

        assertThat(authentication).isInstanceOf(EmbeddedMcpServerOAuth2AuthenticationToken.class);

        EmbeddedMcpServerOAuth2AuthenticationToken oAuth2AuthenticationToken =
            (EmbeddedMcpServerOAuth2AuthenticationToken) authentication;

        assertThat(oAuth2AuthenticationToken.getExternalUserId()).isEqualTo(EXTERNAL_USER_ID);
        assertThat(oAuth2AuthenticationToken.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(oAuth2AuthenticationToken.getMcpServerSecretKey()).isEqualTo(MCP_SERVER_SECRET_KEY);
        assertThat(oAuth2AuthenticationToken.getEnvironmentId()).isEqualTo(Environment.PRODUCTION.ordinal());
    }

    @Test
    void testMapsGroupClaimToAuthorities() {
        String token = sign(
            claims(ISSUER_URI, EMBEDDED_SCOPE).claim("groups", List.of("editors", "unmapped")), keyPair);

        EmbeddedMcpServerOAuth2AuthenticationToken oAuth2AuthenticationToken =
            (EmbeddedMcpServerOAuth2AuthenticationToken) Objects.requireNonNull(
                converter(identityProvider(Map.of("editors", "ROLE_EDITOR"), false)).convert(request(token)),
                "converted authentication");

        assertThat(authorityNames(oAuth2AuthenticationToken)).containsExactly("ROLE_EDITOR");
    }

    @Test
    void testNeverGrantsScopeAuthoritiesFromGroupMappings() {
        String token = sign(
            claims(ISSUER_URI, EMBEDDED_SCOPE).claim("groups", List.of("admins", "SCOPE_mcp:embedded")), keyPair);

        EmbeddedMcpServerOAuth2AuthenticationToken oAuth2AuthenticationToken =
            (EmbeddedMcpServerOAuth2AuthenticationToken) Objects.requireNonNull(
                converter(identityProvider(Map.of("admins", "SCOPE_mcp:management"), false)).convert(request(token)),
                "converted authentication");

        assertThat(authorityNames(oAuth2AuthenticationToken)).isEmpty();
    }

    @Test
    void testRejectsAudienceBoundTokenForAnotherAudience() {
        String token = sign(claims(ISSUER_URI, EMBEDDED_SCOPE).audience("https://other.test/mcp"), keyPair);

        EmbeddedMcpServerOAuth2AuthenticationConverter converter = converter(identityProvider(Map.of(), true));

        assertThatExceptionOfType(BadCredentialsException.class)
            .isThrownBy(() -> converter.convert(request(token)));
    }

    @Test
    void testAcceptsAudienceBoundTokenForTheEndpoint() {
        String token = sign(claims(ISSUER_URI, EMBEDDED_SCOPE).audience("http://localhost" + SERVLET_PATH), keyPair);

        assertThat(converter(identityProvider(Map.of(), true)).convert(request(token)))
            .isInstanceOf(EmbeddedMcpServerOAuth2AuthenticationToken.class);
    }

    private static List<String> authorityNames(EmbeddedMcpServerOAuth2AuthenticationToken authenticationToken) {
        return authenticationToken.getMappedAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .toList();
    }

    private EmbeddedMcpServerOAuth2AuthenticationConverter converter(IdentityProvider identityProvider) {
        when(identityProviderService.getIdentityProviders()).thenReturn(List.of(identityProvider));

        return new EmbeddedMcpServerOAuth2AuthenticationConverter(
            new EmbeddedMcpTrustedIssuerResolver(new McpTenantIssuerResolver(identityProviderService)),
            () -> List.of(Environment.values()), decoderFactory(),
            new McpTenantSecretVerifier(() -> List.of(request -> Optional.of(secretVerified.get()))), null);
    }

    private static IdentityProvider identityProvider(
        Map<String, String> authorityMappings, boolean validateMcpAudience) {

        IdentityProvider identityProvider = mock(IdentityProvider.class);

        when(identityProvider.isEnabled()).thenReturn(true);
        when(identityProvider.isMcpEmbedded()).thenReturn(true);
        when(identityProvider.getIssuerUri()).thenReturn(ISSUER_URI);
        when(identityProvider.getAuthorityMappings()).thenReturn(authorityMappings);
        when(identityProvider.isValidateMcpAudience()).thenReturn(validateMcpAudience);

        return identityProvider;
    }

    private McpJwtDecoderFactory decoderFactory() {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        return issuer -> {
            if (!ISSUER_URI.equals(issuer)) {
                return null;
            }

            NimbusJwtDecoder nimbusJwtDecoder = NimbusJwtDecoder.withPublicKey(publicKey)
                .build();

            nimbusJwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));

            return (JwtDecoder) nimbusJwtDecoder;
        };
    }

    private static MockHttpServletRequest request(String bearerToken) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", SERVLET_PATH);

        request.setServletPath(SERVLET_PATH);

        if (bearerToken != null) {
            request.addHeader("Authorization", "Bearer " + bearerToken);
        }

        return request;
    }

    private static JWTClaimsSet.Builder claims(String issuer, String scope) {
        return new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject(EXTERNAL_USER_ID)
            .claim("scope", scope)
            .issueTime(Date.from(Instant.now()))
            .expirationTime(Date.from(Instant.now()
                .plus(5, ChronoUnit.MINUTES)));
    }

    private static String sign(JWTClaimsSet.Builder claimsSetBuilder, KeyPair signingKeyPair) {
        try {
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSetBuilder.build());

            signedJWT.sign(new RSASSASigner((RSAPrivateKey) signingKeyPair.getPrivate()));

            return signedJWT.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

            keyPairGenerator.initialize(2048);

            return keyPairGenerator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}

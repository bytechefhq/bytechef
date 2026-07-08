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

import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpServerOAuth2AuthenticationToken;
import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpTrustedIssuerResolver;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver;
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

    private static final String ISSUER_URI = "https://idp.customer.test";
    private static final String TENANT_ID = "public";

    private final KeyPair keyPair = generateRsaKeyPair();
    private final KeyPair otherKeyPair = generateRsaKeyPair();

    private final EmbeddedMcpServerOAuth2AuthenticationConverter converter =
        new EmbeddedMcpServerOAuth2AuthenticationConverter(trustedIssuerResolver(), decoderFactory());

    @Test
    void testReturnsNullWhenNoAuthorizationHeader() {
        assertThat(converter.convert(request(null))).isNull();
    }

    @Test
    void testReturnsNullForNonJwtBearer() {
        assertThat(converter.convert(request("an-opaque-token-without-dots"))).isNull();
    }

    @Test
    void testReturnsNullForUntrustedIssuer() {
        String token = signJwt("https://evil.example.com", "ext-user-1", keyPair);

        assertThat(converter.convert(request(token))).isNull();
    }

    @Test
    void testRejectsTrustedIssuerWithBadSignature() {
        String token = signJwt(ISSUER_URI, "ext-user-1", otherKeyPair);

        assertThatExceptionOfType(BadCredentialsException.class)
            .isThrownBy(() -> converter.convert(request(token)));
    }

    @Test
    void testConvertsValidToken() {
        String token = signJwt(ISSUER_URI, "ext-user-1", keyPair);

        Authentication authentication = converter.convert(request(token));

        assertThat(authentication).isInstanceOf(EmbeddedMcpServerOAuth2AuthenticationToken.class);

        EmbeddedMcpServerOAuth2AuthenticationToken oAuth2AuthenticationToken =
            (EmbeddedMcpServerOAuth2AuthenticationToken) authentication;

        assertThat(oAuth2AuthenticationToken.getExternalUserId()).isEqualTo("ext-user-1");
        assertThat(oAuth2AuthenticationToken.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(oAuth2AuthenticationToken.getEnvironmentId()).isEqualTo(Environment.PRODUCTION.ordinal());
    }

    @Test
    void testMapsGroupClaimToAuthorities() {
        EmbeddedMcpServerOAuth2AuthenticationConverter mappingConverter =
            new EmbeddedMcpServerOAuth2AuthenticationConverter(
                trustedIssuerResolver(Map.of("editors", "ROLE_EDITOR")), decoderFactory());

        String token = signJwtWithGroups(ISSUER_URI, "ext-user-1", keyPair, List.of("editors", "unmapped"));

        EmbeddedMcpServerOAuth2AuthenticationToken oAuth2AuthenticationToken =
            (EmbeddedMcpServerOAuth2AuthenticationToken) mappingConverter.convert(request(token));

        assertThat(
            oAuth2AuthenticationToken.getMappedAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList()).containsExactly("ROLE_EDITOR");
    }

    private EmbeddedMcpTrustedIssuerResolver trustedIssuerResolver() {
        return trustedIssuerResolver(Map.of());
    }

    private EmbeddedMcpTrustedIssuerResolver trustedIssuerResolver(Map<String, String> authorityMappings) {
        IdentityProviderService identityProviderService = mock(IdentityProviderService.class);

        IdentityProvider identityProvider = mock(IdentityProvider.class);

        when(identityProvider.isEnabled()).thenReturn(true);
        when(identityProvider.isMcpEmbedded()).thenReturn(true);
        when(identityProvider.getIssuerUri()).thenReturn(ISSUER_URI);
        when(identityProvider.getAuthorityMappings()).thenReturn(authorityMappings);

        when(identityProviderService.getIdentityProviders()).thenReturn(List.of(identityProvider));

        return new EmbeddedMcpTrustedIssuerResolver(new McpTenantIssuerResolver(identityProviderService), null);
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
        String servletPath = "/api/embedded/" + TenantKey.of(TENANT_ID) + "/mcp";

        MockHttpServletRequest request = new MockHttpServletRequest("POST", servletPath);

        request.setServletPath(servletPath);

        if (bearerToken != null) {
            request.addHeader("Authorization", "Bearer " + bearerToken);
        }

        return request;
    }

    private static String signJwt(String issuer, String subject, KeyPair signingKeyPair) {
        return sign(
            new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(subject),
            signingKeyPair);
    }

    private static String signJwtWithGroups(
        String issuer, String subject, KeyPair signingKeyPair, List<String> groups) {

        return sign(
            new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(subject)
                .claim("groups", groups),
            signingKeyPair);
    }

    private static String sign(JWTClaimsSet.Builder claimsSetBuilder, KeyPair signingKeyPair) {
        try {
            JWTClaimsSet claimsSet = claimsSetBuilder
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now()
                    .plus(5, ChronoUnit.MINUTES)))
                .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);

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

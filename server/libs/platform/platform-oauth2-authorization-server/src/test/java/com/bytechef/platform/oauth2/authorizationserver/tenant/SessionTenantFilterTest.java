/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.oauth2.authorizationserver.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import jakarta.servlet.FilterChain;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * @author Ivica Cardic
 */
class SessionTenantFilterTest {

    private static final KeyPair SIGNING_KEY_PAIR = generateRsaKeyPair();

    private final List<String> decodedTokens = new ArrayList<>();
    private final JwtDecoder signingKeyJwtDecoder = NimbusJwtDecoder.withPublicKey(
        (RSAPublicKey) SIGNING_KEY_PAIR.getPublic())
        .build();
    private final JwtDecoder jwtDecoder = token -> {
        decodedTokens.add(token);

        return signingKeyJwtDecoder.decode(token);
    };

    private final SessionTenantFilter sessionTenantFilter = new SessionTenantFilter(
        "/oauth2/token", "/oauth2/revoke", "/oauth2/introspect", jwtDecoder);

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testAuthorizeRequestRunsInSessionTenant() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorize");
        MockHttpSession session = new MockHttpSession();

        session.setAttribute(TenantConstants.CURRENT_TENANT_ID, "acme");

        request.setSession(session);

        assertThat(filter(request)).containsExactly("acme");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testTokenRequestRunsInTenantCarriedByCodeOrRefreshToken() throws Exception {
        MockHttpServletRequest codeRequest = new MockHttpServletRequest("POST", "/oauth2/token");

        codeRequest.setParameter(OAuth2ParameterNames.GRANT_TYPE, "authorization_code");
        codeRequest.setParameter(OAuth2ParameterNames.CODE, TenantKeyedTokenValues.generate("globex"));

        MockHttpServletRequest refreshRequest = new MockHttpServletRequest("POST", "/oauth2/token");

        refreshRequest.setParameter(OAuth2ParameterNames.GRANT_TYPE, "refresh_token");
        refreshRequest.setParameter(OAuth2ParameterNames.REFRESH_TOKEN, TenantKeyedTokenValues.generate("initech"));

        assertThat(filter(codeRequest)).containsExactly("globex");
        assertThat(filter(refreshRequest)).containsExactly("initech");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testRefreshRequestIgnoresConflictingKeyedCode() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/token");

        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, "refresh_token");
        request.setParameter(OAuth2ParameterNames.REFRESH_TOKEN, TenantKeyedTokenValues.generate("acme"));
        request.setParameter(OAuth2ParameterNames.CODE, TenantKeyedTokenValues.generate("globex"));

        assertThat(filter(request)).containsExactly("acme");
    }

    @Test
    void testTokenRequestWithOtherGrantTypeKeepsTenant() throws Exception {
        TenantContext.setCurrentTenantId("outer");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/token");

        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, "client_credentials");
        request.setParameter(OAuth2ParameterNames.CODE, TenantKeyedTokenValues.generate("globex"));
        request.setParameter(OAuth2ParameterNames.REFRESH_TOKEN, TenantKeyedTokenValues.generate("globex"));

        assertThat(filter(request)).containsExactly("outer");
    }

    @Test
    void testAuthorizeRequestIgnoresKeyedCodeAndUsesSessionTenant() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorize");
        MockHttpSession session = new MockHttpSession();

        session.setAttribute(TenantConstants.CURRENT_TENANT_ID, "acme");

        request.setSession(session);
        request.setParameter(OAuth2ParameterNames.CODE, TenantKeyedTokenValues.generate("globex"));

        assertThat(filter(request)).containsExactly("acme");
    }

    @Test
    void testNonTokenRequestWithKeyedParametersAndNoSessionKeepsTenant() throws Exception {
        TenantContext.setCurrentTenantId("outer");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/authorize");

        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, "refresh_token");
        request.setParameter(OAuth2ParameterNames.CODE, TenantKeyedTokenValues.generate("globex"));
        request.setParameter(OAuth2ParameterNames.REFRESH_TOKEN, TenantKeyedTokenValues.generate("globex"));

        assertThat(filter(request)).containsExactly("outer");
    }

    @Test
    void testRequestWithoutTenantLeavesTenantUnchanged() throws Exception {
        TenantContext.setCurrentTenantId("outer");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/token");

        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, "authorization_code");
        request.setParameter(OAuth2ParameterNames.CODE, "legacy-opaque-value");

        assertThat(filter(request)).containsExactly("outer");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo("outer");
    }

    @Test
    void testRevokeRequestWithKeyedRefreshTokenRunsInItsTenant() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/revoke");

        request.setParameter(OAuth2ParameterNames.TOKEN, TenantKeyedTokenValues.generate("globex"));

        assertThat(filter(request)).containsExactly("globex");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testRevokeRequestWithSelfSignedAccessTokenRunsInItsTenantIdClaim() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/revoke");

        request.setParameter(OAuth2ParameterNames.TOKEN, createAccessToken(SIGNING_KEY_PAIR, "acme"));

        assertThat(filter(request)).containsExactly("acme");
    }

    @Test
    void testIntrospectRequestWithSelfSignedAccessTokenRunsInItsTenantIdClaim() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/introspect");

        request.setParameter(OAuth2ParameterNames.TOKEN, createAccessToken(SIGNING_KEY_PAIR, "acme"));

        assertThat(filter(request)).containsExactly("acme");
    }

    @Test
    void testRevokeRequestWithAccessTokenSignedByAnotherKeyKeepsTenant() throws Exception {
        TenantContext.setCurrentTenantId("outer");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/revoke");

        request.setParameter(OAuth2ParameterNames.TOKEN, createAccessToken(generateRsaKeyPair(), "acme"));

        assertThat(filter(request)).containsExactly("outer");
    }

    @Test
    void testAccessTokenSignedByAnotherKeyIsLoggedAsWarning() throws Exception {
        TenantContext.setCurrentTenantId("outer");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/revoke");

        request.setParameter(OAuth2ParameterNames.TOKEN, createAccessToken(generateRsaKeyPair(), "acme"));

        List<ILoggingEvent> loggingEvents = captureLoggingEvents(() -> filter(request));

        assertThat(loggingEvents).singleElement()
            .satisfies(loggingEvent -> {
                assertThat(loggingEvent.getLevel()).isEqualTo(Level.WARN);
                assertThat(loggingEvent.getFormattedMessage()).contains("outer");
            });
    }

    @Test
    void testExpiredAccessTokenIsLoggedAtDebug() throws Exception {
        TenantContext.setCurrentTenantId("outer");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/revoke");

        Instant issuedAt = Instant.now()
            .minus(Duration.ofHours(2));

        request.setParameter(
            OAuth2ParameterNames.TOKEN,
            createAccessToken(SIGNING_KEY_PAIR, "acme", issuedAt, issuedAt.plus(Duration.ofMinutes(5))));

        List<ILoggingEvent> loggingEvents = captureLoggingEvents(() -> assertThat(filter(request))
            .containsExactly("outer"));

        assertThat(loggingEvents).singleElement()
            .satisfies(loggingEvent -> {
                assertThat(loggingEvent.getLevel()).isEqualTo(Level.DEBUG);
                assertThat(loggingEvent.getFormattedMessage()).contains("outer");
            });
    }

    @Test
    void testRevokeRequestWithUnsafeTenantIdClaimKeepsTenant() throws Exception {
        TenantContext.setCurrentTenantId("outer");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/revoke");

        request.setParameter(OAuth2ParameterNames.TOKEN, createAccessToken(SIGNING_KEY_PAIR, "acme; DROP TABLE x"));

        assertThat(filter(request)).containsExactly("outer");
    }

    @Test
    void testRevokeRequestWithNonJwtShapedTokenIsNotDecoded() throws Exception {
        TenantContext.setCurrentTenantId("outer");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/revoke");

        request.setParameter(OAuth2ParameterNames.TOKEN, "a.b.c.d");

        assertThat(filter(request)).containsExactly("outer");

        request.setParameter(OAuth2ParameterNames.TOKEN, "opaque-value");

        assertThat(filter(request)).containsExactly("outer");
        assertThat(decodedTokens).isEmpty();
    }

    @Test
    void testNonPostRevokeRequestKeepsTenant() throws Exception {
        TenantContext.setCurrentTenantId("outer");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/revoke");

        request.setParameter(OAuth2ParameterNames.TOKEN, createAccessToken(SIGNING_KEY_PAIR, "acme"));

        assertThat(filter(request)).containsExactly("outer");
    }

    private static String createAccessToken(KeyPair keyPair, String tenantId) {
        Instant issuedAt = Instant.now();

        return createAccessToken(keyPair, tenantId, issuedAt, issuedAt.plusSeconds(300));
    }

    private static String createAccessToken(KeyPair keyPair, String tenantId, Instant issuedAt, Instant expiresAt) {
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
            .privateKey((RSAPrivateKey) keyPair.getPrivate())
            .keyID(UUID.randomUUID()
                .toString())
            .build();

        NimbusJwtEncoder nimbusJwtEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));

        JwtClaimsSet jwtClaimsSet = JwtClaimsSet.builder()
            .subject("user")
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .claim("tenant_id", tenantId)
            .build();

        Jwt jwt = nimbusJwtEncoder.encode(
            JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256)
                .build(), jwtClaimsSet));

        return jwt.getTokenValue();
    }

    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

            keyPairGenerator.initialize(2048);

            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException(noSuchAlgorithmException);
        }
    }

    private static List<ILoggingEvent> captureLoggingEvents(ThrowingRunnable throwingRunnable) throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(SessionTenantFilter.class);

        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

        listAppender.start();

        logger.addAppender(listAppender);
        logger.setLevel(Level.DEBUG);

        try {
            throwingRunnable.run();
        } finally {
            logger.detachAppender(listAppender);
            logger.setLevel(previousLevel);
        }

        return listAppender.list;
    }

    private List<String> filter(MockHttpServletRequest request) throws Exception {
        List<String> seenTenantIds = new ArrayList<>();

        FilterChain filterChain = (servletRequest, servletResponse) -> seenTenantIds.add(
            TenantContext.getCurrentTenantId());

        sessionTenantFilter.doFilter(request, new MockHttpServletResponse(), filterChain);

        return seenTenantIds;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {

        void run() throws Exception;
    }
}

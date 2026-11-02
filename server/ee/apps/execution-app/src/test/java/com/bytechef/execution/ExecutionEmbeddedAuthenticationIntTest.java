/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.tenant.domain.TenantKey;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.config.testcontainers.RedisContainerConfiguration;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @version ee
 */
@AutoConfigureMockMvc
@Import({
    PostgreSQLContainerConfiguration.class, RedisContainerConfiguration.class
})
@SpringBootTest(classes = ExecutionApplication.class)
class ExecutionEmbeddedAuthenticationIntTest {

    private static final String API_KEY = TenantKey.of("public")
        .toString();
    private static final KeyPair KEY_PAIR = generateKeyPair();
    private static final String PATH = "/api/embedded/v1/external-user-1/tools";

    @MockitoBean
    private ApiKeyService apiKeyService;

    @MockitoBean
    private ConnectedUserService connectedUserService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SigningKeyService signingKeyService;

    @BeforeEach
    void beforeEach() {
        when(connectedUserService.fetchConnectedUser("external-user-1", Environment.PRODUCTION.ordinal()))
            .thenReturn(
                Optional.of(new ConnectedUser(Map.of(), "external@example.com", true, "external-user-1", 9L, "", 0)));
        when(signingKeyService.getPublicKey(API_KEY, Environment.PRODUCTION.ordinal()))
            .thenReturn(KEY_PAIR.getPublic());
    }

    @Test
    void testRequestWithoutCredentialsIsRejected() throws Exception {
        assertThat(status(request())).isEqualTo(401);
    }

    @Test
    void testValidApiKeyIsAccepted() throws Exception {
        ApiKey apiKey = new ApiKey();

        apiKey.setId(1L);

        when(apiKeyService.fetchApiKey(API_KEY, Environment.PRODUCTION.ordinal())).thenReturn(Optional.of(apiKey));

        assertThat(status(authorized(API_KEY))).isNotIn(401, 403);
    }

    @Test
    void testInvalidApiKeyIsRejected() throws Exception {
        when(apiKeyService.fetchApiKey(API_KEY, Environment.PRODUCTION.ordinal())).thenReturn(Optional.empty());

        assertThat(status(authorized(API_KEY))).isEqualTo(401);
    }

    @Test
    void testConnectedUserTokenIsAccepted() throws Exception {
        assertThat(status(authorized(token("external-user-1", KEY_PAIR)))).isNotIn(401, 403);
    }

    @Test
    void testTokenWithWrongSignatureIsRejected() throws Exception {
        assertThat(status(authorized(token("external-user-1", generateKeyPair())))).isEqualTo(401);

        verify(connectedUserService, never()).fetchConnectedUser(anyString(), anyLong());
    }

    @Test
    void testTokenForAnotherUserIsRejected() throws Exception {
        assertThat(status(authorized(token("external-user-2", KEY_PAIR)))).isEqualTo(401);
    }

    @Test
    void testEmbeddedPagesAreDenied() throws Exception {
        assertThat(status(get("/workflow-builder.html"))).isEqualTo(403);
    }

    private static MockHttpServletRequestBuilder authorized(String bearerToken) {
        return request().header("Authorization", "Bearer " + bearerToken)
            .header("X-ENVIRONMENT", Environment.PRODUCTION.name());
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

            keyPairGenerator.initialize(2048);

            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException(noSuchAlgorithmException);
        }
    }

    private static MockHttpServletRequestBuilder request() {
        return get(PATH);
    }

    private int status(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        try {
            return mockMvc.perform(requestBuilder)
                .andReturn()
                .getResponse()
                .getStatus();
        } catch (Exception exception) {
            return 500;
        }
    }

    private static String token(String externalUserId, KeyPair keyPair) {
        return Jwts.builder()
            .header()
            .keyId(API_KEY)
            .and()
            .subject(externalUserId)
            .claim(JwtTokenService.ENVIRONMENT_ID_CLAIM, Environment.PRODUCTION.ordinal())
            .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
            .compact();
    }
}

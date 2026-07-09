/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web;

import static com.bytechef.ee.embedded.ai.mcp.server.security.web.config.EmbeddedMcpServerOAuth2SecurityIntTestConfiguration.ISSUER_URI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.ai.mcp.server.security.web.config.EmbeddedMcpServerOAuth2SecurityIntTestConfiguration;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.user.domain.IdentityProvider;
import com.bytechef.platform.user.service.IdentityProviderService;
import com.bytechef.tenant.domain.TenantKey;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * End-to-end integration test proving the embedded MCP endpoint accepts an OAuth2 Bearer JWT from a per-tenant trusted
 * issuer (resolving a connected user), and that a token from an untrusted issuer falls through to the signing-key
 * filter and is rejected - so the two credential types coexist.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = EmbeddedMcpServerOAuth2SecurityIntTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmbeddedMcpServerOAuth2SecurityIntTest {

    private static final String EXTERNAL_USER_ID = "ext-user-1";
    private static final String PATH_SECRET = String.valueOf(TenantKey.of("public"));
    // A distinct tenant for the audience tests: they configure a different per-IdP audience policy, so they must
    // resolve against their own tenant rather than sharing the default tenant's cached issuers.
    private static final String AUDIENCE_PATH_SECRET = String.valueOf(TenantKey.of("audience"));

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private IdentityProviderService identityProviderService;

    @Autowired
    private KeyPair mcpTestSigningKeyPair;

    @LocalServerPort
    private int port;

    @BeforeEach
    void beforeEach() {
        Mockito.reset(connectedUserService, identityProviderService);

        IdentityProvider identityProvider = mock(IdentityProvider.class);

        when(identityProvider.isEnabled()).thenReturn(true);
        when(identityProvider.isMcpEmbedded()).thenReturn(true);
        when(identityProvider.getIssuerUri()).thenReturn(ISSUER_URI);
        when(identityProviderService.getIdentityProviders()).thenReturn(List.of(identityProvider));

        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.isEnabled()).thenReturn(true);
        when(connectedUser.getExternalId()).thenReturn(EXTERNAL_USER_ID);
        when(connectedUserService.fetchConnectedUser(anyString(), anyLong())).thenReturn(Optional.of(connectedUser));
    }

    @Test
    void testInitializeAndListToolsWithValidOAuth2Jwt() {
        String token = signJwt(ISSUER_URI, EXTERNAL_USER_ID, mcpTestSigningKeyPair);

        try (McpSyncClient mcpSyncClient = createMcpSyncClient(PATH_SECRET, token)) {
            McpSchema.InitializeResult initializeResult = mcpSyncClient.initialize();

            assertThat(initializeResult).isNotNull();
            assertThat(initializeResult.serverInfo()
                .name()).isEqualTo("embedded-mcp-server");

            McpSchema.ListToolsResult listToolsResult = mcpSyncClient.listTools();

            assertThat(listToolsResult).isNotNull();
            assertThat(listToolsResult.tools()).isEmpty();
        }
    }

    @Test
    void testUntrustedIssuerFallsThroughAndIsRejected() throws Exception {
        String token = signJwt("https://evil.example.com", EXTERNAL_USER_ID, mcpTestSigningKeyPair);

        HttpResponse<String> httpResponse = postInitialize(PATH_SECRET, token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testNoTokenReturnsUnauthorizedWithDiscoveryChallenge() throws Exception {
        HttpResponse<String> httpResponse = postInitialize(PATH_SECRET, null);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
        assertThat(httpResponse.headers()
            .firstValue("WWW-Authenticate")).hasValue(
                "Bearer resource_metadata=\"http://localhost:" + port +
                    "/.well-known/oauth-protected-resource/api/embedded/" + PATH_SECRET + "/mcp\"");
    }

    @Test
    void testValidateMcpAudienceRejectsTokenWithoutMatchingAudience() throws Exception {
        stubMcpAudienceValidatingIdentityProvider();

        String token = signJwt(ISSUER_URI, EXTERNAL_USER_ID, mcpTestSigningKeyPair);

        HttpResponse<String> httpResponse = postInitialize(AUDIENCE_PATH_SECRET, token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testValidateMcpAudienceAcceptsTokenWithMatchingAudience() {
        stubMcpAudienceValidatingIdentityProvider();

        String endpointUrl = "http://localhost:" + port + "/api/embedded/" + AUDIENCE_PATH_SECRET + "/mcp";
        String token = signJwt(ISSUER_URI, EXTERNAL_USER_ID, endpointUrl, mcpTestSigningKeyPair);

        try (McpSyncClient mcpSyncClient = createMcpSyncClient(AUDIENCE_PATH_SECRET, token)) {
            McpSchema.InitializeResult initializeResult = mcpSyncClient.initialize();

            assertThat(initializeResult).isNotNull();
        }
    }

    private void stubMcpAudienceValidatingIdentityProvider() {
        IdentityProvider identityProvider = mock(IdentityProvider.class);

        when(identityProvider.isEnabled()).thenReturn(true);
        when(identityProvider.isMcpEmbedded()).thenReturn(true);
        when(identityProvider.getIssuerUri()).thenReturn(ISSUER_URI);
        when(identityProvider.isValidateMcpAudience()).thenReturn(true);
        when(identityProviderService.getIdentityProviders()).thenReturn(List.of(identityProvider));
    }

    private static String signJwt(String issuer, String subject, KeyPair signingKeyPair) {
        return signJwt(issuer, subject, null, signingKeyPair);
    }

    private static String signJwt(String issuer, String subject, String audience, KeyPair signingKeyPair) {
        try {
            JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(subject)
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now()
                    .plus(5, ChronoUnit.MINUTES)));

            if (audience != null) {
                claimsSetBuilder.audience(audience);
            }

            JWTClaimsSet claimsSet = claimsSetBuilder.build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);

            signedJWT.sign(new RSASSASigner((RSAPrivateKey) signingKeyPair.getPrivate()));

            return signedJWT.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private McpSyncClient createMcpSyncClient(String pathSecret, String bearerToken) {
        String baseUrl = "http://localhost:" + port;
        String endpoint = "/api/embedded/" + pathSecret + "/mcp";

        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder(baseUrl)
            .endpoint(endpoint)
            .httpRequestCustomizer((httpRequestBuilder, method, uri, body, transportContext) -> {
                if (bearerToken != null) {
                    httpRequestBuilder.header("Authorization", "Bearer " + bearerToken);
                }
            })
            .build();

        return McpClient.sync(transport)
            .requestTimeout(Duration.ofSeconds(30))
            .build();
    }

    private HttpResponse<String> postInitialize(String pathSecret, String bearerToken) throws Exception {
        String initializeRequest = """
            {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05",\
            "capabilities":{},"clientInfo":{"name":"test-client","version":"1.0.0"}}}""";

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/api/embedded/" + pathSecret + "/mcp"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .POST(HttpRequest.BodyPublishers.ofString(initializeRequest));

        if (bearerToken != null) {
            requestBuilder.header("Authorization", "Bearer " + bearerToken);
        }

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}

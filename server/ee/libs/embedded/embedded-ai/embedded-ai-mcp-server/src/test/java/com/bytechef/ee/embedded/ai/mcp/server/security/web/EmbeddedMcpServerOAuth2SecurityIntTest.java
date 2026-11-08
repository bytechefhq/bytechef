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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.ai.mcp.server.security.web.config.EmbeddedMcpServerOAuth2SecurityIntTestConfiguration;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver;
import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
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
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = EmbeddedMcpServerOAuth2SecurityIntTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmbeddedMcpServerOAuth2SecurityIntTest {

    private static final String EMBEDDED_SCOPE = "mcp:embedded";
    private static final String EXTERNAL_USER_ID = "ext-user-1";
    private static final String TENANT_ID = "public";
    private static final String PATH_SECRET = String.valueOf(TenantKey.of(TENANT_ID));
    private static final String AUDIENCE_PATH_SECRET = String.valueOf(TenantKey.of(TENANT_ID));
    private static final String FORGED_PATH_SECRET = String.valueOf(TenantKey.of(TENANT_ID));
    private static final String NO_AUTH_PATH_SECRET = String.valueOf(TenantKey.of(TENANT_ID));

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private IdentityProviderService identityProviderService;

    @Autowired
    private McpServerService mcpServerService;

    @Autowired
    private McpTenantIssuerResolver mcpTenantIssuerResolver;

    @Autowired
    private KeyPair mcpTestSigningKeyPair;

    @LocalServerPort
    private int port;

    @BeforeEach
    void beforeEach() {
        Mockito.reset(connectedUserService, identityProviderService, mcpServerService);

        mcpTenantIssuerResolver.evictAll();

        when(mcpServerService.getMcpServer(anyString())).thenThrow(new IllegalArgumentException("Unknown server"));

        mockMcpServer(PATH_SECRET, true);
        mockMcpServer(AUDIENCE_PATH_SECRET, true);

        stubIdentityProvider(false);

        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.isEnabled()).thenReturn(true);
        when(connectedUser.getExternalId()).thenReturn(EXTERNAL_USER_ID);
        when(connectedUserService.fetchConnectedUser(anyString(), anyLong())).thenReturn(Optional.of(connectedUser));
    }

    @Test
    void testEmbeddedTokenIsAcceptedAndBoundToTheConnectedUser() {
        String token = sign(claims(ISSUER_URI, EMBEDDED_SCOPE));

        try (McpSyncClient mcpSyncClient = createMcpSyncClient(PATH_SECRET, token)) {
            McpSchema.InitializeResult initializeResult = mcpSyncClient.initialize();

            assertThat(initializeResult).isNotNull();
            assertThat(initializeResult.serverInfo()
                .name()).isEqualTo("embedded-mcp-server");

            McpSchema.ListToolsResult listToolsResult = mcpSyncClient.listTools();

            assertThat(listToolsResult).isNotNull();
            assertThat(listToolsResult.tools()).isEmpty();
        }

        verify(connectedUserService, Mockito.atLeastOnce())
            .fetchConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION.ordinal());
    }

    @Test
    void testEmbeddedTokenOnAForgedTenantKeyIsRejectedWithoutIdentityProviderLookup() throws Exception {
        String token = sign(claims(ISSUER_URI, EMBEDDED_SCOPE));

        HttpResponse<String> httpResponse = postInitialize(FORGED_PATH_SECRET, token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);

        verify(identityProviderService, never()).getIdentityProviders();
        verify(connectedUserService, never()).fetchConnectedUser(anyString(), anyLong());
    }

    @Test
    void testEmbeddedTokenWithoutTheEmbeddedScopeIsForbidden() throws Exception {
        String token = sign(claims(ISSUER_URI, "openid email"));

        HttpResponse<String> httpResponse = postInitialize(PATH_SECRET, token);

        assertThat(httpResponse.statusCode()).isEqualTo(403);
        assertThat(httpResponse.headers()
            .firstValue("WWW-Authenticate")
            .orElse("")).contains("error=\"insufficient_scope\"")
                .contains("scope=\"" + EMBEDDED_SCOPE + "\"");

        verify(connectedUserService, never()).fetchConnectedUser(anyString(), anyLong());
    }

    @Test
    void testEmbeddedTokenScopedForAnotherSurfaceIsForbidden() throws Exception {
        String token = sign(claims(ISSUER_URI, "mcp:automation"));

        HttpResponse<String> httpResponse = postInitialize(PATH_SECRET, token);

        assertThat(httpResponse.statusCode()).isEqualTo(403);
    }

    @Test
    void testUntrustedIssuerFallsThroughAndIsRejected() throws Exception {
        String token = sign(claims("https://evil.example.com", EMBEDDED_SCOPE));

        HttpResponse<String> httpResponse = postInitialize(PATH_SECRET, token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testNoTokenReturnsUnauthorizedWithDiscoveryChallenge() throws Exception {
        HttpResponse<String> httpResponse = postInitialize(PATH_SECRET, null);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
        assertThat(httpResponse.headers()
            .firstValue("WWW-Authenticate")).hasValue(
                "Bearer resource_metadata=\"" + baseUrl() + "/.well-known/oauth-protected-resource/api/embedded/" +
                    PATH_SECRET + "/mcp\", scope=\"" + EMBEDDED_SCOPE + "\"");
    }

    @Test
    void testNoTokenWhenAuthenticationNotRequiredHasNoDiscoveryChallenge() throws Exception {
        mockMcpServer(NO_AUTH_PATH_SECRET, false);

        HttpResponse<String> httpResponse = postInitialize(NO_AUTH_PATH_SECRET, null);

        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(httpResponse.headers()
            .firstValue("WWW-Authenticate")).isEmpty();
    }

    @Test
    void testInitializeAndListToolsWithoutTokenWhenAuthenticationNotRequired() {
        mockMcpServer(NO_AUTH_PATH_SECRET, false);

        try (McpSyncClient mcpSyncClient = createMcpSyncClient(NO_AUTH_PATH_SECRET, null)) {
            McpSchema.InitializeResult initializeResult = mcpSyncClient.initialize();

            assertThat(initializeResult).isNotNull();

            McpSchema.ListToolsResult listToolsResult = mcpSyncClient.listTools();

            assertThat(listToolsResult.tools()).isEmpty();
        }
    }

    @Test
    void testValidateMcpAudienceRejectsTokenWithoutMatchingAudience() throws Exception {
        stubIdentityProvider(true);

        String token = sign(claims(ISSUER_URI, EMBEDDED_SCOPE));

        HttpResponse<String> httpResponse = postInitialize(AUDIENCE_PATH_SECRET, token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testValidateMcpAudienceAcceptsTokenWithMatchingAudience() {
        stubIdentityProvider(true);

        String token = sign(
            claims(ISSUER_URI, EMBEDDED_SCOPE).audience(baseUrl() + "/api/embedded/" + AUDIENCE_PATH_SECRET + "/mcp"));

        try (McpSyncClient mcpSyncClient = createMcpSyncClient(AUDIENCE_PATH_SECRET, token)) {
            assertThat(mcpSyncClient.initialize()).isNotNull();
        }
    }

    @Test
    void testProtectedResourceMetadataAdvertisesTheEmbeddedIdentityProviderAndScope() throws Exception {
        HttpResponse<String> httpResponse = getMetadata(PATH_SECRET);

        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(httpResponse.body())
            .contains("\"authorization_servers\":[\"" + ISSUER_URI + "\"]")
            .contains("\"scopes_supported\":[\"" + EMBEDDED_SCOPE + "\"]")
            .contains("\"resource\":\"" + baseUrl() + "/api/embedded/" + PATH_SECRET + "/mcp\"");
    }

    @Test
    void testProtectedResourceMetadataForAForgedTenantKeyIsNotFoundWithoutIdentityProviderLookup()
        throws Exception {

        HttpResponse<String> httpResponse = getMetadata(FORGED_PATH_SECRET);

        assertThat(httpResponse.statusCode()).isEqualTo(404);
        assertThat(httpResponse.body()).doesNotContain(ISSUER_URI);

        verify(identityProviderService, never()).getIdentityProviders();
    }

    @Test
    void testProtectedResourceMetadataIsNotFoundWithoutAnEmbeddedIdentityProvider() throws Exception {
        when(identityProviderService.getIdentityProviders()).thenReturn(List.of());

        HttpResponse<String> httpResponse = getMetadata(PATH_SECRET);

        assertThat(httpResponse.statusCode()).isEqualTo(404);
    }

    private void mockMcpServer(String secretKey, boolean authenticationRequired) {
        McpServer mcpServer = mock(McpServer.class);

        when(mcpServer.getEnvironment()).thenReturn(Environment.PRODUCTION);
        when(mcpServer.getSecretKey()).thenReturn(secretKey);
        when(mcpServer.getType()).thenReturn(PlatformType.EMBEDDED);
        when(mcpServer.isAuthenticationRequired()).thenReturn(authenticationRequired);
        when(mcpServer.isEnabled()).thenReturn(true);

        Mockito.doReturn(mcpServer)
            .when(mcpServerService)
            .getMcpServer(secretKey);
    }

    private void stubIdentityProvider(boolean validateMcpAudience) {
        IdentityProvider identityProvider = mock(IdentityProvider.class);

        when(identityProvider.isEnabled()).thenReturn(true);
        when(identityProvider.isMcpEmbedded()).thenReturn(true);
        when(identityProvider.getIssuerUri()).thenReturn(ISSUER_URI);
        when(identityProvider.isValidateMcpAudience()).thenReturn(validateMcpAudience);
        when(identityProviderService.getIdentityProviders()).thenReturn(List.of(identityProvider));
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

    private String sign(JWTClaimsSet.Builder claimsSetBuilder) {
        try {
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSetBuilder.build());

            signedJWT.sign(new RSASSASigner((RSAPrivateKey) mcpTestSigningKeyPair.getPrivate()));

            return signedJWT.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private McpSyncClient createMcpSyncClient(String pathSecret, String bearerToken) {
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder(baseUrl())
            .endpoint("/api/embedded/" + pathSecret + "/mcp")
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

    private HttpResponse<String> getMetadata(String pathSecret) throws Exception {
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(
                URI.create(baseUrl() + "/.well-known/oauth-protected-resource/api/embedded/" + pathSecret + "/mcp"))
            .GET()
            .build();

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        }
    }

    private HttpResponse<String> postInitialize(String pathSecret, String bearerToken) throws Exception {
        String initializeRequest = """
            {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05",\
            "capabilities":{},"clientInfo":{"name":"test-client","version":"1.0.0"}}}""";

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl() + "/api/embedded/" + pathSecret + "/mcp"))
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

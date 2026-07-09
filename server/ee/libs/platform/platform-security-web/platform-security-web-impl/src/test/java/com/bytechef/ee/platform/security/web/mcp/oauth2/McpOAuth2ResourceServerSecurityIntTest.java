/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.mcp.oauth2;

import static com.bytechef.ee.platform.security.web.mcp.config.McpOAuth2ResourceServerSecurityIntTestConfiguration.EXTERNAL_AUTHORITIES_CLAIM;
import static com.bytechef.ee.platform.security.web.mcp.config.McpOAuth2ResourceServerSecurityIntTestConfiguration.EXTERNAL_ISSUER_URI;
import static com.bytechef.ee.platform.security.web.mcp.config.McpOAuth2ResourceServerSecurityIntTestConfiguration.ISSUER_URI;
import static com.bytechef.ee.platform.security.web.mcp.config.McpOAuth2ResourceServerSecurityIntTestConfiguration.PER_TENANT_IDP_ISSUER_URI;
import static com.bytechef.ee.platform.security.web.mcp.config.McpOAuth2ResourceServerSecurityIntTestConfiguration.TENANT_CLAIM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.security.web.mcp.config.McpOAuth2ResourceServerSecurityIntTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.repository.ApiKeyRepository;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.domain.TenantKey;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
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
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * End-to-end integration test proving the OAuth2 resource server accepts a Bearer JWT on the management MCP endpoint
 * and that Phase 1 API-key authentication still works on the same endpoint after the resource server is enabled
 * (coexistence). The JWT is minted in-test and validated by a static-key decoder wired through
 * {@code McpJwtDecoderFactory}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = McpOAuth2ResourceServerSecurityIntTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgreSQLContainerConfiguration.class)
class McpOAuth2ResourceServerSecurityIntTest {

    private static final Environment ENVIRONMENT = Environment.PRODUCTION;
    // The management MCP server secret is a per-tenant TenantKey; the OAuth2 tenant is anchored to it (URL anchor).
    private static final String MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of("public"));
    private static final long USER_ID = 1050L;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private KeyPair mcpTestSigningKeyPair;

    @Autowired
    private KeyPair mcpExternalTestSigningKeyPair;

    @Autowired
    private KeyPair mcpPerTenantIdpTestSigningKeyPair;

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private UserService userService;

    @LocalServerPort
    private int port;

    @BeforeEach
    void beforeEach() {
        Mockito.reset(propertyService, userService);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update("DELETE FROM api_key");

        Property property = mock(Property.class);

        when(property.get("secretKey")).thenReturn(MCP_SERVER_SECRET_KEY);
        when(propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null)).thenReturn(property);

        User user = mock(User.class);

        when(user.isActivated()).thenReturn(true);
        when(user.getLogin()).thenReturn("admin@localhost.com");
        when(user.getAuthorityIds()).thenReturn(List.of());
        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));
        when(userService.fetchUserByLogin("admin@localhost.com")).thenReturn(Optional.of(user));
    }

    @AfterEach
    void afterEach() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update("DELETE FROM api_key");
    }

    @Test
    void testInitializeAndListToolsWithValidJwt() {
        String token = signManagementJwt(managementEndpointUrl());

        try (McpSyncClient mcpSyncClient = createMcpSyncClient(MCP_SERVER_SECRET_KEY, token)) {
            McpSchema.InitializeResult initializeResult = mcpSyncClient.initialize();

            assertThat(initializeResult).isNotNull();
            assertThat(initializeResult.serverInfo()
                .name()).isEqualTo("mcp-server");

            McpSchema.ListToolsResult listToolsResult = mcpSyncClient.listTools();

            assertThat(listToolsResult).isNotNull();
            assertThat(listToolsResult.tools()).isEmpty();
        }
    }

    @Test
    void testInitializeWithValidApiKeyStillWorks() {
        String secretKey = seedApiKey();

        try (McpSyncClient mcpSyncClient = createMcpSyncClient(MCP_SERVER_SECRET_KEY, secretKey)) {
            McpSchema.InitializeResult initializeResult = mcpSyncClient.initialize();

            assertThat(initializeResult).isNotNull();
            assertThat(initializeResult.serverInfo()
                .name()).isEqualTo("mcp-server");
        }
    }

    @Test
    void testInitializeWithMismatchedAudienceIsRejected() throws Exception {
        String token = signManagementJwt("http://localhost:" + port + "/api/management/other-secret/mcp");

        HttpResponse<String> httpResponse = postInitialize(MCP_SERVER_SECRET_KEY, token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testInitializeWithDeactivatedUserIsRejected() throws Exception {
        when(userService.fetchUserByLogin("admin@localhost.com")).thenReturn(Optional.empty());

        String token = signManagementJwt(managementEndpointUrl());

        HttpResponse<String> httpResponse = postInitialize(MCP_SERVER_SECRET_KEY, token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testInitializeWithUntrustedIssuerJwtIsRejected() throws Exception {
        String token = signJwt("https://evil.example.com", mcpTestSigningKeyPair);

        HttpResponse<String> httpResponse = postInitialize(MCP_SERVER_SECRET_KEY, token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testInitializeWithWrongScopeIsRejected() throws Exception {
        String token = sign(
            mcpTestSigningKeyPair,
            baseClaims(ISSUER_URI)
                .claim(TENANT_CLAIM, "public")
                .claim("scope", "mcp:automation")
                .build());

        HttpResponse<String> httpResponse = postInitialize(MCP_SERVER_SECRET_KEY, token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testInitializeWithExpiredJwtIsRejected() throws Exception {
        String token = sign(
            mcpTestSigningKeyPair,
            new JWTClaimsSet.Builder()
                .issuer(ISSUER_URI)
                .subject("admin@localhost.com")
                .issueTime(Date.from(Instant.now()
                    .minus(10, ChronoUnit.MINUTES)))
                .expirationTime(Date.from(Instant.now()
                    .minus(5, ChronoUnit.MINUTES)))
                .claim(TENANT_CLAIM, "public")
                .claim("scope", "mcp:management")
                .build());

        HttpResponse<String> httpResponse = postInitialize(MCP_SERVER_SECRET_KEY, token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testUnauthenticatedRequestReturnsDiscoveryChallenge() throws Exception {
        HttpResponse<String> httpResponse = postInitialize(MCP_SERVER_SECRET_KEY, null);

        assertThat(httpResponse.statusCode()).isEqualTo(401);

        String wwwAuthenticate = httpResponse.headers()
            .firstValue("WWW-Authenticate")
            .orElse("");

        assertThat(wwwAuthenticate).contains("Bearer");
        assertThat(wwwAuthenticate).contains("resource_metadata");
    }

    @Test
    void testProtectedResourceMetadataResolvesToConfiguredIssuer() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/.well-known/oauth-protected-resource/api"))
            .GET()
            .build();

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(httpResponse.statusCode()).isEqualTo(200);
            assertThat(httpResponse.body()).contains(ISSUER_URI);
        }
    }

    @Test
    void testInitializeWithExternalIssuerJwtSucceeds() {
        // The external IdP token is identity-only: no tenant claim (tenant comes from the URL), and it carries the
        // endpoint URL as its audience (required for external issuers under the URL anchor).
        String token = sign(
            mcpExternalTestSigningKeyPair,
            baseClaims(EXTERNAL_ISSUER_URI)
                .claim(EXTERNAL_AUTHORITIES_CLAIM, List.of("editors"))
                .claim("scope", "mcp:management")
                .audience(List.of(managementEndpointUrl()))
                .build());

        try (McpSyncClient mcpSyncClient = createMcpSyncClient(MCP_SERVER_SECRET_KEY, token)) {
            McpSchema.InitializeResult initializeResult = mcpSyncClient.initialize();

            assertThat(initializeResult).isNotNull();
            assertThat(initializeResult.serverInfo()
                .name()).isEqualTo("mcp-server");
        }
    }

    @Test
    void testInitializeWithPerTenantIdpJwtSucceeds() {
        // A token from a per-tenant external IdP: only a signature, subject, and group claim - no scope and no audience
        // (trust is scoped to the tenant that configured the provider, so neither is required).
        String token = signPerTenantIdpJwt();

        try (McpSyncClient mcpSyncClient = createMcpSyncClient(MCP_SERVER_SECRET_KEY, token)) {
            McpSchema.InitializeResult initializeResult = mcpSyncClient.initialize();

            assertThat(initializeResult).isNotNull();
            assertThat(initializeResult.serverInfo()
                .name()).isEqualTo("mcp-server");
        }
    }

    @Test
    void testPerTenantIdpJwtIsRejectedOnAnotherTenantsEndpoint() throws Exception {
        // The same issuer is trusted only for the "public" tenant; presented on a different tenant's endpoint secret it
        // must not authenticate.
        String otherTenantSecret = String.valueOf(TenantKey.of("other"));

        String token = signPerTenantIdpJwt();

        HttpResponse<String> httpResponse = postInitialize(otherTenantSecret, token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    private String signPerTenantIdpJwt() {
        return sign(
            mcpPerTenantIdpTestSigningKeyPair,
            baseClaims(PER_TENANT_IDP_ISSUER_URI)
                .claim("groups", List.of("editors"))
                .build());
    }

    private String signManagementJwt(String audience) {
        return sign(
            mcpTestSigningKeyPair,
            baseClaims(ISSUER_URI)
                .claim(TENANT_CLAIM, "public")
                .claim("scope", "mcp:management")
                .audience(List.of(audience))
                .build());
    }

    private String managementEndpointUrl() {
        return "http://localhost:" + port + "/api/management/" + MCP_SERVER_SECRET_KEY + "/mcp";
    }

    private String seedApiKey() {
        String secretKey = String.valueOf(TenantKey.of());

        ApiKey apiKey = new ApiKey();

        apiKey.setName("test");
        apiKey.setSecretKey(secretKey);
        apiKey.setType(null);
        apiKey.setEnvironment(ENVIRONMENT);
        apiKey.setUserId(USER_ID);

        apiKeyRepository.save(apiKey);

        return secretKey;
    }

    private static String signJwt(String issuer, KeyPair signingKeyPair) {
        return sign(
            signingKeyPair,
            baseClaims(issuer)
                .claim(TENANT_CLAIM, "public")
                .claim("scope", "mcp:management")
                .build());
    }

    private static JWTClaimsSet.Builder baseClaims(String issuer) {
        return new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject("admin@localhost.com")
            .issueTime(Date.from(Instant.now()))
            .expirationTime(Date.from(Instant.now()
                .plus(5, ChronoUnit.MINUTES)));
    }

    private static String sign(KeyPair signingKeyPair, JWTClaimsSet claimsSet) {
        try {
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);

            signedJWT.sign(new RSASSASigner((RSAPrivateKey) signingKeyPair.getPrivate()));

            return signedJWT.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private McpSyncClient createMcpSyncClient(String pathSecret, String bearerToken) {
        String baseUrl = "http://localhost:" + port;
        String endpoint = "/api/management/" + pathSecret + "/mcp";

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
            .uri(URI.create("http://localhost:" + port + "/api/management/" + pathSecret + "/mcp"))
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

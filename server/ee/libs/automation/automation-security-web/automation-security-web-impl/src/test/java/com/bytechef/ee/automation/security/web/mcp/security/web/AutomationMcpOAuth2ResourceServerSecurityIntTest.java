/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.security.web.mcp.security.web;

import static com.bytechef.ee.automation.security.web.mcp.config.AutomationMcpOAuth2ResourceServerSecurityIntTestConfiguration.ISSUER_URI;
import static com.bytechef.ee.automation.security.web.mcp.config.AutomationMcpOAuth2ResourceServerSecurityIntTestConfiguration.TENANT_CLAIM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.security.web.mcp.config.AutomationMcpOAuth2ResourceServerSecurityIntTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
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
 * End-to-end integration test proving the OAuth2 resource server accepts a Bearer JWT on the automation MCP endpoint,
 * enforces the {@code mcp:automation} scope there, and that Phase 1 API-key authentication still works on the same
 * endpoint (coexistence). Mirrors the management-endpoint resource-server test in {@code platform-security-web-impl}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AutomationMcpOAuth2ResourceServerSecurityIntTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgreSQLContainerConfiguration.class)
class AutomationMcpOAuth2ResourceServerSecurityIntTest {

    private static final Environment ENVIRONMENT = Environment.PRODUCTION;
    // The automation MCP server secret is a per-tenant TenantKey; the OAuth2 tenant is anchored to it (URL anchor).
    private static final String MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of("public"));
    private static final long USER_ID = 1050L;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private KeyPair mcpTestSigningKeyPair;

    @Autowired
    private McpServerService mcpServerService;

    @Autowired
    private UserService userService;

    @LocalServerPort
    private int port;

    @BeforeEach
    void beforeEach() {
        Mockito.reset(mcpServerService, userService);

        jdbcTemplate().update("DELETE FROM api_key");

        User user = mock(User.class);

        when(user.isActivated()).thenReturn(true);
        when(user.getLogin()).thenReturn("admin@localhost.com");
        when(user.getAuthorityIds()).thenReturn(List.of());
        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate().update("DELETE FROM api_key");
    }

    @Test
    void testInitializeAndListToolsWithValidJwt() {
        String token = signJwt("mcp:automation", mcpTestSigningKeyPair);

        try (McpSyncClient mcpSyncClient = createMcpSyncClient(MCP_SERVER_SECRET_KEY, token)) {
            McpSchema.InitializeResult initializeResult = mcpSyncClient.initialize();

            assertThat(initializeResult).isNotNull();
            assertThat(initializeResult.serverInfo()
                .name()).isEqualTo("automation-mcp-server");

            McpSchema.ListToolsResult listToolsResult = mcpSyncClient.listTools();

            assertThat(listToolsResult).isNotNull();
            assertThat(listToolsResult.tools()).isEmpty();
        }
    }

    @Test
    void testInitializeWithValidApiKeyStillWorks() {
        String secretKey = seedApiKey();

        mockMcpServer();

        try (McpSyncClient mcpSyncClient = createMcpSyncClient(MCP_SERVER_SECRET_KEY, secretKey)) {
            McpSchema.InitializeResult initializeResult = mcpSyncClient.initialize();

            assertThat(initializeResult).isNotNull();
            assertThat(initializeResult.serverInfo()
                .name()).isEqualTo("automation-mcp-server");
        }
    }

    @Test
    void testInitializeWithManagementScopeIsRejected() throws Exception {
        String token = signJwt("mcp:management", mcpTestSigningKeyPair);

        HttpResponse<String> httpResponse = postInitialize(MCP_SERVER_SECRET_KEY, token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testInitializeWithTenantClaimDisagreeingWithUrlIsRejected() throws Exception {
        String token = signJwt("mcp:automation", "victim", mcpTestSigningKeyPair);

        HttpResponse<String> httpResponse = postInitialize(MCP_SERVER_SECRET_KEY, token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    private void mockMcpServer() {
        McpServer mcpServer = mock(McpServer.class);

        when(mcpServer.getEnvironment()).thenReturn(ENVIRONMENT);
        when(mcpServerService.getMcpServer(MCP_SERVER_SECRET_KEY)).thenReturn(mcpServer);
    }

    private String seedApiKey() {
        String secretKey = String.valueOf(TenantKey.of());

        ApiKey apiKey = new ApiKey();

        apiKey.setName("test");
        apiKey.setSecretKey(secretKey);
        apiKey.setType(PlatformType.AUTOMATION);
        apiKey.setEnvironment(ENVIRONMENT);
        apiKey.setUserId(USER_ID);

        apiKeyRepository.save(apiKey);

        return secretKey;
    }

    private String automationEndpointUrl() {
        return "http://localhost:" + port + "/api/automation/" + MCP_SERVER_SECRET_KEY + "/mcp";
    }

    private String signJwt(String scope, KeyPair signingKeyPair) {
        return signJwt(scope, "public", signingKeyPair);
    }

    private String signJwt(String scope, String tenantId, KeyPair signingKeyPair) {
        try {
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(ISSUER_URI)
                .subject("admin@localhost.com")
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now()
                    .plus(5, ChronoUnit.MINUTES)))
                .claim(TENANT_CLAIM, tenantId)
                .claim("scope", scope)
                .audience(List.of(automationEndpointUrl()))
                .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);

            signedJWT.sign(new RSASSASigner((RSAPrivateKey) signingKeyPair.getPrivate()));

            return signedJWT.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private McpSyncClient createMcpSyncClient(String pathSecret, String bearerToken) {
        String baseUrl = "http://localhost:" + port;
        String endpoint = "/api/automation/" + pathSecret + "/mcp";

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
            .uri(URI.create("http://localhost:" + port + "/api/automation/" + pathSecret + "/mcp"))
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

    private JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }
}

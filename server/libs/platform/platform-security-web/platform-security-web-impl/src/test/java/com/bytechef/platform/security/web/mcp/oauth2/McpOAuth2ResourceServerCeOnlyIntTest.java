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

package com.bytechef.platform.security.web.mcp.oauth2;

import static com.bytechef.platform.security.web.mcp.config.McpOAuth2ResourceServerCeOnlyIntTestConfiguration.ISSUER_URI;
import static com.bytechef.platform.security.web.mcp.config.McpOAuth2ResourceServerCeOnlyIntTestConfiguration.TENANT_CLAIM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.web.mcp.config.McpOAuth2ResourceServerCeOnlyIntTestConfiguration;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * Proves the base MCP resource server works standalone in CE - with no external-IdP federation on the context: a
 * self-issued JWT authenticates on the management MCP endpoint, and a token whose issuer is not statically configured
 * is rejected (no {@link McpFederatedIssuerAuthenticator} bean is present to authenticate it). Also asserts the base
 * policy checks - endpoint scope, RFC 8707 audience binding, per-request user revocation - and the RFC 9728 discovery
 * challenge, all without any {@code com.bytechef.ee.*} type on the classpath.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = McpOAuth2ResourceServerCeOnlyIntTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgreSQLContainerConfiguration.class)
class McpOAuth2ResourceServerCeOnlyIntTest {

    private static final String MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of("public"));

    @Autowired
    private KeyPair mcpTestSigningKeyPair;

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private UserService userService;

    @LocalServerPort
    private int port;

    @BeforeEach
    void beforeEach() {
        Mockito.reset(propertyService, userService);

        Property property = mock(Property.class);

        when(property.get("secretKey")).thenReturn(MCP_SERVER_SECRET_KEY);
        when(propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null)).thenReturn(property);

        User user = mock(User.class);

        when(user.isActivated()).thenReturn(true);
        when(user.getLogin()).thenReturn("admin@localhost.com");
        when(user.getAuthorityIds()).thenReturn(List.of());
        when(userService.fetchUserByLogin("admin@localhost.com")).thenReturn(Optional.of(user));
    }

    @Test
    void testInitializeAndListToolsWithValidSelfIssuerJwt() {
        String token = signManagementJwt(managementEndpointUrl());

        try (McpSyncClient mcpSyncClient = createMcpSyncClient(token)) {
            McpSchema.InitializeResult initializeResult = mcpSyncClient.initialize();

            assertThat(initializeResult).isNotNull();
            assertThat(initializeResult.serverInfo()
                .name()).isEqualTo("mcp-server");
            assertThat(mcpSyncClient.listTools()
                .tools()).isEmpty();
        }
    }

    @Test
    void testNonStaticIssuerJwtIsRejected() throws Exception {
        String token = sign(
            baseClaims("https://idp.customer.test")
                .claim(TENANT_CLAIM, "public")
                .claim("scope", "mcp:management")
                .audience(List.of(managementEndpointUrl()))
                .build());

        HttpResponse<String> httpResponse = postInitialize(token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testWrongScopeIsRejected() throws Exception {
        String token = sign(
            baseClaims(ISSUER_URI)
                .claim(TENANT_CLAIM, "public")
                .claim("scope", "mcp:automation")
                .audience(List.of(managementEndpointUrl()))
                .build());

        HttpResponse<String> httpResponse = postInitialize(token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testMismatchedAudienceIsRejected() throws Exception {
        String token = signManagementJwt("http://localhost:" + port + "/api/management/other-secret/mcp");

        HttpResponse<String> httpResponse = postInitialize(token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testDeactivatedUserIsRejected() throws Exception {
        when(userService.fetchUserByLogin("admin@localhost.com")).thenReturn(Optional.empty());

        String token = signManagementJwt(managementEndpointUrl());

        HttpResponse<String> httpResponse = postInitialize(token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testUnauthenticatedRequestReturnsDiscoveryChallenge() throws Exception {
        HttpResponse<String> httpResponse = postInitialize(null);

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

    private String signManagementJwt(String audience) {
        return sign(
            baseClaims(ISSUER_URI)
                .claim(TENANT_CLAIM, "public")
                .claim("scope", "mcp:management")
                .audience(List.of(audience))
                .build());
    }

    private String managementEndpointUrl() {
        return "http://localhost:" + port + "/api/management/" + MCP_SERVER_SECRET_KEY + "/mcp";
    }

    private static JWTClaimsSet.Builder baseClaims(String issuer) {
        return new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject("admin@localhost.com")
            .issueTime(Date.from(Instant.now()))
            .expirationTime(Date.from(Instant.now()
                .plus(5, ChronoUnit.MINUTES)));
    }

    private String sign(JWTClaimsSet claimsSet) {
        try {
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);

            signedJWT.sign(new RSASSASigner((RSAPrivateKey) mcpTestSigningKeyPair.getPrivate()));

            return signedJWT.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private McpSyncClient createMcpSyncClient(String bearerToken) {
        HttpClientStreamableHttpTransport transport =
            HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
                .endpoint("/api/management/" + MCP_SERVER_SECRET_KEY + "/mcp")
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

    private HttpResponse<String> postInitialize(String bearerToken) throws Exception {
        String initializeRequest = """
            {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05",\
            "capabilities":{},"clientInfo":{"name":"test-client","version":"1.0.0"}}}""";

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/api/management/" + MCP_SERVER_SECRET_KEY + "/mcp"))
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

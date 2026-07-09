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

package com.bytechef.ai.mcp.server.security.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.mcp.server.config.ManagementMcpServerSecurityIntTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.repository.ApiKeyRepository;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.domain.TenantKey;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
 * End-to-end integration test that drives a real streamable-HTTP MCP client against a live Tomcat server (random port)
 * backed by a real PostgreSQL (Testcontainers) and a DB-backed {@code ApiKeyService}. Unlike a MockMvc test, this
 * exercises {@code HttpServletRequest#getServletPath()} on a genuine servlet container, which is exactly the path used
 * by {@code McpApiKeyAuthenticationConverter} to extract the URL path secret.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ManagementMcpServerSecurityIntTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgreSQLContainerConfiguration.class)
class ManagementMcpServerSecurityIntTest {

    private static final Environment ENVIRONMENT = Environment.PRODUCTION;
    private static final String MCP_SERVER_SECRET_KEY = "management-server-secret";
    private static final long USER_ID = 1050L;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private UserService userService;

    @LocalServerPort
    private int port;

    @BeforeEach
    void beforeEach() {
        Mockito.reset(authorityService, propertyService, userService);

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
    }

    @AfterEach
    void afterEach() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update("DELETE FROM api_key");
    }

    @Test
    void testInitializeAndListToolsWithValidAdminApiKey() {
        String secretKey = seedApiKey(null, ENVIRONMENT);

        try (McpSyncClient mcpSyncClient = createMcpSyncClient(MCP_SERVER_SECRET_KEY, secretKey, null)) {
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
    void testInitializeWithoutBearerTokenReturnsUnauthorizedWithEmptyBody() throws Exception {
        seedApiKey(null, ENVIRONMENT);

        HttpResponse<String> httpResponse = postInitialize(MCP_SERVER_SECRET_KEY, null, null);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
        assertThat(httpResponse.body()).isEmpty();
    }

    @Test
    void testInitializeWithTypedApiKeyIsRejected() throws Exception {
        String secretKey = seedApiKey(PlatformType.AUTOMATION, ENVIRONMENT);

        HttpResponse<String> httpResponse = postInitialize(MCP_SERVER_SECRET_KEY, secretKey, null);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testInitializeWithEnvironmentMismatchIsRejected() throws Exception {
        String secretKey = seedApiKey(null, ENVIRONMENT);

        HttpResponse<String> httpResponse = postInitialize(MCP_SERVER_SECRET_KEY, secretKey, "STAGING");

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testInitializeWithWrongPathSecretIsRejected() throws Exception {
        String secretKey = seedApiKey(null, ENVIRONMENT);

        HttpResponse<String> httpResponse = postInitialize("wrong-server-secret", secretKey, null);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    private String seedApiKey(PlatformType type, Environment environment) {
        String secretKey = String.valueOf(TenantKey.of());

        ApiKey apiKey = new ApiKey();

        apiKey.setName("test");
        apiKey.setSecretKey(secretKey);
        apiKey.setType(type);
        apiKey.setEnvironment(environment);
        apiKey.setUserId(USER_ID);

        apiKeyRepository.save(apiKey);

        return secretKey;
    }

    private McpSyncClient createMcpSyncClient(String pathSecret, String bearerSecret, String environmentHeader) {
        String baseUrl = "http://localhost:" + port;
        String endpoint = "/api/management/" + pathSecret + "/mcp";

        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder(baseUrl)
            .endpoint(endpoint)
            .httpRequestCustomizer((httpRequestBuilder, method, uri, body, transportContext) -> {
                if (bearerSecret != null) {
                    httpRequestBuilder.header("Authorization", "Bearer " + bearerSecret);
                }

                if (environmentHeader != null) {
                    httpRequestBuilder.header("X-ENVIRONMENT", environmentHeader);
                }
            })
            .build();

        return McpClient.sync(transport)
            .requestTimeout(Duration.ofSeconds(30))
            .build();
    }

    private HttpResponse<String> postInitialize(String pathSecret, String bearerSecret, String environmentHeader)
        throws Exception {

        String initializeRequest = """
            {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05",\
            "capabilities":{},"clientInfo":{"name":"test-client","version":"1.0.0"}}}""";

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/api/management/" + pathSecret + "/mcp"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .POST(HttpRequest.BodyPublishers.ofString(initializeRequest));

        if (bearerSecret != null) {
            requestBuilder.header("Authorization", "Bearer " + bearerSecret);
        }

        if (environmentHeader != null) {
            requestBuilder.header("X-ENVIRONMENT", environmentHeader);
        }

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}

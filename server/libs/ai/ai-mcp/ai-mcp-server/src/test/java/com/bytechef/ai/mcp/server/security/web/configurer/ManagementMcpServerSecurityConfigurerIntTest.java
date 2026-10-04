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

package com.bytechef.ai.mcp.server.security.web.configurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

import com.bytechef.ai.mcp.server.config.ManagementMcpServerSecurityIntTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.repository.ApiKeyRepository;
import com.bytechef.platform.security.service.ApiKeyService;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @author Ivica Cardic
 */
class ManagementMcpServerSecurityConfigurerIntTest {

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(
        classes = FilterChain.SecurityFilterChainTestConfiguration.class)
    @WebAppConfiguration
    class FilterChain {

        private static final String API_SECRET_KEY = String.valueOf(TenantKey.of());
        private static final String MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of());
        private static final String WRONG_MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of());

        @Autowired
        private ApiKeyService apiKeyService;

        @Autowired
        private AuthorityService authorityService;

        @Autowired
        private PropertyService propertyService;

        @Autowired
        private UserService userService;

        @Autowired
        private WebApplicationContext webApplicationContext;

        private MockMvc mockMvc;

        @BeforeEach
        void beforeEach() {
            reset(apiKeyService, authorityService, propertyService, userService);

            Property property = mock(Property.class);

            when(property.get("secretKey")).thenReturn(MCP_SERVER_SECRET_KEY);
            when(property.get("authenticationRequired")).thenReturn(true);
            when(propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null)).thenReturn(property);
            when(propertyService.fetchProperty("mcp.server", Property.Scope.PLATFORM, null))
                .thenReturn(Optional.of(property));

            mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        }

        @Test
        void testRequestWithoutBearerTokenIsRejected() throws Exception {
            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/management/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/management/%s/mcp".formatted(MCP_SERVER_SECRET_KEY)))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        @Test
        void testRequestWithValidAdminApiKeySucceeds() throws Exception {
            mockApiKey(null, Environment.PRODUCTION);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/management/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/management/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .header("Authorization", "Bearer " + API_SECRET_KEY))
                .andExpect(MockMvcResultMatchers.status()
                    .isOk());
        }

        @Test
        void testRequestWithTypedApiKeyIsRejected() throws Exception {
            mockApiKey(PlatformType.AUTOMATION, Environment.PRODUCTION);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/management/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/management/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .header("Authorization", "Bearer " + API_SECRET_KEY))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        @Test
        void testRequestWithWrongPathSecretIsRejected() throws Exception {
            mockApiKey(null, Environment.PRODUCTION);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/management/%s/mcp".formatted(WRONG_MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/management/%s/mcp".formatted(WRONG_MCP_SERVER_SECRET_KEY))
                        .header("Authorization", "Bearer " + API_SECRET_KEY))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        @Test
        void testRequestWithEnvironmentMismatchIsRejected() throws Exception {
            mockApiKey(null, Environment.PRODUCTION);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/management/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/management/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .header("Authorization", "Bearer " + API_SECRET_KEY)
                        .header("X-ENVIRONMENT", "STAGING"))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        private void mockApiKey(PlatformType type, Environment environment) {
            ApiKey apiKey = new ApiKey();

            apiKey.setId(7L);
            apiKey.setName("test");
            apiKey.setSecretKey(API_SECRET_KEY);

            if (type != null) {
                apiKey.setType(type);
            }

            apiKey.setEnvironment(environment);
            apiKey.setUserId(100L);

            when(apiKeyService.fetchApiKey(API_SECRET_KEY)).thenReturn(Optional.of(apiKey));

            User user = mock(User.class);

            when(user.isActivated()).thenReturn(true);
            when(user.getLogin()).thenReturn("admin@localhost.com");
            when(user.getAuthorityIds()).thenReturn(List.of());
            when(userService.fetchUser(100L)).thenReturn(Optional.of(user));
        }

        @Configuration
        @EnableWebMvc
        @EnableWebSecurity
        static class SecurityFilterChainTestConfiguration {

            @Bean
            ApiKeyService apiKeyService() {
                return mock(ApiKeyService.class);
            }

            @Bean
            AuthorityService authorityService() {
                return mock(AuthorityService.class);
            }

            @Bean
            PropertyService propertyService() {
                return mock(PropertyService.class);
            }

            @Bean
            UserService userService() {
                return mock(UserService.class);
            }

            @Bean
            RouterFunction<ServerResponse> mcpStubRouterFunction() {
                return RouterFunctions.route()
                    .POST("/api/management/{secretKey}/mcp", request -> ServerResponse.ok()
                        .build())
                    .build();
            }

            @Bean
            SecurityFilterChain securityFilterChain(
                HttpSecurity http, ApiKeyService apiKeyService, AuthorityService authorityService,
                PropertyService propertyService, UserService userService) throws Exception {

                return http
                    .authorizeHttpRequests(authorize -> authorize.anyRequest()
                        .permitAll())
                    .with(
                        new ManagementMcpServerSecurityConfigurer(
                            apiKeyService, authorityService, propertyService, userService),
                        Customizer.withDefaults())
                    .build();
            }
        }
    }

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @SpringBootTest(
        classes = ManagementMcpServerSecurityIntTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @Import(PostgreSQLContainerConfiguration.class)
    class LiveServer {

        private static final Environment ENVIRONMENT = Environment.PRODUCTION;
        private static final String MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of());
        private static final String WRONG_MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of());
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
            reset(authorityService, propertyService, userService);

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            jdbcTemplate.update("DELETE FROM api_key");

            mockProperty(true);

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

                assertThat(listToolsResult.tools())
                    .extracting(McpSchema.Tool::name)
                    .containsExactly(ManagementMcpServerSecurityIntTestConfiguration.SEEDED_TOOL_NAME);
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

            HttpResponse<String> httpResponse = postInitialize(WRONG_MCP_SERVER_SECRET_KEY, secretKey, null);

            assertThat(httpResponse.statusCode()).isEqualTo(401);
        }

        @Test
        void testInitializeWithoutBearerTokenWhenAuthenticationNotRequiredSucceeds() {
            mockProperty(false);

            try (McpSyncClient mcpSyncClient = createMcpSyncClient(MCP_SERVER_SECRET_KEY, null, null)) {
                McpSchema.InitializeResult initializeResult = mcpSyncClient.initialize();

                assertThat(initializeResult).isNotNull();
                assertThat(initializeResult.serverInfo()
                    .name()).isEqualTo("mcp-server");

                McpSchema.ListToolsResult listToolsResult = mcpSyncClient.listTools();

                assertThat(listToolsResult.tools())
                    .extracting(McpSchema.Tool::name)
                    .containsExactly(ManagementMcpServerSecurityIntTestConfiguration.SEEDED_TOOL_NAME);
            }
        }

        @Test
        void testTokenIgnoredWhenAuthenticationNotRequired() throws Exception {
            String secretKey = seedApiKey(PlatformType.AUTOMATION, ENVIRONMENT);

            mockProperty(false);

            HttpResponse<String> httpResponse = postInitialize(MCP_SERVER_SECRET_KEY, secretKey, null);

            assertThat(httpResponse.statusCode()).isEqualTo(200);
        }

        @Test
        void testInitializeWithoutBearerTokenWhenLegacyPropertyOmitsAuthenticationRequiredSucceeds() throws Exception {
            mockProperty(null);

            HttpResponse<String> httpResponse = postInitialize(MCP_SERVER_SECRET_KEY, null, null);

            assertThat(httpResponse.statusCode()).isEqualTo(200);
        }

        private void mockProperty(Boolean authenticationRequired) {
            Property property = mock(Property.class);

            when(property.get("secretKey")).thenReturn(MCP_SERVER_SECRET_KEY);
            when(property.get("authenticationRequired")).thenReturn(authenticationRequired);
            when(propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null)).thenReturn(property);
            when(propertyService.fetchProperty("mcp.server", Property.Scope.PLATFORM, null))
                .thenReturn(Optional.of(property));
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
}

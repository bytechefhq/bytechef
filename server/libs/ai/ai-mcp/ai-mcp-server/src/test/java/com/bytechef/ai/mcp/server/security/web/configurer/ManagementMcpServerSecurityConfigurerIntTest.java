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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

import com.bytechef.ai.mcp.server.config.ManagementMcpServerSecurityIntTestConfiguration;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.server.security.web.configurer.AutomationA2AServerSecurityConfigurer;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.automation.ai.mcp.server.security.web.configurer.AutomationMcpServerSecurityConfigurer;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.repository.ApiKeyRepository;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
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
import java.util.concurrent.atomic.AtomicReference;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
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
        private static final String CANONICAL_MCP_PATH = "/api/management/mcp";
        private static final String CANONICAL_TENANT_ID = "acme";
        private static final String CANONICAL_API_SECRET_KEY = String.valueOf(TenantKey.of(CANONICAL_TENANT_ID));
        private static final AtomicReference<String> CAPTURED_TENANT_ID = new AtomicReference<>();
        private static final String MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of());
        private static final String NON_TENANT_BEARER_TOKEN = "not-a-tenant-key";
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

            CAPTURED_TENANT_ID.set(null);

            mockMcpServerProperty();

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
        void testRequestWithUnparseablePathSecretIsRejected() throws Exception {
            mockApiKey(null, Environment.PRODUCTION);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/management/wrong-secret/mcp")
                        .servletPath("/api/management/wrong-secret/mcp")
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

        @Test
        void testUiSessionWithNonTenantBearerTokenIsRejected() throws Exception {
            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/management/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/management/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .session(createUiSession())
                        .header("Authorization", "Bearer " + NON_TENANT_BEARER_TOKEN))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        @Test
        void testUiSessionWithoutBearerTokenIsRejected() throws Exception {
            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/management/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/management/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .session(createUiSession()))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        @Test
        void testUiSessionWithValidAdminApiKeySucceeds() throws Exception {
            mockApiKey(null, Environment.PRODUCTION);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/management/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/management/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .session(createUiSession())
                        .header("Authorization", "Bearer " + API_SECRET_KEY))
                .andExpect(MockMvcResultMatchers.status()
                    .isOk());
        }

        @Test
        void testUiSessionWithoutBearerTokenIsRejectedForALegacyAuthenticationNotRequiredProperty() throws Exception {
            mockLegacyMcpServerProperty(false);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/management/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/management/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .session(createUiSession()))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        @Test
        void testCanonicalEndpointAcceptsApiKeyAndResolvesTenantFromKey() throws Exception {
            mockApiKey(CANONICAL_API_SECRET_KEY, null, Environment.PRODUCTION);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post(CANONICAL_MCP_PATH)
                        .servletPath(CANONICAL_MCP_PATH)
                        .header("Authorization", "Bearer " + CANONICAL_API_SECRET_KEY))
                .andExpect(MockMvcResultMatchers.status()
                    .isOk());

            assertThat(CAPTURED_TENANT_ID.get()).isEqualTo(CANONICAL_TENANT_ID);
        }

        @Test
        void testCanonicalEndpointRejectsTenantWithoutManagementServer() throws Exception {
            mockApiKey(CANONICAL_API_SECRET_KEY, null, Environment.PRODUCTION);

            when(propertyService.fetchProperty("mcp.server", Property.Scope.PLATFORM, null))
                .thenReturn(Optional.empty());

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post(CANONICAL_MCP_PATH)
                        .servletPath(CANONICAL_MCP_PATH)
                        .header("Authorization", "Bearer " + CANONICAL_API_SECRET_KEY))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());

            assertThat(CAPTURED_TENANT_ID.get()).isNull();
        }

        @Test
        void testCanonicalEndpointRejectsNonTenantKeyBearer() throws Exception {
            mockMvc
                .perform(
                    MockMvcRequestBuilders.post(CANONICAL_MCP_PATH)
                        .servletPath(CANONICAL_MCP_PATH)
                        .header("Authorization", "Bearer " + NON_TENANT_BEARER_TOKEN))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        @Test
        void testCanonicalEndpointRejectsRequestWithoutBearerToken() throws Exception {
            mockMvc
                .perform(
                    MockMvcRequestBuilders.post(CANONICAL_MCP_PATH)
                        .servletPath(CANONICAL_MCP_PATH))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        @Test
        void testCanonicalEndpointRejectsTypedApiKey() throws Exception {
            mockApiKey(CANONICAL_API_SECRET_KEY, PlatformType.AUTOMATION, Environment.PRODUCTION);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post(CANONICAL_MCP_PATH)
                        .servletPath(CANONICAL_MCP_PATH)
                        .header("Authorization", "Bearer " + CANONICAL_API_SECRET_KEY))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        @Test
        void testTenantAnchoredEndpointStillWorks() throws Exception {
            mockApiKey(null, Environment.PRODUCTION);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/management/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/management/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .header("Authorization", "Bearer " + API_SECRET_KEY))
                .andExpect(MockMvcResultMatchers.status()
                    .isOk());
        }

        private static MockHttpSession createUiSession() {
            MockHttpSession mockHttpSession = new MockHttpSession();

            mockHttpSession.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(
                    UsernamePasswordAuthenticationToken.authenticated(
                        "admin@localhost.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))));

            return mockHttpSession;
        }

        private Property mockMcpServerProperty() {
            Property property = mock(Property.class);

            when(property.get("secretKey")).thenReturn(MCP_SERVER_SECRET_KEY);
            when(propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null)).thenReturn(property);
            when(propertyService.fetchProperty("mcp.server", Property.Scope.PLATFORM, null))
                .thenReturn(Optional.of(property));

            return property;
        }

        private void mockLegacyMcpServerProperty(boolean authenticationRequired) {
            Property property = mockMcpServerProperty();

            when(property.get("authenticationRequired")).thenReturn(authenticationRequired);
        }

        private void mockApiKey(PlatformType type, Environment environment) {
            mockApiKey(API_SECRET_KEY, type, environment);
        }

        private void mockApiKey(String secretKey, PlatformType type, Environment environment) {
            ApiKey apiKey = new ApiKey();

            apiKey.setId(7L);
            apiKey.setName("test");
            apiKey.setSecretKey(secretKey);

            if (type != null) {
                apiKey.setType(type);
            }

            apiKey.setEnvironment(environment);
            apiKey.setUserId(100L);

            when(apiKeyService.fetchApiKey(secretKey)).thenReturn(Optional.of(apiKey));

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
                    .POST(CANONICAL_MCP_PATH, request -> {
                        CAPTURED_TENANT_ID.set(TenantContext.getCurrentTenantId());

                        return ServerResponse.ok()
                            .build();
                    })
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
                            apiKeyService, authorityService, () -> List.of(Environment.values()), propertyService,
                            userService),
                        Customizer.withDefaults())
                    .build();
            }
        }
    }

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(classes = SharedFilterChain.SharedSecurityFilterChainTestConfiguration.class)
    @WebAppConfiguration
    class SharedFilterChain {

        private static final String API_SECRET_KEY = String.valueOf(TenantKey.of());
        private static final String A2A_SERVER_SECRET_KEY = String.valueOf(TenantKey.of());
        private static final String AUTOMATION_MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of());
        private static final String MANAGEMENT_MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of());

        @Autowired
        private A2aServerService a2aServerService;

        @Autowired
        private ApiKeyService apiKeyService;

        @Autowired
        private AuthorityService authorityService;

        @Autowired
        private McpServerService mcpServerService;

        @Autowired
        private PropertyService propertyService;

        @Autowired
        private UserService userService;

        @Autowired
        private WebApplicationContext webApplicationContext;

        private MockMvc mockMvc;

        @BeforeEach
        void beforeEach() {
            reset(a2aServerService, apiKeyService, authorityService, mcpServerService, propertyService, userService);

            Property property = mock(Property.class);

            when(property.get("secretKey")).thenReturn(MANAGEMENT_MCP_SERVER_SECRET_KEY);
            when(propertyService.fetchProperty("mcp.server", Property.Scope.PLATFORM, null))
                .thenReturn(Optional.of(property));
            when(propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null)).thenReturn(property);
            when(mcpServerService.getMcpServer(anyString())).thenThrow(new IllegalArgumentException());

            mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        }

        @Test
        void testAnonymousAutomationMcpServerSecretIsRejectedOnManagementEndpoint() throws Exception {
            mockAutomationMcpServer(false);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/management/%s/mcp".formatted(AUTOMATION_MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/management/%s/mcp".formatted(AUTOMATION_MCP_SERVER_SECRET_KEY)))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        @Test
        void testAutomationApiKeyWithAutomationMcpServerSecretIsRejectedOnManagementEndpoint() throws Exception {
            mockAutomationMcpServer(true);
            mockApiKey(PlatformType.AUTOMATION);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/management/%s/mcp".formatted(AUTOMATION_MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/management/%s/mcp".formatted(AUTOMATION_MCP_SERVER_SECRET_KEY))
                        .header("Authorization", "Bearer " + API_SECRET_KEY))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        @Test
        void testAnonymousA2aServerSecretIsRejectedOnManagementEndpoint() throws Exception {
            mockA2aServer(false);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/management/%s/mcp".formatted(A2A_SERVER_SECRET_KEY))
                        .servletPath("/api/management/%s/mcp".formatted(A2A_SERVER_SECRET_KEY)))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        @Test
        void testAutomationApiKeyWithA2aServerSecretIsRejectedOnManagementEndpoint() throws Exception {
            mockA2aServer(true);
            mockApiKey(PlatformType.AUTOMATION);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/management/%s/mcp".formatted(A2A_SERVER_SECRET_KEY))
                        .servletPath("/api/management/%s/mcp".formatted(A2A_SERVER_SECRET_KEY))
                        .header("Authorization", "Bearer " + API_SECRET_KEY))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        @Test
        void testAdminApiKeyIsAcceptedOnManagementEndpoint() throws Exception {
            mockApiKey(null);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/management/%s/mcp".formatted(MANAGEMENT_MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/management/%s/mcp".formatted(MANAGEMENT_MCP_SERVER_SECRET_KEY))
                        .header("Authorization", "Bearer " + API_SECRET_KEY))
                .andExpect(MockMvcResultMatchers.status()
                    .isOk());
        }

        @Test
        void testAnonymousAutomationMcpServerIsStillAcceptedOnAutomationEndpoint() throws Exception {
            mockAutomationMcpServer(false);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/automation/%s/mcp".formatted(AUTOMATION_MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/automation/%s/mcp".formatted(AUTOMATION_MCP_SERVER_SECRET_KEY)))
                .andExpect(MockMvcResultMatchers.status()
                    .isOk());
        }

        private void mockA2aServer(boolean authenticationRequired) {
            A2aServer a2aServer = new A2aServer("a2a", null, Environment.PRODUCTION);

            a2aServer.setId(3L);
            a2aServer.setAuthenticationRequired(authenticationRequired);
            a2aServer.setEnabled(true);

            when(a2aServerService.fetchA2aServer(A2A_SERVER_SECRET_KEY)).thenReturn(Optional.of(a2aServer));
        }

        private void mockApiKey(PlatformType type) {
            ApiKey apiKey = new ApiKey();

            apiKey.setId(7L);
            apiKey.setName("test");
            apiKey.setSecretKey(API_SECRET_KEY);

            if (type != null) {
                apiKey.setType(type);
            }

            apiKey.setEnvironment(Environment.PRODUCTION);
            apiKey.setUserId(100L);

            when(apiKeyService.fetchApiKey(API_SECRET_KEY)).thenReturn(Optional.of(apiKey));

            User user = mock(User.class);

            when(user.isActivated()).thenReturn(true);
            when(user.getLogin()).thenReturn("admin@localhost.com");
            when(user.getAuthorityIds()).thenReturn(List.of());
            when(userService.fetchUser(100L)).thenReturn(Optional.of(user));
        }

        private void mockAutomationMcpServer(boolean authenticationRequired) {
            McpServer mcpServer = mock(McpServer.class);

            when(mcpServer.getId()).thenReturn(5L);
            when(mcpServer.getType()).thenReturn(PlatformType.AUTOMATION);
            when(mcpServer.getEnvironment()).thenReturn(Environment.PRODUCTION);
            when(mcpServer.isEnabled()).thenReturn(true);
            when(mcpServer.isAuthenticationRequired()).thenReturn(authenticationRequired);
            doReturn(mcpServer).when(mcpServerService)
                .getMcpServer(AUTOMATION_MCP_SERVER_SECRET_KEY);
        }

        @Configuration
        @EnableWebMvc
        @EnableWebSecurity
        static class SharedSecurityFilterChainTestConfiguration {

            @Bean
            A2aServerService a2aServerService() {
                return mock(A2aServerService.class);
            }

            @Bean
            ApiKeyService apiKeyService() {
                return mock(ApiKeyService.class);
            }

            @Bean
            AuthorityService authorityService() {
                return mock(AuthorityService.class);
            }

            @Bean
            McpServerService mcpServerService() {
                return mock(McpServerService.class);
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
                    .POST("/api/automation/{secretKey}/mcp", request -> ServerResponse.ok()
                        .build())
                    .build();
            }

            @Bean
            SecurityFilterChain securityFilterChain(
                HttpSecurity http, A2aServerService a2aServerService, ApiKeyService apiKeyService,
                AuthorityService authorityService, McpServerService mcpServerService, PropertyService propertyService,
                UserService userService) throws Exception {

                EnvironmentService environmentService = () -> List.of(Environment.values());

                return http
                    .authorizeHttpRequests(authorize -> authorize.anyRequest()
                        .permitAll())
                    .with(
                        new AutomationMcpServerSecurityConfigurer(
                            apiKeyService, authorityService, environmentService, mcpServerService, userService),
                        Customizer.withDefaults())
                    .with(
                        new AutomationA2AServerSecurityConfigurer(
                            apiKeyService, authorityService, environmentService, a2aServerService, userService),
                        Customizer.withDefaults())
                    .with(
                        new ManagementMcpServerSecurityConfigurer(
                            apiKeyService, authorityService, environmentService, propertyService, userService),
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

        private static final String CANONICAL_MCP_PATH = "/api/management/mcp";
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

            mockProperty();

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

            try (McpSyncClient mcpSyncClient = createMcpSyncClient(
                "/api/management/" + MCP_SERVER_SECRET_KEY + "/mcp", secretKey, null)) {
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
        void testCanonicalEndpointInitializesAndListsToolsWithValidAdminApiKey() {
            String secretKey = seedApiKey(null, ENVIRONMENT);

            try (McpSyncClient mcpSyncClient = createMcpSyncClient(CANONICAL_MCP_PATH, secretKey, null)) {
                McpSchema.InitializeResult initializeResult = mcpSyncClient.initialize();

                assertThat(initializeResult.serverInfo()
                    .name()).isEqualTo("mcp-server");

                McpSchema.ListToolsResult listToolsResult = mcpSyncClient.listTools();

                assertThat(listToolsResult.tools())
                    .extracting(McpSchema.Tool::name)
                    .containsExactly(ManagementMcpServerSecurityIntTestConfiguration.SEEDED_TOOL_NAME);
            }
        }

        @Test
        void testCanonicalEndpointWithoutBearerTokenIsRejected() throws Exception {
            seedApiKey(null, ENVIRONMENT);

            HttpResponse<String> httpResponse = postInitializeTo(CANONICAL_MCP_PATH, null);

            assertThat(httpResponse.statusCode()).isEqualTo(401);
        }

        @Test
        void testCanonicalEndpointIsRejectedWhenTenantHasNoManagementServer() throws Exception {
            String secretKey = seedApiKey(null, ENVIRONMENT);

            when(propertyService.fetchProperty("mcp.server", Property.Scope.PLATFORM, null))
                .thenReturn(Optional.empty());

            HttpResponse<String> httpResponse = postInitializeTo(CANONICAL_MCP_PATH, secretKey);

            assertThat(httpResponse.statusCode()).isEqualTo(401);
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
        void testInitializeWithoutBearerTokenIsRejectedForALegacyAuthenticationNotRequiredProperty()
            throws Exception {

            mockLegacyProperty(false);

            HttpResponse<String> httpResponse = postInitialize(MCP_SERVER_SECRET_KEY, null, null);

            assertThat(httpResponse.statusCode()).isEqualTo(401);
        }

        @Test
        void testTypedApiKeyIsRejectedForALegacyAuthenticationNotRequiredProperty() throws Exception {
            String secretKey = seedApiKey(PlatformType.AUTOMATION, ENVIRONMENT);

            mockLegacyProperty(false);

            HttpResponse<String> httpResponse = postInitialize(MCP_SERVER_SECRET_KEY, secretKey, null);

            assertThat(httpResponse.statusCode()).isEqualTo(401);
        }

        @Test
        void testInitializeWithoutBearerTokenWhenLegacyPropertyOmitsAuthenticationRequiredIsRejected()
            throws Exception {

            mockLegacyProperty(null);

            HttpResponse<String> httpResponse = postInitialize(MCP_SERVER_SECRET_KEY, null, null);

            assertThat(httpResponse.statusCode()).isEqualTo(401);
        }

        private Property mockProperty() {
            Property property = mock(Property.class);

            when(property.get("secretKey")).thenReturn(MCP_SERVER_SECRET_KEY);
            when(propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null)).thenReturn(property);
            when(propertyService.fetchProperty("mcp.server", Property.Scope.PLATFORM, null))
                .thenReturn(Optional.of(property));

            return property;
        }

        private void mockLegacyProperty(Boolean authenticationRequired) {
            Property property = mockProperty();

            when(property.get("authenticationRequired")).thenReturn(authenticationRequired);
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

        private McpSyncClient createMcpSyncClient(String endpoint, String bearerSecret, String environmentHeader) {
            String baseUrl = "http://localhost:" + port;

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

        private HttpResponse<String> postInitializeTo(String endpoint, String bearerSecret) throws Exception {
            return postInitializeTo(endpoint, bearerSecret, null);
        }

        private HttpResponse<String> postInitialize(String pathSecret, String bearerSecret, String environmentHeader)
            throws Exception {

            return postInitializeTo("/api/management/" + pathSecret + "/mcp", bearerSecret, environmentHeader);
        }

        private HttpResponse<String> postInitializeTo(String endpoint, String bearerSecret, String environmentHeader)
            throws Exception {

            String initializeRequest = """
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05",\
                "capabilities":{},"clientInfo":{"name":"test-client","version":"1.0.0"}}}""";

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + endpoint))
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

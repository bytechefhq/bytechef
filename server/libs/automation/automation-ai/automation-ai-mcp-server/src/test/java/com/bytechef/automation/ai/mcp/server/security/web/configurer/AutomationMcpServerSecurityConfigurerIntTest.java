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

package com.bytechef.automation.ai.mcp.server.security.web.configurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

import com.bytechef.automation.ai.mcp.server.config.AutomationMcpServerSecurityIntTestConfiguration;
import com.bytechef.automation.ai.mcp.server.facade.AutomationMcpToolFacade;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.repository.ApiKeyRepository;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.tool.function.FunctionToolCallback;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
class AutomationMcpServerSecurityConfigurerIntTest {

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(
        classes = FilterChain.SecurityFilterChainTestConfiguration.class)
    @WebAppConfiguration
    class FilterChain {

        private static final String API_SECRET_KEY = String.valueOf(TenantKey.of());
        private static final String MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of());
        private static final String OTHER_TENANT_ID = "tenantb";
        private static final String OTHER_TENANT_MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of(OTHER_TENANT_ID));

        @Autowired
        private ApiKeyService apiKeyService;

        @Autowired
        private AuthorityService authorityService;

        @Autowired
        private McpServerService mcpServerService;

        @Autowired
        private UserService userService;

        @Autowired
        private WebApplicationContext webApplicationContext;

        private MockMvc mockMvc;

        @BeforeEach
        void beforeEach() {
            reset(apiKeyService, authorityService, mcpServerService, userService);

            mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        }

        @Test
        void testRequestWithoutBearerTokenIsRejected() throws Exception {
            mockMcpServer(Environment.PRODUCTION);

            mockMvc
                .perform(MockMvcRequestBuilders.post("/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                    .servletPath("/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY)))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        @Test
        void testRequestWithValidApiKeySucceeds() throws Exception {
            mockApiKey(PlatformType.AUTOMATION, Environment.PRODUCTION);
            mockMcpServer(Environment.PRODUCTION);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .header("Authorization", "Bearer " + API_SECRET_KEY))
                .andExpect(MockMvcResultMatchers.status()
                    .isOk());
        }

        @Test
        void testRequestWithUnknownApiKeyIsRejected() throws Exception {
            mockMcpServer(Environment.PRODUCTION);

            when(apiKeyService.fetchApiKey(API_SECRET_KEY)).thenReturn(Optional.empty());

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .header("Authorization", "Bearer " + API_SECRET_KEY))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        @Test
        void testRequestWithWrongTypeApiKeyIsRejected() throws Exception {
            mockApiKey(PlatformType.EMBEDDED, Environment.PRODUCTION);
            mockMcpServer(Environment.PRODUCTION);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .header("Authorization", "Bearer " + API_SECRET_KEY))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        @Test
        void testRequestWithEnvironmentMismatchIsRejected() throws Exception {
            mockApiKey(PlatformType.AUTOMATION, Environment.STAGING);
            mockMcpServer(Environment.PRODUCTION);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .header("Authorization", "Bearer " + API_SECRET_KEY))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        @Test
        void testRequestWithUnknownMcpServerSecretKeyIsRejected() throws Exception {
            mockApiKey(PlatformType.AUTOMATION, Environment.PRODUCTION);

            when(mcpServerService.getMcpServer(MCP_SERVER_SECRET_KEY)).thenThrow(new IllegalArgumentException());

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .header("Authorization", "Bearer " + API_SECRET_KEY))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        @Test
        void testRequestWithValidApiKeyForAnEmbeddedMcpServerSecretKeyIsRejected() throws Exception {
            mockApiKey(PlatformType.AUTOMATION, Environment.PRODUCTION);
            mockMcpServer(Environment.PRODUCTION, PlatformType.EMBEDDED, true);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .header("Authorization", "Bearer " + API_SECRET_KEY))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        @Test
        void testRequestWithoutBearerTokenForAnEmbeddedMcpServerSecretKeyIsRejected() throws Exception {
            mockMcpServer(Environment.PRODUCTION, PlatformType.EMBEDDED, false);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY)))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        @Test
        void testRequestWithoutBearerTokenResolvesMcpServerInsidePathTenant() throws Exception {
            List<String> mcpServerLookupTenantIds = mockOtherTenantMcpServer(false);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/automation/%s/mcp".formatted(OTHER_TENANT_MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/automation/%s/mcp".formatted(OTHER_TENANT_MCP_SERVER_SECRET_KEY)))
                .andExpect(MockMvcResultMatchers.status()
                    .isOk());

            assertThat(mcpServerLookupTenantIds).containsExactly(OTHER_TENANT_ID);
        }

        @Test
        void testCrossTenantApiKeyIsRejectedWhenAuthenticationIsRequired() throws Exception {
            mockApiKey(PlatformType.AUTOMATION, Environment.PRODUCTION);
            mockOtherTenantMcpServer(true);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/automation/%s/mcp".formatted(OTHER_TENANT_MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/automation/%s/mcp".formatted(OTHER_TENANT_MCP_SERVER_SECRET_KEY))
                        .header("Authorization", "Bearer " + API_SECRET_KEY))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        @Test
        void testCrossTenantApiKeyIsIgnoredWhenAuthenticationIsNotRequired() throws Exception {
            List<String> mcpServerLookupTenantIds = mockOtherTenantMcpServer(false);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/automation/%s/mcp".formatted(OTHER_TENANT_MCP_SERVER_SECRET_KEY))
                        .servletPath("/api/automation/%s/mcp".formatted(OTHER_TENANT_MCP_SERVER_SECRET_KEY))
                        .header("Authorization", "Bearer " + API_SECRET_KEY))
                .andExpect(MockMvcResultMatchers.status()
                    .isOk());

            assertThat(mcpServerLookupTenantIds).containsExactly(OTHER_TENANT_ID);
            verifyNoInteractions(apiKeyService);
        }

        @Test
        void testUnparseableMcpServerSecretKeyIsRejected() throws Exception {
            mockApiKey(PlatformType.AUTOMATION, Environment.PRODUCTION);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/api/automation/server-secret/mcp")
                        .servletPath("/api/automation/server-secret/mcp")
                        .header("Authorization", "Bearer " + API_SECRET_KEY))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());

            verifyNoInteractions(mcpServerService);
        }

        private void mockApiKey(PlatformType type, Environment environment) {
            ApiKey apiKey = new ApiKey();

            apiKey.setId(7L);
            apiKey.setName("test");
            apiKey.setSecretKey(API_SECRET_KEY);
            apiKey.setType(type);
            apiKey.setEnvironment(environment);
            apiKey.setUserId(100L);

            when(apiKeyService.fetchApiKey(API_SECRET_KEY)).thenReturn(Optional.of(apiKey));

            User user = mock(User.class);

            when(user.isActivated()).thenReturn(true);
            when(user.getLogin()).thenReturn("admin@localhost.com");
            when(user.getAuthorityIds()).thenReturn(List.of());
            when(userService.fetchUser(100L)).thenReturn(Optional.of(user));
        }

        private void mockMcpServer(Environment environment) {
            mockMcpServer(environment, PlatformType.AUTOMATION, true);
        }

        private void mockMcpServer(Environment environment, PlatformType type, boolean authenticationRequired) {
            McpServer mcpServer = mock(McpServer.class);

            when(mcpServer.getType()).thenReturn(type);
            when(mcpServer.getEnvironment()).thenReturn(environment);
            when(mcpServer.isEnabled()).thenReturn(true);
            when(mcpServer.isAuthenticationRequired()).thenReturn(authenticationRequired);
            when(mcpServerService.getMcpServer(MCP_SERVER_SECRET_KEY)).thenReturn(mcpServer);
        }

        private List<String> mockOtherTenantMcpServer(boolean authenticationRequired) {
            McpServer mcpServer = mock(McpServer.class);

            when(mcpServer.getType()).thenReturn(PlatformType.AUTOMATION);
            when(mcpServer.getEnvironment()).thenReturn(Environment.PRODUCTION);
            when(mcpServer.isEnabled()).thenReturn(true);
            when(mcpServer.isAuthenticationRequired()).thenReturn(authenticationRequired);

            List<String> mcpServerLookupTenantIds = new ArrayList<>();

            when(mcpServerService.getMcpServer(OTHER_TENANT_MCP_SERVER_SECRET_KEY)).thenAnswer(invocation -> {
                mcpServerLookupTenantIds.add(TenantContext.getCurrentTenantId());

                return mcpServer;
            });

            return mcpServerLookupTenantIds;
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
            McpServerService mcpServerService() {
                return mock(McpServerService.class);
            }

            @Bean
            UserService userService() {
                return mock(UserService.class);
            }

            @Bean
            RouterFunction<ServerResponse> mcpStubRouterFunction() {
                return RouterFunctions.route()
                    .POST("/api/automation/{secretKey}/mcp", request -> ServerResponse.ok()
                        .build())
                    .build();
            }

            @Bean
            SecurityFilterChain securityFilterChain(
                HttpSecurity http, ApiKeyService apiKeyService, AuthorityService authorityService,
                McpServerService mcpServerService, UserService userService) throws Exception {

                return http
                    .authorizeHttpRequests(authorize -> authorize.anyRequest()
                        .permitAll())
                    .with(
                        new AutomationMcpServerSecurityConfigurer(
                            apiKeyService, authorityService, mcpServerService, userService),
                        Customizer.withDefaults())
                    .build();
            }
        }
    }

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @SpringBootTest(
        classes = AutomationMcpServerSecurityIntTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @Import(PostgreSQLContainerConfiguration.class)
    class LiveServer {

        private static final Environment ENVIRONMENT = Environment.PRODUCTION;
        private static final long MCP_SERVER_ID = 1L;
        private static final String MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of());
        private static final String WRONG_MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of());
        private static final long ROLE_X_AUTHORITY_ID = 7L;
        private static final long USER_ID = 1050L;

        @Autowired
        private ApiKeyRepository apiKeyRepository;

        @Autowired
        private AuthorityService authorityService;

        @Autowired
        private DataSource dataSource;

        @Autowired
        private McpComponentService mcpComponentService;

        @Autowired
        private McpProjectService mcpProjectService;

        @Autowired
        private McpServerService mcpServerService;

        @MockitoBean
        private AutomationMcpToolFacade mcpToolFacade;

        @Autowired
        private McpToolService mcpToolService;

        @Autowired
        private UserService userService;

        @Autowired
        private WorkspaceMcpServerService workspaceMcpServerService;

        @LocalServerPort
        private int port;

        @BeforeEach
        void beforeEach() {
            reset(
                authorityService, mcpComponentService, mcpProjectService, mcpServerService, mcpToolService, userService,
                workspaceMcpServerService);

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            jdbcTemplate.update("DELETE FROM api_key");

            User user = mock(User.class);

            when(user.isActivated()).thenReturn(true);
            when(user.getLogin()).thenReturn("admin@localhost.com");
            when(user.getAuthorityIds()).thenReturn(List.of(ROLE_X_AUTHORITY_ID));
            when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

            Authority roleXAuthority = new Authority();

            roleXAuthority.setId(ROLE_X_AUTHORITY_ID);
            roleXAuthority.setName("ROLE_X");

            when(authorityService.fetchAuthority(ROLE_X_AUTHORITY_ID)).thenReturn(Optional.of(roleXAuthority));
            when(mcpToolFacade.getFunctionToolCallback(any(McpTool.class))).thenAnswer(invocation -> {
                McpTool mcpTool = invocation.getArgument(0);

                return functionToolCallback(mcpTool.getName());
            });
        }

        @AfterEach
        void afterEach() {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            jdbcTemplate.update("DELETE FROM api_key");
        }

        @Test
        void testInitializeAndListToolsWithValidApiKey() {
            String secretKey = seedApiKey(PlatformType.AUTOMATION, ENVIRONMENT);

            McpServer mcpServer = mockMcpServer(ENVIRONMENT, true);

            seedServerTool(mcpServer, "seededTool");

            try (McpSyncClient mcpSyncClient = createMcpSyncClient(MCP_SERVER_SECRET_KEY, secretKey, null)) {
                McpSchema.InitializeResult initializeResult = mcpSyncClient.initialize();

                assertThat(initializeResult).isNotNull();
                assertThat(initializeResult.serverInfo()
                    .name()).isEqualTo("automation-mcp-server");

                McpSchema.ListToolsResult listToolsResult = mcpSyncClient.listTools();

                assertThat(listToolsResult.tools())
                    .extracting(McpSchema.Tool::name)
                    .containsExactly("seededTool");
            }
        }

        @Test
        void testEnforcingServerListsAndCallsOnlyToolsGrantedByTheApiKeyUsersAuthorities() {
            String secretKey = seedApiKey(PlatformType.AUTOMATION, ENVIRONMENT);

            McpServer mcpServer = mockMcpServer(ENVIRONMENT, true);

            when(mcpServer.getId()).thenReturn(MCP_SERVER_ID);
            when(mcpServer.isEnforceToolAuthorization()).thenReturn(true);

            seedComponentTool(10L, "roleXTool");
            seedComponentTool(20L, "roleYTool");

            when(mcpComponentService.getMcpServerMcpComponents(MCP_SERVER_ID))
                .thenReturn(List.of(component(10L, "ROLE_X"), component(20L, "ROLE_Y")));

            try (McpSyncClient mcpSyncClient = createMcpSyncClient(MCP_SERVER_SECRET_KEY, secretKey, null)) {
                mcpSyncClient.initialize();

                McpSchema.ListToolsResult listToolsResult = mcpSyncClient.listTools();

                assertThat(listToolsResult.tools())
                    .extracting(McpSchema.Tool::name)
                    .containsExactly("roleXTool");

                McpSchema.CallToolResult callToolResult =
                    mcpSyncClient.callTool(new McpSchema.CallToolRequest("roleXTool", Map.of()));

                assertThat(callToolResult.isError()).isNotEqualTo(Boolean.TRUE);

                assertThatThrownBy(() -> mcpSyncClient.callTool(new McpSchema.CallToolRequest("roleYTool", Map.of())))
                    .isInstanceOf(McpError.class)
                    .hasMessageContaining("Unknown tool");
            }
        }

        @Test
        void testInitializeWithoutBearerTokenReturnsUnauthorizedWithEmptyBody() throws Exception {
            seedApiKey(PlatformType.AUTOMATION, ENVIRONMENT);

            mockMcpServer(ENVIRONMENT);

            HttpResponse<String> httpResponse = postInitialize(MCP_SERVER_SECRET_KEY, null, null);

            assertThat(httpResponse.statusCode()).isEqualTo(401);
            assertThat(httpResponse.body()).isEmpty();
        }

        @Test
        void testInitializeWithWrongTypeApiKeyIsRejected() throws Exception {
            String secretKey = seedApiKey(PlatformType.EMBEDDED, ENVIRONMENT);

            mockMcpServer(ENVIRONMENT);

            HttpResponse<String> httpResponse = postInitialize(MCP_SERVER_SECRET_KEY, secretKey, null);

            assertThat(httpResponse.statusCode()).isEqualTo(401);
        }

        @Test
        void testInitializeWithEnvironmentMismatchIsRejected() throws Exception {
            String secretKey = seedApiKey(PlatformType.AUTOMATION, Environment.STAGING);

            mockMcpServer(Environment.PRODUCTION);

            HttpResponse<String> httpResponse = postInitialize(MCP_SERVER_SECRET_KEY, secretKey, null);

            assertThat(httpResponse.statusCode()).isEqualTo(401);
        }

        @Test
        void testInitializeWithWrongPathSecretIsRejected() throws Exception {
            String secretKey = seedApiKey(PlatformType.AUTOMATION, ENVIRONMENT);

            when(mcpServerService.getMcpServer(WRONG_MCP_SERVER_SECRET_KEY)).thenThrow(new IllegalArgumentException());

            HttpResponse<String> httpResponse = postInitialize(WRONG_MCP_SERVER_SECRET_KEY, secretKey, null);

            assertThat(httpResponse.statusCode()).isEqualTo(401);
        }

        @Test
        void testInitializeWithoutBearerTokenWhenAuthenticationNotRequiredSucceeds() {
            McpServer mcpServer = mockMcpServer(ENVIRONMENT, false);

            seedServerTool(mcpServer, "seededTool");

            try (McpSyncClient mcpSyncClient = createMcpSyncClient(MCP_SERVER_SECRET_KEY, null, null)) {
                McpSchema.InitializeResult initializeResult = mcpSyncClient.initialize();

                assertThat(initializeResult).isNotNull();
                assertThat(initializeResult.serverInfo()
                    .name()).isEqualTo("automation-mcp-server");

                McpSchema.ListToolsResult listToolsResult = mcpSyncClient.listTools();

                assertThat(listToolsResult.tools())
                    .extracting(McpSchema.Tool::name)
                    .containsExactly("seededTool");
            }
        }

        @Test
        void testAnonymousCallerSeesNoToolsOnEnforcingServer() {
            McpServer mcpServer = mockMcpServer(ENVIRONMENT, false);

            when(mcpServer.isEnforceToolAuthorization()).thenReturn(true);

            seedServerTool(mcpServer, "seededTool");

            try (McpSyncClient mcpSyncClient = createMcpSyncClient(MCP_SERVER_SECRET_KEY, null, null)) {
                mcpSyncClient.initialize();

                McpSchema.ListToolsResult listToolsResult = mcpSyncClient.listTools();

                assertThat(listToolsResult.tools()).isEmpty();
            }
        }

        @Test
        void testTokenIgnoredWhenAuthenticationNotRequired() throws Exception {
            String secretKey = seedApiKey(PlatformType.EMBEDDED, ENVIRONMENT);

            mockMcpServer(ENVIRONMENT, false);

            HttpResponse<String> httpResponse = postInitialize(MCP_SERVER_SECRET_KEY, secretKey, null);

            assertThat(httpResponse.statusCode()).isEqualTo(200);
        }

        private static McpComponent component(long mcpComponentId, String requiredAuthority) {
            McpComponent mcpComponent = new McpComponent();

            mcpComponent.setId(mcpComponentId);
            mcpComponent.setRequiredAuthorities(Set.of(requiredAuthority));

            return mcpComponent;
        }

        private static FunctionToolCallback<Map<String, Object>, Object> functionToolCallback(String toolName) {
            Function<Map<String, Object>, Object> toolFunction = request -> "ok";

            return FunctionToolCallback.builder(toolName, toolFunction)
                .inputType(Map.class)
                .inputSchema("{\"type\":\"object\"}")
                .build();
        }

        private void mockMcpServer(Environment environment) {
            mockMcpServer(environment, true);
        }

        private McpServer mockMcpServer(Environment environment, boolean authenticationRequired) {
            McpServer mcpServer = mock(McpServer.class);

            when(mcpServer.getType()).thenReturn(PlatformType.AUTOMATION);
            when(mcpServer.getEnvironment()).thenReturn(environment);
            when(mcpServer.isAuthenticationRequired()).thenReturn(authenticationRequired);
            when(mcpServer.isEnabled()).thenReturn(true);
            when(mcpServerService.getMcpServer(MCP_SERVER_SECRET_KEY)).thenReturn(mcpServer);

            return mcpServer;
        }

        private void seedServerTool(McpServer mcpServer, String toolName) {
            when(mcpServer.getId()).thenReturn(MCP_SERVER_ID);

            seedComponentTool(30L, toolName);

            when(mcpComponentService.getMcpServerMcpComponents(MCP_SERVER_ID))
                .thenReturn(List.of(component(30L, "ROLE_X")));
        }

        private void seedComponentTool(long mcpComponentId, String toolName) {
            McpTool mcpTool = new McpTool(toolName, Map.of(), mcpComponentId);

            when(mcpToolService.getMcpComponentMcpTools(mcpComponentId)).thenReturn(List.of(mcpTool));
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
            String endpoint = "/api/automation/" + pathSecret + "/mcp";

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
                .uri(URI.create("http://localhost:" + port + "/api/automation/" + pathSecret + "/mcp"))
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

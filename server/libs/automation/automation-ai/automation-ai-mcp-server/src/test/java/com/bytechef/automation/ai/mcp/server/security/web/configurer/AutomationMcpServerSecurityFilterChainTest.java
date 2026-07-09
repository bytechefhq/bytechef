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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.domain.TenantKey;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
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
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AutomationMcpServerSecurityFilterChainTest.SecurityFilterChainTestConfiguration.class)
@WebAppConfiguration
class AutomationMcpServerSecurityFilterChainTest {

    private static final String API_SECRET_KEY = String.valueOf(TenantKey.of());
    private static final String MCP_SERVER_SECRET_KEY = "server-secret";

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
        McpServer mcpServer = mock(McpServer.class);

        when(mcpServer.getEnvironment()).thenReturn(environment);
        when(mcpServerService.getMcpServer(MCP_SERVER_SECRET_KEY)).thenReturn(mcpServer);
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

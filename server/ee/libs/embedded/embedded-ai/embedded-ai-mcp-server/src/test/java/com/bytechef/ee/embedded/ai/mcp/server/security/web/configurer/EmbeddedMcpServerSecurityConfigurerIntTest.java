/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.configurer;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.tenant.domain.TenantKey;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedMcpServerSecurityConfigurerIntTest {

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(classes = FilterChain.SecurityFilterChainTestConfiguration.class)
    @WebAppConfiguration
    class FilterChain {

        private static final long CONNECTED_USER_ID = 5L;
        private static final String EXTERNAL_USER_ID = "external-user";
        private static final KeyPair KEY_PAIR = generateRsaKeyPair();
        private static final String MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of());
        private static final String MCP_SERVER_PATH = "/api/embedded/%s/mcp".formatted(MCP_SERVER_SECRET_KEY);
        private static final String NON_JWT_BEARER_TOKEN = "not-a-signing-key-jwt";

        @Autowired
        private ConnectedUserService connectedUserService;

        @Autowired
        private McpServerService mcpServerService;

        @Autowired
        private SigningKeyService signingKeyService;

        @Autowired
        private WebApplicationContext webApplicationContext;

        private MockMvc mockMvc;

        @BeforeEach
        void beforeEach() {
            reset(connectedUserService, mcpServerService, signingKeyService);

            when(signingKeyService.getPublicKey(anyString(), anyLong())).thenReturn(KEY_PAIR.getPublic());

            mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        }

        @Test
        void testUiSessionWithNonJwtBearerTokenIsRejectedWhenAuthenticationIsRequired() throws Exception {
            mockMcpServer(true);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post(MCP_SERVER_PATH)
                        .servletPath(MCP_SERVER_PATH)
                        .session(createUiSession())
                        .header("Authorization", "Bearer " + NON_JWT_BEARER_TOKEN))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        @Test
        void testUiSessionWithoutBearerTokenIsRejectedWhenAuthenticationIsRequired() throws Exception {
            mockMcpServer(true);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post(MCP_SERVER_PATH)
                        .servletPath(MCP_SERVER_PATH)
                        .session(createUiSession()))
                .andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
        }

        @Test
        void testRequestWithSigningKeyJwtSucceeds() throws Exception {
            mockMcpServer(true);
            mockConnectedUser();

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post(MCP_SERVER_PATH)
                        .servletPath(MCP_SERVER_PATH)
                        .header("Authorization", "Bearer " + signJwt()))
                .andExpect(MockMvcResultMatchers.status()
                    .isOk());
        }

        @Test
        void testUiSessionWithSigningKeyJwtSucceeds() throws Exception {
            mockMcpServer(true);
            mockConnectedUser();

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post(MCP_SERVER_PATH)
                        .servletPath(MCP_SERVER_PATH)
                        .session(createUiSession())
                        .header("Authorization", "Bearer " + signJwt()))
                .andExpect(MockMvcResultMatchers.status()
                    .isOk());
        }

        @Test
        void testUiSessionWithoutBearerTokenSucceedsWhenAuthenticationIsNotRequired() throws Exception {
            mockMcpServer(false);

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post(MCP_SERVER_PATH)
                        .servletPath(MCP_SERVER_PATH)
                        .session(createUiSession()))
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

        private static KeyPair generateRsaKeyPair() {
            try {
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

                keyPairGenerator.initialize(2048);

                return keyPairGenerator.generateKeyPair();
            } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
                throw new IllegalStateException(noSuchAlgorithmException);
            }
        }

        private static String signJwt() {
            return Jwts.builder()
                .header()
                .keyId(String.valueOf(TenantKey.of()))
                .and()
                .subject(EXTERNAL_USER_ID)
                .signWith(KEY_PAIR.getPrivate())
                .compact();
        }

        private void mockConnectedUser() {
            ConnectedUser connectedUser = mock(ConnectedUser.class);

            when(connectedUser.getId()).thenReturn(CONNECTED_USER_ID);
            when(connectedUser.getExternalId()).thenReturn(EXTERNAL_USER_ID);
            when(connectedUser.isEnabled()).thenReturn(true);
            when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION.ordinal()))
                .thenReturn(Optional.of(connectedUser));
        }

        private void mockMcpServer(boolean authenticationRequired) {
            McpServer mcpServer = mock(McpServer.class);

            when(mcpServer.getType()).thenReturn(PlatformType.EMBEDDED);
            when(mcpServer.isEnabled()).thenReturn(true);
            when(mcpServer.isAuthenticationRequired()).thenReturn(authenticationRequired);
            when(mcpServerService.getMcpServer(MCP_SERVER_SECRET_KEY)).thenReturn(mcpServer);
        }

        @Configuration
        @EnableWebMvc
        @EnableWebSecurity
        static class SecurityFilterChainTestConfiguration {

            @Bean
            ConnectedUserService connectedUserService() {
                return mock(ConnectedUserService.class);
            }

            @Bean
            McpServerService mcpServerService() {
                return mock(McpServerService.class);
            }

            @Bean
            SigningKeyService signingKeyService() {
                return mock(SigningKeyService.class);
            }

            @Bean
            RouterFunction<ServerResponse> mcpStubRouterFunction() {
                return RouterFunctions.route()
                    .POST("/api/embedded/{secretKey}/mcp", request -> ServerResponse.ok()
                        .build())
                    .build();
            }

            @Bean
            SecurityFilterChain securityFilterChain(
                HttpSecurity http, ConnectedUserService connectedUserService, McpServerService mcpServerService,
                SigningKeyService signingKeyService) throws Exception {

                return http
                    .authorizeHttpRequests(authorize -> authorize.anyRequest()
                        .permitAll())
                    .with(
                        new EmbeddedMcpServerSecurityConfigurer(
                            connectedUserService, mcpServerService, signingKeyService),
                        Customizer.withDefaults())
                    .build();
            }
        }
    }
}

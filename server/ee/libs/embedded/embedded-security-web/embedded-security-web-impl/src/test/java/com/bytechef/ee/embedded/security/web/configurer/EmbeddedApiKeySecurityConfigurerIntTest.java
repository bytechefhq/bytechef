/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.configurer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.execution.facade.ToolFacade;
import com.bytechef.ee.embedded.execution.public_.web.rest.ToolApiController;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.service.ApiKeyService;
import io.jsonwebtoken.Jwts;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringJUnitWebConfig(EmbeddedApiKeySecurityConfigurerIntTest.PathBindingConfiguration.class)
@TestPropertySource(properties = "openapi.openAPIDefinition.base-path.embedded=/api/embedded")
class EmbeddedApiKeySecurityConfigurerIntTest {

    private static final String API_KEY = EncodingUtils.base64EncodeToString("public:secret");

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private SigningKeyService signingKeyService;

    @Autowired
    private ToolFacade toolFacade;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(apiKeyService, connectedUserService, signingKeyService, toolFacade);

        when(apiKeyService.exists(anyString(), anyLong(), eq(PlatformType.EMBEDDED))).thenReturn(true);
        when(connectedUserService.fetchConnectedUser(anyString(), anyLong()))
            .thenAnswer(invocation -> Optional.of(
                new ConnectedUser(Map.of(), null, true, invocation.getArgument(0), 1L, null, 0)));
        when(toolFacade.getTools(anyString(), any(), any(), any(), any())).thenReturn(Map.of());

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
    }

    @Test
    void testAnApiKeyCallWithAnEncodedExternalUserIdReachesTheGuardedEndpoint() throws Exception {
        mockMvc.perform(
            get(URI.create("/api/embedded/v1/user%40example.com/tools"))
                .header("Authorization", "Bearer " + API_KEY))
            .andExpect(status().isOk());
    }

    @Test
    void testAConnectedUserTokenCannotReachAnotherExternalUsersPath() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

        keyPairGenerator.initialize(2048);

        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        when(signingKeyService.getPublicKey(anyString(), anyLong())).thenReturn(keyPair.getPublic());

        String jwtToken = Jwts.builder()
            .header()
            .keyId(EncodingUtils.base64EncodeToString("public:keyId"))
            .and()
            .subject("alice")
            .signWith(keyPair.getPrivate())
            .compact();

        mockMvc.perform(
            get(URI.create("/api/embedded/v1/user%40example.com/tools"))
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(
            get("/api/embedded/v1/alice/tools")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    static class PathBindingConfiguration {

        @Bean
        ApiKeyService apiKeyService() {
            return mock(ApiKeyService.class);
        }

        @Bean
        ConnectedUserService connectedUserService() {
            return mock(ConnectedUserService.class);
        }

        @Bean
        SigningKeyService signingKeyService() {
            return mock(SigningKeyService.class);
        }

        @Bean
        ToolFacade toolFacade() {
            return mock(ToolFacade.class);
        }

        @Bean
        ToolApiController toolApiController(ToolFacade toolFacade) {
            return new ToolApiController(toolFacade, mock(EnvironmentService.class));
        }

        @Bean
        SecurityFilterChain securityFilterChain(
            HttpSecurity httpSecurity, ApiKeyService apiKeyService, ConnectedUserService connectedUserService,
            SigningKeyService signingKeyService)
            throws Exception {

            httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize.anyRequest()
                    .authenticated())
                .with(
                    new EmbeddedApiKeySecurityConfigurer(
                        apiKeyService, connectedUserService, mock(JwtTokenService.class), signingKeyService),
                    Customizer.withDefaults());

            return httpSecurity.build();
        }
    }
}

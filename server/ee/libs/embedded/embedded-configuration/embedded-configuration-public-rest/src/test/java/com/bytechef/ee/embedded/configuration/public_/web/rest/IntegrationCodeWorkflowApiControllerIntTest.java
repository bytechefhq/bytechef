/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.config.Customizer.withDefaults;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationCodeWorkflowFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.EmbeddedConfigurationPublicRestSharedMocks;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.EmbeddedConfigurationPublicRestTestConfiguration;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.ee.embedded.security.web.configurer.EmbeddedApiKeySecurityConfigurer;
import com.bytechef.ee.embedded.security.web.configurer.EmbeddedPlatformUserApiKeySecurityConfigurer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = EmbeddedConfigurationPublicRestTestConfiguration.class)
@TestPropertySource(properties = "bytechef.edition=ee")
@WebMvcTest(IntegrationCodeWorkflowApiController.class)
@EmbeddedConfigurationPublicRestSharedMocks
class IntegrationCodeWorkflowApiControllerIntTest {

    private static final byte[] INTEGRATION_BYTES =
        "({componentName: 'httpClient', workflows: []})".getBytes(StandardCharsets.UTF_8);

    @MockitoBean
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @MockitoBean
    private EnvironmentService environmentService;

    @MockitoBean
    private IntegrationCodeWorkflowFacade integrationCodeWorkflowFacade;

    @Autowired
    private MockMvc mockMvc;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        webTestClient = MockMvcWebTestClient
            .bindTo(mockMvc)
            .build();

        when(environmentService.getEnvironment(nullable(String.class))).thenCallRealMethod();
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testDeployIntegration() {
        deploy("/v1/integration-code-workflows/deploy", null)
            .expectStatus()
            .isNoContent();

        verify(integrationCodeWorkflowFacade).save(
            eq(INTEGRATION_BYTES), eq(Language.JAVASCRIPT), eq(Environment.PRODUCTION), eq(false));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testDeployAcceptsTheEnvironmentHeaderCaseInsensitively() {
        deploy("/v1/integration-code-workflows/deploy", null, "staging")
            .expectStatus()
            .isNoContent();

        verify(integrationCodeWorkflowFacade).save(
            eq(INTEGRATION_BYTES), eq(Language.JAVASCRIPT), eq(Environment.STAGING), eq(false));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testDeployPassesTheEnableFlagThrough() {
        deploy("/v1/integration-code-workflows/deploy?enable=true", null, "STAGING")
            .expectStatus()
            .isNoContent();

        verify(integrationCodeWorkflowFacade).save(
            eq(INTEGRATION_BYTES), eq(Language.JAVASCRIPT), eq(Environment.STAGING), eq(true));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testDeployRejectsAnUnknownEnvironmentHeader() {
        deploy("/v1/integration-code-workflows/deploy", null, "nowhere")
            .expectStatus()
            .isBadRequest();

        verify(integrationCodeWorkflowFacade, never()).save(any(), any(), any(), anyBoolean());
    }

    private WebTestClient.ResponseSpec deploy(String uri, String bearerToken) {
        return deploy(uri, bearerToken, null);
    }

    private WebTestClient.ResponseSpec deploy(String uri, String bearerToken, String environment) {
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();

        multipartBodyBuilder.part("integrationFile", new ByteArrayResource(INTEGRATION_BYTES) {

            @Override
            public String getFilename() {
                return "integration.js";
            }
        });

        WebTestClient.RequestBodySpec requestBodySpec = webTestClient
            .post()
            .uri(uri)
            .contentType(MediaType.MULTIPART_FORM_DATA);

        if (bearerToken != null) {
            requestBodySpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        }

        if (environment != null) {
            requestBodySpec.header("X-Environment", environment);
        }

        return requestBodySpec
            .bodyValue(multipartBodyBuilder.build())
            .exchange();
    }

    @Nested
    @ContextConfiguration(classes = ApiKeyAuthentication.ApiKeySecurityTestConfiguration.class)
    @TestPropertySource(properties = "openapi.openAPIDefinition.base-path.embedded=/api/embedded")
    class ApiKeyAuthentication {

        private static final String DEPLOY_PATH = "/api/embedded/v1/integration-code-workflows/deploy";
        private static final String SECRET_KEY = EncodingUtils.base64EncodeToString("test_tenant:secret");

        @MockitoBean
        private ApiKeyService apiKeyService;

        @MockitoBean
        private AuthorityService authorityService;

        @MockitoBean
        private JwtTokenService jwtTokenService;

        @MockitoBean
        private SigningKeyService signingKeyService;

        @MockitoBean
        private UserService userService;

        @BeforeEach
        void beforeEach() {
            User user = mock(User.class);

            when(user.isActivated()).thenReturn(true);
            when(user.getAuthorityIds()).thenReturn(List.of());
            when(user.getLogin()).thenReturn("admin@localhost.com");
            when(user.getPassword()).thenReturn("password");
            when(userService.fetchUser(7L)).thenReturn(Optional.of(user));
        }

        @Test
        void testTypedApiKeyReachesTheFacade() {
            stubApiKey(PlatformType.EMBEDDED);

            deploy(DEPLOY_PATH, SECRET_KEY)
                .expectStatus()
                .isNoContent();

            verify(integrationCodeWorkflowFacade).save(
                eq(INTEGRATION_BYTES), eq(Language.JAVASCRIPT), any(), eq(false));
        }

        @Test
        void testAdminApiKeyIsRejected() {
            stubApiKey(null);

            deploy(DEPLOY_PATH, SECRET_KEY)
                .expectStatus()
                .isUnauthorized();

            verify(integrationCodeWorkflowFacade, never()).save(any(), any(), any(), anyBoolean());
        }

        private void stubApiKey(PlatformType type) {
            ApiKey apiKey = new ApiKey();

            apiKey.setType(type);
            apiKey.setUserId(7L);

            when(apiKeyService.getApiKey(anyString(), anyLong())).thenReturn(apiKey);
        }

        @EnableWebSecurity
        static class ApiKeySecurityTestConfiguration {

            @Bean
            SecurityFilterChain securityFilterChain(
                HttpSecurity httpSecurity, ApiKeyService apiKeyService, AuthorityService authorityService,
                ConnectedUserService connectedUserService, JwtTokenService jwtTokenService,
                SigningKeyService signingKeyService, UserService userService) throws Exception {

                httpSecurity
                    .authorizeHttpRequests(authorize -> authorize
                        .anyRequest()
                        .authenticated())
                    .with(
                        new EmbeddedApiKeySecurityConfigurer(
                            apiKeyService, connectedUserService, () -> List.of(Environment.values()), jwtTokenService,
                            signingKeyService),
                        withDefaults())
                    .with(
                        new EmbeddedPlatformUserApiKeySecurityConfigurer(
                            apiKeyService, () -> List.of(Environment.values()), authorityService, userService),
                        withDefaults());

                return httpSecurity.build();
            }
        }
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.config.Customizer.withDefaults;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectCodeWorkflowFacade;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@WebMvcTest(AutomationProjectCodeWorkflowApiController.class)
@EmbeddedConfigurationPublicRestSharedMocks
class AutomationProjectCodeWorkflowApiControllerIntTest {

    private static final byte[] PROJECT_BYTES = "({name: 'p', workflows: []})".getBytes(StandardCharsets.UTF_8);

    @MockitoBean
    private AutomationWorkflowProjectCodeWorkflowFacade automationWorkflowProjectCodeWorkflowFacade;

    @MockitoBean
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @MockitoBean
    private EnvironmentService environmentService;

    @Autowired
    private MockMvc mockMvc;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        webTestClient = MockMvcWebTestClient
            .bindTo(mockMvc)
            .build();
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testDeployReturnsWarnings() {
        when(automationWorkflowProjectCodeWorkflowFacade.save(eq(PROJECT_BYTES), eq(Language.JAVASCRIPT), any(),
            anyBoolean()))
                .thenReturn(List.of("Workflow 'internal-only' will not be invocable"));

        deploy(webTestClient, "/v1/automation-project-code-workflows/deploy", null)
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.warnings[0]")
            .isEqualTo("Workflow 'internal-only' will not be invocable");

        verify(automationWorkflowProjectCodeWorkflowFacade).save(eq(PROJECT_BYTES), eq(Language.JAVASCRIPT), any(),
            anyBoolean());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testDeployAcceptsTheEnvironmentHeaderCaseInsensitively() {
        when(automationWorkflowProjectCodeWorkflowFacade.save(eq(PROJECT_BYTES), eq(Language.JAVASCRIPT), any(),
            anyBoolean()))
                .thenReturn(List.of());

        deploy(webTestClient, "/v1/automation-project-code-workflows/deploy", null, "staging")
            .expectStatus()
            .isOk();

        verify(automationWorkflowProjectCodeWorkflowFacade).save(eq(PROJECT_BYTES), eq(Language.JAVASCRIPT), any(),
            anyBoolean());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testDeployPassesTheReleaseFlagAndTheRequestEnvironmentToTheFacade() {
        when(environmentService.getEnvironment("STAGING")).thenReturn(Environment.STAGING);
        when(automationWorkflowProjectCodeWorkflowFacade.save(any(), any(), any(), anyBoolean()))
            .thenReturn(List.of());

        deploy(webTestClient, "/v1/automation-project-code-workflows/deploy", null, "staging", "true")
            .expectStatus()
            .isOk();

        verify(automationWorkflowProjectCodeWorkflowFacade)
            .save(eq(PROJECT_BYTES), eq(Language.JAVASCRIPT), eq(Environment.STAGING), eq(true));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testDeployWithoutTheReleaseFieldPassesFalseToTheFacade() {
        when(environmentService.getEnvironment("STAGING")).thenReturn(Environment.STAGING);
        when(automationWorkflowProjectCodeWorkflowFacade.save(any(), any(), any(), anyBoolean()))
            .thenReturn(List.of());

        deploy(webTestClient, "/v1/automation-project-code-workflows/deploy", null, "staging")
            .expectStatus()
            .isOk();

        verify(automationWorkflowProjectCodeWorkflowFacade)
            .save(eq(PROJECT_BYTES), eq(Language.JAVASCRIPT), eq(Environment.STAGING), eq(false));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testDeployRejectsAnUnknownEnvironmentHeader() {
        deploy(webTestClient, "/v1/automation-project-code-workflows/deploy", null, "nowhere")
            .expectStatus()
            .isBadRequest();

        verify(automationWorkflowProjectCodeWorkflowFacade, never()).save(any(), any(), any(), anyBoolean());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testListMapsProjectsWithoutProjectIds() {
        ConnectedUserWorkflowTemplateDTO workflowTemplate = new ConnectedUserWorkflowTemplateDTO(
            "workflow-uuid", "Greet on Request", null, null, List.of(), List.of(), List.of(), null, null);

        when(automationWorkflowProjectFacade.getPublishedProjects())
            .thenReturn(List.of(
                new AutomationWorkflowProjectDTO(
                    42L, "Sample Catalog Greeting", null, null, List.of(), true, 1, 1, List.of(workflowTemplate), null,
                    true, true, 0, null)));

        webTestClient
            .get()
            .uri("/v1/automation-project-code-workflows")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$[0].name")
            .isEqualTo("Sample Catalog Greeting")
            .jsonPath("$[0].kind")
            .isEqualTo("REFERENCE")
            .jsonPath("$[0].workflowTemplates[0].label")
            .isEqualTo("Greet on Request")
            .jsonPath("$[0].id")
            .doesNotExist();
    }

    static WebTestClient.ResponseSpec deploy(WebTestClient webTestClient, String uri, String bearerToken) {
        return deploy(webTestClient, uri, bearerToken, null);
    }

    static WebTestClient.ResponseSpec deploy(
        WebTestClient webTestClient, String uri, String bearerToken, String environment) {

        return deploy(webTestClient, uri, bearerToken, environment, null);
    }

    static WebTestClient.ResponseSpec deploy(
        WebTestClient webTestClient, String uri, String bearerToken, String environment, String release) {

        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();

        multipartBodyBuilder.part("projectFile", new ByteArrayResource(PROJECT_BYTES) {

            @Override
            public String getFilename() {
                return "project.js";
            }
        });

        if (release != null) {
            multipartBodyBuilder.part("release", release);
        }

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
    @ContextConfiguration(classes = MethodSecurity.MethodSecurityTestConfiguration.class)
    class MethodSecurity {

        @Test
        @WithMockUser(authorities = "ROLE_USER")
        void testListIsForbiddenForNonAdmin() {
            assertThatThrownBy(() -> webTestClient
                .get()
                .uri("/v1/automation-project-code-workflows")
                .exchange())
                    .hasRootCauseInstanceOf(AccessDeniedException.class);

            verify(automationWorkflowProjectFacade, never()).getPublishedProjects();
        }

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        void testListIsAllowedForAdmin() {
            when(automationWorkflowProjectFacade.getPublishedProjects()).thenReturn(List.of());

            webTestClient
                .get()
                .uri("/v1/automation-project-code-workflows")
                .exchange()
                .expectStatus()
                .isOk();
        }

        @EnableMethodSecurity(proxyTargetClass = true)
        static class MethodSecurityTestConfiguration {
        }
    }

    @Nested
    @ContextConfiguration(classes = ApiKeyAuthentication.ApiKeySecurityTestConfiguration.class)
    @TestPropertySource(properties = "openapi.openAPIDefinition.base-path.embedded=/api/embedded")
    class ApiKeyAuthentication {

        private static final String DEPLOY_PATH = "/api/embedded/v1/automation-project-code-workflows/deploy";
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

            when(automationWorkflowProjectCodeWorkflowFacade.save(any(), any(), any(), anyBoolean()))
                .thenReturn(List.of());

            deploy(webTestClient, DEPLOY_PATH, SECRET_KEY)
                .expectStatus()
                .isOk();

            verify(automationWorkflowProjectCodeWorkflowFacade).save(eq(PROJECT_BYTES), eq(Language.JAVASCRIPT), any(),
                anyBoolean());
        }

        @Test
        void testAdminApiKeyIsRejected() {
            stubApiKey(null);

            deploy(webTestClient, DEPLOY_PATH, SECRET_KEY)
                .expectStatus()
                .isUnauthorized();

            verify(automationWorkflowProjectCodeWorkflowFacade, never()).save(any(), any(), any(), anyBoolean());
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

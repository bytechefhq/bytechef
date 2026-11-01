/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.configurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.authentication.CredentialAuthenticationSuccessEvent;
import com.bytechef.platform.security.web.authentication.UnresolvedCredentialAuthenticationToken;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.domain.TenantKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
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
 * @version ee
 * @author Ivica Cardic
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PlatformApiKeySecurityConfigurerIntTest.SecurityFilterChainTestConfiguration.class)
@WebAppConfiguration
class PlatformApiKeySecurityConfigurerIntTest {

    private static final String API_SECRET_KEY = String.valueOf(TenantKey.of());
    private static final String PLATFORM_PATH = "/api/platform/v1/workspaces";

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private AuthenticationEventRecorder authenticationEventRecorder;

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private UserService userService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void beforeEach() {
        reset(apiKeyService, authorityService, userService);

        authenticationEventRecorder.clear();

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
    }

    @AfterEach
    void afterEach() {
        assertThat(authenticationEventRecorder.getStoredFailureTexts())
            .noneMatch(storedFailureText -> storedFailureText.contains(API_SECRET_KEY))
            .noneMatch(storedFailureText -> storedFailureText.contains("not-a-tenant-key"));
    }

    @Test
    void testAdminApiKeyIsAccepted() throws Exception {
        mockApiKey(null);

        mockMvc
            .perform(
                MockMvcRequestBuilders.get(PLATFORM_PATH)
                    .servletPath(PLATFORM_PATH)
                    .header("Authorization", "Bearer " + API_SECRET_KEY))
            .andExpect(MockMvcResultMatchers.status()
                .isOk());
    }

    @Test
    void testAutomationApiKeyIsRejected() throws Exception {
        mockApiKey(PlatformType.AUTOMATION);

        mockMvc
            .perform(
                MockMvcRequestBuilders.get(PLATFORM_PATH)
                    .servletPath(PLATFORM_PATH)
                    .header("Authorization", "Bearer " + API_SECRET_KEY))
            .andExpect(MockMvcResultMatchers.status()
                .isUnauthorized());
    }

    @Test
    void testEmbeddedApiKeyIsRejected() throws Exception {
        mockApiKey(PlatformType.EMBEDDED);

        mockMvc
            .perform(
                MockMvcRequestBuilders.get(PLATFORM_PATH)
                    .servletPath(PLATFORM_PATH)
                    .header("Authorization", "Bearer " + API_SECRET_KEY))
            .andExpect(MockMvcResultMatchers.status()
                .isUnauthorized());
    }

    @Test
    void testRequestWithoutApiKeyIsRejected() throws Exception {
        mockMvc
            .perform(
                MockMvcRequestBuilders.get(PLATFORM_PATH)
                    .servletPath(PLATFORM_PATH))
            .andExpect(MockMvcResultMatchers.status()
                .isUnauthorized());
    }

    @Test
    void testAdminApiKeyPublishesOneCredentialSuccessAndNoSpringSuccess() throws Exception {
        mockApiKey(null);

        mockMvc
            .perform(
                MockMvcRequestBuilders.get(PLATFORM_PATH)
                    .servletPath(PLATFORM_PATH)
                    .header("Authorization", "Bearer " + API_SECRET_KEY))
            .andExpect(MockMvcResultMatchers.status()
                .isOk());

        assertThat(authenticationEventRecorder.getPrincipals(CredentialAuthenticationSuccessEvent.class))
            .containsExactly("admin@localhost.com");
        assertThat(authenticationEventRecorder.getPrincipals(AuthenticationSuccessEvent.class)).isEmpty();
        assertThat(authenticationEventRecorder.getPrincipals(AbstractAuthenticationFailureEvent.class)).isEmpty();
    }

    @Test
    void testAutomationApiKeyPublishesAFailureForTheUnresolvedCredential() throws Exception {
        mockApiKey(PlatformType.AUTOMATION);

        mockMvc
            .perform(
                MockMvcRequestBuilders.get(PLATFORM_PATH)
                    .servletPath(PLATFORM_PATH)
                    .header("Authorization", "Bearer " + API_SECRET_KEY))
            .andExpect(MockMvcResultMatchers.status()
                .isUnauthorized());

        assertThat(authenticationEventRecorder.getPrincipals(AbstractAuthenticationFailureEvent.class))
            .containsExactly(UnresolvedCredentialAuthenticationToken.PRINCIPAL);
        assertThat(authenticationEventRecorder.getPrincipals(CredentialAuthenticationSuccessEvent.class)).isEmpty();
    }

    @Test
    void testUnparseableApiKeyPublishesAFailureForTheUnresolvedCredential() throws Exception {
        mockMvc
            .perform(
                MockMvcRequestBuilders.get(PLATFORM_PATH)
                    .servletPath(PLATFORM_PATH)
                    .header("Authorization", "Bearer not-a-tenant-key"))
            .andExpect(MockMvcResultMatchers.status()
                .isUnauthorized());

        assertThat(authenticationEventRecorder.getPrincipals(AbstractAuthenticationFailureEvent.class))
            .containsExactly(UnresolvedCredentialAuthenticationToken.PRINCIPAL);
        assertThat(authenticationEventRecorder.getPrincipals(CredentialAuthenticationSuccessEvent.class)).isEmpty();
    }

    private void mockApiKey(PlatformType type) {
        ApiKey apiKey = mock(ApiKey.class);

        when(apiKey.getType()).thenReturn(type);
        when(apiKey.getUserId()).thenReturn(100L);
        when(apiKeyService.fetchApiKey(anyString())).thenReturn(Optional.of(apiKey));

        User user = mock(User.class);

        when(user.isActivated()).thenReturn(true);
        when(user.getLogin()).thenReturn("admin@localhost.com");
        when(user.getPassword()).thenReturn("password");
        when(user.getAuthorityIds()).thenReturn(List.of(1L));
        when(userService.fetchUser(100L)).thenReturn(Optional.of(user));

        Authority authority = mock(Authority.class);

        when(authority.getName()).thenReturn("ROLE_ADMIN");
        when(authorityService.fetchAuthority(1L)).thenReturn(Optional.of(authority));
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    static class SecurityFilterChainTestConfiguration {

        @Bean
        AuthenticationEventPublisher authenticationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
            return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
        }

        @Bean
        AuthenticationEventRecorder authenticationEventRecorder() {
            return new AuthenticationEventRecorder();
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
        UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        RouterFunction<ServerResponse> platformStubRouterFunction() {
            return RouterFunctions.route()
                .GET(PLATFORM_PATH, request -> ServerResponse.ok()
                    .build())
                .build();
        }

        @Bean
        SecurityFilterChain securityFilterChain(
            HttpSecurity http, ApiKeyService apiKeyService, AuthorityService authorityService,
            UserService userService) throws Exception {

            return http
                .authorizeHttpRequests(authorize -> authorize.anyRequest()
                    .hasAuthority("ROLE_ADMIN"))
                .with(
                    new PlatformApiKeySecurityConfigurer(
                        apiKeyService, () -> List.of(Environment.values()), authorityService, userService),
                    Customizer.withDefaults())
                .build();
        }
    }

    static class AuthenticationEventRecorder implements ApplicationListener<ApplicationEvent> {

        private final List<ApplicationEvent> applicationEvents = new CopyOnWriteArrayList<>();

        @Override
        public void onApplicationEvent(ApplicationEvent applicationEvent) {
            if (applicationEvent instanceof AbstractAuthenticationEvent ||
                applicationEvent instanceof CredentialAuthenticationSuccessEvent) {

                applicationEvents.add(applicationEvent);
            }
        }

        void clear() {
            applicationEvents.clear();
        }

        List<String> getStoredFailureTexts() {
            List<String> storedFailureTexts = new ArrayList<>();

            for (ApplicationEvent applicationEvent : applicationEvents) {
                if (!(applicationEvent instanceof AbstractAuthenticationFailureEvent failureEvent)) {
                    continue;
                }

                Authentication authentication = failureEvent.getAuthentication();

                storedFailureTexts.add(authentication.getName());
                storedFailureTexts.add(String.valueOf(authentication.getDetails()));

                for (Throwable throwable = failureEvent.getException(); throwable != null; throwable =
                    throwable.getCause()) {

                    storedFailureTexts.add(throwable.getClass()
                        .getName());
                    storedFailureTexts.add(String.valueOf(throwable.getMessage()));
                }
            }

            return storedFailureTexts;
        }

        List<String> getPrincipals(Class<? extends ApplicationEvent> eventType) {
            return applicationEvents.stream()
                .filter(eventType::isInstance)
                .map(AuthenticationEventRecorder::getAuthentication)
                .map(Authentication::getName)
                .toList();
        }

        private static Authentication getAuthentication(ApplicationEvent applicationEvent) {
            if (applicationEvent instanceof AbstractAuthenticationEvent abstractAuthenticationEvent) {
                return abstractAuthenticationEvent.getAuthentication();
            }

            CredentialAuthenticationSuccessEvent credentialAuthenticationSuccessEvent =
                (CredentialAuthenticationSuccessEvent) applicationEvent;

            return credentialAuthenticationSuccessEvent.getAuthentication();
        }
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.security.web.app;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.security.AppPrincipal;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.security.web.configurer.AutomationApiKeySecurityConfigurer;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.domain.TenantKey;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Runs both API key configurers in one real security filter chain, with CSRF enabled as in production, and proves that
 * an app key and a user API key are each accepted only on their own endpoints.
 *
 * @version ee
 */
@SpringJUnitWebConfig(AppApiKeyFilterChainTest.FilterChainTestConfiguration.class)
class AppApiKeyFilterChainTest {

    private static final String APP_ERROR_HEADER = "X-ByteChef-App-Error";
    private static final String UNAUTHORIZED_CODE = "\"code\":\"UNAUTHORIZED\"";

    private static final String APP_API_PATH = "/api/app/v1/api/v1/crm/contacts";
    private static final String APP_PATH = "/api/app/v1/ping";
    private static final String APP_WORKFLOW_RUN_PATH = "/api/app/v1/workflow-runs/run-1";
    private static final String OTHER_PATH = "/api/other/v1/ping";
    private static final String AUTOMATION_PATH = "/api/automation/v1/ping";

    private final String appKey = TenantKey.of()
        .toString();
    private final String disabledAppKey = TenantKey.of()
        .toString();
    private final String userKey = TenantKey.of()
        .toString();

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private AppService appService;

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private UserService userService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void beforeEach() {
        reset(apiKeyService, appService, authorityService, userService);

        when(appService.fetchAppBySecretKey(anyString())).thenReturn(Optional.empty());
        when(appService.fetchAppBySecretKey(appKey)).thenReturn(Optional.of(app(1L, AppStatus.ENABLED)));
        when(appService.fetchAppBySecretKey(disabledAppKey)).thenReturn(Optional.of(app(2L, AppStatus.DISABLED)));

        when(apiKeyService.fetchApiKey(anyString(), anyLong())).thenAnswer(invocation -> {
            if (!userKey.equals(invocation.getArgument(0))) {
                return Optional.empty();
            }

            ApiKey apiKey = new ApiKey();

            apiKey.setSecretKey(userKey);
            apiKey.setType(PlatformType.AUTOMATION);
            apiKey.setUserId(5L);

            return Optional.of(apiKey);
        });

        User user = new User();

        user.setActivated(true);
        user.setAuthorityIds(List.of(1L));
        user.setId(5L);
        user.setLogin("user");
        user.setPassword(String.valueOf(UUID.randomUUID()));

        Authority authority = new Authority();

        authority.setId(1L);
        authority.setName("ROLE_USER");

        when(userService.fetchUser(5L)).thenReturn(Optional.of(user));
        when(authorityService.fetchAuthority(1L)).thenReturn(Optional.of(authority));

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
    }

    @Test
    void testAppKeyIsAcceptedOnTheAppApiWithoutACsrfToken() throws Exception {
        mockMvc.perform(post(APP_PATH).header("Authorization", "Bearer " + appKey))
            .andExpect(status().isOk())
            .andExpect(content().string(AppPrincipal.class.getSimpleName()));
    }

    @Test
    void testAppKeyIsAcceptedOnTheWorkflowRunPath() throws Exception {
        mockMvc.perform(get(APP_WORKFLOW_RUN_PATH).header("Authorization", "Bearer " + appKey))
            .andExpect(status().isOk())
            .andExpect(content().string(AppPrincipal.class.getSimpleName()));
    }

    @Test
    void testCsrfFailureOnANonAppPathIsNotMarked() throws Exception {
        mockMvc.perform(post(OTHER_PATH))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist(APP_ERROR_HEADER));
    }

    @Test
    void testUserKeyIsAcceptedOnTheAutomationApi() throws Exception {
        mockMvc.perform(post(AUTOMATION_PATH).header("Authorization", "Bearer " + userKey))
            .andExpect(status().isOk());
    }

    @Test
    void testUserKeyIsRejectedOnTheAppApi() throws Exception {
        mockMvc.perform(get(APP_PATH).header("Authorization", "Bearer " + userKey))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(APP_ERROR_HEADER, "true"))
            .andExpect(content().string(containsString(UNAUTHORIZED_CODE)));
        mockMvc.perform(post(APP_PATH).header("Authorization", "Bearer " + userKey))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(APP_ERROR_HEADER, "true"));
    }

    @Test
    void testUserKeyIsRejectedOnTheWorkflowRunPath() throws Exception {
        mockMvc.perform(get(APP_WORKFLOW_RUN_PATH).header("Authorization", "Bearer " + userKey))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testAppKeyIsRejectedOnTheAutomationApi() throws Exception {
        mockMvc.perform(get(AUTOMATION_PATH).header("Authorization", "Bearer " + appKey))
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist(APP_ERROR_HEADER));
        mockMvc.perform(post(AUTOMATION_PATH).header("Authorization", "Bearer " + appKey))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testDisabledAppIsRejected() throws Exception {
        mockMvc.perform(post(APP_PATH).header("Authorization", "Bearer " + disabledAppKey))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(APP_ERROR_HEADER, "true"))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().string("{\"code\":\"APP_DISABLED\",\"message\":\"App is disabled\"}"));
    }

    @Test
    void testUnknownOrRotatedKeyIsRejected() throws Exception {
        String rotatedAwayKey = TenantKey.of()
            .toString();

        mockMvc.perform(post(APP_PATH).header("Authorization", "Bearer " + rotatedAwayKey))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(APP_ERROR_HEADER, "true"))
            .andExpect(content().string("{\"code\":\"UNAUTHORIZED\",\"message\":\"Unknown app key\"}"));
    }

    @Test
    void testDisabledAppOnApiPathIsRejected() throws Exception {
        mockMvc.perform(post(APP_API_PATH).header("Authorization", "Bearer " + appKey))
            .andExpect(status().isOk())
            .andExpect(content().string(AppPrincipal.class.getSimpleName()));

        when(appService.fetchAppBySecretKey(appKey)).thenReturn(Optional.of(app(1L, AppStatus.DISABLED)));

        mockMvc.perform(post(APP_API_PATH).header("Authorization", "Bearer " + appKey))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(APP_ERROR_HEADER, "true"))
            .andExpect(content().string(containsString("\"code\":\"APP_DISABLED\"")));
        mockMvc.perform(get(APP_API_PATH).header("Authorization", "Bearer " + disabledAppKey))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(APP_ERROR_HEADER, "true"));
    }

    @Test
    void testRotatedKeyOnApiPathIsRejected() throws Exception {
        mockMvc.perform(get(APP_API_PATH).header("Authorization", "Bearer " + appKey))
            .andExpect(status().isOk());

        when(appService.fetchAppBySecretKey(appKey)).thenReturn(Optional.empty());

        mockMvc.perform(get(APP_API_PATH).header("Authorization", "Bearer " + appKey))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testMalformedOrMissingKeyIsRejected() throws Exception {
        mockMvc.perform(post(APP_PATH).header("Authorization", "Bearer not-a-key"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(APP_ERROR_HEADER, "true"))
            .andExpect(content().string(containsString(UNAUTHORIZED_CODE)));
        mockMvc.perform(post(APP_PATH))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(APP_ERROR_HEADER, "true"))
            .andExpect(content().string(containsString(UNAUTHORIZED_CODE)));
    }

    private static App app(long id, AppStatus status) {
        App app = new App();

        app.setId(id);
        app.setWorkspaceId(7L);
        app.setEnvironment(Environment.PRODUCTION);
        app.setStatus(status);

        return app;
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    static class FilterChainTestConfiguration {

        @Bean
        ApiKeyService apiKeyService() {
            return mock(ApiKeyService.class);
        }

        @Bean
        AppService appService() {
            return mock(AppService.class);
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
        PingController pingController() {
            return new PingController();
        }

        @Bean
        SecurityFilterChain securityFilterChain(
            HttpSecurity http, ApiKeyService apiKeyService, AppService appService, AuthorityService authorityService,
            UserService userService) throws Exception {

            http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .authorizeHttpRequests(authorize -> authorize
                    .anyRequest()
                    .authenticated())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

            http.with(
                new AutomationApiKeySecurityConfigurer(
                    apiKeyService, () -> List.of(Environment.values()), authorityService, userService),
                withDefaults());
            http.with(new AppApiKeySecurityConfigurer(appService, mock(EnvironmentService.class)), withDefaults());

            return http.build();
        }
    }

    @RestController
    static class PingController {

        @GetMapping({
            APP_API_PATH, APP_PATH, APP_WORKFLOW_RUN_PATH, AUTOMATION_PATH
        })
        String getPing(Authentication authentication) {
            return principalType(authentication);
        }

        @PostMapping({
            APP_API_PATH, APP_PATH, AUTOMATION_PATH
        })
        String postPing(Authentication authentication) {
            return principalType(authentication);
        }

        private static String principalType(Authentication authentication) {
            Object principal = Objects.requireNonNull(authentication.getPrincipal());

            return principal.getClass()
                .getSimpleName();
        }
    }
}

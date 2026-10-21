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

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.security.AppPrincipal;
import com.bytechef.ee.automation.app.security.AppUser;
import com.bytechef.ee.automation.app.security.AppUserTokenVerifier;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.security.web.configurer.AutomationApiKeySecurityConfigurer;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.mcp.SelfIssuedTokenRevocationChecker;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
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
    private static final String CLIENT_ID = "app-client";
    private static final String CONTEXT_PATH = "/api/app/v1/context";
    private static final String ISSUER = "https://bytechef.example.com";
    private static final String MEMBER_LOGIN = "member";
    private static final RSAKey SIGNING_KEY = generateSigningKey();
    private static final String USER_PATH = "/api/app/v1/user";
    private static final String USER_TOKEN_HEADER = "X-ByteChef-User-Token";
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
    private AppInstanceService appInstanceService;

    @Autowired
    private AppService appService;

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private SelfIssuedTokenRevocationChecker selfIssuedTokenRevocationChecker;

    @Autowired
    private UserService userService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void beforeEach() {
        reset(
            apiKeyService, appInstanceService, appService, authorityService, permissionService,
            selfIssuedTokenRevocationChecker, userService);

        when(appInstanceService.fetchAppInstanceBySecretKey(anyString())).thenReturn(Optional.empty());
        when(appInstanceService.fetchAppInstanceBySecretKey(appKey)).thenReturn(Optional.of(appInstance(11L, 1L)));
        when(appInstanceService.fetchAppInstanceBySecretKey(disabledAppKey))
            .thenReturn(Optional.of(appInstance(12L, 2L)));
        when(appService.fetchApp(1L)).thenReturn(Optional.of(app(1L, AppStatus.ACTIVE)));
        when(appService.fetchApp(2L)).thenReturn(Optional.of(app(2L, AppStatus.SUSPENDED)));

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

        User member = new User();

        member.setActivated(true);
        member.setAuthorityIds(List.of(1L));
        member.setId(6L);
        member.setLogin(MEMBER_LOGIN);

        when(userService.fetchUserByLogin(MEMBER_LOGIN)).thenReturn(Optional.of(member));
        when(permissionService.hasResourceScope(11L, "AppInstance", "APP_USE")).thenReturn(true);

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
    void testUserTokenWithoutKeyIs401() throws Exception {
        mockMvc.perform(get(USER_PATH).header(USER_TOKEN_HEADER, userToken(CLIENT_ID)))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(APP_ERROR_HEADER, "true"))
            .andExpect(content().string(containsString(UNAUTHORIZED_CODE)));
    }

    @Test
    void testValidUserTokenPutsTheUserOnThePrincipal() throws Exception {
        withInstance(AppUserAuthMode.OPTIONAL, CLIENT_ID);

        mockMvc.perform(
            get(USER_PATH).header("Authorization", "Bearer " + appKey)
                .header(USER_TOKEN_HEADER, userToken(CLIENT_ID)))
            .andExpect(status().isOk())
            .andExpect(content().string("6:" + MEMBER_LOGIN));
    }

    @Test
    void testOptionalModeWithoutTokenHasNoUser() throws Exception {
        withInstance(AppUserAuthMode.OPTIONAL, CLIENT_ID);

        mockMvc.perform(get(USER_PATH).header("Authorization", "Bearer " + appKey))
            .andExpect(status().isOk())
            .andExpect(content().string("none"));
    }

    @Test
    void testInvalidTokenInOptionalModeIs401NotAnonymous() throws Exception {
        withInstance(AppUserAuthMode.OPTIONAL, CLIENT_ID);

        mockMvc.perform(
            get(USER_PATH).header("Authorization", "Bearer " + appKey)
                .header(USER_TOKEN_HEADER, "not-a-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(APP_ERROR_HEADER, "true"))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(
                content().string("{\"code\":\"APP_USER_TOKEN_INVALID\",\"message\":\"The user token is invalid\"}"));
    }

    @Test
    void testInvalidTokenInNoneModeIs401NotAnonymous() throws Exception {
        withInstance(AppUserAuthMode.NONE, null);

        mockMvc.perform(
            get(USER_PATH).header("Authorization", "Bearer " + appKey)
                .header(USER_TOKEN_HEADER, "not-a-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(containsString("\"code\":\"APP_USER_TOKEN_INVALID\"")));
    }

    @Test
    void testValidTokenOnAnInstanceWithoutClientIs401() throws Exception {
        withInstance(AppUserAuthMode.NONE, null);

        mockMvc.perform(
            get(USER_PATH).header("Authorization", "Bearer " + appKey)
                .header(USER_TOKEN_HEADER, userToken(CLIENT_ID)))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(containsString("\"code\":\"APP_USER_TOKEN_INVALID\"")));
    }

    @Test
    void testRequiredModeWithoutTokenIs401() throws Exception {
        withInstance(AppUserAuthMode.REQUIRED, CLIENT_ID);

        mockMvc.perform(get(USER_PATH).header("Authorization", "Bearer " + appKey))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(APP_ERROR_HEADER, "true"))
            .andExpect(
                content().string("{\"code\":\"APP_USER_REQUIRED\",\"message\":\"A signed-in user is required\"}"));
        mockMvc.perform(post(APP_PATH).header("Authorization", "Bearer " + appKey))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(containsString("\"code\":\"APP_USER_REQUIRED\"")));
    }

    @Test
    void testRequiredInstanceWithoutUrlAnswersUserRequiredAndServesItsContext() throws Exception {
        AppInstance appInstance = withInstance(AppUserAuthMode.REQUIRED, null);

        appInstance.setUrl(null);

        mockMvc.perform(get(USER_PATH).header("Authorization", "Bearer " + appKey))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(containsString("\"code\":\"APP_USER_REQUIRED\"")));
        mockMvc.perform(get(CONTEXT_PATH).header("Authorization", "Bearer " + appKey))
            .andExpect(status().isOk())
            .andExpect(content().string("none"));
    }

    @Test
    void testInvalidTokenOnContextIs401() throws Exception {
        withInstance(AppUserAuthMode.REQUIRED, CLIENT_ID);

        mockMvc.perform(
            get(CONTEXT_PATH).header("Authorization", "Bearer " + appKey)
                .header(USER_TOKEN_HEADER, "not-a-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(containsString("\"code\":\"APP_USER_TOKEN_INVALID\"")));
    }

    @Test
    void testUserWithoutAppUseIs403() throws Exception {
        withInstance(AppUserAuthMode.REQUIRED, CLIENT_ID);

        when(permissionService.hasResourceScope(11L, "AppInstance", "APP_USE")).thenReturn(false);

        mockMvc.perform(
            get(USER_PATH).header("Authorization", "Bearer " + appKey)
                .header(USER_TOKEN_HEADER, userToken(CLIENT_ID)))
            .andExpect(status().isForbidden())
            .andExpect(header().string(APP_ERROR_HEADER, "true"))
            .andExpect(
                content().string(
                    "{\"code\":\"APP_USER_FORBIDDEN\",\"message\":\"The signed-in user may not use this app instance\"}"));
    }

    @Test
    void testTokenOfAnotherClientIs401() throws Exception {
        withInstance(AppUserAuthMode.REQUIRED, CLIENT_ID);

        mockMvc.perform(
            get(USER_PATH).header("Authorization", "Bearer " + appKey)
                .header(USER_TOKEN_HEADER, userToken("app-other")))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(containsString("\"code\":\"APP_USER_TOKEN_INVALID\"")));
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
    void testSuspendedAppRejectsEveryInstanceKey() throws Exception {
        String secondInstanceKey = TenantKey.of()
            .toString();

        when(appInstanceService.fetchAppInstanceBySecretKey(secondInstanceKey))
            .thenReturn(Optional.of(appInstance(13L, 1L)));
        when(appService.fetchApp(1L)).thenReturn(Optional.of(app(1L, AppStatus.SUSPENDED)));

        mockMvc.perform(post(APP_PATH).header("Authorization", "Bearer " + appKey))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(containsString("\"code\":\"APP_DISABLED\"")));
        mockMvc.perform(post(APP_PATH).header("Authorization", "Bearer " + secondInstanceKey))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(containsString("\"code\":\"APP_DISABLED\"")));
    }

    @Test
    void testSuspendedInstanceRejectsOnlyItsOwnKey() throws Exception {
        String siblingInstanceKey = TenantKey.of()
            .toString();
        AppInstance suspendedInstance = appInstance(11L, 1L);

        suspendedInstance.setStatus(AppInstanceStatus.SUSPENDED);

        when(appInstanceService.fetchAppInstanceBySecretKey(appKey)).thenReturn(Optional.of(suspendedInstance));
        when(appInstanceService.fetchAppInstanceBySecretKey(siblingInstanceKey))
            .thenReturn(Optional.of(appInstance(13L, 1L)));

        mockMvc.perform(post(APP_PATH).header("Authorization", "Bearer " + appKey))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(containsString("\"code\":\"APP_DISABLED\"")));
        mockMvc.perform(post(APP_PATH).header("Authorization", "Bearer " + siblingInstanceKey))
            .andExpect(status().isOk())
            .andExpect(content().string(AppPrincipal.class.getSimpleName()));
    }

    @Test
    void testTerminatedInstanceKeyIsRejected() throws Exception {
        AppInstance terminatedInstance = appInstance(11L, 1L);

        terminatedInstance.setStatus(AppInstanceStatus.TERMINATED);

        when(appInstanceService.fetchAppInstanceBySecretKey(appKey)).thenReturn(Optional.of(terminatedInstance));

        mockMvc.perform(post(APP_PATH).header("Authorization", "Bearer " + appKey))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(APP_ERROR_HEADER, "true"))
            .andExpect(content().string(containsString("\"code\":\"APP_DISABLED\"")));
    }

    @Test
    void testDraftAppKeyAuthenticates() throws Exception {
        when(appService.fetchApp(1L)).thenReturn(Optional.of(app(1L, AppStatus.DRAFT)));

        mockMvc.perform(post(APP_PATH).header("Authorization", "Bearer " + appKey))
            .andExpect(status().isOk())
            .andExpect(content().string(AppPrincipal.class.getSimpleName()));
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

        when(appService.fetchApp(1L)).thenReturn(Optional.of(app(1L, AppStatus.SUSPENDED)));

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

        when(appInstanceService.fetchAppInstanceBySecretKey(appKey)).thenReturn(Optional.empty());

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

    private AppInstance withInstance(AppUserAuthMode userAuthMode, String registeredClientId) {
        AppInstance appInstance = appInstance(11L, 1L);

        appInstance.setRegisteredClientId(registeredClientId);
        appInstance.setUserAuthMode(userAuthMode);

        when(appInstanceService.fetchAppInstanceBySecretKey(appKey)).thenReturn(Optional.of(appInstance));

        return appInstance;
    }

    private static String userToken(String audience) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(ISSUER)
            .subject(MEMBER_LOGIN)
            .audience(List.of(audience))
            .issuedAt(now)
            .expiresAt(now.plusSeconds(300))
            .claim("scope", List.of("openid", "profile", "app:use"))
            .claim("tenant_id", TenantContext.DEFAULT_TENANT_ID)
            .build();

        NimbusJwtEncoder jwtEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(SIGNING_KEY)));

        return jwtEncoder.encode(
            JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256)
                    .build(),
                claims))
            .getTokenValue();
    }

    private static RSAKey generateSigningKey() {
        try {
            return new RSAKeyGenerator(2048).keyID("signing")
                .generate();
        } catch (JOSEException joseException) {
            throw new IllegalStateException(joseException);
        }
    }

    private static App app(long id, AppStatus status) {
        App app = new App();

        app.setId(id);
        app.setWorkspaceId(7L);
        app.setStatus(status);

        return app;
    }

    private static AppInstance appInstance(long id, long appId) {
        AppInstance appInstance = new AppInstance();

        appInstance.setId(id);
        appInstance.setAppId(appId);
        appInstance.setWorkspaceId(7L);
        appInstance.setEnvironment(Environment.PRODUCTION);
        appInstance.setStatus(AppInstanceStatus.ACTIVE);

        return appInstance;
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
        AppInstanceService appInstanceService() {
            return mock(AppInstanceService.class);
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
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }

        @Bean
        SelfIssuedTokenRevocationChecker selfIssuedTokenRevocationChecker() {
            return mock(SelfIssuedTokenRevocationChecker.class);
        }

        @Bean
        AppUserTokenVerifier appUserTokenVerifier(
            AuthorityService authorityService, PermissionService permissionService,
            SelfIssuedTokenRevocationChecker selfIssuedTokenRevocationChecker, UserService userService)
            throws JOSEException {

            ApplicationProperties applicationProperties = new ApplicationProperties();

            applicationProperties.setPublicUrl(ISSUER);

            JwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(SIGNING_KEY.toRSAPublicKey())
                .build();

            return new AppUserTokenVerifier(
                applicationProperties, authorityService, objectProvider(jwtDecoder), permissionService,
                objectProvider(selfIssuedTokenRevocationChecker), userService, objectProvider(null));
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
            HttpSecurity http, ApiKeyService apiKeyService, AppInstanceService appInstanceService,
            AppService appService, AppUserTokenVerifier appUserTokenVerifier, AuthorityService authorityService,
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
            http.with(
                new AppApiKeySecurityConfigurer(
                    appInstanceService, appService, appUserTokenVerifier, mock(EnvironmentService.class)),
                withDefaults());

            return http.build();
        }

        @SuppressWarnings("unchecked")
        private static <T> ObjectProvider<T> objectProvider(T bean) {
            ObjectProvider<T> objectProvider = mock(ObjectProvider.class);

            when(objectProvider.getIfAvailable()).thenReturn(bean);

            return objectProvider;
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

        @GetMapping({
            CONTEXT_PATH, USER_PATH
        })
        String getUser(Authentication authentication) {
            AppPrincipal appPrincipal = (AppPrincipal) Objects.requireNonNull(authentication.getPrincipal());

            AppUser appUser = appPrincipal.user();

            return appUser == null ? "none" : appUser.id() + ":" + appUser.login();
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

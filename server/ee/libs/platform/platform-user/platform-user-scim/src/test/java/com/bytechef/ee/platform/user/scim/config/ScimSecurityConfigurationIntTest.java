/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.scim.web.filter.ScimBearerTokenAuthenticationFilter;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.platform.security.web.config.AuthorizeHttpRequestContributor;
import com.bytechef.platform.security.web.config.SecurityConfigurerContributor;
import com.bytechef.platform.security.web.config.SpaWebFilterContributor;
import com.bytechef.security.config.RememberMeKey;
import com.bytechef.security.config.SecurityConfiguration;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import jakarta.servlet.Filter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@AutoConfigureMockMvc
@EnableConfigurationProperties(ApplicationProperties.class)
@SpringBootTest(
    classes = {
        SecurityConfiguration.class, ApplicationProperties.class, RememberMeKey.class, ScimSecurityConfiguration.class,
        ScimSecurityConfigurationIntTest.ScimSecurityTestConfiguration.class
    },
    properties = {
        "bytechef.edition=ee",
        "bytechef.security.content-security-policy=default-src 'self'",
        "bytechef.security.remember-me.key=scim-security-int-test",
        "bytechef.security.sso.enabled=true",
        "bytechef.tenant.mode=multi"
    })
class ScimSecurityConfigurationIntTest {

    private static final String[] ENABLING_PROPERTIES = {
        "bytechef.edition=ee", "bytechef.security.sso.enabled=true", "bytechef.tenant.mode=multi"
    };
    private static final MediaType SCIM_JSON = MediaType.parseMediaType("application/scim+json");
    private static final String SCIM_API_KEY = "scim-api-key-value";
    private static final String SCIM_USERS_PATH = "/api/scim/v2/Users";
    private static final String TENANT_ID = "000001";

    @MockitoBean
    private AuthenticationFailureHandler authenticationFailureHandler;

    @MockitoBean
    private AuthenticationSuccessHandler authenticationSuccessHandler;

    @MockitoBean(name = "corsConfigurationSource")
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private FilterChainProxy filterChainProxy;

    @MockitoBean
    private IdentityProviderService identityProviderService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private RememberMeServices rememberMeServices;

    @MockitoBean
    private TenantService tenantService;

    @BeforeEach
    void beforeEach() {
        IdentityProvider identityProvider = new IdentityProvider();

        identityProvider.setId(5L);
        identityProvider.setName("Okta");

        when(tenantService.getTenantIds()).thenReturn(List.of(TenantContext.DEFAULT_TENANT_ID, TENANT_ID));
        when(identityProviderService.fetchByScimApiKey(SCIM_API_KEY))
            .thenAnswer(invocation -> TENANT_ID.equals(TenantContext.getCurrentTenantId())
                ? Optional.of(identityProvider) : Optional.empty());
    }

    @Test
    void testConfigurationIsActiveOnlyForEnterpriseMultiTenantSso() {
        ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ScimSecurityConfiguration.class)
            .withBean(IdentityProviderService.class, () -> mock(IdentityProviderService.class))
            .withBean(TenantService.class, () -> mock(TenantService.class));

        applicationContextRunner
            .withPropertyValues(ENABLING_PROPERTIES)
            .run(context -> {
                assertThat(context).hasSingleBean(AuthorizeHttpRequestContributor.class);
                assertThat(context).hasSingleBean(SecurityConfigurerContributor.class);

                AuthorizeHttpRequestContributor authorizeHttpRequestContributor =
                    context.getBean(AuthorizeHttpRequestContributor.class);

                assertThat(authorizeHttpRequestContributor.getApiPermitAllRequestMatcherPaths())
                    .containsExactly("/api/scim/v2/**");
                assertThat(authorizeHttpRequestContributor.getPermitAllRequestMatcherPaths()).isEmpty();
                assertThat(authorizeHttpRequestContributor.getFrameablePermitAllRequestMatcherPaths()).isEmpty();
            });

        for (String missingProperty : ENABLING_PROPERTIES) {
            String[] propertyValues = Arrays.stream(ENABLING_PROPERTIES)
                .filter(property -> !property.equals(missingProperty))
                .toArray(String[]::new);

            applicationContextRunner
                .withPropertyValues(propertyValues)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AuthorizeHttpRequestContributor.class);
                    assertThat(context).doesNotHaveBean(SecurityConfigurerContributor.class);
                });
        }
    }

    @Test
    void testApiFilterChainRunsTheBearerTokenFilterOnceBeforeAuthorization() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", SCIM_USERS_PATH);

        request.setRequestURI(SCIM_USERS_PATH);

        List<Filter> filters = filterChainProxy.getFilterChains()
            .stream()
            .filter(securityFilterChain -> securityFilterChain.matches(request))
            .findFirst()
            .map(SecurityFilterChain::getFilters)
            .orElseThrow();

        List<Class<?>> filterClasses = filters.stream()
            .<Class<?>>map(Filter::getClass)
            .toList();

        assertThat(filterClasses).containsOnlyOnce(ScimBearerTokenAuthenticationFilter.class);
        assertThat(filterClasses.indexOf(ScimBearerTokenAuthenticationFilter.class))
            .isLessThan(filterClasses.indexOf(AuthorizationFilter.class));
    }

    @Test
    void testScimRequestWithoutBearerTokenIsRejectedWithAScimError() throws Exception {
        mockMvc.perform(get(SCIM_USERS_PATH))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(SCIM_JSON))
            .andExpect(jsonPath("$.schemas[0]").value("urn:ietf:params:scim:api:messages:2.0:Error"))
            .andExpect(jsonPath("$.detail").value("Missing or invalid Authorization header"))
            .andExpect(jsonPath("$.status").value("401"));
    }

    @Test
    void testScimRequestWithAnUnknownScimApiKeyIsRejected() throws Exception {
        mockMvc.perform(
            get(SCIM_USERS_PATH)
                .header("Authorization", "Bearer unknown-key"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Invalid SCIM API key"));
    }

    @Test
    void testScimRequestWithTheScimApiKeyReachesTheControllerAsTheScimClientInItsTenant() throws Exception {
        mockMvc.perform(
            get(SCIM_USERS_PATH)
                .header("Authorization", "Bearer " + SCIM_API_KEY))
            .andExpect(status().isOk())
            .andExpect(content().string("scim:Okta (5)@" + TENANT_ID));
    }

    @Test
    void testNonScimApiRequestIsNotAuthenticatedByTheScimApiKey() throws Exception {
        mockMvc.perform(
            get("/api/other")
                .header("Authorization", "Bearer " + SCIM_API_KEY))
            .andExpect(status().isUnauthorized());
    }

    @TestConfiguration
    static class ScimSecurityTestConfiguration {

        @Bean
        List<SpaWebFilterContributor> spaWebFilterContributors() {
            return List.of();
        }

        @RestController
        static class ScimProbeController {

            @GetMapping({
                "/api/other", "/api/scim/v2/Users"
            })
            String probe() {
                SecurityContext securityContext = SecurityContextHolder.getContext();

                Authentication authentication = Objects.requireNonNull(securityContext.getAuthentication());

                return authentication.getName() + "@" + TenantContext.getCurrentTenantId();
            }
        }
    }
}

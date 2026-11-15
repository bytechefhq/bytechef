/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.domain.ScimApiKey;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ScimBearerTokenAuthenticationFilterTest {

    private static final String API_KEY = "bcscim_000007_scim-api-key";
    private static final String SCIM_USERS_URI = "/api/scim/v2/Users";

    private final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);
    private final TenantService tenantService = mock(TenantService.class);
    private final ScimBearerTokenAuthenticationFilter scimBearerTokenAuthenticationFilter =
        new ScimBearerTokenAuthenticationFilter(identityProviderService, tenantService);

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testTokenFormat() {
        String scimApiKey = ScimApiKey.generate("000004");

        assertThat(scimApiKey).startsWith("bcscim_000004_");
        assertThat(ScimApiKey.tenantIdOf(scimApiKey)).contains("000004");
        assertThat(ScimApiKey.hash(scimApiKey)).hasSize(64)
            .isNotEqualTo(scimApiKey);
        assertThat(ScimApiKey.tenantIdOf("not-a-scim-token")).isEmpty();
        assertThat(ScimApiKey.tenantIdOf("bcscim__secret")).isEmpty();
    }

    @Test
    void testNonScimRequestIsNotFiltered() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/platform/internal/users");
        MockFilterChain filterChain = new MockFilterChain();

        scimBearerTokenAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(filterChain.getRequest()).isSameAs(request);
        assertThat(response.getStatus()).isEqualTo(200);

        verifyNoInteractions(identityProviderService, tenantService);
    }

    @Test
    void testScimDiscoveryRootWithoutTrailingSlashIsNotFiltered() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/scim/v2");
        MockFilterChain filterChain = new MockFilterChain();

        scimBearerTokenAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(filterChain.getRequest()).isSameAs(request);
    }

    @Test
    void testMissingAuthorizationHeaderIsRejected() throws Exception {
        MockFilterChain filterChain = new MockFilterChain();

        scimBearerTokenAuthenticationFilter.doFilter(
            new MockHttpServletRequest("GET", SCIM_USERS_URI), response, filterChain);

        assertUnauthorized("Missing or invalid Authorization header");
        assertThat(filterChain.getRequest()).isNull();

        verifyNoInteractions(identityProviderService, tenantService);
    }

    @Test
    void testNonBearerAuthorizationHeaderIsRejected() throws Exception {
        MockHttpServletRequest request = createScimRequest("Basic dXNlcjpwYXNz");
        MockFilterChain filterChain = new MockFilterChain();

        scimBearerTokenAuthenticationFilter.doFilter(request, response, filterChain);

        assertUnauthorized("Missing or invalid Authorization header");
        assertThat(filterChain.getRequest()).isNull();
    }

    @Test
    void testEmptyBearerTokenIsRejected() throws Exception {
        MockHttpServletRequest request = createScimRequest("Bearer    ");
        MockFilterChain filterChain = new MockFilterChain();

        scimBearerTokenAuthenticationFilter.doFilter(request, response, filterChain);

        assertUnauthorized("Invalid SCIM API key");
        assertThat(filterChain.getRequest()).isNull();

        verifyNoInteractions(identityProviderService);
    }

    @Test
    void testTokenWithoutTenantIsRejectedWithoutLookup() throws Exception {
        MockFilterChain filterChain = new MockFilterChain();

        scimBearerTokenAuthenticationFilter.doFilter(
            createScimRequest("Bearer scim-api-key"), response, filterChain);

        assertUnauthorized("Invalid SCIM API key");
        assertThat(filterChain.getRequest()).isNull();

        verifyNoInteractions(identityProviderService);
    }

    @Test
    void testTokenOfAnotherTenantModeIsRejectedWithoutLookup() throws Exception {
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);

        MockFilterChain filterChain = new MockFilterChain();

        scimBearerTokenAuthenticationFilter.doFilter(createScimRequest("Bearer " + API_KEY), response, filterChain);

        assertUnauthorized("Invalid SCIM API key");
        assertThat(filterChain.getRequest()).isNull();

        verify(identityProviderService, never()).fetchByScimApiKey(anyString());
    }

    @Test
    void testUnknownApiKeyIsRejectedAfterOneLookupInItsTenant() throws Exception {
        AtomicReference<String> lookupTenantId = new AtomicReference<>();

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(identityProviderService.fetchByScimApiKey(API_KEY)).thenAnswer(invocation -> {
            lookupTenantId.set(TenantContext.getCurrentTenantId());

            return Optional.empty();
        });

        MockFilterChain filterChain = new MockFilterChain();

        scimBearerTokenAuthenticationFilter.doFilter(createScimRequest("Bearer " + API_KEY), response, filterChain);

        assertUnauthorized("Invalid SCIM API key");
        assertThat(filterChain.getRequest()).isNull();
        assertThat(lookupTenantId.get()).isEqualTo("000007");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testValidApiKeyRunsChainInTenantAndScimSecurityContext() throws Exception {
        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(identityProviderService.fetchByScimApiKey(API_KEY))
            .thenReturn(Optional.of(createIdentityProvider(5L, "Okta")));

        AtomicReference<String> chainTenantId = new AtomicReference<>();
        AtomicReference<Authentication> chainAuthentication = new AtomicReference<>();

        FilterChain filterChain = (servletRequest, servletResponse) -> {
            SecurityContext securityContext = SecurityContextHolder.getContext();

            chainTenantId.set(TenantContext.getCurrentTenantId());
            chainAuthentication.set(securityContext.getAuthentication());
        };

        scimBearerTokenAuthenticationFilter.doFilter(
            createScimRequest("Bearer  " + API_KEY + "  "), response, filterChain);

        Authentication authentication = chainAuthentication.get();

        assertThat(chainTenantId.get()).isEqualTo("000007");
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isEqualTo("scim:Okta (5)");
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_SCIM");
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void testSingleTenantApiKeyRunsChainInTheDefaultTenant() throws Exception {
        String apiKey = "bcscim_" + TenantContext.DEFAULT_TENANT_ID + "_scim-api-key";

        when(tenantService.isMultiTenantEnabled()).thenReturn(false);
        when(identityProviderService.fetchByScimApiKey(apiKey))
            .thenReturn(Optional.of(createIdentityProvider(5L, "Okta")));

        AtomicReference<String> chainTenantId = new AtomicReference<>();

        scimBearerTokenAuthenticationFilter.doFilter(
            createScimRequest("Bearer " + apiKey), response,
            (servletRequest, servletResponse) -> chainTenantId.set(TenantContext.getCurrentTenantId()));

        assertThat(chainTenantId.get()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void testPreviousSecurityAndTenantContextsAreRestoredAfterChain() throws Exception {
        SecurityContext previousSecurityContext = new SecurityContextImpl(
            new TestingAuthenticationToken("previous", null));

        SecurityContextHolder.setContext(previousSecurityContext);

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(identityProviderService.fetchByScimApiKey(API_KEY))
            .thenReturn(Optional.of(createIdentityProvider(5L, "Okta")));

        scimBearerTokenAuthenticationFilter.doFilter(
            createScimRequest("Bearer " + API_KEY), response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext()).isSameAs(previousSecurityContext);
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testContextsAreRestoredWhenChainFails() {
        SecurityContext previousSecurityContext = new SecurityContextImpl(
            new TestingAuthenticationToken("previous", null));

        SecurityContextHolder.setContext(previousSecurityContext);

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(identityProviderService.fetchByScimApiKey(API_KEY))
            .thenReturn(Optional.of(createIdentityProvider(5L, "Okta")));

        ServletException servletException = new ServletException("downstream failure");

        FilterChain filterChain = new FilterChain() {

            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
                throws ServletException {

                throw servletException;
            }
        };

        assertThatThrownBy(
            () -> scimBearerTokenAuthenticationFilter.doFilter(
                createScimRequest("Bearer " + API_KEY), response, filterChain))
                    .satisfies(throwable -> assertThat(findCause(throwable, ServletException.class))
                        .isSameAs(servletException));

        assertThat(SecurityContextHolder.getContext()).isSameAs(previousSecurityContext);
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testLongIdentityProviderNameIsTruncatedInPrincipal() throws Exception {
        String longName = "A".repeat(80);

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(identityProviderService.fetchByScimApiKey(API_KEY))
            .thenReturn(Optional.of(createIdentityProvider(123L, longName)));

        String principal = filterAndCapturePrincipal();

        assertThat(principal).hasSize(50);
        assertThat(principal).startsWith("scim:AAAA");
        assertThat(principal).endsWith(" (123)");
    }

    @Test
    void testMissingIdentityProviderNameProducesPrincipalWithIdOnly() throws Exception {
        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(identityProviderService.fetchByScimApiKey(API_KEY))
            .thenReturn(Optional.of(createIdentityProvider(9L, null)));

        String principal = filterAndCapturePrincipal();

        assertThat(principal).isEqualTo("scim: (9)");
    }

    private void assertUnauthorized(String expectedDetail) throws Exception {
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/scim+json");

        JSONObject errorJsonObject = new JSONObject(response.getContentAsString());
        JSONArray schemasJsonArray = errorJsonObject.getJSONArray("schemas");
        SecurityContext securityContext = SecurityContextHolder.getContext();

        assertThat(schemasJsonArray.toList()).containsExactly("urn:ietf:params:scim:api:messages:2.0:Error");
        assertThat(errorJsonObject.getString("detail")).isEqualTo(expectedDetail);
        assertThat(errorJsonObject.getString("status")).isEqualTo("401");
        assertThat(securityContext.getAuthentication()).isNull();
    }

    private String filterAndCapturePrincipal() throws Exception {
        AtomicReference<Authentication> chainAuthentication = new AtomicReference<>();

        scimBearerTokenAuthenticationFilter.doFilter(
            createScimRequest("Bearer " + API_KEY), response, (servletRequest, servletResponse) -> {
                SecurityContext securityContext = SecurityContextHolder.getContext();

                chainAuthentication.set(securityContext.getAuthentication());
            });

        Authentication authentication = chainAuthentication.get();

        assertThat(authentication).isNotNull();

        return (String) authentication.getPrincipal();
    }

    private static MockHttpServletRequest createScimRequest(String authorizationHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", SCIM_USERS_URI);

        request.addHeader("Authorization", authorizationHeader);

        return request;
    }

    private static IdentityProvider createIdentityProvider(long id, String name) {
        IdentityProvider identityProvider = new IdentityProvider();

        identityProvider.setId(id);
        identityProvider.setName(name);
        identityProvider.setScimApiKey(ScimApiKey.hash(API_KEY));

        return identityProvider;
    }

    private static Throwable findCause(Throwable throwable, Class<? extends Throwable> causeClass) {
        Throwable current = throwable;

        while (current != null && !causeClass.isInstance(current)) {
            current = current.getCause();
        }

        return current;
    }
}

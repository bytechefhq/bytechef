/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.domain.ScimApiKey;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ScimBearerTokenAuthenticationFilterTest {

    private final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);
    private final TenantService tenantService = mock(TenantService.class);
    private final ScimBearerTokenAuthenticationFilter filter =
        new ScimBearerTokenAuthenticationFilter(identityProviderService, tenantService);

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
    void testMissingTokenIsUnauthorizedWithScimError() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(scimRequest(null), response, (request, servletResponse) -> {
            throw new AssertionError("chain must not run");
        });

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/scim+json");
        assertThat(response.getContentAsString()).contains("urn:ietf:params:scim:api:messages:2.0:Error");
    }

    @Test
    void testTokenOfAnotherTenantModeIsRejectedWithoutLookup() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(
            scimRequest(ScimApiKey.generate("000004")), response, (request, servletResponse) -> {
                throw new AssertionError("chain must not run");
            });

        assertThat(response.getStatus()).isEqualTo(401);

        verify(identityProviderService, never()).fetchByScimApiKey(anyString());
    }

    @Test
    void testValidTokenRunsTheRequestAsTheScimPrincipalInItsTenant() throws Exception {
        String scimApiKey = ScimApiKey.generate("000004");
        AtomicReference<String> tenantIdInChain = new AtomicReference<>();
        AtomicReference<Authentication> authenticationInChain = new AtomicReference<>();
        IdentityProvider identityProvider = new IdentityProvider();

        identityProvider.setName("Okta");

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(identityProviderService.fetchByScimApiKey(scimApiKey)).thenAnswer(invocation -> {
            assertThat(TenantContext.getCurrentTenantId()).isEqualTo("000004");

            return Optional.of(identityProvider);
        });

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(scimRequest(scimApiKey), response, (request, servletResponse) -> {
            tenantIdInChain.set(TenantContext.getCurrentTenantId());
            authenticationInChain.set(SecurityContextHolder.getContext()
                .getAuthentication());
        });

        assertThat(tenantIdInChain.get()).isEqualTo("000004");
        assertThat(authenticationInChain.get()
            .getName()).isEqualTo("scim:Okta");
        assertThat(authenticationInChain.get()
            .getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_SCIM");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
        assertThat(SecurityContextHolder.getContext()
            .getAuthentication()).isNull();
    }

    @Test
    void testUnknownTokenIsUnauthorized() throws Exception {
        when(identityProviderService.fetchByScimApiKey(anyString())).thenReturn(Optional.empty());

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(scimRequest(ScimApiKey.generate("public")), response, (request, servletResponse) -> {
            throw new AssertionError("chain must not run");
        });

        assertThat(response.getStatus()).isEqualTo(401);
    }

    private static MockHttpServletRequest scimRequest(String bearerToken) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/scim/v2/Users");

        if (bearerToken != null) {
            request.addHeader("Authorization", "Bearer " + bearerToken);
        }

        return request;
    }
}

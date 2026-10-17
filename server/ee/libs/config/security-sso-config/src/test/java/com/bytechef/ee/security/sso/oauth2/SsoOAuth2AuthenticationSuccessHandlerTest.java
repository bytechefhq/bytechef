/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.security.web.authentication.TenantUser;
import com.bytechef.security.web.oauth2.CustomOAuth2User;
import com.bytechef.security.web.oauth2.CustomOidcUser;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.service.TenantService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.RememberMeServices;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SsoOAuth2AuthenticationSuccessHandlerTest {

    private static final String EMAIL = "someone@example.com";
    private static final String TENANT_ID = "000003";
    private static final String TENANT_LOGIN = "someone";

    private final RememberMeServices rememberMeServices = mock(RememberMeServices.class);
    private final TenantService tenantService = mock(TenantService.class);
    private final UserService userService = mock(UserService.class);

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Test
    void testOidcSignInUsesTheSingleTenantMatchingTheEmail() throws IOException {
        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIdsByUserEmail(EMAIL)).thenReturn(List.of(TENANT_ID));
        when(userService.fetchUserByEmail(EMAIL)).thenAnswer(invocation -> fetchTenantUser());

        handler().onAuthenticationSuccess(request, response, oidcAuthentication());

        assertThat(request.getSession()
            .getAttribute(TenantConstants.CURRENT_TENANT_ID)).isEqualTo(TENANT_ID);
        assertThat(response.getRedirectedUrl()).isEqualTo("/oauth2/redirect");

        TenantUser tenantUser = captureRememberMePrincipal();

        assertThat(tenantUser.getUsername()).isEqualTo(TENANT_LOGIN);
        assertThat(tenantUser.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void testEmailMatchingSeveralTenantsIsRejected() throws IOException {
        MockHttpSession session = new MockHttpSession();

        request.setSession(session);

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIdsByUserEmail(EMAIL)).thenReturn(List.of(TENANT_ID, "000004"));

        handler().onAuthenticationSuccess(request, response, oidcAuthentication());

        assertThat(session.isInvalid()).isTrue();
        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error=oauth2");

        verify(rememberMeServices).loginFail(request, response);
        verify(rememberMeServices, never()).loginSuccess(any(), any(), any());
    }

    @Test
    void testSingleTenantSignInDoesNotLookUpTenants() throws IOException {
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);
        when(tenantService.getTenantIdsByUserEmail(anyString())).thenThrow(new UnsupportedOperationException());
        when(tenantService.getTenantIdsByUserLogin(anyString())).thenThrow(new UnsupportedOperationException());
        when(userService.fetchUserByEmail(EMAIL)).thenReturn(Optional.of(createUser()));

        CustomOAuth2User customOAuth2User = new CustomOAuth2User(EMAIL, List.of(), Map.of("email", EMAIL));

        handler().onAuthenticationSuccess(
            request, response, new TestingAuthenticationToken(customOAuth2User, null, List.of()));

        assertThat(response.getRedirectedUrl()).isEqualTo("/oauth2/redirect");
        assertThat(captureRememberMePrincipal().getUsername()).isEqualTo(TENANT_LOGIN);
    }

    private TenantUser captureRememberMePrincipal() {
        ArgumentCaptor<Authentication> authenticationArgumentCaptor = ArgumentCaptor.forClass(Authentication.class);

        verify(rememberMeServices).loginSuccess(eq(request), eq(response), authenticationArgumentCaptor.capture());

        Authentication authentication = authenticationArgumentCaptor.getValue();

        assertThat(authentication.getPrincipal()).isInstanceOf(TenantUser.class);

        return (TenantUser) authentication.getPrincipal();
    }

    private static User createUser() {
        User user = new User();

        user.setActivated(true);
        user.setEmail(EMAIL);
        user.setLogin(TENANT_LOGIN);

        return user;
    }

    private static Optional<User> fetchTenantUser() {
        if (!TENANT_ID.equals(TenantContext.getCurrentTenantId())) {
            return Optional.empty();
        }

        return Optional.of(createUser());
    }

    private SsoOAuth2AuthenticationSuccessHandler handler() {
        return new SsoOAuth2AuthenticationSuccessHandler(rememberMeServices, tenantService, userService);
    }

    private static Authentication oidcAuthentication() {
        OidcUser oidcUser = mock(OidcUser.class);

        when(oidcUser.getClaims()).thenReturn(Map.of(StandardClaimNames.EMAIL, EMAIL));

        CustomOidcUser customOidcUser = new CustomOidcUser("public-login", List.of(), oidcUser);

        return new TestingAuthenticationToken(customOidcUser, null, List.of());
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.saml2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.security.web.authentication.TenantUser;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.web.authentication.RememberMeServices;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SsoSaml2AuthenticationSuccessHandlerTest {

    private static final String EMAIL_CLAIM = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress";
    private static final String EMAIL = "someone@example.com";
    private static final String TENANT_ID = "000003";
    private static final String TENANT_LOGIN = "someone";

    private final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);
    private final RememberMeServices rememberMeServices = mock(RememberMeServices.class);
    private final TenantService tenantService = mock(TenantService.class);
    private final UserService userService = mock(UserService.class);

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final HttpSession session = mock(HttpSession.class);

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testRefusedProvisioningEndsTheAuthenticatedSession() throws IOException {
        when(
            userService.findOrCreateSocialUser(
                anyString(), nullable(String.class), nullable(String.class), nullable(String.class), anyString(),
                anyString(), anyBoolean(), anyString()))
                    .thenThrow(new IllegalStateException("Auto-provisioning is disabled for this identity provider"));

        when(request.getSession(false)).thenReturn(session);

        Authentication authentication = authentication();

        SecurityContextHolder.getContext()
            .setAuthentication(authentication);

        handler().onAuthenticationSuccess(request, response, authentication);

        assertThat(SecurityContextHolder.getContext()
            .getAuthentication())
                .as("the session the filter already authenticated must not survive a refused provisioning")
                .isNull();

        verify(session).invalidate();
        verify(rememberMeServices).loginFail(request, response);
        verify(rememberMeServices, never()).loginSuccess(any(), any(), any());
        verify(response).sendRedirect("/login?error=saml");
        verify(response, never()).sendRedirect("/oauth2/redirect");
    }

    @Test
    void testProvisionedUserReachesTheApplicationInTheSingleMatchingTenant() throws IOException {
        stubProvisioning();

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIdsByUserEmail(EMAIL)).thenReturn(List.of(TENANT_ID));
        when(userService.fetchUserByEmail(EMAIL)).thenAnswer(invocation -> fetchTenantUser());
        when(request.getSession()).thenReturn(session);

        handler().onAuthenticationSuccess(request, response, authentication());

        verify(session).setAttribute(TenantConstants.CURRENT_TENANT_ID, TENANT_ID);
        verify(response).sendRedirect("/oauth2/redirect");
        verify(session, never()).invalidate();

        TenantUser tenantUser = captureRememberMePrincipal();

        assertThat(tenantUser.getUsername()).isEqualTo(TENANT_LOGIN);
        assertThat(tenantUser.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void testEmailMatchingSeveralTenantsIsRejected() throws IOException {
        stubProvisioning();

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIdsByUserEmail(EMAIL)).thenReturn(List.of(TENANT_ID, "000004"));
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);

        handler().onAuthenticationSuccess(request, response, authentication());

        verify(session, never()).setAttribute(eq(TenantConstants.CURRENT_TENANT_ID), any());
        verify(session).invalidate();
        verify(rememberMeServices).loginFail(request, response);
        verify(rememberMeServices, never()).loginSuccess(any(), any(), any());
        verify(response).sendRedirect("/login?error=saml");
    }

    @Test
    void testSingleTenantSignInIssuesRememberMeForTheByteChefLogin() throws IOException {
        stubProvisioning();

        when(tenantService.isMultiTenantEnabled()).thenReturn(false);
        when(tenantService.getTenantIdsByUserEmail(anyString())).thenThrow(new UnsupportedOperationException());
        when(userService.fetchUserByEmail(EMAIL)).thenReturn(Optional.of(createUser()));
        when(request.getSession()).thenReturn(session);

        handler().onAuthenticationSuccess(request, response, authentication());

        verify(response).sendRedirect("/oauth2/redirect");
        verify(session, never()).invalidate();

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

    private void stubProvisioning() {
        when(
            userService.findOrCreateSocialUser(
                anyString(), nullable(String.class), nullable(String.class), nullable(String.class), anyString(),
                anyString(), anyBoolean(), anyString()))
                    .thenReturn(new User());
    }

    private Authentication authentication() {
        Saml2AuthenticatedPrincipal principal = mock(Saml2AuthenticatedPrincipal.class);

        when(principal.getAttribute(EMAIL_CLAIM)).thenReturn(List.of(EMAIL));
        when(principal.getName()).thenReturn("saml-name-id");
        when(principal.getRelyingPartyRegistrationId()).thenReturn(null);

        return new TestingAuthenticationToken(principal, "credentials");
    }

    private SsoSaml2AuthenticationSuccessHandler handler() {
        return new SsoSaml2AuthenticationSuccessHandler(
            identityProviderService, rememberMeServices, tenantService, userService);
    }
}

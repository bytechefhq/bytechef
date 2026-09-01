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
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.IdentityProviderService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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
    void testProvisionedUserReachesTheApplication() throws IOException {
        when(
            userService.findOrCreateSocialUser(
                anyString(), nullable(String.class), nullable(String.class), nullable(String.class), anyString(),
                anyString(), anyBoolean(), anyString()))
                    .thenReturn(new User());

        when(tenantService.getTenantIdsByUserEmail(EMAIL)).thenReturn(List.of("tenant-1"));
        when(request.getSession()).thenReturn(session);

        Authentication authentication = authentication();

        handler().onAuthenticationSuccess(request, response, authentication);

        verify(session).setAttribute(TenantConstants.CURRENT_TENANT_ID, "tenant-1");
        verify(rememberMeServices).loginSuccess(request, response, authentication);
        verify(response).sendRedirect("/oauth2/redirect");
        verify(session, never()).invalidate();
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

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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.security.sso.IdentityProviderLocator;
import com.bytechef.ee.security.sso.IdentityProviderLocator.LocatedIdentityProvider;
import com.bytechef.security.web.oauth2.CustomOidcUser;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.service.TenantService;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.RememberMeServices;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SsoOAuth2AuthenticationSuccessHandlerTest {

    private static final String LOGIN = "someone";
    private static final String REGISTRATION_ID = "sso-000003-7";
    private static final String TENANT_ID = "000003";

    private final IdentityProviderLocator identityProviderLocator = mock(IdentityProviderLocator.class);
    private final RememberMeServices rememberMeServices = mock(RememberMeServices.class);
    private final TenantService tenantService = mock(TenantService.class);

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Test
    void testOidcSignInBindsTheSessionToTheIdentityProvidersTenant() throws IOException {
        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(identityProviderLocator.findByRegistrationId(REGISTRATION_ID))
            .thenReturn(Optional.of(new LocatedIdentityProvider(TENANT_ID, new IdentityProvider())));

        handler().onAuthenticationSuccess(request, response, oidcAuthentication());

        assertThat(request.getSession()
            .getAttribute(TenantConstants.CURRENT_TENANT_ID)).isEqualTo(TENANT_ID);
        assertThat(response.getRedirectedUrl()).isEqualTo("/oauth2/redirect");

        ArgumentCaptor<Authentication> authenticationArgumentCaptor = ArgumentCaptor.forClass(Authentication.class);

        verify(rememberMeServices).loginSuccess(eq(request), eq(response), authenticationArgumentCaptor.capture());
        verify(tenantService, never()).getTenantIdsByUserLogin(anyString());

        Authentication authentication = authenticationArgumentCaptor.getValue();

        assertThat(authentication.getName()).isEqualTo(LOGIN);
    }

    @Test
    void testUnknownIdentityProviderIsRejected() throws IOException {
        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(identityProviderLocator.findByRegistrationId(REGISTRATION_ID)).thenReturn(Optional.empty());

        handler().onAuthenticationSuccess(request, response, oidcAuthentication());

        assertThat(request.getSession(false)).isNull();
        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error=oauth2");

        verify(rememberMeServices, never()).loginSuccess(any(), any(), any());
    }

    @Test
    void testSingleTenantSignInDoesNotLookUpTheTenant() throws IOException {
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);

        handler().onAuthenticationSuccess(request, response, oidcAuthentication());

        assertThat(response.getRedirectedUrl()).isEqualTo("/oauth2/redirect");

        verify(rememberMeServices).loginSuccess(eq(request), eq(response), any());
        verifyNoInteractions(identityProviderLocator);
    }

    private SsoOAuth2AuthenticationSuccessHandler handler() {
        return new SsoOAuth2AuthenticationSuccessHandler(identityProviderLocator, rememberMeServices, tenantService,
            null);
    }

    private static Authentication oidcAuthentication() {
        CustomOidcUser customOidcUser = new CustomOidcUser(LOGIN, List.of(), mock(OidcUser.class));

        return new OAuth2AuthenticationToken(customOidcUser, List.of(), REGISTRATION_ID);
    }
}

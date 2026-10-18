/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.saml2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.security.sso.IdentityProviderLocator;
import com.bytechef.ee.security.sso.IdentityProviderLocator.LocatedIdentityProvider;
import com.bytechef.ee.security.sso.SsoUserProvisioner;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.security.web.oauth2.SocialUserProvisioner.ProvisionedUser;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2ResponseAssertionAccessor;
import org.springframework.security.web.authentication.RememberMeServices;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SsoSaml2AuthenticationSuccessHandlerTest {

    private static final String EMAIL = "someone@acme.com";
    private static final String REGISTRATION_ID = "saml-000003-7";

    private final IdentityProviderLocator identityProviderLocator = mock(IdentityProviderLocator.class);
    private final RememberMeServices rememberMeServices = mock(RememberMeServices.class);
    private final SsoUserProvisioner ssoUserProvisioner = mock(SsoUserProvisioner.class);
    private final TenantService tenantService = mock(TenantService.class);

    private final LocatedIdentityProvider locatedIdentityProvider =
        new LocatedIdentityProvider("000003", new IdentityProvider());

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testRefusedProvisioningEndsTheAuthenticatedSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = new MockHttpSession();

        request.setSession(session);

        when(identityProviderLocator.findByRegistrationId(REGISTRATION_ID))
            .thenReturn(Optional.of(locatedIdentityProvider));
        when(
            ssoUserProvisioner.provision(
                any(), anyString(), nullable(String.class), nullable(String.class), isNull(), anyString()))
                    .thenThrow(new OAuth2AuthenticationException(new OAuth2Error("account_not_provisioned")));

        Authentication authentication = authentication();

        SecurityContextHolder.getContext()
            .setAuthentication(authentication);

        handler().onAuthenticationSuccess(request, response, authentication);

        assertThat(SecurityContextHolder.getContext()
            .getAuthentication())
                .as("the session the filter already authenticated must not survive a refused provisioning")
                .isNull();
        assertThat(session.isInvalid()).isTrue();
        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error=saml&code=account_not_provisioned");

        verify(rememberMeServices).loginFail(request, response);
        verify(rememberMeServices, never()).loginSuccess(any(), any(), any());
    }

    @Test
    void testProvisionedUserReachesTheApplicationAsTheByteChefLogin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(identityProviderLocator.findByRegistrationId(REGISTRATION_ID))
            .thenReturn(Optional.of(locatedIdentityProvider));
        when(
            ssoUserProvisioner.provision(
                eq(locatedIdentityProvider), eq(EMAIL), nullable(String.class), nullable(String.class), isNull(),
                eq("saml-name-id")))
                    .thenReturn(
                        new ProvisionedUser("someone", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        handler().onAuthenticationSuccess(request, response, authentication());

        assertThat(SecurityUtils.fetchCurrentUserLogin())
            .as("the application must see the ByteChef login, not the IdP's NameID")
            .contains("someone");
        assertThat(request.getSession()
            .getAttribute(TenantConstants.CURRENT_TENANT_ID)).isEqualTo("000003");
        assertThat(response.getRedirectedUrl()).isEqualTo("/oauth2/redirect");
    }

    @Test
    void testSingleTenantModeDoesNotLookUpTenants() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(identityProviderLocator.findByRegistrationId(REGISTRATION_ID))
            .thenReturn(Optional.of(locatedIdentityProvider));
        when(
            ssoUserProvisioner.provision(
                any(), anyString(), nullable(String.class), nullable(String.class), isNull(), anyString()))
                    .thenReturn(new ProvisionedUser("someone", List.of()));

        handler().onAuthenticationSuccess(request, response, authentication());

        assertThat(request.getSession()
            .getAttribute(TenantConstants.CURRENT_TENANT_ID)).isNull();
        assertThat(response.getRedirectedUrl()).isEqualTo("/oauth2/redirect");

        verify(tenantService, never()).getTenantIdsByUserEmail(any());
        verify(tenantService, never()).getTenantIdsByUserLogin(any());
    }

    @Test
    void testUnknownIdentityProviderIsRejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(identityProviderLocator.findByRegistrationId(REGISTRATION_ID)).thenReturn(Optional.empty());

        handler().onAuthenticationSuccess(request, response, authentication());

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error=saml&code=identity_provider_not_found");
    }

    @Test
    void testReadsEmailFromThePlainMailAttribute() throws Exception {
        Saml2AuthenticatedPrincipal principal = mock(Saml2AuthenticatedPrincipal.class);

        when(principal.getAttributes()).thenReturn(Map.of("mail", List.of(EMAIL)));
        when(principal.getName()).thenReturn("_transient-name-id");
        when(principal.getRelyingPartyRegistrationId()).thenReturn(REGISTRATION_ID);
        when(identityProviderLocator.findByRegistrationId(REGISTRATION_ID))
            .thenReturn(Optional.of(locatedIdentityProvider));
        when(
            ssoUserProvisioner.provision(
                any(), eq(EMAIL), nullable(String.class), nullable(String.class), isNull(), anyString()))
                    .thenReturn(new ProvisionedUser("someone", List.of()));

        MockHttpServletResponse response = new MockHttpServletResponse();

        handler().onAuthenticationSuccess(
            new MockHttpServletRequest(), response, new TestingAuthenticationToken(principal, "credentials"));

        assertThat(response.getRedirectedUrl()).isEqualTo("/oauth2/redirect");
    }

    @Test
    void testReadsTheRegistrationIdFromTheAssertionAuthentication() throws Exception {
        Saml2ResponseAssertionAccessor saml2ResponseAssertionAccessor = mock(Saml2ResponseAssertionAccessor.class);

        when(saml2ResponseAssertionAccessor.getNameId()).thenReturn("saml-name-id");
        when(saml2ResponseAssertionAccessor.getAttributes())
            .thenReturn(Map.of(SsoSaml2AuthenticationSuccessHandler.EMAIL_ATTRIBUTE, List.of(EMAIL)));
        when(identityProviderLocator.findByRegistrationId(REGISTRATION_ID))
            .thenReturn(Optional.of(locatedIdentityProvider));
        when(
            ssoUserProvisioner.provision(
                any(), eq(EMAIL), nullable(String.class), nullable(String.class), isNull(), eq("saml-name-id")))
                    .thenReturn(new ProvisionedUser("someone", List.of()));

        Saml2AssertionAuthentication saml2AssertionAuthentication = new Saml2AssertionAuthentication(
            saml2ResponseAssertionAccessor, List.of(), REGISTRATION_ID);

        MockHttpServletResponse response = new MockHttpServletResponse();

        handler().onAuthenticationSuccess(new MockHttpServletRequest(), response, saml2AssertionAuthentication);

        assertThat(response.getRedirectedUrl()).isEqualTo("/oauth2/redirect");
    }

    @Test
    void testProvisionsWithoutTheSamlAuthenticationInTheSecurityContext() throws Exception {
        when(identityProviderLocator.findByRegistrationId(REGISTRATION_ID))
            .thenReturn(Optional.of(locatedIdentityProvider));
        when(
            ssoUserProvisioner.provision(
                any(), anyString(), nullable(String.class), nullable(String.class), isNull(), anyString()))
                    .thenAnswer(invocation -> {
                        assertThat(SecurityContextHolder.getContext()
                            .getAuthentication())
                                .as("audit fields of a provisioned user must not record the IdP's NameID")
                                .isNull();

                        return new ProvisionedUser("someone", List.of());
                    });

        Authentication authentication = authentication();

        SecurityContextHolder.getContext()
            .setAuthentication(authentication);

        MockHttpServletResponse response = new MockHttpServletResponse();

        handler().onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo("/oauth2/redirect");
    }

    private static Authentication authentication() {
        Saml2AuthenticatedPrincipal principal = mock(Saml2AuthenticatedPrincipal.class);

        when(principal.getAttributes())
            .thenReturn(Map.of(SsoSaml2AuthenticationSuccessHandler.EMAIL_ATTRIBUTE, List.of(EMAIL)));
        when(principal.getName()).thenReturn("saml-name-id");
        when(principal.getRelyingPartyRegistrationId()).thenReturn(REGISTRATION_ID);

        return new TestingAuthenticationToken(principal, "credentials");
    }

    private SsoSaml2AuthenticationSuccessHandler handler() {
        return new SsoSaml2AuthenticationSuccessHandler(
            identityProviderLocator, rememberMeServices, ssoUserProvisioner, tenantService, null);
    }
}

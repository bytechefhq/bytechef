/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.security.web.config.TwoFactorAuthenticationCustomizer;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class MultiTenantAuthenticationSuccessHandlerTest {

    private final TenantService tenantService = mock(TenantService.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<TwoFactorAuthenticationCustomizer> twoFactorAuthenticationCustomizerProvider =
        mock(ObjectProvider.class);

    private final RememberMeServices rememberMeServices = mock(RememberMeServices.class);

    private final MultiTenantAuthenticationSuccessHandler multiTenantAuthenticationSuccessHandler =
        new MultiTenantAuthenticationSuccessHandler(
            rememberMeServices, tenantService, twoFactorAuthenticationCustomizerProvider);

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testSignInWithEmailOfUserWhoseLoginDiffersResolvesTenantByEmail() {
        when(tenantService.getTenantIdsByUserEmail("jane@example.com")).thenReturn(List.of("000002"));

        MockHttpServletRequest request = createLoginRequest("jane@example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        multiTenantAuthenticationSuccessHandler.onAuthenticationSuccess(
            request, response, createAuthentication("jane"));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(request.getSession()
            .getAttribute(TenantConstants.CURRENT_TENANT_ID)).isEqualTo("000002");

        verify(rememberMeServices, never()).loginFail(any(), any());
    }

    @Test
    void testSignInWithLoginResolvesTenantByLogin() {
        when(tenantService.getTenantIdsByUserLogin("jane")).thenReturn(List.of("000003"));

        MockHttpServletRequest request = createLoginRequest("Jane");
        MockHttpServletResponse response = new MockHttpServletResponse();

        multiTenantAuthenticationSuccessHandler.onAuthenticationSuccess(
            request, response, createAuthentication("jane"));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(request.getSession()
            .getAttribute(TenantConstants.CURRENT_TENANT_ID)).isEqualTo("000003");
    }

    @Test
    void testSignInWithoutTenantIsRejectedAndSignsOut() {
        when(tenantService.getTenantIdsByUserEmail("jane@example.com")).thenReturn(List.of());

        Authentication authentication = createAuthentication("jane");

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);

        SecurityContextHolder.setContext(securityContext);

        MockHttpServletRequest request = createLoginRequest("jane@example.com");
        MockHttpSession session = new MockHttpSession();

        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        request.setSession(session);

        MockHttpServletResponse response = new MockHttpServletResponse();

        multiTenantAuthenticationSuccessHandler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(session.isInvalid()).isTrue();

        verify(rememberMeServices).loginFail(request, response);

        assertThat(SecurityContextHolder.getContext()
            .getAuthentication()).isNull();
    }

    private static Authentication createAuthentication(String login) {
        return UsernamePasswordAuthenticationToken.authenticated(
            new User(login, "", List.of()), null, List.of());
    }

    private static MockHttpServletRequest createLoginRequest(String username) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/authentication");

        request.setParameter("username", username);

        return request;
    }
}

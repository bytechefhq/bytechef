/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.security.web.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.RememberMeServices;

/**
 * @author Ivica Cardic
 */
class OAuth2AuthenticationHandlersTest {

    private static final Authentication AUTHENTICATION = new TestingAuthenticationToken(
        "someone@example.com", null, "ROLE_ADMIN");

    private final RememberMeServices rememberMeServices = mock(RememberMeServices.class);
    private final TenantService tenantService = mock(TenantService.class);

    @Test
    void testSuccessInSingleTenantModeRedirectsWithoutTenantLookup() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        new OAuth2AuthenticationSuccessHandler(rememberMeServices, tenantService)
            .onAuthenticationSuccess(request, response, AUTHENTICATION);

        assertThat(response.getRedirectedUrl()).isEqualTo("/oauth2/redirect");
        assertThat(request.getSession(false)).isNull();

        verify(tenantService, never()).getTenantIdsByUserLogin(any());
        verify(rememberMeServices).loginSuccess(request, response, AUTHENTICATION);
    }

    @Test
    void testSuccessInMultiTenantModeBindsSessionToUsersTenant() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIdsByUserLogin("someone@example.com")).thenReturn(List.of("000003"));

        new OAuth2AuthenticationSuccessHandler(rememberMeServices, tenantService)
            .onAuthenticationSuccess(request, response, AUTHENTICATION);

        assertThat(request.getSession()
            .getAttribute(TenantConstants.CURRENT_TENANT_ID)).isEqualTo("000003");
        assertThat(response.getRedirectedUrl()).isEqualTo("/oauth2/redirect");
    }

    @Test
    void testSuccessInMultiTenantModeWithoutTenantFailsTheLogin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIdsByUserLogin("someone@example.com")).thenReturn(List.of());

        new OAuth2AuthenticationSuccessHandler(rememberMeServices, tenantService)
            .onAuthenticationSuccess(request, response, AUTHENTICATION);

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error=oauth2");

        verify(rememberMeServices, never()).loginSuccess(any(), any(), any());
    }

    @Test
    void testFailurePassesOAuth2ErrorCodeToLoginPage() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new OAuth2AuthenticationFailureHandler().onAuthenticationFailure(
            new MockHttpServletRequest(), response,
            new OAuth2AuthenticationException(new OAuth2Error("account_not_provisioned")));

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error=oauth2&code=account_not_provisioned");
    }

    @Test
    void testFailureWithoutOAuth2ErrorRedirectsWithGenericError() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new OAuth2AuthenticationFailureHandler().onAuthenticationFailure(
            new MockHttpServletRequest(), response, new BadCredentialsException("bad"));

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error=oauth2");
    }
}

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

package com.bytechef.security.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.user.service.UserService;
import com.bytechef.security.web.authentication.TwoFactorAuthentication;
import com.bytechef.security.web.authentication.TwoFactorAuthenticationSupport;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.service.TenantService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.RememberMeServices;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class TwoFactorVerificationFilterTest {

    private static final Authentication PRIMARY = new TestingAuthenticationToken("bob", null, "ROLE_USER");

    private final RememberMeServices rememberMeServices = mock(RememberMeServices.class);
    private final TenantService tenantService = mock(TenantService.class);
    private final UserService userService = mock(UserService.class);
    private final TwoFactorVerificationFilter twoFactorVerificationFilter = new TwoFactorVerificationFilter(
        JsonMapper.builder()
            .build(),
        rememberMeServices, tenantService, userService);

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testValidCodeCompletesTheLoginAndRotatesTheSession() throws Exception {
        MockHttpServletRequest request = verifyRequest("{\"code\":\"123456\"}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String sessionIdBefore = request.getSession()
            .getId();

        pending();

        when(userService.verifyTotpCode("bob", "123456")).thenReturn(true);

        twoFactorVerificationFilter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext()
            .getAuthentication()).isSameAs(PRIMARY);
        assertThat(request.getSession()
            .getId()).isNotEqualTo(sessionIdBefore);

        verify(rememberMeServices, never()).loginSuccess(any(), any(), any());
    }

    @Test
    void testValidCodeIssuesTheRememberMeCookieRequestedAtLogin() throws Exception {
        MockHttpServletRequest request = verifyRequest("{\"code\":\"123456\"}");

        request.getSession()
            .setAttribute(TwoFactorAuthenticationSupport.REMEMBER_ME_REQUESTED_ATTRIBUTE, Boolean.TRUE);

        pending();

        when(userService.verifyTotpCode("bob", "123456")).thenReturn(true);

        twoFactorVerificationFilter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);

        verify(rememberMeServices).loginSuccess(requestCaptor.capture(), any(), eq(PRIMARY));

        assertThat(requestCaptor.getValue()
            .getParameter("remember-me")).isEqualTo("true");
    }

    @Test
    void testVerifiesInsideTheTenantTheSessionBelongsTo() throws Exception {
        AtomicReference<String> tenantIdDuringVerification = new AtomicReference<>();
        MockHttpServletRequest request = verifyRequest("{\"code\":\"123456\"}");

        request.getSession()
            .setAttribute(TenantConstants.CURRENT_TENANT_ID, "000005");

        pending();

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(userService.verifyTotpCode("bob", "123456")).thenAnswer(invocation -> {
            tenantIdDuringVerification.set(TenantContext.getCurrentTenantId());

            return true;
        });

        MockHttpServletResponse response = new MockHttpServletResponse();

        twoFactorVerificationFilter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(tenantIdDuringVerification.get()).isEqualTo("000005");
    }

    @Test
    void testRepeatedWrongCodesLockTheLoginAndEndThePendingSession() throws Exception {
        when(userService.verifyTotpCode(eq("bob"), anyString())).thenReturn(false);

        MockHttpServletResponse response = null;
        MockHttpSession session = null;

        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = verifyRequest("{\"code\":\"000000\"}");

            session = (MockHttpSession) request.getSession();

            pending();

            response = new MockHttpServletResponse();

            twoFactorVerificationFilter.doFilter(request, response, mock(FilterChain.class));
        }

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(session.isInvalid()).isTrue();

        MockHttpServletRequest lockedRequest = verifyRequest("{\"code\":\"123456\"}");

        pending();

        MockHttpServletResponse lockedResponse = new MockHttpServletResponse();

        twoFactorVerificationFilter.doFilter(lockedRequest, lockedResponse, mock(FilterChain.class));

        assertThat(lockedResponse.getStatus()).isEqualTo(429);
    }

    @Test
    void testMalformedBodyIsABadRequest() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        pending();

        twoFactorVerificationFilter.doFilter(verifyRequest("{not json"), response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    void testWithoutPendingLoginIsUnauthorized() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        twoFactorVerificationFilter.doFilter(
            verifyRequest("{\"code\":\"123456\"}"), response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(401);
    }

    private static void pending() {
        SecurityContextHolder.getContext()
            .setAuthentication(new TwoFactorAuthentication(PRIMARY));
    }

    private static MockHttpServletRequest verifyRequest(String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/mfa/verify");

        request.setServletPath("/api/mfa/verify");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.setContentType("application/json");

        return request;
    }
}

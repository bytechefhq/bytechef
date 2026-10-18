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

package com.bytechef.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.security.web.config.TwoFactorAuthenticationCustomizer;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.RememberMeServices;

/**
 * @author Ivica Cardic
 */
class TwoFactorAuthenticationTest {

    private static final Authentication AUTHENTICATION = new TestingAuthenticationToken("bob", null, "ROLE_USER");

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testCustomizerLooksTheUserUpInsideTheirTenant() {
        TenantService tenantService = mock(TenantService.class);
        UserService userService = mock(UserService.class);

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIdsByUserLogin("bob")).thenReturn(List.of("000004"));
        when(userService.fetchUserByLogin("bob")).thenAnswer(invocation -> {
            User user = new User();

            user.setTotpEnabled("000004".equals(TenantContext.getCurrentTenantId()));

            return Optional.of(user);
        });

        assertThat(new TwoFactorAuthenticationCustomizerImpl(tenantService, userService).isTotpEnabled(AUTHENTICATION))
            .as("the TOTP flag lives in the user's tenant schema, not in the default one")
            .isTrue();
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testCustomizerUsesTheAuthenticationNameForNonUserDetailsPrincipals() {
        TenantService tenantService = mock(TenantService.class);
        UserService userService = mock(UserService.class);
        User user = new User();

        user.setTotpEnabled(true);

        when(userService.fetchUserByLogin("bob")).thenReturn(Optional.of(user));

        Authentication oauth2LikeAuthentication = new TestingAuthenticationToken(
            (AuthenticatedPrincipal) () -> "bob", null, "ROLE_USER");

        assertThat(
            new TwoFactorAuthenticationCustomizerImpl(tenantService, userService)
                .isTotpEnabled(oauth2LikeAuthentication)).isTrue();
    }

    @Test
    void testRememberMeCookieIsWithheldWhileTheCodeIsPending() {
        RememberMeServices delegate = mock(RememberMeServices.class);
        TwoFactorAuthenticationCustomizer customizer = authentication -> true;

        new TwoFactorRememberMeServices(delegate, customizer).loginSuccess(
            new MockHttpServletRequest(), new MockHttpServletResponse(), AUTHENTICATION);

        verify(delegate, never()).loginSuccess(any(), any(), any());
    }

    @Test
    void testRememberMeCookieIsIssuedWithoutTwoFactor() {
        RememberMeServices delegate = mock(RememberMeServices.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        new TwoFactorRememberMeServices(delegate, authentication -> false).loginSuccess(
            request, response, AUTHENTICATION);

        verify(delegate).loginSuccess(request, response, AUTHENTICATION);
    }

    @Test
    void testBeginSecondFactorStoresPendingAuthenticationAndRememberMeRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setParameter("remember-me", "true");

        TwoFactorAuthenticationSupport.beginSecondFactor(request, new MockHttpServletResponse(), AUTHENTICATION);

        Authentication pendingAuthentication = Objects.requireNonNull(
            SecurityContextHolder.getContext()
                .getAuthentication());

        assertThat(pendingAuthentication).isInstanceOf(TwoFactorAuthentication.class);
        assertThat(pendingAuthentication.isAuthenticated()).isFalse();
        assertThat(request.getSession()
            .getAttribute(TwoFactorAuthenticationSupport.REMEMBER_ME_REQUESTED_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
    }

    @Test
    void testAttemptLimiterLocksAfterMaxFailuresAndUnlocksAfterTheWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-18T00:00:00Z"));
        TotpAttemptLimiter totpAttemptLimiter = new TotpAttemptLimiter(clock);

        for (int i = 0; i < TotpAttemptLimiter.MAX_FAILURES; i++) {
            assertThat(totpAttemptLimiter.isLocked("bob")).isFalse();

            totpAttemptLimiter.recordFailure("bob");
        }

        assertThat(totpAttemptLimiter.isLocked("bob")).isTrue();
        assertThat(totpAttemptLimiter.isLocked("alice")).isFalse();

        clock.instant = clock.instant.plus(TotpAttemptLimiter.LOCKOUT)
            .plusSeconds(1);

        assertThat(totpAttemptLimiter.isLocked("bob")).isFalse();
    }

    @Test
    void testAttemptLimiterResetsOnSuccess() {
        TotpAttemptLimiter totpAttemptLimiter = new TotpAttemptLimiter();

        totpAttemptLimiter.recordFailure("bob");
        totpAttemptLimiter.recordSuccess("bob");

        for (int i = 0; i < TotpAttemptLimiter.MAX_FAILURES - 1; i++) {
            totpAttemptLimiter.recordFailure("bob");
        }

        assertThat(totpAttemptLimiter.isLocked("bob")).isFalse();
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}

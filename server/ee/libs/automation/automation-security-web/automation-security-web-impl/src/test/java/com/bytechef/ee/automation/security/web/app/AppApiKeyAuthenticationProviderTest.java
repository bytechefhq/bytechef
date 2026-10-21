/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.security.web.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.security.AppPrincipal;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AppApiKeyAuthenticationProviderTest {

    @Mock
    private AppService appService;

    @Test
    void testAuthenticateUnknownKeyIsBadCredentials() {
        when(appService.fetchAppBySecretKey("k")).thenReturn(Optional.empty());

        AppApiKeyAuthenticationProvider provider = new AppApiKeyAuthenticationProvider(appService);

        assertThatThrownBy(() -> provider.authenticate(new AppAuthenticationToken("k", "public")))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testAuthenticateDisabledAppIsDisabledException() {
        when(appService.fetchAppBySecretKey("k")).thenReturn(Optional.of(app(AppStatus.DISABLED)));

        AppApiKeyAuthenticationProvider provider = new AppApiKeyAuthenticationProvider(appService);

        assertThatThrownBy(() -> provider.authenticate(new AppAuthenticationToken("k", "public")))
            .isInstanceOf(DisabledException.class);

        verify(appService, never()).touchSecretKeyLastUsedDate(1L);
    }

    @Test
    void testAuthenticateEnabledAppYieldsPrincipalWithEnvironmentFromTheRow() {
        when(appService.fetchAppBySecretKey("k")).thenReturn(Optional.of(app(AppStatus.ENABLED)));

        Authentication authentication = Objects.requireNonNull(
            new AppApiKeyAuthenticationProvider(appService).authenticate(new AppAuthenticationToken("k", "tenant9")));

        AppPrincipal principal = (AppPrincipal) authentication.getPrincipal();

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(principal.tenantId()).isEqualTo("tenant9");
        assertThat(principal.appId()).isEqualTo(1L);
        assertThat(principal.workspaceId()).isEqualTo(7L);
        assertThat(principal.environment()).isEqualTo(Environment.STAGING);
        assertThat(authentication.getAuthorities()).extracting(Object::toString)
            .containsExactly("ROLE_APP");
        assertThat(((AbstractApiKeyAuthenticationToken) authentication).getEnvironmentId())
            .isEqualTo((long) Environment.STAGING.ordinal());

        verify(appService).touchSecretKeyLastUsedDate(1L);
    }

    @Test
    void testAuthenticateAppWithoutWorkspaceIsBadCredentials() {
        App app = app(AppStatus.ENABLED);

        app.setWorkspaceId(null);

        when(appService.fetchAppBySecretKey("k")).thenReturn(Optional.of(app));

        AppApiKeyAuthenticationProvider provider = new AppApiKeyAuthenticationProvider(appService);

        assertThatThrownBy(() -> provider.authenticate(new AppAuthenticationToken("k", "public")))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testSupports() {
        AppApiKeyAuthenticationProvider provider = new AppApiKeyAuthenticationProvider(appService);

        assertThat(provider.supports(AppAuthenticationToken.class)).isTrue();
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class)).isFalse();
    }

    private static App app(AppStatus status) {
        App app = new App();

        app.setId(1L);
        app.setWorkspaceId(7L);
        app.setEnvironment(Environment.STAGING);
        app.setStatus(status);

        return app;
    }
}

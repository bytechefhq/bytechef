/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.security.web.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.security.AppPrincipal;
import com.bytechef.ee.automation.app.security.AppUser;
import com.bytechef.ee.automation.app.security.AppUserAuthenticationException;
import com.bytechef.ee.automation.app.security.AppUserAuthenticator;
import com.bytechef.ee.automation.app.service.AppInstanceService;
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
    private AppInstanceService appInstanceService;

    @Mock
    private AppService appService;

    @Mock
    private AppUserAuthenticator appUserAuthenticator;

    @Test
    void testAuthenticateUnknownKeyIsBadCredentials() {
        when(appInstanceService.fetchAppInstanceBySecretKey("k")).thenReturn(Optional.empty());

        AppApiKeyAuthenticationProvider provider =
            new AppApiKeyAuthenticationProvider(appInstanceService, appService, appUserAuthenticator);

        assertThatThrownBy(() -> provider.authenticate(new AppAuthenticationToken("k", "public")))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testAuthenticateSuspendedAppIsDisabledException() {
        when(appInstanceService.fetchAppInstanceBySecretKey("k"))
            .thenReturn(Optional.of(appInstance(AppInstanceStatus.ACTIVE)));
        when(appService.fetchApp(1L)).thenReturn(Optional.of(app(AppStatus.SUSPENDED)));

        AppApiKeyAuthenticationProvider provider =
            new AppApiKeyAuthenticationProvider(appInstanceService, appService, appUserAuthenticator);

        assertThatThrownBy(() -> provider.authenticate(new AppAuthenticationToken("k", "public")))
            .isInstanceOf(DisabledException.class);

        verify(appInstanceService, never()).touchSecretKeyLastUsedDate(anyLong());
    }

    @Test
    void testAuthenticateSuspendedInstanceIsDisabledException() {
        when(appInstanceService.fetchAppInstanceBySecretKey("k"))
            .thenReturn(Optional.of(appInstance(AppInstanceStatus.SUSPENDED)));
        when(appService.fetchApp(1L)).thenReturn(Optional.of(app(AppStatus.ACTIVE)));

        AppApiKeyAuthenticationProvider provider =
            new AppApiKeyAuthenticationProvider(appInstanceService, appService, appUserAuthenticator);

        assertThatThrownBy(() -> provider.authenticate(new AppAuthenticationToken("k", "public")))
            .isInstanceOf(DisabledException.class);

        verify(appInstanceService, never()).touchSecretKeyLastUsedDate(anyLong());
    }

    @Test
    void testAuthenticateActiveInstanceOfADraftAppYieldsPrincipalWithTheInstanceEnvironment() {
        when(appInstanceService.fetchAppInstanceBySecretKey("k"))
            .thenReturn(Optional.of(appInstance(AppInstanceStatus.ACTIVE)));
        when(appService.fetchApp(1L)).thenReturn(Optional.of(app(AppStatus.DRAFT)));

        Authentication authentication = Objects.requireNonNull(
            new AppApiKeyAuthenticationProvider(appInstanceService, appService, appUserAuthenticator).authenticate(
                new AppAuthenticationToken("k", "tenant9")));

        AppPrincipal principal = (AppPrincipal) authentication.getPrincipal();

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(principal.tenantId()).isEqualTo("tenant9");
        assertThat(principal.appId()).isEqualTo(1L);
        assertThat(principal.appInstanceId()).isEqualTo(11L);
        assertThat(principal.workspaceId()).isEqualTo(7L);
        assertThat(principal.environment()).isEqualTo(Environment.STAGING);
        assertThat(authentication.getAuthorities()).extracting(Object::toString)
            .containsExactly("ROLE_APP");
        assertThat(((AbstractApiKeyAuthenticationToken) authentication).getEnvironmentId())
            .isEqualTo((long) Environment.STAGING.ordinal());

        verify(appInstanceService).touchSecretKeyLastUsedDate(11L);
    }

    @Test
    void testAuthenticateInstanceWithoutWorkspaceIsBadCredentials() {
        AppInstance appInstance = appInstance(AppInstanceStatus.ACTIVE);

        appInstance.setWorkspaceId(null);

        when(appInstanceService.fetchAppInstanceBySecretKey("k")).thenReturn(Optional.of(appInstance));
        when(appService.fetchApp(1L)).thenReturn(Optional.of(app(AppStatus.ACTIVE)));

        AppApiKeyAuthenticationProvider provider =
            new AppApiKeyAuthenticationProvider(appInstanceService, appService, appUserAuthenticator);

        assertThatThrownBy(() -> provider.authenticate(new AppAuthenticationToken("k", "public")))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testUserTokenIsVerifiedInTheKeyTenantAndPutsTheUserOnThePrincipal() {
        AppInstance appInstance = appInstance(AppInstanceStatus.ACTIVE);

        appInstance.setUserAuthMode(AppUserAuthMode.OPTIONAL);

        when(appInstanceService.fetchAppInstanceBySecretKey("k")).thenReturn(Optional.of(appInstance));
        when(appService.fetchApp(1L)).thenReturn(Optional.of(app(AppStatus.ACTIVE)));
        when(appUserAuthenticator.authenticate(appInstance, "tenant9", "user-token"))
            .thenReturn(new AppUser(5L, "member"));

        Authentication authentication = Objects.requireNonNull(
            createProvider().authenticate(new AppAuthenticationToken("k", "tenant9", "user-token", false)));

        AppPrincipal principal = (AppPrincipal) authentication.getPrincipal();

        assertThat(principal.user()).isEqualTo(new AppUser(5L, "member"));
        assertThat(principal.userId()).isEqualTo(5L);
    }

    @Test
    void testRejectedUserTokenIsNeverDowngradedToNoUser() {
        AppInstance appInstance = appInstance(AppInstanceStatus.ACTIVE);

        appInstance.setUserAuthMode(AppUserAuthMode.OPTIONAL);

        AppUserAuthenticationException invalidToken = new AppUserAuthenticationException(
            AppErrorType.APP_USER_TOKEN_INVALID, "The user token is invalid");

        when(appInstanceService.fetchAppInstanceBySecretKey("k")).thenReturn(Optional.of(appInstance));
        when(appService.fetchApp(1L)).thenReturn(Optional.of(app(AppStatus.ACTIVE)));
        when(appUserAuthenticator.authenticate(appInstance, "public", "bad")).thenThrow(invalidToken);

        AppApiKeyAuthenticationProvider provider = createProvider();

        assertThatThrownBy(() -> provider.authenticate(new AppAuthenticationToken("k", "public", "bad", false)))
            .isSameAs(invalidToken);
    }

    @Test
    void testRequiredInstanceWithoutUserTokenIsUserRequired() {
        AppInstance appInstance = appInstance(AppInstanceStatus.ACTIVE);

        appInstance.setUserAuthMode(AppUserAuthMode.REQUIRED);

        when(appInstanceService.fetchAppInstanceBySecretKey("k")).thenReturn(Optional.of(appInstance));
        when(appService.fetchApp(1L)).thenReturn(Optional.of(app(AppStatus.ACTIVE)));

        AppApiKeyAuthenticationProvider provider = createProvider();

        assertThatThrownBy(() -> provider.authenticate(new AppAuthenticationToken("k", "public")))
            .isInstanceOfSatisfying(
                AppUserAuthenticationException.class,
                exception -> assertThat(exception.getErrorType()).isEqualTo(AppErrorType.APP_USER_REQUIRED));
    }

    @Test
    void testRequiredInstanceAnswersAUserOptionalRequestWithoutUser() {
        AppInstance appInstance = appInstance(AppInstanceStatus.ACTIVE);

        appInstance.setUserAuthMode(AppUserAuthMode.REQUIRED);

        when(appInstanceService.fetchAppInstanceBySecretKey("k")).thenReturn(Optional.of(appInstance));
        when(appService.fetchApp(1L)).thenReturn(Optional.of(app(AppStatus.ACTIVE)));

        Authentication authentication = Objects.requireNonNull(
            createProvider().authenticate(new AppAuthenticationToken("k", "public", null, true)));

        assertThat(((AppPrincipal) authentication.getPrincipal()).user()).isNull();
    }

    @Test
    void testOptionalInstanceWithoutUserTokenHasNoUser() {
        AppInstance appInstance = appInstance(AppInstanceStatus.ACTIVE);

        appInstance.setUserAuthMode(AppUserAuthMode.OPTIONAL);

        when(appInstanceService.fetchAppInstanceBySecretKey("k")).thenReturn(Optional.of(appInstance));
        when(appService.fetchApp(1L)).thenReturn(Optional.of(app(AppStatus.ACTIVE)));

        Authentication authentication = Objects.requireNonNull(
            createProvider().authenticate(new AppAuthenticationToken("k", "public")));

        assertThat(((AppPrincipal) authentication.getPrincipal()).user()).isNull();
    }

    @Test
    void testSupports() {
        AppApiKeyAuthenticationProvider provider =
            new AppApiKeyAuthenticationProvider(appInstanceService, appService, appUserAuthenticator);

        assertThat(provider.supports(AppAuthenticationToken.class)).isTrue();
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class)).isFalse();
    }

    private AppApiKeyAuthenticationProvider createProvider() {
        return new AppApiKeyAuthenticationProvider(appInstanceService, appService, appUserAuthenticator);
    }

    private static App app(AppStatus status) {
        App app = new App();

        app.setId(1L);
        app.setWorkspaceId(7L);
        app.setStatus(status);

        return app;
    }

    private static AppInstance appInstance(AppInstanceStatus status) {
        AppInstance appInstance = new AppInstance();

        appInstance.setId(11L);
        appInstance.setAppId(1L);
        appInstance.setWorkspaceId(7L);
        appInstance.setEnvironment(Environment.STAGING);
        appInstance.setStatus(status);

        return appInstance;
    }
}

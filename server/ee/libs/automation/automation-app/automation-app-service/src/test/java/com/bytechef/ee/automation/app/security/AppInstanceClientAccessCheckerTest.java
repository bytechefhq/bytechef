/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AppInstanceClientAccessCheckerTest {

    private static final long APP_ID = 1L;
    private static final long APP_INSTANCE_ID = 11L;
    private static final String CLIENT_ID = "app-instance-uuid";
    private static final String LOGIN = "alice";

    @Mock
    private AppInstanceService appInstanceService;

    @Mock
    private AppService appService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private UserService userService;

    private final Authentication principal = UsernamePasswordAuthenticationToken.authenticated(LOGIN, null, List.of());

    private AppInstanceClientAccessChecker appInstanceClientAccessChecker;

    @BeforeEach
    void beforeEach() {
        appInstanceClientAccessChecker = new AppInstanceClientAccessChecker(
            appInstanceService, appService, permissionService, userService);

        lenient().when(appInstanceService.fetchAppInstanceByUuid("instance-uuid"))
            .thenReturn(Optional.of(newAppInstance(AppInstanceStatus.ACTIVE)));
        lenient().when(appService.getApp(APP_ID))
            .thenReturn(newApp(AppStatus.ACTIVE));
        lenient().when(userService.fetchUserByLogin(LOGIN))
            .thenReturn(Optional.of(newUser(true)));
        lenient().when(permissionService.hasResourceScope(APP_INSTANCE_ID, "AppInstance", "APP_USE"))
            .thenReturn(true);
    }

    @Test
    void testSupportsEveryAppClientAndNoOtherClient() {
        assertThat(appInstanceClientAccessChecker.supports(newRegisteredClient(CLIENT_ID, "app:use"))).isTrue();
        assertThat(appInstanceClientAccessChecker.supports(newRegisteredClient("app-unknown", "app:use"))).isTrue();
        assertThat(appInstanceClientAccessChecker.supports(newRegisteredClient("claude", "mcp:automation")))
            .isFalse();
    }

    @Test
    void testMemberWithAppUseOnAnActiveInstanceIsAllowed() {
        assertThat(appInstanceClientAccessChecker.isAllowed(newRegisteredClient(CLIENT_ID, "app:use"), principal))
            .isTrue();

        verify(permissionService).hasResourceScope(APP_INSTANCE_ID, "AppInstance", "APP_USE");
    }

    @Test
    void testUserWithoutAppUseGetsAccessDenied() {
        when(permissionService.hasResourceScope(APP_INSTANCE_ID, "AppInstance", "APP_USE")).thenReturn(false);

        assertThat(appInstanceClientAccessChecker.isAllowed(newRegisteredClient(CLIENT_ID, "app:use"), principal))
            .isFalse();
    }

    @Test
    void testAppClientThatResolvesToNoInstanceIsDenied() {
        when(appInstanceService.fetchAppInstanceByUuid("missing")).thenReturn(Optional.empty());

        assertThat(appInstanceClientAccessChecker.isAllowed(newRegisteredClient("app-missing", "app:use"), principal))
            .isFalse();
        assertThat(appInstanceClientAccessChecker.isAllowed(newRegisteredClient("other-client", "app:use"), principal))
            .isFalse();

        verify(permissionService, never()).hasResourceScope(any(), anyString(), anyString());
    }

    @Test
    void testClientThatIsNotTheInstancesRegisteredClientIsDenied() {
        AppInstance appInstance = newAppInstance(AppInstanceStatus.ACTIVE);

        appInstance.setRegisteredClientId(null);

        when(appInstanceService.fetchAppInstanceByUuid("instance-uuid")).thenReturn(Optional.of(appInstance));

        assertThat(appInstanceClientAccessChecker.isAllowed(newRegisteredClient(CLIENT_ID, "app:use"), principal))
            .isFalse();
    }

    @Test
    void testInstanceWithoutSignInIsDenied() {
        AppInstance appInstance = newAppInstance(AppInstanceStatus.ACTIVE);

        appInstance.setUserAuthMode(AppUserAuthMode.NONE);

        when(appInstanceService.fetchAppInstanceByUuid("instance-uuid")).thenReturn(Optional.of(appInstance));

        assertThat(appInstanceClientAccessChecker.isAllowed(newRegisteredClient(CLIENT_ID, "app:use"), principal))
            .isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "DRAFT, ACTIVE, true",
        "ACTIVE, ACTIVE, true",
        "SUSPENDED, ACTIVE, false",
        "ARCHIVED, ACTIVE, false",
        "ACTIVE, SUSPENDED, false",
        "ACTIVE, TERMINATED, false"
    })
    void testAppAndInstanceStatusDecide(AppStatus appStatus, AppInstanceStatus appInstanceStatus, boolean allowed) {
        when(appInstanceService.fetchAppInstanceByUuid("instance-uuid"))
            .thenReturn(Optional.of(newAppInstance(appInstanceStatus)));
        when(appService.getApp(APP_ID)).thenReturn(newApp(appStatus));

        assertThat(appInstanceClientAccessChecker.isAllowed(newRegisteredClient(CLIENT_ID, "app:use"), principal))
            .isEqualTo(allowed);
    }

    @Test
    void testDeactivatedUserIsDenied() {
        when(userService.fetchUserByLogin(LOGIN)).thenReturn(Optional.of(newUser(false)));

        assertThat(appInstanceClientAccessChecker.isAllowed(newRegisteredClient(CLIENT_ID, "app:use"), principal))
            .isFalse();

        verify(permissionService, never()).hasResourceScope(any(), anyString(), anyString());
    }

    @Test
    void testUnknownUserIsDenied() {
        when(userService.fetchUserByLogin(LOGIN)).thenReturn(Optional.empty());

        assertThat(appInstanceClientAccessChecker.isAllowed(newRegisteredClient(CLIENT_ID, "app:use"), principal))
            .isFalse();
    }

    private static App newApp(AppStatus appStatus) {
        App app = new App();

        app.setId(APP_ID);
        app.setStatus(appStatus);

        return app;
    }

    private static AppInstance newAppInstance(AppInstanceStatus appInstanceStatus) {
        AppInstance appInstance = new AppInstance();

        appInstance.setId(APP_INSTANCE_ID);
        appInstance.setAppId(APP_ID);
        appInstance.setUuid("instance-uuid");
        appInstance.setStatus(appInstanceStatus);
        appInstance.setUserAuthMode(AppUserAuthMode.REQUIRED);
        appInstance.setRegisteredClientId(CLIENT_ID);

        return appInstance;
    }

    private static RegisteredClient newRegisteredClient(String clientId, String scope) {
        return RegisteredClient.withId("id-" + clientId)
            .clientId(clientId)
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("https://crm.example.com/auth/callback")
            .scope(scope)
            .build();
    }

    private static User newUser(boolean activated) {
        User user = new User();

        user.setId(5L);
        user.setLogin(LOGIN);
        user.setActivated(activated);

        return user;
    }
}

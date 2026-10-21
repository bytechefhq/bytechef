/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.APP;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.APP_INSTANCE;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.EDITOR;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.PRIVATE_APP_INSTANCE;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.TENANT_ADMIN;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.VIEWER;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.WORKSPACE_A;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.WORKSPACE_ADMIN;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.authenticate;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.newAppInstance;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.userId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.user.service.UserService;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs the {@code @PreAuthorize} guards on {@link AppSharingFacadeImpl} through real Spring method security and the EE
 * {@code PermissionServiceImpl} (see {@link AppAuthorizationTestConfiguration}). Every denial is paired with an
 * allowance on the same guard, and every refused call also checks that the facade body never ran.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AppAuthorizationTestConfiguration.class, properties = "bytechef.edition=ee")
class AppSharingFacadeAuthorizationTest {

    private static final String APP_EXPRESSION = "@permissionService.isResourceOwner('App', #appId) || " +
        "hasPermission(#appId, 'App', 'APP_MANAGE')";
    private static final String APP_INSTANCE_EXPRESSION =
        "@permissionService.isResourceOwner('AppInstance', #appInstanceId) || " +
            "hasPermission(#appInstanceId, 'AppInstance', 'APP_MANAGE')";

    private static final long EDITOR_OWNED_APP_INSTANCE = 40L;

    @Autowired
    private AppInstanceService appInstanceService;

    @Autowired
    private AppService appService;

    @Autowired
    private AppSharingFacade appSharingFacade;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private ResourceGrantService resourceGrantService;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkspaceConnectionRepository workspaceConnectionRepository;

    @Autowired
    private WorkspaceUserRepository workspaceUserRepository;

    @Autowired
    private WorkspaceUserService workspaceUserService;

    @BeforeEach
    void beforeEach() {
        AppAuthorizationFixture.stubTenant(
            appInstanceService, appService, connectionService, userService, workspaceConnectionRepository,
            workspaceUserRepository);
        AppAuthorizationFixture.stubMembership(workspaceUserService);
        AppAuthorizationFixture.stubGrants(resourceGrantService);

        AppInstance editorOwnedAppInstance = newAppInstance(EDITOR_OWNED_APP_INSTANCE, APP, WORKSPACE_A, EDITOR);

        when(appInstanceService.fetchAppInstance(EDITOR_OWNED_APP_INSTANCE))
            .thenReturn(Optional.of(editorOwnedAppInstance));
        when(appInstanceService.getAppInstance(EDITOR_OWNED_APP_INSTANCE)).thenReturn(editorOwnedAppInstance);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "getAppGrants", "grantAppAccess", "revokeAppAccess", "setAppVisibility", "getAppInstanceGrants",
        "grantAppInstanceAccess", "revokeAppInstanceAccess", "setAppInstanceVisibility"
    })
    void testOwnerOrAppManageHolderMayShare(String methodName) {
        PreAuthorize preAuthorize = findMethod(methodName).getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("Method '%s' must carry @PreAuthorize", methodName)
            .isNotNull();
        assertThat(preAuthorize.value())
            .as("Method '%s' must be owner or APP_MANAGE holder", methodName)
            .isEqualTo(methodName.contains("AppInstance") ? APP_INSTANCE_EXPRESSION : APP_EXPRESSION);
    }

    @Test
    void testEverySharingMethodIsGuarded() {
        List<String> guardedMethodNames = Arrays.stream(AppSharingFacadeImpl.class.getDeclaredMethods())
            .filter(method -> method.getAnnotation(PreAuthorize.class) != null)
            .map(Method::getName)
            .distinct()
            .sorted()
            .toList();

        assertThat(guardedMethodNames).containsExactly(
            "getAppGrants", "getAppInstanceGrants", "grantAppAccess", "grantAppInstanceAccess", "revokeAppAccess",
            "revokeAppInstanceAccess", "setAppInstanceVisibility", "setAppVisibility");
    }

    @Test
    void testOwnerWithoutAppManageSharesTheirInstance() {
        authenticate(EDITOR);

        appSharingFacade.setAppInstanceVisibility(
            WORKSPACE_A, EDITOR_OWNED_APP_INSTANCE, ResourceVisibility.PRIVATE);
        appSharingFacade.grantAppInstanceAccess(WORKSPACE_A, EDITOR_OWNED_APP_INSTANCE, userId(VIEWER));

        assertThat(appSharingFacade.getAppInstanceGrants(WORKSPACE_A, EDITOR_OWNED_APP_INSTANCE))
            .containsExactly(userId(VIEWER));

        verify(appInstanceService).updateVisibility(EDITOR_OWNED_APP_INSTANCE, ResourceVisibility.PRIVATE);
    }

    @Test
    void testWorkspaceAdminWhoIsNotOwnerSharesWorkspaceVisibleInstance() {
        authenticate(WORKSPACE_ADMIN);

        appSharingFacade.setAppInstanceVisibility(
            WORKSPACE_A, EDITOR_OWNED_APP_INSTANCE, ResourceVisibility.PRIVATE);
        appSharingFacade.grantAppInstanceAccess(WORKSPACE_A, EDITOR_OWNED_APP_INSTANCE, userId(VIEWER));
        appSharingFacade.revokeAppInstanceAccess(WORKSPACE_A, EDITOR_OWNED_APP_INSTANCE, userId(VIEWER));

        assertThat(appSharingFacade.getAppInstanceGrants(WORKSPACE_A, EDITOR_OWNED_APP_INSTANCE)).isEmpty();

        verify(appInstanceService).updateVisibility(EDITOR_OWNED_APP_INSTANCE, ResourceVisibility.PRIVATE);
        verify(resourceGrantService).grant("AppInstance", EDITOR_OWNED_APP_INSTANCE, userId(VIEWER));
        verify(resourceGrantService).revoke("AppInstance", EDITOR_OWNED_APP_INSTANCE, userId(VIEWER));
    }

    @Test
    void testEditorWhoIsNotOwnerCannotShare() {
        authenticate(EDITOR);

        assertThatThrownBy(
            () -> appSharingFacade.setAppInstanceVisibility(WORKSPACE_A, APP_INSTANCE, ResourceVisibility.PRIVATE))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> appSharingFacade.grantAppInstanceAccess(WORKSPACE_A, APP_INSTANCE, userId(VIEWER)))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> appSharingFacade.revokeAppInstanceAccess(WORKSPACE_A, APP_INSTANCE, userId(VIEWER)))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> appSharingFacade.getAppInstanceGrants(WORKSPACE_A, APP_INSTANCE))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> appSharingFacade.setAppVisibility(WORKSPACE_A, APP, ResourceVisibility.PRIVATE))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> appSharingFacade.grantAppAccess(WORKSPACE_A, APP, userId(VIEWER)))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> appSharingFacade.revokeAppAccess(WORKSPACE_A, APP, userId(VIEWER)))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> appSharingFacade.getAppGrants(WORKSPACE_A, APP))
            .isInstanceOf(AccessDeniedException.class);

        verify(appInstanceService, never()).updateVisibility(anyLong(), any());
        verify(appService, never()).updateVisibility(anyLong(), any());
        verify(resourceGrantService, never()).grant(anyString(), anyLong(), anyLong());
        verify(resourceGrantService, never()).revoke(anyString(), anyLong(), anyLong());
        verify(resourceGrantService, never()).getGrantedUserIds(anyString(), anyLong());

        authenticate(WORKSPACE_ADMIN);

        appSharingFacade.setAppVisibility(WORKSPACE_A, APP, ResourceVisibility.PRIVATE);

        verify(appService).updateVisibility(APP, ResourceVisibility.PRIVATE);
    }

    @Test
    void testPlainUserCannotListGrants() {
        authenticate(WORKSPACE_ADMIN);

        appSharingFacade.grantAppInstanceAccess(WORKSPACE_A, PRIVATE_APP_INSTANCE, userId(VIEWER));

        assertThat(appSharingFacade.getAppInstanceGrants(WORKSPACE_A, PRIVATE_APP_INSTANCE))
            .containsExactly(userId(VIEWER));

        authenticate(VIEWER);

        assertThatThrownBy(() -> appSharingFacade.getAppInstanceGrants(WORKSPACE_A, PRIVATE_APP_INSTANCE))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> appSharingFacade.getAppInstanceGrants(WORKSPACE_A, APP_INSTANCE))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> appSharingFacade.getAppGrants(WORKSPACE_A, APP))
            .isInstanceOf(AccessDeniedException.class);

        authenticate(TENANT_ADMIN);

        assertThat(appSharingFacade.getAppInstanceGrants(WORKSPACE_A, PRIVATE_APP_INSTANCE))
            .containsExactly(userId(VIEWER));
    }

    private static Method findMethod(String methodName) {
        return Arrays.stream(AppSharingFacadeImpl.class.getDeclaredMethods())
            .filter(method -> method.getName()
                .equals(methodName))
            .filter(method -> !method.isSynthetic())
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected '" + methodName + "' on AppSharingFacadeImpl"));
    }
}

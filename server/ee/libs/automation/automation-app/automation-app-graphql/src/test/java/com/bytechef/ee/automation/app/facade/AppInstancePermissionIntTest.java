/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.APP;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.APP_INSTANCE;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.APP_OWNER;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.FOREIGN_APP_INSTANCE;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.OUTSIDER;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.PRODUCTION_ONLY_VIEWER;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.TENANT_ADMIN;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.TWO_WORKSPACE_ADMIN;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.VIEWER;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.WORKSPACE_A;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.WORKSPACE_ADMIN;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.WORKSPACE_B;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.authenticate;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.newApp;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.newAppInstance;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.userId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.dto.AppDTO;
import com.bytechef.ee.automation.app.dto.AppInstanceDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Pins what a grant on an app instance does against the real EE {@code PermissionServiceImpl}, the EE visibility
 * resolver and the production AppInstance resolvers (see {@link AppAuthorizationTestConfiguration}): visibility is a
 * precondition of {@code hasResourceScope}, a grant restores it for one member and no more, the role in the instance's
 * workspace and environment still decides the scope, and the bulk filter of {@link AppFacade#getApp} agrees with the
 * per-id check.
 *
 * <p>
 * {@link #CROSS_WORKSPACE_APP} belongs to workspace A and its owner {@link AppAuthorizationFixture#APP_OWNER} is not a
 * member of workspace B, where {@link AppAuthorizationFixture#OUTSIDER}, B's admin, created
 * {@link #CROSS_WORKSPACE_APP_INSTANCE}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AppAuthorizationTestConfiguration.class, properties = "bytechef.edition=ee")
class AppInstancePermissionIntTest {

    private static final String APP_INSTANCE_RESOURCE = "AppInstance";
    private static final String APP_USE = "APP_USE";
    private static final String APP_VIEW = "APP_VIEW";

    private static final long CROSS_WORKSPACE_APP = 12L;
    private static final long CROSS_WORKSPACE_APP_INSTANCE = 35L;
    private static final long DEVELOPMENT_APP_INSTANCE = 36L;
    private static final long PRIVATE_INSTANCE_OF_APP = 37L;

    @Autowired
    private AppFacade appFacade;

    @Autowired
    private AppInstanceFacade appInstanceFacade;

    @Autowired
    private AppInstanceService appInstanceService;

    @Autowired
    private AppService appService;

    @Autowired
    private AppSharingFacade appSharingFacade;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private PermissionService permissionService;

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

        AppInstance privateInstanceOfApp = newAppInstance(PRIVATE_INSTANCE_OF_APP, APP, WORKSPACE_A, WORKSPACE_ADMIN);

        privateInstanceOfApp.setName("Croatia");
        privateInstanceOfApp.setVisibility(ResourceVisibility.PRIVATE);

        AppInstance developmentAppInstance =
            newAppInstance(DEVELOPMENT_APP_INSTANCE, APP, WORKSPACE_A, WORKSPACE_ADMIN);

        developmentAppInstance.setName("Development");
        developmentAppInstance.setEnvironment(Environment.DEVELOPMENT);

        AppInstance crossWorkspaceAppInstance = newAppInstance(
            CROSS_WORKSPACE_APP_INSTANCE, CROSS_WORKSPACE_APP, WORKSPACE_B, OUTSIDER);

        for (AppInstance appInstance : List.of(
            privateInstanceOfApp, developmentAppInstance, crossWorkspaceAppInstance)) {

            when(appInstanceService.fetchAppInstance(appInstance.getId())).thenReturn(Optional.of(appInstance));
            when(appInstanceService.getAppInstance(appInstance.getId())).thenReturn(appInstance);
        }

        List<AppInstance> appInstances = List.of(
            appInstanceService.getAppInstance(APP_INSTANCE), privateInstanceOfApp);

        when(appInstanceService.getAppInstances(APP)).thenReturn(appInstances);

        App crossWorkspaceApp = newApp(CROSS_WORKSPACE_APP, ResourceVisibility.WORKSPACE);

        crossWorkspaceApp.setOwnerUserId(userId(APP_OWNER));
        crossWorkspaceApp.setCreatedBy(APP_OWNER);

        when(appService.fetchApp(CROSS_WORKSPACE_APP)).thenReturn(Optional.of(crossWorkspaceApp));
        when(appService.getApp(CROSS_WORKSPACE_APP)).thenReturn(crossWorkspaceApp);
        when(appInstanceService.getAppInstances(CROSS_WORKSPACE_APP)).thenReturn(List.of(crossWorkspaceAppInstance));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testMemberWithAppUseIsRefusedPrivateInstanceWithoutGrant() {
        authenticate(VIEWER);

        assertThat(permissionService.hasResourceScope(APP_INSTANCE, APP_INSTANCE_RESOURCE, APP_USE))
            .as("the viewer holds APP_USE in A's PRODUCTION environment")
            .isTrue();
        assertThat(permissionService.hasResourceScope(PRIVATE_INSTANCE_OF_APP, APP_INSTANCE_RESOURCE, APP_USE))
            .as("the instance is the workspace admin's and PRIVATE, and nobody granted it")
            .isFalse();
        assertThat(permissionService.hasResourceScope(PRIVATE_INSTANCE_OF_APP, APP_INSTANCE_RESOURCE, APP_VIEW))
            .isFalse();
        assertThat(instanceIds(appFacade.getApp(APP))).containsExactly(APP_INSTANCE);
    }

    @Test
    void testGrantMakesPrivateInstanceUsable() {
        authenticate(WORKSPACE_ADMIN);

        appSharingFacade.grantAppInstanceAccess(WORKSPACE_A, PRIVATE_INSTANCE_OF_APP, userId(VIEWER));

        authenticate(VIEWER);

        assertThat(permissionService.hasResourceScope(PRIVATE_INSTANCE_OF_APP, APP_INSTANCE_RESOURCE, APP_USE))
            .isTrue();
        assertThat(permissionService.hasResourceScope(PRIVATE_INSTANCE_OF_APP, APP_INSTANCE_RESOURCE, APP_VIEW))
            .isTrue();
        assertThat(instanceIds(appFacade.getApp(APP)))
            .as("the bulk filter of getApp agrees with the per-id check for a granted PRIVATE instance")
            .containsExactly(APP_INSTANCE, PRIVATE_INSTANCE_OF_APP);

        authenticate(WORKSPACE_ADMIN);

        appSharingFacade.revokeAppInstanceAccess(WORKSPACE_A, PRIVATE_INSTANCE_OF_APP, userId(VIEWER));

        authenticate(VIEWER);

        assertThat(permissionService.hasResourceScope(PRIVATE_INSTANCE_OF_APP, APP_INSTANCE_RESOURCE, APP_USE))
            .isFalse();
        assertThat(instanceIds(appFacade.getApp(APP))).containsExactly(APP_INSTANCE);
    }

    @Test
    void testTargetWorkspaceAdminWhoCreatedForeignInstanceCanShareIt() {
        authenticate(OUTSIDER);

        assertThat(permissionService.isResourceOwner(APP_INSTANCE_RESOURCE, CROSS_WORKSPACE_APP_INSTANCE)).isTrue();

        appSharingFacade.setAppInstanceVisibility(
            WORKSPACE_B, CROSS_WORKSPACE_APP_INSTANCE, ResourceVisibility.PRIVATE);
        appSharingFacade.grantAppInstanceAccess(WORKSPACE_B, CROSS_WORKSPACE_APP_INSTANCE, userId(TWO_WORKSPACE_ADMIN));

        assertThat(appSharingFacade.getAppInstanceGrants(WORKSPACE_B, CROSS_WORKSPACE_APP_INSTANCE))
            .containsExactly(userId(TWO_WORKSPACE_ADMIN));

        verify(appInstanceService).updateVisibility(CROSS_WORKSPACE_APP_INSTANCE, ResourceVisibility.PRIVATE);
    }

    @Test
    void testAppOwnerWhoIsNotMemberOfInstanceWorkspaceCannotShareIt() {
        assertThat(workspaceUserRepository.existsByUserIdAndWorkspaceId(userId(APP_OWNER), WORKSPACE_B))
            .as("the app's owner has no workspace_user row in the instance's workspace")
            .isFalse();

        authenticate(APP_OWNER);

        assertThat(permissionService.isResourceOwner("App", CROSS_WORKSPACE_APP)).isTrue();
        assertThat(permissionService.isResourceOwner(APP_INSTANCE_RESOURCE, CROSS_WORKSPACE_APP_INSTANCE)).isFalse();
        assertThat(
            permissionService.hasResourceScope(CROSS_WORKSPACE_APP_INSTANCE, APP_INSTANCE_RESOURCE, "APP_MANAGE"))
                .isFalse();

        assertThatThrownBy(
            () -> appSharingFacade.setAppInstanceVisibility(
                WORKSPACE_B, CROSS_WORKSPACE_APP_INSTANCE, ResourceVisibility.PRIVATE))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> appSharingFacade.grantAppInstanceAccess(WORKSPACE_B, CROSS_WORKSPACE_APP_INSTANCE, userId(OUTSIDER)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> appSharingFacade.getAppInstanceGrants(WORKSPACE_B, CROSS_WORKSPACE_APP_INSTANCE))
            .isInstanceOf(AccessDeniedException.class);

        verify(appInstanceService, never()).updateVisibility(anyLong(), any());
        verify(resourceGrantService, never()).grant(anyString(), eq(CROSS_WORKSPACE_APP_INSTANCE), anyLong());

        appSharingFacade.setAppVisibility(WORKSPACE_A, CROSS_WORKSPACE_APP, ResourceVisibility.PRIVATE);

        verify(appService).updateVisibility(CROSS_WORKSPACE_APP, ResourceVisibility.PRIVATE);
    }

    @Test
    void testViewerInProductionOnlyIsRefusedDevelopmentInstance() {
        authenticate(PRODUCTION_ONLY_VIEWER);

        assertThat(permissionService.hasResourceScope(APP_INSTANCE, APP_INSTANCE_RESOURCE, APP_USE)).isTrue();
        assertThat(permissionService.hasResourceScope(DEVELOPMENT_APP_INSTANCE, APP_INSTANCE_RESOURCE, APP_USE))
            .as("the viewer's only role in A is in PRODUCTION")
            .isFalse();
        assertThat(permissionService.hasResourceScope(FOREIGN_APP_INSTANCE, APP_INSTANCE_RESOURCE, APP_USE))
            .isFalse();
    }

    @Test
    void testAfterOwnerLeftWorkspaceAdminManagesWorkspaceVisibleInstance() {
        ownerLeaves(WORKSPACE_ADMIN, WORKSPACE_A);

        authenticate(WORKSPACE_ADMIN);

        assertThat(permissionService.isResourceOwner(APP_INSTANCE_RESOURCE, APP_INSTANCE))
            .as("the departed member no longer counts as the owner")
            .isFalse();

        authenticate(TWO_WORKSPACE_ADMIN);

        assertThat(permissionService.isResourceOwner(APP_INSTANCE_RESOURCE, APP_INSTANCE)).isFalse();
        assertThat(permissionService.hasResourceScope(APP_INSTANCE, APP_INSTANCE_RESOURCE, "APP_MANAGE"))
            .as("a workspace ADMIN manages a WORKSPACE-visible instance whose owner left")
            .isTrue();

        appSharingFacade.setAppInstanceVisibility(WORKSPACE_A, APP_INSTANCE, ResourceVisibility.PRIVATE);
        appSharingFacade.grantAppInstanceAccess(WORKSPACE_A, APP_INSTANCE, userId(VIEWER));

        assertThat(appSharingFacade.getAppInstanceGrants(WORKSPACE_A, APP_INSTANCE)).containsExactly(userId(VIEWER));

        verify(appInstanceService).updateVisibility(APP_INSTANCE, ResourceVisibility.PRIVATE);
    }

    @Test
    void testAfterOwnerLeftPrivateInstanceIsManagedOnlyByTenantAdmin() {
        ownerLeaves(WORKSPACE_ADMIN, WORKSPACE_A);

        authenticate(TWO_WORKSPACE_ADMIN);

        assertThat(permissionService.hasResourceScope(PRIVATE_INSTANCE_OF_APP, APP_INSTANCE_RESOURCE, "APP_MANAGE"))
            .as("visibility is a precondition, so a workspace ADMIN does not pass on a PRIVATE instance")
            .isFalse();

        assertThatThrownBy(
            () -> appSharingFacade.setAppInstanceVisibility(
                WORKSPACE_A, PRIVATE_INSTANCE_OF_APP, ResourceVisibility.WORKSPACE))
                    .isInstanceOf(AccessDeniedException.class);

        authenticate(WORKSPACE_ADMIN);

        assertThat(permissionService.hasResourceScope(PRIVATE_INSTANCE_OF_APP, APP_INSTANCE_RESOURCE, "APP_MANAGE"))
            .isFalse();

        authenticate(TENANT_ADMIN);

        assertThat(permissionService.hasResourceScope(PRIVATE_INSTANCE_OF_APP, APP_INSTANCE_RESOURCE, "APP_MANAGE"))
            .isTrue();

        appSharingFacade.setAppInstanceVisibility(WORKSPACE_A, PRIVATE_INSTANCE_OF_APP, ResourceVisibility.WORKSPACE);

        verify(appInstanceService).updateVisibility(PRIVATE_INSTANCE_OF_APP, ResourceVisibility.WORKSPACE);
    }

    @Test
    void testAppOwnerLeavingAppWorkspaceLeavesForeignInstanceOwnerIntact() {
        ownerLeaves(APP_OWNER, WORKSPACE_A);

        authenticate(APP_OWNER);

        assertThat(permissionService.isResourceOwner("App", CROSS_WORKSPACE_APP))
            .as("the app's owner left the app's workspace")
            .isFalse();

        authenticate(OUTSIDER);

        assertThat(permissionService.isResourceOwner(APP_INSTANCE_RESOURCE, CROSS_WORKSPACE_APP_INSTANCE))
            .as("the instance has its own owner and workspace, both untouched")
            .isTrue();

        appSharingFacade.setAppInstanceVisibility(
            WORKSPACE_B, CROSS_WORKSPACE_APP_INSTANCE, ResourceVisibility.PRIVATE);

        verify(appInstanceService).updateVisibility(CROSS_WORKSPACE_APP_INSTANCE, ResourceVisibility.PRIVATE);
    }

    @Test
    void testInstanceOfAnotherWorkspaceAnswersLikeAnUnknownOne() {
        authenticate(OUTSIDER);

        ConfigurationException existingInstanceException = requireInEnvironmentFailure(APP_INSTANCE);

        when(appInstanceService.fetchAppInstance(APP_INSTANCE)).thenReturn(Optional.empty());

        ConfigurationException unknownInstanceException = requireInEnvironmentFailure(APP_INSTANCE);

        assertThat(existingInstanceException.getErrorKey())
            .isEqualTo(AppErrorType.APP_INSTANCE_NOT_FOUND.getErrorKey())
            .isEqualTo(unknownInstanceException.getErrorKey());
        assertThat(existingInstanceException.getMessage()).isEqualTo(unknownInstanceException.getMessage());
        assertThat(existingInstanceException.getEntityClass()).isEqualTo(unknownInstanceException.getEntityClass());
    }

    @Test
    void testPrivateInstanceWithoutGrantAnswersLikeAnUnknownOne() {
        authenticate(VIEWER);

        assertThat(requireInEnvironmentFailure(PRIVATE_INSTANCE_OF_APP).getErrorKey())
            .isEqualTo(AppErrorType.APP_INSTANCE_NOT_FOUND.getErrorKey());
    }

    @Test
    void testViewerWhoMayNotManagePassesTheExistenceCheckAndIsDeniedTheWrite() {
        authenticate(VIEWER);

        appInstanceFacade.requireAppInstanceInEnvironment(APP_INSTANCE, Environment.PRODUCTION);

        assertThatThrownBy(() -> appInstanceFacade.updateAppInstance(APP_INSTANCE, "Renamed", null, null))
            .isInstanceOf(AccessDeniedException.class);

        verify(appInstanceService, never()).update(anyLong(), any(), any(), any());
    }

    private ConfigurationException requireInEnvironmentFailure(long appInstanceId) {
        Throwable throwable = catchThrowable(
            () -> appInstanceFacade.requireAppInstanceInEnvironment(appInstanceId, Environment.PRODUCTION));

        assertThat(throwable).isInstanceOf(ConfigurationException.class);

        return (ConfigurationException) throwable;
    }

    private void ownerLeaves(String login, long workspaceId) {
        when(workspaceUserRepository.existsByUserIdAndWorkspaceId(userId(login), workspaceId)).thenReturn(false);
        when(workspaceUserService.isWorkspaceMember(userId(login), workspaceId)).thenReturn(false);
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(userId(login), workspaceId))
            .thenReturn(Optional.empty());
    }

    private static List<Long> instanceIds(AppDTO appDTO) {
        return appDTO.instances()
            .stream()
            .map(AppInstanceDTO::id)
            .toList();
    }
}

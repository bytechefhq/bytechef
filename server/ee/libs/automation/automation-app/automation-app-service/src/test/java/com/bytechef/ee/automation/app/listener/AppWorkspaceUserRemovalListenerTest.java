/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.event.WorkspaceUserRemovedEvent;
import com.bytechef.ee.automation.app.audit.AppAuditEvent;
import com.bytechef.ee.automation.app.audit.AppAuditPublisher;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.security.AppInstanceClients;
import com.bytechef.ee.automation.app.service.AppFavoriteService;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppInstanceUsageService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AppWorkspaceUserRemovalListenerTest {

    private static final long REMOVED_USER_ID = 7L;
    private static final long OTHER_USER_ID = 8L;
    private static final long WORKSPACE_ID = 42L;
    private static final long OTHER_WORKSPACE_ID = 43L;
    private static final long NON_MEMBER_WORKSPACE_ID = 44L;

    @Mock
    private AppAuditPublisher appAuditPublisher;

    @Mock
    private AppFavoriteService appFavoriteService;

    @Mock
    private AppInstanceClients appInstanceClients;

    @Mock
    private AppInstanceService appInstanceService;

    @Mock
    private AppInstanceUsageService appInstanceUsageService;

    @Mock
    private AppService appService;

    @Mock
    private ResourceGrantService resourceGrantService;

    @Mock
    private WorkspaceUserService workspaceUserService;

    @InjectMocks
    private AppWorkspaceUserRemovalListener listener;

    @Test
    void testRemovalRevokesAppAndInstanceGrantsOfThatWorkspaceOnly() {
        App app = newApp(100L, OTHER_USER_ID, ResourceVisibility.WORKSPACE);
        App otherApp = newApp(101L, OTHER_USER_ID, ResourceVisibility.PRIVATE);
        AppInstance instance = newAppInstance(200L, 100L, OTHER_USER_ID, ResourceVisibility.WORKSPACE);
        AppInstance foreignAppInstance = newAppInstance(201L, 300L, OTHER_USER_ID, ResourceVisibility.PRIVATE);

        when(appService.getApps(WORKSPACE_ID)).thenReturn(List.of(app, otherApp));
        stubWorkspaceAppInstances(Environment.PRODUCTION, List.of(instance, foreignAppInstance));

        listener
            .onWorkspaceUserRemoved(new WorkspaceUserRemovedEvent(WORKSPACE_ID, REMOVED_USER_ID, "gone@example.com"));

        verify(resourceGrantService).revokeUserGrants("App", REMOVED_USER_ID, List.of(100L, 101L));
        verify(resourceGrantService).revokeUserGrants("AppInstance", REMOVED_USER_ID, List.of(200L, 201L));
        verify(appService).getApps(WORKSPACE_ID);

        for (Environment environment : Environment.values()) {
            verify(appInstanceService).getWorkspaceAppInstances(WORKSPACE_ID, environment);
        }

        verify(appAuditPublisher, never()).publish(any(), anyLong(), any());
    }

    @Test
    void testRemovalDeletesTheUsersFavoritesAndUsageOfTheWorkspacesInstancesOnly() {
        AppInstance instance = newAppInstance(200L, 100L, OTHER_USER_ID, ResourceVisibility.WORKSPACE);
        AppInstance siblingInstance = newAppInstance(201L, 100L, OTHER_USER_ID, ResourceVisibility.WORKSPACE);
        AppInstance foreignAppInstance = newAppInstance(202L, 300L, OTHER_USER_ID, ResourceVisibility.PRIVATE);

        when(appService.getApps(WORKSPACE_ID))
            .thenReturn(List.of(newApp(101L, OTHER_USER_ID, ResourceVisibility.WORKSPACE)));
        stubWorkspaceAppInstances(Environment.PRODUCTION, List.of(instance, siblingInstance, foreignAppInstance));

        listener
            .onWorkspaceUserRemoved(new WorkspaceUserRemovedEvent(WORKSPACE_ID, REMOVED_USER_ID, "gone@example.com"));

        verify(appInstanceUsageService).deleteUsages(REMOVED_USER_ID, List.of(200L, 201L, 202L));
        verify(appFavoriteService).deleteFavorites(REMOVED_USER_ID, List.of(100L, 300L));
        verifyNoMoreInteractions(appFavoriteService, appInstanceUsageService);
    }

    @Test
    void testRemovalKeepsAFavoriteOfAnAppStillReachableThroughAnotherWorkspace() {
        AppInstance instance = newAppInstance(200L, 100L, OTHER_USER_ID, ResourceVisibility.WORKSPACE);
        AppInstance foreignAppInstance = newAppInstance(202L, 300L, OTHER_USER_ID, ResourceVisibility.WORKSPACE);
        AppInstance otherWorkspaceInstance = newAppInstance(210L, 100L, OTHER_USER_ID, ResourceVisibility.WORKSPACE);

        instance.setStatus(AppInstanceStatus.ACTIVE);
        foreignAppInstance.setStatus(AppInstanceStatus.ACTIVE);
        otherWorkspaceInstance.setStatus(AppInstanceStatus.ACTIVE);
        otherWorkspaceInstance.setWorkspaceId(OTHER_WORKSPACE_ID);

        when(appService.getApps(WORKSPACE_ID)).thenReturn(List.of());
        stubWorkspaceAppInstances(Environment.PRODUCTION, List.of(instance, foreignAppInstance));
        when(appInstanceService.getAppInstances(100L)).thenReturn(List.of(instance, otherWorkspaceInstance));
        when(appInstanceService.getAppInstances(300L)).thenReturn(List.of(foreignAppInstance));
        when(workspaceUserService.isWorkspaceMember(REMOVED_USER_ID, OTHER_WORKSPACE_ID)).thenReturn(true);

        listener
            .onWorkspaceUserRemoved(new WorkspaceUserRemovedEvent(WORKSPACE_ID, REMOVED_USER_ID, "gone@example.com"));

        verify(appInstanceUsageService).deleteUsages(REMOVED_USER_ID, List.of(200L, 202L));
        verify(appFavoriteService).deleteFavorites(REMOVED_USER_ID, List.of(300L));
        verify(workspaceUserService, never()).isWorkspaceMember(REMOVED_USER_ID, WORKSPACE_ID);
    }

    @Test
    void testRemovalDeletesAFavoriteWhenNoOtherWorkspaceOfTheUserHostsAnActiveInstanceOfTheApp() {
        AppInstance instance = newAppInstance(200L, 100L, OTHER_USER_ID, ResourceVisibility.WORKSPACE);
        AppInstance suspendedInstance = newAppInstance(210L, 100L, OTHER_USER_ID, ResourceVisibility.WORKSPACE);
        AppInstance nonMemberWorkspaceInstance = newAppInstance(
            211L, 100L, OTHER_USER_ID, ResourceVisibility.WORKSPACE);

        instance.setStatus(AppInstanceStatus.ACTIVE);
        suspendedInstance.setStatus(AppInstanceStatus.SUSPENDED);
        suspendedInstance.setWorkspaceId(OTHER_WORKSPACE_ID);
        nonMemberWorkspaceInstance.setStatus(AppInstanceStatus.ACTIVE);
        nonMemberWorkspaceInstance.setWorkspaceId(NON_MEMBER_WORKSPACE_ID);

        when(appService.getApps(WORKSPACE_ID)).thenReturn(List.of());
        stubWorkspaceAppInstances(Environment.PRODUCTION, List.of(instance));
        when(appInstanceService.getAppInstances(100L))
            .thenReturn(List.of(instance, suspendedInstance, nonMemberWorkspaceInstance));
        lenient().when(workspaceUserService.isWorkspaceMember(REMOVED_USER_ID, OTHER_WORKSPACE_ID))
            .thenReturn(true);
        when(workspaceUserService.isWorkspaceMember(REMOVED_USER_ID, NON_MEMBER_WORKSPACE_ID)).thenReturn(false);

        listener
            .onWorkspaceUserRemoved(new WorkspaceUserRemovedEvent(WORKSPACE_ID, REMOVED_USER_ID, "gone@example.com"));

        verify(appInstanceUsageService).deleteUsages(REMOVED_USER_ID, List.of(200L));
        verify(appFavoriteService).deleteFavorites(REMOVED_USER_ID, List.of(100L));
    }

    @Test
    void testOwnerLeavingPublishesOwnerLeftForPrivateAppsAndInstancesOnly() {
        App privateOwnedApp = newApp(100L, REMOVED_USER_ID, ResourceVisibility.PRIVATE);
        App workspaceOwnedApp = newApp(101L, REMOVED_USER_ID, ResourceVisibility.WORKSPACE);
        App privateForeignOwnedApp = newApp(102L, OTHER_USER_ID, ResourceVisibility.PRIVATE);
        AppInstance privateOwnedInstance = newAppInstance(200L, 100L, REMOVED_USER_ID, ResourceVisibility.PRIVATE);
        AppInstance workspaceOwnedInstance =
            newAppInstance(201L, 100L, REMOVED_USER_ID, ResourceVisibility.WORKSPACE);
        AppInstance privateForeignOwnedInstance =
            newAppInstance(202L, 100L, OTHER_USER_ID, ResourceVisibility.PRIVATE);

        when(appService.getApps(WORKSPACE_ID))
            .thenReturn(List.of(privateOwnedApp, workspaceOwnedApp, privateForeignOwnedApp));
        stubWorkspaceAppInstances(Environment.DEVELOPMENT,
            List.of(privateOwnedInstance, workspaceOwnedInstance, privateForeignOwnedInstance));

        listener
            .onWorkspaceUserRemoved(new WorkspaceUserRemovedEvent(WORKSPACE_ID, REMOVED_USER_ID, "gone@example.com"));

        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_OWNER_LEFT, 100L, Map.of("userId", REMOVED_USER_ID, "workspaceId", WORKSPACE_ID));
        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_INSTANCE_OWNER_LEFT, 100L,
            Map.of("appInstanceId", 200L, "userId", REMOVED_USER_ID, "workspaceId", WORKSPACE_ID));
        verify(appAuditPublisher, never()).publish(eq(AppAuditEvent.APP_OWNER_LEFT), eq(101L), any());
        verify(appAuditPublisher, never()).publish(eq(AppAuditEvent.APP_OWNER_LEFT), eq(102L), any());
        verify(appAuditPublisher, never()).publish(eq(AppAuditEvent.APP_INSTANCE_OWNER_LEFT), anyLong(), eq(
            Map.of("appInstanceId", 201L, "userId", REMOVED_USER_ID, "workspaceId", WORKSPACE_ID)));
        verify(appAuditPublisher, never()).publish(eq(AppAuditEvent.APP_INSTANCE_OWNER_LEFT), anyLong(), eq(
            Map.of("appInstanceId", 202L, "userId", REMOVED_USER_ID, "workspaceId", WORKSPACE_ID)));
    }

    @Test
    void testOwnerLeavingDoesNotChangeVisibilityOrOwner() {
        App app = newApp(100L, REMOVED_USER_ID, ResourceVisibility.PRIVATE);
        AppInstance appInstance = newAppInstance(200L, 100L, REMOVED_USER_ID, ResourceVisibility.PRIVATE);

        when(appService.getApps(WORKSPACE_ID)).thenReturn(List.of(app));
        stubWorkspaceAppInstances(Environment.PRODUCTION, List.of(appInstance));

        listener
            .onWorkspaceUserRemoved(new WorkspaceUserRemovedEvent(WORKSPACE_ID, REMOVED_USER_ID, "gone@example.com"));

        assertThat(app.getOwnerUserId()).isEqualTo(REMOVED_USER_ID);
        assertThat(app.getVisibility()).isEqualTo(ResourceVisibility.PRIVATE);
        assertThat(appInstance.getOwnerUserId()).isEqualTo(REMOVED_USER_ID);
        assertThat(appInstance.getVisibility()).isEqualTo(ResourceVisibility.PRIVATE);

        verify(appService, never()).updateVisibility(anyLong(), any());
        verify(appService, never()).update(anyLong(), any(), any());
        verify(appInstanceService, never()).updateVisibility(anyLong(), any());
    }

    @Test
    void testRemovalRevokesTheUsersAuthorizationsOnTheWorkspacesInstanceClientsAfterCommit() {
        AppInstance registeredInstance = newAppInstance(200L, 100L, OTHER_USER_ID, ResourceVisibility.WORKSPACE);
        AppInstance unregisteredInstance = newAppInstance(201L, 100L, OTHER_USER_ID, ResourceVisibility.WORKSPACE);

        registeredInstance.setRegisteredClientId("app-registered");

        when(appService.getApps(WORKSPACE_ID)).thenReturn(List.of());
        stubWorkspaceAppInstances(Environment.STAGING, List.of(registeredInstance, unregisteredInstance));

        TransactionSynchronizationManager.initSynchronization();

        try {
            listener.onWorkspaceUserRemoved(
                new WorkspaceUserRemovedEvent(WORKSPACE_ID, REMOVED_USER_ID, "gone@example.com"));

            verify(appInstanceClients, never()).revokeGrant(any(), any());

            for (TransactionSynchronization transactionSynchronization : TransactionSynchronizationManager
                .getSynchronizations()) {

                transactionSynchronization.afterCommit();
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(appInstanceClients).revokeGrant("app-registered", "gone@example.com");
        verifyNoMoreInteractions(appInstanceClients);
    }

    @Test
    void testRolledBackRemovalRevokesNoAuthorization() {
        AppInstance registeredInstance = newAppInstance(200L, 100L, OTHER_USER_ID, ResourceVisibility.WORKSPACE);

        registeredInstance.setRegisteredClientId("app-registered");

        when(appService.getApps(WORKSPACE_ID)).thenReturn(List.of());
        stubWorkspaceAppInstances(Environment.STAGING, List.of(registeredInstance));

        TransactionSynchronizationManager.initSynchronization();

        try {
            listener.onWorkspaceUserRemoved(
                new WorkspaceUserRemovedEvent(WORKSPACE_ID, REMOVED_USER_ID, "gone@example.com"));

            for (TransactionSynchronization transactionSynchronization : TransactionSynchronizationManager
                .getSynchronizations()) {

                transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verifyNoInteractions(appInstanceClients);
    }

    private void stubWorkspaceAppInstances(Environment populatedEnvironment, List<AppInstance> appInstances) {
        for (Environment environment : Environment.values()) {
            lenient().when(appInstanceService.getWorkspaceAppInstances(WORKSPACE_ID, environment))
                .thenReturn(environment == populatedEnvironment ? appInstances : List.of());
        }
    }

    private static App newApp(long id, long ownerUserId, ResourceVisibility visibility) {
        App app = new App();

        app.setId(id);
        app.setOwnerUserId(ownerUserId);
        app.setVisibility(visibility);
        app.setWorkspaceId(WORKSPACE_ID);

        return app;
    }

    private static AppInstance newAppInstance(long id, long appId, long ownerUserId, ResourceVisibility visibility) {
        AppInstance appInstance = new AppInstance();

        appInstance.setId(id);
        appInstance.setAppId(appId);
        appInstance.setOwnerUserId(ownerUserId);
        appInstance.setVisibility(visibility);
        appInstance.setWorkspaceId(WORKSPACE_ID);

        return appInstance;
    }
}

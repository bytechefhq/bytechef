/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.app.audit.AppAuditEvent;
import com.bytechef.ee.automation.app.audit.AppAuditPublisher;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.security.AppInstanceVisibilityPolicy;
import com.bytechef.ee.automation.app.security.AppVisibilityPolicy;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AppSharingFacadeTest {

    private static final String APP = "App";
    private static final String APP_INSTANCE = "AppInstance";

    private static final long APP_ID = 10L;
    private static final long APP_INSTANCE_ID = 30L;
    private static final long APP_WORKSPACE_ID = 1L;
    private static final long INSTANCE_WORKSPACE_ID = 2L;
    private static final long MEMBER_OF_APP_WORKSPACE_ID = 7L;
    private static final long MEMBER_OF_INSTANCE_WORKSPACE_ID = 8L;
    private static final long NON_MEMBER_USER_ID = 99L;
    private static final long UNKNOWN_USER_ID = 404L;
    private static final long UNKNOWN_APP_ID = 404L;
    private static final long UNKNOWN_APP_INSTANCE_ID = 405L;

    private final AppAuditPublisher appAuditPublisher = mock(AppAuditPublisher.class);
    private final AppInstanceService appInstanceService = mock(AppInstanceService.class);
    private final AppService appService = mock(AppService.class);
    private final ResourceGrantService resourceGrantService = mock(ResourceGrantService.class);
    private final WorkspaceUserService workspaceUserService = mock(WorkspaceUserService.class);

    private AppSharingFacade appSharingFacade;

    @BeforeEach
    void beforeEach() {
        when(appService.fetchApp(APP_ID)).thenReturn(Optional.of(newApp(ResourceVisibility.WORKSPACE)));
        when(appInstanceService.fetchAppInstance(APP_INSTANCE_ID))
            .thenReturn(Optional.of(newAppInstance(ResourceVisibility.WORKSPACE)));

        when(workspaceUserService.isWorkspaceMember(MEMBER_OF_APP_WORKSPACE_ID, APP_WORKSPACE_ID)).thenReturn(true);
        when(workspaceUserService.isWorkspaceMember(MEMBER_OF_INSTANCE_WORKSPACE_ID, INSTANCE_WORKSPACE_ID))
            .thenReturn(true);

        appSharingFacade = new AppSharingFacadeImpl(
            appAuditPublisher, appInstanceService, appService, resourceGrantService,
            new ResourceVisibilityPolicyRegistry(
                List.of(new AppInstanceVisibilityPolicy(), new AppVisibilityPolicy())),
            workspaceUserService);
    }

    @Test
    void testSetAppVisibilityUpdatesAndAudits() {
        appSharingFacade.setAppVisibility(APP_WORKSPACE_ID, APP_ID, ResourceVisibility.PRIVATE);

        verify(appService).updateVisibility(APP_ID, ResourceVisibility.PRIVATE);
        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_VISIBILITY_CHANGED, APP_ID,
            Map.of("visibility", "PRIVATE", "previousVisibility", "WORKSPACE"));
    }

    @Test
    void testSetAppVisibilityToPrivateRevokesGrantsRetainedFromWorkspaceVisibility() {
        appSharingFacade.setAppVisibility(APP_WORKSPACE_ID, APP_ID, ResourceVisibility.PRIVATE);

        InOrder inOrder = inOrder(resourceGrantService, appService);

        inOrder.verify(resourceGrantService)
            .deleteGrants(APP, APP_ID);
        inOrder.verify(appService)
            .updateVisibility(APP_ID, ResourceVisibility.PRIVATE);
    }

    @Test
    void testSetAppVisibilityToPrivateOnAPrivateAppKeepsItsGrants() {
        when(appService.fetchApp(APP_ID)).thenReturn(Optional.of(newApp(ResourceVisibility.PRIVATE)));

        appSharingFacade.setAppVisibility(APP_WORKSPACE_ID, APP_ID, ResourceVisibility.PRIVATE);

        verify(resourceGrantService, never()).deleteGrants(anyString(), anyLong());
        verify(appService).updateVisibility(APP_ID, ResourceVisibility.PRIVATE);
    }

    @Test
    void testSetAppVisibilityToWorkspaceKeepsGrants() {
        when(appService.fetchApp(APP_ID)).thenReturn(Optional.of(newApp(ResourceVisibility.PRIVATE)));

        appSharingFacade.setAppVisibility(APP_WORKSPACE_ID, APP_ID, ResourceVisibility.WORKSPACE);

        verify(resourceGrantService, never()).deleteGrants(anyString(), anyLong());
        verify(appService).updateVisibility(APP_ID, ResourceVisibility.WORKSPACE);
    }

    @Test
    void testSetAppVisibilityRejectsOrganization() {
        assertFailsWith(
            () -> appSharingFacade.setAppVisibility(APP_WORKSPACE_ID, APP_ID, ResourceVisibility.ORGANIZATION),
            AppErrorType.APP_VISIBILITY_UNSUPPORTED, "does not support ORGANIZATION visibility");

        verify(appService, never()).updateVisibility(anyLong(), any());
        verify(resourceGrantService, never()).deleteGrants(anyString(), anyLong());
    }

    @Test
    void testSetAppVisibilityRejectsAppOutsideWorkspace() {
        assertFailsWith(
            () -> appSharingFacade.setAppVisibility(INSTANCE_WORKSPACE_ID, APP_ID, ResourceVisibility.PRIVATE),
            AppErrorType.APP_NOT_FOUND, "does not belong to workspace");

        verify(appService, never()).updateVisibility(anyLong(), any());
    }

    @Test
    void testGrantAppAccessValidatesMembershipThenGrantsAndAudits() {
        appSharingFacade.grantAppAccess(APP_WORKSPACE_ID, APP_ID, MEMBER_OF_APP_WORKSPACE_ID);

        verify(workspaceUserService).isWorkspaceMember(MEMBER_OF_APP_WORKSPACE_ID, APP_WORKSPACE_ID);
        verify(resourceGrantService).grant(APP, APP_ID, MEMBER_OF_APP_WORKSPACE_ID);
        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_ACCESS_GRANTED, APP_ID, Map.of("userId", MEMBER_OF_APP_WORKSPACE_ID));
    }

    @Test
    void testGrantToNonMemberFailsLikeUnknownUser() {
        ConfigurationException nonMemberException = catchConfigurationException(
            () -> appSharingFacade.grantAppInstanceAccess(INSTANCE_WORKSPACE_ID, APP_INSTANCE_ID, NON_MEMBER_USER_ID));
        ConfigurationException unknownUserException = catchConfigurationException(
            () -> appSharingFacade.grantAppInstanceAccess(INSTANCE_WORKSPACE_ID, APP_INSTANCE_ID, UNKNOWN_USER_ID));

        assertThat(nonMemberException.getErrorKey()).isEqualTo(AppErrorType.APP_INSTANCE_NOT_FOUND.getErrorKey());
        assertThat(unknownUserException.getErrorKey()).isEqualTo(nonMemberException.getErrorKey());
        assertThat(unknownUserException.getMessage()
            .replace(String.valueOf(UNKNOWN_USER_ID), "<user>"))
                .isEqualTo(
                    nonMemberException.getMessage()
                        .replace(String.valueOf(NON_MEMBER_USER_ID), "<user>"));

        ConfigurationException appNonMemberException = catchConfigurationException(
            () -> appSharingFacade.grantAppAccess(APP_WORKSPACE_ID, APP_ID, NON_MEMBER_USER_ID));
        ConfigurationException appUnknownUserException = catchConfigurationException(
            () -> appSharingFacade.grantAppAccess(APP_WORKSPACE_ID, APP_ID, UNKNOWN_USER_ID));

        assertThat(appNonMemberException.getErrorKey()).isEqualTo(AppErrorType.APP_NOT_FOUND.getErrorKey());
        assertThat(appUnknownUserException.getErrorKey()).isEqualTo(appNonMemberException.getErrorKey());

        verify(resourceGrantService, never()).grant(anyString(), anyLong(), anyLong());
        verify(appAuditPublisher, never()).publish(any(), anyLong(), any());
    }

    @Test
    void testGrantAppInstanceAccessRequiresMembershipOfTheInstanceWorkspaceNotTheAppWorkspace() {
        assertFailsWith(
            () -> appSharingFacade.grantAppInstanceAccess(
                INSTANCE_WORKSPACE_ID, APP_INSTANCE_ID, MEMBER_OF_APP_WORKSPACE_ID),
            AppErrorType.APP_INSTANCE_NOT_FOUND, "is not a member of workspace");

        verify(workspaceUserService).isWorkspaceMember(MEMBER_OF_APP_WORKSPACE_ID, INSTANCE_WORKSPACE_ID);
        verify(resourceGrantService, never()).grant(anyString(), anyLong(), anyLong());

        appSharingFacade.grantAppInstanceAccess(INSTANCE_WORKSPACE_ID, APP_INSTANCE_ID,
            MEMBER_OF_INSTANCE_WORKSPACE_ID);

        verify(resourceGrantService).grant(APP_INSTANCE, APP_INSTANCE_ID, MEMBER_OF_INSTANCE_WORKSPACE_ID);
        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_INSTANCE_ACCESS_GRANTED, APP_ID,
            Map.of("appInstanceId", APP_INSTANCE_ID, "userId", MEMBER_OF_INSTANCE_WORKSPACE_ID));
    }

    @Test
    void testGrantIsIdempotent() {
        appSharingFacade.grantAppInstanceAccess(INSTANCE_WORKSPACE_ID, APP_INSTANCE_ID,
            MEMBER_OF_INSTANCE_WORKSPACE_ID);
        appSharingFacade.grantAppInstanceAccess(INSTANCE_WORKSPACE_ID, APP_INSTANCE_ID,
            MEMBER_OF_INSTANCE_WORKSPACE_ID);
        appSharingFacade.grantAppAccess(APP_WORKSPACE_ID, APP_ID, MEMBER_OF_APP_WORKSPACE_ID);
        appSharingFacade.grantAppAccess(APP_WORKSPACE_ID, APP_ID, MEMBER_OF_APP_WORKSPACE_ID);

        verify(resourceGrantService, times(2)).grant(APP_INSTANCE, APP_INSTANCE_ID, MEMBER_OF_INSTANCE_WORKSPACE_ID);
        verify(resourceGrantService, times(2)).grant(APP, APP_ID, MEMBER_OF_APP_WORKSPACE_ID);
        verify(resourceGrantService, never()).getGrantedUserIds(anyString(), anyLong());
    }

    @Test
    void testRevokeSkipsMembershipCheckAndAudits() {
        appSharingFacade.revokeAppInstanceAccess(INSTANCE_WORKSPACE_ID, APP_INSTANCE_ID, NON_MEMBER_USER_ID);
        appSharingFacade.revokeAppAccess(APP_WORKSPACE_ID, APP_ID, NON_MEMBER_USER_ID);

        verify(resourceGrantService).revoke(APP_INSTANCE, APP_INSTANCE_ID, NON_MEMBER_USER_ID);
        verify(resourceGrantService).revoke(APP, APP_ID, NON_MEMBER_USER_ID);
        verify(workspaceUserService, never()).isWorkspaceMember(anyLong(), anyLong());
        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_INSTANCE_ACCESS_REVOKED, APP_ID,
            Map.of("appInstanceId", APP_INSTANCE_ID, "userId", NON_MEMBER_USER_ID));
        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_ACCESS_REVOKED, APP_ID, Map.of("userId", NON_MEMBER_USER_ID));
    }

    @Test
    void testGetGrantsReturnsUserIds() {
        when(resourceGrantService.getGrantedUserIds(APP, APP_ID)).thenReturn(List.of(MEMBER_OF_APP_WORKSPACE_ID));
        when(resourceGrantService.getGrantedUserIds(APP_INSTANCE, APP_INSTANCE_ID))
            .thenReturn(List.of(MEMBER_OF_INSTANCE_WORKSPACE_ID));

        assertThat(appSharingFacade.getAppGrants(APP_WORKSPACE_ID, APP_ID))
            .containsExactly(MEMBER_OF_APP_WORKSPACE_ID);
        assertThat(appSharingFacade.getAppInstanceGrants(INSTANCE_WORKSPACE_ID, APP_INSTANCE_ID))
            .containsExactly(MEMBER_OF_INSTANCE_WORKSPACE_ID);
    }

    @Test
    void testGetGrantsRejectsResourceOutsideWorkspace() {
        assertFailsWith(
            () -> appSharingFacade.getAppGrants(INSTANCE_WORKSPACE_ID, APP_ID), AppErrorType.APP_NOT_FOUND,
            "does not belong to workspace");
        assertFailsWith(
            () -> appSharingFacade.getAppInstanceGrants(APP_WORKSPACE_ID, APP_INSTANCE_ID),
            AppErrorType.APP_INSTANCE_NOT_FOUND, "does not belong to workspace");

        verify(resourceGrantService, never()).getGrantedUserIds(anyString(), anyLong());
    }

    @Test
    void testUnknownResourceFailsWithATypedError() {
        assertFailsWith(
            () -> appSharingFacade.grantAppAccess(APP_WORKSPACE_ID, UNKNOWN_APP_ID, MEMBER_OF_APP_WORKSPACE_ID),
            AppErrorType.APP_NOT_FOUND, "does not belong to workspace");
        assertFailsWith(
            () -> appSharingFacade.grantAppInstanceAccess(
                INSTANCE_WORKSPACE_ID, UNKNOWN_APP_INSTANCE_ID, MEMBER_OF_INSTANCE_WORKSPACE_ID),
            AppErrorType.APP_INSTANCE_NOT_FOUND, "does not belong to workspace");

        verify(workspaceUserService, never()).isWorkspaceMember(anyLong(), anyLong());
        verify(resourceGrantService, never()).grant(anyString(), anyLong(), anyLong());
    }

    @Test
    void testSetAppInstanceVisibilityToPrivateRevokesGrantsUpdatesAndAudits() {
        appSharingFacade.setAppInstanceVisibility(INSTANCE_WORKSPACE_ID, APP_INSTANCE_ID, ResourceVisibility.PRIVATE);

        InOrder inOrder = inOrder(resourceGrantService, appInstanceService);

        inOrder.verify(resourceGrantService)
            .deleteGrants(APP_INSTANCE, APP_INSTANCE_ID);
        inOrder.verify(appInstanceService)
            .updateVisibility(APP_INSTANCE_ID, ResourceVisibility.PRIVATE);

        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_INSTANCE_VISIBILITY_CHANGED, APP_ID,
            Map.of("appInstanceId", APP_INSTANCE_ID, "visibility", "PRIVATE", "previousVisibility", "WORKSPACE"));
    }

    @Test
    void testSetAppInstanceVisibilityRejectsOrganizationAndForeignWorkspace() {
        assertFailsWith(
            () -> appSharingFacade.setAppInstanceVisibility(
                INSTANCE_WORKSPACE_ID, APP_INSTANCE_ID, ResourceVisibility.ORGANIZATION),
            AppErrorType.APP_VISIBILITY_UNSUPPORTED, "does not support ORGANIZATION visibility");
        assertFailsWith(
            () -> appSharingFacade.setAppInstanceVisibility(
                APP_WORKSPACE_ID, APP_INSTANCE_ID, ResourceVisibility.PRIVATE),
            AppErrorType.APP_INSTANCE_NOT_FOUND, "does not belong to workspace");

        verify(appInstanceService, never()).updateVisibility(anyLong(), any());
        verify(resourceGrantService, never()).deleteGrants(anyString(), anyLong());
    }

    private static void assertFailsWith(ThrowingCallable callable, AppErrorType appErrorType, String message) {
        assertThatThrownBy(callable)
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining(message)
            .satisfies(
                exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(appErrorType.getErrorKey()));
    }

    private static ConfigurationException catchConfigurationException(ThrowingCallable callable) {
        Throwable throwable = catchThrowable(callable);

        assertThat(throwable).isInstanceOf(ConfigurationException.class);

        return (ConfigurationException) throwable;
    }

    private static App newApp(ResourceVisibility visibility) {
        App app = new App();

        app.setId(APP_ID);
        app.setWorkspaceId(APP_WORKSPACE_ID);
        app.setVisibility(visibility);

        return app;
    }

    private static AppInstance newAppInstance(ResourceVisibility visibility) {
        AppInstance appInstance = new AppInstance();

        appInstance.setId(APP_INSTANCE_ID);
        appInstance.setAppId(APP_ID);
        appInstance.setWorkspaceId(INSTANCE_WORKSPACE_ID);
        appInstance.setVisibility(visibility);

        return appInstance;
    }
}

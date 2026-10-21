/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.APP;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.CONNECTION_A;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.CONNECTION_B;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.EDITOR;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.OUTSIDER;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.PRIVATE_APP;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.PRODUCTION_VIEWER;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.TENANT_ADMIN;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.VIEWER;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.WORKSPACE_A;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.WORKSPACE_ADMIN;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.authenticate;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.authenticateAnonymously;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.newApp;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppConnection;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.dto.AppBindingDTO;
import com.bytechef.ee.automation.app.service.AppConnectionService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs the {@code @PreAuthorize} guards on {@link AppFacadeImpl} through real Spring method security, the production
 * expression root and permission evaluator, the EE {@code PermissionServiceImpl} and the production App and Connection
 * resolvers (see {@link AppAuthorizationTestConfiguration}). Only who is calling and which workspace rows exist are
 * stubbed ({@link AppAuthorizationFixture}).
 *
 * <p>
 * Every denial is paired with an allowance on the same guard, so no assertion here can pass because the context denies
 * everything; and every refused mutation also checks that the facade body never ran.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AppAuthorizationTestConfiguration.class, properties = "bytechef.edition=ee")
class AppFacadeAuthorizationTest {

    @Autowired
    private AppConnectionService appConnectionService;

    @Autowired
    private AppFacade appFacade;

    @Autowired
    private AppService appService;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkspaceConnectionRepository workspaceConnectionRepository;

    @Autowired
    private WorkspaceUserRepository workspaceUserRepository;

    @BeforeEach
    void beforeEach() {
        AppAuthorizationFixture.stubTenant(
            appService, connectionService, userService, workspaceConnectionRepository, workspaceUserRepository);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class AppView {

        @Test
        void testViewerCanGetApp() {
            authenticate(VIEWER);

            assertThat(appFacade.getApp(APP)
                .id()).isEqualTo(APP);
        }

        @Test
        void testMemberOfAnotherWorkspaceIsDeniedGetApp() {
            authenticate(OUTSIDER);

            assertThatThrownBy(() -> appFacade.getApp(APP)).isInstanceOf(AccessDeniedException.class);

            verify(appService, never()).getApp(anyLong());
        }

        @Test
        void testMemberOfAnotherWorkspaceIsDeniedGetBindings() {
            authenticate(OUTSIDER);

            assertThatThrownBy(() -> appFacade.getBindings(APP)).isInstanceOf(AccessDeniedException.class);

            verify(appConnectionService, never()).getBindings(anyLong());
        }

        @Test
        void testViewerCanGetBindings() {
            authenticate(VIEWER);

            assertThat(appFacade.getBindings(APP)).isEmpty();

            verify(appConnectionService).getBindings(APP);
        }

        @Test
        void testMemberOfAnotherWorkspaceIsDeniedGetApps() {
            authenticate(OUTSIDER);

            assertThatThrownBy(() -> appFacade.getApps(WORKSPACE_A, Environment.PRODUCTION.ordinal()))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void testViewerCanGetApps() {
            authenticate(VIEWER);

            assertThat(appFacade.getApps(WORKSPACE_A, Environment.PRODUCTION.ordinal())).isEmpty();

            verify(appService).getApps(WORKSPACE_A, Environment.PRODUCTION);
        }

        @Test
        void testViewerIsDeniedAColleaguesPrivateApp() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appFacade.getApp(PRIVATE_APP)).isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void testCreatorCanGetTheirPrivateApp() {
            authenticate(WORKSPACE_ADMIN);

            assertThat(appFacade.getApp(PRIVATE_APP)
                .id()).isEqualTo(PRIVATE_APP);
        }

        @Test
        void testUnknownAppIsDenied() {
            authenticate(WORKSPACE_ADMIN);

            assertThatThrownBy(() -> appFacade.getApp(999L)).isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    class AppManage {

        @Test
        void testViewerIsDeniedRotateSecretKey() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appFacade.rotateSecretKey(APP)).isInstanceOf(AccessDeniedException.class);

            verify(appService, never()).rotateSecretKey(anyLong());
        }

        @Test
        void testEditorIsDeniedRotateSecretKey() {
            authenticate(EDITOR);

            assertThatThrownBy(() -> appFacade.rotateSecretKey(APP)).isInstanceOf(AccessDeniedException.class);

            verify(appService, never()).rotateSecretKey(anyLong());
        }

        @Test
        void testWorkspaceAdminCanRotateSecretKey() {
            authenticate(WORKSPACE_ADMIN);

            App app = newApp(APP, ResourceVisibility.WORKSPACE);

            when(appService.rotateSecretKey(APP)).thenReturn(new AppService.RotatedKey(app, "new-key"));

            assertThat(appFacade.rotateSecretKey(APP)
                .secretKey()).isEqualTo("new-key");
        }

        @Test
        void testAdminInDevelopmentButViewerInProductionIsDeniedRotateSecretKeyOnAProductionApp() {
            authenticate(PRODUCTION_VIEWER);

            assertThatThrownBy(() -> appFacade.rotateSecretKey(APP)).isInstanceOf(AccessDeniedException.class);

            verify(appService, never()).rotateSecretKey(anyLong());
        }

        @Test
        void testViewerIsDeniedDeleteApp() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appFacade.deleteApp(APP)).isInstanceOf(AccessDeniedException.class);

            verify(appService, never()).delete(anyLong());
        }

        @Test
        void testWorkspaceAdminCanDeleteApp() {
            authenticate(WORKSPACE_ADMIN);

            appFacade.deleteApp(APP);

            verify(appService).delete(APP);
        }

        @Test
        void testTenantAdminWithoutMembershipCanDeleteApp() {
            authenticate(TENANT_ADMIN);

            appFacade.deleteApp(APP);

            verify(appService).delete(APP);
        }

        @Test
        void testViewerIsDeniedUpdateAppStatus() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appFacade.updateAppStatus(APP, AppStatus.DISABLED))
                .isInstanceOf(AccessDeniedException.class);

            verify(appService, never()).updateStatus(anyLong(), any());
        }

        @Test
        void testWorkspaceAdminCanUpdateAppStatus() {
            authenticate(WORKSPACE_ADMIN);

            App app = newApp(APP, ResourceVisibility.WORKSPACE);

            app.setStatus(AppStatus.DISABLED);

            when(appService.updateStatus(APP, AppStatus.DISABLED)).thenReturn(app);

            assertThat(appFacade.updateAppStatus(APP, AppStatus.DISABLED)
                .status()).isEqualTo(AppStatus.DISABLED);
        }

        @Test
        void testViewerIsDeniedUpdateApp() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appFacade.updateApp(APP, "renamed", null, null, null, null, null))
                .isInstanceOf(AccessDeniedException.class);

            verify(appService, never()).update(anyLong(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void testViewerIsDeniedUnbindConnection() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appFacade.unbindConnection(APP, "crm")).isInstanceOf(AccessDeniedException.class);

            verify(appConnectionService, never()).unbind(anyLong(), anyString());
        }

        @Test
        void testViewerIsDeniedGetBindableConnections() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appFacade.getBindableConnections(APP, null))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void testViewerIsDeniedCreateApp() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appFacade.createApp(
                WORKSPACE_A, Environment.PRODUCTION, "crm", null, AppHostingTarget.VERCEL,
                ResourceVisibility.WORKSPACE))
                    .isInstanceOf(AccessDeniedException.class);

            verify(appService, never()).create(any());
        }

        @Test
        void testWorkspaceAdminCanCreateApp() {
            authenticate(WORKSPACE_ADMIN);

            when(appService.create(any()))
                .thenReturn(new AppService.CreatedApp(newApp(APP, ResourceVisibility.WORKSPACE), "secret"));

            assertThat(appFacade.createApp(
                WORKSPACE_A, Environment.PRODUCTION, "crm", null, AppHostingTarget.VERCEL,
                ResourceVisibility.WORKSPACE)
                .secretKey()).isEqualTo("secret");
        }
    }

    @Nested
    class ConnectionUse {

        @Test
        void testViewerIsDeniedBindConnection() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> bindConnection(CONNECTION_A)).isInstanceOf(AccessDeniedException.class);

            verify(appConnectionService, never()).bind(
                anyLong(), anyString(), anyLong(), anyList(), anyList(), anyBoolean(), anyList());
        }

        @Test
        void testEditorWithConnectionUseButNotAppManageIsDeniedBindConnection() {
            authenticate(EDITOR);

            assertThatThrownBy(() -> bindConnection(CONNECTION_A)).isInstanceOf(AccessDeniedException.class);

            verify(appConnectionService, never()).bind(
                anyLong(), anyString(), anyLong(), anyList(), anyList(), anyBoolean(), anyList());
        }

        @Test
        void testAppManagerWithoutConnectionUseIsDeniedBindConnection() {
            authenticate(WORKSPACE_ADMIN);

            assertThatThrownBy(() -> bindConnection(CONNECTION_B)).isInstanceOf(AccessDeniedException.class);

            verify(appConnectionService, never()).bind(
                anyLong(), anyString(), anyLong(), anyList(), anyList(), anyBoolean(), anyList());
        }

        @Test
        void testAppManagerWithConnectionUseCanBindConnection() {
            authenticate(WORKSPACE_ADMIN);

            AppConnection appConnection = new AppConnection();

            appConnection.setId(1L);
            appConnection.setAppId(APP);
            appConnection.setBindingKey("crm");
            appConnection.setConnectionId(CONNECTION_A);

            when(appConnectionService.bind(
                eq(APP), eq("crm"), eq(CONNECTION_A), anyList(), anyList(), anyBoolean(), anyList()))
                    .thenReturn(appConnection);

            assertThat(bindConnection(CONNECTION_A).bindingKey()).isEqualTo("crm");
        }

        private AppBindingDTO bindConnection(long connectionId) {
            return appFacade.bindConnection(
                APP, "crm", connectionId, List.of("send_message"), List.of(), false, List.of());
        }
    }

    @Nested
    class ConnectionView {

        @Test
        void testViewerCanGetConnections() {
            authenticate(VIEWER);

            assertThat(appFacade.getConnections(WORKSPACE_A, Environment.PRODUCTION, null)).isEmpty();
        }

        @Test
        void testMemberOfAnotherWorkspaceIsDeniedGetConnections() {
            authenticate(OUTSIDER);

            assertThatThrownBy(() -> appFacade.getConnections(WORKSPACE_A, Environment.PRODUCTION, null))
                .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    class Authenticated {

        @Test
        void testAnonymousIsDeniedGetComponentOperations() {
            authenticateAnonymously();

            assertThatThrownBy(() -> appFacade.getComponentOperations("slack", 1))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void testAnyAuthenticatedUserCanGetComponentOperations() {
            authenticate(OUTSIDER);

            assertThat(appFacade.getComponentOperations("slack", 1)
                .actionNames()).isEmpty();
        }
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.APP;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.APP_INSTANCE;
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
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.newAppInstance;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.file.storage.AppFileStorage;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.user.service.UserService;
import java.io.ByteArrayInputStream;
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
    private AppFacade appFacade;

    @Autowired
    private AppFileStorage appFileStorage;

    @Autowired
    private AppInstanceService appInstanceService;

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
            appInstanceService, appService, connectionService, userService, workspaceConnectionRepository,
            workspaceUserRepository);
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
        void testMemberOfAnotherWorkspaceIsDeniedGetApps() {
            authenticate(OUTSIDER);

            assertThatThrownBy(() -> appFacade.getApps(WORKSPACE_A))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void testViewerCanGetApps() {
            authenticate(VIEWER);

            assertThat(appFacade.getApps(WORKSPACE_A)).isEmpty();

            verify(appService).getApps(WORKSPACE_A);
            verify(appInstanceService, never()).getAppInstancesByAppIds(any());
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

            assertThatThrownBy(() -> appFacade.updateAppStatus(APP, AppStatus.SUSPENDED))
                .isInstanceOf(AccessDeniedException.class);

            verify(appService, never()).updateStatus(anyLong(), any());
        }

        @Test
        void testAdminInDevelopmentButViewerInProductionIsDeniedDeleteApp() {
            authenticate(PRODUCTION_VIEWER);

            assertThatThrownBy(() -> appFacade.deleteApp(APP)).isInstanceOf(AccessDeniedException.class);

            verify(appService, never()).delete(anyLong());

            authenticate(WORKSPACE_ADMIN);

            appFacade.deleteApp(APP);

            verify(appService).delete(APP);
        }

        @Test
        void testAdminInDevelopmentButViewerInProductionIsDeniedUpdateAppStatus() {
            authenticate(PRODUCTION_VIEWER);

            assertThatThrownBy(() -> appFacade.updateAppStatus(APP, AppStatus.SUSPENDED))
                .isInstanceOf(AccessDeniedException.class);

            verify(appService, never()).updateStatus(anyLong(), any());

            authenticate(WORKSPACE_ADMIN);

            when(appService.updateStatus(APP, AppStatus.SUSPENDED))
                .thenReturn(newApp(APP, ResourceVisibility.WORKSPACE));

            appFacade.updateAppStatus(APP, AppStatus.SUSPENDED);

            verify(appService).updateStatus(APP, AppStatus.SUSPENDED);
        }

        @Test
        void testWorkspaceAdminCanUpdateAppStatus() {
            authenticate(WORKSPACE_ADMIN);

            App app = newApp(APP, ResourceVisibility.WORKSPACE);

            app.setStatus(AppStatus.SUSPENDED);

            when(appService.updateStatus(APP, AppStatus.SUSPENDED)).thenReturn(app);

            assertThat(appFacade.updateAppStatus(APP, AppStatus.SUSPENDED)
                .status()).isEqualTo(AppStatus.SUSPENDED);
        }

        @Test
        void testViewerIsDeniedUpdateApp() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appFacade.updateApp(APP, "renamed", null, null, true))
                .isInstanceOf(AccessDeniedException.class);

            verify(appService, never()).update(anyLong(), any(), any());
        }

        @Test
        void testViewerIsDeniedCreateApp() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appFacade.createApp(
                WORKSPACE_A, Environment.PRODUCTION, "crm", null, AppHostingTarget.VERCEL,
                ResourceVisibility.WORKSPACE, AppUserAuthMode.NONE, null, true))
                    .isInstanceOf(AccessDeniedException.class);

            verify(appService, never()).create(any());
        }

        @Test
        void testAdminInDevelopmentButViewerInProductionIsDeniedCreateAppInProduction() {
            authenticate(PRODUCTION_VIEWER);

            assertThatThrownBy(() -> appFacade.createApp(
                WORKSPACE_A, Environment.PRODUCTION, "crm", null, AppHostingTarget.VERCEL,
                ResourceVisibility.WORKSPACE, AppUserAuthMode.NONE, null, true))
                    .isInstanceOf(AccessDeniedException.class);

            verify(appService, never()).create(any());
        }

        @Test
        void testOutsiderIsDeniedUpdateApp() {
            authenticate(OUTSIDER);

            assertThatThrownBy(() -> appFacade.updateApp(APP, "renamed", null, null, true))
                .isInstanceOf(AccessDeniedException.class);

            verify(appService, never()).update(anyLong(), any(), any());
        }

        @Test
        void testWorkspaceAdminCanPublishAnAppToThePortal() {
            authenticate(WORKSPACE_ADMIN);

            App app = newApp(APP, ResourceVisibility.WORKSPACE);

            when(appService.update(APP, null, null)).thenReturn(app);
            when(appService.updatePortalDiscoverable(APP, true)).thenReturn(app);

            appFacade.updateApp(APP, null, null, null, true);

            verify(appService).updatePortalDiscoverable(APP, true);
        }

        @Test
        void testViewerIsDeniedGetAppCategories() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appFacade.getAppCategories(WORKSPACE_A))
                .isInstanceOf(AccessDeniedException.class);

            verify(appService, never()).getApps(anyLong());
        }

        @Test
        void testOutsiderIsDeniedGetAppCategories() {
            authenticate(OUTSIDER);

            assertThatThrownBy(() -> appFacade.getAppCategories(WORKSPACE_A))
                .isInstanceOf(AccessDeniedException.class);

            verify(appService, never()).getApps(anyLong());
        }

        @Test
        void testWorkspaceAdminCanGetAppCategories() {
            authenticate(WORKSPACE_ADMIN);

            assertThat(appFacade.getAppCategories(WORKSPACE_A)).isEmpty();

            verify(appService).getApps(WORKSPACE_A);
        }

        @Test
        void testWorkspaceAdminCanCreateApp() {
            authenticate(WORKSPACE_ADMIN);

            when(appService.create(any())).thenReturn(newApp(APP, ResourceVisibility.WORKSPACE));
            when(appInstanceService.create(any()))
                .thenReturn(new AppInstanceService.CreatedAppInstance(
                    newAppInstance(APP_INSTANCE, APP, WORKSPACE_A, WORKSPACE_ADMIN), "secret"));

            assertThat(appFacade.createApp(
                WORKSPACE_A, Environment.PRODUCTION, "crm", null, AppHostingTarget.VERCEL,
                ResourceVisibility.WORKSPACE, AppUserAuthMode.NONE, null, true)
                .secretKey()).isEqualTo("secret");
        }
    }

    @Nested
    class AppIcon {

        private static final byte[] PNG = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
        };

        @Test
        void testViewerIsDeniedUpdateAppIcon() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appFacade.updateAppIcon(APP, new ByteArrayInputStream(PNG)))
                .isInstanceOf(AccessDeniedException.class);

            verify(appFileStorage, never()).storeIcon(any(), any());
            verify(appService, never()).updateIcon(anyLong(), any());
        }

        @Test
        void testOutsiderIsDeniedUpdateAppIcon() {
            authenticate(OUTSIDER);

            assertThatThrownBy(() -> appFacade.updateAppIcon(APP, new ByteArrayInputStream(PNG)))
                .isInstanceOf(AccessDeniedException.class);

            verify(appFileStorage, never()).storeIcon(any(), any());
        }

        @Test
        void testWorkspaceAdminCanUpdateAppIcon() {
            authenticate(WORKSPACE_ADMIN);

            FileEntry icon = new FileEntry("icon.png", "base64:///app_icons/icon.png");

            when(appFileStorage.storeIcon("icon.png", PNG)).thenReturn(icon);

            appFacade.updateAppIcon(APP, new ByteArrayInputStream(PNG));

            verify(appService).updateIcon(APP, icon);
        }

        @Test
        void testViewerIsDeniedDeleteAppIcon() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appFacade.deleteAppIcon(APP)).isInstanceOf(AccessDeniedException.class);

            verify(appService, never()).getApp(anyLong());
        }

        @Test
        void testWorkspaceAdminCanDeleteAppIcon() {
            authenticate(WORKSPACE_ADMIN);

            appFacade.deleteAppIcon(APP);

            verify(appService).getApp(APP);
        }

        @Test
        void testViewerCanGetAppIcon() {
            authenticate(VIEWER);

            assertThat(appFacade.getAppIcon(APP)).isEmpty();

            verify(appService).getApp(APP);
        }

        @Test
        void testMemberOfAnotherWorkspaceIsDeniedGetAppIcon() {
            authenticate(OUTSIDER);

            assertThatThrownBy(() -> appFacade.getAppIcon(APP)).isInstanceOf(AccessDeniedException.class);

            verify(appService, never()).getApp(anyLong());
        }

        @Test
        void testViewerIsDeniedTheIconOfAColleaguesPrivateApp() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appFacade.getAppIcon(PRIVATE_APP)).isInstanceOf(AccessDeniedException.class);

            verify(appService, never()).getApp(anyLong());
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

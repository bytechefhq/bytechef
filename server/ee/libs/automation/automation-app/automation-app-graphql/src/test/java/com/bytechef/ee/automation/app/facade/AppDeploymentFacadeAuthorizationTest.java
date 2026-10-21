/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.APP;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.APP_INSTANCE;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.DEVELOPMENT_ADMIN;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.EDITOR;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.FOREIGN_APP_INSTANCE;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.OUTSIDER;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.PRODUCTION_ONLY_VIEWER;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.PRODUCTION_VIEWER;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.VIEWER;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.WORKSPACE_A;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.WORKSPACE_ADMIN;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.authenticate;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.authenticateAnonymously;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.newAppInstance;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.ee.automation.app.domain.AppDeployment;
import com.bytechef.ee.automation.app.domain.AppDeploymentProviderType;
import com.bytechef.ee.automation.app.domain.AppDeploymentStatus;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppVersion;
import com.bytechef.ee.automation.app.dto.AppDeploymentDTO;
import com.bytechef.ee.automation.app.service.AppDeploymentService;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.app.service.AppVersionService;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.user.service.UserService;
import java.time.Instant;
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
 * Runs the {@code @PreAuthorize} guards on {@link AppDeploymentFacadeImpl} through real Spring method security, the
 * production expression root and permission evaluator, the EE {@code PermissionServiceImpl} and the production App and
 * AppInstance resolvers (see {@link AppAuthorizationTestConfiguration}). Every denial is paired with an allowance on
 * the same guard, and every refused write also checks that the facade body never ran.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AppAuthorizationTestConfiguration.class, properties = "bytechef.edition=ee")
class AppDeploymentFacadeAuthorizationTest {

    private static final long DEVELOPMENT_APP_INSTANCE = 34L;

    @Autowired
    private AppDeploymentFacade appDeploymentFacade;

    @Autowired
    private AppDeploymentService appDeploymentService;

    @Autowired
    private AppInstanceService appInstanceService;

    @Autowired
    private AppService appService;

    @Autowired
    private AppVersionService appVersionService;

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

        AppInstance developmentAppInstance = newAppInstance(
            DEVELOPMENT_APP_INSTANCE, APP, WORKSPACE_A, WORKSPACE_ADMIN);

        developmentAppInstance.setName("Development");
        developmentAppInstance.setEnvironment(Environment.DEVELOPMENT);

        when(appInstanceService.fetchAppInstance(DEVELOPMENT_APP_INSTANCE))
            .thenReturn(Optional.of(developmentAppInstance));
        when(appInstanceService.getAppInstance(DEVELOPMENT_APP_INSTANCE)).thenReturn(developmentAppInstance);

        when(appVersionService.fetchAppVersion(anyLong(), anyString())).thenReturn(Optional.empty());
        when(appVersionService.create(any())).thenAnswer(invocation -> {
            AppVersion appVersion = invocation.getArgument(0);

            appVersion.setId(50L);
            appVersion.setCreatedBy(WORKSPACE_ADMIN);
            appVersion.setCreatedDate(Instant.now());

            return appVersion;
        });
        when(appDeploymentService.create(any())).thenAnswer(invocation -> {
            AppDeployment appDeployment = invocation.getArgument(0);

            appDeployment.setId(60L);
            appDeployment.setCreatedBy(WORKSPACE_ADMIN);
            appDeployment.setCreatedDate(Instant.now());

            return appDeployment;
        });
        when(appDeploymentService.getAppDeployments(anyLong(), anyInt())).thenReturn(List.of());
        when(appVersionService.getAppVersions(anyLong(), anyInt())).thenReturn(List.of());
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testEditorMayRecordAndViewerMayNot() {
        authenticate(VIEWER);

        assertThatThrownBy(() -> recordDeployment(APP_INSTANCE))
            .isInstanceOf(AccessDeniedException.class);

        verify(appDeploymentService, never()).create(any());
        verify(appInstanceService, never()).updateCurrentAppDeployment(anyLong(), anyLong());

        assertThat(appDeploymentFacade.getDeployments(APP_INSTANCE, 50)).isEmpty();

        authenticate(EDITOR);

        assertThat(recordDeployment(APP_INSTANCE).id()).isEqualTo(60L);

        verify(appInstanceService).updateCurrentAppDeployment(APP_INSTANCE, 60L);
    }

    @Test
    void testRecordInProductionNeedsProductionRole() {
        authenticate(PRODUCTION_VIEWER);

        assertThatThrownBy(() -> recordDeployment(APP_INSTANCE))
            .isInstanceOf(AccessDeniedException.class);

        verify(appDeploymentService, never()).create(any());

        assertThat(appDeploymentFacade.getDeployments(APP_INSTANCE, 50)).isEmpty();
        assertThat(recordDeployment(DEVELOPMENT_APP_INSTANCE).id()).isEqualTo(60L);

        authenticate(DEVELOPMENT_ADMIN);

        assertThatThrownBy(() -> recordDeployment(APP_INSTANCE))
            .isInstanceOf(AccessDeniedException.class);
        assertThat(recordDeployment(DEVELOPMENT_APP_INSTANCE).id()).isEqualTo(60L);
    }

    @Test
    void testRecordOnForeignInstanceNeedsDeployInItsOwnWorkspace() {
        authenticate(WORKSPACE_ADMIN);

        assertThatThrownBy(() -> recordDeployment(FOREIGN_APP_INSTANCE))
            .isInstanceOf(AccessDeniedException.class);

        verify(appDeploymentService, never()).create(any());

        authenticate(OUTSIDER);

        assertThat(recordDeployment(FOREIGN_APP_INSTANCE).id()).isEqualTo(60L);
    }

    @Test
    void testListingDeploymentsNeedsViewOnTheInstance() {
        authenticate(OUTSIDER);

        assertThatThrownBy(() -> appDeploymentFacade.getDeployments(APP_INSTANCE, 50))
            .isInstanceOf(AccessDeniedException.class);

        verify(appDeploymentService, never()).getAppDeployments(anyLong(), anyInt());

        authenticate(PRODUCTION_ONLY_VIEWER);

        assertThat(appDeploymentFacade.getDeployments(APP_INSTANCE, 50)).isEmpty();
    }

    @Test
    void testReadingTheCurrentDeploymentNeedsViewOnTheInstance() {
        authenticate(OUTSIDER);

        assertThatThrownBy(() -> appDeploymentFacade.getCurrentDeployment(APP_INSTANCE))
            .isInstanceOf(AccessDeniedException.class);

        verify(appDeploymentService, never()).getAppDeployment(anyLong());

        authenticate(PRODUCTION_ONLY_VIEWER);

        assertThat(appDeploymentFacade.getCurrentDeployment(APP_INSTANCE)).isEmpty();
    }

    @Test
    void testEditorMayCreateVersionAndViewerMayNot() {
        authenticate(VIEWER);

        assertThatThrownBy(() -> appDeploymentFacade.createVersion(APP, "1.0", null, null, null))
            .isInstanceOf(AccessDeniedException.class);

        verify(appVersionService, never()).create(any());

        assertThat(appDeploymentFacade.getVersions(APP, 50)).isEmpty();

        authenticate(EDITOR);

        assertThat(appDeploymentFacade.createVersion(APP, "1.0", null, null, null)
            .id()).isEqualTo(50L);
    }

    @Test
    void testCreateVersionPassesOnDeployHeldInOneEnvironmentOnly() {
        authenticate(PRODUCTION_ONLY_VIEWER);

        assertThatThrownBy(() -> appDeploymentFacade.createVersion(APP, "1.0", null, null, null))
            .isInstanceOf(AccessDeniedException.class);

        verify(appVersionService, never()).create(any());

        authenticate(DEVELOPMENT_ADMIN);

        assertThat(appDeploymentFacade.createVersion(APP, "1.0", null, null, null)
            .id()).isEqualTo(50L);
    }

    @Test
    void testListingVersionsNeedsViewOnTheApp() {
        authenticate(OUTSIDER);

        assertThatThrownBy(() -> appDeploymentFacade.getVersions(APP, 50))
            .isInstanceOf(AccessDeniedException.class);

        verify(appVersionService, never()).getAppVersions(anyLong(), anyInt());

        authenticate(VIEWER);

        assertThat(appDeploymentFacade.getVersions(APP, 50)).isEmpty();
    }

    @Test
    void testReadingADeploymentNeedsViewOnTheInstance() {
        AppDeployment appDeployment = new AppDeployment();

        appDeployment.setId(60L);
        appDeployment.setAppInstanceId(APP_INSTANCE);
        appDeployment.setAppVersionId(50L);
        appDeployment.setProvider(AppDeploymentProviderType.VERCEL);
        appDeployment.setStatus(AppDeploymentStatus.BUILDING);
        appDeployment.setCreatedBy(WORKSPACE_ADMIN);
        appDeployment.setCreatedDate(Instant.now());

        AppVersion appVersion = new AppVersion();

        appVersion.setId(50L);
        appVersion.setAppId(APP);
        appVersion.setName("1.0");
        appVersion.setCreatedBy(WORKSPACE_ADMIN);
        appVersion.setCreatedDate(Instant.now());

        when(appDeploymentService.getAppDeployment(60L)).thenReturn(appDeployment);
        when(appVersionService.getAppVersion(50L)).thenReturn(appVersion);

        authenticate(OUTSIDER);

        assertThatThrownBy(() -> appDeploymentFacade.getDeployment(APP_INSTANCE, 60L))
            .isInstanceOf(AccessDeniedException.class);

        verify(appDeploymentService, never()).getAppDeployment(anyLong());

        authenticate(PRODUCTION_ONLY_VIEWER);

        assertThat(appDeploymentFacade.getDeployment(APP_INSTANCE, 60L)
            .id()).isEqualTo(60L);
    }

    @Test
    void testFetchingAVersionByNameNeedsViewOnTheApp() {
        authenticate(OUTSIDER);

        assertThatThrownBy(() -> appDeploymentFacade.fetchVersion(APP, "1.0"))
            .isInstanceOf(AccessDeniedException.class);

        verify(appVersionService, never()).fetchAppVersion(anyLong(), anyString());

        authenticate(VIEWER);

        assertThat(appDeploymentFacade.fetchVersion(APP, "1.0")).isEmpty();
    }

    @Test
    void testHostWorkspaceMemberReadsTheVersionsThroughTheInstanceOnly() {
        authenticate(OUTSIDER);

        assertThatThrownBy(() -> appDeploymentFacade.getVersions(APP, 50))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> appDeploymentFacade.fetchVersion(APP, "1.0"))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(appDeploymentFacade.getVersionsForInstance(FOREIGN_APP_INSTANCE, 50)).isEmpty();
        assertThat(appDeploymentFacade.fetchVersionForInstance(FOREIGN_APP_INSTANCE, "1.0")).isEmpty();

        verify(appVersionService).getAppVersions(APP, 50);
        verify(appVersionService).fetchAppVersion(APP, "1.0");
    }

    @Test
    void testReadingVersionsThroughAnInstanceNeedsViewOnTheInstance() {
        authenticate(VIEWER);

        assertThatThrownBy(() -> appDeploymentFacade.getVersionsForInstance(FOREIGN_APP_INSTANCE, 50))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> appDeploymentFacade.fetchVersionForInstance(FOREIGN_APP_INSTANCE, "1.0"))
            .isInstanceOf(AccessDeniedException.class);

        verify(appVersionService, never()).getAppVersions(anyLong(), anyInt());
        verify(appVersionService, never()).fetchAppVersion(anyLong(), anyString());

        assertThat(appDeploymentFacade.getVersionsForInstance(APP_INSTANCE, 50)).isEmpty();
    }

    @Test
    void testAskingWhetherAProviderIsAvailableNeedsAnAuthenticatedCaller() {
        authenticateAnonymously();

        assertThatThrownBy(() -> appDeploymentFacade.isDeploymentProviderAvailable(AppHostingTarget.VERCEL))
            .isInstanceOf(AccessDeniedException.class);

        authenticate(OUTSIDER);

        assertThat(appDeploymentFacade.isDeploymentProviderAvailable(AppHostingTarget.VERCEL)).isFalse();
    }

    private AppDeploymentDTO recordDeployment(long appInstanceId) {
        return appDeploymentFacade.recordDeployment(appInstanceId, "1.0", "https://crm.example.com", "abc123");
    }
}

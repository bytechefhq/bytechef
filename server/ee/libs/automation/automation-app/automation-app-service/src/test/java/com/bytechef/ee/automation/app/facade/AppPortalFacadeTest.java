/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppFavorite;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppInstanceUsage;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.dto.AppIconDTO;
import com.bytechef.ee.automation.app.dto.PortalAppDTO;
import com.bytechef.ee.automation.app.dto.PortalAppInstanceDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.file.storage.AppFileStorage;
import com.bytechef.ee.automation.app.service.AppFavoriteService;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppInstanceUsageService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.AuthorizationDeniedException;

/**
 * The portal filter matrix against a small permission model: {@code APP_USE} per workspace and environment, instance
 * visibility per workspace (WORKSPACE, or PRIVATE plus a grant).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AppPortalFacadeTest {

    private static final long USER_ID = 7L;
    private static final long WORKSPACE_A = 1L;
    private static final long WORKSPACE_B = 2L;
    private static final long WORKSPACE_C = 3L;
    private static final byte[] PNG = {
        (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D
    };

    private final AppFavoriteService appFavoriteService = mock(AppFavoriteService.class);
    private final AppFileStorage appFileStorage = mock(AppFileStorage.class);
    private final AppInstanceService appInstanceService = mock(AppInstanceService.class);
    private final AppInstanceUsageService appInstanceUsageService = mock(AppInstanceUsageService.class);
    private final AppService appService = mock(AppService.class);
    private final CategoryService categoryService = mock(CategoryService.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final ResourceVisibilityResolver resourceVisibilityResolver = mock(ResourceVisibilityResolver.class);
    private final UserService userService = mock(UserService.class);
    private final WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

    private final List<App> apps = new ArrayList<>();
    private final List<AppInstance> appInstances = new ArrayList<>();
    private final Set<Long> grantedAppInstanceIds = new HashSet<>();
    private final Set<String> appUseScopes = new HashSet<>();

    private AppPortalFacade appPortalFacade;

    @BeforeEach
    void beforeEach() {
        User user = new User();

        user.setId(USER_ID);
        user.setLogin("member");

        when(userService.getCurrentUser()).thenReturn(user);
        when(workspaceFacade.getUserWorkspaces(USER_ID))
            .thenReturn(List.of(newWorkspace(WORKSPACE_A, "Alpha"), newWorkspace(WORKSPACE_B, "Beta")));

        when(permissionService.hasWorkspaceScope(anyLong(), anyString(), any(Environment.class)))
            .thenAnswer(invocation -> invocation.getArgument(1)
                .equals("APP_USE") &&
                appUseScopes.contains(scopeKey(invocation.getArgument(0), invocation.getArgument(2))));
        when(resourceVisibilityResolver.filterVisibleIds(eq("AppInstance"), anyLong(), anyCollection()))
            .thenAnswer(invocation -> {
                Collection<VisibilityRecord> visibilityRecords = invocation.getArgument(2);

                return visibilityRecords.stream()
                    .filter(visibilityRecord -> visibilityRecord.visibility() == ResourceVisibility.WORKSPACE ||
                        grantedAppInstanceIds.contains(visibilityRecord.id()))
                    .map(VisibilityRecord::id)
                    .collect(Collectors.toSet());
            });

        when(appInstanceService.getWorkspaceAppInstances(anyLong(), any(Environment.class)))
            .thenAnswer(invocation -> appInstances.stream()
                .filter(appInstance -> appInstance.getWorkspaceId() == (long) invocation.getArgument(0) &&
                    appInstance.getEnvironment() == invocation.getArgument(1))
                .toList());
        when(appInstanceService.getAppInstances(anyLong()))
            .thenAnswer(invocation -> appInstances.stream()
                .filter(appInstance -> appInstance.getAppId() == (long) invocation.getArgument(0))
                .toList());
        when(appInstanceService.fetchAppInstance(anyLong()))
            .thenAnswer(invocation -> appInstances.stream()
                .filter(appInstance -> appInstance.getId() == (long) invocation.getArgument(0))
                .findFirst());
        when(appService.getAppsByIds(anyCollection()))
            .thenAnswer(invocation -> {
                Collection<Long> ids = invocation.getArgument(0);

                return apps.stream()
                    .filter(app -> ids.contains(app.getId()))
                    .toList();
            });

        allowAppUse(WORKSPACE_A, Environment.PRODUCTION);
        allowAppUse(WORKSPACE_B, Environment.PRODUCTION);

        appPortalFacade = new AppPortalFacadeImpl(
            appFavoriteService, appFileStorage, appInstanceService, appInstanceUsageService, appService,
            categoryService, permissionService, resourceVisibilityResolver, userService, workspaceFacade);
    }

    @Test
    void testListsAnActiveDiscoverableAppWithAUsableInstance() {
        addApp(10L, "CRM", WORKSPACE_A, AppStatus.ACTIVE, true);
        addAppInstance(30L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);

        List<PortalAppDTO> portalApps = appPortalFacade.getPortalApps(Environment.PRODUCTION);

        assertThat(portalApps).hasSize(1);

        PortalAppDTO portalApp = portalApps.getFirst();

        assertThat(portalApp.id()).isEqualTo(10L);
        assertThat(portalApp.name()).isEqualTo("CRM");
        assertThat(portalApp.favorite()).isFalse();
        assertThat(portalApp.lastUsedDate()).isNull();
        assertThat(portalApp.instances()).containsExactly(
            new PortalAppInstanceDTO(30L, "instance-30", WORKSPACE_A, "Alpha", "https://30.example.com", null, 0));
    }

    @Test
    void testCategoryNamesOfThePortalAppsAreReadInOneLookup() {
        addApp(10L, "CRM", WORKSPACE_A, AppStatus.ACTIVE, true).setCategoryId(90L);
        addApp(11L, "HR", WORKSPACE_A, AppStatus.ACTIVE, true).setCategoryId(90L);
        addApp(12L, "Wiki", WORKSPACE_B, AppStatus.ACTIVE, true).setCategoryId(91L);
        addApp(13L, "Notes", WORKSPACE_B, AppStatus.ACTIVE, true);
        addApp(14L, "Hidden", WORKSPACE_A, AppStatus.ACTIVE, false).setCategoryId(92L);
        addAppInstance(30L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);
        addAppInstance(31L, 11L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);
        addAppInstance(32L, 12L, WORKSPACE_B, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);
        addAppInstance(33L, 13L, WORKSPACE_B, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);
        addAppInstance(34L, 14L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);

        when(categoryService.getCategories(argThat(ids -> ids != null && Set.copyOf(ids)
            .equals(Set.of(90L, 91L)))))
                .thenReturn(List.of(new Category(90L, "Sales"), new Category(91L, "Knowledge")));

        List<PortalAppDTO> portalApps = appPortalFacade.getPortalApps(Environment.PRODUCTION);

        assertThat(portalApps).extracting(PortalAppDTO::name, PortalAppDTO::categoryId, PortalAppDTO::categoryName)
            .containsExactly(
                tuple("CRM", 90L, "Sales"), tuple("HR", 90L, "Sales"), tuple("Notes", null, null),
                tuple("Wiki", 91L, "Knowledge"));

        verify(categoryService).getCategories(any());
    }

    @Test
    void testPortalWithoutCategorizedAppsReadsNoCategories() {
        addApp(10L, "CRM", WORKSPACE_A, AppStatus.ACTIVE, true);
        addApp(14L, "Hidden", WORKSPACE_A, AppStatus.ACTIVE, false).setCategoryId(92L);
        addAppInstance(30L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);
        addAppInstance(34L, 14L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);

        assertThat(appPortalFacade.getPortalApps(Environment.PRODUCTION)).extracting(PortalAppDTO::categoryName)
            .containsExactly((String) null);

        verify(categoryService, never()).getCategories(any());
    }

    @Test
    void testNotDiscoverableAppIsHidden() {
        addApp(10L, "CRM", WORKSPACE_A, AppStatus.ACTIVE, false);
        addAppInstance(30L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);

        assertThat(appPortalFacade.getPortalApps(Environment.PRODUCTION)).isEmpty();
    }

    @Test
    void testDraftSuspendedAndArchivedAppsAreHidden() {
        addApp(10L, "Draft", WORKSPACE_A, AppStatus.DRAFT, true);
        addApp(11L, "Suspended", WORKSPACE_A, AppStatus.SUSPENDED, true);
        addApp(12L, "Archived", WORKSPACE_A, AppStatus.ARCHIVED, true);
        addAppInstance(30L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);
        addAppInstance(31L, 11L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);
        addAppInstance(32L, 12L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);

        assertThat(appPortalFacade.getPortalApps(Environment.PRODUCTION)).isEmpty();
    }

    @Test
    void testAppWithoutAUsableInstanceIsHidden() {
        addApp(10L, "CRM", WORKSPACE_A, AppStatus.ACTIVE, true);
        addApp(11L, "HR", WORKSPACE_A, AppStatus.ACTIVE, true);

        addAppInstance(30L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE)
            .setStatus(AppInstanceStatus.SUSPENDED);
        addAppInstance(31L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE)
            .setStatus(AppInstanceStatus.TERMINATED);
        addAppInstance(32L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.PRIVATE);
        addAppInstance(33L, 11L, WORKSPACE_C, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);

        assertThat(appPortalFacade.getPortalApps(Environment.PRODUCTION)).isEmpty();
    }

    @Test
    void testInstanceWithoutUrlIsHidden() {
        addApp(10L, "CRM", WORKSPACE_A, AppStatus.ACTIVE, true);
        addAppInstance(30L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE).setUrl(null);
        addAppInstance(31L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE).setUrl(" ");

        assertThat(appPortalFacade.getPortalApps(Environment.PRODUCTION)).isEmpty();
    }

    @Test
    void testInstanceWithoutSignInIsListedUnderTheSameRule() {
        addApp(10L, "CRM", WORKSPACE_A, AppStatus.ACTIVE, true);
        addAppInstance(30L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE)
            .setUserAuthMode(AppUserAuthMode.NONE);
        addAppInstance(31L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.PRIVATE)
            .setUserAuthMode(AppUserAuthMode.NONE);
        addAppInstance(32L, 10L, WORKSPACE_A, Environment.STAGING, ResourceVisibility.WORKSPACE)
            .setUserAuthMode(AppUserAuthMode.NONE);

        assertThat(getInstanceIds(appPortalFacade.getPortalApps(Environment.PRODUCTION))).containsExactly(30L);
        assertThat(appPortalFacade.getPortalApps(Environment.STAGING)).isEmpty();

        grantedAppInstanceIds.add(31L);

        assertThat(getInstanceIds(appPortalFacade.getPortalApps(Environment.PRODUCTION)))
            .containsExactlyInAnyOrder(30L, 31L);
    }

    @Test
    void testPrivateInstanceAppearsOnlyWithAGrant() {
        addApp(10L, "CRM", WORKSPACE_A, AppStatus.ACTIVE, true);
        addAppInstance(30L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.PRIVATE);
        addAppInstance(31L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.PRIVATE);

        assertThat(appPortalFacade.getPortalApps(Environment.PRODUCTION)).isEmpty();

        grantedAppInstanceIds.add(31L);

        assertThat(getInstanceIds(appPortalFacade.getPortalApps(Environment.PRODUCTION))).containsExactly(31L);
    }

    @Test
    void testInstanceOfAnotherEnvironmentIsHidden() {
        allowAppUse(WORKSPACE_A, Environment.DEVELOPMENT);

        addApp(10L, "CRM", WORKSPACE_A, AppStatus.ACTIVE, true);
        addAppInstance(30L, 10L, WORKSPACE_A, Environment.DEVELOPMENT, ResourceVisibility.WORKSPACE);
        addAppInstance(31L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);

        assertThat(getInstanceIds(appPortalFacade.getPortalApps(Environment.PRODUCTION))).containsExactly(31L);
        assertThat(getInstanceIds(appPortalFacade.getPortalApps(Environment.DEVELOPMENT))).containsExactly(30L);
        assertThat(appPortalFacade.getPortalApps(Environment.STAGING)).isEmpty();
    }

    @Test
    void testMemberOfTwoWorkspacesSeesTheInstancesOfBothUnderOneCard() {
        App foreignApp = addApp(10L, "CRM", WORKSPACE_C, AppStatus.ACTIVE, true);

        foreignApp.setDescription("Customers");
        foreignApp.setCategoryId(5L);

        addAppInstance(30L, 10L, WORKSPACE_B, Environment.PRODUCTION, ResourceVisibility.WORKSPACE)
            .setName("Germany");
        addAppInstance(31L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE)
            .setName("Croatia");
        addAppInstance(32L, 10L, WORKSPACE_C, Environment.PRODUCTION, ResourceVisibility.WORKSPACE)
            .setName("Headquarters");

        List<PortalAppDTO> portalApps = appPortalFacade.getPortalApps(Environment.PRODUCTION);

        assertThat(portalApps).hasSize(1);

        PortalAppDTO portalApp = portalApps.getFirst();

        assertThat(portalApp.description()).isEqualTo("Customers");
        assertThat(portalApp.categoryId()).isEqualTo(5L);
        assertThat(portalApp.instances()).extracting(PortalAppInstanceDTO::id, PortalAppInstanceDTO::workspaceName)
            .containsExactly(
                tuple(31L, "Alpha"), tuple(30L, "Beta"));

        verify(appInstanceService, never()).getWorkspaceAppInstances(eq(WORKSPACE_C), any());
        verify(permissionService, never()).hasWorkspaceScope(eq(WORKSPACE_C), anyString(), any(Environment.class));
    }

    @Test
    void testUserWithoutAppUseSeesNothing() {
        appUseScopes.clear();

        addApp(10L, "CRM", WORKSPACE_A, AppStatus.ACTIVE, true);
        addAppInstance(30L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);
        addAppInstance(31L, 10L, WORKSPACE_B, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);

        grantedAppInstanceIds.add(30L);

        assertThat(appPortalFacade.getPortalApps(Environment.PRODUCTION)).isEmpty();

        verify(resourceVisibilityResolver, never()).filterVisibleIds(anyString(), anyLong(), anyCollection());
    }

    @Test
    void testAppUseInOneWorkspaceOnlyShowsThatWorkspacesInstances() {
        appUseScopes.remove(scopeKey(WORKSPACE_B, Environment.PRODUCTION));

        addApp(10L, "CRM", WORKSPACE_A, AppStatus.ACTIVE, true);
        addAppInstance(30L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);
        addAppInstance(31L, 10L, WORKSPACE_B, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);

        assertThat(getInstanceIds(appPortalFacade.getPortalApps(Environment.PRODUCTION))).containsExactly(30L);
    }

    @Test
    void testFavoriteAndUsageOfTheUsableInstancesOnly() {
        addApp(10L, "CRM", WORKSPACE_A, AppStatus.ACTIVE, true);
        addApp(11L, "Archive", WORKSPACE_A, AppStatus.ACTIVE, true);
        addAppInstance(30L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);
        addAppInstance(31L, 10L, WORKSPACE_B, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);
        addAppInstance(32L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.PRIVATE);
        addAppInstance(33L, 11L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);

        Instant earlier = Instant.parse("2026-11-01T10:00:00Z");
        Instant later = Instant.parse("2026-11-02T10:00:00Z");

        when(appFavoriteService.getFavorites(eq(USER_ID), anyCollection()))
            .thenReturn(List.of(new AppFavorite(USER_ID, 10L, earlier)));
        when(appInstanceUsageService.getUsages(eq(USER_ID), anyCollection()))
            .thenReturn(
                List.of(new AppInstanceUsage(USER_ID, 30L, earlier, 3), new AppInstanceUsage(USER_ID, 31L, later, 1)));

        List<PortalAppDTO> portalApps = appPortalFacade.getPortalApps(Environment.PRODUCTION);

        assertThat(portalApps).extracting(PortalAppDTO::name)
            .containsExactly("Archive", "CRM");

        PortalAppDTO crm = portalApps.get(1);

        assertThat(crm.favorite()).isTrue();
        assertThat(crm.lastUsedDate()).isEqualTo(later);
        assertThat(crm.instances()).extracting(PortalAppInstanceDTO::useCount)
            .containsExactly(3L, 1L);
        assertThat(portalApps.getFirst()
            .favorite()).isFalse();

        verify(appFavoriteService).getFavorites(USER_ID, Set.of(10L, 11L));
        verify(appInstanceUsageService).getUsages(
            eq(USER_ID), argThat(appInstanceIds -> Set.copyOf(appInstanceIds)
                .equals(Set.of(30L, 31L, 33L))));
    }

    @Test
    void testFavoriteOfAnAppThatLeftThePortalDoesNotBringItsCardBack() {
        addApp(10L, "CRM", WORKSPACE_A, AppStatus.ACTIVE, true);
        addApp(11L, "Suspended", WORKSPACE_A, AppStatus.SUSPENDED, true);
        addApp(12L, "Hidden", WORKSPACE_A, AppStatus.ACTIVE, false);
        addAppInstance(30L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);
        addAppInstance(31L, 11L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);
        addAppInstance(32L, 12L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);

        Instant createdDate = Instant.parse("2026-11-01T10:00:00Z");

        when(appFavoriteService.getFavorites(eq(USER_ID), anyCollection())).thenReturn(
            List.of(
                new AppFavorite(USER_ID, 10L, createdDate), new AppFavorite(USER_ID, 11L, createdDate),
                new AppFavorite(USER_ID, 12L, createdDate)));

        List<PortalAppDTO> portalApps = appPortalFacade.getPortalApps(Environment.PRODUCTION);

        assertThat(portalApps).extracting(PortalAppDTO::id)
            .containsExactly(10L);
        assertThat(portalApps.getFirst()
            .favorite()).isTrue();

        verify(appFavoriteService).getFavorites(USER_ID, Set.of(10L));
    }

    @Test
    void testSetFavoriteOnAPortalAppStoresIt() {
        addApp(10L, "CRM", WORKSPACE_C, AppStatus.ACTIVE, true);
        addAppInstance(30L, 10L, WORKSPACE_B, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);

        appPortalFacade.setAppFavorite(10L, true);

        verify(appFavoriteService).addFavorite(USER_ID, 10L);
    }

    @Test
    void testSetFavoriteOutsideThePortalIsRefusedLikeAnUnknownApp() {
        addApp(10L, "Hidden", WORKSPACE_A, AppStatus.ACTIVE, false);
        addAppInstance(30L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);
        addApp(11L, "Private", WORKSPACE_A, AppStatus.ACTIVE, true);
        addAppInstance(31L, 11L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.PRIVATE);
        addApp(12L, "Elsewhere", WORKSPACE_C, AppStatus.ACTIVE, true);
        addAppInstance(32L, 12L, WORKSPACE_C, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);

        for (long appId : List.of(10L, 11L, 12L)) {
            ConfigurationException refusal = refusal(() -> appPortalFacade.setAppFavorite(appId, true));
            ConfigurationException unknown = refusal(() -> appPortalFacade.setAppFavorite(404L, true));

            assertThat(refusal.getErrorKey()).isEqualTo(AppErrorType.APP_NOT_FOUND.getErrorKey());
            assertThat(refusal.getErrorKey()).isEqualTo(unknown.getErrorKey());
            assertThat(refusal.getMessage()).isEqualTo(unknown.getMessage()
                .replace("404", String.valueOf(appId)));
        }

        verify(appFavoriteService, never()).addFavorite(anyLong(), anyLong());
    }

    @Test
    void testPortalAppIconIsServedForAnAppInThePortal() {
        FileEntry icon = new FileEntry("icon.png", "base64:///app_icons/crm/icon.png");

        addApp(10L, "CRM", WORKSPACE_C, AppStatus.ACTIVE, true).setIcon(icon);
        addAppInstance(30L, 10L, WORKSPACE_B, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);

        when(appFileStorage.readIcon(icon)).thenReturn(PNG);

        Optional<AppIconDTO> appIcon = appPortalFacade.getPortalAppIcon(10L);

        assertThat(appIcon).isPresent();
        assertThat(appIcon.get()
            .content()).isEqualTo(PNG);
        assertThat(appIcon.get()
            .contentType()).isEqualTo("image/png");
    }

    @Test
    void testPortalAppIconOfAnAppInThePortalWithoutIconIsEmpty() {
        addApp(10L, "CRM", WORKSPACE_A, AppStatus.ACTIVE, true);
        addAppInstance(30L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);

        assertThat(appPortalFacade.getPortalAppIcon(10L)).isEmpty();

        verify(appFileStorage, never()).readIcon(any());
    }

    @Test
    void testPortalAppIconOutsideThePortalAnswersLikeAnUnknownApp() {
        FileEntry icon = new FileEntry("icon.png", "base64:///app_icons/crm/icon.png");

        addApp(10L, "Hidden", WORKSPACE_A, AppStatus.ACTIVE, false).setIcon(icon);
        addAppInstance(30L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);
        addApp(11L, "Private", WORKSPACE_A, AppStatus.ACTIVE, true).setIcon(icon);
        addAppInstance(31L, 11L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.PRIVATE);
        addApp(12L, "Elsewhere", WORKSPACE_C, AppStatus.ACTIVE, true).setIcon(icon);
        addAppInstance(32L, 12L, WORKSPACE_C, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);
        addApp(13L, "Suspended", WORKSPACE_A, AppStatus.SUSPENDED, true).setIcon(icon);
        addAppInstance(33L, 13L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);

        when(appFileStorage.readIcon(icon)).thenReturn(PNG);

        Optional<AppIconDTO> unknown = appPortalFacade.getPortalAppIcon(404L);

        assertThat(unknown).isEmpty();

        for (long appId : List.of(10L, 11L, 12L, 13L)) {
            assertThat(appPortalFacade.getPortalAppIcon(appId)).isEqualTo(unknown);
        }

        verify(appFileStorage, never()).readIcon(any());
    }

    @Test
    void testPortalAppIconIsServedWhenTheAppIsInThePortalOnlyInAnotherEnvironment() {
        allowAppUse(WORKSPACE_A, Environment.DEVELOPMENT);

        FileEntry icon = new FileEntry("icon.png", "base64:///app_icons/crm/icon.png");

        addApp(10L, "CRM", WORKSPACE_A, AppStatus.ACTIVE, true).setIcon(icon);
        addAppInstance(30L, 10L, WORKSPACE_A, Environment.DEVELOPMENT, ResourceVisibility.WORKSPACE);

        when(appFileStorage.readIcon(icon)).thenReturn(PNG);

        assertThat(appPortalFacade.getPortalApps(Environment.PRODUCTION)).isEmpty();
        assertThat(appPortalFacade.getPortalAppIcon(10L)).isPresent();
    }

    @Test
    void testUnsetFavoriteDeletesTheRowWithoutAnyLookup() {
        appPortalFacade.setAppFavorite(404L, false);

        verify(appFavoriteService).removeFavorite(USER_ID, 404L);
        verify(appInstanceService, never()).getAppInstances(anyLong());
    }

    @Test
    void testRecordLaunchOfAPortalInstanceRecordsIt() {
        addApp(10L, "CRM", WORKSPACE_A, AppStatus.ACTIVE, true);
        addAppInstance(30L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);

        appPortalFacade.recordAppLaunch(30L);

        verify(appInstanceUsageService).recordLaunch(USER_ID, 30L);
    }

    @Test
    void testRecordLaunchOfAnInstanceOutsideThePortalIsRefused() {
        addApp(10L, "Draft", WORKSPACE_A, AppStatus.DRAFT, true);
        addAppInstance(30L, 10L, WORKSPACE_A, Environment.PRODUCTION, ResourceVisibility.WORKSPACE);

        for (long appInstanceId : List.of(30L, 404L)) {
            Throwable refusal = catchThrowable(() -> appPortalFacade.recordAppLaunch(appInstanceId));

            assertThat(refusal).isExactlyInstanceOf(AuthorizationDeniedException.class)
                .hasMessage("Access Denied");
        }

        verify(appInstanceUsageService, never()).recordLaunch(anyLong(), anyLong());
    }

    @Test
    void testGuards() throws NoSuchMethodException {
        assertThat(getPreAuthorize("getPortalApps", Environment.class)).isEqualTo("isAuthenticated()");
        assertThat(getPreAuthorize("getPortalAppIcon", long.class)).isEqualTo("isAuthenticated()");
        assertThat(getPreAuthorize("setAppFavorite", long.class, boolean.class)).isEqualTo("isAuthenticated()");
        assertThat(getPreAuthorize("recordAppLaunch", long.class))
            .isEqualTo("hasPermission(#appInstanceId, 'AppInstance', 'APP_USE')");
    }

    private App addApp(long id, String name, long workspaceId, AppStatus status, boolean portalDiscoverable) {
        App app = new App();

        app.setId(id);
        app.setName(name);
        app.setWorkspaceId(workspaceId);
        app.setStatus(status);
        app.setPortalDiscoverable(portalDiscoverable);
        app.setVisibility(ResourceVisibility.WORKSPACE);

        apps.add(app);

        return app;
    }

    private AppInstance addAppInstance(
        long id, long appId, long workspaceId, Environment environment, ResourceVisibility visibility) {

        AppInstance appInstance = new AppInstance();

        appInstance.setId(id);
        appInstance.setAppId(appId);
        appInstance.setName("instance-" + id);
        appInstance.setWorkspaceId(workspaceId);
        appInstance.setEnvironment(environment);
        appInstance.setStatus(AppInstanceStatus.ACTIVE);
        appInstance.setVisibility(visibility);
        appInstance.setHostingTarget(AppHostingTarget.VERCEL);
        appInstance.setUrl("https://" + id + ".example.com");
        appInstance.setCreatedBy("owner");

        appInstances.add(appInstance);

        return appInstance;
    }

    private void allowAppUse(long workspaceId, Environment environment) {
        appUseScopes.add(scopeKey(workspaceId, environment));
    }

    private static List<Long> getInstanceIds(List<PortalAppDTO> portalApps) {
        return portalApps.stream()
            .flatMap(portalApp -> portalApp.instances()
                .stream())
            .map(PortalAppInstanceDTO::id)
            .toList();
    }

    private static String getPreAuthorize(String methodName, Class<?>... parameterTypes)
        throws NoSuchMethodException {

        Method method = AppPortalFacadeImpl.class.getMethod(methodName, parameterTypes);

        PreAuthorize preAuthorize = Objects.requireNonNull(method.getAnnotation(PreAuthorize.class));

        return preAuthorize.value();
    }

    private static Workspace newWorkspace(long id, String name) {
        Workspace workspace = new Workspace();

        workspace.setId(id);
        workspace.setName(name);

        return workspace;
    }

    private static ConfigurationException refusal(Runnable runnable) {
        Throwable throwable = catchThrowable(runnable::run);

        assertThat(throwable).isInstanceOf(ConfigurationException.class);

        return (ConfigurationException) throwable;
    }

    private static String scopeKey(long workspaceId, Environment environment) {
        return workspaceId + "@" + environment;
    }
}

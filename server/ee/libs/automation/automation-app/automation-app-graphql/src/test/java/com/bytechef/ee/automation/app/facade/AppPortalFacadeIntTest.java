/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.DEVELOPMENT_ADMIN;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.EDITOR;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.OUTSIDER;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.PRODUCTION_ONLY_VIEWER;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.TENANT_ADMIN;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.TWO_WORKSPACE_ADMIN;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.VIEWER;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.WORKSPACE_A;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.WORKSPACE_ADMIN;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.WORKSPACE_B;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.authenticate;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.authenticateAnonymously;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.userId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppFavorite;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppKeyAccess;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.dto.AppIconDTO;
import com.bytechef.ee.automation.app.dto.PortalAppDTO;
import com.bytechef.ee.automation.app.dto.PortalAppInstanceDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.file.storage.AppFileStorage;
import com.bytechef.ee.automation.app.security.AppInstanceClientAccessChecker;
import com.bytechef.ee.automation.app.service.AppFavoriteService;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppInstanceUsageService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.service.CustomRoleScopeResolver;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs the App Portal against the real EE {@code PermissionServiceImpl}, visibility resolver and AppInstance resolvers
 * (see {@link AppAuthorizationTestConfiguration}) and holds it to the per-instance check that sign-in applies,
 * {@link AppInstanceClientAccessChecker}: for every user, environment and instance of the matrix below, the portal
 * lists the instance exactly when sign-in would let that user in and the portal's own conditions hold (an
 * {@code ACTIVE}, discoverable app, an instance with a URL, the asked environment).
 *
 * <p>
 * Apps 40 to 46 and instances 50 to 63 are this test's own; workspace C ({@link #WORKSPACE_C}) has no member but the
 * tenant admin. Every instance requires sign-in and has its own OAuth2 client.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AppAuthorizationTestConfiguration.class, properties = "bytechef.edition=ee")
class AppPortalFacadeIntTest {

    private static final long WORKSPACE_C = 3L;
    private static final byte[] PNG = {
        (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D
    };

    private static final long DISCOVERABLE_APP = 40L;
    private static final long HIDDEN_APP = 41L;
    private static final long DRAFT_APP = 42L;
    private static final long SUSPENDED_APP = 43L;
    private static final long ARCHIVED_APP = 44L;
    private static final long FOREIGN_APP = 45L;
    private static final long PRIVATE_ONLY_APP = 46L;
    private static final long NO_SIGN_IN_APP = 47L;

    private static final long WORKSPACE_INSTANCE = 50L;
    private static final long PRIVATE_INSTANCE = 51L;
    private static final long DEVELOPMENT_INSTANCE = 52L;
    private static final long INSTANCE_IN_B = 53L;
    private static final long SUSPENDED_INSTANCE = 54L;
    private static final long INSTANCE_WITHOUT_URL = 55L;
    private static final long TERMINATED_INSTANCE = 56L;
    private static final long HIDDEN_APP_INSTANCE = 57L;
    private static final long DRAFT_APP_INSTANCE = 58L;
    private static final long SUSPENDED_APP_INSTANCE = 59L;
    private static final long ARCHIVED_APP_INSTANCE = 60L;
    private static final long FOREIGN_APP_INSTANCE_IN_B = 61L;
    private static final long FOREIGN_APP_INSTANCE_IN_C = 62L;
    private static final long PRIVATE_ONLY_INSTANCE = 63L;
    private static final long NO_SIGN_IN_INSTANCE = 64L;
    private static final long PRIVATE_NO_SIGN_IN_INSTANCE = 65L;
    private static final long UNKNOWN_INSTANCE = 404L;
    private static final long CUSTOM_ROLE = 900L;

    private static final List<String> USERS = List.of(
        VIEWER, TWO_WORKSPACE_ADMIN, OUTSIDER, DEVELOPMENT_ADMIN, WORKSPACE_ADMIN, PRODUCTION_ONLY_VIEWER,
        TENANT_ADMIN);

    private static final Map<String, List<Long>> USER_WORKSPACES = Map.of(
        VIEWER, List.of(WORKSPACE_A),
        TWO_WORKSPACE_ADMIN, List.of(WORKSPACE_A, WORKSPACE_B),
        OUTSIDER, List.of(WORKSPACE_B),
        DEVELOPMENT_ADMIN, List.of(WORKSPACE_A),
        EDITOR, List.of(WORKSPACE_A),
        WORKSPACE_ADMIN, List.of(WORKSPACE_A, WORKSPACE_B),
        PRODUCTION_ONLY_VIEWER, List.of(WORKSPACE_A),
        TENANT_ADMIN, List.of(WORKSPACE_A, WORKSPACE_B, WORKSPACE_C));

    private static final Map<Long, String> WORKSPACE_NAMES = Map.of(
        WORKSPACE_A, "Alpha", WORKSPACE_B, "Beta", WORKSPACE_C, "Gamma");

    @Autowired
    private AppFavoriteService appFavoriteService;

    @Autowired
    private AppFileStorage appFileStorage;

    @Autowired
    private AppInstanceClientAccessChecker appInstanceClientAccessChecker;

    @Autowired
    private AppInstanceService appInstanceService;

    @Autowired
    private AppInstanceUsageService appInstanceUsageService;

    @Autowired
    private AppPortalFacade appPortalFacade;

    @Autowired
    private AppService appService;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private CustomRoleScopeResolver customRoleScopeResolver;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ResourceGrantService resourceGrantService;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkspaceConnectionRepository workspaceConnectionRepository;

    @Autowired
    private WorkspaceFacade workspaceFacade;

    @Autowired
    private WorkspaceUserRepository workspaceUserRepository;

    @Autowired
    private WorkspaceUserService workspaceUserService;

    private final Map<Long, App> apps = new LinkedHashMap<>();
    private final Map<Long, AppInstance> appInstances = new LinkedHashMap<>();

    private Set<AppAuthorizationFixture.Grant> grants;

    @BeforeEach
    void beforeEach() {
        AppAuthorizationFixture.stubTenant(
            appInstanceService, appService, connectionService, userService, workspaceConnectionRepository,
            workspaceUserRepository);
        AppAuthorizationFixture.stubMembership(workspaceUserService);

        grants = AppAuthorizationFixture.stubGrants(resourceGrantService);

        when(userService.getCurrentUser()).thenAnswer(invocation -> newUser(currentLogin()));
        when(userService.fetchUserByLogin(anyString()))
            .thenAnswer(invocation -> Optional.of(newUser(invocation.getArgument(0))));
        when(workspaceFacade.getUserWorkspaces(anyLong()))
            .thenAnswer(invocation -> getUserWorkspaces(invocation.getArgument(0)));

        addApp(DISCOVERABLE_APP, WORKSPACE_A, AppStatus.ACTIVE, true);
        addApp(HIDDEN_APP, WORKSPACE_A, AppStatus.ACTIVE, false);
        addApp(DRAFT_APP, WORKSPACE_A, AppStatus.DRAFT, true);
        addApp(SUSPENDED_APP, WORKSPACE_A, AppStatus.SUSPENDED, true);
        addApp(ARCHIVED_APP, WORKSPACE_A, AppStatus.ARCHIVED, true);
        addApp(FOREIGN_APP, WORKSPACE_C, AppStatus.ACTIVE, true);
        addApp(PRIVATE_ONLY_APP, WORKSPACE_A, AppStatus.ACTIVE, true);

        addAppInstance(WORKSPACE_INSTANCE, DISCOVERABLE_APP, WORKSPACE_A, Environment.PRODUCTION, WORKSPACE_ADMIN);
        addAppInstance(PRIVATE_INSTANCE, DISCOVERABLE_APP, WORKSPACE_A, Environment.PRODUCTION, WORKSPACE_ADMIN)
            .setVisibility(ResourceVisibility.PRIVATE);
        addAppInstance(DEVELOPMENT_INSTANCE, DISCOVERABLE_APP, WORKSPACE_A, Environment.DEVELOPMENT, WORKSPACE_ADMIN);
        addAppInstance(INSTANCE_IN_B, DISCOVERABLE_APP, WORKSPACE_B, Environment.PRODUCTION, OUTSIDER);
        addAppInstance(SUSPENDED_INSTANCE, DISCOVERABLE_APP, WORKSPACE_A, Environment.PRODUCTION, WORKSPACE_ADMIN)
            .setStatus(AppInstanceStatus.SUSPENDED);
        addAppInstance(INSTANCE_WITHOUT_URL, DISCOVERABLE_APP, WORKSPACE_A, Environment.PRODUCTION, WORKSPACE_ADMIN)
            .setUrl(null);
        addAppInstance(TERMINATED_INSTANCE, DISCOVERABLE_APP, WORKSPACE_A, Environment.PRODUCTION, WORKSPACE_ADMIN)
            .setStatus(AppInstanceStatus.TERMINATED);
        addAppInstance(HIDDEN_APP_INSTANCE, HIDDEN_APP, WORKSPACE_A, Environment.PRODUCTION, WORKSPACE_ADMIN);
        addAppInstance(DRAFT_APP_INSTANCE, DRAFT_APP, WORKSPACE_A, Environment.PRODUCTION, WORKSPACE_ADMIN);
        addAppInstance(SUSPENDED_APP_INSTANCE, SUSPENDED_APP, WORKSPACE_A, Environment.PRODUCTION, WORKSPACE_ADMIN);
        addAppInstance(ARCHIVED_APP_INSTANCE, ARCHIVED_APP, WORKSPACE_A, Environment.PRODUCTION, WORKSPACE_ADMIN);
        addAppInstance(FOREIGN_APP_INSTANCE_IN_B, FOREIGN_APP, WORKSPACE_B, Environment.PRODUCTION, OUTSIDER);
        addAppInstance(FOREIGN_APP_INSTANCE_IN_C, FOREIGN_APP, WORKSPACE_C, Environment.PRODUCTION, TENANT_ADMIN);
        addAppInstance(PRIVATE_ONLY_INSTANCE, PRIVATE_ONLY_APP, WORKSPACE_A, Environment.PRODUCTION, WORKSPACE_ADMIN)
            .setVisibility(ResourceVisibility.PRIVATE);

        stubAppsAndInstances();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testPortalAgreesWithSignInOnEveryCaseOfTheMatrix() {
        addInstancesWithoutSignIn();

        grants.add(new AppAuthorizationFixture.Grant("AppInstance", PRIVATE_INSTANCE, userId(VIEWER)));

        List<String> disagreements = new ArrayList<>();
        int listedCount = 0;
        int listedWithoutSignInCount = 0;
        int signInOnlyCount = 0;

        for (String login : USERS) {
            for (Environment environment : List.of(Environment.DEVELOPMENT, Environment.PRODUCTION)) {
                authenticate(login);

                Set<Long> listed = Set.copyOf(getInstanceIds(appPortalFacade.getPortalApps(environment)));

                for (AppInstance appInstance : appInstances.values()) {
                    boolean signInAllowed;

                    if (appInstance.getUserAuthMode() == AppUserAuthMode.NONE) {
                        signInAllowed = isUsableWithoutSignIn(appInstance);
                    } else {
                        signInAllowed = appInstanceClientAccessChecker.isAllowed(
                            newRegisteredClient(appInstance), currentAuthentication());
                    }

                    boolean expected = signInAllowed && isPortalCandidate(appInstance, environment);
                    boolean actual = listed.contains(appInstance.getId());

                    if (expected != actual) {
                        disagreements.add(
                            "%s in %s: instance %d listed=%s, sign-in=%s".formatted(
                                login, environment, appInstance.getId(), actual, signInAllowed));
                    }

                    if (actual) {
                        listedCount++;
                    }

                    if (actual && appInstance.getUserAuthMode() == AppUserAuthMode.NONE) {
                        listedWithoutSignInCount++;
                    }

                    if (signInAllowed && !actual) {
                        signInOnlyCount++;
                    }
                }
            }
        }

        assertThat(disagreements).isEmpty();
        assertThat(listedCount).isPositive();
        assertThat(listedWithoutSignInCount).isPositive();
        assertThat(signInOnlyCount).isPositive();
    }

    @Test
    void testInstanceWithoutSignInIsListedUnderTheSameRule() {
        addInstancesWithoutSignIn();

        authenticate(VIEWER);

        assertThat(getInstanceIds(appPortalFacade.getPortalApps(Environment.PRODUCTION)))
            .contains(NO_SIGN_IN_INSTANCE)
            .doesNotContain(PRIVATE_NO_SIGN_IN_INSTANCE);

        grants.add(new AppAuthorizationFixture.Grant("AppInstance", PRIVATE_NO_SIGN_IN_INSTANCE, userId(VIEWER)));

        assertThat(getInstanceIds(appPortalFacade.getPortalApps(Environment.PRODUCTION)))
            .contains(NO_SIGN_IN_INSTANCE, PRIVATE_NO_SIGN_IN_INSTANCE);

        authenticate(WORKSPACE_ADMIN);

        assertThat(getInstanceIds(appPortalFacade.getPortalApps(Environment.PRODUCTION)))
            .contains(NO_SIGN_IN_INSTANCE, PRIVATE_NO_SIGN_IN_INSTANCE);

        authenticate(DEVELOPMENT_ADMIN);

        assertThat(appPortalFacade.getPortalApps(Environment.PRODUCTION)).isEmpty();
    }

    @Test
    void testMemberSeesTheWorkspaceInstanceOfAnActiveDiscoverableAppOnly() {
        authenticate(VIEWER);

        List<PortalAppDTO> portalApps = appPortalFacade.getPortalApps(Environment.PRODUCTION);

        assertThat(portalApps).extracting(PortalAppDTO::id)
            .containsExactly(DISCOVERABLE_APP);
        assertThat(getInstanceIds(portalApps)).containsExactly(WORKSPACE_INSTANCE);
    }

    @Test
    void testPrivateInstanceAppearsOnlyWithAGrant() {
        authenticate(VIEWER);

        assertThat(getInstanceIds(appPortalFacade.getPortalApps(Environment.PRODUCTION)))
            .doesNotContain(PRIVATE_INSTANCE, PRIVATE_ONLY_INSTANCE);

        grants.add(new AppAuthorizationFixture.Grant("AppInstance", PRIVATE_INSTANCE, userId(VIEWER)));

        assertThat(getInstanceIds(appPortalFacade.getPortalApps(Environment.PRODUCTION)))
            .containsExactlyInAnyOrder(WORKSPACE_INSTANCE, PRIVATE_INSTANCE);
    }

    @Test
    void testCreatorSeesTheirPrivateInstance() {
        authenticate(WORKSPACE_ADMIN);

        List<PortalAppDTO> portalApps = appPortalFacade.getPortalApps(Environment.PRODUCTION);

        assertThat(portalApps).extracting(PortalAppDTO::id)
            .containsExactlyInAnyOrder(DISCOVERABLE_APP, FOREIGN_APP, PRIVATE_ONLY_APP);
        assertThat(getInstanceIds(portalApps)).containsExactlyInAnyOrder(
            WORKSPACE_INSTANCE, PRIVATE_INSTANCE, INSTANCE_IN_B, FOREIGN_APP_INSTANCE_IN_B, PRIVATE_ONLY_INSTANCE);
    }

    @Test
    void testInstanceOfAnotherEnvironmentIsHidden() {
        authenticate(DEVELOPMENT_ADMIN);

        assertThat(appPortalFacade.getPortalApps(Environment.PRODUCTION)).isEmpty();
        assertThat(getInstanceIds(appPortalFacade.getPortalApps(Environment.DEVELOPMENT)))
            .containsExactly(DEVELOPMENT_INSTANCE);

        authenticate(PRODUCTION_ONLY_VIEWER);

        assertThat(appPortalFacade.getPortalApps(Environment.DEVELOPMENT)).isEmpty();
        assertThat(getInstanceIds(appPortalFacade.getPortalApps(Environment.PRODUCTION)))
            .containsExactly(WORKSPACE_INSTANCE);
    }

    @Test
    void testMemberOfTwoWorkspacesSeesBothUnderOneCardIncludingAForeignApp() {
        authenticate(TWO_WORKSPACE_ADMIN);

        List<PortalAppDTO> portalApps = appPortalFacade.getPortalApps(Environment.PRODUCTION);

        Map<Long, List<Long>> instanceIdsByAppId = portalApps.stream()
            .collect(Collectors.toMap(
                PortalAppDTO::id,
                portalApp -> portalApp.instances()
                    .stream()
                    .map(PortalAppInstanceDTO::id)
                    .toList()));

        assertThat(instanceIdsByAppId).containsOnlyKeys(DISCOVERABLE_APP, FOREIGN_APP);
        assertThat(instanceIdsByAppId.get(DISCOVERABLE_APP)).containsExactly(WORKSPACE_INSTANCE, INSTANCE_IN_B);
        assertThat(instanceIdsByAppId.get(FOREIGN_APP)).containsExactly(FOREIGN_APP_INSTANCE_IN_B);

        PortalAppDTO foreignApp = portalApps.stream()
            .filter(portalApp -> portalApp.id() == FOREIGN_APP)
            .findFirst()
            .orElseThrow();

        assertThat(foreignApp.instances()).extracting(PortalAppInstanceDTO::workspaceName)
            .containsExactly("Beta");
    }

    @Test
    void testUserWithoutAppUseSeesNothing() {
        authenticate(DEVELOPMENT_ADMIN);

        assertThat(appPortalFacade.getPortalApps(Environment.PRODUCTION)).isEmpty();
        assertThat(appPortalFacade.getPortalApps(Environment.STAGING)).isEmpty();
    }

    @Test
    void testMemberWhoseRoleLacksAppUseSeesNothing() {
        authenticate(EDITOR);

        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(userId(EDITOR), WORKSPACE_A))
            .thenReturn(Optional.of(WorkspaceUser.forCustomRole(userId(EDITOR), WORKSPACE_A, CUSTOM_ROLE)));
        when(customRoleScopeResolver.resolveScopes(CUSTOM_ROLE)).thenReturn(Optional.of(Set.of("APP_VIEW")));

        assertThat(appPortalFacade.getPortalApps(Environment.PRODUCTION)).isEmpty();
        assertThatThrownBy(() -> appPortalFacade.recordAppLaunch(WORKSPACE_INSTANCE))
            .isInstanceOf(AccessDeniedException.class);
        assertThat(refusal(() -> appPortalFacade.setAppFavorite(DISCOVERABLE_APP, true)).getMessage())
            .isEqualTo("App %d not found".formatted(DISCOVERABLE_APP));

        when(customRoleScopeResolver.resolveScopes(CUSTOM_ROLE))
            .thenReturn(Optional.of(Set.of("APP_VIEW", "APP_USE")));

        assertThat(getInstanceIds(appPortalFacade.getPortalApps(Environment.PRODUCTION)))
            .containsExactly(WORKSPACE_INSTANCE);

        verify(appInstanceUsageService, never()).recordLaunch(anyLong(), anyLong());
        verify(appFavoriteService, never()).addFavorite(anyLong(), anyLong());
    }

    @Test
    void testFavoriteOfAnAppThatLeftThePortalDoesNotBringItsCardBack() {
        authenticate(VIEWER);

        Instant createdDate = Instant.parse("2026-11-01T10:00:00Z");

        when(appFavoriteService.getFavorites(eq(userId(VIEWER)), anyCollection())).thenReturn(
            List.of(
                new AppFavorite(userId(VIEWER), DISCOVERABLE_APP, createdDate),
                new AppFavorite(userId(VIEWER), HIDDEN_APP, createdDate),
                new AppFavorite(userId(VIEWER), SUSPENDED_APP, createdDate)));

        List<PortalAppDTO> portalApps = appPortalFacade.getPortalApps(Environment.PRODUCTION);

        assertThat(portalApps).extracting(PortalAppDTO::id)
            .containsExactly(DISCOVERABLE_APP);
        assertThat(portalApps.getFirst()
            .favorite()).isTrue();

        verify(appFavoriteService).getFavorites(
            eq(userId(VIEWER)), argThat(appIds -> Set.copyOf(appIds)
                .equals(Set.of(DISCOVERABLE_APP))));
    }

    @Test
    void testAnonymousCallerIsRefused() {
        authenticateAnonymously();

        assertThatThrownBy(() -> appPortalFacade.getPortalApps(Environment.PRODUCTION))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> appPortalFacade.setAppFavorite(DISCOVERABLE_APP, true))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testFavoriteOutsideThePortalIsRefusedLikeAnUnknownApp() {
        authenticate(VIEWER);

        appPortalFacade.setAppFavorite(DISCOVERABLE_APP, true);

        verify(appFavoriteService).addFavorite(userId(VIEWER), DISCOVERABLE_APP);

        ConfigurationException unknown = refusal(() -> appPortalFacade.setAppFavorite(404L, true));

        for (long appId : List.of(HIDDEN_APP, DRAFT_APP, SUSPENDED_APP, ARCHIVED_APP, FOREIGN_APP, PRIVATE_ONLY_APP)) {
            ConfigurationException refusal = refusal(() -> appPortalFacade.setAppFavorite(appId, true));

            assertThat(refusal).isExactlyInstanceOf(unknown.getClass());
            assertThat(refusal.getErrorKey()).isEqualTo(AppErrorType.APP_NOT_FOUND.getErrorKey());
            assertThat(refusal.getErrorKey()).isEqualTo(unknown.getErrorKey());
            assertThat(refusal.getMessage()).isEqualTo("App %d not found".formatted(appId));
        }

        assertThat(unknown.getMessage()).isEqualTo("App 404 not found");

        verify(appFavoriteService, times(1)).addFavorite(anyLong(), anyLong());
    }

    @Test
    void testPortalAppIconIsServedForAnAppInThePortalAndAnswersLikeAnUnknownAppOtherwise() {
        FileEntry icon = new FileEntry("icon.png", "base64:///app_icons/portal/icon.png");

        for (App app : apps.values()) {
            app.setIcon(icon);
        }

        when(appFileStorage.readIcon(icon)).thenReturn(PNG);

        authenticate(VIEWER);

        Optional<AppIconDTO> appIcon = appPortalFacade.getPortalAppIcon(DISCOVERABLE_APP);

        assertThat(appIcon).isPresent();
        assertThat(appIcon.get()
            .content()).isEqualTo(PNG);
        assertThat(appIcon.get()
            .contentType()).isEqualTo("image/png");

        Optional<AppIconDTO> unknown = appPortalFacade.getPortalAppIcon(404L);

        assertThat(unknown).isEmpty();

        for (long appId : List.of(HIDDEN_APP, DRAFT_APP, SUSPENDED_APP, ARCHIVED_APP, FOREIGN_APP, PRIVATE_ONLY_APP)) {
            assertThat(appPortalFacade.getPortalAppIcon(appId)).isEqualTo(unknown);
        }

        grants.add(new AppAuthorizationFixture.Grant("AppInstance", PRIVATE_ONLY_INSTANCE, userId(VIEWER)));

        assertThat(appPortalFacade.getPortalAppIcon(PRIVATE_ONLY_APP)).isPresent();

        authenticate(OUTSIDER);

        assertThat(appPortalFacade.getPortalAppIcon(DISCOVERABLE_APP)).isPresent();
        assertThat(appPortalFacade.getPortalAppIcon(PRIVATE_ONLY_APP)).isEmpty();
    }

    @Test
    void testPortalAppIconNeedsAnAuthenticatedCaller() {
        authenticateAnonymously();

        assertThatThrownBy(() -> appPortalFacade.getPortalAppIcon(DISCOVERABLE_APP))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testLaunchNeedsAppUseOnTheInstanceAndAnUnknownInstanceAnswersTheSame() {
        authenticate(VIEWER);

        appPortalFacade.recordAppLaunch(WORKSPACE_INSTANCE);

        verify(appInstanceUsageService).recordLaunch(userId(VIEWER), WORKSPACE_INSTANCE);

        assertThatThrownBy(() -> appPortalFacade.recordAppLaunch(PRIVATE_INSTANCE))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> appPortalFacade.recordAppLaunch(INSTANCE_IN_B))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> appPortalFacade.recordAppLaunch(UNKNOWN_INSTANCE))
            .isInstanceOf(AccessDeniedException.class);

        verify(appInstanceUsageService, times(1)).recordLaunch(anyLong(), anyLong());
    }

    @Test
    void testLaunchOutsideThePortalAnswersExactlyLikeAnUnknownInstance() {
        authenticate(VIEWER);

        Throwable unknown = catchThrowable(() -> appPortalFacade.recordAppLaunch(UNKNOWN_INSTANCE));

        assertThat(unknown).isInstanceOf(AccessDeniedException.class);

        for (long appInstanceId : List.of(
            HIDDEN_APP_INSTANCE, SUSPENDED_APP_INSTANCE, INSTANCE_WITHOUT_URL, DRAFT_APP_INSTANCE,
            ARCHIVED_APP_INSTANCE, SUSPENDED_INSTANCE, TERMINATED_INSTANCE)) {

            Throwable refusal = catchThrowable(() -> appPortalFacade.recordAppLaunch(appInstanceId));

            assertThat(refusal).as("instance %d", appInstanceId)
                .isExactlyInstanceOf(unknown.getClass())
                .hasMessage(unknown.getMessage());
        }

        verify(appInstanceUsageService, never()).recordLaunch(anyLong(), anyLong());
    }

    private void addInstancesWithoutSignIn() {
        addApp(NO_SIGN_IN_APP, WORKSPACE_A, AppStatus.ACTIVE, true);

        addAppInstance(NO_SIGN_IN_INSTANCE, NO_SIGN_IN_APP, WORKSPACE_A, Environment.PRODUCTION, WORKSPACE_ADMIN)
            .setUserAuthMode(AppUserAuthMode.NONE);

        AppInstance privateAppInstance = addAppInstance(
            PRIVATE_NO_SIGN_IN_INSTANCE, NO_SIGN_IN_APP, WORKSPACE_A, Environment.PRODUCTION, WORKSPACE_ADMIN);

        privateAppInstance.setUserAuthMode(AppUserAuthMode.NONE);
        privateAppInstance.setVisibility(ResourceVisibility.PRIVATE);

        stubAppsAndInstances();
    }

    private App addApp(long id, long workspaceId, AppStatus status, boolean portalDiscoverable) {
        App app = AppAuthorizationFixture.newApp(id, ResourceVisibility.WORKSPACE);

        app.setWorkspaceId(workspaceId);
        app.setStatus(status);
        app.setPortalDiscoverable(portalDiscoverable);

        apps.put(id, app);

        return app;
    }

    private AppInstance addAppInstance(
        long id, long appId, long workspaceId, Environment environment, String ownerLogin) {

        AppInstance appInstance = new AppInstance();

        appInstance.setId(id);
        appInstance.setUuid("uuid-" + id);
        appInstance.setAppId(appId);
        appInstance.setName("instance-" + id);
        appInstance.setWorkspaceId(workspaceId);
        appInstance.setEnvironment(environment);
        appInstance.setOwnerUserId(userId(ownerLogin));
        appInstance.setStatus(AppInstanceStatus.ACTIVE);
        appInstance.setVisibility(ResourceVisibility.WORKSPACE);
        appInstance.setHostingTarget(AppHostingTarget.VERCEL);
        appInstance.setUrl("https://" + id + ".example.com");
        appInstance.setUserAuthMode(AppUserAuthMode.REQUIRED);
        appInstance.setRegisteredClientId("app-uuid-" + id);
        appInstance.setCreatedBy(ownerLogin);

        appInstances.put(id, appInstance);

        return appInstance;
    }

    private static String currentLogin() {
        return currentAuthentication().getName();
    }

    private static Authentication currentAuthentication() {
        return Objects.requireNonNull(
            SecurityContextHolder.getContext()
                .getAuthentication());
    }

    private static List<Long> getInstanceIds(List<PortalAppDTO> portalApps) {
        return portalApps.stream()
            .flatMap(portalApp -> portalApp.instances()
                .stream())
            .map(PortalAppInstanceDTO::id)
            .toList();
    }

    private static List<Workspace> getUserWorkspaces(long userId) {
        List<Workspace> workspaces = new ArrayList<>();

        for (Map.Entry<String, List<Long>> entry : USER_WORKSPACES.entrySet()) {
            if (userId(entry.getKey()) != userId) {
                continue;
            }

            for (long workspaceId : entry.getValue()) {
                Workspace workspace = new Workspace();

                workspace.setId(workspaceId);
                workspace.setName(WORKSPACE_NAMES.get(workspaceId));

                workspaces.add(workspace);
            }
        }

        return workspaces;
    }

    private boolean isPortalCandidate(AppInstance appInstance, Environment environment) {
        App app = apps.get(appInstance.getAppId());

        return app.getStatus() == AppStatus.ACTIVE && app.isPortalDiscoverable() && appInstance.getUrl() != null &&
            appInstance.getEnvironment() == environment;
    }

    private boolean isUsableWithoutSignIn(AppInstance appInstance) {
        App app = apps.get(appInstance.getAppId());

        return AppKeyAccess.isAllowed(app.getStatus(), appInstance.getStatus()) &&
            permissionService.hasResourceScope(appInstance.getId(), "AppInstance", "APP_USE");
    }

    private static RegisteredClient newRegisteredClient(AppInstance appInstance) {
        return RegisteredClient.withId("registered-" + appInstance.getId())
            .clientId(Objects.requireNonNull(appInstance.getRegisteredClientId()))
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("https://" + appInstance.getId() + ".example.com/callback")
            .scope("app:use")
            .build();
    }

    private static User newUser(String login) {
        User user = new User();

        user.setActivated(true);
        user.setId(userId(login));
        user.setLogin(login);

        return user;
    }

    private static ConfigurationException refusal(Runnable runnable) {
        Throwable throwable = catchThrowable(runnable::run);

        assertThat(throwable).isInstanceOf(ConfigurationException.class);

        return (ConfigurationException) throwable;
    }

    private void stubAppsAndInstances() {
        for (App app : apps.values()) {
            when(appService.fetchApp(app.getId())).thenReturn(Optional.of(app));
            when(appService.getApp(app.getId())).thenReturn(app);
            when(appInstanceService.getAppInstances(app.getId())).thenReturn(
                appInstances.values()
                    .stream()
                    .filter(appInstance -> Objects.equals(appInstance.getAppId(), app.getId()))
                    .toList());
        }

        when(appService.getAppsByIds(anyCollection())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);

            return ids.stream()
                .map(apps::get)
                .filter(Objects::nonNull)
                .toList();
        });

        for (AppInstance appInstance : appInstances.values()) {
            when(appInstanceService.fetchAppInstance(appInstance.getId())).thenReturn(Optional.of(appInstance));
            when(appInstanceService.getAppInstance(appInstance.getId())).thenReturn(appInstance);
            when(appInstanceService.fetchAppInstanceByUuid(appInstance.getUuid()))
                .thenReturn(Optional.of(appInstance));
        }

        when(appInstanceService.getWorkspaceAppInstances(anyLong(), any(Environment.class)))
            .thenAnswer(invocation -> {
                long workspaceId = invocation.getArgument(0);
                Environment environment = invocation.getArgument(1);

                return appInstances.values()
                    .stream()
                    .filter(appInstance -> appInstance.getWorkspaceId() == workspaceId &&
                        appInstance.getEnvironment() == environment)
                    .toList();
            });
    }
}

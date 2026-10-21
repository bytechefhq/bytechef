/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.exception.UserNotFoundException;
import com.bytechef.platform.user.service.UserService;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The tenant the app authorization tests run against. Workspace A holds the apps and connection {@link #CONNECTION_A};
 * workspace B holds {@link #CONNECTION_B}. Roles are workspace rows, resolved by the real scope registry:
 *
 * <ul>
 * <li>{@link #VIEWER} is VIEWER in A;</li>
 * <li>{@link #EDITOR} is EDITOR in A;</li>
 * <li>{@link #WORKSPACE_ADMIN} is ADMIN in A and VIEWER in B, and created both apps;</li>
 * <li>{@link #PRODUCTION_VIEWER} is ADMIN in A, but VIEWER in A's PRODUCTION environment;</li>
 * <li>{@link #OUTSIDER} is ADMIN in B and not a member of A;</li>
 * <li>{@link #TENANT_ADMIN} holds {@code ROLE_ADMIN} and no workspace row at all;</li>
 * <li>{@link #TWO_WORKSPACE_ADMIN} is ADMIN in A and in B;</li>
 * <li>{@link #DEVELOPMENT_ADMIN} holds only a DEVELOPMENT row in A, as ADMIN, so it has no role in A's other
 * environments;</li>
 * <li>{@link #PRODUCTION_ONLY_VIEWER} holds only a PRODUCTION row in A, as VIEWER;</li>
 * <li>{@link #APP_OWNER} is ADMIN in A and not a member of B.</li>
 * </ul>
 *
 * <p>
 * A user is a member of a workspace when it holds any row there.
 *
 * <p>
 * {@link #APP} has two instances: {@link #APP_INSTANCE} in A and {@link #FOREIGN_APP_INSTANCE} in B, owned by
 * {@link #OUTSIDER}. {@link #PRIVATE_APP_INSTANCE} is the private instance of {@link #PRIVATE_APP}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AppAuthorizationFixture {

    public static final long WORKSPACE_A = 1L;
    public static final long WORKSPACE_B = 2L;

    public static final long APP = 10L;
    public static final long PRIVATE_APP = 11L;

    public static final long APP_INSTANCE = 30L;
    public static final long PRIVATE_APP_INSTANCE = 31L;
    public static final long FOREIGN_APP_INSTANCE = 32L;

    public static final long CONNECTION_A = 20L;
    public static final long CONNECTION_B = 21L;

    public static final String VIEWER = "viewer";
    public static final String EDITOR = "editor";
    public static final String WORKSPACE_ADMIN = "workspace-admin";
    public static final String PRODUCTION_VIEWER = "production-viewer";
    public static final String OUTSIDER = "outsider";
    public static final String TENANT_ADMIN = "tenant-admin";
    public static final String TWO_WORKSPACE_ADMIN = "two-workspace-admin";
    public static final String DEVELOPMENT_ADMIN = "development-admin";
    public static final String PRODUCTION_ONLY_VIEWER = "production-only-viewer";
    public static final String APP_OWNER = "app-owner";

    private static final Map<String, Long> USER_IDS = Map.of(
        VIEWER, 101L, EDITOR, 102L, WORKSPACE_ADMIN, 103L, PRODUCTION_VIEWER, 104L, OUTSIDER, 105L, TENANT_ADMIN, 106L,
        TWO_WORKSPACE_ADMIN, 107L, DEVELOPMENT_ADMIN, 108L, PRODUCTION_ONLY_VIEWER, 109L, APP_OWNER, 110L);

    private static final Map<String, WorkspaceRole> IMPLICIT_ROLES = Map.of(
        key(VIEWER, WORKSPACE_A), WorkspaceRole.VIEWER,
        key(EDITOR, WORKSPACE_A), WorkspaceRole.EDITOR,
        key(WORKSPACE_ADMIN, WORKSPACE_A), WorkspaceRole.ADMIN,
        key(WORKSPACE_ADMIN, WORKSPACE_B), WorkspaceRole.VIEWER,
        key(PRODUCTION_VIEWER, WORKSPACE_A), WorkspaceRole.ADMIN,
        key(OUTSIDER, WORKSPACE_B), WorkspaceRole.ADMIN,
        key(TWO_WORKSPACE_ADMIN, WORKSPACE_A), WorkspaceRole.ADMIN,
        key(TWO_WORKSPACE_ADMIN, WORKSPACE_B), WorkspaceRole.ADMIN,
        key(APP_OWNER, WORKSPACE_A), WorkspaceRole.ADMIN);

    private AppAuthorizationFixture() {
    }

    public static void stubTenant(
        AppInstanceService appInstanceService, AppService appService, ConnectionService connectionService,
        UserService userService,
        WorkspaceConnectionRepository workspaceConnectionRepository,
        WorkspaceUserRepository workspaceUserRepository) {

        when(userService.getUser(anyString())).thenAnswer(invocation -> {
            String login = invocation.getArgument(0);

            Long userId = USER_IDS.get(login);

            if (userId == null) {
                throw new UserNotFoundException();
            }

            return newUser(userId, login);
        });
        when(userService.fetchUserByLogin(anyString())).thenAnswer(invocation -> {
            String login = invocation.getArgument(0);

            Long userId = USER_IDS.get(login);

            return userId == null ? Optional.empty() : Optional.of(newUser(userId, login));
        });
        when(userService.getCurrentUser()).thenAnswer(invocation -> newUser(103L, WORKSPACE_ADMIN));

        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(anyLong(), anyLong()))
            .thenAnswer(invocation -> findRole(invocation.getArgument(0), invocation.getArgument(1))
                .map(role -> new WorkspaceUser(invocation.getArgument(0), invocation.getArgument(1), role.ordinal())));
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(anyLong(), anyLong(), anyInt()))
            .thenAnswer(invocation -> {
                long userId = invocation.getArgument(0);
                long workspaceId = invocation.getArgument(1);
                int environment = invocation.getArgument(2);

                if (userId == USER_IDS.get(PRODUCTION_VIEWER) && workspaceId == WORKSPACE_A &&
                    environment == Environment.PRODUCTION.ordinal()) {

                    return Optional.of(new WorkspaceUser(userId, workspaceId, WorkspaceRole.VIEWER.ordinal()));
                }

                if (isDevelopmentAdminInA(userId, workspaceId) && environment == Environment.DEVELOPMENT.ordinal()) {
                    return Optional.of(developmentAdminRow());
                }

                if (isProductionOnlyViewerInA(userId, workspaceId) && environment == Environment.PRODUCTION.ordinal()) {
                    return Optional.of(productionOnlyViewerRow());
                }

                return Optional.empty();
            });

        when(workspaceUserRepository.existsByUserIdAndWorkspaceIdAndEnvironmentIsNotNull(anyLong(), anyLong()))
            .thenAnswer(invocation -> hasEnvironmentRows(invocation.getArgument(0), invocation.getArgument(1)));
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(anyLong(), anyLong()))
            .thenAnswer(invocation -> environmentRows(invocation.getArgument(0), invocation.getArgument(1)));
        when(workspaceUserRepository.existsByUserIdAndWorkspaceId(anyLong(), anyLong()))
            .thenAnswer(invocation -> isMember(invocation.getArgument(0), invocation.getArgument(1)));

        App app = newApp(APP, ResourceVisibility.WORKSPACE);
        App privateApp = newApp(PRIVATE_APP, ResourceVisibility.PRIVATE);

        when(appService.fetchApp(anyLong())).thenReturn(Optional.empty());
        when(appService.fetchApp(APP)).thenReturn(Optional.of(app));
        when(appService.fetchApp(PRIVATE_APP)).thenReturn(Optional.of(privateApp));
        when(appService.getApp(APP)).thenReturn(app);
        when(appService.getApp(PRIVATE_APP)).thenReturn(privateApp);

        AppInstance appInstance = newAppInstance(APP_INSTANCE, APP, WORKSPACE_A, WORKSPACE_ADMIN);
        AppInstance privateAppInstance = newAppInstance(
            PRIVATE_APP_INSTANCE, PRIVATE_APP, WORKSPACE_A, WORKSPACE_ADMIN);
        AppInstance foreignAppInstance = newAppInstance(FOREIGN_APP_INSTANCE, APP, WORKSPACE_B, OUTSIDER);

        privateAppInstance.setVisibility(ResourceVisibility.PRIVATE);
        foreignAppInstance.setName("Germany");

        when(appInstanceService.fetchAppInstance(anyLong())).thenReturn(Optional.empty());

        for (AppInstance stubbedAppInstance : List.of(appInstance, privateAppInstance, foreignAppInstance)) {
            when(appInstanceService.fetchAppInstance(stubbedAppInstance.getId()))
                .thenReturn(Optional.of(stubbedAppInstance));
            when(appInstanceService.getAppInstance(stubbedAppInstance.getId())).thenReturn(stubbedAppInstance);
        }

        when(appInstanceService.getAppInstances(anyLong())).thenReturn(List.of());
        when(appInstanceService.getAppInstances(APP)).thenReturn(List.of(appInstance, foreignAppInstance));
        when(appInstanceService.getAppInstances(PRIVATE_APP)).thenReturn(List.of(privateAppInstance));

        Connection connectionA = newConnection(CONNECTION_A);
        Connection connectionB = newConnection(CONNECTION_B);

        when(connectionService.fetchConnection(CONNECTION_A)).thenReturn(Optional.of(connectionA));
        when(connectionService.fetchConnection(CONNECTION_B)).thenReturn(Optional.of(connectionB));
        when(connectionService.getConnection(CONNECTION_A)).thenReturn(connectionA);
        when(connectionService.getConnection(CONNECTION_B)).thenReturn(connectionB);

        when(workspaceConnectionRepository.findByConnectionId(CONNECTION_A))
            .thenReturn(Optional.of(new WorkspaceConnection(CONNECTION_A, WORKSPACE_A)));
        when(workspaceConnectionRepository.findByConnectionId(CONNECTION_B))
            .thenReturn(Optional.of(new WorkspaceConnection(CONNECTION_B, WORKSPACE_B)));
    }

    public static Set<Grant> stubGrants(ResourceGrantService resourceGrantService) {
        Set<Grant> grants = new HashSet<>();

        doAnswer(invocation -> grants.add(
            new Grant(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2))))
                .when(resourceGrantService)
                .grant(anyString(), anyLong(), anyLong());
        doAnswer(invocation -> grants.remove(
            new Grant(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2))))
                .when(resourceGrantService)
                .revoke(anyString(), anyLong(), anyLong());
        doAnswer(invocation -> grants.removeIf(
            grant -> grant.resourceType()
                .equals(invocation.getArgument(0)) && grant.resourceId() == (long) invocation.getArgument(1)))
                    .when(resourceGrantService)
                    .deleteGrants(anyString(), anyLong());

        when(resourceGrantService.getGrantedUserIds(anyString(), anyLong()))
            .thenAnswer(invocation -> grants.stream()
                .filter(grant -> grant.resourceType()
                    .equals(invocation.getArgument(0)) && grant.resourceId() == (long) invocation.getArgument(1))
                .map(Grant::userId)
                .sorted()
                .toList());
        when(resourceGrantService.filterGrantedResourceIds(anyString(), anyLong(), anyCollection()))
            .thenAnswer(invocation -> {
                String resourceType = invocation.getArgument(0);
                long userId = invocation.getArgument(1);
                Collection<Long> resourceIds = invocation.getArgument(2);

                return resourceIds.stream()
                    .filter(resourceId -> grants.contains(new Grant(resourceType, resourceId, userId)))
                    .collect(Collectors.toSet());
            });

        return grants;
    }

    public static void stubMembership(WorkspaceUserService workspaceUserService) {
        when(workspaceUserService.isWorkspaceMember(anyLong(), anyLong()))
            .thenAnswer(invocation -> isMember(invocation.getArgument(0), invocation.getArgument(1)));
    }

    public static void authenticate(String login) {
        List<SimpleGrantedAuthority> authorities = TENANT_ADMIN.equals(login)
            ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"))
            : List.of(new SimpleGrantedAuthority("ROLE_USER"));

        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(login, "n/a", authorities));
    }

    public static void authenticateAnonymously() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new AnonymousAuthenticationToken(
                    "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
    }

    public static App newApp(long id, ResourceVisibility visibility) {
        App app = new App();

        app.setId(id);
        app.setName("app-" + id);
        app.setWorkspaceId(WORKSPACE_A);
        app.setOwnerUserId(USER_IDS.get(WORKSPACE_ADMIN));
        app.setStatus(AppStatus.ACTIVE);
        app.setVisibility(visibility);
        app.setCreatedBy(WORKSPACE_ADMIN);

        return app;
    }

    public static AppInstance newAppInstance(long id, long appId, long workspaceId, String ownerLogin) {
        AppInstance appInstance = new AppInstance();

        appInstance.setId(id);
        appInstance.setAppId(appId);
        appInstance.setName("Production");
        appInstance.setWorkspaceId(workspaceId);
        appInstance.setEnvironment(Environment.PRODUCTION);
        appInstance.setOwnerUserId(USER_IDS.get(ownerLogin));
        appInstance.setStatus(AppInstanceStatus.ACTIVE);
        appInstance.setVisibility(ResourceVisibility.WORKSPACE);
        appInstance.setHostingTarget(AppHostingTarget.VERCEL);
        appInstance.setSecretKeyHint(String.valueOf(id));
        appInstance.setCreatedBy(ownerLogin);

        return appInstance;
    }

    public static long userId(String login) {
        return USER_IDS.get(login);
    }

    public record Grant(String resourceType, long resourceId, long userId) {
    }

    private static boolean isDevelopmentAdminInA(long userId, long workspaceId) {
        return userId == USER_IDS.get(DEVELOPMENT_ADMIN) && workspaceId == WORKSPACE_A;
    }

    private static boolean isProductionOnlyViewerInA(long userId, long workspaceId) {
        return userId == USER_IDS.get(PRODUCTION_ONLY_VIEWER) && workspaceId == WORKSPACE_A;
    }

    private static boolean hasEnvironmentRows(long userId, long workspaceId) {
        return !environmentRows(userId, workspaceId).isEmpty();
    }

    private static List<WorkspaceUser> environmentRows(long userId, long workspaceId) {
        if (isDevelopmentAdminInA(userId, workspaceId)) {
            return List.of(developmentAdminRow());
        }

        if (isProductionOnlyViewerInA(userId, workspaceId)) {
            return List.of(productionOnlyViewerRow());
        }

        return List.of();
    }

    private static boolean isMember(long userId, long workspaceId) {
        return findRole(userId, workspaceId).isPresent() || hasEnvironmentRows(userId, workspaceId);
    }

    private static WorkspaceUser productionOnlyViewerRow() {
        return WorkspaceUser.forRole(
            USER_IDS.get(PRODUCTION_ONLY_VIEWER), WORKSPACE_A, WorkspaceRole.VIEWER, Environment.PRODUCTION);
    }

    private static WorkspaceUser developmentAdminRow() {
        return WorkspaceUser.forRole(
            USER_IDS.get(DEVELOPMENT_ADMIN), WORKSPACE_A, WorkspaceRole.ADMIN, Environment.DEVELOPMENT);
    }

    private static Optional<WorkspaceRole> findRole(long userId, long workspaceId) {
        return USER_IDS.entrySet()
            .stream()
            .filter(entry -> entry.getValue() == userId)
            .map(entry -> IMPLICIT_ROLES.get(key(entry.getKey(), workspaceId)))
            .filter(role -> role != null)
            .findFirst();
    }

    private static String key(String login, long workspaceId) {
        return login + "@" + workspaceId;
    }

    private static Connection newConnection(long id) {
        Connection connection = new Connection();

        connection.setId(id);
        connection.setName("connection-" + id);
        connection.setComponentName("slack");
        connection.setEnvironmentId(Environment.PRODUCTION.ordinal());
        connection.setVisibility(ResourceVisibility.WORKSPACE);
        connection.setCreatedBy(WORKSPACE_ADMIN);

        return connection;
    }

    private static User newUser(long id, String login) {
        User user = new User();

        user.setId(id);
        user.setLogin(login);

        return user;
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppFavorite;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppInstanceUsage;
import com.bytechef.ee.automation.app.domain.AppKeyAccess;
import com.bytechef.ee.automation.app.domain.AppStatus;
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
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * An instance is usable in the portal when it lives in a workspace the current user belongs to, it and its app are
 * {@code ACTIVE}, the app is portal-discoverable, the instance has a URL, and the user may use it: the Design 2 check
 * that sign-in applies one instance at a time ({@code hasResourceScope(id, "AppInstance", "APP_USE")}), computed in
 * bulk as {@code APP_USE} in the instance's workspace and environment plus the instance visibility filter. A launch of
 * an instance outside the portal is refused with the guard's own answer, so it cannot be told from an unknown id. An
 * app icon is served when the app has a usable instance in any environment, and an app outside the portal answers like
 * an unknown app.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class AppPortalFacadeImpl implements AppPortalFacade {

    private static final String ACCESS_DENIED = "Access Denied";
    private static final String APP_INSTANCE = "AppInstance";
    private static final String APP_USE = "APP_USE";

    private final AppFavoriteService appFavoriteService;
    private final AppFileStorage appFileStorage;
    private final AppInstanceService appInstanceService;
    private final AppInstanceUsageService appInstanceUsageService;
    private final AppService appService;
    private final CategoryService categoryService;
    private final PermissionService permissionService;
    private final ResourceVisibilityResolver resourceVisibilityResolver;
    private final UserService userService;
    private final WorkspaceFacade workspaceFacade;

    @SuppressFBWarnings("EI")
    public AppPortalFacadeImpl(
        AppFavoriteService appFavoriteService, AppFileStorage appFileStorage, AppInstanceService appInstanceService,
        AppInstanceUsageService appInstanceUsageService, AppService appService, CategoryService categoryService,
        PermissionService permissionService, ResourceVisibilityResolver resourceVisibilityResolver,
        UserService userService, WorkspaceFacade workspaceFacade) {

        this.appFavoriteService = appFavoriteService;
        this.appFileStorage = appFileStorage;
        this.appInstanceService = appInstanceService;
        this.appInstanceUsageService = appInstanceUsageService;
        this.appService = appService;
        this.categoryService = categoryService;
        this.permissionService = permissionService;
        this.resourceVisibilityResolver = resourceVisibilityResolver;
        this.userService = userService;
        this.workspaceFacade = workspaceFacade;
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Optional<AppIconDTO> getPortalAppIcon(long appId) {
        UsableAppInstances usableAppInstances = getUsableAppInstances(
            getUserWorkspaces(getCurrentUserId()), appInstanceService.getAppInstances(appId));

        App app = usableAppInstances.apps()
            .get(appId);

        if (usableAppInstances.isEmpty() || app == null) {
            return Optional.empty();
        }

        FileEntry icon = app.getIcon();

        if (icon == null) {
            return Optional.empty();
        }

        byte[] content = appFileStorage.readIcon(icon);

        AppIconType iconType = AppIconType.detect(content);

        return Optional.of(new AppIconDTO(content, iconType.getContentType()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<PortalAppDTO> getPortalApps(Environment environment) {
        long userId = getCurrentUserId();

        Map<Long, Workspace> workspaces = getUserWorkspaces(userId);

        List<AppInstance> candidateAppInstances = new ArrayList<>();

        for (Workspace workspace : workspaces.values()) {
            candidateAppInstances.addAll(
                appInstanceService.getWorkspaceAppInstances(workspace.getId(), environment));
        }

        UsableAppInstances usableAppInstances = getUsableAppInstances(workspaces, candidateAppInstances);

        return toPortalApps(userId, workspaces, usableAppInstances);
    }

    @Override
    @PreAuthorize("hasPermission(#appInstanceId, 'AppInstance', 'APP_USE')")
    public void recordAppLaunch(long appInstanceId) {
        long userId = getCurrentUserId();

        List<AppInstance> candidateAppInstances = appInstanceService.fetchAppInstance(appInstanceId)
            .map(List::of)
            .orElse(List.of());

        UsableAppInstances usableAppInstances = getUsableAppInstances(
            getUserWorkspaces(userId), candidateAppInstances);

        if (usableAppInstances.isEmpty()) {
            throw new AuthorizationDeniedException(ACCESS_DENIED);
        }

        appInstanceUsageService.recordLaunch(userId, appInstanceId);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void setAppFavorite(long appId, boolean favorite) {
        long userId = getCurrentUserId();

        if (!favorite) {
            appFavoriteService.removeFavorite(userId, appId);

            return;
        }

        UsableAppInstances usableAppInstances = getUsableAppInstances(
            getUserWorkspaces(userId), appInstanceService.getAppInstances(appId));

        if (usableAppInstances.isEmpty()) {
            throw new ConfigurationException("App %s not found".formatted(appId), AppErrorType.APP_NOT_FOUND);
        }

        appFavoriteService.addFavorite(userId, appId);
    }

    private Map<Long, String> getCategoryNames(Set<Long> portalAppIds, Map<Long, App> apps) {
        List<Long> categoryIds = portalAppIds.stream()
            .map(apps::get)
            .map(App::getCategoryId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        if (categoryIds.isEmpty()) {
            return Map.of();
        }

        return categoryService.getCategories(categoryIds)
            .stream()
            .collect(Collectors.toMap(Category::getId, Category::getName));
    }

    private long getCurrentUserId() {
        User user = userService.getCurrentUser();

        return Objects.requireNonNull(user.getId());
    }

    private Set<Long> getUsableIdsInWorkspace(long workspaceId, List<AppInstance> workspaceAppInstances) {
        Map<Environment, Boolean> usableEnvironments = new EnumMap<>(Environment.class);

        List<AppInstance> scopedAppInstances = workspaceAppInstances.stream()
            .filter(appInstance -> usableEnvironments.computeIfAbsent(
                appInstance.getEnvironment(),
                environment -> permissionService.hasWorkspaceScope(workspaceId, APP_USE, environment)))
            .toList();

        if (scopedAppInstances.isEmpty()) {
            return Set.of();
        }

        return resourceVisibilityResolver.filterVisibleIds(
            APP_INSTANCE, workspaceId,
            scopedAppInstances.stream()
                .map(appInstance -> new VisibilityRecord(
                    appInstance.getId(), appInstance.getVisibility(), appInstance.getCreatedBy()))
                .toList());
    }

    private UsableAppInstances getUsableAppInstances(
        Map<Long, Workspace> workspaces, List<AppInstance> candidateAppInstances) {

        List<AppInstance> launchableAppInstances = candidateAppInstances.stream()
            .filter(appInstance -> workspaces.containsKey(appInstance.getWorkspaceId()))
            .filter(appInstance -> appInstance.getStatus() == AppInstanceStatus.ACTIVE)
            .filter(appInstance -> StringUtils.isNotBlank(appInstance.getUrl()))
            .toList();

        if (launchableAppInstances.isEmpty()) {
            return UsableAppInstances.EMPTY;
        }

        Set<Long> appIds = launchableAppInstances.stream()
            .map(AppInstance::getAppId)
            .collect(Collectors.toSet());

        List<App> apps = appService.getAppsByIds(appIds);

        Map<Long, App> portalApps = apps.stream()
            .filter(app -> app.getStatus() == AppStatus.ACTIVE && app.isPortalDiscoverable())
            .collect(Collectors.toMap(App::getId, Function.identity()));

        Map<Long, List<AppInstance>> appInstancesByWorkspaceId = launchableAppInstances.stream()
            .filter(appInstance -> {
                App app = portalApps.get(appInstance.getAppId());

                return app != null && AppKeyAccess.isAllowed(app.getStatus(), appInstance.getStatus());
            })
            .collect(Collectors.groupingBy(AppInstance::getWorkspaceId, LinkedHashMap::new, Collectors.toList()));

        List<AppInstance> usableAppInstances = new ArrayList<>();

        for (Map.Entry<Long, List<AppInstance>> entry : appInstancesByWorkspaceId.entrySet()) {
            Set<Long> usableIds = getUsableIdsInWorkspace(entry.getKey(), entry.getValue());

            for (AppInstance appInstance : entry.getValue()) {
                if (usableIds.contains(appInstance.getId())) {
                    usableAppInstances.add(appInstance);
                }
            }
        }

        return new UsableAppInstances(portalApps, usableAppInstances);
    }

    private Map<Long, Workspace> getUserWorkspaces(long userId) {
        List<Workspace> userWorkspaces = workspaceFacade.getUserWorkspaces(userId);

        return userWorkspaces.stream()
            .collect(Collectors.toMap(Workspace::getId, Function.identity(), (first, second) -> first,
                LinkedHashMap::new));
    }

    private static @Nullable Instant getLatestUsedDate(List<PortalAppInstanceDTO> portalAppInstances) {
        return portalAppInstances.stream()
            .map(PortalAppInstanceDTO::lastUsedDate)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(null);
    }

    private List<PortalAppDTO> toPortalApps(
        long userId, Map<Long, Workspace> workspaces, UsableAppInstances usableAppInstances) {

        if (usableAppInstances.isEmpty()) {
            return List.of();
        }

        List<AppInstance> appInstances = usableAppInstances.appInstances();

        Map<Long, List<AppInstance>> appInstancesByAppId = appInstances.stream()
            .collect(Collectors.groupingBy(AppInstance::getAppId, LinkedHashMap::new, Collectors.toList()));

        List<AppFavorite> favorites = appFavoriteService.getFavorites(userId, appInstancesByAppId.keySet());

        Set<Long> favoriteAppIds = favorites.stream()
            .map(AppFavorite::appId)
            .collect(Collectors.toSet());

        List<Long> appInstanceIds = appInstances.stream()
            .map(AppInstance::getId)
            .toList();

        List<AppInstanceUsage> appInstanceUsages = appInstanceUsageService.getUsages(userId, appInstanceIds);

        Map<Long, AppInstanceUsage> usages = appInstanceUsages.stream()
            .collect(Collectors.toMap(AppInstanceUsage::appInstanceId, Function.identity()));

        Map<Long, App> apps = usableAppInstances.apps();

        Map<Long, String> categoryNames = getCategoryNames(appInstancesByAppId.keySet(), apps);

        List<PortalAppDTO> portalApps = new ArrayList<>();

        for (Map.Entry<Long, List<AppInstance>> entry : appInstancesByAppId.entrySet()) {
            App app = apps.get(entry.getKey());

            Long categoryId = app.getCategoryId();

            List<AppInstance> appInstancesOfApp = entry.getValue();

            List<PortalAppInstanceDTO> portalAppInstances = appInstancesOfApp.stream()
                .map(appInstance -> toPortalAppInstance(
                    appInstance, workspaces.get(appInstance.getWorkspaceId()), usages.get(appInstance.getId())))
                .sorted(
                    Comparator.comparing(PortalAppInstanceDTO::workspaceName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(PortalAppInstanceDTO::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingLong(PortalAppInstanceDTO::id))
                .toList();

            portalApps.add(
                new PortalAppDTO(
                    app.getId(), app.getName(), app.getDescription(), categoryId,
                    categoryId == null ? null : categoryNames.get(categoryId), app.getIcon() != null,
                    favoriteAppIds.contains(app.getId()), getLatestUsedDate(portalAppInstances), portalAppInstances));
        }

        portalApps.sort(
            Comparator.comparing(PortalAppDTO::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparingLong(PortalAppDTO::id));

        return portalApps;
    }

    private static PortalAppInstanceDTO toPortalAppInstance(
        AppInstance appInstance, Workspace workspace, @Nullable AppInstanceUsage usage) {

        return new PortalAppInstanceDTO(
            appInstance.getId(), appInstance.getName(), workspace.getId(), workspace.getName(),
            Objects.requireNonNull(appInstance.getUrl()), usage == null ? null : usage.lastUsedDate(),
            usage == null ? 0 : usage.useCount());
    }

    private record UsableAppInstances(Map<Long, App> apps, List<AppInstance> appInstances) {

        private static final UsableAppInstances EMPTY = new UsableAppInstances(Map.of(), List.of());

        private boolean isEmpty() {
            return appInstances.isEmpty();
        }
    }
}

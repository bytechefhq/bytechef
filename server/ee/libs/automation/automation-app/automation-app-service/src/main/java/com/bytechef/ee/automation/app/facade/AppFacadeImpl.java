/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.ee.automation.app.audit.AppAuditEvent;
import com.bytechef.ee.automation.app.audit.AppAuditPublisher;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.dto.AppCategoryDTO;
import com.bytechef.ee.automation.app.dto.AppComponentOperationsDTO;
import com.bytechef.ee.automation.app.dto.AppDTO;
import com.bytechef.ee.automation.app.dto.AppIconDTO;
import com.bytechef.ee.automation.app.dto.AppInstanceDTO;
import com.bytechef.ee.automation.app.dto.AppSecretKeyDTO;
import com.bytechef.ee.automation.app.dto.AppTagDTO;
import com.bytechef.ee.automation.app.dto.AppWorkspaceConnectionDTO;
import com.bytechef.ee.automation.app.dto.ForeignAppInstanceDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.file.storage.AppFileStorage;
import com.bytechef.ee.automation.app.security.AppInstanceClients;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class AppFacadeImpl implements AppFacade {

    private static final Logger log = LoggerFactory.getLogger(AppFacadeImpl.class);

    private static final String APP = "App";
    private static final String APP_INSTANCE = "AppInstance";
    private static final String APP_MANAGE = "APP_MANAGE";
    private static final String APP_VIEW = "APP_VIEW";
    private static final String CONNECTION = "Connection";
    private static final String CONNECTION_EDIT = "CONNECTION_EDIT";
    private static final String CATEGORY_ID = "categoryId";
    private static final String TAG_IDS = "tagIds";
    private static final String ICON = "icon";
    private static final int MAX_CATEGORY_NAME_LENGTH = 256;
    private static final String PORTAL_DISCOVERABLE = "portalDiscoverable";
    private static final int MAX_ICON_SIZE = 256 * 1024;

    private final ActionDefinitionService actionDefinitionService;
    private final AppAuditPublisher appAuditPublisher;
    private final AppFileStorage appFileStorage;
    private final AppInstanceClients appInstanceClients;
    private final AppInstanceService appInstanceService;
    private final AppService appService;
    private final CategoryService categoryService;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final PermissionService permissionService;
    private final ResourceGrantService resourceGrantService;
    private final ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry;
    private final ResourceVisibilityResolver resourceVisibilityResolver;
    private final TagService tagService;
    private final UserService userService;
    private final WorkspaceConnectionFacade workspaceConnectionFacade;

    @SuppressFBWarnings("EI")
    public AppFacadeImpl(
        ActionDefinitionService actionDefinitionService, AppAuditPublisher appAuditPublisher,
        AppFileStorage appFileStorage, AppInstanceClients appInstanceClients, AppInstanceService appInstanceService,
        AppService appService, CategoryService categoryService,
        ClusterElementDefinitionService clusterElementDefinitionService, PermissionService permissionService,
        ResourceGrantService resourceGrantService, ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry,
        ResourceVisibilityResolver resourceVisibilityResolver, TagService tagService, UserService userService,
        WorkspaceConnectionFacade workspaceConnectionFacade) {

        this.actionDefinitionService = actionDefinitionService;
        this.appAuditPublisher = appAuditPublisher;
        this.appFileStorage = appFileStorage;
        this.appInstanceClients = appInstanceClients;
        this.appInstanceService = appInstanceService;
        this.appService = appService;
        this.categoryService = categoryService;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.permissionService = permissionService;
        this.resourceGrantService = resourceGrantService;
        this.resourceVisibilityPolicyRegistry = resourceVisibilityPolicyRegistry;
        this.resourceVisibilityResolver = resourceVisibilityResolver;
        this.tagService = tagService;
        this.userService = userService;
        this.workspaceConnectionFacade = workspaceConnectionFacade;
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEnvironment(#workspaceId, 'APP_MANAGE', #environment)")
    public AppSecretKeyDTO createApp(
        long workspaceId, Environment environment, String name, @Nullable String description,
        AppHostingTarget hostingTarget, ResourceVisibility visibility, @Nullable AppUserAuthMode userAuthMode,
        @Nullable AppCategoryDTO category, boolean portalDiscoverable) {

        checkVisibilitySupported(visibility);

        AppUserAuthMode resolvedUserAuthMode =
            userAuthMode == null ? appInstanceClients.getDefaultUserAuthMode() : userAuthMode;

        appInstanceClients.checkAvailable(resolvedUserAuthMode);

        Long categoryId = category == null ? null : resolveCategoryId(category);

        User user = userService.getCurrentUser();

        App app = new App();

        app.setWorkspaceId(workspaceId);
        app.setName(name);
        app.setDescription(description);
        app.setVisibility(visibility);
        app.setOwnerUserId(user.getId());
        app.setCategoryId(categoryId);
        app.setPortalDiscoverable(portalDiscoverable);

        App createdApp = appService.create(app);

        Map<String, Object> createdAuditData = new HashMap<>();

        createdAuditData.put("name", name);
        createdAuditData.put("workspaceId", workspaceId);
        createdAuditData.put(PORTAL_DISCOVERABLE, portalDiscoverable);

        if (categoryId != null) {
            createdAuditData.put(CATEGORY_ID, categoryId);
        }

        appAuditPublisher.publish(AppAuditEvent.APP_CREATED, createdApp.getId(), createdAuditData);

        if (portalDiscoverable) {
            appAuditPublisher.publish(AppAuditEvent.APP_PUBLISHED_TO_PORTAL, createdApp.getId());
        }

        AppInstance appInstance = new AppInstance();

        appInstance.setAppId(createdApp.getId());
        appInstance.setWorkspaceId(workspaceId);
        appInstance.setEnvironment(environment);
        appInstance.setName(StringUtils.capitalize(environment.name()
            .toLowerCase(Locale.ROOT)));
        appInstance.setHostingTarget(hostingTarget);
        appInstance.setOwnerUserId(createdApp.getOwnerUserId());
        appInstance.setUserAuthMode(resolvedUserAuthMode);
        appInstance.setVisibility(visibility);

        AppInstanceService.CreatedAppInstance createdAppInstance = appInstanceService.create(appInstance);

        AppInstance createdInstance = createdAppInstance.appInstance();

        appAuditPublisher.publish(
            AppAuditEvent.APP_INSTANCE_CREATED, createdApp.getId(),
            Map.of(
                "appInstanceId", createdInstance.getId(), "name", createdInstance.getName(), "workspaceId",
                workspaceId, "environment", environment.name()));

        return new AppSecretKeyDTO(AppInstanceDTO.of(createdApp, createdInstance), createdAppInstance.secretKey());
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'App', 'APP_MANAGE')")
    public void deleteApp(long id) {
        App app = appService.getApp(id);

        checkManageInEveryEnvironment(app);

        List<AppInstance> appInstances = appInstanceService.getAppInstances(id);

        for (AppInstance appInstance : appInstances) {
            String registeredClientId = appInstance.getRegisteredClientId();

            if (registeredClientId != null) {
                appInstanceClients.deleteClient(registeredClientId);
            }
        }

        appService.delete(id);

        resourceGrantService.deleteGrants(APP, id);

        for (AppInstance appInstance : appInstances) {
            resourceGrantService.deleteGrants(APP_INSTANCE, appInstance.getId());
        }

        FileEntry icon = app.getIcon();

        if (icon != null) {
            deleteIconFile(icon);
        }

        appAuditPublisher.publish(AppAuditEvent.APP_DELETED, id);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'App', 'APP_MANAGE')")
    public void deleteAppIcon(long id) {
        App app = appService.getApp(id);

        FileEntry icon = app.getIcon();

        if (icon == null) {
            return;
        }

        appService.updateIcon(id, null);

        deleteIconFile(icon);

        appAuditPublisher.publish(AppAuditEvent.APP_UPDATED, id, toUpdatedAuditData(List.of(ICON), app));
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'App', 'APP_VIEW')")
    @Transactional(readOnly = true)
    public AppDTO getApp(long id) {
        return toAppDTO(appService.getApp(id), appInstanceService.getAppInstances(id));
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'App', 'APP_VIEW')")
    @Transactional(readOnly = true)
    public Optional<AppIconDTO> getAppIcon(long id) {
        App app = appService.getApp(id);

        FileEntry icon = app.getIcon();

        if (icon == null) {
            return Optional.empty();
        }

        byte[] content = appFileStorage.readIcon(icon);

        AppIconType iconType = AppIconType.detect(content);

        return Optional.of(new AppIconDTO(content, iconType.getContentType()));
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'APP_VIEW')")
    @Transactional(readOnly = true)
    public List<AppCategoryDTO> getAppCategories(long workspaceId) {
        List<Long> categoryIds = getVisibleApps(workspaceId, appService.getApps(workspaceId))
            .stream()
            .map(App::getCategoryId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        if (categoryIds.isEmpty()) {
            return List.of();
        }

        return categoryService.getCategories(categoryIds)
            .stream()
            .map(category -> new AppCategoryDTO(category.getId(), category.getName()))
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'APP_VIEW')")
    @Transactional(readOnly = true)
    public List<AppTagDTO> getAppTags(long workspaceId) {
        List<Long> tagIds = getVisibleApps(workspaceId, appService.getApps(workspaceId))
            .stream()
            .flatMap(app -> app.getTagIds()
                .stream())
            .distinct()
            .toList();

        return tagService.getTags(tagIds)
            .stream()
            .map(tag -> new AppTagDTO(tag.getId(), tag.getName()))
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'APP_VIEW')")
    @Transactional(readOnly = true)
    public List<AppDTO> getApps(long workspaceId) {
        List<App> visibleApps = getVisibleApps(workspaceId, appService.getApps(workspaceId));

        if (visibleApps.isEmpty()) {
            return List.of();
        }

        Map<Long, List<AppInstance>> appInstancesByAppId = appInstanceService
            .getAppInstancesByAppIds(
                visibleApps.stream()
                    .map(App::getId)
                    .toList())
            .stream()
            .collect(Collectors.groupingBy(AppInstance::getAppId));

        Set<Long> viewableAppInstanceIds = getViewableAppInstanceIds(
            workspaceId, appInstancesByAppId.values()
                .stream()
                .flatMap(List::stream)
                .toList());

        return visibleApps.stream()
            .map(app -> toAppDTO(
                app, appInstancesByAppId.getOrDefault(app.getId(), List.of()), viewableAppInstanceIds))
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'APP_VIEW')")
    public boolean isUserAuthAvailable(long workspaceId) {
        return appInstanceClients.isAvailable();
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'App', 'APP_MANAGE')")
    public AppDTO updateApp(
        long id, @Nullable String name, @Nullable String description, @Nullable AppCategoryDTO category,
        @Nullable Boolean portalDiscoverable) {

        Long categoryId = category == null ? null : resolveCategoryId(category);

        EditableFields previousFields = EditableFields.of(appService.getApp(id));

        App app = appService.update(id, name, description);

        if (category != null && !Objects.equals(previousFields.categoryId(), categoryId)) {
            app = appService.updateCategory(id, categoryId);
        }

        if (portalDiscoverable != null && previousFields.portalDiscoverable() != portalDiscoverable) {
            app = appService.updatePortalDiscoverable(id, portalDiscoverable);
        }

        List<String> changedFieldNames = previousFields.getChangedFieldNames(EditableFields.of(app));

        if (!changedFieldNames.isEmpty()) {
            appAuditPublisher.publish(AppAuditEvent.APP_UPDATED, id, toUpdatedAuditData(changedFieldNames, app));

            if (changedFieldNames.contains(PORTAL_DISCOVERABLE)) {
                appAuditPublisher.publish(
                    app.isPortalDiscoverable() ? AppAuditEvent.APP_PUBLISHED_TO_PORTAL
                        : AppAuditEvent.APP_UNPUBLISHED_FROM_PORTAL,
                    id);
            }
        }

        return toAppDTO(app, appInstanceService.getAppInstances(id));
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'App', 'APP_MANAGE')")
    public void updateAppIcon(long id, InputStream inputStream) {
        byte[] content = readIconContent(inputStream);

        AppIconType iconType = AppIconType.detect(content);

        App app = appService.getApp(id);

        FileEntry previousIcon = app.getIcon();

        FileEntry icon = appFileStorage.storeIcon("icon." + iconType.getExtension(), content);

        appService.updateIcon(id, icon);

        if (previousIcon != null) {
            deleteIconFile(previousIcon);
        }

        appAuditPublisher.publish(AppAuditEvent.APP_UPDATED, id, toUpdatedAuditData(List.of(ICON), app));
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'App', 'APP_MANAGE')")
    public AppDTO updateAppTags(long id, List<AppTagDTO> tags) {
        App app = appService.getApp(id);

        List<Long> previousTagIds = app.getTagIds();

        List<Long> tagIds = resolveTagIds(tags);

        if (!previousTagIds.equals(tagIds)) {
            app = appService.updateTags(id, tagIds);

            appAuditPublisher.publish(
                AppAuditEvent.APP_UPDATED, id, Map.of("changedFields", List.of(TAG_IDS), TAG_IDS, app.getTagIds()));
        }

        return toAppDTO(app, appInstanceService.getAppInstances(id));
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'App', 'APP_MANAGE')")
    public AppDTO updateAppStatus(long id, AppStatus status) {
        checkManageInEveryEnvironment(appService.getApp(id));

        App app = appService.updateStatus(id, status);

        if (status == AppStatus.SUSPENDED || status == AppStatus.ARCHIVED) {
            for (AppInstance appInstance : appInstanceService.getAppInstances(id)) {
                String registeredClientId = appInstance.getRegisteredClientId();

                if (registeredClientId != null) {
                    appInstanceClients.revokeGrants(registeredClientId);
                }
            }
        }

        AppAuditEvent appAuditEvent = switch (status) {
            case ACTIVE -> AppAuditEvent.APP_ACTIVATED;
            case SUSPENDED -> AppAuditEvent.APP_SUSPENDED;
            case ARCHIVED -> AppAuditEvent.APP_ARCHIVED;
            case DRAFT -> throw new IllegalArgumentException("An app cannot return to DRAFT");
        };

        appAuditPublisher.publish(appAuditEvent, id);

        return toAppDTO(app, appInstanceService.getAppInstances(id));
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEnvironment(#workspaceId, 'CONNECTION_VIEW', #environment)")
    @Transactional(readOnly = true)
    public List<AppWorkspaceConnectionDTO> getConnections(
        long workspaceId, Environment environment, @Nullable String componentName) {

        return workspaceConnectionFacade
            .getConnections(workspaceId, componentName, null, (long) environment.ordinal(), null)
            .stream()
            .map(this::toWorkspaceConnectionDTO)
            .toList();
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public AppComponentOperationsDTO getComponentOperations(String componentName, int componentVersion) {
        List<String> actionNames = actionDefinitionService.getActionDefinitions(componentName, componentVersion)
            .stream()
            .map(ActionDefinition::getName)
            .sorted()
            .toList();
        List<String> toolNames = clusterElementDefinitionService
            .getClusterElementDefinitions(componentName, componentVersion, BaseToolFunction.TOOLS)
            .stream()
            .map(ClusterElementDefinition::getName)
            .sorted()
            .toList();

        return new AppComponentOperationsDTO(actionNames, toolNames);
    }

    private void checkManageInEveryEnvironment(App app) {
        if (!permissionService.hasWorkspaceScopeInEveryEnvironment(app.getWorkspaceId(), APP_MANAGE)) {
            throw new AccessDeniedException("Access Denied");
        }
    }

    private void deleteIconFile(FileEntry icon) {
        try {
            appFileStorage.deleteIcon(icon);
        } catch (RuntimeException runtimeException) {
            log.warn("Failed to delete the app icon file {}", icon.getUrl(), runtimeException);
        }
    }

    private List<App> getVisibleApps(long workspaceId, List<App> apps) {
        Set<Long> visibleAppIds = resourceVisibilityResolver.filterVisibleIds(
            APP, workspaceId,
            apps.stream()
                .map(app -> new VisibilityRecord(app.getId(), app.getVisibility(), app.getCreatedBy()))
                .toList());

        return apps.stream()
            .filter(app -> visibleAppIds.contains(app.getId()))
            .toList();
    }

    private Set<Long> getViewableAppInstanceIds(long workspaceId, List<AppInstance> appInstances) {
        List<AppInstance> workspaceAppInstances = appInstances.stream()
            .filter(appInstance -> Objects.equals(appInstance.getWorkspaceId(), workspaceId))
            .toList();

        if (workspaceAppInstances.isEmpty()) {
            return Set.of();
        }

        Set<Long> visibleAppInstanceIds = resourceVisibilityResolver.filterVisibleIds(
            APP_INSTANCE, workspaceId,
            workspaceAppInstances.stream()
                .map(appInstance -> new VisibilityRecord(
                    appInstance.getId(), appInstance.getVisibility(), appInstance.getCreatedBy()))
                .toList());

        Map<Environment, Boolean> viewableEnvironments = new EnumMap<>(Environment.class);

        return workspaceAppInstances.stream()
            .filter(appInstance -> visibleAppInstanceIds.contains(appInstance.getId()))
            .filter(appInstance -> viewableEnvironments.computeIfAbsent(
                appInstance.getEnvironment(),
                environment -> permissionService.hasWorkspaceScope(workspaceId, APP_VIEW, environment)))
            .map(AppInstance::getId)
            .collect(Collectors.toSet());
    }

    private AppDTO toAppDTO(App app, List<AppInstance> appInstances) {
        return toAppDTO(app, appInstances, getViewableAppInstanceIds(app.getWorkspaceId(), appInstances));
    }

    /**
     * An instance in another workspace than its app's shows without its key hint: the app's workspace governs the app,
     * the instance's workspace governs the instance.
     */
    private static AppDTO toAppDTO(App app, List<AppInstance> appInstances, Set<Long> viewableAppInstanceIds) {
        List<AppInstanceDTO> appInstanceDTOs = new ArrayList<>();
        List<ForeignAppInstanceDTO> foreignAppInstanceDTOs = new ArrayList<>();

        for (AppInstance appInstance : appInstances) {
            if (!Objects.equals(appInstance.getWorkspaceId(), app.getWorkspaceId())) {
                foreignAppInstanceDTOs.add(ForeignAppInstanceDTO.of(appInstance));
            } else if (viewableAppInstanceIds.contains(appInstance.getId())) {
                appInstanceDTOs.add(AppInstanceDTO.of(app, appInstance));
            }
        }

        return AppDTO.of(app, appInstanceDTOs, foreignAppInstanceDTOs);
    }

    private static byte[] readIconContent(InputStream inputStream) {
        byte[] content;

        try {
            content = inputStream.readNBytes(MAX_ICON_SIZE + 1);
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }

        if (content.length > MAX_ICON_SIZE) {
            throw new ConfigurationException(
                "App icon must be at most %d KB".formatted(MAX_ICON_SIZE / 1024), AppErrorType.APP_ICON_INVALID);
        }

        return content;
    }

    private static Map<String, Object> toUpdatedAuditData(List<String> changedFieldNames, App app) {
        Map<String, Object> data = new HashMap<>();

        data.put("changedFields", changedFieldNames);

        if (changedFieldNames.contains("name")) {
            data.put("name", app.getName());
        }

        Long categoryId = app.getCategoryId();

        if (changedFieldNames.contains(CATEGORY_ID) && categoryId != null) {
            data.put(CATEGORY_ID, categoryId);
        }

        if (changedFieldNames.contains(PORTAL_DISCOVERABLE)) {
            data.put(PORTAL_DISCOVERABLE, app.isPortalDiscoverable());
        }

        return data;
    }

    private List<Long> resolveTagIds(List<AppTagDTO> tags) {
        List<Long> existingTagIds = tags.stream()
            .map(AppTagDTO::id)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        List<Tag> existingTags = tagService.getTags(existingTagIds);

        if (existingTags.size() != existingTagIds.size()) {
            throw new ConfigurationException("App tag does not exist", AppErrorType.APP_TAG_INVALID);
        }

        List<Tag> newTags = tags.stream()
            .filter(tag -> tag.id() == null)
            .map(tag -> StringUtils.trim(tag.name()))
            .filter(StringUtils::isNotEmpty)
            .distinct()
            .map(Tag::new)
            .toList();

        List<Tag> savedTags = newTags.isEmpty() ? List.of() : tagService.save(newTags);

        return Stream.concat(existingTags.stream(), savedTags.stream())
            .map(Tag::getId)
            .distinct()
            .sorted()
            .toList();
    }

    private @Nullable Long resolveCategoryId(AppCategoryDTO category) {
        Long categoryId = category.id();

        if (categoryId != null) {
            List<Category> categories = categoryService.getCategories(List.of(categoryId));

            if (categories.isEmpty()) {
                throw new ConfigurationException(
                    "Category %s not found".formatted(categoryId), AppErrorType.APP_CATEGORY_INVALID);
            }

            return categoryId;
        }

        String categoryName = StringUtils.trimToNull(category.name());

        if (categoryName == null) {
            return null;
        }

        if (categoryName.length() > MAX_CATEGORY_NAME_LENGTH) {
            throw new ConfigurationException(
                "Category name must be at most %d characters".formatted(MAX_CATEGORY_NAME_LENGTH),
                AppErrorType.APP_FIELD_TOO_LONG);
        }

        Category savedCategory = categoryService.save(new Category(categoryName));

        return savedCategory.getId();
    }

    private void checkVisibilitySupported(@Nullable ResourceVisibility visibility) {
        if (visibility == null || !resourceVisibilityPolicyRegistry.supports(APP, visibility)) {
            throw new ConfigurationException(
                "App does not support %s visibility".formatted(visibility), AppErrorType.APP_VISIBILITY_UNSUPPORTED);
        }
    }

    private boolean isUsable(long connectionId, @Nullable CredentialStatus credentialStatus) {
        return credentialStatus == CredentialStatus.VALID &&
            permissionService.hasResourceScope(connectionId, CONNECTION, CONNECTION_EDIT);
    }

    private AppWorkspaceConnectionDTO toWorkspaceConnectionDTO(ConnectionDTO connectionDTO) {
        long connectionId = Objects.requireNonNull(connectionDTO.id(), "id");
        boolean aiProvider = connectionId < 0;
        AuthorizationType authorizationType = connectionDTO.authorizationType();

        return new AppWorkspaceConnectionDTO(
            connectionId, connectionDTO.name(), connectionDTO.componentName(), connectionDTO.connectionVersion(),
            authorizationType == null ? null : authorizationType.name(),
            String.valueOf(connectionDTO.credentialStatus()), aiProvider,
            !aiProvider && isUsable(connectionId, connectionDTO.credentialStatus()));
    }

    /**
     * The values an update may change, copied out of the entity because the service mutates the app it loads.
     */
    private record EditableFields(
        @Nullable String name, @Nullable String description, @Nullable Long categoryId, boolean portalDiscoverable) {

        static EditableFields of(App app) {
            return new EditableFields(
                app.getName(), app.getDescription(), app.getCategoryId(), app.isPortalDiscoverable());
        }

        List<String> getChangedFieldNames(EditableFields updatedFields) {
            List<String> changedFieldNames = new ArrayList<>();

            if (!Objects.equals(name, updatedFields.name)) {
                changedFieldNames.add("name");
            }

            if (!Objects.equals(description, updatedFields.description)) {
                changedFieldNames.add("description");
            }

            if (!Objects.equals(categoryId, updatedFields.categoryId)) {
                changedFieldNames.add(CATEGORY_ID);
            }

            if (portalDiscoverable != updatedFields.portalDiscoverable) {
                changedFieldNames.add(PORTAL_DISCOVERABLE);
            }

            return changedFieldNames;
        }
    }
}

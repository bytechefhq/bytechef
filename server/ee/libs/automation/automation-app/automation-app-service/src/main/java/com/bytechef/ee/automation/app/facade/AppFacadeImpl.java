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
import com.bytechef.ee.automation.app.domain.AppConnection;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.dto.AppBindableConnectionDTO;
import com.bytechef.ee.automation.app.dto.AppBindingDTO;
import com.bytechef.ee.automation.app.dto.AppComponentOperationsDTO;
import com.bytechef.ee.automation.app.dto.AppDTO;
import com.bytechef.ee.automation.app.dto.AppSecretKeyDTO;
import com.bytechef.ee.automation.app.dto.AppWorkspaceConnectionDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.service.AppConnectionService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
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

    private static final String APP = "App";
    private static final String CONNECTION = "Connection";
    private static final String CONNECTION_EDIT = "CONNECTION_EDIT";

    private final ActionDefinitionService actionDefinitionService;
    private final AppAuditPublisher appAuditPublisher;
    private final AppConnectionService appConnectionService;
    private final AppService appService;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectionService connectionService;
    private final PermissionService permissionService;
    private final ResourceGrantService resourceGrantService;
    private final ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry;
    private final ResourceVisibilityResolver resourceVisibilityResolver;
    private final UserService userService;
    private final WorkspaceConnectionFacade workspaceConnectionFacade;

    @SuppressFBWarnings("EI")
    public AppFacadeImpl(
        ActionDefinitionService actionDefinitionService, AppAuditPublisher appAuditPublisher,
        AppConnectionService appConnectionService, AppService appService,
        ClusterElementDefinitionService clusterElementDefinitionService,
        ComponentDefinitionService componentDefinitionService, ConnectionService connectionService,
        PermissionService permissionService, ResourceGrantService resourceGrantService,
        ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry,
        ResourceVisibilityResolver resourceVisibilityResolver, UserService userService,
        WorkspaceConnectionFacade workspaceConnectionFacade) {

        this.actionDefinitionService = actionDefinitionService;
        this.appAuditPublisher = appAuditPublisher;
        this.appConnectionService = appConnectionService;
        this.appService = appService;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.componentDefinitionService = componentDefinitionService;
        this.connectionService = connectionService;
        this.permissionService = permissionService;
        this.resourceGrantService = resourceGrantService;
        this.resourceVisibilityPolicyRegistry = resourceVisibilityPolicyRegistry;
        this.resourceVisibilityResolver = resourceVisibilityResolver;
        this.userService = userService;
        this.workspaceConnectionFacade = workspaceConnectionFacade;
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEnvironment(#workspaceId, 'APP_MANAGE', #environment)")
    public AppSecretKeyDTO createApp(
        long workspaceId, Environment environment, String name, @Nullable String description,
        AppHostingTarget hostingTarget, ResourceVisibility visibility) {

        checkVisibilitySupported(visibility);

        User user = userService.getCurrentUser();

        App app = new App();

        app.setWorkspaceId(workspaceId);
        app.setEnvironment(environment);
        app.setName(name);
        app.setDescription(description);
        app.setHostingTarget(hostingTarget);
        app.setVisibility(visibility);
        app.setOwnerUserId(user.getId());

        AppService.CreatedApp createdApp = appService.create(app);

        Map<String, Object> data = new HashMap<>();

        data.put("name", name);
        data.put("workspaceId", workspaceId);

        appAuditPublisher.publish(AppAuditEvent.APP_CREATED, createdApp.app()
            .getId(), data);

        return new AppSecretKeyDTO(AppDTO.of(createdApp.app()), createdApp.secretKey());
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'App', 'APP_MANAGE')")
    public void deleteApp(long id) {
        appService.delete(id);

        resourceGrantService.deleteGrants(APP, id);

        appAuditPublisher.publish(AppAuditEvent.APP_DELETED, id);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'App', 'APP_VIEW')")
    @Transactional(readOnly = true)
    public AppDTO getApp(long id) {
        return AppDTO.of(appService.getApp(id));
    }

    @Override
    @Transactional(readOnly = true)
    public void requireAppInEnvironment(long id, Environment environment) {
        App app = appService.getApp(id);

        if (app.getEnvironment() != environment) {
            throw new ConfigurationException("App %s not found".formatted(id), AppErrorType.APP_NOT_FOUND);
        }
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEnvironmentId(#workspaceId, 'APP_VIEW', #environmentId)")
    @Transactional(readOnly = true)
    public List<AppDTO> getApps(long workspaceId, long environmentId) {
        List<App> apps = appService.getApps(workspaceId, Environment.values()[(int) environmentId]);

        Set<Long> visibleAppIds = resourceVisibilityResolver.filterVisibleIds(
            APP, workspaceId,
            apps.stream()
                .map(app -> new VisibilityRecord(app.getId(), app.getVisibility(), app.getCreatedBy()))
                .toList());

        return apps.stream()
            .filter(app -> visibleAppIds.contains(app.getId()))
            .map(AppDTO::of)
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'App', 'APP_MANAGE')")
    public AppDTO updateApp(
        long id, @Nullable String name, @Nullable String description, @Nullable AppHostingTarget hostingTarget,
        @Nullable String url, @Nullable Instant lastDeployedDate, @Nullable ResourceVisibility visibility) {

        if (visibility != null) {
            checkVisibilitySupported(visibility);
        }

        EditableFields previousFields = EditableFields.of(appService.getApp(id));

        App app = appService.update(id, name, description, hostingTarget, url, lastDeployedDate, visibility);

        List<String> changedFieldNames = previousFields.getChangedFieldNames(EditableFields.of(app));

        if (!changedFieldNames.isEmpty()) {
            appAuditPublisher.publish(AppAuditEvent.APP_UPDATED, id, toUpdatedAuditData(changedFieldNames, app));
        }

        return AppDTO.of(app);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'App', 'APP_MANAGE')")
    public AppDTO updateAppStatus(long id, AppStatus status) {
        App app = appService.updateStatus(id, status);

        appAuditPublisher.publish(status == AppStatus.ENABLED ? AppAuditEvent.APP_ENABLED : AppAuditEvent.APP_DISABLED,
            id);

        return AppDTO.of(app);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'App', 'APP_MANAGE')")
    public AppSecretKeyDTO rotateSecretKey(long id) {
        AppService.RotatedKey rotatedKey = appService.rotateSecretKey(id);

        appAuditPublisher.publish(AppAuditEvent.APP_SECRET_KEY_ROTATED, id);

        return new AppSecretKeyDTO(AppDTO.of(rotatedKey.app()), rotatedKey.secretKey());
    }

    @Override
    @PreAuthorize("hasPermission(#appId, 'App', 'APP_VIEW')")
    @Transactional(readOnly = true)
    public List<AppBindingDTO> getBindings(long appId) {
        return appConnectionService.getBindings(appId)
            .stream()
            .map(this::toBindingDTO)
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#appId, 'App', 'APP_MANAGE')")
    @Transactional(readOnly = true)
    public List<AppBindableConnectionDTO> getBindableConnections(long appId, @Nullable String componentName) {
        App app = appService.getApp(appId);

        List<Connection> bindableConnections = appConnectionService.getBindableConnections(appId)
            .stream()
            .filter(connection -> componentName == null || Objects.equals(connection.getComponentName(), componentName))
            .toList();

        Set<Long> visibleConnectionIds = resourceVisibilityResolver.filterVisibleIds(
            CONNECTION, Objects.requireNonNull(app.getWorkspaceId(), "workspaceId"),
            bindableConnections.stream()
                .map(connection -> new VisibilityRecord(
                    connection.getId(), connection.getVisibility(), connection.getCreatedBy()))
                .toList());

        Map<String, Optional<Integer>> latestComponentVersions = new HashMap<>();

        return bindableConnections.stream()
            .filter(connection -> visibleConnectionIds.contains(connection.getId()))
            .flatMap(connection -> latestComponentVersions
                .computeIfAbsent(connection.getComponentName(), this::fetchLatestVersion)
                .map(componentVersion -> new AppBindableConnectionDTO(
                    connection.getId(), connection.getName(), connection.getComponentName(),
                    connection.getConnectionVersion(), componentVersion,
                    isUsable(connection.getId(), connection.getCredentialStatus())))
                .stream())
            .toList();
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
    @PreAuthorize("hasPermission(#appId, 'App', 'APP_MANAGE') and " +
        "hasPermission(#connectionId, 'Connection', 'CONNECTION_EDIT')")
    public AppBindingDTO bindConnection(
        long appId, String bindingKey, long connectionId, List<String> actionNames, List<String> toolNames,
        boolean proxyEnabled, List<String> proxyAllowedMethods) {

        Optional<AppConnection> existingBinding = appConnectionService.fetchBinding(appId, bindingKey);

        AppConnection appConnection = appConnectionService.bind(
            appId, bindingKey, connectionId, actionNames, toolNames, proxyEnabled, proxyAllowedMethods);

        if (existingBinding.isEmpty()) {
            appAuditPublisher.publish(
                AppAuditEvent.APP_CONNECTION_BOUND, appId,
                Map.of("bindingKey", bindingKey, "connectionId", connectionId));
        } else if (Objects.equals(existingBinding.get()
            .getConnectionId(), connectionId)) {

            appAuditPublisher.publish(
                AppAuditEvent.APP_OPERATIONS_CHANGED, appId,
                Map.of("bindingKey", bindingKey, "connectionId", connectionId));
        } else {
            appAuditPublisher.publish(
                AppAuditEvent.APP_CONNECTION_BOUND, appId,
                Map.of(
                    "bindingKey", bindingKey, "connectionId", connectionId, "previousConnectionId",
                    existingBinding.get()
                        .getConnectionId()));
        }

        return toBindingDTO(appConnection);
    }

    @Override
    @PreAuthorize("hasPermission(#appId, 'App', 'APP_MANAGE')")
    public void unbindConnection(long appId, String bindingKey) {
        if (appConnectionService.unbind(appId, bindingKey)) {
            appAuditPublisher.publish(
                AppAuditEvent.APP_CONNECTION_UNBOUND, appId, Map.of("bindingKey", bindingKey));
        }
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

    private static Map<String, Object> toUpdatedAuditData(List<String> changedFieldNames, App app) {
        Map<String, Object> data = new HashMap<>();

        data.put("changedFields", changedFieldNames);

        if (changedFieldNames.contains("name")) {
            data.put("name", app.getName());
        }

        if (changedFieldNames.contains("hostingTarget")) {
            data.put("hostingTarget", String.valueOf(app.getHostingTarget()));
        }

        if (changedFieldNames.contains("visibility")) {
            data.put("visibility", String.valueOf(app.getVisibility()));
        }

        return data;
    }

    private void checkVisibilitySupported(ResourceVisibility visibility) {
        if (visibility == null || !resourceVisibilityPolicyRegistry.supports(APP, visibility)) {
            throw new ConfigurationException(
                "App does not support %s visibility".formatted(visibility), AppErrorType.APP_VISIBILITY_UNSUPPORTED);
        }
    }

    private Optional<Integer> fetchLatestVersion(String componentName) {
        return componentDefinitionService.fetchComponentDefinition(componentName, null)
            .map(ComponentDefinition::getVersion);
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

    private AppBindingDTO toBindingDTO(AppConnection appConnection) {
        Connection connection = connectionService.getConnection(appConnection.getConnectionId());

        return AppBindingDTO.of(
            appConnection, connection, fetchLatestVersion(connection.getComponentName()).orElse(null));
    }

    /**
     * The values an update may change, copied out of the entity because the service mutates the instance it loads.
     */
    private record EditableFields(
        @Nullable String name, @Nullable String description, @Nullable AppHostingTarget hostingTarget,
        @Nullable String url, @Nullable Instant lastDeployedDate, @Nullable ResourceVisibility visibility) {

        static EditableFields of(App app) {
            return new EditableFields(
                app.getName(), app.getDescription(), app.getHostingTarget(), app.getUrl(), app.getLastDeployedDate(),
                app.getVisibility());
        }

        List<String> getChangedFieldNames(EditableFields updatedFields) {
            List<String> changedFieldNames = new ArrayList<>();

            if (!Objects.equals(name, updatedFields.name)) {
                changedFieldNames.add("name");
            }

            if (!Objects.equals(description, updatedFields.description)) {
                changedFieldNames.add("description");
            }

            if (hostingTarget != updatedFields.hostingTarget) {
                changedFieldNames.add("hostingTarget");
            }

            if (!Objects.equals(url, updatedFields.url)) {
                changedFieldNames.add("url");
            }

            if (!Objects.equals(lastDeployedDate, updatedFields.lastDeployedDate)) {
                changedFieldNames.add("lastDeployedDate");
            }

            if (visibility != updatedFields.visibility) {
                changedFieldNames.add("visibility");
            }

            return changedFieldNames;
        }
    }
}

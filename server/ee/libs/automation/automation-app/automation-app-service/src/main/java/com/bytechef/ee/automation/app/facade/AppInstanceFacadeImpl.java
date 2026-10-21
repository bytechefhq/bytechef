/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.automation.app.audit.AppAuditEvent;
import com.bytechef.ee.automation.app.audit.AppAuditPublisher;
import com.bytechef.ee.automation.app.deployment.AppDeploymentProvider;
import com.bytechef.ee.automation.app.deployment.AppDeploymentSettingsException;
import com.bytechef.ee.automation.app.deployment.AppDeploymentSynchronizer;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppConnection;
import com.bytechef.ee.automation.app.domain.AppDeployment;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.dto.AppBindableConnectionDTO;
import com.bytechef.ee.automation.app.dto.AppBindingDTO;
import com.bytechef.ee.automation.app.dto.AppInstanceDTO;
import com.bytechef.ee.automation.app.dto.AppSecretKeyDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.security.AppInstanceClients;
import com.bytechef.ee.automation.app.service.AppApiCollectionService;
import com.bytechef.ee.automation.app.service.AppConnectionService;
import com.bytechef.ee.automation.app.service.AppDeploymentService;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.app.service.AppWorkflowService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
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
public class AppInstanceFacadeImpl implements AppInstanceFacade {

    private static final String APP_INSTANCE = "AppInstance";
    private static final String APP_INSTANCE_ID = "appInstanceId";
    private static final String APP_VIEW = "APP_VIEW";
    private static final String CONNECTION = "Connection";
    private static final String CONNECTION_EDIT = "CONNECTION_EDIT";
    private static final int DEPLOYMENTS_CHECKED_ON_TERMINATE = 10;
    private static final int MAX_CONFIGURATION_BYTES = 64 * 1024;

    private final AppApiCollectionService appApiCollectionService;
    private final AppAuditPublisher appAuditPublisher;
    private final AppConnectionService appConnectionService;
    private final AppDeploymentService appDeploymentService;
    private final AppDeploymentSynchronizer appDeploymentSynchronizer;
    private final AppInstanceClients appInstanceClients;
    private final AppInstanceService appInstanceService;
    private final AppService appService;
    private final AppWorkflowService appWorkflowService;
    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectionService connectionService;
    private final PermissionService permissionService;
    private final ResourceGrantService resourceGrantService;
    private final ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry;
    private final ResourceVisibilityResolver resourceVisibilityResolver;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public AppInstanceFacadeImpl(
        AppApiCollectionService appApiCollectionService, AppAuditPublisher appAuditPublisher,
        AppConnectionService appConnectionService, AppInstanceClients appInstanceClients,
        AppDeploymentService appDeploymentService, AppDeploymentSynchronizer appDeploymentSynchronizer,
        AppInstanceService appInstanceService, AppService appService, AppWorkflowService appWorkflowService,
        ComponentDefinitionService componentDefinitionService,
        ConnectionService connectionService, PermissionService permissionService,
        ResourceGrantService resourceGrantService, ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry,
        ResourceVisibilityResolver resourceVisibilityResolver, UserService userService) {

        this.appApiCollectionService = appApiCollectionService;
        this.appAuditPublisher = appAuditPublisher;
        this.appConnectionService = appConnectionService;
        this.appDeploymentService = appDeploymentService;
        this.appDeploymentSynchronizer = appDeploymentSynchronizer;
        this.appInstanceClients = appInstanceClients;
        this.appInstanceService = appInstanceService;
        this.appService = appService;
        this.appWorkflowService = appWorkflowService;
        this.componentDefinitionService = componentDefinitionService;
        this.connectionService = connectionService;
        this.permissionService = permissionService;
        this.resourceGrantService = resourceGrantService;
        this.resourceVisibilityPolicyRegistry = resourceVisibilityPolicyRegistry;
        this.resourceVisibilityResolver = resourceVisibilityResolver;
        this.userService = userService;
    }

    @Override
    @PreAuthorize("hasPermission(#appId, 'App', 'APP_MANAGE') and " +
        "hasWorkspaceScopeInEnvironment(#workspaceId, 'APP_MANAGE', #environment)")
    public AppSecretKeyDTO createAppInstance(
        long appId, long workspaceId, Environment environment, String name, AppHostingTarget hostingTarget,
        ResourceVisibility visibility, @Nullable AppUserAuthMode userAuthMode) {

        checkVisibilitySupported(visibility);

        AppUserAuthMode resolvedUserAuthMode =
            userAuthMode == null ? appInstanceClients.getDefaultUserAuthMode() : userAuthMode;

        appInstanceClients.checkAvailable(resolvedUserAuthMode);

        User user = userService.getCurrentUser();

        AppInstance appInstance = new AppInstance();

        appInstance.setAppId(appId);
        appInstance.setWorkspaceId(workspaceId);
        appInstance.setEnvironment(environment);
        appInstance.setName(name);
        appInstance.setHostingTarget(hostingTarget);
        appInstance.setVisibility(visibility);
        appInstance.setOwnerUserId(user.getId());
        appInstance.setUserAuthMode(resolvedUserAuthMode);

        AppInstanceService.CreatedAppInstance createdAppInstance = appInstanceService.create(appInstance);

        AppInstance createdInstance = createdAppInstance.appInstance();

        appAuditPublisher.publish(
            AppAuditEvent.APP_INSTANCE_CREATED, appId,
            Map.of(
                APP_INSTANCE_ID, createdInstance.getId(), "name", createdInstance.getName(), "workspaceId",
                workspaceId, "environment", environment.name()));

        return new AppSecretKeyDTO(toAppInstanceDTO(createdInstance), createdAppInstance.secretKey());
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'AppInstance', 'APP_MANAGE')")
    public void deleteAppInstance(long id) {
        AppInstance appInstance = appInstanceService.getAppInstance(id);

        String registeredClientId = appInstance.getRegisteredClientId();

        if (registeredClientId != null) {
            appInstanceClients.deleteClient(registeredClientId);
        }

        appInstanceService.delete(id);

        resourceGrantService.deleteGrants(APP_INSTANCE, id);

        appAuditPublisher.publish(AppAuditEvent.APP_INSTANCE_DELETED, appInstance.getAppId(),
            Map.of(APP_INSTANCE_ID, id));
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'AppInstance', 'APP_VIEW')")
    @Transactional(readOnly = true)
    public AppInstanceDTO getAppInstance(long id) {
        return toAppInstanceDTO(appInstanceService.getAppInstance(id));
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEnvironment(#workspaceId, 'APP_VIEW', #environment)")
    @Transactional(readOnly = true)
    public List<AppInstanceDTO> getHostedAppInstances(long workspaceId, Environment environment) {
        List<AppInstance> appInstances = appInstanceService.getWorkspaceAppInstances(workspaceId, environment);

        if (appInstances.isEmpty()) {
            return List.of();
        }

        Set<Long> visibleAppInstanceIds = resourceVisibilityResolver.filterVisibleIds(
            APP_INSTANCE, workspaceId,
            appInstances.stream()
                .map(appInstance -> new VisibilityRecord(
                    appInstance.getId(), appInstance.getVisibility(), appInstance.getCreatedBy()))
                .toList());

        Map<Long, App> apps = new HashMap<>();
        List<AppInstanceDTO> hostedAppInstances = new ArrayList<>();

        for (AppInstance appInstance : appInstances) {
            if (!visibleAppInstanceIds.contains(appInstance.getId())) {
                continue;
            }

            App app = apps.computeIfAbsent(appInstance.getAppId(), appService::getApp);

            if (!Objects.equals(app.getWorkspaceId(), workspaceId)) {
                hostedAppInstances.add(AppInstanceDTO.of(app, appInstance));
            }
        }

        return hostedAppInstances;
    }

    @Override
    @Transactional(readOnly = true)
    public void requireAppInstanceInEnvironment(long id, Environment environment) {
        Optional<AppInstance> appInstanceOptional = appInstanceService.fetchAppInstance(id);

        if (appInstanceOptional.isEmpty()) {
            throw newAppInstanceNotFoundException(id);
        }

        AppInstance appInstance = appInstanceOptional.get();

        if (appInstance.getEnvironment() != environment) {
            throw newAppInstanceNotFoundException(id);
        }

        if (!permissionService.hasResourceScope(id, APP_INSTANCE, APP_VIEW)) {
            throw newAppInstanceNotFoundException(id);
        }
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'AppInstance', 'APP_MANAGE')")
    public AppInstanceDTO updateAppInstance(
        long id, @Nullable String name, @Nullable AppHostingTarget hostingTarget, @Nullable String url) {

        return applyUpdate(id, name, hostingTarget, url, null);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'AppInstance', 'APP_MANAGE')")
    public AppInstanceDTO updateAppInstance(
        long id, @Nullable String name, @Nullable AppHostingTarget hostingTarget, @Nullable String url,
        @Nullable Object configuration) {

        return applyUpdate(id, name, hostingTarget, url,
            configuration == null ? null : toConfigurationMap(configuration));
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'AppInstance', 'APP_MANAGE')")
    public AppInstanceDTO updateAppInstanceConfiguration(long id, Object configuration) {
        Map<String, ?> configurationMap = toConfigurationMap(configuration);

        Map<String, ?> previousConfiguration = new HashMap<>(
            appInstanceService.getAppInstance(id)
                .getConfiguration());

        AppInstance appInstance = appInstanceService.updateConfiguration(id, configurationMap);

        if (!Objects.equals(previousConfiguration, configurationMap)) {
            appAuditPublisher.publish(
                AppAuditEvent.APP_INSTANCE_UPDATED, appInstance.getAppId(),
                Map.of(APP_INSTANCE_ID, id, "changedFields", List.of("configuration")));
        }

        return toAppInstanceDTO(appInstance);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'AppInstance', 'APP_MANAGE') and (#deploymentConnectionId == null or " +
        "(hasPermission(#deploymentConnectionId, 'Connection', 'CONNECTION_EDIT') && " +
        "(isResourceOwner(#deploymentConnectionId, 'Connection') || isTenantAdmin())))")
    public AppInstanceDTO updateAppInstanceDeploymentSettings(
        long id, @Nullable Long deploymentConnectionId, @Nullable Map<String, ?> deploymentSettings) {

        Map<String, ?> deploymentSettingsMap = toJsonObjectMap(
            deploymentSettings == null ? Map.of() : deploymentSettings, "deployment settings");

        AppInstance previousAppInstance = appInstanceService.getAppInstance(id);

        if (deploymentConnectionId == null) {
            if (!deploymentSettingsMap.isEmpty()) {
                throw new ConfigurationException(
                    "Deployment settings need a deployment connection", AppErrorType.APP_DEPLOYMENT_SETTINGS_INVALID);
            }
        } else {
            if (!appConnectionService.isBindable(previousAppInstance, deploymentConnectionId)) {
                throw new ConfigurationException(
                    "Connection %s cannot be the deployment connection of app instance %s: it must be an automation connection in the instance's workspace and environment"
                        .formatted(deploymentConnectionId, id),
                    AppErrorType.APP_CONNECTION_NOT_BINDABLE);
            }

            validateDeploymentSettings(previousAppInstance, deploymentConnectionId, deploymentSettingsMap);
        }

        appDeploymentService.checkNoDeploymentInProgress(id);

        Long previousDeploymentConnectionId = previousAppInstance.getDeploymentConnectionId();
        Map<String, ?> previousDeploymentSettings = new HashMap<>(previousAppInstance.getDeploymentSettings());

        AppInstance appInstance = appInstanceService.updateDeploymentSettings(
            id, deploymentConnectionId, deploymentSettingsMap);

        List<String> changedFieldNames = new ArrayList<>();

        if (!Objects.equals(previousDeploymentConnectionId, deploymentConnectionId)) {
            changedFieldNames.add("deploymentConnection");
        }

        if (!Objects.equals(previousDeploymentSettings, deploymentSettingsMap)) {
            changedFieldNames.add("deploymentSettings");
        }

        if (!changedFieldNames.isEmpty()) {
            Map<String, Object> auditData = new HashMap<>();

            auditData.put(APP_INSTANCE_ID, id);
            auditData.put("changedFields", changedFieldNames);

            if (deploymentConnectionId != null) {
                auditData.put("deploymentConnectionId", deploymentConnectionId);
            }

            appAuditPublisher.publish(AppAuditEvent.APP_INSTANCE_UPDATED, appInstance.getAppId(), auditData);
        }

        return toAppInstanceDTO(appInstance);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'AppInstance', 'APP_MANAGE')")
    public AppInstanceDTO updateAppInstanceStatus(long id, AppInstanceStatus status) {
        if (status == AppInstanceStatus.TERMINATED) {
            cancelDeploymentsInProgress(id);
        }

        AppInstance appInstance = appInstanceService.updateStatus(id, status);

        if (status == AppInstanceStatus.TERMINATED) {
            appConnectionService.deleteBindings(id);
            appWorkflowService.deleteBindings(id);
            appApiCollectionService.deleteBindings(id);
        }

        String registeredClientId = appInstance.getRegisteredClientId();

        if (registeredClientId != null && status == AppInstanceStatus.SUSPENDED) {
            appInstanceClients.revokeGrants(registeredClientId);
        }

        if (registeredClientId != null && status == AppInstanceStatus.TERMINATED) {
            appInstanceClients.deleteClient(registeredClientId);

            appInstance = appInstanceService.updateUserAuth(id, appInstance.getUserAuthMode(), null);
        }

        AppAuditEvent appAuditEvent = switch (status) {
            case ACTIVE -> AppAuditEvent.APP_INSTANCE_ACTIVATED;
            case SUSPENDED -> AppAuditEvent.APP_INSTANCE_SUSPENDED;
            case TERMINATED -> AppAuditEvent.APP_INSTANCE_TERMINATED;
        };

        appAuditPublisher.publish(appAuditEvent, appInstance.getAppId(), Map.of(APP_INSTANCE_ID, id));

        return toAppInstanceDTO(appInstance);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'AppInstance', 'APP_MANAGE')")
    public AppInstanceDTO updateAppInstanceUserAuth(long id, AppUserAuthMode userAuthMode) {
        AppInstance appInstance = appInstanceService.getAppInstance(id);

        if (appInstance.getStatus() == AppInstanceStatus.TERMINATED) {
            throw new ConfigurationException(
                "App instance %s is terminated".formatted(id), AppErrorType.APP_INSTANCE_TERMINATED);
        }

        AppUserAuthMode previousUserAuthMode = appInstance.getUserAuthMode();

        if (previousUserAuthMode == userAuthMode) {
            return toAppInstanceDTO(appInstance);
        }

        appInstanceClients.checkAvailable(userAuthMode);

        String registeredClientId = appInstance.getRegisteredClientId();

        if (userAuthMode == AppUserAuthMode.NONE && registeredClientId != null) {
            appInstanceClients.deleteClient(registeredClientId);

            registeredClientId = null;
        }

        if (userAuthMode != AppUserAuthMode.NONE && registeredClientId == null && appInstance.getUrl() != null) {
            appInstance.setUserAuthMode(userAuthMode);

            registeredClientId = appInstanceClients.registerClient(appInstance, getAppName(appInstance));
        }

        AppInstance updatedAppInstance = appInstanceService.updateUserAuth(id, userAuthMode, registeredClientId);

        appAuditPublisher.publish(
            AppAuditEvent.APP_USER_AUTH_CHANGED, updatedAppInstance.getAppId(),
            Map.of(
                APP_INSTANCE_ID, id, "userAuthMode", userAuthMode.name(), "previousUserAuthMode",
                previousUserAuthMode.name()));

        return toAppInstanceDTO(updatedAppInstance);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'AppInstance', 'APP_MANAGE')")
    public AppSecretKeyDTO rotateSecretKey(long id) {
        AppInstanceService.RotatedKey rotatedKey = appInstanceService.rotateSecretKey(id);

        AppInstance appInstance = rotatedKey.appInstance();

        appAuditPublisher.publish(
            AppAuditEvent.APP_SECRET_KEY_ROTATED, appInstance.getAppId(), Map.of(APP_INSTANCE_ID, id));

        return new AppSecretKeyDTO(toAppInstanceDTO(appInstance), rotatedKey.secretKey());
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'AppInstance', 'APP_VIEW')")
    @Transactional(readOnly = true)
    public List<AppBindingDTO> getBindings(long id) {
        return appConnectionService.getBindings(id)
            .stream()
            .map(this::toBindingDTO)
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'AppInstance', 'APP_MANAGE')")
    @Transactional(readOnly = true)
    public List<AppBindableConnectionDTO> getBindableConnections(long id, @Nullable String componentName) {
        AppInstance appInstance = appInstanceService.getAppInstance(id);

        List<Connection> bindableConnections = appConnectionService.getBindableConnections(appInstance)
            .stream()
            .filter(connection -> componentName == null || Objects.equals(connection.getComponentName(), componentName))
            .toList();

        Set<Long> visibleConnectionIds = resourceVisibilityResolver.filterVisibleIds(
            CONNECTION, appInstance.getWorkspaceId(),
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
    @PreAuthorize("hasPermission(#id, 'AppInstance', 'APP_MANAGE') and " +
        "hasPermission(#connectionId, 'Connection', 'CONNECTION_EDIT')")
    public AppBindingDTO bindConnection(
        long id, String bindingKey, long connectionId, List<String> actionNames, List<String> toolNames,
        boolean proxyEnabled, List<String> proxyAllowedMethods) {

        AppInstance appInstance = appInstanceService.getAppInstance(id);

        AppInstanceStatusChecks.checkNotTerminated(appInstance);

        Optional<AppConnection> existingBinding = appConnectionService.fetchBinding(id, bindingKey);

        AppConnection appConnection = appConnectionService.bind(
            appInstance, bindingKey, connectionId, actionNames, toolNames, proxyEnabled, proxyAllowedMethods);

        Map<String, Object> data = new HashMap<>();

        data.put(APP_INSTANCE_ID, id);
        data.put("bindingKey", bindingKey);
        data.put("connectionId", connectionId);

        AppAuditEvent appAuditEvent = AppAuditEvent.APP_CONNECTION_BOUND;

        if (existingBinding.isPresent()) {
            AppConnection previousBinding = existingBinding.get();

            if (Objects.equals(previousBinding.getConnectionId(), connectionId)) {
                appAuditEvent = AppAuditEvent.APP_OPERATIONS_CHANGED;
            } else {
                data.put("previousConnectionId", previousBinding.getConnectionId());
            }
        }

        appAuditPublisher.publish(appAuditEvent, appInstance.getAppId(), data);

        return toBindingDTO(appConnection);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'AppInstance', 'APP_MANAGE')")
    public void unbindConnection(long id, String bindingKey) {
        AppInstance appInstance = appInstanceService.getAppInstance(id);

        if (appConnectionService.unbind(id, bindingKey)) {
            appAuditPublisher.publish(
                AppAuditEvent.APP_CONNECTION_UNBOUND, appInstance.getAppId(),
                Map.of(APP_INSTANCE_ID, id, "bindingKey", bindingKey));
        }
    }

    private void validateDeploymentSettings(
        AppInstance appInstance, long deploymentConnectionId, Map<String, ?> deploymentSettings) {
        Optional<AppDeploymentProvider> appDeploymentProvider = appDeploymentSynchronizer.fetchProvider(
            appInstance.getHostingTarget());

        if (appDeploymentProvider.isEmpty()) {
            return;
        }

        AppDeploymentProvider provider = appDeploymentProvider.get();

        String componentName = provider.getDeploymentConnectionComponentName();

        if (!appConnectionService.isConnectionOfComponent(deploymentConnectionId, componentName)) {
            throw new ConfigurationException(
                "The deployment connection must be a connection of the '%s' component".formatted(componentName),
                AppErrorType.APP_DEPLOYMENT_SETTINGS_INVALID);
        }

        try {
            provider.validateDeploymentSettings(deploymentSettings, appInstance.getEnvironment());
        } catch (AppDeploymentSettingsException appDeploymentSettingsException) {
            throw new ConfigurationException(
                appDeploymentSettingsException.getMessage(), AppErrorType.APP_DEPLOYMENT_SETTINGS_INVALID);
        }
    }

    private static ConfigurationException newAppInstanceNotFoundException(long id) {
        return new ConfigurationException(
            "App instance %s not found".formatted(id), AppErrorType.APP_INSTANCE_NOT_FOUND);
    }

    @SuppressWarnings("unchecked")
    private AppInstanceDTO applyUpdate(
        long id, @Nullable String name, @Nullable AppHostingTarget hostingTarget, @Nullable String url,
        @Nullable Map<String, ?> configurationMap) {

        AppInstance previousAppInstance = appInstanceService.getAppInstance(id);

        EditableFields previousFields = EditableFields.of(previousAppInstance);
        Map<String, ?> previousConfiguration = new HashMap<>(previousAppInstance.getConfiguration());

        AppInstance appInstance = appInstanceService.update(id, name, hostingTarget, url);

        List<String> changedFieldNames = new ArrayList<>(
            previousFields.getChangedFieldNames(EditableFields.of(appInstance)));

        appInstance = synchronizeClient(appInstance, changedFieldNames);

        if (configurationMap != null) {
            appInstance = appInstanceService.updateConfiguration(id, configurationMap);

            if (!Objects.equals(previousConfiguration, configurationMap)) {
                changedFieldNames.add("configuration");
            }
        }

        if (!changedFieldNames.isEmpty()) {
            appAuditPublisher.publish(
                AppAuditEvent.APP_INSTANCE_UPDATED, appInstance.getAppId(),
                toUpdatedAuditData(changedFieldNames, appInstance));
        }

        return toAppInstanceDTO(appInstance);
    }

    private void cancelDeploymentsInProgress(long appInstanceId) {
        List<AppDeployment> appDeploymentsInProgress = appDeploymentService
            .getAppDeployments(appInstanceId, DEPLOYMENTS_CHECKED_ON_TERMINATE)
            .stream()
            .filter(appDeployment -> !appDeployment.getStatus()
                .isTerminal())
            .toList();

        for (AppDeployment appDeployment : appDeploymentsInProgress) {
            String providerDeploymentId = appDeployment.getProviderDeploymentId();

            if (providerDeploymentId != null) {
                appDeploymentSynchronizer.cancelAtProvider(appDeployment, providerDeploymentId);
            }

            appDeploymentSynchronizer.cancel(appDeployment.getId(), providerDeploymentId);
        }
    }

    private String getAppName(AppInstance appInstance) {
        App app = appService.getApp(appInstance.getAppId());

        return app.getName();
    }

    private AppInstance synchronizeClient(AppInstance appInstance, List<String> changedFieldNames) {
        AppUserAuthMode userAuthMode = appInstance.getUserAuthMode();

        if (userAuthMode == AppUserAuthMode.NONE || appInstance.getStatus() == AppInstanceStatus.TERMINATED) {
            return appInstance;
        }

        String registeredClientId = appInstance.getRegisteredClientId();

        if (appInstance.getUrl() == null) {
            if (registeredClientId == null) {
                return appInstance;
            }

            appInstanceClients.deleteClient(registeredClientId);

            return appInstanceService.updateUserAuth(appInstance.getId(), userAuthMode, null);
        }

        if (registeredClientId == null) {
            String clientId = appInstanceClients.registerClient(appInstance, getAppName(appInstance));

            return appInstanceService.updateUserAuth(appInstance.getId(), userAuthMode, clientId);
        }

        if (changedFieldNames.contains("url") || changedFieldNames.contains("name")) {
            appInstanceClients.updateClient(appInstance, getAppName(appInstance));
        }

        return appInstance;
    }

    private static Map<String, ?> toConfigurationMap(@Nullable Object configuration) {
        return toJsonObjectMap(configuration, "configuration");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> toJsonObjectMap(@Nullable Object value, String label) {
        if (!(value instanceof Map<?, ?> map) ||
            !map.keySet()
                .stream()
                .allMatch(String.class::isInstance)) {

            throw new ConfigurationException(
                "App instance %s must be a JSON object".formatted(label),
                AppErrorType.APP_INSTANCE_CONFIGURATION_INVALID);
        }

        Map<String, ?> jsonObjectMap = (Map<String, ?>) map;

        String serializedConfiguration = JsonUtils.write(jsonObjectMap);

        byte[] serializedBytes = serializedConfiguration.getBytes(StandardCharsets.UTF_8);

        if (serializedBytes.length > MAX_CONFIGURATION_BYTES) {
            throw new ConfigurationException(
                "App instance %s must be at most %d bytes".formatted(label, MAX_CONFIGURATION_BYTES),
                AppErrorType.APP_INSTANCE_CONFIGURATION_TOO_LARGE);
        }

        return jsonObjectMap;
    }

    private static Map<String, Object> toUpdatedAuditData(List<String> changedFieldNames, AppInstance appInstance) {
        Map<String, Object> data = new HashMap<>();

        data.put(APP_INSTANCE_ID, appInstance.getId());
        data.put("changedFields", changedFieldNames);

        if (changedFieldNames.contains("name")) {
            data.put("name", appInstance.getName());
        }

        if (changedFieldNames.contains("hostingTarget")) {
            data.put("hostingTarget", String.valueOf(appInstance.getHostingTarget()));
        }

        return data;
    }

    private void checkVisibilitySupported(ResourceVisibility visibility) {
        if (!resourceVisibilityPolicyRegistry.supports(APP_INSTANCE, visibility)) {
            throw new ConfigurationException(
                "App instance does not support %s visibility".formatted(visibility),
                AppErrorType.APP_VISIBILITY_UNSUPPORTED);
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

    private AppInstanceDTO toAppInstanceDTO(AppInstance appInstance) {
        return AppInstanceDTO.of(appService.getApp(appInstance.getAppId()), appInstance);
    }

    private AppBindingDTO toBindingDTO(AppConnection appConnection) {
        Connection connection = connectionService.getConnection(appConnection.getConnectionId());

        return AppBindingDTO.of(appConnection, connection,
            fetchLatestVersion(connection.getComponentName()).orElse(null));
    }

    /**
     * The values an update may change, copied out of the entity because the service mutates the instance it loads.
     */
    private record EditableFields(@Nullable String name, @Nullable AppHostingTarget hostingTarget,
        @Nullable String url) {

        static EditableFields of(AppInstance appInstance) {
            return new EditableFields(appInstance.getName(), appInstance.getHostingTarget(), appInstance.getUrl());
        }

        List<String> getChangedFieldNames(EditableFields updatedFields) {
            List<String> changedFieldNames = new ArrayList<>();

            if (!Objects.equals(name, updatedFields.name)) {
                changedFieldNames.add("name");
            }

            if (hostingTarget != updatedFields.hostingTarget) {
                changedFieldNames.add("hostingTarget");
            }

            if (!Objects.equals(url, updatedFields.url)) {
                changedFieldNames.add("url");
            }

            return changedFieldNames;
        }
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import com.bytechef.async.config.AsyncConfiguration;
import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppConnection;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppKeyAccess;
import com.bytechef.ee.automation.app.domain.AppOperationType;
import com.bytechef.ee.automation.app.dto.AppActionDTO;
import com.bytechef.ee.automation.app.dto.AppBindingDTO;
import com.bytechef.ee.automation.app.dto.AppContextDTO;
import com.bytechef.ee.automation.app.dto.AppCurrentUserDTO;
import com.bytechef.ee.automation.app.dto.AppToolDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.proxy.AppProxyExecutor;
import com.bytechef.ee.automation.app.proxy.ProxyRequest;
import com.bytechef.ee.automation.app.proxy.ProxyResponse;
import com.bytechef.ee.automation.app.proxy.ProxyStorageException;
import com.bytechef.ee.automation.app.security.AppPrincipal;
import com.bytechef.ee.automation.app.security.AppUser;
import com.bytechef.ee.automation.app.security.AppUserTokenVerifier;
import com.bytechef.ee.automation.app.service.AppConnectionService;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppInstanceUsageService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.app.util.AppBindingKeys;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.ai.tool.util.ToolPropertyUtils;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.util.ActionInputSchemaGenerator;
import com.bytechef.platform.component.util.JsonSchemaGeneratorUtils;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationEvent;
import com.bytechef.platform.external.invocation.ExternalInvocationKind;
import com.bytechef.platform.external.invocation.ExternalInvocationRecorder;
import com.bytechef.platform.external.invocation.ExternalInvocationSource;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.stereotype.Service;

/**
 * Runs the actions and tools an app is allowed to call through its bound connections. Identity, allow-list and
 * connection checks happen on the request thread; only the component call itself runs on the app lane
 * ({@code appExecutor}), a bulkhead of its own, so a burst from apps takes permits neither from scheduled runs nor from
 * the API Platform sync path and the editor's Test button. The facade is deliberately not transactional: component code
 * must not hold a database transaction.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class AppExecutionFacadeImpl implements AppExecutionFacade {

    private static final Logger log = LoggerFactory.getLogger(AppExecutionFacadeImpl.class);

    private static final List<String> APP_SCOPES = List.of("APP_VIEW", "APP_USE", "APP_DEPLOY", "APP_MANAGE");
    private static final String APP_INSTANCE_RESOURCE_TYPE = "AppInstance";
    private static final String OPEN_OBJECT_SCHEMA_JSON = "{\"type\":\"object\"}";

    private static final int MAX_CAUSE_DEPTH = 16;
    private static final int MAX_RECORDED_PATH_LENGTH = 200;
    private static final Pattern METHOD_PATTERN = Pattern.compile("[A-Za-z]{1,16}");

    private final ActionDefinitionFacade actionDefinitionFacade;
    private final AppConnectionService appConnectionService;
    private final AppInstanceService appInstanceService;
    private final AppInstanceUsageService appInstanceUsageService;
    private final ApplicationProperties applicationProperties;
    private final AppProxyExecutor appProxyExecutor;
    private final AppService appService;
    private final AppUserTokenVerifier appUserTokenVerifier;
    private final ObjectProvider<AuthorizationServerSettings> authorizationServerSettingsProvider;
    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectionService connectionService;
    private final AsyncTaskExecutor appExecutor;
    private final Semaphore appLanePermits;
    private final PermissionService permissionService;
    private final ExternalInvocationRecorder toolExecutionRecorder;
    private final UserService userService;
    private final WorkspaceConnectionService workspaceConnectionService;
    private final WorkspaceUserService workspaceUserService;

    @SuppressFBWarnings({
        "EI", "EI2", "CT_CONSTRUCTOR_THROW"
    })
    public AppExecutionFacadeImpl(
        ActionDefinitionFacade actionDefinitionFacade, AppConnectionService appConnectionService,
        AppInstanceService appInstanceService, AppInstanceUsageService appInstanceUsageService,
        ApplicationProperties applicationProperties, AppProxyExecutor appProxyExecutor, AppService appService,
        AppUserTokenVerifier appUserTokenVerifier,
        ObjectProvider<AuthorizationServerSettings> authorizationServerSettingsProvider,
        ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        ClusterElementDefinitionService clusterElementDefinitionService,
        ComponentDefinitionService componentDefinitionService,
        ConnectionService connectionService,
        @Qualifier(AsyncConfiguration.APP_EXECUTOR) AsyncTaskExecutor appExecutor,
        PermissionService permissionService, ExternalInvocationRecorder toolExecutionRecorder, UserService userService,
        WorkspaceConnectionService workspaceConnectionService, WorkspaceUserService workspaceUserService) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.appConnectionService = appConnectionService;
        this.appInstanceService = appInstanceService;
        this.appInstanceUsageService = appInstanceUsageService;
        this.applicationProperties = applicationProperties;
        this.appProxyExecutor = appProxyExecutor;
        this.appService = appService;
        this.appUserTokenVerifier = appUserTokenVerifier;
        this.authorizationServerSettingsProvider = authorizationServerSettingsProvider;
        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.componentDefinitionService = componentDefinitionService;
        this.connectionService = connectionService;
        this.appExecutor = appExecutor;
        this.appLanePermits = new Semaphore(getConcurrencyLimit(applicationProperties), true);
        this.permissionService = permissionService;
        this.toolExecutionRecorder = toolExecutionRecorder;
        this.userService = userService;
        this.workspaceConnectionService = workspaceConnectionService;
        this.workspaceUserService = workspaceUserService;
    }

    @Override
    public Object executeAction(
        String bindingKey, String actionName, @Nullable Integer componentVersion, Map<String, Object> input) {

        ResolvedBinding resolved = resolveBinding(bindingKey, AppOperationType.ACTION, actionName);
        Connection connection = resolved.connection();
        int version = componentVersion != null ? componentVersion : getLatestVersion(connection.getComponentName());

        ExternalInvocationEvent.Builder eventBuilder = eventBuilder(
            ExternalInvocationSource.APP_ACTION, actionName, resolved)
                .componentVersion(version)
                .operationName(actionName);

        return toolExecutionRecorder.record(
            eventBuilder,
            () -> runOnAppLane(() -> SecurityUtils.runAsSystem(() -> actionDefinitionFacade.executePerform(
                connection.getComponentName(), version, actionName, null, null, null, null, null, input,
                Map.of(connection.getComponentName(), connection.getId()), Map.of(),
                (long) resolved.appInstance()
                    .getEnvironmentId(),
                PlatformType.AUTOMATION, false, null, null, null))));
    }

    @Override
    public Object executeTool(String bindingKey, String toolName, Map<String, Object> input) {
        ResolvedBinding resolved = resolveBinding(bindingKey, AppOperationType.TOOL, toolName);
        Connection connection = resolved.connection();
        int version = getLatestVersion(connection.getComponentName());

        ExternalInvocationEvent.Builder eventBuilder = eventBuilder(
            ExternalInvocationSource.APP_TOOL, flatToolName(bindingKey, toolName), resolved)
                .componentVersion(version)
                .operationName(toolName);

        return toolExecutionRecorder.record(
            eventBuilder,
            () -> runOnAppLane(() -> SecurityUtils.runAsSystem(() -> clusterElementDefinitionFacade.executeTool(
                connection.getComponentName(), version, toolName, input, connection.getId()))));
    }

    @Override
    public List<AppActionDTO> getActions() {
        AppInstance appInstance = resolveApp().appInstance();

        List<AppActionDTO> actions = new ArrayList<>();

        for (AppConnection binding : appConnectionService.getBindings(appInstance.getId())) {
            List<String> allowedActionNames = binding.getOperationNames(AppOperationType.ACTION);

            if (allowedActionNames.isEmpty()) {
                continue;
            }

            Connection connection = connectionService.getConnection(binding.getConnectionId());

            Optional<ComponentDefinition> componentDefinition = componentDefinitionService.fetchComponentDefinition(
                connection.getComponentName(), null);

            if (componentDefinition.isEmpty()) {
                continue;
            }

            for (ActionDefinition definition : componentDefinition.get()
                .getActions()) {

                if (!allowedActionNames.contains(definition.getName())) {
                    continue;
                }

                actions.add(
                    new AppActionDTO(
                        binding.getBindingKey(), connection.getComponentName(), definition.getName(),
                        definition.getDescription(),
                        generateActionInputSchema(connection.getComponentName(), definition)));
            }
        }

        return actions;
    }

    @Override
    public List<AppBindingDTO> getBindings() {
        AppInstance appInstance = resolveApp().appInstance();

        return appConnectionService.getBindings(appInstance.getId())
            .stream()
            .map(binding -> {
                Connection connection = connectionService.getConnection(binding.getConnectionId());

                Integer componentVersion = fetchLatestVersion(connection.getComponentName()).orElse(null);

                return AppBindingDTO.of(binding, connection, componentVersion);
            })
            .toList();
    }

    @Override
    public AppContextDTO getContext() {
        AppInstance appInstance = resolveApp().appInstance();

        App app = appService.getApp(appInstance.getAppId());

        String clientId = appInstance.getRegisteredClientId();
        String issuer = clientId == null ? null : appUserTokenVerifier.getIssuer();

        String authorizationEndpoint = null;
        String tokenEndpoint = null;

        AuthorizationServerSettings authorizationServerSettings = authorizationServerSettingsProvider.getIfAvailable();

        if (issuer != null && authorizationServerSettings != null) {
            authorizationEndpoint = issuer + authorizationServerSettings.getAuthorizationEndpoint();
            tokenEndpoint = issuer + authorizationServerSettings.getTokenEndpoint();
        }

        Map<String, ?> configuration = appInstance.getConfiguration();

        return new AppContextDTO(
            Objects.requireNonNull(app.getId()), app.getName(), Objects.requireNonNull(appInstance.getId()),
            appInstance.getName(), appInstance.getEnvironment(), configuration == null ? Map.of() : configuration,
            appInstance.getUserAuthMode(), issuer, clientId, authorizationEndpoint, tokenEndpoint);
    }

    @Override
    public AppCurrentUserDTO getCurrentUser() {
        ResolvedApp resolvedApp = resolveApp();

        AppUser appUser = resolvedApp.principal()
            .user();

        if (appUser == null) {
            throw new ConfigurationException("A signed-in user is required", AppErrorType.APP_USER_REQUIRED);
        }

        AppInstance appInstance = resolvedApp.appInstance();

        User user = userService.getUser(appUser.id());

        Long appInstanceId = appInstance.getId();

        List<String> scopes = appUserTokenVerifier.callAsUser(
            user, () -> APP_SCOPES.stream()
                .filter(scope -> permissionService.hasResourceScope(appInstanceId, APP_INSTANCE_RESOURCE_TYPE, scope))
                .toList());

        AppCurrentUserDTO currentUser = new AppCurrentUserDTO(
            appUser.id(), user.getLogin(), user.getFirstName(), user.getLastName(), user.getEmail(),
            getWorkspaceRoleName(user, appInstance), scopes);

        touchLastUsedDate(appUser.id(), appInstanceId);

        return currentUser;
    }

    @Override
    public List<AppToolDTO> getTools() {
        AppInstance appInstance = resolveApp().appInstance();

        List<AppToolDTO> tools = new ArrayList<>();

        for (AppConnection binding : appConnectionService.getBindings(appInstance.getId())) {
            List<String> allowedToolNames = binding.getOperationNames(AppOperationType.TOOL);

            if (allowedToolNames.isEmpty()) {
                continue;
            }

            Connection connection = connectionService.getConnection(binding.getConnectionId());

            Optional<Integer> version = fetchLatestVersion(connection.getComponentName());

            if (version.isEmpty()) {
                continue;
            }

            for (ClusterElementDefinition definition : clusterElementDefinitionService.getClusterElementDefinitions(
                connection.getComponentName(), version.get(), BaseToolFunction.TOOLS)) {

                if (!allowedToolNames.contains(definition.getName())) {
                    continue;
                }

                tools.add(
                    new AppToolDTO(
                        flatToolName(binding.getBindingKey(), definition.getName()), binding.getBindingKey(),
                        connection.getComponentName(), definition.getName(), definition.getDescription(),
                        generateToolInputSchema(connection.getComponentName(), definition)));
            }
        }

        return tools;
    }

    @Override
    public ProxyResponse proxy(String bindingKey, ProxyRequest request) {
        ResolvedBinding resolved = resolveProxyBinding(bindingKey);

        String operation = proxyOperationName(request);

        ExternalInvocationEvent.Builder eventBuilder = eventBuilder(
            ExternalInvocationSource.APP_PROXY, ExternalInvocationKind.HTTP_PROXY, operation, resolved)
                .operationName(operation);

        return toolExecutionRecorder.recordWithErrorMessage(
            eventBuilder,
            () -> runOnAppLane(() -> SecurityUtils.runAsSystem(() -> executeProxy(resolved, request))),
            response -> response.status() >= 400 ? "upstream " + response.status() : null);
    }

    static String flatToolName(String bindingKey, String toolName) {
        String key = AppBindingKeys.toFlatKey(bindingKey);

        StringBuilder tool = new StringBuilder();

        for (int i = 0; i < toolName.length(); i++) {
            char character = toolName.charAt(i);

            if (Character.isUpperCase(character) && i > 0) {
                tool.append('_');
            }

            tool.append(Character.toUpperCase(character));
        }

        return key + "__" + tool;
    }

    /**
     * One action whose properties cannot be turned into a schema must not take down the listing of every binding: it is
     * listed with an open object schema and the failure is logged with the component and action names.
     */
    private static Map<String, Object> generateActionInputSchema(String componentName, ActionDefinition definition) {
        try {
            return ActionInputSchemaGenerator.generateInputSchema(definition.getProperties());
        } catch (RuntimeException runtimeException) {
            log.warn(
                "Unable to generate the input schema of action '{}' of component '{}', listing it as an open object",
                definition.getName(), componentName, runtimeException);

            return Map.of("type", "object");
        }
    }

    /**
     * The tool counterpart of {@link #generateActionInputSchema(String, ActionDefinition)}.
     */
    private static String generateToolInputSchema(String componentName, ClusterElementDefinition definition) {
        try {
            return JsonSchemaGeneratorUtils.generateInputSchema(
                ToolPropertyUtils.withoutToolOverrideProperties(definition.getProperties()));
        } catch (RuntimeException runtimeException) {
            log.warn(
                "Unable to generate the input schema of tool '{}' of component '{}', listing it as an open object",
                definition.getName(), componentName, runtimeException);

            return OPEN_OBJECT_SCHEMA_JSON;
        }
    }

    /**
     * The only connection status check on the execution path: the tool path
     * ({@code ClusterElementDefinitionFacadeImpl}) does not validate connection status itself, so a connection that is
     * not ACTIVE must be refused here.
     */
    private Connection checkAvailable(AppInstance appInstance, AppConnection binding) {
        Connection connection = connectionService.fetchConnection(binding.getConnectionId())
            .orElseThrow(() -> unavailable(binding));

        boolean inWorkspace = workspaceConnectionService
            .getWorkspaceConnections(appInstance.getWorkspaceId())
            .stream()
            .map(WorkspaceConnection::getConnectionId)
            .anyMatch(workspaceConnectionId -> Objects.equals(workspaceConnectionId, connection.getId()));

        if (!inWorkspace || connection.getEnvironmentId() != appInstance.getEnvironmentId()
            || connection.getStatus() != ConnectionStatus.ACTIVE) {

            throw unavailable(binding);
        }

        return connection;
    }

    private ExternalInvocationEvent.Builder eventBuilder(
        ExternalInvocationSource surface, String toolName, ResolvedBinding resolved) {

        return eventBuilder(surface, ExternalInvocationKind.COMPONENT, toolName, resolved);
    }

    private ExternalInvocationEvent.Builder eventBuilder(
        ExternalInvocationSource surface, ExternalInvocationKind kind, String toolName, ResolvedBinding resolved) {

        AppInstance appInstance = resolved.appInstance();
        Connection connection = resolved.connection();

        AppPrincipal principal = resolved.principal();

        return ExternalInvocationEvent.builder(PlatformType.AUTOMATION, surface, kind, toolName)
            .appId(appInstance.getAppId())
            .appInstanceId(appInstance.getId())
            .userId(principal.userId())
            .workspaceId(appInstance.getWorkspaceId())
            .environment(appInstance.getEnvironmentId())
            .componentName(connection.getComponentName())
            .connectionId(connection.getId());
    }

    /**
     * Runs on the app lane. A transport failure reaching the upstream (an {@link IOException} somewhere in the cause
     * chain of the exception the component HTTP client throws) is reported with a fixed message: the original message
     * can carry upstream body text, which must not reach the app or the execution record. A local storage failure is
     * reported as {@code APP_PROXY_STORAGE_FAILED}, never as an unavailable upstream. Errors the executor already
     * classified (a failed token refresh, a refused path, a size limit) pass through unchanged.
     */
    private ProxyResponse executeProxy(ResolvedBinding resolved, ProxyRequest request) {
        try {
            return appProxyExecutor.execute(resolved.binding(), resolved.connection(), request);
        } catch (ConfigurationException configurationException) {
            throw configurationException;
        } catch (ProxyStorageException proxyStorageException) {
            logProxyFailure("Proxy storage failed", resolved, proxyStorageException);

            throw new ConfigurationException(
                "The proxy could not store or read one of its temporary files", proxyStorageException,
                AppErrorType.APP_PROXY_STORAGE_FAILED);
        } catch (RuntimeException runtimeException) {
            if (isUpstreamTransportFailure(runtimeException)) {
                logProxyFailure("Proxy upstream unavailable", resolved, runtimeException);

                throw new ConfigurationException(
                    "The upstream is unavailable", runtimeException, AppErrorType.APP_PROXY_UPSTREAM_UNAVAILABLE);
            }

            throw runtimeException;
        }
    }

    private static void logProxyFailure(String message, ResolvedBinding resolved, Exception exception) {
        AppInstance appInstance = resolved.appInstance();
        AppConnection binding = resolved.binding();
        Connection connection = resolved.connection();

        log.warn(
            "{} for app {}, binding '{}', connection {}", message, appInstance.getAppId(), binding.getBindingKey(),
            connection.getId(), exception);
    }

    private @Nullable String getWorkspaceRoleName(User user, AppInstance appInstance) {
        boolean tenantAdmin = appUserTokenVerifier.callAsUser(user, permissionService::isTenantAdmin);

        if (tenantAdmin) {
            return WorkspaceRole.ADMIN.name();
        }

        List<WorkspaceUser> workspaceUsers = workspaceUserService.getUserWorkspaceUsers(
            Objects.requireNonNull(user.getId()))
            .stream()
            .filter(workspaceUser -> Objects.equals(workspaceUser.getWorkspaceId(), appInstance.getWorkspaceId()))
            .toList();

        Optional<WorkspaceUser> workspaceUser = workspaceUsers.stream()
            .filter(candidate -> Objects.equals(candidate.getEnvironment(), appInstance.getEnvironment()))
            .findFirst();

        if (workspaceUser.isEmpty() && workspaceUsers.stream()
            .noneMatch(candidate -> candidate.getEnvironment() != null)) {

            workspaceUser = workspaceUsers.stream()
                .filter(candidate -> candidate.getEnvironment() == null)
                .findFirst();
        }

        return workspaceUser.map(WorkspaceUser::getWorkspaceRole)
            .filter(ordinal -> ordinal >= 0 && ordinal < WorkspaceRole.values().length)
            .map(ordinal -> WorkspaceRole.values()[ordinal].name())
            .orElse(null);
    }

    private void touchLastUsedDate(long userId, long appInstanceId) {
        try {
            appInstanceUsageService.touchLastUsedDate(userId, appInstanceId);
        } catch (RuntimeException runtimeException) {
            if (log.isWarnEnabled()) {
                log.warn(
                    "Failed to record the last use of app instance {}: {}", appInstanceId,
                    runtimeException.getClass()
                        .getName());
            }
        }
    }

    private Optional<Integer> fetchLatestVersion(String componentName) {
        return componentDefinitionService.fetchComponentDefinition(componentName, null)
            .map(ComponentDefinition::getVersion);
    }

    private int getLatestVersion(String componentName) {
        return componentDefinitionService.getComponentDefinition(componentName, null)
            .getVersion();
    }

    private ResolvedApp resolveApp() {
        AppPrincipal principal = AppPrincipal.getCurrent();
        AppInstance appInstance = appInstanceService.getAppInstance(principal.appInstanceId());
        App app = appService.getApp(appInstance.getAppId());

        if (!AppKeyAccess.isAllowed(app.getStatus(), appInstance.getStatus())) {
            throw new ConfigurationException("App is disabled", AppErrorType.APP_DISABLED);
        }

        return new ResolvedApp(principal, appInstance);
    }

    private ResolvedBinding resolveBinding(String bindingKey, AppOperationType type, String operationName) {
        ResolvedApp resolvedApp = resolveApp();
        AppInstance appInstance = resolvedApp.appInstance();

        AppConnection binding = appConnectionService.fetchBinding(appInstance.getId(), bindingKey)
            .orElseThrow(() -> new ConfigurationException(
                "Binding '%s' not found".formatted(bindingKey), AppErrorType.APP_CONNECTION_NOT_FOUND));

        if (!binding.allows(type, operationName)) {
            List<String> allowedOperationNames = binding.getOperationNames(type);

            throw new ConfigurationException(
                "'%s' is not allowed on binding '%s'; allowed %s: %s".formatted(
                    operationName, bindingKey, type == AppOperationType.ACTION ? "actions" : "tools",
                    allowedOperationNames.isEmpty() ? "none" : String.join(", ", allowedOperationNames)),
                AppErrorType.APP_OPERATION_NOT_ALLOWED);
        }

        return new ResolvedBinding(
            resolvedApp.principal(), appInstance, binding, checkAvailable(appInstance, binding));
    }

    /**
     * Unlike {@link #resolveBinding}, no action or tool allow-list applies: the proxy executor owns the binding's proxy
     * switch and method allow-list, and answers a refusal with the same message format.
     */
    private ResolvedBinding resolveProxyBinding(String bindingKey) {
        ResolvedApp resolvedApp = resolveApp();
        AppInstance appInstance = resolvedApp.appInstance();

        AppConnection binding = appConnectionService.fetchBinding(appInstance.getId(), bindingKey)
            .orElseThrow(() -> new ConfigurationException(
                "Binding '%s' not found".formatted(bindingKey), AppErrorType.APP_CONNECTION_NOT_FOUND));

        return new ResolvedBinding(
            resolvedApp.principal(), appInstance, binding, checkAvailable(appInstance, binding));
    }

    /**
     * Submits the platform call to the app lane. The timeout is validated before submitting: once the component has
     * been started its side effects cannot be undone, so a missing timeout must fail the request instead of running the
     * component and then failing. Waiting for a lane permit counts against the same timeout, so a saturated lane
     * answers {@code APP_EXECUTION_TIMEOUT} instead of parking the request thread. The call runs as the system
     * principal, exactly like components in workflow workers: the app principal propagated onto the lane is neither a
     * user nor a login, so the OAuth2 token refresh (which updates the connection under an owner-or-admin check) would
     * fail with it.
     */
    private <T> T runOnAppLane(Callable<T> callable) {
        Duration timeout = applicationProperties.getApp()
            .getExecution()
            .getTimeout();

        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalStateException(
                "bytechef.app.execution.timeout must be set to a positive duration, but was " + timeout);
        }

        long deadlineNanos = System.nanoTime() + timeout.toNanos();

        acquireLanePermit(timeout);

        long remainingNanos = deadlineNanos - System.nanoTime();

        if (remainingNanos <= 0) {
            appLanePermits.release();

            throw laneTimeout(timeout);
        }

        FutureTask<T> futureTask = new FutureTask<>(callable);

        try {
            appExecutor.execute(() -> {
                try {
                    futureTask.run();
                } finally {
                    appLanePermits.release();
                }
            });
        } catch (RuntimeException runtimeException) {
            appLanePermits.release();

            throw runtimeException;
        }

        try {
            return futureTask.get(remainingNanos, TimeUnit.NANOSECONDS);
        } catch (TimeoutException timeoutException) {
            futureTask.cancel(true);

            throw new ConfigurationException(
                "Execution exceeded " + timeout, timeoutException, AppErrorType.APP_EXECUTION_TIMEOUT);
        } catch (InterruptedException interruptedException) {
            futureTask.cancel(true);

            Thread.currentThread()
                .interrupt();

            throw new ConfigurationException(
                "Execution was interrupted", interruptedException, AppErrorType.APP_EXECUTION_TIMEOUT);
        } catch (ExecutionException executionException) {
            Throwable cause = executionException.getCause();

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            if (cause instanceof Error error) {
                throw error;
            }

            throw new IllegalStateException(cause);
        }
    }

    private void acquireLanePermit(Duration timeout) {
        boolean acquired;

        try {
            acquired = appLanePermits.tryAcquire(timeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new ConfigurationException(
                "Execution was interrupted", interruptedException, AppErrorType.APP_EXECUTION_TIMEOUT);
        }

        if (!acquired) {
            throw laneTimeout(timeout);
        }
    }

    private static int getConcurrencyLimit(ApplicationProperties applicationProperties) {
        int concurrencyLimit = applicationProperties.getApp()
            .getExecution()
            .getConcurrencyLimit();

        if (concurrencyLimit < 1) {
            throw new IllegalStateException(
                "bytechef.app.execution.concurrency-limit must be a positive number, but was " + concurrencyLimit);
        }

        return concurrencyLimit;
    }

    private static boolean isUpstreamTransportFailure(RuntimeException runtimeException) {
        Throwable cause = runtimeException;

        for (int depth = 0; cause != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (cause instanceof IOException) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }

    private static String proxyOperationName(ProxyRequest request) {
        String method = request.method();
        String path = request.path();

        int queryIndex = StringUtils.indexOfAny(path, "?#");
        int end = Math.min(queryIndex < 0 ? path.length() : queryIndex, MAX_RECORDED_PATH_LENGTH);

        StringBuilder operation = new StringBuilder();

        if (METHOD_PATTERN.matcher(method)
            .matches()) {

            operation.append(method.toUpperCase(Locale.ROOT));
        } else {
            operation.append("INVALID");
        }

        operation.append(' ');

        for (int i = 0; i < end; i++) {
            char character = path.charAt(i);

            operation.append(Character.isISOControl(character) ? '?' : character);
        }

        return operation.toString();
    }

    private static ConfigurationException laneTimeout(Duration timeout) {
        String message = "No app execution slot became free within " + timeout;

        return new ConfigurationException(message, new TimeoutException(message), AppErrorType.APP_EXECUTION_TIMEOUT);
    }

    private static ConfigurationException unavailable(AppConnection binding) {
        return new ConfigurationException(
            "Connection of binding '%s' is unavailable".formatted(binding.getBindingKey()),
            AppErrorType.APP_CONNECTION_UNAVAILABLE);
    }

    private record ResolvedApp(AppPrincipal principal, AppInstance appInstance) {
    }

    private record ResolvedBinding(
        AppPrincipal principal, AppInstance appInstance, AppConnection binding, Connection connection) {
    }
}

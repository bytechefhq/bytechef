/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import com.bytechef.async.config.AsyncConfiguration;
import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppConnection;
import com.bytechef.ee.automation.app.domain.AppOperationType;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.dto.AppActionDTO;
import com.bytechef.ee.automation.app.dto.AppBindingDTO;
import com.bytechef.ee.automation.app.dto.AppToolDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.proxy.AppProxyExecutor;
import com.bytechef.ee.automation.app.proxy.ProxyRequest;
import com.bytechef.ee.automation.app.proxy.ProxyResponse;
import com.bytechef.ee.automation.app.proxy.ProxyStorageException;
import com.bytechef.ee.automation.app.security.AppPrincipal;
import com.bytechef.ee.automation.app.service.AppConnectionService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.app.util.AppBindingKeys;
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
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.tool.execution.ToolExecutionEvent;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
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

    private static final String OPEN_OBJECT_SCHEMA_JSON = "{\"type\":\"object\"}";

    private static final int MAX_CAUSE_DEPTH = 16;
    private static final int MAX_RECORDED_PATH_LENGTH = 200;
    private static final Pattern METHOD_PATTERN = Pattern.compile("[A-Za-z]{1,16}");

    private final ActionDefinitionFacade actionDefinitionFacade;
    private final AppConnectionService appConnectionService;
    private final ApplicationProperties applicationProperties;
    private final AppProxyExecutor appProxyExecutor;
    private final AppService appService;
    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectionService connectionService;
    private final AsyncTaskExecutor appExecutor;
    private final Semaphore appLanePermits;
    private final ToolExecutionRecorder toolExecutionRecorder;
    private final WorkspaceConnectionService workspaceConnectionService;

    @SuppressFBWarnings({
        "EI", "EI2", "CT_CONSTRUCTOR_THROW"
    })
    public AppExecutionFacadeImpl(
        ActionDefinitionFacade actionDefinitionFacade, AppConnectionService appConnectionService,
        ApplicationProperties applicationProperties, AppProxyExecutor appProxyExecutor, AppService appService,
        ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        ClusterElementDefinitionService clusterElementDefinitionService,
        ComponentDefinitionService componentDefinitionService,
        ConnectionService connectionService,
        @Qualifier(AsyncConfiguration.APP_EXECUTOR) AsyncTaskExecutor appExecutor,
        ToolExecutionRecorder toolExecutionRecorder, WorkspaceConnectionService workspaceConnectionService) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.appConnectionService = appConnectionService;
        this.applicationProperties = applicationProperties;
        this.appProxyExecutor = appProxyExecutor;
        this.appService = appService;
        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.componentDefinitionService = componentDefinitionService;
        this.connectionService = connectionService;
        this.appExecutor = appExecutor;
        this.appLanePermits = new Semaphore(getConcurrencyLimit(applicationProperties), true);
        this.toolExecutionRecorder = toolExecutionRecorder;
        this.workspaceConnectionService = workspaceConnectionService;
    }

    @Override
    public Object executeAction(
        String bindingKey, String actionName, @Nullable Integer componentVersion, Map<String, Object> input) {

        ResolvedBinding resolved = resolveBinding(bindingKey, AppOperationType.ACTION, actionName);
        Connection connection = resolved.connection();
        int version = componentVersion != null ? componentVersion : getLatestVersion(connection.getComponentName());

        ToolExecutionEvent.Builder eventBuilder = eventBuilder(
            ToolExecutionSurface.AUTOMATION_APP_ACTION, actionName, resolved)
                .componentVersion(version)
                .operationName(actionName);

        return toolExecutionRecorder.record(
            eventBuilder,
            () -> runOnAppLane(() -> SecurityUtils.runAsSystem(() -> actionDefinitionFacade.executePerform(
                connection.getComponentName(), version, actionName, null, null, null, null, null, input,
                Map.of(connection.getComponentName(), connection.getId()), Map.of(),
                (long) resolved.app()
                    .getEnvironmentId(),
                PlatformType.AUTOMATION, false, null, null, null))));
    }

    @Override
    public Object executeTool(String bindingKey, String toolName, Map<String, Object> input) {
        ResolvedBinding resolved = resolveBinding(bindingKey, AppOperationType.TOOL, toolName);
        Connection connection = resolved.connection();
        int version = getLatestVersion(connection.getComponentName());

        ToolExecutionEvent.Builder eventBuilder = eventBuilder(
            ToolExecutionSurface.AUTOMATION_APP_TOOL, flatToolName(bindingKey, toolName), resolved)
                .componentVersion(version)
                .operationName(toolName);

        return toolExecutionRecorder.record(
            eventBuilder,
            () -> runOnAppLane(() -> SecurityUtils.runAsSystem(() -> clusterElementDefinitionFacade.executeTool(
                connection.getComponentName(), version, toolName, input, connection.getId()))));
    }

    @Override
    public List<AppActionDTO> getActions() {
        App app = resolveApp().app();

        List<AppActionDTO> actions = new ArrayList<>();

        for (AppConnection binding : appConnectionService.getBindings(app.getId())) {
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
        App app = resolveApp().app();

        return appConnectionService.getBindings(app.getId())
            .stream()
            .map(binding -> {
                Connection connection = connectionService.getConnection(binding.getConnectionId());

                Integer componentVersion = fetchLatestVersion(connection.getComponentName()).orElse(null);

                return AppBindingDTO.of(binding, connection, componentVersion);
            })
            .toList();
    }

    @Override
    public List<AppToolDTO> getTools() {
        App app = resolveApp().app();

        List<AppToolDTO> tools = new ArrayList<>();

        for (AppConnection binding : appConnectionService.getBindings(app.getId())) {
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

        ToolExecutionEvent.Builder eventBuilder = eventBuilder(
            ToolExecutionSurface.AUTOMATION_APP_PROXY, ToolExecutionKind.HTTP_PROXY, operation, resolved)
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
    private Connection checkAvailable(App app, AppConnection binding) {
        Connection connection = connectionService.fetchConnection(binding.getConnectionId())
            .orElseThrow(() -> unavailable(binding));

        boolean inWorkspace = workspaceConnectionService
            .getWorkspaceConnections(Objects.requireNonNull(app.getWorkspaceId(), "workspaceId"))
            .stream()
            .map(WorkspaceConnection::getConnectionId)
            .anyMatch(workspaceConnectionId -> Objects.equals(workspaceConnectionId, connection.getId()));

        if (!inWorkspace || connection.getEnvironmentId() != app.getEnvironmentId()
            || connection.getStatus() != ConnectionStatus.ACTIVE) {

            throw unavailable(binding);
        }

        return connection;
    }

    private ToolExecutionEvent.Builder eventBuilder(
        ToolExecutionSurface surface, String toolName, ResolvedBinding resolved) {

        return eventBuilder(surface, ToolExecutionKind.COMPONENT, toolName, resolved);
    }

    private ToolExecutionEvent.Builder eventBuilder(
        ToolExecutionSurface surface, ToolExecutionKind kind, String toolName, ResolvedBinding resolved) {

        App app = resolved.app();
        Connection connection = resolved.connection();

        return ToolExecutionEvent.builder(PlatformType.AUTOMATION, surface, kind, toolName)
            .appId(app.getId())
            .workspaceId(app.getWorkspaceId())
            .environment(app.getEnvironmentId())
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
        App app = resolved.app();
        AppConnection binding = resolved.binding();
        Connection connection = resolved.connection();

        log.warn(
            "{} for app {}, binding '{}', connection {}", message, app.getId(), binding.getBindingKey(),
            connection.getId(), exception);
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
        App app = appService.getApp(principal.appId());

        if (app.getStatus() != AppStatus.ENABLED) {
            throw new ConfigurationException("App is disabled", AppErrorType.APP_DISABLED);
        }

        return new ResolvedApp(principal, app);
    }

    private ResolvedBinding resolveBinding(String bindingKey, AppOperationType type, String operationName) {
        ResolvedApp resolvedApp = resolveApp();
        App app = resolvedApp.app();

        AppConnection binding = appConnectionService.fetchBinding(app.getId(), bindingKey)
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

        return new ResolvedBinding(resolvedApp.principal(), app, binding, checkAvailable(app, binding));
    }

    /**
     * Unlike {@link #resolveBinding}, no action or tool allow-list applies: the proxy executor owns the binding's proxy
     * switch and method allow-list, and answers a refusal with the same message format.
     */
    private ResolvedBinding resolveProxyBinding(String bindingKey) {
        ResolvedApp resolvedApp = resolveApp();
        App app = resolvedApp.app();

        AppConnection binding = appConnectionService.fetchBinding(app.getId(), bindingKey)
            .orElseThrow(() -> new ConfigurationException(
                "Binding '%s' not found".formatted(bindingKey), AppErrorType.APP_CONNECTION_NOT_FOUND));

        return new ResolvedBinding(resolvedApp.principal(), app, binding, checkAvailable(app, binding));
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

    private record ResolvedApp(AppPrincipal principal, App app) {
    }

    private record ResolvedBinding(AppPrincipal principal, App app, AppConnection binding, Connection connection) {
    }
}

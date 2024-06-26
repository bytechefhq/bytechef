/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.component.registry.facade;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.Authorization.EXPIRES_IN;
import static com.bytechef.component.definition.Authorization.REFRESH_TOKEN;
import static com.bytechef.component.definition.Authorization.RefreshTokenResponse;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.platform.component.exception.ComponentExecutionException;
import com.bytechef.platform.component.registry.definition.ActionContextImpl;
import com.bytechef.platform.component.registry.definition.factory.ContextFactory;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.component.registry.domain.Option;
import com.bytechef.platform.component.registry.domain.Output;
import com.bytechef.platform.component.registry.domain.Property;
import com.bytechef.platform.component.registry.exception.ActionDefinitionErrorType;
import com.bytechef.platform.component.registry.service.ActionDefinitionService;
import com.bytechef.platform.component.registry.service.ConnectionDefinitionService;
import com.bytechef.platform.component.registry.util.RefreshCredentialsUtils;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.exception.ErrorType;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
@Service("actionDefinitionFacade")
public class ActionDefinitionFacadeImpl implements ActionDefinitionFacade {

    private static final Cache<Long, ReentrantLock> REENTRANT_LOCK_CACHE =
        Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .maximumSize(100000)
            .build();

    private final ConnectionService connectionService;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final ContextFactory contextFactory;
    private final ActionDefinitionService actionDefinitionService;

    @SuppressFBWarnings("EI")
    public ActionDefinitionFacadeImpl(
        ConnectionService connectionService, ConnectionDefinitionService connectionDefinitionService,
        ContextFactory contextFactory,
        ActionDefinitionService actionDefinitionService) {

        this.contextFactory = contextFactory;
        this.actionDefinitionService = actionDefinitionService;
        this.connectionService = connectionService;
        this.connectionDefinitionService = connectionDefinitionService;
    }

    @Override
    public List<Property> executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        Map<String, ?> inputParameters, @NonNull List<String> lookupDependsOnPaths, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        ActionContext actionContext = contextFactory.createActionContext(
            componentName, componentVersion, actionName, null, null, null, componentConnection);

        return executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, actionContext,
            ActionDefinitionErrorType.EXECUTE_DYNAMIC_PROPERTIES,
            (curComponentConnection, curActionContext) -> actionDefinitionService.executeDynamicProperties(
                componentName, componentVersion, actionName, propertyName, inputParameters, lookupDependsOnPaths,
                curComponentConnection, curActionContext));
    }

    @Override
    public List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, @NonNull List<String> lookupDependsOnPaths, String searchText,
        Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        ActionContext actionContext = contextFactory.createActionContext(
            componentName, componentVersion, actionName, null, null, null, componentConnection);

        return executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, actionContext,
            ActionDefinitionErrorType.EXECUTE_OPTIONS,
            (componentConnection1, actionContext1) -> actionDefinitionService.executeOptions(
                componentName, componentVersion, actionName, propertyName, inputParameters, lookupDependsOnPaths,
                searchText, componentConnection1, actionContext1));
    }

    @Override
    public Output executeOutput(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, @NonNull Map<String, Long> connectionIds) {

        ExecuteFunctionData executeFunctionData = getExecuteFunctionData(
            componentName, componentVersion, actionName, connectionIds);

        ActionContext actionContext = contextFactory.createActionContext(
            componentName, componentVersion, actionName, null, null, null, executeFunctionData.componentConnection());

        if (executeFunctionData.singleConnectionPerform()) {
            return executeSingleConnectionFunction(
                componentName, componentVersion, executeFunctionData.componentConnection(), actionContext,
                ActionDefinitionErrorType.EXECUTE_OUTPUT,
                (componentConnection1, actionContext1) -> actionDefinitionService.executeSingleConnectionOutput(
                    componentName, componentVersion, actionName, inputParameters, componentConnection1,
                    actionContext1));
        } else {
            return actionDefinitionService.executeMultipleConnectionsOutput(
                componentName, componentVersion, actionName, inputParameters,
                executeFunctionData.componentConnections(), actionContext);
        }
    }

    @Override
    public Object executePerformForPolyglot(
        String componentName, int componentVersion, String actionName, @NonNull Map<String, ?> inputParameters,
        ComponentConnection componentConnection, ActionContext actionContext) {

        return executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, actionContext,
            ActionDefinitionErrorType.EXECUTE_PERFORM,
            (componentConnection1, actionContext1) -> actionDefinitionService.executeSingleConnectionPerform(
                componentName, componentVersion, actionName, inputParameters, componentConnection1,
                actionContext1));
    }

    @Override
    public Object executePerform(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull AppType type,
        Long instanceId, Long instanceWorkflowId, Long jobId, @NonNull Map<String, ?> inputParameters,
        @NonNull Map<String, Long> connectionIds) {

        ExecuteFunctionData executeFunctionData = getExecuteFunctionData(
            componentName, componentVersion, actionName, connectionIds);

        ActionContext actionContext = contextFactory.createActionContext(
            componentName, componentVersion, actionName, type, instanceWorkflowId, jobId,
            executeFunctionData.componentConnection);

        if (executeFunctionData.singleConnectionPerform) {
            return executeSingleConnectionFunction(
                componentName, componentVersion, executeFunctionData.componentConnection, actionContext,
                ActionDefinitionErrorType.EXECUTE_PERFORM,
                (componentConnection1, actionContext1) -> actionDefinitionService.executeSingleConnectionPerform(
                    componentName, componentVersion, actionName, inputParameters, componentConnection1,
                    actionContext1));
        } else {
            return actionDefinitionService.executeMultipleConnectionsPerform(
                componentName, componentVersion, actionName, inputParameters, executeFunctionData.componentConnections,
                actionContext);
        }
    }

    @Override
    public String executeWorkflowNodeDescription(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters) {

        return actionDefinitionService.executeWorkflowNodeDescription(
            componentName, componentVersion, actionName, inputParameters,
            contextFactory.createActionContext(componentName, componentVersion, actionName, null, null, null, null));
    }

    private <V> V executeSingleConnectionFunction(
        String componentName, int componentVersion, ComponentConnection componentConnection,
        ActionContext actionContext, ErrorType errorType, BiFunction<ComponentConnection, ActionContext, V> function) {

        try {
            return function.apply(componentConnection, actionContext);
        } catch (Exception exception) {
            List<Object> refreshOn = connectionDefinitionService.getAuthorizationRefreshOn(
                componentName, componentConnection.version(), componentConnection.authorizationName());

            if (componentConnection.canCredentialsBeRefreshed() &&
                RefreshCredentialsUtils.matches(refreshOn, exception)) {

                componentConnection = getRefreshedCredentialsComponentConnection(componentConnection, actionContext);

                ActionContextImpl actionContextImpl = (ActionContextImpl) actionContext;

                actionContext = contextFactory.createActionContext(
                    componentName, componentVersion, actionContextImpl.getActionName(), actionContextImpl.getAppType(),
                    actionContextImpl.getInstanceWorkflowId(), actionContextImpl.getJobId(), componentConnection);

                return function.apply(componentConnection, actionContext);
            }

            if (exception instanceof ProviderException) {
                throw new ComponentExecutionException(exception, errorType);
            }

            throw exception;
        }
    }

    private ExecuteFunctionData getExecuteFunctionData(
        String componentName, int componentVersion, String actionName, Map<String, Long> connectionIds) {

        Map<String, ComponentConnection> componentConnections = getComponentConnections(connectionIds);

        Set<Map.Entry<String, ComponentConnection>> entries = componentConnections.entrySet();

        boolean singleConnectionPerform = actionDefinitionService.isSingleConnectionPerform(
            componentName, componentVersion, actionName);

        ComponentConnection componentConnection = singleConnectionPerform && !entries.isEmpty()
            ? CollectionUtils.getFirstMap(entries, Map.Entry::getValue) : null;

        return new ExecuteFunctionData(componentConnections, componentConnection, singleConnectionPerform);
    }

    private ComponentConnection getComponentConnection(Long connectionId) {
        ComponentConnection componentConnection = null;

        if (connectionId != null) {
            Connection connection = connectionService.getConnection(connectionId);

            componentConnection = new ComponentConnection(
                connection.getComponentName(), connection.getConnectionVersion(), connectionId,
                connection.getParameters(), connection.getAuthorizationName());
        }

        return componentConnection;
    }

    private ComponentConnection getComponentConnection(Map.Entry<String, Long> entry) {
        Connection connection = connectionService.getConnection(entry.getValue());

        return new ComponentConnection(
            connection.getComponentName(), connection.getConnectionVersion(), entry.getValue(),
            connection.getParameters(), connection.getAuthorizationName());
    }

    private Map<String, ComponentConnection> getComponentConnections(Map<String, Long> connectionIds) {
        return connectionIds.entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, this::getComponentConnection));
    }

    private ComponentConnection getRefreshedCredentialsComponentConnection(
        ComponentConnection componentConnection, ActionContext actionContext) {

        Connection connection;
        Map<String, ?> parameters;

        try {
            if (componentConnection.isAuthorizationOauth2AuthorizationCode()) {
                RefreshTokenResponse refreshTokenResponse =
                    connectionDefinitionService.executeRefresh(
                        componentConnection.componentName(), componentConnection.version(),
                        componentConnection.authorizationName(), componentConnection.getParameters(), actionContext);

                parameters = new HashMap<>() {
                    {
                        put(ACCESS_TOKEN, refreshTokenResponse.accessToken());

                        if (refreshTokenResponse.refreshToken() != null) {
                            put(REFRESH_TOKEN, refreshTokenResponse.refreshToken());
                        }

                        if (refreshTokenResponse.expiresIn() != null) {
                            put(EXPIRES_IN, refreshTokenResponse.expiresIn());
                        }
                    }
                };
            } else {
                parameters = connectionDefinitionService.executeAcquire(
                    componentConnection.componentName(), componentConnection.version(),
                    componentConnection.authorizationName(), componentConnection.getParameters(), actionContext);
            }

            ReentrantLock reentrantLock = REENTRANT_LOCK_CACHE.get(
                componentConnection.connectionId(), (key) -> new ReentrantLock(true));

            reentrantLock.lock();

            try {
                connection = connectionService.updateConnectionParameters(
                    componentConnection.connectionId(), parameters);
            } finally {
                reentrantLock.unlock();
            }
        } catch (Exception e) {
            connectionService.updateConnectionCredentialStatus(
                componentConnection.connectionId(), CredentialStatus.INVALID);

            throw e;
        }

        return new ComponentConnection(
            componentConnection.componentName(), connection.getConnectionVersion(), componentConnection.connectionId(),
            connection.getParameters(), connection.getAuthorizationName());
    }

    private record ExecuteFunctionData(
        Map<String, ComponentConnection> componentConnections, ComponentConnection componentConnection,
        boolean singleConnectionPerform) {
    }
}

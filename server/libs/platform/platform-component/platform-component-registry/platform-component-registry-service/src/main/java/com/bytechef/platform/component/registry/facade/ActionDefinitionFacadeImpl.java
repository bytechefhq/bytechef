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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.platform.component.registry.definition.factory.ContextFactory;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.component.registry.domain.Option;
import com.bytechef.platform.component.registry.domain.Output;
import com.bytechef.platform.component.registry.domain.Property;
import com.bytechef.platform.component.registry.service.ActionDefinitionService;
import com.bytechef.platform.component.registry.service.ConnectionDefinitionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.AppType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
@Service("actionDefinitionFacade")
public class ActionDefinitionFacadeImpl implements ActionDefinitionFacade {

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

        return actionDefinitionService.executeDynamicProperties(
            componentName, componentVersion, actionName, propertyName, inputParameters, lookupDependsOnPaths,
            componentConnection,
            contextFactory.createActionContext(
                componentName, componentVersion, actionName, null, null, null, componentConnection));
    }

    @Override
    public List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, @NonNull List<String> lookupDependsOnPaths, String searchText,
        Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        ActionContext actionContext = contextFactory.createActionContext(
            componentName, componentVersion, actionName, null, null, null, componentConnection);

        Exception executionException;

        try {
            return actionDefinitionService.executeOptions(
                componentName, componentVersion, actionName, propertyName, inputParameters, lookupDependsOnPaths,
                searchText,
                componentConnection, actionContext);
        } catch (Exception exception) {
            executionException = exception;

            Map<String, ComponentConnection> tokenRefreshedComponentConnections = getTokenRefreshedComponentConnection(
                componentName, Map.of("tmpName", connectionId), exception, Map.of("tmpName", componentConnection),
                actionContext);

            if (!tokenRefreshedComponentConnections.isEmpty()) {
                return actionDefinitionService.executeOptions(
                    componentName, componentVersion, actionName, propertyName, inputParameters, lookupDependsOnPaths,
                    searchText, componentConnection, actionContext);
            }
        }

        throw new UnsupportedOperationException("Unable to recover from execution error", executionException);
    }

    @Override
    public Output executeOutput(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, @NonNull Map<String, Long> connectionIds) {

        Map<String, ComponentConnection> componentConnections = getComponentConnections(connectionIds);

        Set<Map.Entry<String, ComponentConnection>> entries = componentConnections.entrySet();

        return actionDefinitionService.executeOutput(
            componentName, componentVersion, actionName, inputParameters, componentConnections,
            contextFactory.createActionContext(
                componentName, componentVersion, actionName, null, null, null,
                entries.size() == 1 ? CollectionUtils.getFirstMap(entries, Map.Entry::getValue) : null));
    }

    @Override
    public Object executePerformForPolyglot(
        String componentName, int componentVersion, String actionName, @NonNull Map<String, ?> inputParameters,
        Map<String, ComponentConnection> componentConnections,
        ActionContext actionContext) {

        Exception executionException;

        try {
            return actionDefinitionService.executePerform(
                componentName, componentVersion, actionName, inputParameters, componentConnections, actionContext);
        } catch (Exception exception) {
            executionException = exception;

            ComponentConnection componentConnection = componentConnections.values()
                .stream()
                .findFirst()
                .get();

            componentConnections = getTokenRefreshedComponentConnection(
                componentName, Map.of(componentConnection.componentName(), componentConnection.getConnectionId()),
                exception, Map.of(componentConnection.componentName(), componentConnection),
                actionContext);

            if (!componentConnections.isEmpty()) {
                return actionDefinitionService.executePerform(
                    componentName, componentVersion, actionName, inputParameters, componentConnections, actionContext);
            }
        }

        throw new UnsupportedOperationException("Unable to recover from execution error", executionException);
    }

    @Override
    public Object executePerform(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull AppType type,
        Long instanceId, Long instanceWorkflowId, Long jobId, @NonNull Map<String, ?> inputParameters,
        @NonNull Map<String, Long> connectionIds) {

        Map<String, ComponentConnection> componentConnections = getComponentConnections(connectionIds);

        Set<Map.Entry<String, ComponentConnection>> entries = componentConnections.entrySet();

        ActionContext actionContext = contextFactory.createActionContext(
            componentName, componentVersion, actionName, type, instanceWorkflowId, jobId,
            entries.size() == 1
                ? CollectionUtils.getFirstMap(entries, Map.Entry::getValue)
                : null);

        Exception executionException;

        try {
            return actionDefinitionService.executePerform(
                componentName, componentVersion, actionName, inputParameters, componentConnections, actionContext);
        } catch (Exception exception) {
            executionException = exception;

            componentConnections = getTokenRefreshedComponentConnection(
                componentName, connectionIds, exception, componentConnections,
                actionContext);

            if (!componentConnections.isEmpty()) {
                return actionDefinitionService.executePerform(
                    componentName, componentVersion, actionName, inputParameters, componentConnections, actionContext);
            }
        }

        throw new UnsupportedOperationException("Unable to recover from execution error", executionException);

    }

    private Map<String, ComponentConnection> getTokenRefreshedComponentConnection(
        String componentName, Map<String, Long> connectionIds, Exception exception,
        Map<String, ComponentConnection> componentConnections, ActionContext actionContext) {

        if (!ProviderException.hasAuthorizationFailedExceptionContent(exception)) {
            throw new UnsupportedOperationException(
                "Unable to recover failed request with token refresh procedure", exception);
        }

        HashMap<String, ComponentConnection> refreshedConnections = new HashMap<>();

        componentConnections.forEach((connectionName, componentConnection) -> {

            if (!componentConnection.isAuthorizationNameOauth2AuthorizationCode()) {
                return;
            }

            String realComponentName = componentName;

            if (!Objects.equals(ProviderException.getComponentName(exception), componentName)) {
                realComponentName = ProviderException.getComponentName(exception);
            }

            Authorization.RefreshTokenResponse refreshTokenResponse =
                connectionDefinitionService.executeRefresh(realComponentName, componentConnection.authorizationName(),
                    componentConnection.getParameters(), actionContext);

            Long connectionId = connectionIds.get(realComponentName);
            Connection connection = connectionService.updateConnectionParameter(
                connectionId, "access_token",
                Objects.requireNonNull(refreshTokenResponse.accessToken()));

            refreshedConnections.put(connectionName, new ComponentConnection(
                realComponentName, connection.getVersion(), connectionId, connection.getParameters(),
                connection.getAuthorizationName()));

        });

        return refreshedConnections;
    }

    @Override
    public String executeWorkflowNodeDescription(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters) {

        return actionDefinitionService.executeWorkflowNodeDescription(
            componentName, componentVersion, actionName, inputParameters,
            contextFactory.createActionContext(componentName, componentVersion, actionName, null, null, null, null));
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

    private Map<String, ComponentConnection> getComponentConnections(Map<String, Long> connectionIds) {
        return connectionIds
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> {
                        Connection connection = connectionService.getConnection(entry.getValue());

                        return new ComponentConnection(
                            connection.getComponentName(), connection.getConnectionVersion(), entry.getValue(),
                            connection.getParameters(), connection.getAuthorizationName());
                    }));
    }
}

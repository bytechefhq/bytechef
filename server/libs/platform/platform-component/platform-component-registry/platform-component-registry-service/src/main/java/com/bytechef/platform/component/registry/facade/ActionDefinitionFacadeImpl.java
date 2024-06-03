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
import com.bytechef.platform.component.registry.definition.factory.ContextFactory;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.component.registry.domain.Option;
import com.bytechef.platform.component.registry.domain.Output;
import com.bytechef.platform.component.registry.domain.Property;
import com.bytechef.platform.component.registry.service.ActionDefinitionService;
import com.bytechef.platform.component.registry.service.ConnectionDefinitionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.Type;
import com.bytechef.component.exception.ProviderException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
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

        try {
            return actionDefinitionService.executeOptions(
                componentName, componentVersion, actionName, propertyName, inputParameters, lookupDependsOnPaths,
                searchText,
                componentConnection, actionContext);
        } catch (Exception exception) {
            componentConnection = getTokenRefreshedComponentConnection(
                componentName, connectionId, exception, componentConnection, actionContext);
        }

        return actionDefinitionService.executeOptions(
            componentName, componentVersion, actionName, propertyName, inputParameters, lookupDependsOnPaths,
            searchText, componentConnection, actionContext);
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
    public Object executePerform(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull Type type,
        Long instanceId, Long instanceWorkflowId, Long jobId, @NonNull Map<String, ?> inputParameters,
        @NonNull Map<String, Long> connectionIds) {

        Map<String, ComponentConnection> componentConnections = getComponentConnections(connectionIds);

        Set<Map.Entry<String, ComponentConnection>> entries = componentConnections.entrySet();

        ActionContext actionContext = contextFactory.createActionContext(
            componentName, componentVersion, actionName, type, instanceWorkflowId, jobId,
            entries.size() == 1
                ? CollectionUtils.getFirstMap(entries, Map.Entry::getValue)
                : null);

        try {
            return actionDefinitionService.executePerform(
                componentName, componentVersion, actionName, inputParameters, componentConnections, actionContext);
        } catch (Exception exception) {
            ComponentConnection refreshedComponentConnection = getTokenRefreshedComponentConnection(
                componentName, connectionIds.get(componentName), exception, componentConnections.get(componentName),
                actionContext);

            componentConnections.replace(componentName, refreshedComponentConnection);
        }

        return actionDefinitionService.executePerform(
            componentName, componentVersion, actionName, inputParameters, componentConnections, actionContext);
    }

    private ComponentConnection getTokenRefreshedComponentConnection(
        String componentName, Long connectionId, Exception exception,
        ComponentConnection componentConnection, ActionContext actionContext) {

        if (!Objects.equals(ProviderException.AuthorizationFailedException.class, exception.getClass()) &&
            !StringUtils.contains(exception.getMessage(), "401")) {

            throw new UnsupportedOperationException(
                "Unable to recover failed request with token refresh procedure", exception);
        }

        Authorization.RefreshTokenResponse refreshTokenResponse =
            connectionDefinitionService.executeRefresh(componentName, componentConnection, actionContext);

        Connection connection = connectionService.updateConnectionParameter(
            connectionId, "access_token", Objects.requireNonNull(refreshTokenResponse.accessToken()));

        return new ComponentConnection(
            componentName, connection.getVersion(), connection.getParameters(), connection.getAuthorizationName());
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
                connection.getComponentName(), connection.getConnectionVersion(), connection.getParameters(),
                connection.getAuthorizationName());
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
                            connection.getComponentName(), connection.getConnectionVersion(),
                            connection.getParameters(), connection.getAuthorizationName());
                    }));
    }
}

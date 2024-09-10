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
import com.bytechef.component.exception.ProviderException;
import com.bytechef.platform.component.registry.definition.ActionContextImpl;
import com.bytechef.platform.component.registry.definition.ContextFactory;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.component.registry.domain.Option;
import com.bytechef.platform.component.registry.domain.Property;
import com.bytechef.platform.component.registry.exception.ActionDefinitionErrorType;
import com.bytechef.platform.component.registry.helper.TokenRefreshHelper;
import com.bytechef.platform.component.registry.service.ActionDefinitionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.registry.domain.OutputResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
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
    private final ContextFactory contextFactory;
    private final ActionDefinitionService actionDefinitionService;
    private final TokenRefreshHelper tokenRefreshHelper;

    @SuppressFBWarnings("EI")
    public ActionDefinitionFacadeImpl(
        ConnectionService connectionService,
        ContextFactory contextFactory,
        ActionDefinitionService actionDefinitionService, TokenRefreshHelper tokenRefreshHelper) {

        this.contextFactory = contextFactory;
        this.actionDefinitionService = actionDefinitionService;
        this.connectionService = connectionService;
        this.tokenRefreshHelper = tokenRefreshHelper;
    }

    @Override
    public List<Property> executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        Map<String, ?> inputParameters, @NonNull List<String> lookupDependsOnPaths, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        ActionContext actionContext = contextFactory.createActionContext(
            componentName, componentVersion, actionName, null, null, null, null, componentConnection);

        return tokenRefreshHelper.executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, actionContext,
            ActionDefinitionErrorType.EXECUTE_DYNAMIC_PROPERTIES,
            (curComponentConnection, curActionContext) -> actionDefinitionService.executeDynamicProperties(
                componentName, componentVersion, actionName, propertyName, inputParameters, lookupDependsOnPaths,
                curComponentConnection, curActionContext),
            componentConnection1 -> contextFactory.createActionContext(
                componentName, componentVersion, actionName, null, null, null, null, componentConnection));
    }

    @Override
    public List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, @NonNull List<String> lookupDependsOnPaths, String searchText,
        Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        ActionContext actionContext = contextFactory.createActionContext(
            componentName, componentVersion, actionName, null, null, null, null, componentConnection);

        return tokenRefreshHelper.executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, actionContext,
            ActionDefinitionErrorType.EXECUTE_OPTIONS,
            (componentConnection1, actionContext1) -> actionDefinitionService.executeOptions(
                componentName, componentVersion, actionName, propertyName, inputParameters, lookupDependsOnPaths,
                searchText, componentConnection1, actionContext1),
            componentConnection1 -> contextFactory.createActionContext(
                componentName, componentVersion, actionName, null, null, null, null, componentConnection));
    }

    @Override
    public OutputResponse executeOutput(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, @NonNull Map<String, Long> connectionIds) {

        ExecuteFunctionData executeFunctionData = getExecuteFunctionData(
            componentName, componentVersion, actionName, connectionIds);

        ActionContext actionContext = contextFactory.createActionContext(
            componentName, componentVersion, actionName, null, null, null, null,
            executeFunctionData.componentConnection());

        if (executeFunctionData.singleConnectionPerform()) {
            return tokenRefreshHelper.executeSingleConnectionFunction(
                componentName, componentVersion, executeFunctionData.componentConnection(), actionContext,
                ActionDefinitionErrorType.EXECUTE_OUTPUT,
                (componentConnection1, actionContext1) -> actionDefinitionService.executeSingleConnectionOutput(
                    componentName, componentVersion, actionName, inputParameters, componentConnection1,
                    actionContext1),
                componentConnection1 -> contextFactory.createActionContext(
                    componentName, componentVersion, actionName, null, null, null, null, componentConnection1));
        } else {
            return actionDefinitionService.executeMultipleConnectionsOutput(
                componentName, componentVersion, actionName, inputParameters,
                executeFunctionData.componentConnections(), Map.of(), actionContext);
        }
    }

    @Override
    public Object executePerform(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull AppType type,
        Long instanceId, Long instanceWorkflowId, Long jobId, @NonNull Map<String, ?> inputParameters,
        @NonNull Map<String, Long> connectionIds, Map<String, ?> extensions) {

        ExecuteFunctionData executeFunctionData = getExecuteFunctionData(
            componentName, componentVersion, actionName, connectionIds);

        ActionContext actionContext = contextFactory.createActionContext(
            componentName, componentVersion, actionName, type, instanceId, instanceWorkflowId, jobId,
            executeFunctionData.componentConnection);

        if (executeFunctionData.singleConnectionPerform) {
            return tokenRefreshHelper.executeSingleConnectionFunction(
                componentName, componentVersion, executeFunctionData.componentConnection, actionContext,
                ActionDefinitionErrorType.EXECUTE_PERFORM,
                (componentConnection1, actionContext1) -> actionDefinitionService.executeSingleConnectionPerform(
                    componentName, componentVersion, actionName, inputParameters, componentConnection1,
                    actionContext1),
                componentConnection1 -> contextFactory.createActionContext(
                    componentName, componentVersion, actionName, type, instanceId, instanceWorkflowId, jobId,
                    componentConnection1));
        } else {
            return actionDefinitionService.executeMultipleConnectionsPerform(
                componentName, componentVersion, actionName, inputParameters, executeFunctionData.componentConnections,
                extensions, actionContext);
        }
    }

    @Override
    public Object executePerformForPolyglot(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, ComponentConnection componentConnection,
        @NonNull ActionContext actionContext) {

        ActionContextImpl actionContextImpl = (ActionContextImpl) actionContext;

        return tokenRefreshHelper.executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, actionContext,
            ActionDefinitionErrorType.EXECUTE_PERFORM,
            (componentConnection1, actionContext1) -> actionDefinitionService.executeSingleConnectionPerform(
                componentName, componentVersion, actionName, inputParameters, componentConnection1,
                actionContext1),
            componentConnection1 -> contextFactory.createActionContext(
                componentName, componentVersion, actionName, actionContextImpl.getType(),
                actionContextImpl.getInstanceId(), actionContextImpl.getInstanceWorkflowId(),
                actionContextImpl.getJobId(), componentConnection));
    }

    @Override
    public ProviderException executeProcessErrorResponse(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, int statusCode,
        Object body) {

        ActionContext actionContext = contextFactory.createActionContext(
            componentName, componentVersion, actionName, null, null, null, null, null);

        return actionDefinitionService.executeProcessErrorResponse(
            componentName, componentVersion, actionName, statusCode, body, actionContext);
    }

    @Override
    public String executeWorkflowNodeDescription(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters) {

        return actionDefinitionService.executeWorkflowNodeDescription(
            componentName, componentVersion, actionName, inputParameters,
            contextFactory.createActionContext(
                componentName, componentVersion, actionName, null, null, null, null, null));
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

    private record ExecuteFunctionData(
        Map<String, ComponentConnection> componentConnections, ComponentConnection componentConnection,
        boolean singleConnectionPerform) {
    }
}

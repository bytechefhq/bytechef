/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.component.facade;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.exception.ActionDefinitionErrorType;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.util.TokenRefreshHelper;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.domain.OutputResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String workflowId, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        ActionContext actionContext = contextFactory.createActionContext(
            componentName, componentVersion, actionName, null, null, null, workflowId, componentConnection, null, true);

        return tokenRefreshHelper.executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, actionContext,
            ActionDefinitionErrorType.EXECUTE_DYNAMIC_PROPERTIES,
            (componentConnection1, actionContext1) -> actionDefinitionService.executeDynamicProperties(
                componentName, componentVersion, actionName, propertyName, inputParameters, lookupDependsOnPaths,
                componentConnection1, actionContext1),
            componentConnection1 -> contextFactory.createActionContext(
                componentName, componentVersion, actionName, null, null, null, workflowId, componentConnection1, null,
                true));
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        ActionContext actionContext = contextFactory.createActionContext(
            componentName, componentVersion, actionName, null, null, null, null, componentConnection, null, true);

        return tokenRefreshHelper.executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, actionContext,
            ActionDefinitionErrorType.EXECUTE_OPTIONS,
            (componentConnection1, actionContext1) -> actionDefinitionService.executeOptions(
                componentName, componentVersion, actionName, propertyName, inputParameters, lookupDependsOnPaths,
                searchText, componentConnection1, actionContext1),
            componentConnection1 -> contextFactory.createActionContext(
                componentName, componentVersion, actionName, null, null, null, null, componentConnection1, null, true));
    }

    @Override
    public OutputResponse executeOutput(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        Map<String, Long> connectionIds) {

        ExecuteFunctionData executeFunctionData = getExecuteFunctionData(
            componentName, componentVersion, actionName, connectionIds);

        if (executeFunctionData.singleConnectionPerform()) {
            ComponentConnection singleComponentConnection = executeFunctionData.getSingleComponentConnection();

            ActionContext actionContext = contextFactory.createActionContext(
                componentName, componentVersion, actionName, null, null, null, null, singleComponentConnection, null,
                true);

            return tokenRefreshHelper.executeSingleConnectionFunction(
                componentName, componentVersion, singleComponentConnection, actionContext,
                ActionDefinitionErrorType.EXECUTE_OUTPUT,
                (componentConnection1, actionContext1) -> actionDefinitionService.executeSingleConnectionOutput(
                    componentName, componentVersion, actionName, inputParameters, componentConnection1,
                    actionContext1),
                componentConnection1 -> contextFactory.createActionContext(
                    componentName, componentVersion, actionName, null, null, null, null, componentConnection1, null,
                    true));
        } else {
            ActionContext actionContext = contextFactory.createActionContext(
                componentName, componentVersion, actionName, null, null, null, null, null, null, true);

            return actionDefinitionService.executeMultipleConnectionsOutput(
                componentName, componentVersion, actionName, inputParameters,
                executeFunctionData.componentConnections(), Map.of(), actionContext);
        }
    }

    @Override
    public Object executePerform(
        String componentName, int componentVersion, String actionName, ModeType type, Long jobPrincipalId,
        Long jobPrincipalWorkflowId, Long jobId, String workflowId, Map<String, ?> inputParameters,
        Map<String, Long> connectionIds, Map<String, ?> extensions, boolean editorEnvironment) {

        ExecuteFunctionData executeFunctionData = getExecuteFunctionData(
            componentName, componentVersion, actionName, connectionIds);

        if (executeFunctionData.singleConnectionPerform) {
            ComponentConnection singleComponentConnection = executeFunctionData.getSingleComponentConnection();

            ActionContext actionContext = contextFactory.createActionContext(
                componentName, componentVersion, actionName, jobPrincipalId, jobPrincipalWorkflowId, jobId, workflowId,
                singleComponentConnection, type, editorEnvironment);

            return tokenRefreshHelper.executeSingleConnectionFunction(
                componentName, componentVersion, singleComponentConnection, actionContext,
                ActionDefinitionErrorType.EXECUTE_PERFORM,
                (componentConnection1, actionContext1) -> actionDefinitionService.executeSingleConnectionPerform(
                    componentName, componentVersion, actionName, inputParameters, componentConnection1,
                    actionContext1),
                componentConnection1 -> contextFactory.createActionContext(
                    componentName, componentVersion, actionName, jobPrincipalId, jobPrincipalWorkflowId, jobId,
                    workflowId, componentConnection1, type, editorEnvironment));
        } else {
            ActionContext actionContext = contextFactory.createActionContext(
                componentName, componentVersion, actionName, jobPrincipalId, jobPrincipalWorkflowId, jobId, workflowId,
                null, type, editorEnvironment);

            return actionDefinitionService.executeMultipleConnectionsPerform(
                componentName, componentVersion, actionName, inputParameters, executeFunctionData.componentConnections,
                extensions, actionContext);
        }
    }

    @Override
    public Object executePerformForPolyglot(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        ComponentConnection componentConnection, ActionContext actionContext) {

        ActionContextAware actionContextAware = (ActionContextAware) actionContext;

        return tokenRefreshHelper.executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, actionContext,
            ActionDefinitionErrorType.EXECUTE_PERFORM,
            (componentConnection1, actionContext1) -> actionDefinitionService.executeSingleConnectionPerform(
                componentName, componentVersion, actionName, inputParameters, componentConnection1, actionContext1),
            componentConnection1 -> contextFactory.createActionContext(
                componentName, componentVersion, actionName, actionContextAware.getJobPrincipalId(),
                actionContextAware.getJobPrincipalWorkflowId(), actionContextAware.getJobId(),
                actionContextAware.getWorkflowId(), componentConnection1, actionContextAware.getModeType(),
                actionContextAware.isEditorEnvironment()));
    }

    @Override
    public ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, String actionName, int statusCode, Object body) {

        return actionDefinitionService.executeProcessErrorResponse(
            componentName, componentVersion, actionName, statusCode, body,
            contextFactory.createActionContext(
                componentName, componentVersion, actionName, null, null, null, null, null, null, false));
    }

    @Override
    public String executeWorkflowNodeDescription(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters) {

        return actionDefinitionService.executeWorkflowNodeDescription(
            componentName, componentVersion, actionName, inputParameters,
            contextFactory.createActionContext(
                componentName, componentVersion, actionName, null, null, null, null, null, null, true));
    }

    private ExecuteFunctionData getExecuteFunctionData(
        String componentName, int componentVersion, String actionName, Map<String, Long> connectionIds) {

        Map<String, ComponentConnection> componentConnections = getComponentConnections(connectionIds);

        boolean singleConnectionPerform = actionDefinitionService.isSingleConnectionPerform(
            componentName, componentVersion, actionName);

        return new ExecuteFunctionData(componentConnections, singleConnectionPerform);
    }

    private ComponentConnection getComponentConnection(Long connectionId) {
        ComponentConnection componentConnection = null;

        if (connectionId != null) {
            Connection connection = connectionService.getConnection(connectionId);

            componentConnection = new ComponentConnection(
                connection.getComponentName(), connection.getConnectionVersion(), connectionId,
                connection.getParameters(), connection.getAuthorizationType());
        }

        return componentConnection;
    }

    private Map<String, ComponentConnection> getComponentConnections(Map<String, Long> connectionIds) {
        return connectionIds.entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> getComponentConnection(entry.getValue())));
    }

    private record ExecuteFunctionData(
        Map<String, ComponentConnection> componentConnections, boolean singleConnectionPerform) {

        ComponentConnection getSingleComponentConnection() {
            Set<Map.Entry<String, ComponentConnection>> entries = componentConnections.entrySet();

            return singleConnectionPerform && !entries.isEmpty()
                ? CollectionUtils.getFirstMap(entries, Map.Entry::getValue) : null;
        }
    }
}

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

import com.bytechef.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.platform.component.registry.definition.factory.ContextFactory;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.component.registry.domain.Option;
import com.bytechef.platform.component.registry.domain.Output;
import com.bytechef.platform.component.registry.domain.Property;
import com.bytechef.platform.component.registry.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.Type;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service("triggerDefinitionFacade")
public class TriggerDefinitionFacadeImpl implements TriggerDefinitionFacade {

    private final ConnectionService connectionService;
    private final ContextFactory contextFactory;
    private final TriggerDefinitionService triggerDefinitionService;

    @SuppressFBWarnings("EI")
    public TriggerDefinitionFacadeImpl(
        ConnectionService connectionService, ContextFactory contextFactory,
        TriggerDefinitionService triggerDefinitionService) {

        this.connectionService = connectionService;
        this.contextFactory = contextFactory;
        this.triggerDefinitionService = triggerDefinitionService;
    }

    @Override
    public List<Property> executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, @NonNull List<String> lookupDependsOnPaths, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeDynamicProperties(
            componentName, componentVersion, triggerName, inputParameters, propertyName, lookupDependsOnPaths,
            componentConnection, contextFactory.createTriggerContext(componentName, componentVersion, triggerName, null,
                null, null, componentConnection));
    }

    @Override
    public void executeDynamicWebhookDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId,
        @NonNull Map<String, ?> outputParameters, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        triggerDefinitionService.executeDynamicWebhookDisable(
            componentName, componentVersion, triggerName, inputParameters, workflowExecutionId, outputParameters,
            componentConnection, contextFactory.createTriggerContext(componentName, componentVersion, triggerName, null,
                null, null, componentConnection));
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId, Long connectionId,
        @NonNull String webhookUrl) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeDynamicWebhookEnable(
            componentName, componentVersion, triggerName, inputParameters,
            webhookUrl, workflowExecutionId, componentConnection,
            contextFactory.createTriggerContext(componentName, componentVersion, triggerName, null, null, null,
                componentConnection));
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookRefresh(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> outputParameters) {

        return triggerDefinitionService.executeDynamicWebhookRefresh(
            componentName, componentVersion, triggerName, outputParameters,
            contextFactory.createTriggerContext(componentName, componentVersion, triggerName, null, null, null,
                null));
    }

    @Override
    public void executeListenerDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        triggerDefinitionService.executeListenerDisable(
            componentName, componentVersion, triggerName, inputParameters, workflowExecutionId,
            componentConnection, contextFactory.createTriggerContext(componentName, componentVersion, triggerName, null,
                null, null, componentConnection));
    }

    @Override
    public void executeListenerEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        triggerDefinitionService.executeListenerEnable(
            componentName, componentVersion, triggerName, inputParameters, workflowExecutionId, componentConnection,
            contextFactory.createTriggerContext(componentName, componentVersion, triggerName, null, null, null,
                componentConnection));
    }

    @Override
    public List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, @NonNull List<String> lookupDependsOnPaths, String searchText,
        Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeOptions(
            componentName, componentVersion, triggerName, inputParameters, propertyName, lookupDependsOnPaths,
            searchText,
            componentConnection, contextFactory.createTriggerContext(componentName, componentVersion, triggerName, null,
                null, null, componentConnection));
    }

    @Override
    public Output executeOutput(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeOutput(
            componentName, componentVersion, triggerName, inputParameters, componentConnection,
            contextFactory.createTriggerContext(componentName, componentVersion, triggerName, null, null, null,
                componentConnection));
    }

    @Override
    public TriggerOutput executeTrigger(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Type type, Long instanceId, String workflowId, Long jobId,
        @NonNull Map<String, ?> inputParameters, Object triggerState, WebhookRequest webhookRequest,
        Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeTrigger(
            componentName, componentVersion, triggerName, inputParameters, triggerState, webhookRequest,
            componentConnection, contextFactory.createTriggerContext(componentName, componentVersion, triggerName, type,
                workflowId, jobId, componentConnection));
    }

    @Override
    public boolean executeWebhookValidate(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters,
        @NonNull WebhookRequest webhookRequest, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeWebhookValidate(
            componentName, componentVersion, triggerName, inputParameters, webhookRequest,
            componentConnection, contextFactory.createTriggerContext(componentName, componentVersion, triggerName, null,
                null, null, componentConnection));
    }

    @Override
    public String executeWorkflowNodeDescription(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters) {

        return triggerDefinitionService.executeWorkflowNodeDescription(
            componentName, componentVersion, triggerName, inputParameters,
            contextFactory.createTriggerContext(componentName, componentVersion, triggerName, null, null, null,
                null));
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
}

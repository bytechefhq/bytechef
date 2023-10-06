
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.component.registry.facade;

import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.component.definition.factory.ContextFactory;
import com.bytechef.hermes.component.registry.dto.ComponentConnection;
import com.bytechef.hermes.component.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.component.registry.trigger.TriggerOutput;
import com.bytechef.hermes.component.registry.trigger.WebhookRequest;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.registry.domain.Option;
import com.bytechef.hermes.registry.domain.ValueProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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
    public List<? extends ValueProperty<?>> executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName, @NonNull String propertyName,
        @NonNull Map<String, Object> triggerParameters, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeDynamicProperties(
            componentName, componentVersion, triggerName, triggerParameters, propertyName,
            componentConnection, contextFactory.createTriggerContext(componentName, componentConnection));
    }

    @Override
    public void executeDynamicWebhookDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters, @NonNull String workflowExecutionId,
        @NonNull Map<String, ?> outputParameters, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        triggerDefinitionService.executeDynamicWebhookDisable(
            componentName, componentVersion, triggerName, triggerParameters, workflowExecutionId, outputParameters,
            componentConnection, contextFactory.createTriggerContext(componentName, componentConnection));
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters, @NonNull String workflowExecutionId, Long connectionId,
        @NonNull String webhookUrl) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeDynamicWebhookEnable(
            componentName, componentVersion, triggerName, triggerParameters,
            createWebhookUrl(workflowExecutionId, webhookUrl), workflowExecutionId,
            componentConnection, contextFactory.createTriggerContext(componentName, componentConnection));
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookRefresh(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> outputParameters) {

        return triggerDefinitionService.executeDynamicWebhookRefresh(
            componentName, componentVersion, triggerName, outputParameters,
            contextFactory.createTriggerContext(componentName, null));
    }

    @Override
    public String executeEditorDescription(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeEditorDescription(
            componentName, componentVersion, triggerName, triggerParameters, componentConnection,
            contextFactory.createTriggerContext(componentName, componentConnection));
    }

    @Override
    public void executeListenerDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters, @NonNull String workflowExecutionId, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        triggerDefinitionService.executeListenerDisable(
            componentName, componentVersion, triggerName, triggerParameters, workflowExecutionId,
            componentConnection, contextFactory.createTriggerContext(componentName, componentConnection));
    }

    @Override
    public void executeListenerEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters, @NonNull String workflowExecutionId, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        triggerDefinitionService.executeOnEnableListener(
            componentName, componentVersion, triggerName, triggerParameters, workflowExecutionId, componentConnection,
            contextFactory.createTriggerContext(componentName, componentConnection));
    }

    @Override
    public List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName, @NonNull String propertyName,
        @NonNull Map<String, ?> triggerParameters, Long connectionId, String searchText) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeOptions(
            componentName, componentVersion, triggerName, triggerParameters, propertyName, searchText,
            componentConnection, contextFactory.createTriggerContext(componentName, componentConnection));
    }

    @Override
    public List<? extends ValueProperty<?>> executeOutputSchema(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeOutputSchema(
            componentName, componentVersion, triggerName, triggerParameters, componentConnection,
            contextFactory.createTriggerContext(componentName, componentConnection));

    }

    @Override
    public Object executeSampleOutput(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeSampleOutput(
            componentName, componentVersion, triggerName, triggerParameters, componentConnection,
            contextFactory.createTriggerContext(componentName, componentConnection));

    }

    @Override
    public TriggerOutput executeTrigger(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters,
        Object triggerState, @NonNull WebhookRequest webhookRequest, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeTrigger(
            componentName, componentVersion, triggerName, inputParameters, triggerState, webhookRequest,
            componentConnection, contextFactory.createTriggerContext(componentName, componentConnection));
    }

    @Override
    public boolean executeWebhookValidate(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters,
        @NonNull WebhookRequest webhookRequest, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeWebhookValidate(
            componentName, componentVersion, triggerName, inputParameters, webhookRequest,
            componentConnection, contextFactory.createTriggerContext(componentName, componentConnection));
    }

    private String createWebhookUrl(String workflowExecutionId, String webhookUrl) {
        return webhookUrl + "/api/webhooks/" + workflowExecutionId;
    }

    private ComponentConnection getComponentConnection(Long connectionId) {
        ComponentConnection componentConnection = null;

        if (connectionId != null) {
            Connection connection = connectionService.getConnection(connectionId);

            componentConnection = new ComponentConnection(
                connection.getConnectionVersion(), connection.getParameters(), connection.getAuthorizationName());
        }

        return componentConnection;
    }
}


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
import com.bytechef.hermes.component.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.component.registry.trigger.TriggerOutput;
import com.bytechef.hermes.component.registry.trigger.WebhookRequest;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.RemoteConnectionService;
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
public class TriggerDefinitionFacadeImpl implements TriggerDefinitionFacade, RemoteTriggerDefinitionFacade {

    private final RemoteConnectionService connectionService;
    private final TriggerDefinitionService triggerDefinitionService;

    @SuppressFBWarnings("EI")
    public TriggerDefinitionFacadeImpl(
        RemoteConnectionService connectionService, TriggerDefinitionService triggerDefinitionService) {

        this.connectionService = connectionService;
        this.triggerDefinitionService = triggerDefinitionService;
    }

    @Override
    public List<? extends ValueProperty<?>> executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName, @NonNull String propertyName,
        @NonNull Map<String, Object> triggerParameters, Long connectionId) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        return triggerDefinitionService.executeDynamicProperties(
            componentName, componentVersion, triggerName, triggerParameters, propertyName, connection);
    }

    @Override
    public void executeDynamicWebhookDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters, @NonNull String workflowExecutionId,
        @NonNull Map<String, ?> outputParameters, Long connectionId) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        triggerDefinitionService.executeDynamicWebhookDisable(
            componentName, componentVersion, triggerName, triggerParameters, workflowExecutionId, outputParameters,
            connection);
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters, @NonNull String workflowExecutionId, Long connectionId,
        @NonNull String webhookUrl) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        return triggerDefinitionService.executeDynamicWebhookEnable(
            componentName, componentVersion, triggerName, triggerParameters,
            createWebhookUrl(workflowExecutionId, webhookUrl), workflowExecutionId, connection);
    }

    @Override
    public String executeEditorDescription(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters, Long connectionId) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        return triggerDefinitionService.executeEditorDescription(
            componentName, componentVersion, triggerName, triggerParameters, connection);
    }

    @Override
    public void executeListenerDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters, @NonNull String workflowExecutionId, Long connectionId) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        triggerDefinitionService.executeListenerDisable(
            componentName, componentVersion, triggerName, triggerParameters, workflowExecutionId, connection);
    }

    @Override
    public void executeListenerEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters,
        @NonNull String workflowExecutionId, Long connectionId) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        triggerDefinitionService.executeOnEnableListener(
            componentName, componentVersion, triggerName, triggerParameters, workflowExecutionId, connection);
    }

    @Override
    public List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName, @NonNull String propertyName,
        @NonNull Map<String, ?> triggerParameters, Long connectionId, String searchText) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        return triggerDefinitionService.executeOptions(
            componentName, componentVersion, triggerName, triggerParameters, propertyName, searchText, connection);
    }

    @Override
    public List<? extends ValueProperty<?>> executeOutputSchema(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters, Long connectionId) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        return triggerDefinitionService.executeOutputSchema(
            componentName, componentVersion, triggerName, triggerParameters, connection);

    }

    @Override
    public Object executeSampleOutput(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters, Long connectionId) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        return triggerDefinitionService.executeSampleOutput(
            componentName, componentVersion, triggerName, triggerParameters, connection);

    }

    @Override
    public TriggerOutput executeTrigger(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters,
        Object triggerState, @NonNull WebhookRequest webhookRequest, Long connectionId) {

        return triggerDefinitionService.executeTrigger(
            componentName, componentVersion, triggerName, inputParameters, triggerState, webhookRequest,
            connectionId == null ? null : connectionService.getConnection(connectionId));
    }

    @Override
    public boolean executeWebhookValidate(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters,
        @NonNull WebhookRequest webhookRequest, Long connectionId) {

        return triggerDefinitionService.executeWebhookValidate(
            componentName, componentVersion, triggerName, inputParameters, webhookRequest,
            connectionId == null ? null : connectionService.getConnection(connectionId));
    }

    private String createWebhookUrl(String workflowExecutionId, String webhookUrl) {
        return webhookUrl + "/api/webhooks/" + workflowExecutionId;
    }
}

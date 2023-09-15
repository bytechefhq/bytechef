
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

package com.bytechef.hermes.component.registry.remote.client.service;

import com.bytechef.commons.webclient.DefaultWebClient;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.component.registry.dto.WebhookTriggerFlags;
import com.bytechef.hermes.component.registry.trigger.WebhookRequest;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.registry.domain.Option;
import com.bytechef.hermes.component.registry.domain.TriggerDefinition;
import com.bytechef.hermes.registry.domain.ValueProperty;
import com.bytechef.hermes.component.registry.remote.client.AbstractWorkerClient;
import com.bytechef.hermes.component.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.component.registry.trigger.TriggerOutput;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnMissingClass(value = "com.bytechef.worker.WorkerApplication")
public class TriggerDefinitionServiceClient extends AbstractWorkerClient
    implements TriggerDefinitionService {

    public TriggerDefinitionServiceClient(
        DefaultWebClient defaultWebClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultWebClient, discoveryClient, objectMapper);
    }

    @Override
    public List<? extends ValueProperty<?>> executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters,
        @NonNull String propertyName, Connection connection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void executeDynamicWebhookDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters,
        @NonNull String workflowExecutionId, @NonNull Map<String, ?> outputParameters, Connection connection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters,
        @NonNull String webhookUrl, @NonNull String workflowExecutionId, Connection connection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookRefresh(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> outputParameters) {

        return defaultWebClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, "/trigger-definition-service/execute-dynamic-webhook-refresh"),
            new DynamicWebhookRefresh(componentName, componentVersion, triggerName, outputParameters),
            DynamicWebhookEnableOutput.class);
    }

    @Override
    public String executeEditorDescription(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters,
        Connection connection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void executeListenerDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters,
        @NonNull String workflowExecutionId, Connection connection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void executeOnEnableListener(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters,
        @NonNull String workflowExecutionId, Connection connection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters,
        @NonNull String propertyName, String searchText, Connection connection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends ValueProperty<?>> executeOutputSchema(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters,
        Connection connection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object executeSampleOutput(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        Map<String, ?> inputParameters,
        Connection connection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public TriggerOutput executeTrigger(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters,
        Object triggerState, @NonNull WebhookRequest webhookRequest, Connection connection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean executeWebhookValidate(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull WebhookRequest webhookRequest, Connection connection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public TriggerDefinition getTriggerDefinition(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName) {

        return defaultWebClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                "/trigger-definition-service/get-trigger-definition/{componentName}/{componentVersion}/{triggerName}",
                componentName, componentVersion, triggerName),
            TriggerDefinition.class);
    }

    @Override
    public List<TriggerDefinition> getTriggerDefinitions(@NonNull String componentName, int componentVersion) {
        return defaultWebClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                "/trigger-definition-service/get-trigger-definitions/{componentName}/{componentVersion}", componentName,
                componentVersion),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public WebhookTriggerFlags getWebhookTriggerFlags(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName) {

        return defaultWebClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                "/trigger-definition-service/get-webhook-trigger-flags/{componentName}/{componentVersion}" +
                    "/{triggerName}",
                componentName, componentVersion, triggerName),
            WebhookTriggerFlags.class);
    }

    private record DynamicWebhookRefresh(
        String componentName, int componentVersion, String triggerName, Map<String, ?> outputParameters) {
    }
}

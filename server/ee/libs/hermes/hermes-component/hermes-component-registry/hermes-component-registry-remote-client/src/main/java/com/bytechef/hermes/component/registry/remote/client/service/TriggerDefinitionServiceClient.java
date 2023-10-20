
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
import com.bytechef.hermes.component.registry.trigger.WebhookRequest;
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
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        String propertyName, Long connectionId, Map<String, ?> connectionParameters, String authorizationName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void executeDynamicWebhookDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        Map<String, ?> connectionParameters, String authorizationName, String workflowExecutionId,
        DynamicWebhookEnableOutput output) {

        throw new UnsupportedOperationException();
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        Map<String, ?> connectionParameters, String authorizationName, String webhookUrl, String workflowExecutionId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookRefresh(
        String componentName, int componentVersion, String triggerName, DynamicWebhookEnableOutput output) {

        return defaultWebClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, "/trigger-definition-service/execute-dynamic-webhook-refresh"),
            new DynamicWebhookRefresh(componentName, componentVersion, triggerName, output),
            DynamicWebhookEnableOutput.class);
    }

    @Override
    public String executeEditorDescription(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        Long connectionId, Map<String, ?> connectionParameters, String authorizationName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void executeListenerDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        Map<String, ?> connectionParameters, String authorizationName, String workflowExecutionId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void executeOnEnableListener(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        Map<String, ?> connectionParameters, String authorizationName, String workflowExecutionId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        String propertyName, Long connectionId, Map<String, ?> connectionParameters, String authorizationName,
        String searchText) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends ValueProperty<?>> executeOutputSchema(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        Long connectionId, Map<String, ?> connectionParameters, String authorizationName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object executeSampleOutput(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        Long connectionId, Map<String, ?> connectionParameters, String authorizationName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public TriggerOutput executeTrigger(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        Object triggerState, WebhookRequest webhookRequest, Map<String, Long> connectionIdMap) {

        return defaultWebClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, "/trigger-definition-service/execute-trigger"),
            new TriggerRequest(
                componentName, componentVersion, triggerName, inputParameters, triggerState, webhookRequest,
                connectionIdMap),
            TriggerOutput.class);
    }

    @Override
    public TriggerDefinition getTriggerDefinition(String componentName, int componentVersion, String triggerName) {
        return defaultWebClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                "/trigger-definition-service/get-trigger-definition/{componentName}/{componentVersion}/{triggerName}",
                componentName, componentVersion, triggerName),
            TriggerDefinition.class);
    }

    @Override
    public boolean executeWebhookValidate(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        WebhookRequest webhookRequest, Map<String, Long> connectionIdMap) {

        return defaultWebClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, "/trigger-definition-service/execute-webhook-validate"),
            new WebhookValidateRequest(
                componentName, componentVersion, triggerName, inputParameters, webhookRequest,
                connectionIdMap),
            Boolean.class);
    }

    @Override
    public List<TriggerDefinition> getTriggerDefinitions(String componentName, int componentVersion) {
        return defaultWebClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                "/trigger-definition-service/get-trigger-definitions/{componentName}/{componentVersion}", componentName,
                componentVersion),
            new ParameterizedTypeReference<>() {});
    }

    private record DynamicWebhookRefresh(
        String componentName, int componentVersion, String triggerName, DynamicWebhookEnableOutput output) {
    }

    private record TriggerRequest(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters, Object state,
        WebhookRequest webhookRequest, Map<String, Long> connectionIdMap) {
    }

    private record WebhookValidateRequest(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        WebhookRequest webhookRequest, Map<String, Long> connectionIdMap) {
    }
}

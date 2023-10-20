
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

package com.bytechef.hermes.component.registry.remote.client.facade;

import com.bytechef.commons.webclient.DefaultWebClient;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.component.registry.trigger.TriggerOutput;
import com.bytechef.hermes.component.registry.trigger.WebhookRequest;
import com.bytechef.hermes.registry.domain.Option;
import com.bytechef.hermes.registry.domain.ValueProperty;
import com.bytechef.hermes.component.registry.facade.TriggerDefinitionFacade;
import com.bytechef.hermes.component.registry.remote.client.AbstractWorkerClient;
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
public class TriggerDefinitionFacadeClient extends AbstractWorkerClient implements TriggerDefinitionFacade {

    public TriggerDefinitionFacadeClient(
        DefaultWebClient defaultWebClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultWebClient, discoveryClient, objectMapper);
    }

    @Override
    public String executeEditorDescription(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters, Long connectionId) {

        return defaultWebClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, "/trigger-definition-facade/execute-editor-description"),
            new EditorDescriptionRequest(
                triggerName, triggerParameters, componentName, componentVersion, connectionId),
            String.class);
    }

    @Override
    public void executeListenerDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters, @NonNull String workflowExecutionId, Long connectionId) {

        defaultWebClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, "/trigger-definition-facade/execute-listener-disable"),
            new ListenerDisableRequest(
                componentName, componentVersion, triggerName, triggerParameters, workflowExecutionId, connectionId));
    }

    @Override
    public void executeListenerEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters, @NonNull String workflowExecutionId, Long connectionId) {

        defaultWebClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, "/trigger-definition-facade/execute-listener-enable"),
            new ListenerEnableRequest(
                componentName, componentVersion, triggerName, triggerParameters, workflowExecutionId, connectionId));
    }

    @Override
    public List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName, @NonNull String propertyName,
        @NonNull Map<String, ?> triggerParameters, Long connectionId, String searchText) {

        return defaultWebClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, "/trigger-definition-facade/execute-options"),
            new OptionsRequest(
                triggerName, propertyName, triggerParameters, componentName, componentVersion, connectionId,
                searchText),
            new ParameterizedTypeReference<List<Option>>() {});
    }

    @Override
    public List<? extends ValueProperty<?>> executeOutputSchema(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters, Long connectionId) {

        return defaultWebClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, "/trigger-definition-facade/execute-output-schema"),
            new OutputSchemaRequest(triggerName, triggerParameters, componentName, componentVersion, connectionId),
            new ParameterizedTypeReference<List<? extends ValueProperty<?>>>() {});
    }

    @Override
    public List<? extends ValueProperty<?>> executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName, @NonNull String propertyName,
        @NonNull Map<String, Object> triggerParameters, Long connectionId) {

        return defaultWebClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, "/trigger-definition-facade/execute-properties"),
            new PropertiesRequest(
                triggerName, triggerParameters, componentName, componentVersion, connectionId, propertyName),
            new ParameterizedTypeReference<List<? extends ValueProperty<?>>>() {});
    }

    @Override
    public void executeDynamicWebhookDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters, @NonNull String workflowExecutionId, Map<String, ?> outputParameters,
        Long connectionId) {

        defaultWebClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, "/trigger-definition-facade/execute-dynamic-webhook-disable"),
            new DynamicWebhookDisableRequest(
                componentName, componentVersion, triggerName, triggerParameters, workflowExecutionId, outputParameters,
                connectionId));
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters, @NonNull String workflowExecutionId, Long connectionId,
        @NonNull String webhookUrl) {

        return defaultWebClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, "/trigger-definition-facade/execute-dynamic-webhook-enable"),
            new DynamicWebhookEnableRequest(
                componentName, componentVersion, triggerName, triggerParameters, workflowExecutionId, connectionId,
                webhookUrl),
            DynamicWebhookEnableOutput.class);
    }

    @Override
    public Object executeSampleOutput(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters, Long connectionId) {

        return defaultWebClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, "/trigger-definition-facade/execute-sample-output"),
            new SampleOutputRequest(triggerName, triggerParameters, componentName, componentVersion, connectionId),
            Object.class);
    }

    @Override
    public TriggerOutput executeTrigger(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, Object triggerState, @NonNull WebhookRequest webhookRequest,
        Long connectionId) {

        return defaultWebClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, "/trigger-definition-facade/execute-trigger"),
            new TriggerRequest(
                componentName, componentVersion, triggerName, inputParameters, triggerState, webhookRequest,
                connectionId),
            TriggerOutput.class);
    }

    @Override
    public boolean executeWebhookValidate(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull WebhookRequest webhookRequest, Long connectionId) {

        return defaultWebClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, "/trigger-definition-facade/execute-webhook-validate"),
            new WebhookValidateRequest(
                componentName, componentVersion, triggerName, inputParameters, webhookRequest,
                connectionId),
            Boolean.class);
    }

    private record EditorDescriptionRequest(
        String triggerName, Map<String, ?> triggerParameters, String componentName, int componentVersion,
        Long connectionId) {
    }

    private record OptionsRequest(
        String triggerName, String propertyName, Map<String, ?> triggerParameters,
        String componentName, int componentVersion, Long connectionId, String searchText) {
    }

    private record OutputSchemaRequest(
        String triggerName, Map<String, ?> triggerParameters, String componentName, int componentVersion,
        Long connectionId) {
    }

    private record PropertiesRequest(
        String triggerName, Map<String, ?> triggerParameters, String componentName, int componentVersion,
        Long connectionId, String propertyName) {
    }

    private record SampleOutputRequest(
        String triggerName, Map<String, ?> triggerParameters, String componentName, int componentVersion,
        Long connectionId) {
    }

    private record DynamicWebhookDisableRequest(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        String workflowExecutionId, Map<String, ?> outputParameters, Long connectionIdd) {
    }

    private record DynamicWebhookEnableRequest(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        String workflowExecutionId, Long connectionId, String webhookUrl) {
    }

    private record ListenerDisableRequest(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        String workflowExecutionId, Long connectionId) {
    }

    private record ListenerEnableRequest(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        String workflowExecutionId, Long connectionI) {
    }

    private record TriggerRequest(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters, Object state,
        WebhookRequest webhookRequest, Long connectionId) {
    }

    private record WebhookValidateRequest(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        WebhookRequest webhookRequest, Long connectionId) {
    }
}


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

package com.bytechef.hermes.component.registry.remote.client.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.webclient.DefaultWebClient;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerContext;
import com.bytechef.hermes.component.registry.ComponentOperation;
import com.bytechef.hermes.component.registry.dto.ComponentConnection;
import com.bytechef.hermes.component.registry.dto.WebhookTriggerFlags;
import com.bytechef.hermes.component.registry.domain.TriggerDefinition;
import com.bytechef.hermes.component.registry.remote.client.AbstractWorkerClient;
import com.bytechef.hermes.component.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.component.registry.trigger.TriggerOutput;
import com.bytechef.hermes.component.registry.trigger.WebhookRequest;
import com.bytechef.hermes.registry.domain.Option;
import com.bytechef.hermes.registry.domain.ValueProperty;
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
public class RemoteTriggerDefinitionServiceClient extends AbstractWorkerClient implements TriggerDefinitionService {

    private static final String TRIGGER_DEFINITION_SERVICE = "/trigger-definition-service";

    public RemoteTriggerDefinitionServiceClient(
        DefaultWebClient defaultWebClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultWebClient, discoveryClient, objectMapper);
    }

    @Override
    public void executeDynamicWebhookDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, Map<String, ?> outputParameters, ComponentConnection connection,
        TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends ValueProperty<?>> executeDynamicProperties(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String propertyName, ComponentConnection connection,
        TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookEnable(
        String componentName, int componentVersion, String triggerName,
        Map<String, ?> inputParameters, String webhookUrl, String workflowExecutionId, ComponentConnection connection,
        TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookRefresh(
        String componentName, int componentVersion, String triggerName, Map<String, ?> outputParameters,
        TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public String executeEditorDescription(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        ComponentConnection connection, TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void executeListenerDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, ComponentConnection connection, TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void executeOnEnableListener(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, ComponentConnection connection, TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String propertyName, String searchText, ComponentConnection connection, TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends ValueProperty<?>> executeOutputSchema(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        ComponentConnection connection, TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object executeSampleOutput(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        ComponentConnection connection, TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public TriggerOutput executeTrigger(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        Object triggerState, WebhookRequest webhookRequest, ComponentConnection connection, TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean executeWebhookValidate(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        WebhookRequest webhookRequest, ComponentConnection connection, TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public TriggerDefinition getTriggerDefinition(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName) {

        return defaultWebClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                TRIGGER_DEFINITION_SERVICE + "/get-trigger-definition/{componentName}/{componentVersion}/{triggerName}",
                componentName, componentVersion, triggerName),
            TriggerDefinition.class);
    }

    @Override
    public List<TriggerDefinition> getTriggerDefinitions(@NonNull String componentName, int componentVersion) {
        return defaultWebClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                TRIGGER_DEFINITION_SERVICE + "/get-trigger-definitions/{componentName}/{componentVersion}",
                componentName,
                componentVersion),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<TriggerDefinition> getTriggerDefinitions(List<ComponentOperation> componentOperations) {
        return CollectionUtils.map(
            componentOperations,
            componentOperation -> getTriggerDefinition(
                componentOperation.componentName(), componentOperation.componentVersion(),
                componentOperation.operationName()));
    }

    @Override
    public WebhookTriggerFlags getWebhookTriggerFlags(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName) {

        return defaultWebClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                TRIGGER_DEFINITION_SERVICE + "/get-webhook-trigger-flags/{componentName}/{componentVersion}" +
                    "/{triggerName}",
                componentName, componentVersion, triggerName),
            WebhookTriggerFlags.class);
    }
}

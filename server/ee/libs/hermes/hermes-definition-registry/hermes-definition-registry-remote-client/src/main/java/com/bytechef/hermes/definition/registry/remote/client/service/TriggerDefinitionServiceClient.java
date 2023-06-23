
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

package com.bytechef.hermes.definition.registry.remote.client.service;

import com.bytechef.hermes.definition.registry.dto.OptionDTO;
import com.bytechef.hermes.definition.registry.dto.TriggerDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.ValuePropertyDTO;
import com.bytechef.hermes.definition.registry.remote.client.AbstractWorkerClient;
import com.bytechef.hermes.definition.registry.service.TriggerDefinitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
import java.util.Map;

import static com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;

/**
 * @author Ivica Cardic
 */
public class TriggerDefinitionServiceClient extends AbstractWorkerClient
    implements TriggerDefinitionService {

    public TriggerDefinitionServiceClient(DiscoveryClient discoveryClient, ObjectMapper objectMapper) {
        super(discoveryClient, objectMapper);
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

        return WORKER_WEB_CLIENT
            .post()
            .uri(uriBuilder -> toUri(
                uriBuilder, componentName, "/trigger-definition-service/execute-dynamic-webhook-refresh"))
            .bodyValue(new DynamicWebhookRefresh(componentName, componentVersion, triggerName, output))
            .retrieve()
            .bodyToMono(DynamicWebhookEnableOutput.class)
            .block();
    }

    @Override
    public String executeEditorDescription(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        Map<String, ?> connectionParameters, String authorizationName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void executeListenerDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        Map<String, ?> connectionParameters, String authorizationName, String workflowExecutionId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void executeListenerEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        Map<String, ?> connectionParameters, String authorizationName, String workflowExecutionId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<OptionDTO> executeOptions(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        String propertyName, Map<String, ?> connectionParameters, String authorizationName, String searchText) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends ValuePropertyDTO<?>> executeOutputSchema(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        Map<String, ?> connectionParameters, String authorizationName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends ValuePropertyDTO<?>> executeDynamicProperties(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        String propertyName, Map<String, ?> connectionParameters, String authorizationName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object executeSampleOutput(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        Map<String, ?> connectionParameters, String authorizationName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public TriggerDefinitionDTO getTriggerDefinition(String componentName, int componentVersion, String triggerName) {
        return WORKER_WEB_CLIENT
            .get()
            .uri(uriBuilder -> toUri(
                uriBuilder, componentName,
                "/trigger-definition-service/get-trigger-definition/{componentName}/{componentVersion}/{triggerName}",
                componentName, componentVersion, triggerName))
            .retrieve()
            .bodyToMono(TriggerDefinitionDTO.class)
            .block();
    }

    @Override
    public List<TriggerDefinitionDTO> getTriggerDefinitions(String componentName, int componentVersion) {
        return WORKER_WEB_CLIENT
            .get()
            .uri(uriBuilder -> toUri(
                uriBuilder, componentName,
                "/trigger-definition-service/get-trigger-definitions/{componentName}/{componentVersion}", componentName,
                componentVersion))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<TriggerDefinitionDTO>>() {})
            .block();
    }

    private record DynamicWebhookRefresh(
        String componentName, int componentVersion, String triggerName, DynamicWebhookEnableOutput output) {
    }
}

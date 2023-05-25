
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

package com.bytechef.hermes.definition.registry.service.web.rest.client.service;

import com.bytechef.commons.reactor.util.MonoUtils;
import com.bytechef.hermes.definition.registry.dto.OptionDTO;
import com.bytechef.hermes.definition.registry.dto.TriggerDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.ValuePropertyDTO;
import com.bytechef.hermes.definition.registry.service.web.rest.client.AbstractWorkerClient;
import com.bytechef.hermes.definition.registry.service.TriggerDefinitionService;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;

/**
 * @author Ivica Cardic
 */
public class TriggerDefinitionServiceClient extends AbstractWorkerClient
    implements TriggerDefinitionService {

    public TriggerDefinitionServiceClient(DiscoveryClient discoveryClient) {
        super(discoveryClient);
    }

    @Override
    public void executeDynamicWebhookDisable(
        String triggerName, String componentName, int componentVersion, Map<String, ?> connectionParameters,
        String authorizationName, Map<String, ?> triggerParameters, String workflowExecutionId,
        DynamicWebhookEnableOutput output) {

        WORKER_WEB_CLIENT
            .post()
            .uri(uriBuilder -> toUri(uriBuilder, componentName, "/trigger-definitions/dynamic-webhook-disable"))
            .bodyValue(
                new DynamicWebhookDisable(
                    authorizationName, componentName, componentVersion, connectionParameters, output, triggerName,
                    triggerParameters, workflowExecutionId))
            .retrieve();
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookEnable(
        String triggerName, String componentName, int componentVersion, Map<String, ?> connectionParameters,
        String authorizationName, Map<String, ?> triggerParameters, String webhookUrl,
        String workflowExecutionId) {

        return MonoUtils.get(WORKER_WEB_CLIENT
            .post()
            .uri(uriBuilder -> toUri(uriBuilder, componentName, "/trigger-definitions/dynamic-webhook-enable"))
            .bodyValue(
                new DynamicWebhookEnable(
                    authorizationName, componentName, componentVersion, connectionParameters, triggerName,
                    triggerParameters, webhookUrl, workflowExecutionId))
            .retrieve()
            .bodyToMono(DynamicWebhookEnableOutput.class));
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookRefresh(
        String componentName, int componentVersion, String triggerName, DynamicWebhookEnableOutput output) {

        return MonoUtils.get(WORKER_WEB_CLIENT
            .post()
            .uri(uriBuilder -> toUri(uriBuilder, componentName, "/trigger-definitions/dynamic-webhook-enable"))
            .bodyValue(new DynamicWebhookRefresh(componentName, componentVersion, output, triggerName))
            .retrieve()
            .bodyToMono(DynamicWebhookEnableOutput.class));
    }

    @Override
    public String executeEditorDescription(
        String triggerName, String componentName, int componentVersion, Map<String, ?> triggerParameters,
        String authorizationName, Map<String, ?> connectionParameters) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void executeListenerDisable(
        String triggerName, String componentName, int componentVersion, Map<String, ?> connectionParameters,
        String authorizationName, Map<String, ?> triggerParameters, String workflowExecutionId) {

        WORKER_WEB_CLIENT
            .post()
            .uri(uriBuilder -> toUri(uriBuilder, componentName, "/trigger-definitions/execute-listener-disable"))
            .bodyValue(
                new ListenerDisable(
                    authorizationName, componentName, componentVersion, connectionParameters, triggerName,
                    triggerParameters, workflowExecutionId))
            .retrieve();
    }

    @Override
    public void executeListenerEnable(
        String triggerName, String componentName, int componentVersion, Map<String, ?> connectionParameters,
        String authorizationName, Map<String, ?> triggerParameters,
        String workflowExecutionId) {

        WORKER_WEB_CLIENT
            .post()
            .uri(uriBuilder -> toUri(uriBuilder, componentName, "/trigger-definitions/execute-listener-enable"))
            .bodyValue(
                new ListenerEnable(
                    authorizationName, componentName, componentVersion, connectionParameters, triggerName,
                    triggerParameters, workflowExecutionId))
            .retrieve();
    }

    @Override
    public List<OptionDTO> executeOptions(
        String propertyName, String triggerName, String componentName, int componentVersion,
        Map<String, ?> triggerParameters, String authorizationName, Map<String, ?> connectionParameters) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends ValuePropertyDTO<?>> executeOutputSchema(
        String triggerName, String componentName, int componentVersion, Map<String, ?> triggerParameters,
        String authorizationName, Map<String, ?> connectionParameters) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends ValuePropertyDTO<?>> executeDynamicProperties(
        String propertyName, String triggerName, String componentName, int componentVersion,
        Map<String, ?> triggerParameters, String authorizationName, Map<String, ?> connectionParameters) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object executeSampleOutput(
        String triggerName, String componentName, int componentVersion, Map<String, ?> triggerParameters,
        String authorizationName, Map<String, ?> connectionParameters) {

        throw new UnsupportedOperationException();
    }

    @Override
    public TriggerDefinitionDTO getTriggerDefinition(String triggerName, String componentName, int componentVersion) {
        return MonoUtils.get(getTriggerDefinitionMono(triggerName, componentName, componentVersion));
    }

    @Override
    public Mono<TriggerDefinitionDTO> getTriggerDefinitionMono(
        String triggerName, String componentName, int componentVersion) {

        return WORKER_WEB_CLIENT
            .get()
            .uri(uriBuilder -> toUri(
                uriBuilder, componentName,
                "/component-definitions/{componentName}/{componentVersion}/trigger-definitions/{triggerName}",
                componentName, componentVersion, triggerName))
            .retrieve()
            .bodyToMono(TriggerDefinitionDTO.class);
    }

    @Override
    public List<TriggerDefinitionDTO> getTriggerDefinitions(String componentName, int componentVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<List<TriggerDefinitionDTO>> getTriggerDefinitionsMono(String componentName, int componentVersion) {
        return WORKER_WEB_CLIENT
            .get()
            .uri(uriBuilder -> toUri(
                uriBuilder, componentName,
                "/component-definitions/{componentName}/{componentVersion}/trigger-definitions", componentName,
                componentVersion))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<>() {});
    }

    private record DynamicWebhookDisable(
        String authorizationName, String componentName, int componentVersion,
        Map<String, ?> connectionParameters, DynamicWebhookEnableOutput output, String triggerName,
        Map<String, ?> triggerParameters, String workflowExecutionId) {
    }

    private record DynamicWebhookEnable(
        String authorizationName, String componentName, int componentVersion, Map<String, ?> connectionParameters,
        String triggerName, Map<String, ?> triggerParameters, String webhookUrl, String workflowExecutionId) {
    }

    private record DynamicWebhookRefresh(
        String componentName, int componentVersion, DynamicWebhookEnableOutput output, String triggerName) {
    }

    private record ListenerDisable(
        String authorizationName, String componentName, int componentVersion, Map<String, ?> connectionParameters,
        String triggerName, Map<String, ?> triggerParameters, String workflowExecutionId) {
    }

    private record ListenerEnable(
        String authorizationName, String componentName, int componentVersion, Map<String, ?> connectionParameters,
        String triggerName, Map<String, ?> triggerParameters, String workflowExecutionId) {
    }
}

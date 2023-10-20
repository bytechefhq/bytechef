
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

package com.bytechef.hermes.definition.registry.rsocket.client.service;

import com.bytechef.commons.reactor.util.MonoUtils;
import com.bytechef.hermes.definition.Option;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.registry.dto.TriggerDefinitionDTO;
import com.bytechef.hermes.definition.registry.rsocket.client.AbstractRSocketClient;
import com.bytechef.hermes.definition.registry.service.TriggerDefinitionService;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static com.bytechef.hermes.component.definition.TriggerDefinition.*;

/**
 * @author Ivica Cardic
 */
public class TriggerDefinitionServiceRSocketClient extends AbstractRSocketClient
    implements TriggerDefinitionService {

    public TriggerDefinitionServiceRSocketClient(
        DiscoveryClient discoveryClient, RSocketRequester.Builder rSocketRequesterBuilder) {

        super(discoveryClient, rSocketRequesterBuilder);
    }

    @Override
    public void executeDynamicWebhookDisable(
        String triggerName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> triggerParameters, String workflowExecutionId,
        DynamicWebhookEnableOutput output) {

        getRSocketRequester(componentName)
            .route("TriggerDefinitionService.executeDynamicWebhookDisable")
            .data(new DynamicWebhookDisable(
                authorizationName, componentName, componentVersion, connectionParameters, output, triggerName,
                triggerParameters, workflowExecutionId))
            .send();
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookEnable(
        String triggerName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> triggerParameters, String webhookUrl,
        String workflowExecutionId) {

        return MonoUtils.get(getRSocketRequester(componentName)
            .route("TriggerDefinitionService.executeDynamicWebhookEnable")
            .data(new DynamicWebhookEnable(
                authorizationName, componentName, componentVersion, connectionParameters, triggerName,
                triggerParameters, webhookUrl, workflowExecutionId))
            .retrieveMono(DynamicWebhookEnableOutput.class));
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookRefresh(
        String componentName, int componentVersion, String triggerName, DynamicWebhookEnableOutput output) {

        return MonoUtils.get(getRSocketRequester(componentName)
            .route("TriggerDefinitionService.executeDynamicWebhookRefresh")
            .data(new DynamicWebhookRefresh(componentName, componentVersion, output, triggerName))
            .retrieveMono(DynamicWebhookEnableOutput.class));
    }

    @Override
    public String executeEditorDescription(
        String triggerName, String componentName, int componentVersion, Map<String, Object> triggerParameters,
        String authorizationName, Map<String, Object> connectionParameters) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void executeListenerDisable(
        String triggerName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> triggerParameters, String workflowExecutionId) {

        getRSocketRequester(componentName)
            .route("TriggerDefinitionService.executeListenerDisable")
            .data(new ListenerDisable(
                authorizationName, componentName, componentVersion, connectionParameters, triggerName,
                triggerParameters, workflowExecutionId))
            .send();
    }

    @Override
    public void executeListenerEnable(
        String triggerName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> triggerParameters,
        String workflowExecutionId) {

        getRSocketRequester(componentName)
            .route("TriggerDefinitionService.executeListenerEnable")
            .data(new ListenerEnable(
                authorizationName, componentName, componentVersion, connectionParameters, triggerName,
                triggerParameters, workflowExecutionId))
            .send();
    }

    @Override
    public List<Option<?>> executeOptions(
        String propertyName, String triggerName, String componentName, int componentVersion,
        Map<String, Object> triggerParameters, String authorizationName, Map<String, Object> connectionParameters) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends Property<?>> executeOutputSchema(
        String triggerName, String componentName, int componentVersion, Map<String, Object> triggerParameters,
        String authorizationName, Map<String, Object> connectionParameters) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends Property<?>> executeDynamicProperties(
        String propertyName, String triggerName, String componentName, int componentVersion,
        Map<String, Object> triggerParameters, String authorizationName, Map<String, Object> connectionParameters) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object executeSampleOutput(
        String triggerName, String componentName, int componentVersion, Map<String, Object> triggerParameters,
        String authorizationName, Map<String, Object> connectionParameters) {

        throw new UnsupportedOperationException();
    }

    @Override
    public TriggerDefinitionDTO getTriggerDefinition(String triggerName, String componentName, int componentVersion) {
        return MonoUtils.get(getRSocketRequester(componentName)
            .route("TriggerDefinitionService.getTriggerDefinition")
            .data(
                Map.of(
                    "componentName", componentName, "componentVersion", componentVersion,
                    "triggerName", triggerName))
            .retrieveMono(TriggerDefinitionDTO.class));
    }

    @Override
    public Mono<TriggerDefinitionDTO> getTriggerDefinitionMono(
        String triggerName, String componentName, int componentVersion) {

        return getRSocketRequester(componentName)
            .route("TriggerDefinitionService.getTriggerDefinition")
            .data(
                Map.of(
                    "componentName", componentName, "componentVersion", componentVersion,
                    "triggerName", triggerName))
            .retrieveMono(TriggerDefinitionDTO.class);
    }

    @Override
    public Mono<List<TriggerDefinitionDTO>> getTriggerDefinitions(
        String componentName, int componentVersion) {

        return getRSocketRequester(componentName)
            .route("TriggerDefinitionService.getTriggerDefinitions")
            .data(
                Map.of("componentName", componentName, "componentVersion", componentVersion))
            .retrieveMono(new ParameterizedTypeReference<>() {});
    }

    private record DynamicWebhookDisable(
        String authorizationName, String componentName, int componentVersion,
        Map<String, Object> connectionParameters, DynamicWebhookEnableOutput output, String triggerName,
        Map<String, Object> triggerParameters, String workflowExecutionId) {
    }

    private record DynamicWebhookEnable(
        String authorizationName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String triggerName, Map<String, Object> triggerParameters, String webhookUrl, String workflowExecutionId) {
    }

    private record DynamicWebhookRefresh(
        String componentName, int componentVersion, DynamicWebhookEnableOutput output, String triggerName) {
    }

    private record ListenerDisable(
        String authorizationName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String triggerName, Map<String, Object> triggerParameters, String workflowExecutionId) {
    }

    private record ListenerEnable(
        String authorizationName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String triggerName, Map<String, Object> triggerParameters, String workflowExecutionId) {
    }
}

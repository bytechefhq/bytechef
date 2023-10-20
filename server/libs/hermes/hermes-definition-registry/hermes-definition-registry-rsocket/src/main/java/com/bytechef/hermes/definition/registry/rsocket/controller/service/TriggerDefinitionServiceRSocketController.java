
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

package com.bytechef.hermes.definition.registry.rsocket.controller.service;

import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.definition.registry.dto.TriggerDefinitionDTO;
import com.bytechef.hermes.definition.registry.service.TriggerDefinitionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnProperty(prefix = "spring", name = "application.name", havingValue = "worker-service-app")
public class TriggerDefinitionServiceRSocketController {

    private final TriggerDefinitionService triggerDefinitionService;

    public TriggerDefinitionServiceRSocketController(TriggerDefinitionService triggerDefinitionService) {
        this.triggerDefinitionService = triggerDefinitionService;
    }

    @MessageMapping("TriggerDefinitionService.executeDynamicWebhookDisable")
    public Mono<Void> executeDynamicWebhookDisable(DynamicWebhookDisable dynamicWebhookDisable) {
        triggerDefinitionService.executeDynamicWebhookDisable(
            dynamicWebhookDisable.triggerName, dynamicWebhookDisable.componentName,
            dynamicWebhookDisable.componentVersion, dynamicWebhookDisable.connectionParameters,
            dynamicWebhookDisable.authorizationName, dynamicWebhookDisable.triggerParameters,
            dynamicWebhookDisable.workflowExecutionId, dynamicWebhookDisable.output);

        return Mono.empty();
    }

    @MessageMapping("TriggerDefinitionService.executeDynamicWebhookEnable")
    public Mono<DynamicWebhookEnableOutput> executeDynamicWebhookEnable(
        DynamicWebhookEnable dynamicWebhookEnable) {

        return Mono.just(triggerDefinitionService.executeDynamicWebhookEnable(
            dynamicWebhookEnable.triggerName, dynamicWebhookEnable.componentName, dynamicWebhookEnable.componentVersion,
            dynamicWebhookEnable.connectionParameters, dynamicWebhookEnable.authorizationName,
            dynamicWebhookEnable.triggerParameters, dynamicWebhookEnable.webhookUrl,
            dynamicWebhookEnable.workflowExecutionId));
    }

    @MessageMapping("TriggerDefinitionService.executeDynamicWebhookRefresh")
    public Mono<DynamicWebhookEnableOutput> executeDynamicWebhookRefresh(
        DynamicWebhookRefresh dynamicWebhookRefresh) {

        return Mono.just(triggerDefinitionService.executeDynamicWebhookRefresh(
            dynamicWebhookRefresh.componentName, dynamicWebhookRefresh.componentVersion,
            dynamicWebhookRefresh.triggerName, dynamicWebhookRefresh.output));
    }

    @MessageMapping("TriggerDefinitionFacade.executeListenerDisable")
    public Mono<Void> executeListenerDisable(ListenerDisable listenerDisable) {
        triggerDefinitionService.executeListenerDisable(
            listenerDisable.triggerName, listenerDisable.componentName, listenerDisable.componentVersion,
            listenerDisable.connectionParameters, listenerDisable.authorizationName, listenerDisable.triggerParameters,
            listenerDisable.workflowExecutionId);

        return Mono.empty();
    }

    @MessageMapping("TriggerDefinitionFacade.executeListenerEnable")
    public Mono<Void> executeListenerEnable(ListenerEnable listenerEnable) {
        triggerDefinitionService.executeListenerEnable(
            listenerEnable.triggerName, listenerEnable.componentName, listenerEnable.componentVersion,
            listenerEnable.connectionParameters, listenerEnable.authorizationName, listenerEnable.triggerParameters,
            listenerEnable.workflowExecutionId);

        return Mono.empty();
    }

    @MessageMapping("TriggerDefinitionService.getTriggerDefinition")
    public Mono<TriggerDefinitionDTO> getTriggerDefinitionMono(Map<String, Object> map) {
        return triggerDefinitionService.getTriggerDefinitionMono(
            (String) map.get("triggerName"), (String) map.get("componentName"), (Integer) map.get("componentVersion"));
    }

    @MessageMapping("TriggerDefinitionService.getTriggerDefinitions")
    public Mono<List<TriggerDefinitionDTO>> getTriggerDefinitions(Map<String, Object> map) {
        return triggerDefinitionService.getTriggerDefinitions(
            (String) map.get("componentName"), (Integer) map.get("componentVersion"));
    }

    private record DynamicWebhookDisable(
        String authorizationName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        DynamicWebhookEnableOutput output, String triggerName, Map<String, Object> triggerParameters,
        String workflowExecutionId) {
    }

    private record DynamicWebhookEnable(
        String authorizationName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String triggerName, Map<String, Object> triggerParameters, String webhookUrl, String workflowExecutionId) {
    }

    private record DynamicWebhookRefresh(
        String componentName, int componentVersion, DynamicWebhookEnableOutput output,
        String triggerName) {
    }

    private record ListenerDisable(
        String authorizationName, String componentName, int componentVersion,
        Map<String, Object> connectionParameters, String triggerName, Map<String, Object> triggerParameters,
        String workflowExecutionId) {
    }

    private record ListenerEnable(
        String authorizationName, String componentName, int componentVersion,
        Map<String, Object> connectionParameters, String triggerName, Map<String, Object> triggerParameters,
        String workflowExecutionId) {
    }
}

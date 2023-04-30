
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

package com.bytechef.hermes.definition.registry.rsocket.controller;

import com.bytechef.hermes.definition.registry.TriggerDefinitionFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.Map;

import static com.bytechef.hermes.component.definition.TriggerDefinition.*;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnProperty(prefix = "spring", name = "application.name", havingValue = "worker-service-app")
public class TriggerDefinitionFacadeRSocketController {

    private final TriggerDefinitionFacade triggerDefinitionFacade;

    @SuppressFBWarnings("EI")
    public TriggerDefinitionFacadeRSocketController(TriggerDefinitionFacade triggerDefinitionFacade) {
        this.triggerDefinitionFacade = triggerDefinitionFacade;
    }

    @MessageMapping("TriggerDefinitionFacade.executeDynamicWebhookDisable")
    public Mono<Void> executeDynamicWebhookDisable(DynamicWebhookDisable dynamicWebhookDisable) {
        triggerDefinitionFacade.executeDynamicWebhookDisable(
            dynamicWebhookDisable.triggerName, dynamicWebhookDisable.componentName,
            dynamicWebhookDisable.componentVersion, dynamicWebhookDisable.connectionParameters,
            dynamicWebhookDisable.authorizationName, dynamicWebhookDisable.triggerParameters,
            dynamicWebhookDisable.workflowExecutionId, dynamicWebhookDisable.output);

        return Mono.empty();
    }

    @MessageMapping("TriggerDefinitionFacade.executeDynamicWebhookEnable")
    public Mono<DynamicWebhookEnableOutput> executeDynamicWebhookEnable(DynamicWebhookEnable dynamicWebhookEnable) {

        return Mono.just(triggerDefinitionFacade.executeDynamicWebhookEnable(
            dynamicWebhookEnable.triggerName, dynamicWebhookEnable.componentName, dynamicWebhookEnable.componentVersion,
            dynamicWebhookEnable.connectionParameters, dynamicWebhookEnable.authorizationName,
            dynamicWebhookEnable.triggerParameters, dynamicWebhookEnable.webhookUrl,
            dynamicWebhookEnable.workflowExecutionId));
    }

    @MessageMapping("TriggerDefinitionFacade.executeDynamicWebhookRefresh")
    public Mono<DynamicWebhookEnableOutput> executeDynamicWebhookRefresh(DynamicWebhookRefresh dynamicWebhookRefresh) {
        return Mono.just(triggerDefinitionFacade.executeDynamicWebhookRefresh(
            dynamicWebhookRefresh.triggerName, dynamicWebhookRefresh.componentName,
            dynamicWebhookRefresh.componentVersion, dynamicWebhookRefresh.output));
    }

    @MessageMapping("TriggerDefinitionFacade.executeListenerDisable")
    public Mono<Void> executeListenerDisable(ListenerDisable listenerDisable) {
        triggerDefinitionFacade.executeListenerDisable(
            listenerDisable.triggerName, listenerDisable.componentName, listenerDisable.componentVersion,
            listenerDisable.connectionParameters, listenerDisable.authorizationName, listenerDisable.triggerParameters,
            listenerDisable.workflowExecutionId);

        return Mono.empty();
    }

    @MessageMapping("TriggerDefinitionFacade.executeListenerEnable")
    public Mono<Void> executeListenerEnable(ListenerEnable listenerEnable) {
        triggerDefinitionFacade.executeListenerEnable(
            listenerEnable.triggerName, listenerEnable.componentName, listenerEnable.componentVersion,
            listenerEnable.connectionParameters, listenerEnable.authorizationName, listenerEnable.triggerParameters,
            listenerEnable.workflowExecutionId);

        return Mono.empty();
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
        String componentName, int componentVersion, DynamicWebhookEnableOutput output, String triggerName) {
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

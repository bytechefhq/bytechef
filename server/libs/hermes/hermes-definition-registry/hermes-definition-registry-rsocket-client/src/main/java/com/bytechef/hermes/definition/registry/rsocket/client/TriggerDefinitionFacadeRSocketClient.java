
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

package com.bytechef.hermes.definition.registry.rsocket.client;

import com.bytechef.commons.reactor.util.MonoUtils;
import com.bytechef.hermes.definition.registry.TriggerDefinitionFacade;
import com.bytechef.hermes.definition.registry.rsocket.AbstractRSocketClient;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.messaging.rsocket.RSocketRequester;

import java.util.Map;

import static com.bytechef.hermes.component.definition.TriggerDefinition.*;

/**
 * @author Ivica Cardic
 */
public class TriggerDefinitionFacadeRSocketClient extends AbstractRSocketClient implements TriggerDefinitionFacade {

    public TriggerDefinitionFacadeRSocketClient(
        DiscoveryClient discoveryClient, RSocketRequester.Builder rSocketRequesterBuilder) {

        super(discoveryClient, rSocketRequesterBuilder);
    }

    @Override
    public void executeDynamicWebhookDisable(
        String triggerName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> triggerParameters, String workflowExecutionId,
        DynamicWebhookEnableOutput output) {

        getRSocketRequester(componentName)
            .route("TriggerDefinitionFacade.executeDynamicWebhookDisable")
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
            .route("TriggerDefinitionFacade.executeDynamicWebhookEnable")
            .data(new DynamicWebhookEnable(
                authorizationName, componentName, componentVersion, connectionParameters, triggerName,
                triggerParameters, webhookUrl, workflowExecutionId))
            .retrieveMono(DynamicWebhookEnableOutput.class));
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookRefresh(
        String triggerName, String componentName, int componentVersion, DynamicWebhookEnableOutput output) {

        return MonoUtils.get(getRSocketRequester(componentName)
            .route("TriggerDefinitionFacade.executeDynamicWebhookRefresh")
            .data(new DynamicWebhookRefresh(componentName, componentVersion, output, triggerName))
            .retrieveMono(DynamicWebhookEnableOutput.class));
    }

    @Override
    public void executeListenerDisable(
        String triggerName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> triggerParameters, String workflowExecutionId) {

        getRSocketRequester(componentName)
            .route("TriggerDefinitionFacade.executeListenerDisable")
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
            .route("TriggerDefinitionFacade.executeListenerEnable")
            .data(new ListenerEnable(
                authorizationName, componentName, componentVersion, connectionParameters, triggerName,
                triggerParameters, workflowExecutionId))
            .send();
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

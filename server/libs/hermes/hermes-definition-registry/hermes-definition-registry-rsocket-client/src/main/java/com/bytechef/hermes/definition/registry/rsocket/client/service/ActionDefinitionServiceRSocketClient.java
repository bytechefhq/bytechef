
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
import com.bytechef.hermes.definition.registry.dto.ActionDefinitionDTO;
import com.bytechef.hermes.definition.registry.rsocket.client.AbstractRSocketClient;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionService;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ActionDefinitionServiceRSocketClient extends AbstractRSocketClient
    implements ActionDefinitionService {

    public ActionDefinitionServiceRSocketClient(
        DiscoveryClient discoveryClient, RSocketRequester.Builder rSocketRequesterBuilder) {

        super(discoveryClient, rSocketRequesterBuilder);
    }

    @Override
    public String executeEditorDescription(
        String actionName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> actionParameters) {

        return MonoUtils.get(getRSocketRequester(componentName)
            .route("ActionDefinitionService.executeEditorDescription")
            .data(new EditorDescription(
                actionName, actionParameters, authorizationName, componentName, componentVersion, connectionParameters))
            .retrieveMono(String.class));
    }

    @Override
    public List<Option<?>> executeOptions(
        String propertyName, String actionName, String componentName, int componentVersion,
        Map<String, Object> connectionParameters, String authorizationName, Map<String, Object> actionParameters) {

        return MonoUtils.get(getRSocketRequester(componentName)
            .route("ActionDefinitionService.executeOptions")
            .data(new Options(
                actionName, actionParameters, authorizationName, componentName, componentVersion, connectionParameters,
                propertyName))
            .retrieveMono(new ParameterizedTypeReference<>() {}));
    }

    @Override
    public List<? extends Property<?>> executeOutputSchema(
        String actionName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> actionParameters) {

        return MonoUtils.get(getRSocketRequester(componentName)
            .route("ActionDefinitionService.executeOutputSchema")
            .data(new OutputSchema(
                actionName, actionParameters, authorizationName, componentName, componentVersion, connectionParameters))
            .retrieveMono(new ParameterizedTypeReference<>() {}));
    }

    @Override
    public List<? extends Property<?>> executeProperties(
        String propertyName, String actionName, String componentName, int componentVersion,
        Map<String, Object> connectionParameters, String authorizationName, Map<String, Object> actionParameters) {

        return MonoUtils.get(getRSocketRequester(componentName)
            .route("ActionDefinitionService.executeProperties")
            .data(new Properties(
                actionName, actionParameters, authorizationName, componentName, componentVersion, connectionParameters,
                propertyName))
            .retrieveMono(new ParameterizedTypeReference<>() {}));
    }

    @Override
    public Object executeSampleOutput(
        String actionName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> actionParameters) {

        return MonoUtils.get(getRSocketRequester(componentName)
            .route("ActionDefinitionService.executeSampleOutput")
            .data(new SampleOutput(
                actionName, actionParameters, authorizationName, componentName, componentVersion, connectionParameters))
            .retrieveMono(new ParameterizedTypeReference<>() {}));
    }

    @Override
    public Mono<ActionDefinitionDTO> getComponentActionDefinitionMono(
        String actionName, String componentName, int componentVersion) {

        return getRSocketRequester(componentName)
            .route("ActionDefinitionService.getComponentActionDefinition")
            .data(
                Map.of("componentName", componentName, "componentVersion", componentVersion, "actionName", actionName))
            .retrieveMono(ActionDefinitionDTO.class);
    }

    @Override
    public Mono<List<ActionDefinitionDTO>> getComponentActionDefinitionsMono(
        String componentName, int componentVersion) {

        return getRSocketRequester(componentName)
            .route("ActionDefinitionService.getComponentActionDefinitions")
            .data(
                Map.of("componentName", componentName, "componentVersion", componentVersion))
            .retrieveMono(new ParameterizedTypeReference<>() {});
    }

    private record EditorDescription(
        String actionName, Map<String, Object> actionParameters, String authorizationName, String componentName,
        int componentVersion, Map<String, Object> connectionParameters) {
    }

    private record Options(
        String actionName, Map<String, Object> actionParameters, String authorizationName, String componentName,
        int componentVersion, Map<String, Object> connectionParameters, String propertyName) {
    }

    private record OutputSchema(
        String actionName, Map<String, Object> actionParameters, String authorizationName, String componentName,
        int componentVersion, Map<String, Object> connectionParameters) {
    }

    private record Properties(
        String actionName, Map<String, Object> actionParameters, String authorizationName, String componentName,
        int componentVersion, Map<String, Object> connectionParameters, String propertyName) {
    }

    private record SampleOutput(
        String actionName, Map<String, Object> actionParameters, String authorizationName, String componentName,
        int componentVersion, Map<String, Object> connectionParameters) {
    }
}

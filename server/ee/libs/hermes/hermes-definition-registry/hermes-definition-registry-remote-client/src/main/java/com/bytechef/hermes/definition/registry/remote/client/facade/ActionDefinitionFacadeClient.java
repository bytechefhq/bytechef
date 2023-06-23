
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

package com.bytechef.hermes.definition.registry.remote.client.facade;

import com.bytechef.hermes.definition.registry.dto.OptionDTO;
import com.bytechef.hermes.definition.registry.dto.ValuePropertyDTO;
import com.bytechef.hermes.definition.registry.facade.ActionDefinitionFacade;
import com.bytechef.hermes.definition.registry.remote.client.AbstractWorkerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ActionDefinitionFacadeClient extends AbstractWorkerClient implements ActionDefinitionFacade {

    public ActionDefinitionFacadeClient(DiscoveryClient discoveryClient, ObjectMapper objectMapper) {
        super(discoveryClient, objectMapper);
    }

    @Override
    public String executeEditorDescription(
        String componentName, int componentVersion, String actionName, Map<String, Object> actionParameters,
        Long connectionId) {

        return WORKER_WEB_CLIENT
            .post()
            .uri(
                uriBuilder -> toUri(uriBuilder, componentName, "/action-definition-service/execute-editor-description"))
            .bodyValue(
                new EditorDescriptionRequest(
                    actionName, actionParameters, componentName, componentVersion, connectionId))
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }

    @Override
    public List<OptionDTO> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, Object> actionParameters, Long connectionId, String searchText) {

        return WORKER_WEB_CLIENT
            .post()
            .uri(uriBuilder -> toUri(uriBuilder, componentName, "/action-definition-service/execute-options"))
            .bodyValue(
                new OptionsRequest(
                    actionName, propertyName, actionParameters, componentName, componentVersion, connectionId,
                    searchText))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<OptionDTO>>() {})
            .block();
    }

    @Override
    public List<? extends ValuePropertyDTO<?>> executeOutputSchema(
        String componentName, int componentVersion, String actionName, Map<String, Object> actionParameters,
        Long connectionId) {

        return WORKER_WEB_CLIENT
            .post()
            .uri(uriBuilder -> toUri(uriBuilder, componentName, "/action-definition-service/execute-output-schema"))
            .bodyValue(
                new OutputSchemaRequest(
                    actionName, actionParameters, componentName, componentVersion, connectionId))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<? extends ValuePropertyDTO<?>>>() {})
            .block();
    }

    @Override
    public List<? extends ValuePropertyDTO<?>> executeDynamicProperties(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, Object> actionParameters, Long connectionId) {

        return WORKER_WEB_CLIENT
            .post()
            .uri(uriBuilder -> toUri(uriBuilder, componentName, "/action-definition-service/execute-properties"))
            .bodyValue(
                new PropertiesRequest(
                    actionName, actionParameters, componentName, componentVersion, connectionId, propertyName))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<? extends ValuePropertyDTO<?>>>() {})
            .block();
    }

    @Override
    public Object executeSampleOutput(
        String actionName, String componentName, int componentVersion, Map<String, Object> actionParameters,
        Long connectionId) {

        return WORKER_WEB_CLIENT
            .post()
            .uri(uriBuilder -> toUri(uriBuilder, componentName, "/action-definition-service/execute-sample-output"))
            .bodyValue(
                new SampleOutputRequest(
                    actionName, actionParameters, componentName, componentVersion, connectionId))
            .retrieve()
            .bodyToMono(Object.class)
            .block();
    }

    private record EditorDescriptionRequest(
        String actionName, Map<String, Object> actionParameters, String componentName, int componentVersion,
        Long connectionId) {
    }

    private record OptionsRequest(
        String actionName, String propertyName, Map<String, Object> actionParameters, String componentName,
        int componentVersion, Long connectionId, String searchText) {
    }

    private record OutputSchemaRequest(
        String actionName, Map<String, Object> actionParameters, String componentName, int componentVersion,
        Long connectionId) {
    }

    private record PropertiesRequest(
        String actionName, Map<String, Object> actionParameters, String componentName, int componentVersion,
        Long connectionId, String propertyName) {
    }

    private record SampleOutputRequest(
        String actionName, Map<String, Object> actionParameters, String componentName, int componentVersion,
        Long connectionId) {
    }
}

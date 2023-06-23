
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

import com.bytechef.hermes.definition.registry.dto.ActionDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.OptionDTO;
import com.bytechef.hermes.definition.registry.dto.ComponentOperation;
import com.bytechef.hermes.definition.registry.dto.ValuePropertyDTO;
import com.bytechef.hermes.definition.registry.remote.client.AbstractWorkerClient;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ActionDefinitionServiceClient extends AbstractWorkerClient
    implements ActionDefinitionService {

    public ActionDefinitionServiceClient(DiscoveryClient discoveryClient, ObjectMapper objectMapper) {
        super(discoveryClient, objectMapper);
    }

    @Override
    public String executeEditorDescription(
        String componentName, int componentVersion, String actionName, Map<String, ?> actionParameters,
        String authorizationName, Map<String, ?> connectionParameters) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<OptionDTO> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> actionParameters, String authorizationName, Map<String, ?> connectionParameters,
        String searchText) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends ValuePropertyDTO<?>> executeOutputSchema(
        String componentName, int componentVersion, String actionName, Map<String, ?> actionParameters,
        String authorizationName, Map<String, ?> connectionParameters) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends ValuePropertyDTO<?>> executeDynamicProperties(
        int componentVersion, String componentName, String actionName, String propertyName,
        Map<String, ?> actionParameters, String authorizationName, Map<String, ?> connectionParameters) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object executeSampleOutput(
        String componentName, int componentVersion, String actionName, Map<String, ?> actionParameters,
        String authorizationName, Map<String, ?> connectionParameters) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ActionDefinitionDTO getActionDefinition(String componentName, int componentVersion, String actionName) {
        return WORKER_WEB_CLIENT
            .get()
            .uri(uriBuilder -> toUri(
                uriBuilder, componentName,
                "/action-definition-service/get-action-definition/{componentName}/{componentVersion}/{actionName}",
                componentName, componentVersion, actionName))
            .retrieve()
            .bodyToMono(ActionDefinitionDTO.class)
            .block();
    }

    @Override
    public List<ActionDefinitionDTO> getActionDefinitions(
        String componentName, int componentVersion) {

        return WORKER_WEB_CLIENT
            .get()
            .uri(uriBuilder -> toUri(
                uriBuilder, componentName,
                "/action-definition-service/get-action-definitions/{componentName}/{componentVersion}",
                componentName, componentVersion))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<ActionDefinitionDTO>>() {})
            .block();
    }

    @Override
    public List<ActionDefinitionDTO> getActionDefinitions(List<ComponentOperation> componentOperations) {
        // TODO implement this method

        throw new UnsupportedOperationException();
    }
}

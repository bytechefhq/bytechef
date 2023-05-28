
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

package com.bytechef.hermes.definition.registry.remote.web.rest.client.facade;

import com.bytechef.hermes.definition.registry.dto.OptionDTO;
import com.bytechef.hermes.definition.registry.dto.ValuePropertyDTO;
import com.bytechef.hermes.definition.registry.facade.ActionDefinitionFacade;
import com.bytechef.hermes.definition.registry.remote.web.rest.client.AbstractWorkerClient;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ActionDefinitionFacadeClient extends AbstractWorkerClient implements ActionDefinitionFacade {

    public ActionDefinitionFacadeClient(DiscoveryClient discoveryClient) {
        super(discoveryClient);
    }

    @Override
    public String executeEditorDescription(
        String actionName, String componentName, int componentVersion, Map<String, Object> actionParameters,
        long connectionId) {

        return WORKER_WEB_CLIENT
            .post()
            .uri(uriBuilder -> toUri(uriBuilder, componentName, "/action-definitions/editor-description"))
            .bodyValue(
                new EditorDescription(
                    actionName, actionParameters, componentName, componentVersion, connectionId))
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }

    @Override
    public List<OptionDTO> executeOptions(
        String propertyName, String actionName, String componentName, int componentVersion,
        Map<String, Object> actionParameters, long connectionId) {

        return WORKER_WEB_CLIENT
            .post()
            .uri(uriBuilder -> toUri(uriBuilder, componentName, "/action-definitions/options"))
            .bodyValue(
                new Options(
                    actionName, propertyName, actionParameters, componentName, componentVersion, connectionId))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<OptionDTO>>() {})
            .block();
    }

    @Override
    public List<? extends ValuePropertyDTO<?>> executeOutputSchema(
        String actionName, String componentName, int componentVersion, Map<String, Object> actionParameters,
        long connectionId) {

        return WORKER_WEB_CLIENT
            .post()
            .uri(uriBuilder -> toUri(uriBuilder, componentName, "/action-definitions/output-schema"))
            .bodyValue(
                new OutputSchema(
                    actionName, actionParameters, componentName, componentVersion, connectionId))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<? extends ValuePropertyDTO<?>>>() {})
            .block();
    }

    @Override
    public List<? extends ValuePropertyDTO<?>> executeDynamicProperties(
        String propertyName, String actionName, String componentName, int componentVersion,
        Map<String, Object> actionParameters, long connectionId) {

        return WORKER_WEB_CLIENT
            .post()
            .uri(uriBuilder -> toUri(uriBuilder, componentName, "/action-definitions/properties"))
            .bodyValue(
                new Properties(
                    actionName, actionParameters, componentName, componentVersion, connectionId, propertyName))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<? extends ValuePropertyDTO<?>>>() {})
            .block();
    }

    @Override
    public Object executeSampleOutput(
        String actionName, String componentName, int componentVersion, Map<String, Object> actionParameters,
        long connectionId) {

        return WORKER_WEB_CLIENT
            .post()
            .uri(uriBuilder -> toUri(uriBuilder, componentName, "/action-definitions/sample-output"))
            .bodyValue(
                new SampleOutput(
                    actionName, actionParameters, componentName, componentVersion, connectionId))
            .retrieve()
            .bodyToMono(Object.class)
            .block();
    }

    private record EditorDescription(
        String actionName, Map<String, Object> actionParameters, String componentName, int componentVersion,
        long connectionId) {
    }

    private record Options(
        String actionName, String propertyName, Map<String, Object> actionParameters, String componentName,
        int componentVersion, long connectionId) {
    }

    private record OutputSchema(
        String actionName, Map<String, Object> actionParameters, String componentName, int componentVersion,
        long connectionId) {
    }

    private record Properties(
        String actionName, Map<String, Object> actionParameters, String componentName, int componentVersion,
        long connectionId, String propertyName) {
    }

    private record SampleOutput(
        String actionName, Map<String, Object> actionParameters, String componentName, int componentVersion,
        long connectionId) {
    }
}

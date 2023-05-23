
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

package com.bytechef.hermes.definition.registry.rsocket.client.facade;

import com.bytechef.commons.reactor.util.MonoUtils;
import com.bytechef.hermes.definition.registry.dto.OptionDTO;
import com.bytechef.hermes.definition.registry.dto.ValuePropertyDTO;
import com.bytechef.hermes.definition.registry.facade.ActionDefinitionFacade;
import com.bytechef.hermes.definition.registry.rsocket.client.AbstractRSocketClient;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ActionDefinitionFacadeRSocketClient extends AbstractRSocketClient implements ActionDefinitionFacade {

    public ActionDefinitionFacadeRSocketClient(
        DiscoveryClient discoveryClient, RSocketRequester.Builder rSocketRequesterBuilder) {

        super(discoveryClient, rSocketRequesterBuilder);
    }

    @Override
    public String executeEditorDescription(
        String actionName, String componentName, int componentVersion, Map<String, Object> actionParameters,
        long connectionId) {

        return MonoUtils.get(
            getRSocketRequester(componentName)
                .route("ActionDefinitionFacade.executeEditorDescription")
                .data(new EditorDescription(
                    actionName, actionParameters, componentName, componentVersion, connectionId))
                .retrieveMono(String.class));
    }

    @Override
    public List<OptionDTO> executeOptions(
        String propertyName, String actionName, String componentName, int componentVersion,
        Map<String, Object> actionParameters, long connectionId) {

        return MonoUtils.get(
            getRSocketRequester(componentName)
                .route("ActionDefinitionFacade.executeOptions")
                .data(new Options(
                    actionName, actionParameters, componentName, componentVersion, connectionId,
                    propertyName))
                .retrieveMono(new ParameterizedTypeReference<>() {}));
    }

    @Override
    public List<? extends ValuePropertyDTO<?>> executeOutputSchema(
        String actionName, String componentName, int componentVersion, Map<String, Object> actionParameters,
        long connectionId) {

        return MonoUtils.get(
            getRSocketRequester(componentName)
                .route("ActionDefinitionFacade.executeOutputSchema")
                .data(new OutputSchema(
                    actionName, actionParameters, componentName, componentVersion, connectionId))
                .retrieveMono(new ParameterizedTypeReference<>() {}));
    }

    @Override
    public List<? extends ValuePropertyDTO<?>> executeDynamicProperties(
        String propertyName, String actionName, String componentName, int componentVersion,
        Map<String, Object> actionParameters, long connectionId) {

        return MonoUtils.get(
            getRSocketRequester(componentName)
                .route("ActionDefinitionFacade.executeProperties")
                .data(new Properties(
                    actionName, actionParameters, componentName, componentVersion, connectionId,
                    propertyName))
                .retrieveMono(new ParameterizedTypeReference<>() {}));
    }

    @Override
    public Object executeSampleOutput(
        String actionName, String componentName, int componentVersion, Map<String, Object> actionParameters,
        long connectionId) {

        return MonoUtils.get(
            getRSocketRequester(componentName)
                .route("ActionDefinitionFacade.executeSampleOutput")
                .data(new SampleOutput(
                    actionName, actionParameters, componentName, componentVersion, connectionId))
                .retrieveMono(new ParameterizedTypeReference<>() {}));
    }

    private record EditorDescription(
        String actionName, Map<String, Object> actionParameters, String componentName, int componentVersion,
        long connectionId) {
    }

    private record Options(
        String actionName, Map<String, Object> actionParameters, String componentName, int componentVersion,
        long connectionId, String propertyName) {
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


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
import com.bytechef.hermes.definition.Option;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.registry.facade.TriggerDefinitionFacade;
import com.bytechef.hermes.definition.registry.rsocket.client.AbstractRSocketClient;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester.Builder;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class TriggerDefinitionFacadeRSocketClient extends AbstractRSocketClient implements TriggerDefinitionFacade {

    public TriggerDefinitionFacadeRSocketClient(DiscoveryClient discoveryClient, Builder rSocketRequesterBuilder) {
        super(discoveryClient, rSocketRequesterBuilder);
    }

    @Override
    public String executeEditorDescription(
        String triggerName, String componentName, int componentVersion, Map<String, Object> triggerParameters,
        long connectionId) {

        return MonoUtils.get(
            getRSocketRequester(componentName)
                .route("TriggerDefinitionFacade.executeEditorDescription")
                .data(new EditorDescription(
                    componentName, componentVersion, connectionId, triggerName, triggerParameters))
                .retrieveMono(String.class));
    }

    @Override
    public List<Option<?>> executeOptions(
        String propertyName, String triggerName, String componentName, int componentVersion,
        Map<String, Object> triggerParameters, long connectionId) {

        return MonoUtils.get(
            getRSocketRequester(componentName)
                .route("TriggerDefinitionFacade.executeOptions")
                .data(new Options(
                    componentName, componentVersion, connectionId, propertyName, triggerName, triggerParameters))
                .retrieveMono(new ParameterizedTypeReference<>() {}));
    }

    @Override
    public List<? extends Property<?>> executeOutputSchema(
        String triggerName, String componentName, int componentVersion, Map<String, Object> triggerParameters,
        long connectionId) {

        return MonoUtils.get(
            getRSocketRequester(componentName)
                .route("TriggerDefinitionFacade.executeOutputSchema")
                .data(new OutputSchema(
                    componentName, componentVersion, connectionId, triggerName, triggerParameters))
                .retrieveMono(new ParameterizedTypeReference<>() {}));
    }

    @Override
    public List<? extends Property<?>> executeDynamicProperties(
        String propertyName, String triggerName, String componentName, int componentVersion,
        Map<String, Object> triggerParameters, long connectionId) {

        return MonoUtils.get(
            getRSocketRequester(componentName)
                .route("TriggerDefinitionFacade.executeProperties")
                .data(new Properties(
                    componentName, componentVersion, connectionId, propertyName, triggerName, triggerParameters))
                .retrieveMono(new ParameterizedTypeReference<>() {}));
    }

    @Override
    public Object executeSampleOutput(
        String triggerName, String componentName, int componentVersion, Map<String, Object> triggerParameters,
        long connectionId) {

        return MonoUtils.get(
            getRSocketRequester(componentName)
                .route("TriggerDefinitionFacade.executeSampleOutput")
                .data(new SampleOutput(
                    componentName, componentVersion, connectionId, triggerName, triggerParameters))
                .retrieveMono(new ParameterizedTypeReference<>() {}));
    }

    private record EditorDescription(
        String componentName, int componentVersion, long connectionId, String triggerName,
        Map<String, Object> triggerParameters) {
    }

    private record Options(
        String componentName, int componentVersion, long connectionId, String propertyName, String triggerName,
        Map<String, Object> triggerParameters) {
    }

    private record OutputSchema(
        String componentName, int componentVersion, long connectionId, String triggerName,
        Map<String, Object> triggerParameters) {

    }

    private record Properties(
        String componentName, int componentVersion, long connectionId, String propertyName, String triggerName,
        Map<String, Object> triggerParameters) {
    }

    private record SampleOutput(
        String componentName, int componentVersion, long connectionId, String triggerName,
        Map<String, Object> triggerParameters) {
    }
}


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

import com.bytechef.commons.util.DiscoveryUtils;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.definition.registry.facade.ComponentDefinitionFacade;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class ComponentDefinitionFacadeRSocketClient implements ComponentDefinitionFacade {

    private final DiscoveryClient discoveryClient;
    private final RSocketRequester.Builder rSocketRequesterBuilder;

    public ComponentDefinitionFacadeRSocketClient(
        DiscoveryClient discoveryClient, RSocketRequester.Builder rSocketRequesterBuilder) {

        this.discoveryClient = discoveryClient;
        this.rSocketRequesterBuilder = rSocketRequesterBuilder;
    }

    @Override
    public Mono<List<ComponentDefinition>> getComponentDefinitions(
        Boolean connectionDefinitions, Boolean connectionInstances) {

        return Mono.zip(
            DiscoveryUtils.filterServiceInstances(discoveryClient.getInstances("worker-service-app"))
                .stream()
                .map(serviceInstance -> rSocketRequesterBuilder
                    .websocket(DiscoveryUtils.toWebSocketUri(serviceInstance))
                    .route("ComponentDefinitionFacade.getComponentDefinitions")
                    .data(new HashMap<>() {
                        {
                            put("connectionDefinitions", connectionDefinitions);
                            put("connectionInstances", connectionInstances);
                        }
                    })
                    .retrieveMono(new ParameterizedTypeReference<List<ComponentDefinition>>() {}))
                .toList(),
            this::toComponentDefinitions);
    }

    @SuppressWarnings("unchecked")
    private List<ComponentDefinition> toComponentDefinitions(Object[] objectArray) {
        return Arrays.stream(objectArray)
            .map(object -> (List<ComponentDefinition>) object)
            .flatMap(Collection::stream)
            .toList();
    }
}

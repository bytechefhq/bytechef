
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

import com.bytechef.commons.discovery.util.DiscoveryUtils;
import com.bytechef.commons.reactor.util.MonoUtils;
import com.bytechef.commons.rsocket.util.RSocketUtils;
import com.bytechef.hermes.definition.registry.dto.ComponentDefinitionDTO;
import com.bytechef.hermes.definition.registry.rsocket.client.AbstractRSocketClient;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ComponentDefinitionServiceRSocketClient extends AbstractRSocketClient
    implements ComponentDefinitionService {

    public ComponentDefinitionServiceRSocketClient(
        DiscoveryClient discoveryClient, RSocketRequester.Builder rSocketRequesterBuilder) {

        super(discoveryClient, rSocketRequesterBuilder);
    }

    @Override
    public ComponentDefinitionDTO getComponentDefinition(String name, Integer version) {
        return MonoUtils.get(
            getRSocketRequester(name)
                .route("ComponentDefinitionService.getComponentDefinition")
                .data(Map.of("name", name, "version", version))
                .retrieveMono(ComponentDefinitionDTO.class));
    }

    @Override
    public List<ComponentDefinitionDTO> getComponentDefinitions() {
        return MonoUtils.get(
            Mono.zip(
                DiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_SERVICE_APP))
                    .stream()
                    .map(serviceInstance -> RSocketUtils.getRSocketRequester(serviceInstance, rSocketRequesterBuilder)
                        .route("ComponentDefinitionService.getComponentDefinitions")
                        .retrieveMono(new ParameterizedTypeReference<List<ComponentDefinitionDTO>>() {}))
                    .toList(),
                this::toComponentDefinitions));
    }

    @Override
    public List<ComponentDefinitionDTO> getComponentDefinitions(String name) {
        return MonoUtils.get(
            Mono.zip(
                DiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_SERVICE_APP))
                    .stream()
                    .map(serviceInstance -> RSocketUtils.getRSocketRequester(serviceInstance, rSocketRequesterBuilder)
                        .route("ComponentDefinitionService.getComponentDefinitionsForName")
                        .data(name)
                        .retrieveMono(new ParameterizedTypeReference<List<ComponentDefinitionDTO>>() {}))
                    .toList(),
                this::toComponentDefinitions));
    }

    @SuppressWarnings("unchecked")
    private List<ComponentDefinitionDTO> toComponentDefinitions(Object[] objectArray) {
        return Arrays.stream(objectArray)
            .map(object -> (List<ComponentDefinitionDTO>) object)
            .flatMap(Collection::stream)
            .toList();
    }
}

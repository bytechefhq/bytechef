
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

package com.bytechef.hermes.definition.registry.service.web.rest.client.service;

import com.bytechef.commons.discovery.util.WorkerDiscoveryUtils;
import com.bytechef.hermes.definition.registry.dto.ComponentDefinitionDTO;
import com.bytechef.hermes.definition.registry.service.web.rest.client.AbstractWorkerClient;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class ComponentDefinitionServiceClient extends AbstractWorkerClient
    implements ComponentDefinitionService {

    public ComponentDefinitionServiceClient(DiscoveryClient discoveryClient) {
        super(discoveryClient);
    }

    @Override
    public ComponentDefinitionDTO getComponentDefinition(String name, Integer version) {
        return WORKER_WEB_CLIENT
            .get()
            .uri(uriBuilder -> toUri(
                uriBuilder, name, "/component-definitions/{name}/{version}", name, version == null ? 1 : version))
            .retrieve()
            .bodyToMono(ComponentDefinitionDTO.class)
            .block();
    }

    @Override
    public List<ComponentDefinitionDTO> getComponentDefinitions() {
        return Mono.zip(
            WorkerDiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_SERVICE_APP))
                .stream()
                .map(serviceInstance -> WORKER_WEB_CLIENT
                    .get()
                    .uri(uriBuilder -> toUri(uriBuilder, serviceInstance, "/component-definitions"))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<ComponentDefinitionDTO>>() {}))
                .toList(),
            this::toComponentDefinitions)
            .block();
    }

    @Override
    public List<ComponentDefinitionDTO> getComponentDefinitions(String name) {
        return Mono.zip(
            WorkerDiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_SERVICE_APP))
                .stream()
                .map(serviceInstance -> WORKER_WEB_CLIENT
                    .get()
                    .uri(uriBuilder -> toUri(uriBuilder, serviceInstance, "/component-definitions/{name}", name))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<ComponentDefinitionDTO>>() {}))
                .toList(),
            this::toComponentDefinitions)
            .block();
    }

    @SuppressWarnings("unchecked")
    private List<ComponentDefinitionDTO> toComponentDefinitions(Object[] objectArray) {
        return Arrays.stream(objectArray)
            .map(object -> (List<ComponentDefinitionDTO>) object)
            .flatMap(Collection::stream)
            .toList();
    }
}

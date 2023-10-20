
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

import com.bytechef.commons.discovery.util.WorkerDiscoveryUtils;
import com.bytechef.commons.webclient.DefaultWebClient;
import com.bytechef.hermes.definition.registry.dto.ComponentDefinitionDTO;
import com.bytechef.hermes.definition.registry.remote.client.AbstractWorkerClient;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public ComponentDefinitionServiceClient(
        DefaultWebClient defaultWebClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultWebClient, discoveryClient, objectMapper);
    }

    @Override
    public ComponentDefinitionDTO getComponentDefinition(String name, Integer version) {
        return defaultWebClient.get(
            uriBuilder -> toUri(
                uriBuilder, name, "/component-definition-service/get-component-definition/{name}/{version}", name,
                checkVersion(version)),
            ComponentDefinitionDTO.class);
    }

    @Override
    public List<ComponentDefinitionDTO> getComponentDefinitions() {
        return Mono.zip(
            WorkerDiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_SERVICE_APP), objectMapper)
                .stream()
                .map(serviceInstance -> defaultWebClient.getMono(
                    uriBuilder -> toUri(uriBuilder, serviceInstance,
                        "/component-definition-service/get-component-definitions"),
                    new ParameterizedTypeReference<List<ComponentDefinitionDTO>>() {}))
                .toList(),
            this::toComponentDefinitions)
            .block();
    }

    @Override
    public List<ComponentDefinitionDTO> getComponentDefinitions(String name) {
        return Mono.zip(
            WorkerDiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_SERVICE_APP), objectMapper)
                .stream()
                .map(serviceInstance -> defaultWebClient.getMono(
                    uriBuilder -> toUri(uriBuilder, serviceInstance,
                        "/component-definition-service/get-component-definitions/{name}", name),
                    new ParameterizedTypeReference<List<ComponentDefinitionDTO>>() {}))
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

    private static int checkVersion(Integer version) {
        if (version == null) {
            version = 1;
        }

        return version;
    }
}

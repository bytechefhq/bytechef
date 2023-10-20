
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

import com.bytechef.commons.discovery.util.WorkerDiscoveryUtils;
import com.bytechef.commons.webclient.DefaultWebClient;
import com.bytechef.hermes.definition.registry.domain.ComponentDefinition;
import com.bytechef.hermes.definition.registry.facade.ComponentDefinitionFacade;
import com.bytechef.hermes.definition.registry.remote.client.AbstractWorkerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.LinkedMultiValueMap;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ComponentDefinitionFacadeClient extends AbstractWorkerClient implements ComponentDefinitionFacade {

    public ComponentDefinitionFacadeClient(
        DefaultWebClient defaultWebClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultWebClient, discoveryClient, objectMapper);
    }

    @Override
    public List<ComponentDefinition> getComponentDefinitions(
        Boolean actionDefinitions, Boolean connectionDefinitions, Boolean connectionInstances,
        Boolean triggerDefinitions, List<String> include) {

        return Mono.zip(
            WorkerDiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_SERVICE_APP), objectMapper)
                .stream()
                .map(serviceInstance -> defaultWebClient.getMono(
                    uriBuilder -> toUri(
                        uriBuilder, serviceInstance, "/component-definition-facade/get-components", Map.of(),
                        new LinkedMultiValueMap<>() {
                            {
                                if (actionDefinitions != null) {
                                    put("actionDefinitions", List.of(actionDefinitions.toString()));
                                }

                                if (connectionDefinitions != null) {
                                    put("connectionDefinitions", List.of(connectionDefinitions.toString()));
                                }

                                if (connectionInstances != null) {
                                    put("connectionInstances", List.of(connectionInstances.toString()));
                                }

                                if (triggerDefinitions != null) {
                                    put("triggerDefinitions", List.of(triggerDefinitions.toString()));
                                }
                            }
                        }),
                    new ParameterizedTypeReference<List<ComponentDefinition>>() {}))
                .toList(),
            this::toComponentDefinitions)
            .block();
    }

    @SuppressWarnings("unchecked")
    private List<ComponentDefinition> toComponentDefinitions(Object[] objectArray) {
        return Arrays.stream(objectArray)
            .map(object -> (List<ComponentDefinition>) object)
            .flatMap(Collection::stream)
            .distinct()
            .toList();
    }
}


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

import com.bytechef.commons.discovery.util.WorkerDiscoveryUtils;
import com.bytechef.hermes.definition.registry.dto.ComponentDefinitionDTO;
import com.bytechef.hermes.definition.registry.facade.ComponentDefinitionFacade;
import com.bytechef.hermes.definition.registry.remote.web.rest.client.AbstractWorkerClient;
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

    public ComponentDefinitionFacadeClient(DiscoveryClient discoveryClient) {
        super(discoveryClient);
    }

    @Override
    public List<ComponentDefinitionDTO> searchComponentDefinitions(
        Boolean actionDefinitions, Boolean connectionDefinitions, Boolean connectionInstances,
        Boolean triggerDefinitions) {

        return Mono.zip(
            WorkerDiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_SERVICE_APP))
                .stream()
                .map(serviceInstance -> WORKER_WEB_CLIENT
                    .get()
                    .uri(uriBuilder -> toUri(
                        uriBuilder, serviceInstance, "/component-definitions/search", Map.of(),
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
                        }))
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
            .distinct()
            .toList();
    }
}

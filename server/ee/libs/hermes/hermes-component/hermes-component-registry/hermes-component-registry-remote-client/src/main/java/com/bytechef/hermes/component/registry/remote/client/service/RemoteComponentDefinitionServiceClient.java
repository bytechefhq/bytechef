
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

package com.bytechef.hermes.component.registry.remote.client.service;

import com.bytechef.commons.discovery.util.WorkerDiscoveryUtils;
import com.bytechef.commons.webclient.DefaultWebClient;
import com.bytechef.hermes.component.registry.domain.ComponentDefinition;
import com.bytechef.hermes.component.registry.remote.client.AbstractWorkerClient;
import com.bytechef.hermes.component.registry.service.RemoteComponentDefinitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnMissingClass(value = "com.bytechef.worker.WorkerApplication")
public class RemoteComponentDefinitionServiceClient extends AbstractWorkerClient
    implements RemoteComponentDefinitionService {

    private static final String COMPONENT_DEFINITION_SERVICE = "/component-definition-service";

    public RemoteComponentDefinitionServiceClient(
        DefaultWebClient defaultWebClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultWebClient, discoveryClient, objectMapper);
    }

    @Override
    public ComponentDefinition getComponentDefinition(String name, Integer version) {
        return defaultWebClient.get(
            uriBuilder -> toUri(
                uriBuilder, name, COMPONENT_DEFINITION_SERVICE + "/get-component-definition/{name}/{version}", name,
                checkVersion(version)),
            ComponentDefinition.class);
    }

    @Override
    public List<ComponentDefinition> getComponentDefinitions() {
        return Mono.zip(
            WorkerDiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_APP), objectMapper)
                .stream()
                .map(serviceInstance -> defaultWebClient.getMono(
                    uriBuilder -> toUri(uriBuilder, serviceInstance,
                        COMPONENT_DEFINITION_SERVICE + "/get-component-definitions"),
                    new ParameterizedTypeReference<List<ComponentDefinition>>() {}))
                .toList(),
            this::toComponentDefinitions)
            .block();
    }

    @Override
    public List<ComponentDefinition> getComponentDefinitionVersions(String name) {
        return Mono.zip(
            WorkerDiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_APP), objectMapper)
                .stream()
                .map(serviceInstance -> defaultWebClient.getMono(
                    uriBuilder -> toUri(uriBuilder, serviceInstance,
                        COMPONENT_DEFINITION_SERVICE + "/get-component-definition-versions/{name}", name),
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

    private static int checkVersion(Integer version) {
        if (version == null) {
            version = 1;
        }

        return version;
    }
}

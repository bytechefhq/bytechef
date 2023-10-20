
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

package com.bytechef.hermes.component.service.remote;

import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.component.service.ComponentDefinitionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author Ivica Cardic
 */
@Component
public class ProxyComponentDefinitionService implements ComponentDefinitionService {

    public static final WebClient WEB_CLIENT = WebClient.builder()
        .build();

    private final DiscoveryClient discoveryClient;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public ProxyComponentDefinitionService(DiscoveryClient discoveryClient, ObjectMapper objectMapper) {
        this.discoveryClient = discoveryClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Flux<ComponentDefinition> getComponentDefinitions() {
        return Flux.fromIterable(filterServiceInstances(discoveryClient.getInstances("worker-service-app")))
            .flatMap(serviceInstance -> WEB_CLIENT.get()
                .uri(toComponentDefinitionsUri(serviceInstance))
                .retrieve()
                .bodyToFlux(ComponentDefinition.class))
            .distinct(ComponentDefinition::getName);
    }

    @Override
    public Mono<ComponentDefinition> getComponentDefinition(String name) {
        return WEB_CLIENT.get()
            .uri(
                toComponentDefinitionUri(
                    filterServiceInstance(discoveryClient.getInstances("worker-service-app"), name)),
                Map.of("name", name))
            .retrieve()
            .bodyToMono(ComponentDefinition.class);
    }

    @Override
    public Mono<ActionDefinition> getComponentDefinitionAction(String componentName, String actionName) {
        return WEB_CLIENT.get()
            .uri(
                toComponentDefinitionActionUri(
                    filterServiceInstance(discoveryClient.getInstances("worker-service-app"), componentName)),
                Map.of("componentName", componentName, "actionName", actionName))
            .retrieve()
            .bodyToMono(ActionDefinition.class);
    }

    @Override
    public Flux<ConnectionDefinition> getConnectionDefinitions() {
        return Flux.fromIterable(filterServiceInstances(discoveryClient.getInstances("worker-service-app")))
            .flatMap(serviceInstance -> WEB_CLIENT.get()
                .uri(toComponentDefinitionsUri(serviceInstance))
                .retrieve()
                .bodyToFlux(ComponentDefinition.class))
            .distinct(ComponentDefinition::getName)
            .map(ComponentDefinition::getConnection);
    }

    private ServiceInstance filterServiceInstance(List<ServiceInstance> serviceInstances, String componentName) {
        for (ServiceInstance serviceInstance : serviceInstances) {
            Map<String, String> metadataMap = serviceInstance.getMetadata();

            List<String> componentNames = getComponentNames(metadataMap);

            for (String curComponentName : componentNames) {
                if (curComponentName.equalsIgnoreCase(componentName)) {
                    return serviceInstance;
                }
            }
        }

        throw new IllegalStateException("None od worker instances contains component %s ".formatted(componentName));
    }

    private List<String> getComponentNames(Map<String, String> metadataMap) {
        List<Map<String, Object>> components;

        try {
            components = objectMapper.readValue(metadataMap.get("components"), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return components.stream()
            .map(component -> (String) component.get("name"))
            .toList();
    }

    private Set<ServiceInstance> filterServiceInstances(List<ServiceInstance> serviceInstances) {
        Set<String> componentNameInstanceIds = toUniqueInstanceIds(serviceInstances);

        return componentNameInstanceIds.stream()
            .map(instanceId -> serviceInstances.stream()
                .filter(serviceInstance -> Objects.equals(serviceInstance.getInstanceId(), instanceId))
                .findFirst()
                .orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private Set<String> toUniqueInstanceIds(List<ServiceInstance> serviceInstances) {
        Map<String, Set<String>> componentNameInstanceIds = new HashMap<>();

        for (ServiceInstance serviceInstance : serviceInstances) {
            Map<String, String> metadataMap = serviceInstance.getMetadata();

            List<String> componentNames = getComponentNames(metadataMap);

            for (String componentName : componentNames) {
                componentNameInstanceIds.compute(componentName, (key, instanceIds) -> {
                    if (instanceIds == null) {
                        instanceIds = new HashSet<>();
                    }

                    instanceIds.add(serviceInstance.getInstanceId());

                    return instanceIds;
                });
            }
        }

        return componentNameInstanceIds.values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }

    private String toComponentDefinitionActionUri(ServiceInstance serviceInstance) {
        return "http://%s:%s/api/definitions/components/{componentName}/actions/{actionName}".formatted(
            serviceInstance.getHost(), serviceInstance.getPort());
    }

    private String toComponentDefinitionUri(ServiceInstance serviceInstance) {
        return "http://%s:%s/api/definitions/components/{name}".formatted(
            serviceInstance.getHost(), serviceInstance.getPort());
    }

    private String toComponentDefinitionsUri(ServiceInstance serviceInstance) {
        return "http://%s:%s/api/definitions/components".formatted(
            serviceInstance.getHost(), serviceInstance.getPort());
    }
}

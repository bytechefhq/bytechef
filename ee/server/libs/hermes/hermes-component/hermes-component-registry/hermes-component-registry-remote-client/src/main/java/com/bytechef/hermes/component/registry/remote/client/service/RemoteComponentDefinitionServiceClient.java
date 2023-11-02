/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.component.registry.remote.client.service;

import com.bytechef.commons.discovery.util.WorkerDiscoveryUtils;
import com.bytechef.commons.webclient.DefaultWebClient;
import com.bytechef.hermes.component.registry.domain.ComponentDefinition;
import com.bytechef.hermes.component.registry.remote.client.AbstractWorkerClient;
import com.bytechef.hermes.component.registry.service.ComponentDefinitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import reactor.core.publisher.Mono;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnMissingClass(value = "com.bytechef.worker.WorkerApplication")
public class RemoteComponentDefinitionServiceClient extends AbstractWorkerClient
    implements ComponentDefinitionService {

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
    public List<ComponentDefinition> getComponentDefinitions(
        Boolean actionDefinitions, Boolean connectionDefinitions,
        Boolean triggerDefinitions, List<String> include) {

        return Mono.zip(
            WorkerDiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_APP), objectMapper)
                .stream()
                .map(serviceInstance -> defaultWebClient.getMono(
                    uriBuilder -> toUri(
                        uriBuilder, serviceInstance, COMPONENT_DEFINITION_SERVICE + "/get-component-definitions",
                        Map.of(),
                        getQueryParams(actionDefinitions, connectionDefinitions, triggerDefinitions)),
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

    private static int checkVersion(Integer version) {
        if (version == null) {
            version = 1;
        }

        return version;
    }

    private static LinkedMultiValueMap<String, String> getQueryParams(
        Boolean actionDefinitions, Boolean connectionDefinitions, Boolean triggerDefinitions) {

        LinkedMultiValueMap<String, String> queryParamsMap = new LinkedMultiValueMap<>();

        if (actionDefinitions != null) {
            queryParamsMap.put("actionDefinitions", List.of(actionDefinitions.toString()));
        }

        if (connectionDefinitions != null) {
            queryParamsMap.put("connectionDefinitions", List.of(connectionDefinitions.toString()));
        }

        if (triggerDefinitions != null) {
            queryParamsMap.put("triggerDefinitions", List.of(triggerDefinitions.toString()));
        }

        return queryParamsMap;
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

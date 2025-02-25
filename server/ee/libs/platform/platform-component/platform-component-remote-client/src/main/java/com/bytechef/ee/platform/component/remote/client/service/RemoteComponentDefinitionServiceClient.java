/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.definition.DataStreamItemReader;
import com.bytechef.component.definition.DataStreamItemWriter;
import com.bytechef.component.definition.UnifiedApiDefinition.Category;
import com.bytechef.component.definition.UnifiedApiDefinition.ModelType;
import com.bytechef.component.definition.unified.base.adapter.ProviderModelAdapter;
import com.bytechef.component.definition.unified.base.mapper.ProviderModelMapper;
import com.bytechef.component.definition.unified.base.model.ProviderInputModel;
import com.bytechef.component.definition.unified.base.model.ProviderOutputModel;
import com.bytechef.component.definition.unified.base.model.UnifiedInputModel;
import com.bytechef.component.definition.unified.base.model.UnifiedOutputModel;
import com.bytechef.ee.discovery.util.WorkerDiscoveryUtils;
import com.bytechef.ee.platform.component.remote.client.AbstractWorkerClient;
import com.bytechef.ee.remote.client.DefaultRestClient;
import com.bytechef.platform.component.definition.DataStreamComponentDefinition.ComponentType;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteComponentDefinitionServiceClient extends AbstractWorkerClient
    implements ComponentDefinitionService {

    private static final String COMPONENT_DEFINITION_SERVICE = "/component-definition-service";

    public RemoteComponentDefinitionServiceClient(
        DefaultRestClient defaultRestClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultRestClient, discoveryClient, objectMapper);
    }

    @Override
    public Optional<ComponentDefinition> fetchComponentDefinition(String name, Integer version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ComponentDefinition getComponentDefinition(String name, Integer version) {
        return defaultRestClient.get(
            uriBuilder -> toUri(
                uriBuilder, name, COMPONENT_DEFINITION_SERVICE + "/get-component-definition/{name}/{version}", name,
                checkVersion(version)),
            ComponentDefinition.class);
    }

    @Override
    public List<ComponentDefinition> getComponentDefinitions() {
        List<CompletableFuture<List<ComponentDefinition>>> completableFutures = CollectionUtils.map(
            WorkerDiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_APP)),
            serviceInstance -> CompletableFuture.supplyAsync(() -> defaultRestClient.get(
                uriBuilder -> toUri(
                    uriBuilder, serviceInstance, COMPONENT_DEFINITION_SERVICE + "/get-component-definitions"),
                new ParameterizedTypeReference<>() {})));

        return getComponentDefinitions(completableFutures);
    }

    @Override
    public List<ComponentDefinition> getComponentDefinitions(
        Boolean actionDefinitions, Boolean connectionDefinitions,
        Boolean triggerDefinitions, List<String> include) {

        List<CompletableFuture<List<ComponentDefinition>>> completableFutures = CollectionUtils.map(
            WorkerDiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_APP)),
            serviceInstance -> CompletableFuture.supplyAsync(() -> defaultRestClient.get(
                uriBuilder -> toUri(
                    uriBuilder, serviceInstance, COMPONENT_DEFINITION_SERVICE + "/get-component-definitions",
                    Map.of(), getQueryParams(actionDefinitions, connectionDefinitions, triggerDefinitions)),
                new ParameterizedTypeReference<>() {})));

        return getComponentDefinitions(completableFutures);

    }

    @Override
    public List<ComponentDefinition> getComponentDefinitionVersions(String name) {
        List<CompletableFuture<List<ComponentDefinition>>> completableFutures = CollectionUtils.map(
            WorkerDiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_APP)),
            serviceInstance -> CompletableFuture.supplyAsync(() -> defaultRestClient.get(
                uriBuilder -> toUri(
                    uriBuilder, serviceInstance,
                    COMPONENT_DEFINITION_SERVICE + "/get-component-definition-versions/{name}", name),
                new ParameterizedTypeReference<>() {})));

        return getComponentDefinitions(completableFutures);
    }

    @Override
    public ComponentDefinition getConnectionComponentDefinition(String name, int connectionVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ComponentDefinition> getDataStreamComponentDefinitions(ComponentType componentType) {
        // TODO

        throw new UnsupportedOperationException();
    }

    @Override
    public DataStreamItemReader getDataStreamItemReader(String componentName, int componentVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataStreamItemWriter getDataStreamItemWriter(String componentName, int componentVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ComponentDefinition> getUnifiedApiComponentDefinitions(Category category) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProviderModelAdapter<? super ProviderInputModel, ? extends ProviderOutputModel>
        getUnifiedApiProviderModelAdapter(
            String componentName, Category category, ModelType modelType) {

        throw new UnsupportedOperationException();
    }

    @Override
    public
        ProviderModelMapper<? super UnifiedInputModel, ? extends UnifiedOutputModel, ? extends ProviderInputModel, ? super ProviderOutputModel>
        getUnifiedApiProviderModelMapper(
            String componentName, Category category, ModelType modelTyp) {

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasComponentDefinition(String name, Integer version) {
        throw new UnsupportedOperationException();
    }

    private static int checkVersion(Integer version) {
        if (version == null) {
            version = 1;
        }

        return version;
    }

    private static List<ComponentDefinition> getComponentDefinitions(
        List<CompletableFuture<List<ComponentDefinition>>> completableFutures) {

        CompletableFuture
            .allOf(completableFutures.toArray(new CompletableFuture[0]))
            .join();

        return completableFutures
            .stream()
            .map(CompletableFuture::join)
            .flatMap(CollectionUtils::stream)
            .collect(Collectors.toList());
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
}

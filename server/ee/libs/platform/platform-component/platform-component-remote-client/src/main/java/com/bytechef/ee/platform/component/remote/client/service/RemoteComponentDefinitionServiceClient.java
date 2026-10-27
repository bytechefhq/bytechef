/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.service;

import com.bytechef.ee.platform.component.remote.client.WorkerInstances;
import com.bytechef.ee.platform.component.remote.grpc.ComponentProtoMapper;
import com.bytechef.ee.platform.component.remote.grpc.proto.BoolResponse;
import com.bytechef.ee.platform.component.remote.grpc.proto.ComponentDefinitionRemoteServiceGrpc;
import com.bytechef.ee.platform.component.remote.grpc.proto.ComponentDefinitionRemoteServiceGrpc.ComponentDefinitionRemoteServiceBlockingStub;
import com.bytechef.ee.platform.component.remote.grpc.proto.ComponentDefinitionResult;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetComponentDefinitionRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetComponentDefinitionVersionsRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetComponentDefinitionsRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetConnectionComponentDefinitionRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteComponentDefinition;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteComponentDefinitions;
import com.bytechef.ee.platform.component.remote.grpc.proto.SearchComponentDefinitionsRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.filter.ComponentDefinitionFilter;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.constant.PlatformType;
import io.grpc.StatusRuntimeException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteComponentDefinitionServiceClient implements ComponentDefinitionService {

    private static final Logger log = LoggerFactory.getLogger(RemoteComponentDefinitionServiceClient.class);

    private final ComponentProtoMapper componentProtoMapper;
    private final List<ComponentDefinitionFilter> componentDefinitionFilters;
    private final GrpcChannels grpcChannels;
    private final WorkerInstances workerInstances;

    public RemoteComponentDefinitionServiceClient(
        GrpcChannels grpcChannels, DiscoveryClient discoveryClient, ObjectMapper objectMapper,
        List<ComponentDefinitionFilter> componentDefinitionFilters) {

        this.componentDefinitionFilters = List.copyOf(componentDefinitionFilters);
        this.componentProtoMapper = new ComponentProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
        this.workerInstances = new WorkerInstances(discoveryClient);
    }

    @Override
    public List<Option> executeWorkflowInputOptions(
        String componentName, int componentVersion, String groupName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        @Nullable ComponentConnection componentConnection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<ComponentDefinition> fetchComponentDefinition(String name, @Nullable Integer version) {
        Optional<ServiceInstance> serviceInstance = workerInstances.fetchInstance(name);

        if (serviceInstance.isEmpty()) {
            return Optional.empty();
        }

        GetComponentDefinitionRequest.Builder builder = GetComponentDefinitionRequest.newBuilder()
            .setName(name);

        if (version != null) {
            builder.setVersion(version);
        }

        ComponentDefinitionResult componentDefinitionResult = getStub(serviceInstance.get())
            .fetchComponentDefinition(builder.build());

        if (!componentDefinitionResult.hasComponentDefinition()) {
            return Optional.empty();
        }

        return Optional.of(
            componentProtoMapper.toComponentDefinition(componentDefinitionResult.getComponentDefinition()));
    }

    @Override
    public ComponentDefinition getComponentDefinition(String name, @Nullable Integer version) {
        GetComponentDefinitionRequest.Builder builder = GetComponentDefinitionRequest.newBuilder()
            .setName(name);

        if (version != null) {
            builder.setVersion(version);
        }

        RemoteComponentDefinition remoteComponentDefinition = getStub(workerInstances.getInstance(name))
            .getComponentDefinition(builder.build());

        return componentProtoMapper.toComponentDefinition(remoteComponentDefinition);
    }

    @Override
    public List<ComponentDefinition> getComponentDefinitions() {
        return getMergedComponentDefinitions(
            stub -> stub.getComponentDefinitions(GetComponentDefinitionsRequest.getDefaultInstance()));
    }

    @Override
    public List<ComponentDefinition> getComponentDefinitions(
        Boolean actionDefinitions, Boolean clusterElementDefinitions, Boolean connectionDefinitions,
        Boolean triggerDefinitions, List<String> include, PlatformType platformType) {

        GetComponentDefinitionsRequest.Builder builder = GetComponentDefinitionsRequest.newBuilder();

        if (actionDefinitions != null) {
            builder.setActionDefinitions(actionDefinitions);
        }

        if (clusterElementDefinitions != null) {
            builder.setClusterElementDefinitions(clusterElementDefinitions);
        }

        if (connectionDefinitions != null) {
            builder.setConnectionDefinitions(connectionDefinitions);
        }

        if (triggerDefinitions != null) {
            builder.setTriggerDefinitions(triggerDefinitions);
        }

        if (include != null) {
            builder.addAllInclude(include);
        }

        GetComponentDefinitionsRequest getComponentDefinitionsRequest = builder.build();

        List<ComponentDefinition> componentDefinitions = getMergedComponentDefinitions(
            stub -> stub.getComponentDefinitions(getComponentDefinitionsRequest));

        componentDefinitions = filterByPlatformType(componentDefinitions, platformType);

        if (include != null && !include.isEmpty()) {
            componentDefinitions = new ArrayList<>(componentDefinitions);

            componentDefinitions.sort(Comparator.comparing(componentDefinition -> include.indexOf(
                componentDefinition.getName())));
        }

        return componentDefinitions;
    }

    @Override
    public List<ComponentDefinition> getComponentDefinitions(String query, PlatformType platformType) {
        SearchComponentDefinitionsRequest searchComponentDefinitionsRequest =
            SearchComponentDefinitionsRequest.newBuilder()
                .setQuery(query)
                .build();

        return filterByPlatformType(
            getMergedComponentDefinitions(stub -> stub.searchComponentDefinitions(searchComponentDefinitionsRequest)),
            platformType);
    }

    @Override
    public List<ComponentDefinition> getComponentDefinitionVersions(String name) {
        GetComponentDefinitionVersionsRequest getComponentDefinitionVersionsRequest =
            GetComponentDefinitionVersionsRequest.newBuilder()
                .setName(name)
                .build();

        return getMergedComponentDefinitions(
            stub -> stub.getComponentDefinitionVersions(getComponentDefinitionVersionsRequest));
    }

    @Override
    public ComponentDefinition getConnectionComponentDefinition(String name, int connectionVersion) {
        GetConnectionComponentDefinitionRequest getConnectionComponentDefinitionRequest =
            GetConnectionComponentDefinitionRequest.newBuilder()
                .setName(name)
                .setConnectionVersion(connectionVersion)
                .build();

        return componentProtoMapper.toComponentDefinition(
            getStub(workerInstances.getInstance(name)).getConnectionComponentDefinition(
                getConnectionComponentDefinitionRequest));
    }

    @Override
    public boolean hasComponentDefinition(String name, @Nullable Integer version) {
        Optional<ServiceInstance> serviceInstance = workerInstances.fetchInstance(name);

        if (serviceInstance.isEmpty()) {
            return false;
        }

        GetComponentDefinitionRequest.Builder builder = GetComponentDefinitionRequest.newBuilder()
            .setName(name);

        if (version != null) {
            builder.setVersion(version);
        }

        BoolResponse boolResponse = getStub(serviceInstance.get()).hasComponentDefinition(builder.build());

        return boolResponse.getValue();
    }

    private List<ComponentDefinition> filterByPlatformType(
        List<ComponentDefinition> componentDefinitions, @Nullable PlatformType platformType) {

        if (platformType == null) {
            return componentDefinitions;
        }

        ComponentDefinitionFilter componentDefinitionFilter = componentDefinitionFilters.stream()
            .filter(curComponentDefinitionFilter -> curComponentDefinitionFilter.supports(platformType))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported platform type: " + platformType));

        return componentDefinitions.stream()
            .filter(componentDefinitionFilter::filter)
            .toList();
    }

    private List<ComponentDefinition> getMergedComponentDefinitions(
        Function<ComponentDefinitionRemoteServiceBlockingStub, RemoteComponentDefinitions> call) {

        Map<String, ComponentDefinition> componentDefinitionMap = new LinkedHashMap<>();
        Set<ServiceInstance> serviceInstances = workerInstances.getInstances();
        StatusRuntimeException lastStatusRuntimeException = null;
        int failedInstanceCount = 0;

        for (ServiceInstance serviceInstance : serviceInstances) {
            RemoteComponentDefinitions remoteComponentDefinitions;

            try {
                remoteComponentDefinitions = call.apply(getStub(serviceInstance));
            } catch (StatusRuntimeException statusRuntimeException) {
                log.warn(
                    "Skipping worker instance {} while listing component definitions: {}",
                    serviceInstance.getInstanceId(), statusRuntimeException.getStatus());

                lastStatusRuntimeException = statusRuntimeException;
                failedInstanceCount++;

                continue;
            }

            for (RemoteComponentDefinition remoteComponentDefinition : remoteComponentDefinitions
                .getComponentDefinitionsList()) {

                String key = remoteComponentDefinition.getName() + ":" + remoteComponentDefinition.getVersion();

                if (!componentDefinitionMap.containsKey(key)) {
                    componentDefinitionMap.put(
                        key, componentProtoMapper.toComponentDefinition(remoteComponentDefinition));
                }
            }
        }

        if (lastStatusRuntimeException != null && failedInstanceCount == serviceInstances.size()) {
            throw lastStatusRuntimeException;
        }

        List<ComponentDefinition> componentDefinitions = new ArrayList<>(componentDefinitionMap.values());

        componentDefinitions.sort(
            Comparator.comparing(ComponentDefinition::getName)
                .thenComparing(ComponentDefinition::getVersion));

        return componentDefinitions;
    }

    private ComponentDefinitionRemoteServiceBlockingStub getStub(ServiceInstance serviceInstance) {
        return ComponentDefinitionRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(serviceInstance));
    }
}

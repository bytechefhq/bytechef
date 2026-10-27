/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.facade;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.component.remote.client.WorkerInstances;
import com.bytechef.ee.platform.component.remote.grpc.ComponentProtoMapper;
import com.bytechef.ee.platform.component.remote.grpc.proto.ClusterElementDefinitionFacadeRemoteServiceGrpc;
import com.bytechef.ee.platform.component.remote.grpc.proto.ClusterElementDefinitionFacadeRemoteServiceGrpc.ClusterElementDefinitionFacadeRemoteServiceBlockingStub;
import com.bytechef.ee.platform.component.remote.grpc.proto.ClusterElementExecuteDynamicPropertiesRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.ClusterElementExecuteOptionsRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.ClusterElementExecuteOutputRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.ClusterElementExecuteToolRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.ValueResponse;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.domain.OutputResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteClusterElementDefinitionFacadeClient implements ClusterElementDefinitionFacade {

    private final ComponentProtoMapper componentProtoMapper;
    private final Duration executionDeadline;
    private final GrpcChannels grpcChannels;
    private final WorkerInstances workerInstances;

    public RemoteClusterElementDefinitionFacadeClient(
        ApplicationProperties applicationProperties, GrpcChannels grpcChannels, DiscoveryClient discoveryClient,
        ObjectMapper objectMapper) {

        ApplicationProperties.Remote remote = applicationProperties.getRemote();
        ApplicationProperties.Remote.Grpc grpc = remote.getGrpc();

        this.componentProtoMapper = new ComponentProtoMapper(objectMapper);
        this.executionDeadline = grpc.getExecutionDeadline();
        this.grpcChannels = grpcChannels;
        this.workerInstances = new WorkerInstances(discoveryClient);
    }

    @Override
    public List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, @Nullable Long connectionId) {

        ClusterElementExecuteDynamicPropertiesRequest.Builder builder = getExecuteDynamicPropertiesRequestBuilder(
            componentName, componentVersion, clusterElementName, propertyName, inputParameters, lookupDependsOnPaths,
            connectionId);

        return componentProtoMapper.toProperties(getStub(componentName).executeDynamicProperties(builder.build()));
    }

    @Override
    public List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Map<String, ?> inputParameters, Map<String, ?> extensions, List<String> lookupDependsOnPaths,
        @Nullable Long connectionId, Map<String, Long> clusterElementConnectionIds,
        Map<String, Map<String, ?>> clusterElementInputParameters) {

        ClusterElementExecuteDynamicPropertiesRequest.Builder builder = getExecuteDynamicPropertiesRequestBuilder(
            componentName, componentVersion, clusterElementName, propertyName, inputParameters, lookupDependsOnPaths,
            connectionId)
                .setClusterElements(true);

        if (extensions != null) {
            builder.setExtensions(componentProtoMapper.toBytes(extensions));
        }

        if (clusterElementConnectionIds != null) {
            builder.putAllClusterElementConnectionIds(clusterElementConnectionIds);
        }

        if (clusterElementInputParameters != null) {
            builder.setClusterElementInputParameters(componentProtoMapper.toBytes(clusterElementInputParameters));
        }

        return componentProtoMapper.toProperties(getStub(componentName).executeDynamicProperties(builder.build()));
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Map<String, ?> inputParameters, Map<String, ?> extensions, List<String> lookupDependsOnPaths,
        String searchText, @Nullable Long connectionId, Map<String, Long> clusterElementConnectionIds,
        Map<String, Map<String, ?>> clusterElementInputParameters) {

        ClusterElementExecuteOptionsRequest.Builder builder = ClusterElementExecuteOptionsRequest.newBuilder()
            .setComponentName(componentName)
            .setComponentVersion(componentVersion)
            .setClusterElementName(clusterElementName)
            .setPropertyName(propertyName)
            .setInputParameters(componentProtoMapper.toBytes(inputParameters))
            .addAllLookupDependsOnPaths(lookupDependsOnPaths == null ? List.of() : lookupDependsOnPaths);

        if (extensions != null) {
            builder.setExtensions(componentProtoMapper.toBytes(extensions));
        }

        if (searchText != null) {
            builder.setSearchText(searchText);
        }

        if (connectionId != null) {
            builder.setConnectionId(connectionId);
        }

        if (clusterElementConnectionIds != null) {
            builder.putAllClusterElementConnectionIds(clusterElementConnectionIds);
        }

        if (clusterElementInputParameters != null) {
            builder.setClusterElementInputParameters(componentProtoMapper.toBytes(clusterElementInputParameters));
        }

        return componentProtoMapper.toOptions(getStub(componentName).executeOptions(builder.build()));
    }

    @Override
    public @Nullable OutputResponse executeOutput(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable Long connectionId) {

        ClusterElementExecuteOutputRequest.Builder builder = ClusterElementExecuteOutputRequest.newBuilder()
            .setComponentName(componentName)
            .setComponentVersion(componentVersion)
            .setClusterElementName(clusterElementName)
            .setInputParameters(componentProtoMapper.toBytes(inputParameters));

        if (connectionId != null) {
            builder.setConnectionId(connectionId);
        }

        return componentProtoMapper.toOutputResponse(getStub(componentName).executeOutput(builder.build()));
    }

    @Override
    public Object executeTool(
        String componentName, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable Long connectionId) {

        return executeTool(
            getExecuteToolRequestBuilder(componentName, clusterElementName, inputParameters, connectionId));
    }

    @Override
    public Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable Long connectionId) {

        return executeTool(
            getExecuteToolRequestBuilder(componentName, clusterElementName, inputParameters, connectionId)
                .setComponentVersion(componentVersion));
    }

    private Object executeTool(ClusterElementExecuteToolRequest.Builder builder) {
        ValueResponse valueResponse = getStub(builder.getComponentName()).executeTool(builder.build());

        return componentProtoMapper.toObject(valueResponse.getValue());
    }

    private ClusterElementExecuteDynamicPropertiesRequest.Builder getExecuteDynamicPropertiesRequestBuilder(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, @Nullable Long connectionId) {

        ClusterElementExecuteDynamicPropertiesRequest.Builder builder =
            ClusterElementExecuteDynamicPropertiesRequest.newBuilder()
                .setComponentName(componentName)
                .setComponentVersion(componentVersion)
                .setClusterElementName(clusterElementName)
                .setPropertyName(propertyName)
                .setInputParameters(componentProtoMapper.toBytes(inputParameters))
                .addAllLookupDependsOnPaths(lookupDependsOnPaths == null ? List.of() : lookupDependsOnPaths);

        if (connectionId != null) {
            builder.setConnectionId(connectionId);
        }

        return builder;
    }

    private ClusterElementExecuteToolRequest.Builder getExecuteToolRequestBuilder(
        String componentName, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable Long connectionId) {

        ClusterElementExecuteToolRequest.Builder builder = ClusterElementExecuteToolRequest.newBuilder()
            .setComponentName(componentName)
            .setClusterElementName(clusterElementName)
            .setInputParameters(componentProtoMapper.toBytes(inputParameters));

        if (connectionId != null) {
            builder.setConnectionId(connectionId);
        }

        return builder;
    }

    private ClusterElementDefinitionFacadeRemoteServiceBlockingStub getStub(String componentName) {
        ClusterElementDefinitionFacadeRemoteServiceBlockingStub stub =
            ClusterElementDefinitionFacadeRemoteServiceGrpc.newBlockingStub(
                grpcChannels.getChannel(workerInstances.getInstance(componentName)));

        if (executionDeadline == null) {
            return stub;
        }

        return stub.withDeadlineAfter(executionDeadline.toMillis(), TimeUnit.MILLISECONDS);
    }
}

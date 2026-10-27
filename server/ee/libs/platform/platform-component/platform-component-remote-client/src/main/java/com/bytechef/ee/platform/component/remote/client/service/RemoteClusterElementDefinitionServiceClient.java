/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.service;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.ee.platform.component.remote.client.WorkerInstances;
import com.bytechef.ee.platform.component.remote.grpc.ComponentProtoMapper;
import com.bytechef.ee.platform.component.remote.grpc.proto.ClusterElementDefinitionRemoteServiceGrpc;
import com.bytechef.ee.platform.component.remote.grpc.proto.ClusterElementDefinitionRemoteServiceGrpc.ClusterElementDefinitionRemoteServiceBlockingStub;
import com.bytechef.ee.platform.component.remote.grpc.proto.ClusterElementExecuteWorkflowNodeDescriptionRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetClusterElementDefinitionRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetClusterElementDefinitionsRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetClusterElementTypeRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteClusterElementDefinition;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteClusterElementDefinitions;
import com.bytechef.ee.platform.component.remote.grpc.proto.StringResponse;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.datastream.ClusterElementResolverFunction;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.domain.OutputResponse;
import io.grpc.StatusRuntimeException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public class RemoteClusterElementDefinitionServiceClient implements ClusterElementDefinitionService {

    private static final Logger log = LoggerFactory.getLogger(RemoteClusterElementDefinitionServiceClient.class);

    private final ComponentProtoMapper componentProtoMapper;
    private final GrpcChannels grpcChannels;
    private final WorkerInstances workerInstances;

    public RemoteClusterElementDefinitionServiceClient(
        GrpcChannels grpcChannels, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        this.componentProtoMapper = new ComponentProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
        this.workerInstances = new WorkerInstances(discoveryClient);
    }

    @Override
    public List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths,
        @Nullable ComponentConnection componentConnection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths,
        @Nullable ComponentConnection componentConnection,
        ClusterElementResolverFunction clusterElementResolver) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        @Nullable ComponentConnection componentConnection, ClusterElementResolverFunction clusterElementResolver) {

        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable OutputResponse executeOutput(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection) {

        return null;
    }

    @Override
    public Object executeTool(
        String componentName, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, boolean editorEnvironment) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, boolean editorEnvironment) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        Map<String, ?> extensions, Map<String, ComponentConnection> componentConnections, boolean editorEnvironment) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, ActionContextAware actionContext) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        Map<String, ?> extensions, Map<String, ComponentConnection> componentConnections,
        ActionContextAware actionContext) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object executeApprovalChannel(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        String formUrl, @Nullable ComponentConnection componentConnection, ActionContextAware actionContext) {

        throw new UnsupportedOperationException();
    }

    @Override
    public String executeWorkflowNodeDescription(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters) {

        StringResponse stringResponse = getStub(componentName).executeWorkflowNodeDescription(
            ClusterElementExecuteWorkflowNodeDescriptionRequest.newBuilder()
                .setComponentName(componentName)
                .setComponentVersion(componentVersion)
                .setClusterElementName(clusterElementName)
                .setInputParameters(componentProtoMapper.toBytes(inputParameters))
                .build());

        return stringResponse.hasValue() ? stringResponse.getValue() : null;
    }

    @Override
    public <T> T getClusterElement(String componentName, int componentVersion, String clusterElementName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClusterElementDefinition getClusterElementDefinition(String componentName, String clusterElementName) {
        return getClusterElementDefinition(
            GetClusterElementDefinitionRequest.newBuilder()
                .setComponentName(componentName)
                .setClusterElementName(clusterElementName)
                .build());
    }

    @Override
    public ClusterElementDefinition getClusterElementDefinition(
        String componentName, int componentVersion, String clusterElementName) {

        return getClusterElementDefinition(
            GetClusterElementDefinitionRequest.newBuilder()
                .setComponentName(componentName)
                .setComponentVersion(componentVersion)
                .setClusterElementName(clusterElementName)
                .build());
    }

    @Override
    public ClusterElementDefinition getClusterElementDefinition(
        String componentName, int componentVersion, String clusterElementName, String clusterElementTypeName) {

        return getClusterElementDefinition(
            GetClusterElementDefinitionRequest.newBuilder()
                .setComponentName(componentName)
                .setComponentVersion(componentVersion)
                .setClusterElementName(clusterElementName)
                .setClusterElementTypeName(clusterElementTypeName)
                .build());
    }

    @Override
    public List<ClusterElementDefinition> getClusterElementDefinitions(ClusterElementType clusterElementType) {
        GetClusterElementDefinitionsRequest getClusterElementDefinitionsRequest =
            GetClusterElementDefinitionsRequest.newBuilder()
                .setClusterElementType(componentProtoMapper.toProto(clusterElementType))
                .build();

        Map<String, ClusterElementDefinition> clusterElementDefinitionMap = new LinkedHashMap<>();
        Set<ServiceInstance> serviceInstances = workerInstances.getInstances();
        StatusRuntimeException lastStatusRuntimeException = null;
        int failedInstanceCount = 0;

        for (ServiceInstance serviceInstance : serviceInstances) {
            RemoteClusterElementDefinitions remoteClusterElementDefinitions;

            try {
                remoteClusterElementDefinitions = getStub(serviceInstance).getClusterElementDefinitions(
                    getClusterElementDefinitionsRequest);
            } catch (StatusRuntimeException statusRuntimeException) {
                log.warn(
                    "Skipping worker instance {} while listing cluster element definitions: {}",
                    serviceInstance.getInstanceId(), statusRuntimeException.getStatus());

                lastStatusRuntimeException = statusRuntimeException;
                failedInstanceCount++;

                continue;
            }

            for (RemoteClusterElementDefinition remoteClusterElementDefinition : remoteClusterElementDefinitions
                .getClusterElementDefinitionsList()) {

                String key = remoteClusterElementDefinition.getComponentName() + ":" +
                    remoteClusterElementDefinition.getComponentVersion() + ":" +
                    remoteClusterElementDefinition.getName();

                if (!clusterElementDefinitionMap.containsKey(key)) {
                    clusterElementDefinitionMap.put(
                        key, componentProtoMapper.toClusterElementDefinition(remoteClusterElementDefinition));
                }
            }
        }

        if (lastStatusRuntimeException != null && failedInstanceCount == serviceInstances.size()) {
            throw lastStatusRuntimeException;
        }

        return new ArrayList<>(clusterElementDefinitionMap.values());
    }

    @Override
    public List<ClusterElementDefinition> getClusterElementDefinitions(
        String componentName, int componentVersion, ClusterElementType clusterElementType) {

        RemoteClusterElementDefinitions remoteClusterElementDefinitions = getStub(componentName)
            .getClusterElementDefinitions(
                GetClusterElementDefinitionsRequest.newBuilder()
                    .setComponentName(componentName)
                    .setComponentVersion(componentVersion)
                    .setClusterElementType(componentProtoMapper.toProto(clusterElementType))
                    .build());

        return remoteClusterElementDefinitions.getClusterElementDefinitionsList()
            .stream()
            .map(componentProtoMapper::toClusterElementDefinition)
            .toList();
    }

    @Override
    public ClusterElementType getClusterElementType(
        String rootComponentName, int rootComponentVersion, String clusterElementTypeName) {

        return componentProtoMapper.toClusterElementType(
            getStub(rootComponentName).getClusterElementType(
                GetClusterElementTypeRequest.newBuilder()
                    .setRootComponentName(rootComponentName)
                    .setRootComponentVersion(rootComponentVersion)
                    .setClusterElementTypeName(clusterElementTypeName)
                    .build()));
    }

    @Override
    public List<ClusterElementDefinition> getRootClusterElementDefinitions(
        String rootComponentName, int rootComponentVersion, String clusterElementTypeName) {

        return getClusterElementDefinitions(
            getClusterElementType(rootComponentName, rootComponentVersion, clusterElementTypeName));
    }

    @Override
    public ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, int connectionVersion, String componentOperationName,
        int statusCode, Object body, Map<String, List<String>> headers) {

        throw new UnsupportedOperationException();
    }

    private ClusterElementDefinition getClusterElementDefinition(
        GetClusterElementDefinitionRequest getClusterElementDefinitionRequest) {

        return componentProtoMapper.toClusterElementDefinition(
            getStub(getClusterElementDefinitionRequest.getComponentName())
                .getClusterElementDefinition(getClusterElementDefinitionRequest));
    }

    private ClusterElementDefinitionRemoteServiceBlockingStub getStub(String componentName) {
        return getStub(workerInstances.getInstance(componentName));
    }

    private ClusterElementDefinitionRemoteServiceBlockingStub getStub(ServiceInstance serviceInstance) {
        return ClusterElementDefinitionRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(serviceInstance));
    }
}

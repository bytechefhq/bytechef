/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.service;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.Authorization.RefreshTokenResponse;
import com.bytechef.component.definition.Context;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.ee.platform.component.remote.client.WorkerInstances;
import com.bytechef.ee.platform.component.remote.grpc.ComponentProtoMapper;
import com.bytechef.ee.platform.component.remote.grpc.proto.ConnectionDefinitionRemoteServiceGrpc;
import com.bytechef.ee.platform.component.remote.grpc.proto.ConnectionDefinitionRemoteServiceGrpc.ConnectionDefinitionRemoteServiceBlockingStub;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetAuthorizationTypeRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetConnectionDefinitionRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteConnectionDefinition;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteConnectionDefinitions;
import com.bytechef.ee.platform.component.remote.grpc.proto.StringResponse;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.domain.OAuth2AuthorizationParameters;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.google.protobuf.Empty;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
public class RemoteConnectionDefinitionServiceClient implements ConnectionDefinitionService {

    private final ComponentProtoMapper componentProtoMapper;
    private final GrpcChannels grpcChannels;
    private final WorkerInstances workerInstances;

    public RemoteConnectionDefinitionServiceClient(
        GrpcChannels grpcChannels, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        this.componentProtoMapper = new ComponentProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
        this.workerInstances = new WorkerInstances(discoveryClient);
    }

    @Override
    public Map<String, ?> executeAcquire(
        String componentName, int connectionVersion, AuthorizationType authorizationType,
        Map<String, ?> connectionParameters,
        Context context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ApplyResponse executeAuthorizationApply(
        String componentName, int connectionVersion, AuthorizationType authorizationType,
        Map<String, ?> connectionParameters,
        Context context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<String> executeBaseUri(String componentName, ComponentConnection componentConnection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<String> executeBaseUri(
        String componentName, ComponentConnection componentConnection, Context context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ComponentConnection executeConnectionRefresh(ComponentConnection componentConnection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, int connectionVersion, String componentOperationName,
        int statusCode, Object body, Map<String, List<String>> headers) {

        throw new UnsupportedOperationException();
    }

    @Override
    public RefreshTokenResponse executeRefresh(
        String componentName, int connectionVersion, AuthorizationType authorizationType,
        Map<String, ?> connectionParameters, Context context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
        String componentName, int connectionVersion, AuthorizationType authorizationType,
        Map<String, ?> connectionParameters) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getAuthorizationDetectOn(
        String componentName, int componentVersion, AuthorizationType authorizationType) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Object> getAuthorizationRefreshOn(
        String componentName, int componentVersion, AuthorizationType authorizationType) {

        throw new UnsupportedOperationException();
    }

    @Override
    public AuthorizationType getAuthorizationType(
        String componentName, int connectionVersion, AuthorizationType authorizationType) {

        GetAuthorizationTypeRequest.Builder builder = GetAuthorizationTypeRequest.newBuilder()
            .setComponentName(componentName)
            .setConnectionVersion(connectionVersion);

        if (authorizationType != null) {
            builder.setAuthorizationType(authorizationType.name());
        }

        StringResponse stringResponse = getStub(workerInstances.getInstance(componentName))
            .getAuthorizationType(builder.build());

        return stringResponse.hasValue() ? AuthorizationType.valueOf(stringResponse.getValue()) : null;
    }

    @Override
    public ConnectionDefinition getConnectionConnectionDefinition(
        String componentName, int connectionVersion) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionDefinition getConnectionDefinition(String componentName, Integer componentVersion) {
        RemoteConnectionDefinition remoteConnectionDefinition = getStub(workerInstances.getInstance(componentName))
            .getConnectionDefinition(getConnectionDefinitionRequest(componentName, componentVersion));

        return componentProtoMapper.toConnectionDefinition(remoteConnectionDefinition);
    }

    @Override
    public List<ConnectionDefinition> getConnectionDefinitions(String componentName, Integer componentVersion) {
        RemoteConnectionDefinitions remoteConnectionDefinitions = getStub(workerInstances.getInstance(componentName))
            .getComponentConnectionDefinitions(getConnectionDefinitionRequest(componentName, componentVersion));

        return toConnectionDefinitions(remoteConnectionDefinitions);
    }

    @Override
    public List<ConnectionDefinition> getConnectionDefinitions() {
        Map<String, ConnectionDefinition> connectionDefinitionMap = new LinkedHashMap<>();

        for (ServiceInstance serviceInstance : workerInstances.getInstances()) {
            for (ConnectionDefinition connectionDefinition : toConnectionDefinitions(
                getStub(serviceInstance).getConnectionDefinitions(Empty.getDefaultInstance()))) {

                connectionDefinitionMap.putIfAbsent(
                    connectionDefinition.getComponentName() + ":" + connectionDefinition.getVersion(),
                    connectionDefinition);
            }
        }

        return new ArrayList<>(connectionDefinitionMap.values());
    }

    private static GetConnectionDefinitionRequest getConnectionDefinitionRequest(
        String componentName, Integer componentVersion) {

        GetConnectionDefinitionRequest.Builder builder = GetConnectionDefinitionRequest.newBuilder()
            .setComponentName(componentName);

        if (componentVersion != null) {
            builder.setComponentVersion(componentVersion);
        }

        return builder.build();
    }

    private ConnectionDefinitionRemoteServiceBlockingStub getStub(ServiceInstance serviceInstance) {
        return ConnectionDefinitionRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(serviceInstance));
    }

    private List<ConnectionDefinition> toConnectionDefinitions(
        RemoteConnectionDefinitions remoteConnectionDefinitions) {

        return remoteConnectionDefinitions.getConnectionDefinitionsList()
            .stream()
            .map(componentProtoMapper::toConnectionDefinition)
            .toList();
    }
}

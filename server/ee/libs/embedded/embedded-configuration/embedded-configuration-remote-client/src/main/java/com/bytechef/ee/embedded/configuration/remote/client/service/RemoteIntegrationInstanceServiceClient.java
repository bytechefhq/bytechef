/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.remote.grpc.EmbeddedConfigurationProtoMapper;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.ComponentIntegrationInstanceRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.ConnectedUserIntegrationInstancesRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdsRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationInstanceRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationInstanceResult;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationInstance;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationInstances;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.WorkflowIntegrationInstanceRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.WorkflowUuidIntegrationInstancesRequest;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteIntegrationInstanceServiceClient implements IntegrationInstanceService {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteIntegrationInstanceServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.embeddedConfigurationProtoMapper = new EmbeddedConfigurationProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public IntegrationInstance create(IntegrationInstance integrationInstance) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationInstance create(
        long connectedUserId, long connectionId, long integrationInstanceConfigurationId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<IntegrationInstance> fetchIntegrationInstance(
        long connectedUserId, String componentName, Environment environment) {

        IntegrationInstanceResult integrationInstanceResult = getStub().fetchIntegrationInstance(
            ComponentIntegrationInstanceRequest.newBuilder()
                .setComponentName(componentName)
                .setConnectedUserId(connectedUserId)
                .setEnvironment(environment.ordinal())
                .build());

        if (!integrationInstanceResult.hasIntegrationInstance()) {
            return Optional.empty();
        }

        return Optional.of(
            embeddedConfigurationProtoMapper.toIntegrationInstance(integrationInstanceResult.getIntegrationInstance()));
    }

    @Override
    public Optional<IntegrationInstance> fetchIntegrationInstance(
        long connectedUserId, long integrationInstanceConfigurationId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<IntegrationInstance> fetchIntegrationInstanceByIntegrationId(
        long connectedUserId, long integrationId, Environment environment) {

        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationInstance getIntegrationInstance(
        long connectedUserId, List<String> componentNames, Environment environment) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationInstance> getConnectedUserIntegrationInstances(long connectedUserId) {
        return getConnectedUserIntegrationInstances(
            ConnectedUserIntegrationInstancesRequest.newBuilder()
                .setConnectedUserId(connectedUserId)
                .build());
    }

    @Override
    public boolean existsByConnectionId(long connectionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Long> getConnectionIdsIn(Set<Long> connectionIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationInstance> getConnectedUserIntegrationInstances(long connectedUserId, boolean enabled) {
        return getConnectedUserIntegrationInstances(
            ConnectedUserIntegrationInstancesRequest.newBuilder()
                .setConnectedUserId(connectedUserId)
                .setEnabled(enabled)
                .build());
    }

    @Override
    public List<IntegrationInstance> getConnectedUserIntegrationInstances(
        long connectedUserId, Environment environment) {

        return getConnectedUserIntegrationInstances(
            ConnectedUserIntegrationInstancesRequest.newBuilder()
                .setConnectedUserId(connectedUserId)
                .setEnvironment(environment.ordinal())
                .build());
    }

    @Override
    public List<IntegrationInstance> getIntegrationInstances(
        long connectedUserId, String componentName, Environment environment) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationInstance> getConnectedUserIntegrationInstances(List<Long> connectedUserIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationInstance getIntegrationInstance(long id) {
        RemoteIntegrationInstance remoteIntegrationInstance = mapInvalidArgument(
            () -> getStub().getIntegrationInstance(
                IdRequest.newBuilder()
                    .setId(id)
                    .build()));

        return embeddedConfigurationProtoMapper.toIntegrationInstance(remoteIntegrationInstance);
    }

    @Override
    public List<IntegrationInstance> getIntegrationInstanceConfigurationIntegrationInstances(
        List<Long> integrationInstanceConfigurationIds) {

        RemoteIntegrationInstances remoteIntegrationInstances =
            getStub().getIntegrationInstanceConfigurationIntegrationInstances(
                IdsRequest.newBuilder()
                    .addAllIds(integrationInstanceConfigurationIds)
                    .build());

        return embeddedConfigurationProtoMapper.toIntegrationInstances(remoteIntegrationInstances);
    }

    @Override
    public IntegrationInstance
        getIntegrationInstance(long connectedUserId, String workflowId, Environment environment) {

        RemoteIntegrationInstance remoteIntegrationInstance = mapInvalidArgument(
            () -> getStub().getWorkflowIntegrationInstance(
                WorkflowIntegrationInstanceRequest.newBuilder()
                    .setConnectedUserId(connectedUserId)
                    .setEnvironment(environment.ordinal())
                    .setWorkflowId(workflowId)
                    .build()));

        return embeddedConfigurationProtoMapper.toIntegrationInstance(remoteIntegrationInstance);
    }

    @Override
    public List<IntegrationInstance> getIntegrationInstances(List<Long> ids) {
        RemoteIntegrationInstances remoteIntegrationInstances = getStub().getIntegrationInstances(
            IdsRequest.newBuilder()
                .addAllIds(ids)
                .build());

        return embeddedConfigurationProtoMapper.toIntegrationInstances(remoteIntegrationInstances);
    }

    @Override
    public List<IntegrationInstance> getIntegrationInstancesByWorkflowUuid(
        long connectedUserId, String workflowUuid, Environment environment) {

        RemoteIntegrationInstances remoteIntegrationInstances = getStub().getIntegrationInstancesByWorkflowUuid(
            WorkflowUuidIntegrationInstancesRequest.newBuilder()
                .setConnectedUserId(connectedUserId)
                .setWorkflowUuid(workflowUuid)
                .setEnvironment(environment.ordinal())
                .build());

        return embeddedConfigurationProtoMapper.toIntegrationInstances(remoteIntegrationInstances);
    }

    @Override
    public List<IntegrationInstance> getIntegrationInstances(long integrationInstanceConfigurationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEnabled(long id, boolean enable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationInstance updateIntegrationInstanceConfigurationId(
        long id, long integrationInstanceConfigurationId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationInstance lockIntegrationInstance(long id) {
        throw new UnsupportedOperationException();
    }

    private List<IntegrationInstance> getConnectedUserIntegrationInstances(
        ConnectedUserIntegrationInstancesRequest connectedUserIntegrationInstancesRequest) {

        RemoteIntegrationInstances remoteIntegrationInstances = getStub().getConnectedUserIntegrationInstances(
            connectedUserIntegrationInstancesRequest);

        return embeddedConfigurationProtoMapper.toIntegrationInstances(remoteIntegrationInstances);
    }

    private IntegrationInstanceRemoteServiceGrpc.IntegrationInstanceRemoteServiceBlockingStub getStub() {
        return IntegrationInstanceRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(CONFIGURATION_APP));
    }

    private static <T> T mapInvalidArgument(Supplier<T> remoteCall) {
        try {
            return remoteCall.get();
        } catch (StatusRuntimeException statusRuntimeException) {
            Status status = statusRuntimeException.getStatus();

            if (status.getCode() == Status.Code.INVALID_ARGUMENT) {
                throw new IllegalArgumentException(status.getDescription(), statusRuntimeException);
            }

            throw statusRuntimeException;
        }
    }
}

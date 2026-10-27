/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.remote.grpc.EmbeddedConfigurationProtoMapper;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdsRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationInstanceWorkflowUuidRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationWorkflowRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.OptionalStringResponse;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationWorkflows;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.StringResponse;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.WorkflowIdRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.WorkflowUuidRequest;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.Optional;
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
public class RemoteIntegrationWorkflowServiceClient implements IntegrationWorkflowService {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteIntegrationWorkflowServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.embeddedConfigurationProtoMapper = new EmbeddedConfigurationProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public IntegrationWorkflow addWorkflow(long projectId, int projectVersion, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(List<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long projectId, int projectVersion, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<String> fetchLastWorkflowId(String workflowUuid, Environment environment) {
        OptionalStringResponse optionalStringResponse = getStub().fetchLastWorkflowId(
            toWorkflowUuidRequest(workflowUuid, environment));

        if (!optionalStringResponse.hasValue()) {
            return Optional.empty();
        }

        return Optional.of(optionalStringResponse.getValue());
    }

    @Override
    public IntegrationWorkflow getIntegrationWorkflow(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getWorkflowId(long integrationInstanceId, String workflowUuid) {
        StringResponse stringResponse = mapInvalidArgument(
            () -> getStub().getWorkflowId(
                IntegrationInstanceWorkflowUuidRequest.newBuilder()
                    .setIntegrationInstanceId(integrationInstanceId)
                    .setWorkflowUuid(workflowUuid)
                    .build()));

        return stringResponse.getValue();
    }

    @Override
    public List<Long> getIntegrationWorkflowIds(long projectId, int projectVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationWorkflow> getIntegrationWorkflows() {
        return embeddedConfigurationProtoMapper.toIntegrationWorkflows(
            getStub().getIntegrationWorkflows(Empty.getDefaultInstance()));
    }

    @Override
    public List<IntegrationWorkflow> getIntegrationWorkflows(long integrationId) {
        RemoteIntegrationWorkflows remoteIntegrationWorkflows = getStub().getIntegrationIntegrationWorkflows(
            IdRequest.newBuilder()
                .setId(integrationId)
                .build());

        return embeddedConfigurationProtoMapper.toIntegrationWorkflows(remoteIntegrationWorkflows);
    }

    @Override
    public List<IntegrationWorkflow> getIntegrationWorkflows(List<Long> integrationIds) {
        RemoteIntegrationWorkflows remoteIntegrationWorkflows = getStub().getIntegrationsIntegrationWorkflows(
            IdsRequest.newBuilder()
                .addAllIds(integrationIds)
                .build());

        return embeddedConfigurationProtoMapper.toIntegrationWorkflows(remoteIntegrationWorkflows);
    }

    @Override
    public List<IntegrationWorkflow> getIntegrationWorkflows(long projectId, int lastVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLastWorkflowId(String workflowUuid) {
        StringResponse stringResponse = mapInvalidArgument(
            () -> getStub().getLastWorkflowId(
                WorkflowUuidRequest.newBuilder()
                    .setWorkflowUuid(workflowUuid)
                    .build()));

        return stringResponse.getValue();
    }

    @Override
    public String getLastWorkflowId(String workflowUuid, Environment environment) {
        StringResponse stringResponse = mapInvalidArgument(
            () -> getStub().getLastWorkflowId(toWorkflowUuidRequest(workflowUuid, environment)));

        return stringResponse.getValue();
    }

    @Override
    public List<String> getWorkflowIds(long projectId, int projectVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationWorkflow getWorkflowIntegrationWorkflow(String workflowId) {
        RemoteIntegrationWorkflow remoteIntegrationWorkflow = mapInvalidArgument(
            () -> getStub().getWorkflowIntegrationWorkflow(
                WorkflowIdRequest.newBuilder()
                    .setWorkflowId(workflowId)
                    .build()));

        return embeddedConfigurationProtoMapper.toIntegrationWorkflow(remoteIntegrationWorkflow);
    }

    @Override
    public void publishWorkflow(
        long integrationId, int oldIntegrationVersion, String oldWorkflowId, IntegrationWorkflow integrationWorkflow) {

        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationWorkflow update(IntegrationWorkflow projectWorkflow) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationWorkflow updatePermissionExpression(long id, String permissionExpression) {
        throw new UnsupportedOperationException();
    }

    private IntegrationWorkflowRemoteServiceGrpc.IntegrationWorkflowRemoteServiceBlockingStub getStub() {
        return IntegrationWorkflowRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(CONFIGURATION_APP));
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

    private static WorkflowUuidRequest toWorkflowUuidRequest(String workflowUuid, Environment environment) {
        return WorkflowUuidRequest.newBuilder()
            .setEnvironment(environment.ordinal())
            .setWorkflowUuid(workflowUuid)
            .build();
    }
}

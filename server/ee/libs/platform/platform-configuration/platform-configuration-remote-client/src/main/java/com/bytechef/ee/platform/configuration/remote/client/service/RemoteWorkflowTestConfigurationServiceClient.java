/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.remote.client.service;

import com.bytechef.ee.platform.configuration.remote.grpc.PlatformConfigurationProtoMapper;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.BoolResponse;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.InputsResponse;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.WorkflowTestConfigurationInputsRequest;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.WorkflowTestConfigurationRemoteServiceGrpc;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.WorkflowTestConfigurationRemoteServiceGrpc.WorkflowTestConfigurationRemoteServiceBlockingStub;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteWorkflowTestConfigurationServiceClient implements WorkflowTestConfigurationService {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final GrpcChannels grpcChannels;
    private final PlatformConfigurationProtoMapper platformConfigurationProtoMapper;

    public RemoteWorkflowTestConfigurationServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.grpcChannels = grpcChannels;
        this.platformConfigurationProtoMapper = new PlatformConfigurationProtoMapper(objectMapper);
    }

    @Override
    public void copyWorkflowTestConfigurations(String sourceWorkflowId, String targetWorkflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String workflowId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(List<String> workflowIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteWorkflowTestConfigurationConnection(long connectionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<WorkflowTestConfiguration> fetchWorkflowTestConfiguration(String workflowId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Long> fetchWorkflowTestConfigurationConnectionId(
        String workflowId, String workflowNodeName, long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<WorkflowTestConfigurationConnection> getWorkflowTestConfigurationConnections(
        String workflowId, String workflowNodeName, long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<WorkflowTestConfiguration> getWorkflowTestConfigurations(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConnectionUsed(long connectionId) {
        BoolResponse boolResponse = getStub().isConnectionUsed(
            IdRequest.newBuilder()
                .setId(connectionId)
                .build());

        return boolResponse.getValue();
    }

    @Override
    public WorkflowTestConfiguration saveWorkflowTestConfiguration(
        WorkflowTestConfiguration workflowTestConfiguration) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, ?> getWorkflowTestConfigurationInputs(String workflowId, long environmentId) {
        InputsResponse inputsResponse = getStub().getWorkflowTestConfigurationInputs(
            WorkflowTestConfigurationInputsRequest.newBuilder()
                .setEnvironmentId(environmentId)
                .setWorkflowId(workflowId)
                .build());

        if (!inputsResponse.hasInputs()) {
            return null;
        }

        return platformConfigurationProtoMapper.toMap(inputsResponse.getInputs());
    }

    @Override
    public void deleteWorkflowTestConfigurationConnection(
        String workflowId, String workflowNodeName, String key, long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void saveWorkflowTestConfigurationConnection(
        String workflowId, String workflowNodeName, String key, long connectionId, boolean workflowNodeTrigger,
        long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void saveWorkflowTestConfigurationInputs(String workflowId, String key, Object value, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateWorkflowId(String oldWorkflowId, String newWorkflowId) {
        throw new UnsupportedOperationException();
    }

    private WorkflowTestConfigurationRemoteServiceBlockingStub getStub() {
        return WorkflowTestConfigurationRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(CONFIGURATION_APP));
    }
}

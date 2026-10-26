/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.remote.grpc.EmbeddedConfigurationProtoMapper;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.BoolResponse;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.ConfigurationWorkflowRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationInstanceConfigurationWorkflowConnectionResult;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationInstanceConfigurationWorkflowRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationInstanceConfigurationWorkflowRemoteServiceGrpc.IntegrationInstanceConfigurationWorkflowRemoteServiceBlockingStub;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteIntegrationInstanceConfigurationConfigurationWorkflowServiceClient
    implements IntegrationInstanceConfigurationWorkflowService {

    private static final String CONFIGURATION_APP = "configuration-app";
    private final EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteIntegrationInstanceConfigurationConfigurationWorkflowServiceClient(GrpcChannels grpcChannels,
        ObjectMapper objectMapper) {

        this.embeddedConfigurationProtoMapper = new EmbeddedConfigurationProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public IntegrationInstanceConfigurationWorkflow create(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationInstanceConfigurationWorkflow> create(
        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<IntegrationInstanceConfigurationWorkflowConnection>
        fetchIntegrationInstanceConfigurationWorkflowConnection(
            long integrationInstanceConfigurationId, String workflowId, String workflowNodeName,
            String workflowConnectionKey) {

        IntegrationInstanceConfigurationWorkflowConnectionResult integrationInstanceConfigurationWorkflowConnectionResult =
            getStub().fetchIntegrationInstanceConfigurationWorkflowConnection(
                toRequest(integrationInstanceConfigurationId, workflowId, workflowNodeName, workflowConnectionKey));

        if (!integrationInstanceConfigurationWorkflowConnectionResult.hasConnection()) {
            return Optional.empty();
        }

        return Optional.of(
            embeddedConfigurationProtoMapper.toIntegrationInstanceConfigurationWorkflowConnection(
                integrationInstanceConfigurationWorkflowConnectionResult.getConnection()));
    }

    @Override
    public IntegrationInstanceConfigurationWorkflow getIntegrationInstanceConfigurationWorkflow(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationInstanceConfigurationWorkflow getIntegrationInstanceConfigurationWorkflow(
        long integrationInstanceConfigurationId, String workflowId) {

        return embeddedConfigurationProtoMapper.toIntegrationInstanceConfigurationWorkflow(
            getStub().getIntegrationInstanceConfigurationWorkflow(
                toRequest(integrationInstanceConfigurationId, workflowId, null, null)));
    }

    @Override
    public IntegrationInstanceConfigurationWorkflowConnection getIntegrationInstanceConfigurationWorkflowConnection(
        long integrationInstanceConfigurationId, String workflowId, String operationName,
        String key) {

        return embeddedConfigurationProtoMapper.toIntegrationInstanceConfigurationWorkflowConnection(
            getStub().getIntegrationInstanceConfigurationWorkflowConnection(
                toRequest(integrationInstanceConfigurationId, workflowId, operationName, key)));
    }

    @Override
    public List<IntegrationInstanceConfigurationWorkflowConnection>
        getIntegrationInstanceConfigurationWorkflowConnections(
            Long integrationInstanceConfigurationId, String workflowId, String operationName) {

        return embeddedConfigurationProtoMapper.toIntegrationInstanceConfigurationWorkflowConnections(
            getStub().getIntegrationInstanceConfigurationWorkflowConnections(
                toRequest(integrationInstanceConfigurationId, workflowId, operationName, null)));
    }

    @Override
    public List<IntegrationInstanceConfigurationWorkflow> getIntegrationInstanceConfigurationWorkflows(
        long integrationInstanceConfigurationId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationInstanceConfigurationWorkflow> getIntegrationInstanceConfigurationWorkflows(
        List<Long> integrationInstanceConfigurationIds) {

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
    public boolean isIntegrationInstanceWorkflowEnabled(long integrationInstanceConfigurationId, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationInstanceConfigurationWorkflow update(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationInstanceConfigurationWorkflow> update(
        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEnabled(Long id, boolean enable) {
        throw new UnsupportedOperationException();
    }

    private IntegrationInstanceConfigurationWorkflowRemoteServiceBlockingStub getStub() {
        return IntegrationInstanceConfigurationWorkflowRemoteServiceGrpc.newBlockingStub(
            grpcChannels.getChannel(CONFIGURATION_APP));
    }

    private static ConfigurationWorkflowRequest toRequest(
        @Nullable Long integrationInstanceConfigurationId, String workflowId, @Nullable String workflowNodeName,
        @Nullable String workflowConnectionKey) {

        ConfigurationWorkflowRequest.Builder builder = ConfigurationWorkflowRequest.newBuilder()
            .setWorkflowId(workflowId);

        if (integrationInstanceConfigurationId != null) {
            builder.setIntegrationInstanceConfigurationId(integrationInstanceConfigurationId);
        }

        if (workflowNodeName != null) {
            builder.setWorkflowNodeName(workflowNodeName);
        }

        if (workflowConnectionKey != null) {
            builder.setWorkflowConnectionKey(workflowConnectionKey);
        }

        return builder.build();
    }
}

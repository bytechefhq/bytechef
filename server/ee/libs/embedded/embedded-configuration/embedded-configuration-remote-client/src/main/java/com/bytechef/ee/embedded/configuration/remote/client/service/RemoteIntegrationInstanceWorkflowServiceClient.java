/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.remote.grpc.EmbeddedConfigurationProtoMapper;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationInstanceWorkflowRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationInstanceWorkflowRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationInstanceWorkflowResult;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationInstanceWorkflows;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteIntegrationInstanceWorkflowServiceClient implements IntegrationInstanceWorkflowService {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteIntegrationInstanceWorkflowServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.embeddedConfigurationProtoMapper = new EmbeddedConfigurationProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public IntegrationInstanceWorkflow createIntegrationInstanceWorkflow(
        long integrationInstanceId, long integrationInstanceConfigurationWorkflowId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteByIntegrationInstanceId(Long integrationInstanceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteByIntegrationInstanceConfigurationWorkflowId(Long integrationInstanceConfigurationWorkflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<IntegrationInstanceWorkflow> fetchIntegrationInstanceWorkflow(
        long integrationInstanceId, long integrationInstanceConfigurationWorkflowId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<IntegrationInstanceWorkflow> fetchIntegrationInstanceWorkflow(
        long integrationInstanceId, String workflowId) {

        IntegrationInstanceWorkflowResult integrationInstanceWorkflowResult =
            getStub().fetchIntegrationInstanceWorkflow(
                toRequest(integrationInstanceId, workflowId));

        if (!integrationInstanceWorkflowResult.hasIntegrationInstanceWorkflow()) {
            return Optional.empty();
        }

        return Optional.of(
            embeddedConfigurationProtoMapper.toIntegrationInstanceWorkflow(
                integrationInstanceWorkflowResult.getIntegrationInstanceWorkflow()));
    }

    @Override
    public IntegrationInstanceWorkflow getIntegrationInstanceWorkflow(
        long integrationInstanceId, String workflowId) {

        return embeddedConfigurationProtoMapper.toIntegrationInstanceWorkflow(
            getStub().getIntegrationInstanceWorkflow(toRequest(integrationInstanceId, workflowId)));
    }

    @Override
    public List<IntegrationInstanceWorkflow> getIntegrationInstanceWorkflows(long integrationInstanceId) {
        RemoteIntegrationInstanceWorkflows remoteIntegrationInstanceWorkflows =
            getStub().getIntegrationInstanceWorkflows(
                IdRequest.newBuilder()
                    .setId(integrationInstanceId)
                    .build());

        return embeddedConfigurationProtoMapper.toIntegrationInstanceWorkflows(remoteIntegrationInstanceWorkflows);
    }

    @Override
    public List<IntegrationInstanceWorkflow> getIntegrationInstanceWorkflows(List<Long> integrationInstanceIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(IntegrationInstanceWorkflow integrationInstanceWorkflow) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEnabled(Long id, boolean enabled) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateIntegrationInstanceConfigurationWorkflowId(
        long id, long integrationInstanceConfigurationWorkflowId) {

        throw new UnsupportedOperationException();
    }

    private IntegrationInstanceWorkflowRemoteServiceGrpc.IntegrationInstanceWorkflowRemoteServiceBlockingStub
        getStub() {
        return IntegrationInstanceWorkflowRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(CONFIGURATION_APP));
    }

    private static IntegrationInstanceWorkflowRequest toRequest(long integrationInstanceId, String workflowId) {
        return IntegrationInstanceWorkflowRequest.newBuilder()
            .setIntegrationInstanceId(integrationInstanceId)
            .setWorkflowId(workflowId)
            .build();
    }
}

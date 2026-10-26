/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion;
import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion.Status;
import com.bytechef.ee.embedded.configuration.remote.grpc.EmbeddedConfigurationProtoMapper;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.WorkflowIdRequest;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.google.protobuf.Empty;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteIntegrationServiceClient implements IntegrationService {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteIntegrationServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.embeddedConfigurationProtoMapper = new EmbeddedConfigurationProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public Integration create(Integration integration) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integration getIntegrationInstanceIntegration(long integrationInstanceId) {
        return embeddedConfigurationProtoMapper.toIntegration(
            getStub().getIntegrationInstanceIntegration(toIdRequest(integrationInstanceId)));
    }

    @Override
    public Integration getIntegrationInstanceConfigurationIntegration(long integrationInstanceConfigurationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integration getIntegration(long id) {
        return embeddedConfigurationProtoMapper.toIntegration(getStub().getIntegration(toIdRequest(id)));
    }

    @Override
    public List<Integration> getIntegrations() {
        return embeddedConfigurationProtoMapper.toIntegrations(getStub().getIntegrations(Empty.getDefaultInstance()));
    }

    @Override
    public List<Integration> getIntegrations(List<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationVersion> getIntegrationVersions(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Integration> getIntegrations(Long categoryId, List<Long> ids, Long tagId, Status status) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integration getWorkflowIntegration(String workflowId) {
        return embeddedConfigurationProtoMapper.toIntegration(
            getStub().getWorkflowIntegration(
                WorkflowIdRequest.newBuilder()
                    .setWorkflowId(workflowId)
                    .build()));
    }

    @Override
    public int publishIntegration(long id, String description) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integration update(long id, List<Long> tagIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integration update(Integration integration) {
        throw new UnsupportedOperationException();
    }

    private IntegrationRemoteServiceGrpc.IntegrationRemoteServiceBlockingStub getStub() {
        return IntegrationRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(CONFIGURATION_APP));
    }

    private static IdRequest toIdRequest(long id) {
        return IdRequest.newBuilder()
            .setId(id)
            .build();
    }
}

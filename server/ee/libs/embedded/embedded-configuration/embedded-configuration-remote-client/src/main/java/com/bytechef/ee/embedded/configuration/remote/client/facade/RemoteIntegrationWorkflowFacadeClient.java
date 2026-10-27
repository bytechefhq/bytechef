/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.facade;

import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
import com.bytechef.ee.embedded.configuration.remote.grpc.EmbeddedConfigurationProtoMapper;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationWorkflowFacadeRemoteServiceGrpc;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.dto.VersionWorkflowChange;
import com.bytechef.platform.configuration.dto.VersionWorkflowChanges;
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
public class RemoteIntegrationWorkflowFacadeClient implements IntegrationWorkflowFacade {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteIntegrationWorkflowFacadeClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.embeddedConfigurationProtoMapper = new EmbeddedConfigurationProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public long addWorkflow(long integrationId, String definition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteWorkflow(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationWorkflowDTO>
        getIntegrationVersionWorkflows(long id, int integrationVersion, boolean includeAllFields) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VersionWorkflowChange getIntegrationVersionWorkflowChange(
        long integrationId, int integrationVersion, String workflowUuid) {

        throw new UnsupportedOperationException();
    }

    @Override
    public VersionWorkflowChanges getIntegrationVersionWorkflowChanges(long integrationId, int integrationVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationWorkflowDTO getIntegrationWorkflow(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationWorkflowDTO getIntegrationWorkflow(long integrationWorkflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationWorkflowDTO> getIntegrationWorkflows() {
        IntegrationWorkflowFacadeRemoteServiceGrpc.IntegrationWorkflowFacadeRemoteServiceBlockingStub stub =
            IntegrationWorkflowFacadeRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(CONFIGURATION_APP));

        return embeddedConfigurationProtoMapper.toIntegrationWorkflowDTOs(
            stub.getIntegrationWorkflows(Empty.getDefaultInstance()));
    }

    @Override
    public List<IntegrationWorkflowDTO> getIntegrationWorkflows(long integrationId) {
        return List.of();
    }

    @Override
    public void updatePermissionExpression(long integrationWorkflowId, String permissionExpression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationWorkflowDTO updateWorkflow(String workflowId, String definition, int version) {
        throw new UnsupportedOperationException();
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.remote.client.service;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.platform.configuration.remote.grpc.PlatformConfigurationProtoMapper;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.CreateWorkflowRequest;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.WorkflowIdRequest;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.WorkflowIdsRequest;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.WorkflowRemoteServiceGrpc;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.WorkflowRemoteServiceGrpc.WorkflowRemoteServiceBlockingStub;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.WorkflowResult;
import com.bytechef.ee.remote.grpc.GrpcChannels;
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
public class RemoteWorkflowServiceClient implements WorkflowService {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final GrpcChannels grpcChannels;
    private final PlatformConfigurationProtoMapper platformConfigurationProtoMapper;

    public RemoteWorkflowServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.grpcChannels = grpcChannels;
        this.platformConfigurationProtoMapper = new PlatformConfigurationProtoMapper(objectMapper);
    }

    @Override
    public Workflow create(String definition, Workflow.Format format, Workflow.SourceType sourceType) {
        CreateWorkflowRequest.Builder builder = CreateWorkflowRequest.newBuilder();

        if (definition != null) {
            builder.setDefinition(definition);
        }

        if (format != null) {
            builder.setFormat(format.name());
        }

        if (sourceType != null) {
            builder.setSourceType(sourceType.name());
        }

        return platformConfigurationProtoMapper.toWorkflow(getStub().createWorkflow(builder.build()));
    }

    @Override
    public void delete(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(List<String> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Workflow duplicateWorkflow(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Workflow> fetchWorkflow(String id) {
        WorkflowResult workflowResult = getStub().fetchWorkflow(toWorkflowIdRequest(id));

        if (!workflowResult.hasWorkflow()) {
            return Optional.empty();
        }

        return Optional.of(platformConfigurationProtoMapper.toWorkflow(workflowResult.getWorkflow()));
    }

    @Override
    public List<Workflow> getWorkflows() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Workflow getWorkflow(String id) {
        return platformConfigurationProtoMapper.toWorkflow(getStub().getWorkflow(toWorkflowIdRequest(id)));
    }

    @Override
    public List<Workflow> getWorkflows(List<String> workflowIds) {
        return platformConfigurationProtoMapper.toWorkflows(
            getStub().getWorkflows(
                WorkflowIdsRequest.newBuilder()
                    .addAllIds(workflowIds)
                    .build()));
    }

    @Override
    public void refreshCache(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Workflow update(String id, String definition, int version) {
        throw new UnsupportedOperationException();
    }

    private WorkflowRemoteServiceBlockingStub getStub() {
        return WorkflowRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(CONFIGURATION_APP));
    }

    private static WorkflowIdRequest toWorkflowIdRequest(String id) {
        return WorkflowIdRequest.newBuilder()
            .setId(id)
            .build();
    }
}

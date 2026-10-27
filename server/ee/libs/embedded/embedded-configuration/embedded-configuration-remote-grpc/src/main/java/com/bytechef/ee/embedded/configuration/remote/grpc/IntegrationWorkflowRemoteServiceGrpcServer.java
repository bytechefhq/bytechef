/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.grpc;

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
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class IntegrationWorkflowRemoteServiceGrpcServer
    extends IntegrationWorkflowRemoteServiceGrpc.IntegrationWorkflowRemoteServiceImplBase {

    private final EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper;
    private final IntegrationWorkflowService integrationWorkflowService;

    @SuppressFBWarnings("EI")
    public IntegrationWorkflowRemoteServiceGrpcServer(
        IntegrationWorkflowService integrationWorkflowService,
        EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper) {

        this.embeddedConfigurationProtoMapper = embeddedConfigurationProtoMapper;
        this.integrationWorkflowService = integrationWorkflowService;
    }

    @Override
    public void fetchLastWorkflowId(
        WorkflowUuidRequest request, StreamObserver<OptionalStringResponse> responseObserver) {

        Optional<String> workflowId = integrationWorkflowService.fetchLastWorkflowId(
            request.getWorkflowUuid(), EmbeddedConfigurationProtoMapper.toEnvironment(request.getEnvironment()));

        OptionalStringResponse.Builder builder = OptionalStringResponse.newBuilder();

        workflowId.ifPresent(builder::setValue);

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getIntegrationIntegrationWorkflows(
        IdRequest request, StreamObserver<RemoteIntegrationWorkflows> responseObserver) {

        responseObserver.onNext(
            embeddedConfigurationProtoMapper.toRemoteIntegrationWorkflows(
                integrationWorkflowService.getIntegrationWorkflows(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void getIntegrationWorkflows(Empty request, StreamObserver<RemoteIntegrationWorkflows> responseObserver) {
        responseObserver.onNext(
            embeddedConfigurationProtoMapper.toRemoteIntegrationWorkflows(
                integrationWorkflowService.getIntegrationWorkflows()));
        responseObserver.onCompleted();
    }

    @Override
    public void getIntegrationsIntegrationWorkflows(
        IdsRequest request, StreamObserver<RemoteIntegrationWorkflows> responseObserver) {

        responseObserver.onNext(
            embeddedConfigurationProtoMapper.toRemoteIntegrationWorkflows(
                integrationWorkflowService.getIntegrationWorkflows(request.getIdsList())));
        responseObserver.onCompleted();
    }

    @Override
    public void getLastWorkflowId(WorkflowUuidRequest request, StreamObserver<StringResponse> responseObserver) {
        String workflowId;

        if (request.hasEnvironment()) {
            workflowId = integrationWorkflowService.getLastWorkflowId(
                request.getWorkflowUuid(), EmbeddedConfigurationProtoMapper.toEnvironment(request.getEnvironment()));
        } else {
            workflowId = integrationWorkflowService.getLastWorkflowId(request.getWorkflowUuid());
        }

        responseObserver.onNext(
            StringResponse.newBuilder()
                .setValue(workflowId)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getWorkflowId(
        IntegrationInstanceWorkflowUuidRequest request, StreamObserver<StringResponse> responseObserver) {

        responseObserver.onNext(
            StringResponse.newBuilder()
                .setValue(
                    integrationWorkflowService.getWorkflowId(
                        request.getIntegrationInstanceId(), request.getWorkflowUuid()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getWorkflowIntegrationWorkflow(
        WorkflowIdRequest request, StreamObserver<RemoteIntegrationWorkflow> responseObserver) {

        responseObserver.onNext(
            embeddedConfigurationProtoMapper.toProto(
                integrationWorkflowService.getWorkflowIntegrationWorkflow(request.getWorkflowId())));
        responseObserver.onCompleted();
    }
}

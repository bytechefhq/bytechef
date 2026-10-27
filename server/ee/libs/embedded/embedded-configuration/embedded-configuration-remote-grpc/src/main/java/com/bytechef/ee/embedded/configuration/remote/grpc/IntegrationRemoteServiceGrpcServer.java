/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.grpc;

import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdsRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegration;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrations;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.WorkflowIdRequest;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class IntegrationRemoteServiceGrpcServer extends IntegrationRemoteServiceGrpc.IntegrationRemoteServiceImplBase {

    private final EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper;
    private final IntegrationService integrationService;

    @SuppressFBWarnings("EI")
    public IntegrationRemoteServiceGrpcServer(
        IntegrationService integrationService, EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper) {

        this.embeddedConfigurationProtoMapper = embeddedConfigurationProtoMapper;
        this.integrationService = integrationService;
    }

    @Override
    public void getIntegration(IdRequest request, StreamObserver<RemoteIntegration> responseObserver) {
        responseObserver
            .onNext(embeddedConfigurationProtoMapper.toProto(integrationService.getIntegration(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void
        getIntegrationInstanceIntegration(IdRequest request, StreamObserver<RemoteIntegration> responseObserver) {
        responseObserver.onNext(
            embeddedConfigurationProtoMapper.toProto(
                integrationService.getIntegrationInstanceIntegration(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void getIntegrations(Empty request, StreamObserver<RemoteIntegrations> responseObserver) {
        responseObserver
            .onNext(embeddedConfigurationProtoMapper.toRemoteIntegrations(integrationService.getIntegrations()));
        responseObserver.onCompleted();
    }

    @Override
    public void getIntegrationsByIds(IdsRequest request, StreamObserver<RemoteIntegrations> responseObserver) {
        responseObserver.onNext(
            embeddedConfigurationProtoMapper.toRemoteIntegrations(
                integrationService.getIntegrations(request.getIdsList())));
        responseObserver.onCompleted();
    }

    @Override
    public void getWorkflowIntegration(WorkflowIdRequest request, StreamObserver<RemoteIntegration> responseObserver) {
        responseObserver.onNext(
            embeddedConfigurationProtoMapper
                .toProto(integrationService.getWorkflowIntegration(request.getWorkflowId())));
        responseObserver.onCompleted();
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.grpc;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.BoolResponse;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.ConfigurationWorkflowRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationInstanceConfigurationWorkflowConnectionResult;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationInstanceConfigurationWorkflowRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationInstanceConfigurationWorkflowConnections;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class IntegrationInstanceConfigurationWorkflowRemoteServiceGrpcServer extends
    IntegrationInstanceConfigurationWorkflowRemoteServiceGrpc.IntegrationInstanceConfigurationWorkflowRemoteServiceImplBase {

    private final EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceConfigurationWorkflowRemoteServiceGrpcServer(
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper) {

        this.embeddedConfigurationProtoMapper = embeddedConfigurationProtoMapper;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
    }

    @Override
    public void fetchIntegrationInstanceConfigurationWorkflowConnection(
        ConfigurationWorkflowRequest request,
        StreamObserver<IntegrationInstanceConfigurationWorkflowConnectionResult> responseObserver) {

        Optional<IntegrationInstanceConfigurationWorkflowConnection> integrationInstanceConfigurationWorkflowConnection =
            integrationInstanceConfigurationWorkflowService.fetchIntegrationInstanceConfigurationWorkflowConnection(
                request.getIntegrationInstanceConfigurationId(), request.getWorkflowId(), request.getWorkflowNodeName(),
                request.getWorkflowConnectionKey());

        IntegrationInstanceConfigurationWorkflowConnectionResult.Builder builder =
            IntegrationInstanceConfigurationWorkflowConnectionResult.newBuilder();

        integrationInstanceConfigurationWorkflowConnection.ifPresent(
            curConnection -> builder.setConnection(embeddedConfigurationProtoMapper.toProto(curConnection)));

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getIntegrationInstanceConfigurationWorkflow(
        ConfigurationWorkflowRequest request,
        StreamObserver<RemoteIntegrationInstanceConfigurationWorkflow> responseObserver) {

        responseObserver.onNext(
            embeddedConfigurationProtoMapper.toProto(
                integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                    request.getIntegrationInstanceConfigurationId(), request.getWorkflowId())));
        responseObserver.onCompleted();
    }

    @Override
    public void getIntegrationInstanceConfigurationWorkflowConnection(
        ConfigurationWorkflowRequest request,
        StreamObserver<RemoteIntegrationInstanceConfigurationWorkflowConnection> responseObserver) {

        responseObserver.onNext(
            embeddedConfigurationProtoMapper.toProto(
                integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflowConnection(
                    request.getIntegrationInstanceConfigurationId(), request.getWorkflowId(),
                    request.getWorkflowNodeName(), request.getWorkflowConnectionKey())));
        responseObserver.onCompleted();
    }

    @Override
    public void getIntegrationInstanceConfigurationWorkflowConnections(
        ConfigurationWorkflowRequest request,
        StreamObserver<RemoteIntegrationInstanceConfigurationWorkflowConnections> responseObserver) {

        responseObserver.onNext(
            embeddedConfigurationProtoMapper.toRemoteIntegrationInstanceConfigurationWorkflowConnections(
                integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflowConnections(
                    request.hasIntegrationInstanceConfigurationId() ? request.getIntegrationInstanceConfigurationId()
                        : null,
                    request.getWorkflowId(), request.getWorkflowNodeName())));
        responseObserver.onCompleted();
    }

    @Override
    public void isConnectionUsed(IdRequest request, StreamObserver<BoolResponse> responseObserver) {
        responseObserver.onNext(
            BoolResponse.newBuilder()
                .setValue(integrationInstanceConfigurationWorkflowService.isConnectionUsed(request.getId()))
                .build());
        responseObserver.onCompleted();
    }
}

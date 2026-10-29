/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.grpc;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.ComponentIntegrationInstanceRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.ConnectedUserIntegrationInstancesRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdsRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationInstanceRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationInstanceResult;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationInstance;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationInstances;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.WorkflowIntegrationInstanceRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.WorkflowUuidIntegrationInstancesRequest;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 */
public class IntegrationInstanceRemoteServiceGrpcServer
    extends IntegrationInstanceRemoteServiceGrpc.IntegrationInstanceRemoteServiceImplBase {

    private final EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper;
    private final IntegrationInstanceService integrationInstanceService;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceRemoteServiceGrpcServer(
        IntegrationInstanceService integrationInstanceService,
        EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper) {

        this.embeddedConfigurationProtoMapper = embeddedConfigurationProtoMapper;
        this.integrationInstanceService = integrationInstanceService;
    }

    @Override
    public void fetchIntegrationInstance(
        ComponentIntegrationInstanceRequest request, StreamObserver<IntegrationInstanceResult> responseObserver) {

        Optional<IntegrationInstance> integrationInstance = integrationInstanceService.fetchIntegrationInstance(
            request.getConnectedUserId(), request.getComponentName(),
            EmbeddedConfigurationProtoMapper.toEnvironment(request.getEnvironment()));

        IntegrationInstanceResult.Builder builder = IntegrationInstanceResult.newBuilder();

        integrationInstance.ifPresent(
            curIntegrationInstance -> builder.setIntegrationInstance(
                embeddedConfigurationProtoMapper.toProto(curIntegrationInstance)));

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getConnectedUserIntegrationInstances(
        ConnectedUserIntegrationInstancesRequest request, StreamObserver<RemoteIntegrationInstances> responseObserver) {

        List<IntegrationInstance> integrationInstances;

        if (request.hasEnabled()) {
            integrationInstances = integrationInstanceService.getConnectedUserIntegrationInstances(
                request.getConnectedUserId(), request.getEnabled());
        } else if (request.hasEnvironment()) {
            integrationInstances = integrationInstanceService.getConnectedUserIntegrationInstances(
                request.getConnectedUserId(),
                EmbeddedConfigurationProtoMapper.toEnvironment(request.getEnvironment()));
        } else {
            integrationInstances = integrationInstanceService.getConnectedUserIntegrationInstances(
                request.getConnectedUserId());
        }

        responseObserver.onNext(embeddedConfigurationProtoMapper.toProto(integrationInstances));
        responseObserver.onCompleted();
    }

    @Override
    public void getIntegrationInstance(IdRequest request, StreamObserver<RemoteIntegrationInstance> responseObserver) {
        responseObserver.onNext(
            embeddedConfigurationProtoMapper.toProto(
                integrationInstanceService.getIntegrationInstance(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void getIntegrationInstanceConfigurationIntegrationInstances(
        IdsRequest request, StreamObserver<RemoteIntegrationInstances> responseObserver) {

        responseObserver.onNext(
            embeddedConfigurationProtoMapper.toProto(
                integrationInstanceService.getIntegrationInstanceConfigurationIntegrationInstances(
                    request.getIdsList())));
        responseObserver.onCompleted();
    }

    @Override
    public void
        getIntegrationInstances(IdsRequest request, StreamObserver<RemoteIntegrationInstances> responseObserver) {
        responseObserver.onNext(
            embeddedConfigurationProtoMapper.toProto(
                integrationInstanceService.getIntegrationInstances(request.getIdsList())));
        responseObserver.onCompleted();
    }

    @Override
    public void getIntegrationInstancesByWorkflowUuid(
        WorkflowUuidIntegrationInstancesRequest request, StreamObserver<RemoteIntegrationInstances> responseObserver) {

        responseObserver.onNext(
            embeddedConfigurationProtoMapper.toProto(
                integrationInstanceService.getIntegrationInstancesByWorkflowUuid(
                    request.getConnectedUserId(), request.getWorkflowUuid(),
                    EmbeddedConfigurationProtoMapper.toEnvironment(request.getEnvironment()))));
        responseObserver.onCompleted();
    }

    @Override
    public void getWorkflowIntegrationInstance(
        WorkflowIntegrationInstanceRequest request, StreamObserver<RemoteIntegrationInstance> responseObserver) {

        responseObserver.onNext(
            embeddedConfigurationProtoMapper.toProto(
                integrationInstanceService.getIntegrationInstance(
                    request.getConnectedUserId(), request.getWorkflowId(),
                    EmbeddedConfigurationProtoMapper.toEnvironment(request.getEnvironment()))));
        responseObserver.onCompleted();
    }
}

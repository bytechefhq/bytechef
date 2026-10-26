/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.grpc;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.platform.component.remote.grpc.proto.ConnectionDefinitionRemoteServiceGrpc;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetAuthorizationTypeRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetConnectionDefinitionRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteConnectionDefinition;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteConnectionDefinitions;
import com.bytechef.ee.platform.component.remote.grpc.proto.StringResponse;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ConnectionDefinitionRemoteServiceGrpcServer
    extends ConnectionDefinitionRemoteServiceGrpc.ConnectionDefinitionRemoteServiceImplBase {

    private final ComponentProtoMapper componentProtoMapper;
    private final ConnectionDefinitionService connectionDefinitionService;

    public ConnectionDefinitionRemoteServiceGrpcServer(
        ConnectionDefinitionService connectionDefinitionService, ComponentProtoMapper componentProtoMapper) {

        this.componentProtoMapper = componentProtoMapper;
        this.connectionDefinitionService = connectionDefinitionService;
    }

    @Override
    public void getAuthorizationType(
        GetAuthorizationTypeRequest request, StreamObserver<StringResponse> responseObserver) {

        AuthorizationType authorizationType = connectionDefinitionService.getAuthorizationType(
            request.getComponentName(), request.getConnectionVersion(),
            request.hasAuthorizationType() ? AuthorizationType.valueOf(request.getAuthorizationType()) : null);

        StringResponse.Builder builder = StringResponse.newBuilder();

        if (authorizationType != null) {
            builder.setValue(authorizationType.name());
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getComponentConnectionDefinitions(
        GetConnectionDefinitionRequest request, StreamObserver<RemoteConnectionDefinitions> responseObserver) {

        responseObserver.onNext(
            toRemoteConnectionDefinitions(
                connectionDefinitionService.getConnectionDefinitions(
                    request.getComponentName(), request.hasComponentVersion() ? request.getComponentVersion() : null)));
        responseObserver.onCompleted();
    }

    @Override
    public void getConnectionDefinition(
        GetConnectionDefinitionRequest request, StreamObserver<RemoteConnectionDefinition> responseObserver) {

        ConnectionDefinition connectionDefinition = connectionDefinitionService.getConnectionDefinition(
            request.getComponentName(), request.hasComponentVersion() ? request.getComponentVersion() : null);

        responseObserver.onNext(componentProtoMapper.toProto(connectionDefinition));
        responseObserver.onCompleted();
    }

    @Override
    public void getConnectionDefinitions(Empty request, StreamObserver<RemoteConnectionDefinitions> responseObserver) {
        responseObserver.onNext(toRemoteConnectionDefinitions(connectionDefinitionService.getConnectionDefinitions()));
        responseObserver.onCompleted();
    }

    private RemoteConnectionDefinitions toRemoteConnectionDefinitions(
        List<ConnectionDefinition> connectionDefinitions) {

        RemoteConnectionDefinitions.Builder builder = RemoteConnectionDefinitions.newBuilder();

        for (ConnectionDefinition connectionDefinition : connectionDefinitions) {
            builder.addConnectionDefinitions(componentProtoMapper.toProto(connectionDefinition));
        }

        return builder.build();
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.grpc;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.platform.component.remote.grpc.proto.ConnectionDefinitionRemoteServiceGrpc;
import com.bytechef.ee.platform.component.remote.grpc.proto.ExecuteBaseUriRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetAuthorizationTypeRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetConnectionConnectionDefinitionRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetConnectionDefinitionRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetOAuth2AuthorizationParametersRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteComponentConnection;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteConnectionDefinition;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteConnectionDefinitions;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteOAuth2AuthorizationParameters;
import com.bytechef.ee.platform.component.remote.grpc.proto.StringResponse;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.domain.OAuth2AuthorizationParameters;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    public void executeBaseUri(ExecuteBaseUriRequest request, StreamObserver<StringResponse> responseObserver) {
        Map<String, Object> parameters = componentProtoMapper.toMap(request.getParameters());

        ComponentConnection componentConnection = new ComponentConnection(
            request.getComponentName(), request.getConnectionVersion(), request.getConnectionId(),
            parameters == null ? Map.of() : parameters,
            request.hasAuthorizationType() ? AuthorizationType.valueOf(request.getAuthorizationType()) : null);

        Optional<String> baseUri = connectionDefinitionService.executeBaseUri(
            request.getComponentName(), componentConnection);

        StringResponse.Builder builder = StringResponse.newBuilder();

        baseUri.ifPresent(builder::setValue);

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void executeConnectionRefresh(
        RemoteComponentConnection request, StreamObserver<RemoteComponentConnection> responseObserver) {

        ComponentConnection componentConnection = connectionDefinitionService.executeConnectionRefresh(
            componentProtoMapper.toComponentConnection(request));

        responseObserver.onNext(componentProtoMapper.toProto(componentConnection));
        responseObserver.onCompleted();
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
    public void getConnectionConnectionDefinition(
        GetConnectionConnectionDefinitionRequest request,
        StreamObserver<RemoteConnectionDefinition> responseObserver) {

        ConnectionDefinition connectionDefinition = connectionDefinitionService.getConnectionConnectionDefinition(
            request.getComponentName(), request.getConnectionVersion());

        responseObserver.onNext(componentProtoMapper.toProto(connectionDefinition));
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

    @Override
    public void getOAuth2AuthorizationParameters(
        GetOAuth2AuthorizationParametersRequest request,
        StreamObserver<RemoteOAuth2AuthorizationParameters> responseObserver) {

        Map<String, Object> connectionParameters = componentProtoMapper.toMap(request.getConnectionParameters());

        OAuth2AuthorizationParameters oAuth2AuthorizationParameters =
            connectionDefinitionService.getOAuth2AuthorizationParameters(
                request.getComponentName(), request.getConnectionVersion(),
                request.hasAuthorizationType() ? AuthorizationType.valueOf(request.getAuthorizationType()) : null,
                connectionParameters == null ? Map.of() : connectionParameters);

        responseObserver.onNext(componentProtoMapper.toProto(oAuth2AuthorizationParameters));
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

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.remote.grpc;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.remote.grpc.proto.ConnectedUserRemoteServiceGrpc;
import com.bytechef.ee.embedded.connected.user.remote.grpc.proto.CreateConnectedUserRequest;
import com.bytechef.ee.embedded.connected.user.remote.grpc.proto.CreateConnectedUserResponse;
import com.bytechef.ee.embedded.connected.user.remote.grpc.proto.FetchConnectedUserByExternalIdRequest;
import com.bytechef.ee.embedded.connected.user.remote.grpc.proto.FetchConnectedUserResponse;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.Optional;

/**
 * Serves connected user lookups and creation to the microservices that authenticate embedded API key requests. A
 * rejected external id is answered with {@code INVALID_ARGUMENT}.
 *
 * @version ee
 */
public class ConnectedUserRemoteServiceGrpcServer
    extends ConnectedUserRemoteServiceGrpc.ConnectedUserRemoteServiceImplBase {

    private final ConnectedUserService connectedUserService;

    @SuppressFBWarnings("EI")
    public ConnectedUserRemoteServiceGrpcServer(ConnectedUserService connectedUserService) {
        this.connectedUserService = connectedUserService;
    }

    @Override
    public void createConnectedUser(
        CreateConnectedUserRequest request, StreamObserver<CreateConnectedUserResponse> responseObserver) {

        ConnectedUser connectedUser;

        try {
            connectedUser = connectedUserService.createConnectedUser(
                request.getExternalId(), request.getEnvironmentId());
        } catch (IllegalArgumentException illegalArgumentException) {
            responseObserver.onError(
                Status.INVALID_ARGUMENT.withDescription(illegalArgumentException.getMessage())
                    .asRuntimeException());

            return;
        }

        responseObserver.onNext(
            CreateConnectedUserResponse.newBuilder()
                .setConnectedUser(ConnectedUserProtoMapper.toRemoteConnectedUser(connectedUser))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void fetchConnectedUserByExternalId(
        FetchConnectedUserByExternalIdRequest request, StreamObserver<FetchConnectedUserResponse> responseObserver) {

        Optional<ConnectedUser> connectedUser = connectedUserService.fetchConnectedUser(
            request.getExternalId(), request.getEnvironmentId());

        FetchConnectedUserResponse.Builder builder = FetchConnectedUserResponse.newBuilder();

        connectedUser.ifPresent(presentConnectedUser -> builder.setConnectedUser(
            ConnectedUserProtoMapper.toRemoteConnectedUser(presentConnectedUser)));

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}

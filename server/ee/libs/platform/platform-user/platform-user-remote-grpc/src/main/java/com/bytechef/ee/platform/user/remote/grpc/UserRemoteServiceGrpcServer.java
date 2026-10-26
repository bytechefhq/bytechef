/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.remote.grpc;

import com.bytechef.ee.platform.user.remote.grpc.proto.FetchUserByEmailRequest;
import com.bytechef.ee.platform.user.remote.grpc.proto.FetchUserByIdRequest;
import com.bytechef.ee.platform.user.remote.grpc.proto.FetchUserByLoginRequest;
import com.bytechef.ee.platform.user.remote.grpc.proto.FetchUserResponse;
import com.bytechef.ee.platform.user.remote.grpc.proto.UserRemoteServiceGrpc;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.Optional;

/**
 * Serves user lookups to the other microservices.
 *
 * @version ee
 */
public class UserRemoteServiceGrpcServer extends UserRemoteServiceGrpc.UserRemoteServiceImplBase {

    private final UserService userService;

    @SuppressFBWarnings("EI")
    public UserRemoteServiceGrpcServer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void fetchUserByEmail(
        FetchUserByEmailRequest request, StreamObserver<FetchUserResponse> responseObserver) {

        complete(userService.fetchUserByEmail(request.getEmail()), responseObserver);
    }

    @Override
    public void fetchUserById(FetchUserByIdRequest request, StreamObserver<FetchUserResponse> responseObserver) {
        complete(userService.fetchUser(request.getId()), responseObserver);
    }

    @Override
    public void fetchUserByLogin(
        FetchUserByLoginRequest request, StreamObserver<FetchUserResponse> responseObserver) {

        complete(userService.fetchUserByLogin(request.getLogin()), responseObserver);
    }

    private static void complete(Optional<User> user, StreamObserver<FetchUserResponse> responseObserver) {
        FetchUserResponse.Builder builder = FetchUserResponse.newBuilder();

        user.ifPresent(presentUser -> builder.setUser(UserProtoMapper.toRemoteUser(presentUser)));

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}

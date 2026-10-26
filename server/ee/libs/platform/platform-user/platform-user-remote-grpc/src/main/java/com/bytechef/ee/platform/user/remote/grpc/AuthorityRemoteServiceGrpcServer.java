/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.remote.grpc;

import com.bytechef.ee.platform.user.remote.grpc.proto.AuthorityRemoteServiceGrpc;
import com.bytechef.ee.platform.user.remote.grpc.proto.FetchAuthorityRequest;
import com.bytechef.ee.platform.user.remote.grpc.proto.FetchAuthorityResponse;
import com.bytechef.ee.platform.user.remote.grpc.proto.GetAuthoritiesRequest;
import com.bytechef.ee.platform.user.remote.grpc.proto.GetAuthoritiesResponse;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.service.AuthorityService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.Optional;

/**
 * Serves authority lookups to the other microservices.
 *
 * @version ee
 */
public class AuthorityRemoteServiceGrpcServer extends AuthorityRemoteServiceGrpc.AuthorityRemoteServiceImplBase {

    private final AuthorityService authorityService;

    @SuppressFBWarnings("EI")
    public AuthorityRemoteServiceGrpcServer(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    @Override
    public void fetchAuthority(
        FetchAuthorityRequest request, StreamObserver<FetchAuthorityResponse> responseObserver) {

        Optional<Authority> authority = authorityService.fetchAuthority(request.getId());

        FetchAuthorityResponse.Builder builder = FetchAuthorityResponse.newBuilder();

        authority
            .ifPresent(presentAuthority -> builder.setAuthority(UserProtoMapper.toRemoteAuthority(presentAuthority)));

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getAuthorities(
        GetAuthoritiesRequest request, StreamObserver<GetAuthoritiesResponse> responseObserver) {

        GetAuthoritiesResponse.Builder builder = GetAuthoritiesResponse.newBuilder();

        for (Authority authority : authorityService.getAuthorities()) {
            builder.addAuthorities(UserProtoMapper.toRemoteAuthority(authority));
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}

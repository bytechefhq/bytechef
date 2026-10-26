/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.remote.client.service;

import com.bytechef.ee.platform.user.remote.grpc.UserProtoMapper;
import com.bytechef.ee.platform.user.remote.grpc.proto.AuthorityRemoteServiceGrpc;
import com.bytechef.ee.platform.user.remote.grpc.proto.FetchAuthorityRequest;
import com.bytechef.ee.platform.user.remote.grpc.proto.FetchAuthorityResponse;
import com.bytechef.ee.platform.user.remote.grpc.proto.GetAuthoritiesRequest;
import com.bytechef.ee.platform.user.remote.grpc.proto.GetAuthoritiesResponse;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.service.AuthorityService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteAuthorityServiceClient implements AuthorityService {

    private static final String SERVICE_ID = "api-gateway-app";

    private final GrpcChannels grpcChannels;

    public RemoteAuthorityServiceClient(GrpcChannels grpcChannels) {
        this.grpcChannels = grpcChannels;
    }

    @Override
    public Authority create(Authority authority) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Authority> fetchAuthority(long id) {
        FetchAuthorityResponse fetchAuthorityResponse = getStub().fetchAuthority(
            FetchAuthorityRequest.newBuilder()
                .setId(id)
                .build());

        if (!fetchAuthorityResponse.hasAuthority()) {
            return Optional.empty();
        }

        return Optional.of(UserProtoMapper.toAuthority(fetchAuthorityResponse.getAuthority()));
    }

    @Override
    public List<Authority> getAuthorities() {
        GetAuthoritiesResponse getAuthoritiesResponse =
            getStub().getAuthorities(GetAuthoritiesRequest.getDefaultInstance());

        return getAuthoritiesResponse.getAuthoritiesList()
            .stream()
            .map(UserProtoMapper::toAuthority)
            .toList();
    }

    @Override
    public boolean exists(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Authority update(Authority authority) {
        throw new UnsupportedOperationException();
    }

    private AuthorityRemoteServiceGrpc.AuthorityRemoteServiceBlockingStub getStub() {
        return AuthorityRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(SERVICE_ID));
    }
}

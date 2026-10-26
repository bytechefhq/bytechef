/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.remote.client.service;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.remote.grpc.ConnectedUserProtoMapper;
import com.bytechef.ee.embedded.connected.user.remote.grpc.proto.ConnectedUserRemoteServiceGrpc;
import com.bytechef.ee.embedded.connected.user.remote.grpc.proto.CreateConnectedUserRequest;
import com.bytechef.ee.embedded.connected.user.remote.grpc.proto.CreateConnectedUserResponse;
import com.bytechef.ee.embedded.connected.user.remote.grpc.proto.FetchConnectedUserByExternalIdRequest;
import com.bytechef.ee.embedded.connected.user.remote.grpc.proto.FetchConnectedUserResponse;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteConnectedUserServiceClient implements ConnectedUserService {

    private static final String SERVICE_ID = "configuration-app";

    private final GrpcChannels grpcChannels;

    public RemoteConnectedUserServiceClient(GrpcChannels grpcChannels) {
        this.grpcChannels = grpcChannels;
    }

    @Override
    public ConnectedUser createConnectedUser(String externalId, long environmentId) {
        CreateConnectedUserResponse createConnectedUserResponse;

        try {
            createConnectedUserResponse = getStub().createConnectedUser(
                CreateConnectedUserRequest.newBuilder()
                    .setEnvironmentId(environmentId)
                    .setExternalId(externalId)
                    .build());
        } catch (StatusRuntimeException statusRuntimeException) {
            Status status = statusRuntimeException.getStatus();

            if (status.getCode() == Status.Code.INVALID_ARGUMENT) {
                throw new IllegalArgumentException(status.getDescription(), statusRuntimeException);
            }

            throw statusRuntimeException;
        }

        return ConnectedUserProtoMapper.toConnectedUser(createConnectedUserResponse.getConnectedUser());
    }

    @Override
    public ConnectedUser createConnectedUser(String externalId, Environment environment) {
        return createConnectedUser(externalId, environment.ordinal());
    }

    @Override
    public ConnectedUser createConnectedUser(
        String externalId, Environment environment, @Nullable String name, @Nullable String email,
        @Nullable Map<String, String> metadata) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteConnectedUser(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void enableConnectedUser(long id, boolean enable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<ConnectedUser> fetchConnectedUser(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<ConnectedUser> fetchConnectedUser(String externalId, long environmentId) {
        FetchConnectedUserResponse fetchConnectedUserResponse = getStub().fetchConnectedUserByExternalId(
            FetchConnectedUserByExternalIdRequest.newBuilder()
                .setEnvironmentId(environmentId)
                .setExternalId(externalId)
                .build());

        if (!fetchConnectedUserResponse.hasConnectedUser()) {
            return Optional.empty();
        }

        return Optional.of(ConnectedUserProtoMapper.toConnectedUser(fetchConnectedUserResponse.getConnectedUser()));
    }

    @Override
    public Optional<ConnectedUser> fetchConnectedUser(String externalId, Environment environment) {
        return fetchConnectedUser(externalId, environment.ordinal());
    }

    @Override
    public ConnectedUser getConnectedUser(String externalId, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectedUser getConnectedUser(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Page<ConnectedUser> getConnectedUsers(
        Environment environment, String name, LocalDate createDateFrom, LocalDate createDateTo, Long integrationId,
        int pageNumber) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<ConnectedUser> getConnectedUsers(List<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateConnectedUser(String externalUserId, Environment environment, Map<String, Object> metadata) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectedUser updateConnectedUser(
        long id, @Nullable String name, @Nullable String email, @Nullable Map<String, String> metadata, int version) {

        throw new UnsupportedOperationException();
    }

    private ConnectedUserRemoteServiceGrpc.ConnectedUserRemoteServiceBlockingStub getStub() {
        return ConnectedUserRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(SERVICE_ID));
    }
}

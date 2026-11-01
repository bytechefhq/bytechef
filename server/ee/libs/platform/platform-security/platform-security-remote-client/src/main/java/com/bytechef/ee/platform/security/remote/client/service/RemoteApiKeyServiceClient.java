/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.remote.client.service;

import com.bytechef.ee.platform.security.remote.grpc.ApiKeyProtoMapper;
import com.bytechef.ee.platform.security.remote.grpc.proto.ApiKeyRemoteServiceGrpc;
import com.bytechef.ee.platform.security.remote.grpc.proto.ExistsRequest;
import com.bytechef.ee.platform.security.remote.grpc.proto.FetchApiKeyBySecretKeyAndEnvironmentRequest;
import com.bytechef.ee.platform.security.remote.grpc.proto.FetchApiKeyBySecretKeyRequest;
import com.bytechef.ee.platform.security.remote.grpc.proto.FetchApiKeyResponse;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Looks up API keys in the configuration microservice over gRPC. Only the lookups API key authentication needs are
 * remote.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
public class RemoteApiKeyServiceClient implements ApiKeyService {

    private static final String SERVICE_ID = "configuration-app";

    private final GrpcChannels grpcChannels;

    public RemoteApiKeyServiceClient(GrpcChannels grpcChannels) {
        this.grpcChannels = grpcChannels;
    }

    @Override
    public ApiKey create(ApiKey apiKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists(String secretKey, long environmentId) {
        return getStub().exists(
            ExistsRequest.newBuilder()
                .setEnvironmentId(environmentId)
                .setSecretKey(secretKey)
                .build())
            .getExists();
    }

    @Override
    public Optional<ApiKey> fetchApiKey(String secretKey) {
        return toApiKey(
            getStub().fetchApiKeyBySecretKey(
                FetchApiKeyBySecretKeyRequest.newBuilder()
                    .setSecretKey(secretKey)
                    .build()));
    }

    @Override
    public Optional<ApiKey> fetchApiKey(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ApiKey getApiKey(String secretKey) {
        Optional<ApiKey> apiKey = fetchApiKey(secretKey);

        return apiKey.orElseThrow(() -> new IllegalArgumentException("Api key not found."));
    }

    @Override
    public ApiKey getApiKey(String secretKey, long environmentId) {
        Optional<ApiKey> apiKey = toApiKey(
            getStub().fetchApiKeyBySecretKeyAndEnvironment(
                FetchApiKeyBySecretKeyAndEnvironmentRequest.newBuilder()
                    .setEnvironmentId(environmentId)
                    .setSecretKey(secretKey)
                    .build()));

        return apiKey.orElseThrow(
            () -> new IllegalArgumentException("Api key not found for the specified environment."));
    }

    @Override
    public ApiKey getApiKey(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ApiKey> getApiKeys(long environmentId, @Nullable PlatformType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ApiKey update(ApiKey apiKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateLastUsedDate(long id) {
        throw new UnsupportedOperationException();
    }

    private ApiKeyRemoteServiceGrpc.ApiKeyRemoteServiceBlockingStub getStub() {
        return ApiKeyRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(SERVICE_ID));
    }

    private static Optional<ApiKey> toApiKey(FetchApiKeyResponse fetchApiKeyResponse) {
        if (!fetchApiKeyResponse.hasApiKey()) {
            return Optional.empty();
        }

        return Optional.of(ApiKeyProtoMapper.toApiKey(fetchApiKeyResponse.getApiKey()));
    }
}

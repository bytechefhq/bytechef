/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.remote.grpc;

import com.bytechef.ee.platform.security.remote.grpc.proto.ApiKeyRemoteServiceGrpc;
import com.bytechef.ee.platform.security.remote.grpc.proto.ExistsRequest;
import com.bytechef.ee.platform.security.remote.grpc.proto.ExistsResponse;
import com.bytechef.ee.platform.security.remote.grpc.proto.FetchApiKeyBySecretKeyAndEnvironmentRequest;
import com.bytechef.ee.platform.security.remote.grpc.proto.FetchApiKeyBySecretKeyRequest;
import com.bytechef.ee.platform.security.remote.grpc.proto.FetchApiKeyResponse;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.Optional;

/**
 * Serves API key lookups to the microservices that authenticate API key requests.
 *
 * @version ee
 */
public class ApiKeyRemoteServiceGrpcServer extends ApiKeyRemoteServiceGrpc.ApiKeyRemoteServiceImplBase {

    private final ApiKeyService apiKeyService;

    @SuppressFBWarnings("EI")
    public ApiKeyRemoteServiceGrpcServer(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    public void exists(ExistsRequest request, StreamObserver<ExistsResponse> responseObserver) {
        responseObserver.onNext(
            ExistsResponse.newBuilder()
                .setExists(apiKeyService.exists(request.getSecretKey(), request.getEnvironmentId()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void fetchApiKeyBySecretKey(
        FetchApiKeyBySecretKeyRequest request, StreamObserver<FetchApiKeyResponse> responseObserver) {

        complete(apiKeyService.fetchApiKey(request.getSecretKey()), responseObserver);
    }

    @Override
    public void fetchApiKeyBySecretKeyAndEnvironment(
        FetchApiKeyBySecretKeyAndEnvironmentRequest request, StreamObserver<FetchApiKeyResponse> responseObserver) {

        Optional<ApiKey> apiKey;

        try {
            apiKey = Optional.of(apiKeyService.getApiKey(request.getSecretKey(), request.getEnvironmentId()));
        } catch (IllegalArgumentException illegalArgumentException) {
            apiKey = Optional.empty();
        }

        complete(apiKey, responseObserver);
    }

    private static void complete(Optional<ApiKey> apiKey, StreamObserver<FetchApiKeyResponse> responseObserver) {
        FetchApiKeyResponse.Builder builder = FetchApiKeyResponse.newBuilder();

        apiKey.ifPresent(presentApiKey -> builder.setApiKey(ApiKeyProtoMapper.toRemoteApiKey(presentApiKey)));

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.storage.jdbc.remote.grpc;

import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.DataStorageKeyRequest;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.DataStorageScopeRequest;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.DeleteScopeDataRequest;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.FetchDataStorageValueResponse;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.JdbcDataStorageRemoteServiceGrpc;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.JsonValue;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.PutDataStorageValueRequest;
import com.bytechef.ee.remote.grpc.JsonBytes;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.jdbc.service.JdbcDataStorageService;
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import java.util.Optional;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class JdbcDataStorageRemoteServiceGrpcServer
    extends JdbcDataStorageRemoteServiceGrpc.JdbcDataStorageRemoteServiceImplBase {

    private final JdbcDataStorageService jdbcDataStorageService;
    private final JsonBytes jsonBytes;

    @SuppressFBWarnings("EI")
    public JdbcDataStorageRemoteServiceGrpcServer(JdbcDataStorageService jdbcDataStorageService,
        ObjectMapper objectMapper) {
        this.jdbcDataStorageService = jdbcDataStorageService;
        this.jsonBytes = new JsonBytes(objectMapper);
    }

    @Override
    public void delete(DataStorageKeyRequest request, StreamObserver<Empty> responseObserver) {
        jdbcDataStorageService.delete(
            request.getComponentName(), DataStorageScope.valueOf(request.getScope()), request.getScopeId(),
            request.getKey(), request.getEnvironmentId(), toPlatformType(request.hasType(), request.getType()));

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void deleteScopeData(DeleteScopeDataRequest request, StreamObserver<Empty> responseObserver) {
        jdbcDataStorageService.deleteScopeData(DataStorageScope.valueOf(request.getScope()), request.getScopeId());

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void fetch(DataStorageKeyRequest request, StreamObserver<FetchDataStorageValueResponse> responseObserver) {
        Optional<Object> value = jdbcDataStorageService.fetch(
            request.getComponentName(), DataStorageScope.valueOf(request.getScope()), request.getScopeId(),
            request.getKey(), request.getEnvironmentId(), toPlatformType(request.hasType(), request.getType()));

        FetchDataStorageValueResponse.Builder builder = FetchDataStorageValueResponse.newBuilder();

        value.ifPresent(curValue -> builder.setValue(jsonBytes.write(curValue)));

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getAll(DataStorageScopeRequest request, StreamObserver<JsonValue> responseObserver) {
        Map<String, Object> values = jdbcDataStorageService.getAll(
            request.getComponentName(), DataStorageScope.valueOf(request.getScope()), request.getScopeId(),
            request.getEnvironmentId(), toPlatformType(request.hasType(), request.getType()));

        responseObserver.onNext(
            JsonValue.newBuilder()
                .setValue(jsonBytes.write(values))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void put(PutDataStorageValueRequest request, StreamObserver<Empty> responseObserver) {
        DataStorageKeyRequest key = request.getKey();

        jdbcDataStorageService.put(
            key.getComponentName(), DataStorageScope.valueOf(key.getScope()), key.getScopeId(), key.getKey(),
            key.getEnvironmentId(), toPlatformType(key.hasType(), key.getType()),
            jsonBytes.read(request.getValue()));

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    private static PlatformType toPlatformType(boolean hasType, String type) {
        return hasType ? PlatformType.valueOf(type) : null;
    }
}

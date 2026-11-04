/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.storage.db.remote.client.service;

import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.DataStorageEntryVersion;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.DataStorageKeyRequest;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.DataStorageScopeRequest;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.DeleteScopeDataRequest;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.FetchDataStorageValueResponse;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.FetchVersionedDataStorageValueResponse;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.JdbcDataStorageRemoteServiceGrpc;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.JdbcDataStorageRemoteServiceGrpc.JdbcDataStorageRemoteServiceBlockingStub;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.JsonValue;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.PutDataStorageValueRequest;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.PutIfVersionDataStorageValueRequest;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.PutIfVersionDataStorageValueResponse;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.VersionedDataStorageValue;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.ee.remote.grpc.JsonBytes;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.jdbc.domain.DataEntryVersion;
import com.bytechef.platform.data.storage.jdbc.domain.VersionedValue;
import com.bytechef.platform.data.storage.jdbc.service.JdbcDataStorageService;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class RemoteJdbcDataStorageServiceClient implements JdbcDataStorageService {

    private static final String EXECUTION_APP = "execution-app";

    private final GrpcChannels grpcChannels;
    private final JsonBytes jsonBytes;

    public RemoteJdbcDataStorageServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.grpcChannels = grpcChannels;
        this.jsonBytes = new JsonBytes(objectMapper);
    }

    @Override
    public void delete(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type) {

        getStub().delete(toDataStorageKeyRequest(componentName, scope, scopeId, key, environmentId, type));
    }

    @Override
    public void deleteScopeData(DataStorageScope scope, String scopeId, PlatformType type) {
        DeleteScopeDataRequest deleteScopeDataRequest = DeleteScopeDataRequest.newBuilder()
            .setScope(scope.name())
            .setScopeId(scopeId)
            .setType(type.name())
            .build();

        getStub().deleteScopeData(deleteScopeDataRequest);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> fetch(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type) {

        FetchDataStorageValueResponse fetchDataStorageValueResponse = getStub().fetch(
            toDataStorageKeyRequest(componentName, scope, scopeId, key, environmentId, type));

        if (!fetchDataStorageValueResponse.hasValue()) {
            return Optional.empty();
        }

        return Optional.ofNullable((T) jsonBytes.read(fetchDataStorageValueResponse.getValue()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<VersionedValue<T>> fetchVersioned(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type) {

        FetchVersionedDataStorageValueResponse fetchVersionedDataStorageValueResponse = getStub().fetchVersioned(
            toDataStorageKeyRequest(componentName, scope, scopeId, key, environmentId, type));

        if (!fetchVersionedDataStorageValueResponse.hasVersionedValue()) {
            return Optional.empty();
        }

        VersionedDataStorageValue versionedDataStorageValue =
            fetchVersionedDataStorageValueResponse.getVersionedValue();
        DataStorageEntryVersion dataStorageEntryVersion = versionedDataStorageValue.getVersion();

        return Optional.of(
            new VersionedValue<>(
                (T) jsonBytes.read(versionedDataStorageValue.getValue()),
                new DataEntryVersion(dataStorageEntryVersion.getId(), dataStorageEntryVersion.getVersion())));
    }

    @Override
    public <T> T get(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type) {

        Optional<T> value = fetch(componentName, scope, scopeId, key, environmentId, type);

        return value.orElseThrow(() -> new IllegalArgumentException("No data storage value for key " + key));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getAll(
        String componentName, DataStorageScope scope, String scopeId, long environmentId, PlatformType type) {

        DataStorageScopeRequest.Builder builder = DataStorageScopeRequest.newBuilder()
            .setComponentName(componentName)
            .setScope(scope.name())
            .setScopeId(scopeId)
            .setEnvironmentId(environmentId);

        if (type != null) {
            builder.setType(type.name());
        }

        JsonValue jsonValue = getStub().getAll(builder.build());

        return (Map<String, T>) jsonBytes.readMap(jsonValue.getValue());
    }

    @Override
    public void put(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type, Object value) {

        PutDataStorageValueRequest putDataStorageValueRequest = PutDataStorageValueRequest.newBuilder()
            .setKey(toDataStorageKeyRequest(componentName, scope, scopeId, key, environmentId, type))
            .setValue(jsonBytes.write(value))
            .build();

        getStub().put(putDataStorageValueRequest);
    }

    @Override
    public boolean putIfVersion(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type, Object value, @Nullable DataEntryVersion expectedVersion) {

        PutIfVersionDataStorageValueRequest.Builder builder = PutIfVersionDataStorageValueRequest.newBuilder()
            .setKey(toDataStorageKeyRequest(componentName, scope, scopeId, key, environmentId, type))
            .setValue(jsonBytes.write(value));

        if (expectedVersion != null) {
            builder.setExpectedVersion(
                DataStorageEntryVersion.newBuilder()
                    .setId(expectedVersion.id())
                    .setVersion(expectedVersion.version())
                    .build());
        }

        PutIfVersionDataStorageValueResponse putIfVersionDataStorageValueResponse = getStub().putIfVersion(
            builder.build());

        return putIfVersionDataStorageValueResponse.getWritten();
    }

    private JdbcDataStorageRemoteServiceBlockingStub getStub() {
        return JdbcDataStorageRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(EXECUTION_APP));
    }

    private static DataStorageKeyRequest toDataStorageKeyRequest(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type) {

        DataStorageKeyRequest.Builder builder = DataStorageKeyRequest.newBuilder()
            .setComponentName(componentName)
            .setScope(scope.name())
            .setScopeId(scopeId)
            .setKey(key)
            .setEnvironmentId(environmentId);

        if (type != null) {
            builder.setType(type.name());
        }

        return builder.build();
    }
}

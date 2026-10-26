/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.storage.db.remote.client.service;

import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.DataStorageKeyRequest;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.DataStorageScopeRequest;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.DeleteScopeDataRequest;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.FetchDataStorageValueResponse;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.JdbcDataStorageRemoteServiceGrpc;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.JdbcDataStorageRemoteServiceGrpc.JdbcDataStorageRemoteServiceBlockingStub;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.proto.PutDataStorageValueRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.ee.remote.grpc.ProtoValues;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.jdbc.service.JdbcDataStorageService;
import java.util.Map;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class RemoteJdbcDataStorageServiceClient implements JdbcDataStorageService {

    private static final String EXECUTION_APP = "execution-app";

    private final GrpcChannels grpcChannels;

    public RemoteJdbcDataStorageServiceClient(GrpcChannels grpcChannels) {
        this.grpcChannels = grpcChannels;
    }

    @Override
    public void delete(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type) {

        getStub().delete(toDataStorageKeyRequest(componentName, scope, scopeId, key, environmentId, type));
    }

    @Override
    public void deleteScopeData(DataStorageScope scope, String scopeId) {
        DeleteScopeDataRequest deleteScopeDataRequest = DeleteScopeDataRequest.newBuilder()
            .setScope(scope.name())
            .setScopeId(scopeId)
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

        return Optional.ofNullable((T) ProtoValues.toObject(fetchDataStorageValueResponse.getValue()));
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

        return (Map<String, T>) ProtoValues.toMap(getStub().getAll(builder.build()));
    }

    @Override
    public void put(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type, Object value) {

        PutDataStorageValueRequest putDataStorageValueRequest = PutDataStorageValueRequest.newBuilder()
            .setKey(toDataStorageKeyRequest(componentName, scope, scopeId, key, environmentId, type))
            .setValue(ProtoValues.toValue(value))
            .build();

        getStub().put(putDataStorageValueRequest);
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

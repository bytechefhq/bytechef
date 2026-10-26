/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.client.service;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.platform.connection.remote.grpc.ConnectionProtoMapper;
import com.bytechef.ee.platform.connection.remote.grpc.proto.ConnectionParametersRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.ConnectionRemoteServiceGrpc;
import com.bytechef.ee.platform.connection.remote.grpc.proto.ConnectionRemoteServiceGrpc.ConnectionRemoteServiceBlockingStub;
import com.bytechef.ee.platform.connection.remote.grpc.proto.ConnectionResult;
import com.bytechef.ee.platform.connection.remote.grpc.proto.ConnectionStatusRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.CreateConnectionRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.FindConnectionsRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.GetComponentConnectionsRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.IdRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.IdsRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.TypeRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.UpdateConnectionRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.UpdateConnectionTagIdsRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.ee.remote.grpc.ProtoValues;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteConnectionServiceClient implements ConnectionService {

    private static final String CONNECTION_APP = "connection-app";

    private final ConnectionProtoMapper connectionProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteConnectionServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.connectionProtoMapper = new ConnectionProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public Connection create(Connection connection) {
        return connectionProtoMapper.toConnection(getStub().create(connectionProtoMapper.toProto(connection)));
    }

    @Override
    public Connection create(
        @Nullable AuthorizationType authorizationType, String componentName, int connectionVersion,
        int environmentId, String name, Map<String, Object> parameters, PlatformType type) {

        CreateConnectionRequest.Builder builder = CreateConnectionRequest.newBuilder()
            .setComponentName(componentName)
            .setConnectionVersion(connectionVersion)
            .setEnvironmentId(environmentId)
            .setName(name)
            .setParameters(ProtoValues.toStruct(parameters));

        if (authorizationType != null) {
            builder.setAuthorizationType(authorizationType.name());
        }

        if (type != null) {
            builder.setType(type.name());
        }

        return connectionProtoMapper.toConnection(getStub().createWithParameters(builder.build()));
    }

    @Override
    public void delete(long id) {
        getStub().delete(toIdRequest(id));
    }

    @Override
    public Connection getConnection(long id) {
        return connectionProtoMapper.toConnection(getStub().getConnection(toIdRequest(id)));
    }

    @Override
    public Optional<Connection> fetchConnection(long id) {
        ConnectionResult connectionResult = getStub().fetchConnection(toIdRequest(id));

        if (!connectionResult.hasConnection()) {
            return Optional.empty();
        }

        return Optional.of(connectionProtoMapper.toConnection(connectionResult.getConnection()));
    }

    @Override
    public List<Connection> getConnections(PlatformType type) {
        TypeRequest.Builder builder = TypeRequest.newBuilder();

        if (type != null) {
            builder.setType(type.name());
        }

        return connectionProtoMapper.toConnections(getStub().getConnectionsByType(builder.build()));
    }

    @Override
    public List<Connection> getConnections(String componentName, int version, PlatformType type) {
        GetComponentConnectionsRequest.Builder builder = GetComponentConnectionsRequest.newBuilder()
            .setComponentName(componentName)
            .setVersion(version);

        if (type != null) {
            builder.setType(type.name());
        }

        return connectionProtoMapper.toConnections(getStub().getComponentConnections(builder.build()));
    }

    @Override
    public List<Connection> getConnections(
        String componentName, Integer connectionVersion, Long tagId, Long environmentId, PlatformType type) {

        FindConnectionsRequest.Builder builder = FindConnectionsRequest.newBuilder();

        if (componentName != null) {
            builder.setComponentName(componentName);
        }

        if (connectionVersion != null) {
            builder.setConnectionVersion(connectionVersion);
        }

        if (tagId != null) {
            builder.setTagId(tagId);
        }

        if (environmentId != null) {
            builder.setEnvironmentId(environmentId);
        }

        if (type != null) {
            builder.setType(type.name());
        }

        return connectionProtoMapper.toConnections(getStub().findConnections(builder.build()));
    }

    @Override
    public List<Connection> getConnections(List<Long> connectionIds) {
        IdsRequest idsRequest = IdsRequest.newBuilder()
            .addAllIds(connectionIds)
            .build();

        return connectionProtoMapper.toConnections(getStub().getConnectionsByIds(idsRequest));
    }

    @Override
    public Connection update(long id, List<Long> tagIds) {
        UpdateConnectionTagIdsRequest updateConnectionTagIdsRequest = UpdateConnectionTagIdsRequest.newBuilder()
            .setId(id)
            .addAllTagIds(tagIds)
            .build();

        return connectionProtoMapper.toConnection(getStub().updateTagIds(updateConnectionTagIdsRequest));
    }

    @Override
    public Connection update(long id, String name, List<Long> tagIds, int version) {
        UpdateConnectionRequest updateConnectionRequest = UpdateConnectionRequest.newBuilder()
            .setId(id)
            .setName(name)
            .addAllTagIds(tagIds)
            .setVersion(version)
            .build();

        return connectionProtoMapper.toConnection(getStub().update(updateConnectionRequest));
    }

    @Override
    public Connection updateConnectionCredentialStatus(long connectionId, Connection.CredentialStatus status) {
        return connectionProtoMapper.toConnection(
            getStub().updateCredentialStatus(toConnectionStatusRequest(connectionId, status.name())));
    }

    @Override
    public Connection updateConnectionStatus(long connectionId, ConnectionStatus status) {
        return connectionProtoMapper.toConnection(
            getStub().updateStatus(toConnectionStatusRequest(connectionId, status.name())));
    }

    @Override
    public Connection updateCreatedBy(long id, String newCreatedBy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection updateConnectionParameters(long connectionId, Map<String, ?> parameters) {
        return connectionProtoMapper.toConnection(
            getStub().updateParameters(toConnectionParametersRequest(connectionId, parameters)));
    }

    @Override
    public Connection replaceConnectionParameters(long connectionId, Map<String, ?> parameters) {
        return connectionProtoMapper.toConnection(
            getStub().replaceParameters(toConnectionParametersRequest(connectionId, parameters)));
    }

    private ConnectionRemoteServiceBlockingStub getStub() {
        return ConnectionRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(CONNECTION_APP));
    }

    private static ConnectionParametersRequest toConnectionParametersRequest(
        long connectionId, Map<String, ?> parameters) {

        return ConnectionParametersRequest.newBuilder()
            .setConnectionId(connectionId)
            .setParameters(ProtoValues.toStruct(parameters))
            .build();
    }

    private static ConnectionStatusRequest toConnectionStatusRequest(long connectionId, String status) {
        return ConnectionStatusRequest.newBuilder()
            .setConnectionId(connectionId)
            .setStatus(status)
            .build();
    }

    private static IdRequest toIdRequest(long id) {
        return IdRequest.newBuilder()
            .setId(id)
            .build();
    }
}

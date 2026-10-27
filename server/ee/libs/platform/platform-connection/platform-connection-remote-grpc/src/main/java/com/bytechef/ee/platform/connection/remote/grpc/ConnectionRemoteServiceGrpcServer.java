/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.grpc;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.platform.connection.remote.grpc.proto.ConnectionParametersRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.ConnectionRemoteServiceGrpc;
import com.bytechef.ee.platform.connection.remote.grpc.proto.ConnectionResult;
import com.bytechef.ee.platform.connection.remote.grpc.proto.ConnectionStatusRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.CreateConnectionRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.FindConnectionsRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.GetComponentConnectionsRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.IdRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.IdsRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.RemoteConnection;
import com.bytechef.ee.platform.connection.remote.grpc.proto.RemoteConnections;
import com.bytechef.ee.platform.connection.remote.grpc.proto.TypeRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.UpdateConnectionRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.UpdateConnectionTagIdsRequest;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ConnectionRemoteServiceGrpcServer extends ConnectionRemoteServiceGrpc.ConnectionRemoteServiceImplBase {

    private final ConnectionProtoMapper connectionProtoMapper;
    private final ConnectionService connectionService;

    @SuppressFBWarnings("EI")
    public ConnectionRemoteServiceGrpcServer(
        ConnectionService connectionService, ConnectionProtoMapper connectionProtoMapper) {

        this.connectionProtoMapper = connectionProtoMapper;
        this.connectionService = connectionService;
    }

    @Override
    public void create(RemoteConnection request, StreamObserver<RemoteConnection> responseObserver) {
        reply(connectionService.create(connectionProtoMapper.toConnection(request)), responseObserver);
    }

    @Override
    public void createWithParameters(
        CreateConnectionRequest request, StreamObserver<RemoteConnection> responseObserver) {

        reply(
            connectionService.create(
                request.hasAuthorizationType() ? AuthorizationType.valueOf(request.getAuthorizationType()) : null,
                request.getComponentName(), request.getConnectionVersion(), request.getEnvironmentId(),
                request.getName(), connectionProtoMapper.toMap(request.getParameters()),
                request.hasType() ? PlatformType.valueOf(request.getType()) : null),
            responseObserver);
    }

    @Override
    public void delete(IdRequest request, StreamObserver<Empty> responseObserver) {
        connectionService.delete(request.getId());

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void fetchConnection(IdRequest request, StreamObserver<ConnectionResult> responseObserver) {
        Optional<Connection> connection = connectionService.fetchConnection(request.getId());

        ConnectionResult.Builder builder = ConnectionResult.newBuilder();

        connection.ifPresent(curConnection -> builder.setConnection(connectionProtoMapper.toProto(curConnection)));

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void findConnections(FindConnectionsRequest request, StreamObserver<RemoteConnections> responseObserver) {
        replyList(
            connectionService.getConnections(
                request.hasComponentName() ? request.getComponentName() : null,
                request.hasConnectionVersion() ? request.getConnectionVersion() : null,
                request.hasTagId() ? request.getTagId() : null,
                request.hasEnvironmentId() ? request.getEnvironmentId() : null,
                request.hasType() ? PlatformType.valueOf(request.getType()) : null),
            responseObserver);
    }

    @Override
    public void getComponentConnections(
        GetComponentConnectionsRequest request, StreamObserver<RemoteConnections> responseObserver) {

        replyList(
            connectionService.getConnections(
                request.getComponentName(), request.getVersion(),
                request.hasType() ? PlatformType.valueOf(request.getType()) : null),
            responseObserver);
    }

    @Override
    public void getConnection(IdRequest request, StreamObserver<RemoteConnection> responseObserver) {
        reply(connectionService.getConnection(request.getId()), responseObserver);
    }

    @Override
    public void getConnectionsByIds(IdsRequest request, StreamObserver<RemoteConnections> responseObserver) {
        replyList(connectionService.getConnections(List.copyOf(request.getIdsList())), responseObserver);
    }

    @Override
    public void getConnectionsByType(TypeRequest request, StreamObserver<RemoteConnections> responseObserver) {
        replyList(
            connectionService.getConnections(request.hasType() ? PlatformType.valueOf(request.getType()) : null),
            responseObserver);
    }

    @Override
    public void replaceParameters(
        ConnectionParametersRequest request, StreamObserver<RemoteConnection> responseObserver) {

        reply(
            connectionService.replaceConnectionParameters(
                request.getConnectionId(), connectionProtoMapper.toMap(request.getParameters())),
            responseObserver);
    }

    @Override
    public void update(UpdateConnectionRequest request, StreamObserver<RemoteConnection> responseObserver) {
        reply(
            connectionService.update(
                request.getId(), request.hasName() ? request.getName() : null,
                request.getKeepTagIds() ? null : List.copyOf(request.getTagIdsList()), request.getVersion()),
            responseObserver);
    }

    @Override
    public void updateCredentialStatus(
        ConnectionStatusRequest request, StreamObserver<RemoteConnection> responseObserver) {

        reply(
            connectionService.updateConnectionCredentialStatus(
                request.getConnectionId(), Connection.CredentialStatus.valueOf(request.getStatus())),
            responseObserver);
    }

    @Override
    public void updateParameters(
        ConnectionParametersRequest request, StreamObserver<RemoteConnection> responseObserver) {

        reply(
            connectionService.updateConnectionParameters(
                request.getConnectionId(), connectionProtoMapper.toMap(request.getParameters())),
            responseObserver);
    }

    @Override
    public void updateStatus(ConnectionStatusRequest request, StreamObserver<RemoteConnection> responseObserver) {
        reply(
            connectionService.updateConnectionStatus(
                request.getConnectionId(), ConnectionStatus.valueOf(request.getStatus())),
            responseObserver);
    }

    @Override
    public void updateTagIds(UpdateConnectionTagIdsRequest request, StreamObserver<RemoteConnection> responseObserver) {
        reply(connectionService.update(request.getId(), List.copyOf(request.getTagIdsList())), responseObserver);
    }

    private void reply(Connection connection, StreamObserver<RemoteConnection> responseObserver) {
        responseObserver.onNext(connectionProtoMapper.toProto(connection));
        responseObserver.onCompleted();
    }

    private void replyList(List<Connection> connections, StreamObserver<RemoteConnections> responseObserver) {
        responseObserver.onNext(connectionProtoMapper.toRemoteConnections(connections));
        responseObserver.onCompleted();
    }
}

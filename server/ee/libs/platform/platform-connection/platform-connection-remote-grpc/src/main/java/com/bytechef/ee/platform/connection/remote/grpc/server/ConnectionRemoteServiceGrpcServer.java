/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.grpc.server;

import com.bytechef.ee.platform.connection.remote.grpc.ConnectionListProto;
import com.bytechef.ee.platform.connection.remote.grpc.ConnectionProto;
import com.bytechef.ee.platform.connection.remote.grpc.ConnectionRemoteServiceGrpc;
import com.bytechef.ee.platform.connection.remote.grpc.GetConnectionRequest;
import com.bytechef.ee.platform.connection.remote.grpc.GetConnectionsRequest;
import com.bytechef.ee.platform.connection.remote.grpc.PlatformTypeProto;
import com.bytechef.ee.platform.connection.remote.grpc.mapper.ConnectionProtoMapper;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * gRPC server-side implementation of the {@code ConnectionRemoteService}, mirroring
 * {@code RemoteConnectionServiceController} — it delegates the two read RPCs to the real {@link ConnectionService} and
 * maps results via {@link ConnectionProtoMapper}. Only reads are exposed; writes are not part of the remote surface.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ConnectionRemoteServiceGrpcServer extends ConnectionRemoteServiceGrpc.ConnectionRemoteServiceImplBase {

    private final ConnectionService connectionService;

    @SuppressFBWarnings("EI")
    public ConnectionRemoteServiceGrpcServer(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @Override
    public void getConnection(
        GetConnectionRequest request, StreamObserver<ConnectionProto> responseObserver) {

        try {
            Connection connection = connectionService.getConnection(request.getId());

            responseObserver.onNext(ConnectionProtoMapper.toProto(connection));
            responseObserver.onCompleted();
        } catch (RuntimeException exception) {
            responseObserver.onError(
                Status.NOT_FOUND.withDescription("Connection not found: " + request.getId())
                    .withCause(exception)
                    .asRuntimeException());
        }
    }

    @Override
    public void getConnections(
        GetConnectionsRequest request, StreamObserver<ConnectionListProto> responseObserver) {

        try {
            List<Connection> connections = connectionService.getConnections(toPlatformType(request.getType()));

            ConnectionListProto.Builder builder = ConnectionListProto.newBuilder();

            for (Connection connection : connections) {
                builder.addConnections(ConnectionProtoMapper.toProto(connection));
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (RuntimeException exception) {
            responseObserver.onError(
                Status.INTERNAL.withDescription("Failed to list connections")
                    .withCause(exception)
                    .asRuntimeException());
        }
    }

    private static @Nullable PlatformType toPlatformType(PlatformTypeProto typeProto) {
        if (typeProto == PlatformTypeProto.PLATFORM_TYPE_UNSPECIFIED || typeProto == PlatformTypeProto.UNRECOGNIZED) {
            return null;
        }

        return PlatformType.valueOf(typeProto.name());
    }
}

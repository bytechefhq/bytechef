/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.grpc;

import com.bytechef.ee.platform.connection.remote.grpc.proto.ConnectionFacadeRemoteServiceGrpc;
import com.bytechef.ee.platform.connection.remote.grpc.proto.ConnectionIdsTypeRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.ConnectionParametersRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.CreateConnectionDTORequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.FindConnectionDTOsRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.IdRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.IdResponse;
import com.bytechef.ee.platform.connection.remote.grpc.proto.IntResponse;
import com.bytechef.ee.platform.connection.remote.grpc.proto.RemoteConnectionDTO;
import com.bytechef.ee.platform.connection.remote.grpc.proto.RemoteConnectionDTOs;
import com.bytechef.ee.platform.connection.remote.grpc.proto.RemoteConnections;
import com.bytechef.ee.platform.connection.remote.grpc.proto.RemoteTags;
import com.bytechef.ee.platform.connection.remote.grpc.proto.TypeRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.UpdateConnectionDTORequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.UpdateConnectionDTOTagsRequest;
import com.bytechef.ee.remote.grpc.ProtoValues;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ConnectionFacadeRemoteServiceGrpcServer
    extends ConnectionFacadeRemoteServiceGrpc.ConnectionFacadeRemoteServiceImplBase {

    private final ConnectionFacade connectionFacade;
    private final ConnectionProtoMapper connectionProtoMapper;

    @SuppressFBWarnings("EI")
    public ConnectionFacadeRemoteServiceGrpcServer(
        ConnectionFacade connectionFacade, ConnectionProtoMapper connectionProtoMapper) {

        this.connectionFacade = connectionFacade;
        this.connectionProtoMapper = connectionProtoMapper;
    }

    @Override
    public void create(CreateConnectionDTORequest request, StreamObserver<IdResponse> responseObserver) {
        long id = connectionFacade.create(
            connectionProtoMapper.toConnectionDTO(request.getConnection()),
            request.hasType() ? PlatformType.valueOf(request.getType()) : null);

        responseObserver.onNext(
            IdResponse.newBuilder()
                .setId(id)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void delete(IdRequest request, StreamObserver<Empty> responseObserver) {
        connectionFacade.delete(request.getId());

        replyEmpty(responseObserver);
    }

    @Override
    public void executeConnectionRefresh(IdRequest request, StreamObserver<IntResponse> responseObserver) {
        Integer result = connectionFacade.executeConnectionRefresh(request.getId());

        IntResponse.Builder builder = IntResponse.newBuilder();

        if (result != null) {
            builder.setValue(result);
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void findConnections(
        FindConnectionDTOsRequest request, StreamObserver<RemoteConnectionDTOs> responseObserver) {

        replyList(
            connectionFacade.getConnections(
                request.hasComponentName() ? request.getComponentName() : null,
                request.hasConnectionVersion() ? request.getConnectionVersion() : null,
                request.getHasConnectionIds() ? List.copyOf(request.getConnectionIdsList()) : null,
                request.hasTagId() ? request.getTagId() : null,
                request.hasEnvironmentId() ? request.getEnvironmentId() : null,
                request.hasType() ? PlatformType.valueOf(request.getType()) : null),
            responseObserver);
    }

    @Override
    public void getConnection(IdRequest request, StreamObserver<RemoteConnectionDTO> responseObserver) {
        responseObserver.onNext(connectionProtoMapper.toProto(connectionFacade.getConnection(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void getConnectionTags(TypeRequest request, StreamObserver<RemoteTags> responseObserver) {
        responseObserver.onNext(
            connectionProtoMapper.toRemoteTags(
                connectionFacade.getConnectionTags(
                    request.hasType() ? PlatformType.valueOf(request.getType()) : null)));
        responseObserver.onCompleted();
    }

    @Override
    public void getConnectionsByIds(
        ConnectionIdsTypeRequest request, StreamObserver<RemoteConnectionDTOs> responseObserver) {

        replyList(
            connectionFacade.getConnections(
                request.getHasConnectionIds() ? List.copyOf(request.getConnectionIdsList()) : null,
                request.hasType() ? PlatformType.valueOf(request.getType()) : null),
            responseObserver);
    }

    @Override
    public void replaceAuthorizationParameters(
        ConnectionParametersRequest request, StreamObserver<Empty> responseObserver) {

        connectionFacade.replaceAuthorizationParameters(
            request.getConnectionId(), ProtoValues.toMap(request.getParameters()));

        replyEmpty(responseObserver);
    }

    @Override
    public void toConnectionDTOs(RemoteConnections request, StreamObserver<RemoteConnectionDTOs> responseObserver) {
        replyList(connectionFacade.toConnectionDTOs(connectionProtoMapper.toConnections(request)), responseObserver);
    }

    @Override
    public void update(UpdateConnectionDTORequest request, StreamObserver<Empty> responseObserver) {
        connectionFacade.update(
            request.getId(), request.getName(), connectionProtoMapper.toTags(request.getTagsList()),
            request.getVersion());

        replyEmpty(responseObserver);
    }

    @Override
    public void updateTags(UpdateConnectionDTOTagsRequest request, StreamObserver<Empty> responseObserver) {
        connectionFacade.update(request.getId(), connectionProtoMapper.toTags(request.getTagsList()));

        replyEmpty(responseObserver);
    }

    private static void replyEmpty(StreamObserver<Empty> responseObserver) {
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    private void replyList(
        List<ConnectionDTO> connectionDTOs, StreamObserver<RemoteConnectionDTOs> responseObserver) {

        responseObserver.onNext(connectionProtoMapper.toRemoteConnectionDTOs(connectionDTOs));
        responseObserver.onCompleted();
    }
}

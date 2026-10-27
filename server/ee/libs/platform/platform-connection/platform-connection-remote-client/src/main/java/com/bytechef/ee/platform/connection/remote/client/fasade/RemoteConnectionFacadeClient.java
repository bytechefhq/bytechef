/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.client.fasade;

import com.bytechef.ee.platform.connection.remote.grpc.ConnectionProtoMapper;
import com.bytechef.ee.platform.connection.remote.grpc.proto.ConnectionFacadeRemoteServiceGrpc;
import com.bytechef.ee.platform.connection.remote.grpc.proto.ConnectionFacadeRemoteServiceGrpc.ConnectionFacadeRemoteServiceBlockingStub;
import com.bytechef.ee.platform.connection.remote.grpc.proto.ConnectionIdsTypeRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.ConnectionParametersRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.CreateConnectionDTORequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.FindConnectionDTOsRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.IdRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.IntResponse;
import com.bytechef.ee.platform.connection.remote.grpc.proto.TypeRequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.UpdateConnectionDTORequest;
import com.bytechef.ee.platform.connection.remote.grpc.proto.UpdateConnectionDTOTagsRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteConnectionFacadeClient implements ConnectionFacade {

    private static final String CONNECTION_APP = "connection-app";

    private final ConnectionProtoMapper connectionProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteConnectionFacadeClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.connectionProtoMapper = new ConnectionProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public long create(ConnectionDTO connectionDTO, PlatformType type) {
        CreateConnectionDTORequest.Builder builder = CreateConnectionDTORequest.newBuilder()
            .setConnection(connectionProtoMapper.toProto(connectionDTO));

        if (type != null) {
            builder.setType(type.name());
        }

        return getStub().create(builder.build())
            .getId();
    }

    @Override
    public void delete(Long id) {
        getStub().delete(toIdRequest(id));
    }

    @Override
    public Integer executeConnectionRefresh(Long connectionId) {
        IntResponse intResponse = getStub().executeConnectionRefresh(toIdRequest(connectionId));

        return intResponse.hasValue() ? intResponse.getValue() : null;
    }

    @Override
    public ConnectionDTO getConnection(Long id) {
        return connectionProtoMapper.toConnectionDTO(getStub().getConnection(toIdRequest(id)));
    }

    @Override
    public List<ConnectionDTO> getConnections(List<Long> connectionIds, PlatformType type) {
        ConnectionIdsTypeRequest.Builder builder = ConnectionIdsTypeRequest.newBuilder();

        if (connectionIds != null) {
            builder.setHasConnectionIds(true)
                .addAllConnectionIds(connectionIds);
        }

        if (type != null) {
            builder.setType(type.name());
        }

        return connectionProtoMapper.toConnectionDTOs(getStub().getConnectionsByIds(builder.build()));
    }

    @Override
    public List<ConnectionDTO> getConnections(
        String componentName, Integer connectionVersion, List<Long> connectionIds, Long tagId, Long environmentId,
        PlatformType type) {

        FindConnectionDTOsRequest.Builder builder = FindConnectionDTOsRequest.newBuilder();

        if (componentName != null) {
            builder.setComponentName(componentName);
        }

        if (connectionVersion != null) {
            builder.setConnectionVersion(connectionVersion);
        }

        if (connectionIds != null) {
            builder.setHasConnectionIds(true)
                .addAllConnectionIds(connectionIds);
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

        return connectionProtoMapper.toConnectionDTOs(getStub().findConnections(builder.build()));
    }

    @Override
    public List<ConnectionDTO> toConnectionDTOs(List<Connection> connections) {
        return connectionProtoMapper.toConnectionDTOs(
            getStub().toConnectionDTOs(connectionProtoMapper.toRemoteConnections(connections)));
    }

    @Override
    public List<Tag> getConnectionTags(PlatformType type) {
        TypeRequest.Builder builder = TypeRequest.newBuilder();

        if (type != null) {
            builder.setType(type.name());
        }

        return connectionProtoMapper.toTags(
            getStub().getConnectionTags(builder.build())
                .getTagsList());
    }

    @Override
    public void update(long id, List<Tag> tags) {
        UpdateConnectionDTOTagsRequest updateConnectionDTOTagsRequest = UpdateConnectionDTOTagsRequest.newBuilder()
            .setId(id)
            .addAllTags(connectionProtoMapper.toRemoteTagList(tags))
            .build();

        getStub().updateTags(updateConnectionDTOTagsRequest);
    }

    @Override
    public void update(long id, String name, List<Tag> tags, int version) {
        UpdateConnectionDTORequest.Builder builder = UpdateConnectionDTORequest.newBuilder()
            .setId(id)
            .setVersion(version);

        if (name != null) {
            builder.setName(name);
        }

        if (tags == null) {
            builder.setKeepTags(true);
        } else {
            builder.addAllTags(connectionProtoMapper.toRemoteTagList(tags));
        }

        getStub().update(builder.build());
    }

    @Override
    public void replaceAuthorizationParameters(long id, Map<String, ?> parameters) {
        ConnectionParametersRequest connectionParametersRequest = ConnectionParametersRequest.newBuilder()
            .setConnectionId(id)
            .setParameters(connectionProtoMapper.toBytes(parameters))
            .build();

        getStub().replaceAuthorizationParameters(connectionParametersRequest);
    }

    private ConnectionFacadeRemoteServiceBlockingStub getStub() {
        return ConnectionFacadeRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(CONNECTION_APP));
    }

    private static IdRequest toIdRequest(long id) {
        return IdRequest.newBuilder()
            .setId(id)
            .build();
    }
}

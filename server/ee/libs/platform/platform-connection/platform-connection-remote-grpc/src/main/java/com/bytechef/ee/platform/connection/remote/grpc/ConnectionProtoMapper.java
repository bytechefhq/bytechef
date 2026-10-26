/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.grpc;

import com.bytechef.ee.platform.connection.remote.grpc.proto.RemoteConnection;
import com.bytechef.ee.platform.connection.remote.grpc.proto.RemoteConnectionDTO;
import com.bytechef.ee.platform.connection.remote.grpc.proto.RemoteConnectionDTOs;
import com.bytechef.ee.platform.connection.remote.grpc.proto.RemoteConnections;
import com.bytechef.ee.platform.connection.remote.grpc.proto.RemoteTag;
import com.bytechef.ee.platform.connection.remote.grpc.proto.RemoteTags;
import com.bytechef.ee.remote.grpc.ProtoJsonMapper;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import tools.jackson.databind.ObjectMapper;

/**
 * Maps connections, connection DTOs and tags to their proto messages and back through {@link ProtoJsonMapper}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ConnectionProtoMapper {

    private final ProtoJsonMapper protoJsonMapper;

    public ConnectionProtoMapper(ObjectMapper objectMapper) {
        this.protoJsonMapper = new ProtoJsonMapper(objectMapper);
    }

    public Connection toConnection(RemoteConnection remoteConnection) {
        return protoJsonMapper.toObject(remoteConnection, Connection.class);
    }

    public ConnectionDTO toConnectionDTO(RemoteConnectionDTO remoteConnectionDTO) {
        return protoJsonMapper.toObject(remoteConnectionDTO, ConnectionDTO.class);
    }

    public List<ConnectionDTO> toConnectionDTOs(RemoteConnectionDTOs remoteConnectionDTOs) {
        return remoteConnectionDTOs.getConnectionsList()
            .stream()
            .map(this::toConnectionDTO)
            .toList();
    }

    public List<Connection> toConnections(RemoteConnections remoteConnections) {
        return remoteConnections.getConnectionsList()
            .stream()
            .map(this::toConnection)
            .toList();
    }

    public RemoteConnection toProto(Connection connection) {
        return protoJsonMapper.toProto(connection, RemoteConnection.newBuilder());
    }

    public RemoteConnectionDTO toProto(ConnectionDTO connectionDTO) {
        return protoJsonMapper.toProto(connectionDTO, RemoteConnectionDTO.newBuilder());
    }

    public RemoteTag toProto(Tag tag) {
        return protoJsonMapper.toProto(tag, RemoteTag.newBuilder());
    }

    public RemoteConnectionDTOs toRemoteConnectionDTOs(List<ConnectionDTO> connectionDTOs) {
        RemoteConnectionDTOs.Builder builder = RemoteConnectionDTOs.newBuilder();

        for (ConnectionDTO connectionDTO : connectionDTOs) {
            builder.addConnections(toProto(connectionDTO));
        }

        return builder.build();
    }

    public RemoteConnections toRemoteConnections(List<Connection> connections) {
        RemoteConnections.Builder builder = RemoteConnections.newBuilder();

        for (Connection connection : connections) {
            builder.addConnections(toProto(connection));
        }

        return builder.build();
    }

    public List<RemoteTag> toRemoteTagList(List<Tag> tags) {
        return tags.stream()
            .map(this::toProto)
            .toList();
    }

    public RemoteTags toRemoteTags(List<Tag> tags) {
        return RemoteTags.newBuilder()
            .addAllTags(toRemoteTagList(tags))
            .build();
    }

    public Tag toTag(RemoteTag remoteTag) {
        return protoJsonMapper.toObject(remoteTag, Tag.class);
    }

    public List<Tag> toTags(List<RemoteTag> remoteTags) {
        return remoteTags.stream()
            .map(this::toTag)
            .toList();
    }
}

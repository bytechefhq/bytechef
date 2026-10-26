/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.remote.grpc;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.remote.grpc.proto.RemoteConnectedUser;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Map;

/**
 * Maps connected users to and from their gRPC message. The environment travels as its stored ordinal.
 *
 * @version ee
 */
public final class ConnectedUserProtoMapper {

    private ConnectedUserProtoMapper() {
    }

    public static ConnectedUser toConnectedUser(RemoteConnectedUser remoteConnectedUser) {
        ConnectedUser connectedUser = new ConnectedUser(
            Map.of(), remoteConnectedUser.hasEmail() ? remoteConnectedUser.getEmail() : null,
            remoteConnectedUser.getEnabled(),
            remoteConnectedUser.hasExternalId() ? remoteConnectedUser.getExternalId() : null,
            remoteConnectedUser.hasId() ? remoteConnectedUser.getId() : null,
            remoteConnectedUser.hasName() ? remoteConnectedUser.getName() : null, remoteConnectedUser.getVersion());

        connectedUser.setEnvironment(Environment.values()[remoteConnectedUser.getEnvironment()]);
        connectedUser.setMetadata(remoteConnectedUser.getMetadataMap());

        if (remoteConnectedUser.hasCreatedBy()) {
            connectedUser.setCreatedBy(remoteConnectedUser.getCreatedBy());
        }

        return connectedUser;
    }

    public static RemoteConnectedUser toRemoteConnectedUser(ConnectedUser connectedUser) {
        Environment environment = connectedUser.getEnvironment();

        RemoteConnectedUser.Builder builder = RemoteConnectedUser.newBuilder()
            .setEnabled(connectedUser.isEnabled())
            .setEnvironment(environment.ordinal())
            .setVersion(connectedUser.getVersion());

        Map<String, String> metadata = connectedUser.getMetadata();

        if (metadata != null) {
            builder.putAllMetadata(metadata);
        }

        if (connectedUser.getCreatedBy() != null) {
            builder.setCreatedBy(connectedUser.getCreatedBy());
        }

        if (connectedUser.getEmail() != null) {
            builder.setEmail(connectedUser.getEmail());
        }

        if (connectedUser.getExternalId() != null) {
            builder.setExternalId(connectedUser.getExternalId());
        }

        if (connectedUser.getId() != null) {
            builder.setId(connectedUser.getId());
        }

        if (connectedUser.getName() != null) {
            builder.setName(connectedUser.getName());
        }

        return builder.build();
    }
}

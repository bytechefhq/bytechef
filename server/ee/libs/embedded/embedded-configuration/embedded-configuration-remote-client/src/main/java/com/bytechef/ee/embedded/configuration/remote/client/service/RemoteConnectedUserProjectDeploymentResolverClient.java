/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import com.bytechef.automation.configuration.service.ConnectedUserProjectDeploymentResolver;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.ConnectedUserIdsResponse;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.ConnectedUserProjectDeploymentResolverRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdsRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdsResponse;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteConnectedUserProjectDeploymentResolverClient implements ConnectedUserProjectDeploymentResolver {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final GrpcChannels grpcChannels;

    public RemoteConnectedUserProjectDeploymentResolverClient(GrpcChannels grpcChannels) {
        this.grpcChannels = grpcChannels;
    }

    @Override
    public Map<Long, Long> getConnectedUserIds(Collection<Long> projectDeploymentIds) {
        if (projectDeploymentIds.isEmpty()) {
            return Map.of();
        }

        ConnectedUserProjectDeploymentResolverRemoteServiceGrpc.ConnectedUserProjectDeploymentResolverRemoteServiceBlockingStub stub =
            ConnectedUserProjectDeploymentResolverRemoteServiceGrpc.newBlockingStub(
                grpcChannels.getChannel(CONFIGURATION_APP));

        ConnectedUserIdsResponse connectedUserIdsResponse = stub.getConnectedUserIds(
            IdsRequest.newBuilder()
                .addAllIds(projectDeploymentIds)
                .build());

        return connectedUserIdsResponse.getConnectedUserIdsMap();
    }

    @Override
    public List<Long> getProjectDeploymentIds(long connectedUserId) {
        ConnectedUserProjectDeploymentResolverRemoteServiceGrpc.ConnectedUserProjectDeploymentResolverRemoteServiceBlockingStub stub =
            ConnectedUserProjectDeploymentResolverRemoteServiceGrpc.newBlockingStub(
                grpcChannels.getChannel(CONFIGURATION_APP));

        IdsResponse idsResponse = stub.getProjectDeploymentIds(
            IdRequest.newBuilder()
                .setId(connectedUserId)
                .build());

        return idsResponse.getIdsList();
    }
}

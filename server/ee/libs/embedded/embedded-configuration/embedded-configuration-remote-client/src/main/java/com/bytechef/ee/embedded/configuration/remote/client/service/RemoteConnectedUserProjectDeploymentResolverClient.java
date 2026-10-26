/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import com.bytechef.automation.configuration.service.ConnectedUserProjectDeploymentResolver;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.ConnectedUserProjectDeploymentResolverRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdsResponse;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
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

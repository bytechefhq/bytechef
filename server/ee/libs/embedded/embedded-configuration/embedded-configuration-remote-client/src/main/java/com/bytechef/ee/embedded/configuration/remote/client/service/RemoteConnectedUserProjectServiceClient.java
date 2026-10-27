/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.BoolResponse;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.ConnectedUserProjectRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.OptionalIdResponse;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteConnectedUserProjectServiceClient implements ConnectedUserProjectService {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final GrpcChannels grpcChannels;

    public RemoteConnectedUserProjectServiceClient(GrpcChannels grpcChannels) {
        this.grpcChannels = grpcChannels;
    }

    @Override
    public boolean containsProjectDeployment(long projectDeploymentId) {
        BoolResponse boolResponse = getStub().containsProjectDeployment(
            IdRequest.newBuilder()
                .setId(projectDeploymentId)
                .build());

        return boolResponse.getValue();
    }

    @Override
    public ConnectedUserProject create(ConnectedUserProject connectedUserProject) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectedUserProject create(long connectedUserId, long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Long> fetchConnectedUserId(long projectDeploymentId) {
        OptionalIdResponse optionalIdResponse = getStub().fetchConnectedUserId(
            IdRequest.newBuilder()
                .setId(projectDeploymentId)
                .build());

        if (!optionalIdResponse.hasId()) {
            return Optional.empty();
        }

        return Optional.of(optionalIdResponse.getId());
    }

    @Override
    public Optional<ConnectedUserProject> fetchConnectUserProject(String externalUserId, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectedUserProject getConnectedUserProject(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ConnectedUserProject> getConnectedUserConnectedUserProjects(long connectedUserId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectedUserProject getConnectedUserConnectedUserProject(Long connectedUserid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectedUserProject getConnectUserProject(String externalUserId, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ConnectedUserProject> getConnectedUserProjects(Long connectedUserId, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ConnectedUserProject> getConnectedUserProjects(List<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long lockConnectedUser(String externalUserId, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectedUserProject lockConnectedUserProject(long connectedUserProjectId) {
        throw new UnsupportedOperationException();
    }

    private ConnectedUserProjectRemoteServiceGrpc.ConnectedUserProjectRemoteServiceBlockingStub getStub() {
        return ConnectedUserProjectRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(CONFIGURATION_APP));
    }
}

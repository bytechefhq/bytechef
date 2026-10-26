/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.grpc;

import com.bytechef.automation.configuration.service.ConnectedUserProjectDeploymentResolver;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.ConnectedUserProjectDeploymentResolverRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdsResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ConnectedUserProjectDeploymentResolverRemoteServiceGrpcServer extends
    ConnectedUserProjectDeploymentResolverRemoteServiceGrpc.ConnectedUserProjectDeploymentResolverRemoteServiceImplBase {

    private final ConnectedUserProjectDeploymentResolver connectedUserProjectDeploymentResolver;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectDeploymentResolverRemoteServiceGrpcServer(
        ConnectedUserProjectDeploymentResolver connectedUserProjectDeploymentResolver) {

        this.connectedUserProjectDeploymentResolver = connectedUserProjectDeploymentResolver;
    }

    @Override
    public void getProjectDeploymentIds(IdRequest request, StreamObserver<IdsResponse> responseObserver) {
        responseObserver.onNext(
            IdsResponse.newBuilder()
                .addAllIds(connectedUserProjectDeploymentResolver.getProjectDeploymentIds(request.getId()))
                .build());
        responseObserver.onCompleted();
    }
}

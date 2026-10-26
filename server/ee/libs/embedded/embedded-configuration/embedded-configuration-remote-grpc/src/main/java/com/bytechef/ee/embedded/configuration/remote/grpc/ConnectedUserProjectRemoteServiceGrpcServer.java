/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.grpc;

import com.bytechef.ee.embedded.configuration.remote.grpc.proto.BoolResponse;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.ConnectedUserProjectRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ConnectedUserProjectRemoteServiceGrpcServer
    extends ConnectedUserProjectRemoteServiceGrpc.ConnectedUserProjectRemoteServiceImplBase {

    private final ConnectedUserProjectService connectedUserProjectService;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectRemoteServiceGrpcServer(ConnectedUserProjectService connectedUserProjectService) {
        this.connectedUserProjectService = connectedUserProjectService;
    }

    @Override
    public void containsProjectDeployment(IdRequest request, StreamObserver<BoolResponse> responseObserver) {
        responseObserver.onNext(
            BoolResponse.newBuilder()
                .setValue(connectedUserProjectService.containsProjectDeployment(request.getId()))
                .build());
        responseObserver.onCompleted();
    }
}

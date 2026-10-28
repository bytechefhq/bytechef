/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.grpc;

import com.bytechef.automation.configuration.service.ConnectedUserWorkflowCopyResolver;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.ConnectedUserWorkflowCopyResolverRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.StringsResponse;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.WorkflowUuidRequest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ConnectedUserWorkflowCopyResolverRemoteServiceGrpcServer extends
    ConnectedUserWorkflowCopyResolverRemoteServiceGrpc.ConnectedUserWorkflowCopyResolverRemoteServiceImplBase {

    private final ConnectedUserWorkflowCopyResolver connectedUserWorkflowCopyResolver;

    @SuppressFBWarnings("EI")
    public ConnectedUserWorkflowCopyResolverRemoteServiceGrpcServer(
        ConnectedUserWorkflowCopyResolver connectedUserWorkflowCopyResolver) {

        this.connectedUserWorkflowCopyResolver = connectedUserWorkflowCopyResolver;
    }

    @Override
    public void getWorkflowCopyUuids(WorkflowUuidRequest request, StreamObserver<StringsResponse> responseObserver) {
        responseObserver.onNext(
            StringsResponse.newBuilder()
                .addAllValues(connectedUserWorkflowCopyResolver.getWorkflowCopyUuids(request.getWorkflowUuid()))
                .build());
        responseObserver.onCompleted();
    }
}

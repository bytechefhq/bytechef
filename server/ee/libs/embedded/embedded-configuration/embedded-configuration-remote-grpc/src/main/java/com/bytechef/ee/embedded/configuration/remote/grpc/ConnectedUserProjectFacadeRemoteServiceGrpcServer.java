/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.grpc;

import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.ConnectedUserProjectFacadeRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.CopyWorkflowTemplateRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.StringResponse;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ConnectedUserProjectFacadeRemoteServiceGrpcServer
    extends ConnectedUserProjectFacadeRemoteServiceGrpc.ConnectedUserProjectFacadeRemoteServiceImplBase {

    private final ConnectedUserProjectFacade connectedUserProjectFacade;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectFacadeRemoteServiceGrpcServer(
        ConnectedUserProjectFacade connectedUserProjectFacade) {

        this.connectedUserProjectFacade = connectedUserProjectFacade;
    }

    @Override
    public void
        copyWorkflowTemplate(CopyWorkflowTemplateRequest request, StreamObserver<StringResponse> responseObserver) {
        responseObserver.onNext(
            StringResponse.newBuilder()
                .setValue(
                    connectedUserProjectFacade.copyWorkflowTemplate(
                        request.getExternalUserId(), request.getWorkflowUuid(),
                        Environment.valueOf(request.getEnvironment())))
                .build());
        responseObserver.onCompleted();
    }
}

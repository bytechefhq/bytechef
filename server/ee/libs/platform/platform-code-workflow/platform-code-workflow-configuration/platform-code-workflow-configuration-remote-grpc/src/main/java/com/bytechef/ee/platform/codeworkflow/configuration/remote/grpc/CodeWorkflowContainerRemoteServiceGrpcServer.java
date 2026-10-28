/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.configuration.remote.grpc;

import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.remote.grpc.proto.CodeWorkflowContainerRemoteServiceGrpc;
import com.bytechef.ee.platform.codeworkflow.configuration.remote.grpc.proto.GetCodeWorkflowContainerRequest;
import com.bytechef.ee.platform.codeworkflow.configuration.remote.grpc.proto.RemoteCodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CodeWorkflowContainerRemoteServiceGrpcServer
    extends CodeWorkflowContainerRemoteServiceGrpc.CodeWorkflowContainerRemoteServiceImplBase {

    private final CodeWorkflowContainerProtoMapper codeWorkflowContainerProtoMapper;
    private final CodeWorkflowContainerService codeWorkflowContainerService;

    @SuppressFBWarnings("EI")
    public CodeWorkflowContainerRemoteServiceGrpcServer(
        CodeWorkflowContainerService codeWorkflowContainerService,
        CodeWorkflowContainerProtoMapper codeWorkflowContainerProtoMapper) {

        this.codeWorkflowContainerProtoMapper = codeWorkflowContainerProtoMapper;
        this.codeWorkflowContainerService = codeWorkflowContainerService;
    }

    @Override
    public void getCodeWorkflowContainer(
        GetCodeWorkflowContainerRequest request, StreamObserver<RemoteCodeWorkflowContainer> responseObserver) {

        CodeWorkflowContainer codeWorkflowContainer = codeWorkflowContainerService.getCodeWorkflowContainer(
            request.getCodeWorkflowContainerUuid());

        responseObserver.onNext(codeWorkflowContainerProtoMapper.toProto(codeWorkflowContainer));
        responseObserver.onCompleted();
    }
}

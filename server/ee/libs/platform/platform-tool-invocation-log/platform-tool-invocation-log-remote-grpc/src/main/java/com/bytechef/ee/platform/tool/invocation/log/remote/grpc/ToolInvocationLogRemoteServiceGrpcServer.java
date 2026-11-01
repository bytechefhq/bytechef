/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log.remote.grpc;

import com.bytechef.ee.platform.tool.invocation.log.remote.grpc.proto.RecordToolExecutionRequest;
import com.bytechef.ee.platform.tool.invocation.log.remote.grpc.proto.ToolInvocationLogRemoteServiceGrpc;
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ToolInvocationLogRemoteServiceGrpcServer
    extends ToolInvocationLogRemoteServiceGrpc.ToolInvocationLogRemoteServiceImplBase {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ToolExecutionEventProtoMapper toolExecutionEventProtoMapper;

    @SuppressFBWarnings("EI")
    public ToolInvocationLogRemoteServiceGrpcServer(
        ApplicationEventPublisher applicationEventPublisher,
        ToolExecutionEventProtoMapper toolExecutionEventProtoMapper) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.toolExecutionEventProtoMapper = toolExecutionEventProtoMapper;
    }

    @Override
    public void recordToolExecution(RecordToolExecutionRequest request, StreamObserver<Empty> responseObserver) {
        applicationEventPublisher.publishEvent(toolExecutionEventProtoMapper.toToolExecutionEvent(request));

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}

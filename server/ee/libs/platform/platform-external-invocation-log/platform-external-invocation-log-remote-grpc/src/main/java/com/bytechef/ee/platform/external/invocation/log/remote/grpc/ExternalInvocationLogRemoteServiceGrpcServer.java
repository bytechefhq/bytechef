/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.external.invocation.log.remote.grpc;

import com.bytechef.ee.platform.external.invocation.log.remote.grpc.proto.ExternalInvocationLogRemoteServiceGrpc;
import com.bytechef.ee.platform.external.invocation.log.remote.grpc.proto.RecordExternalInvocationRequest;
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ExternalInvocationLogRemoteServiceGrpcServer
    extends ExternalInvocationLogRemoteServiceGrpc.ExternalInvocationLogRemoteServiceImplBase {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ExternalInvocationEventProtoMapper externalInvocationEventProtoMapper;

    @SuppressFBWarnings("EI")
    public ExternalInvocationLogRemoteServiceGrpcServer(
        ApplicationEventPublisher applicationEventPublisher,
        ExternalInvocationEventProtoMapper externalInvocationEventProtoMapper) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.externalInvocationEventProtoMapper = externalInvocationEventProtoMapper;
    }

    @Override
    public void
        recordExternalInvocation(RecordExternalInvocationRequest request, StreamObserver<Empty> responseObserver) {
        applicationEventPublisher.publishEvent(externalInvocationEventProtoMapper.toExternalInvocationEvent(request));

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}

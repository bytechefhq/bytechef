/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.grpc;

import com.bytechef.ee.atlas.execution.remote.grpc.proto.IdRequest;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.RemoteTriggerExecution;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.TriggerExecutionRemoteServiceGrpc;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 */
public class TriggerExecutionRemoteServiceGrpcServer
    extends TriggerExecutionRemoteServiceGrpc.TriggerExecutionRemoteServiceImplBase {

    private final PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper;
    private final TriggerExecutionService triggerExecutionService;

    @SuppressFBWarnings("EI")
    public TriggerExecutionRemoteServiceGrpcServer(
        TriggerExecutionService triggerExecutionService,
        PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper) {

        this.platformWorkflowExecutionProtoMapper = platformWorkflowExecutionProtoMapper;
        this.triggerExecutionService = triggerExecutionService;
    }

    @Override
    public void create(RemoteTriggerExecution request, StreamObserver<RemoteTriggerExecution> responseObserver) {
        responseObserver.onNext(platformWorkflowExecutionProtoMapper
            .toProto(triggerExecutionService.create(platformWorkflowExecutionProtoMapper.toTriggerExecution(request))));
        responseObserver.onCompleted();
    }

    @Override
    public void getTriggerExecution(IdRequest request, StreamObserver<RemoteTriggerExecution> responseObserver) {
        responseObserver.onNext(
            platformWorkflowExecutionProtoMapper.toProto(triggerExecutionService.getTriggerExecution(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void update(RemoteTriggerExecution request, StreamObserver<RemoteTriggerExecution> responseObserver) {
        responseObserver.onNext(platformWorkflowExecutionProtoMapper
            .toProto(triggerExecutionService.update(platformWorkflowExecutionProtoMapper.toTriggerExecution(request))));
        responseObserver.onCompleted();
    }
}

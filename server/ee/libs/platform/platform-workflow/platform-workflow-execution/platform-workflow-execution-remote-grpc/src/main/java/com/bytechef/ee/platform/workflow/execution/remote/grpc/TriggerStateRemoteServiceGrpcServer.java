/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.grpc;

import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.SaveTriggerStateRequest;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.TriggerStateRemoteServiceGrpc;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.TriggerStateValue;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.WorkflowExecutionIdRequest;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.facade.TriggerStateFacade;
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.Optional;

/**
 * @version ee
 */
public class TriggerStateRemoteServiceGrpcServer
    extends TriggerStateRemoteServiceGrpc.TriggerStateRemoteServiceImplBase {

    private final PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper;
    private final TriggerStateFacade triggerStateFacade;

    @SuppressFBWarnings("EI")
    public TriggerStateRemoteServiceGrpcServer(
        TriggerStateFacade triggerStateFacade,
        PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper) {

        this.platformWorkflowExecutionProtoMapper = platformWorkflowExecutionProtoMapper;
        this.triggerStateFacade = triggerStateFacade;
    }

    @Override
    public void delete(WorkflowExecutionIdRequest request, StreamObserver<Empty> responseObserver) {
        triggerStateFacade.delete(WorkflowExecutionId.parse(request.getWorkflowExecutionId()));

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void fetchValue(WorkflowExecutionIdRequest request, StreamObserver<TriggerStateValue> responseObserver) {
        Optional<Object> valueOptional = triggerStateFacade.fetchValue(
            WorkflowExecutionId.parse(request.getWorkflowExecutionId()));

        responseObserver.onNext(platformWorkflowExecutionProtoMapper.toTriggerStateValue(valueOptional.orElse(null)));
        responseObserver.onCompleted();
    }

    @Override
    public void save(SaveTriggerStateRequest request, StreamObserver<Empty> responseObserver) {
        triggerStateFacade.save(
            WorkflowExecutionId.parse(request.getWorkflowExecutionId()),
            platformWorkflowExecutionProtoMapper.toTriggerStateObject(request.getValue()));

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}

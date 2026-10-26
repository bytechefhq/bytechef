/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.grpc;

import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.TriggerLifecycleFacadeRemoteServiceGrpc;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.TriggerLifecycleRequest;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import java.util.Objects;

/**
 * @version ee
 */
public class TriggerLifecycleFacadeRemoteServiceGrpcServer
    extends TriggerLifecycleFacadeRemoteServiceGrpc.TriggerLifecycleFacadeRemoteServiceImplBase {

    private final PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper;
    private final TriggerLifecycleFacade triggerLifecycleFacade;

    @SuppressFBWarnings("EI")
    public TriggerLifecycleFacadeRemoteServiceGrpcServer(
        TriggerLifecycleFacade triggerLifecycleFacade,
        PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper) {

        this.platformWorkflowExecutionProtoMapper = platformWorkflowExecutionProtoMapper;
        this.triggerLifecycleFacade = triggerLifecycleFacade;
    }

    @Override
    public void executeTriggerDisable(TriggerLifecycleRequest request, StreamObserver<Empty> responseObserver) {
        triggerLifecycleFacade.executeTriggerDisable(
            request.getWorkflowId(), WorkflowExecutionId.parse(request.getWorkflowExecutionId()),
            platformWorkflowExecutionProtoMapper.toWorkflowNodeType(request.getTriggerWorkflowNodeType()),
            Objects.requireNonNullElse(platformWorkflowExecutionProtoMapper.toMap(request.getTriggerParameters()),
                Map.of()),
            request.hasConnectionId() ? request.getConnectionId() : null);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void executeTriggerEnable(TriggerLifecycleRequest request, StreamObserver<Empty> responseObserver) {
        triggerLifecycleFacade.executeTriggerEnable(
            request.getWorkflowId(), WorkflowExecutionId.parse(request.getWorkflowExecutionId()),
            platformWorkflowExecutionProtoMapper.toWorkflowNodeType(request.getTriggerWorkflowNodeType()),
            Objects.requireNonNullElse(platformWorkflowExecutionProtoMapper.toMap(request.getTriggerParameters()),
                Map.of()),
            request.hasConnectionId() ? request.getConnectionId() : null,
            request.hasWebhookUrl() ? request.getWebhookUrl() : null, request.getEnvironmentId());

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.remote.grpc;

import com.bytechef.ee.platform.scheduler.remote.grpc.proto.JobIdRequest;
import com.bytechef.ee.platform.scheduler.remote.grpc.proto.ScheduleDynamicWebhookTriggerRefreshRequest;
import com.bytechef.ee.platform.scheduler.remote.grpc.proto.ScheduleOneTimeTaskRequest;
import com.bytechef.ee.platform.scheduler.remote.grpc.proto.ScheduleScheduleTriggerRequest;
import com.bytechef.ee.platform.scheduler.remote.grpc.proto.TriggerSchedulerRemoteServiceGrpc;
import com.bytechef.ee.platform.scheduler.remote.grpc.proto.WorkflowExecutionIdRequest;
import com.bytechef.ee.remote.grpc.JsonBytes;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class TriggerSchedulerRemoteServiceGrpcServer
    extends TriggerSchedulerRemoteServiceGrpc.TriggerSchedulerRemoteServiceImplBase {

    private final TriggerScheduler triggerScheduler;
    private final JsonBytes jsonBytes;

    @SuppressFBWarnings("EI")
    public TriggerSchedulerRemoteServiceGrpcServer(TriggerScheduler triggerScheduler, ObjectMapper objectMapper) {
        this.triggerScheduler = triggerScheduler;
        this.jsonBytes = new JsonBytes(objectMapper);
    }

    @Override
    public void cancelDynamicWebhookTriggerRefresh(
        WorkflowExecutionIdRequest request, StreamObserver<Empty> responseObserver) {

        triggerScheduler.cancelDynamicWebhookTriggerRefresh(request.getWorkflowExecutionId());

        replyEmpty(responseObserver);
    }

    @Override
    public void cancelOneTimeTask(JobIdRequest request, StreamObserver<Empty> responseObserver) {
        triggerScheduler.cancelOneTimeTask(request.getJobId());

        replyEmpty(responseObserver);
    }

    @Override
    public void cancelPollingTrigger(WorkflowExecutionIdRequest request, StreamObserver<Empty> responseObserver) {
        triggerScheduler.cancelPollingTrigger(request.getWorkflowExecutionId());

        replyEmpty(responseObserver);
    }

    @Override
    public void cancelScheduleTrigger(WorkflowExecutionIdRequest request, StreamObserver<Empty> responseObserver) {
        triggerScheduler.cancelScheduleTrigger(request.getWorkflowExecutionId());

        replyEmpty(responseObserver);
    }

    @Override
    public void scheduleDynamicWebhookTriggerRefresh(
        ScheduleDynamicWebhookTriggerRefreshRequest request, StreamObserver<Empty> responseObserver) {

        triggerScheduler.scheduleDynamicWebhookTriggerRefresh(
            request.hasWebhookExpirationDate()
                ? SchedulerProtoTimestamps.toInstant(request.getWebhookExpirationDate()) : null,
            request.getComponentName(), request.getComponentVersion(),
            WorkflowExecutionId.parse(request.getWorkflowExecutionId()),
            request.hasConnectionId() ? request.getConnectionId() : null);

        replyEmpty(responseObserver);
    }

    @Override
    public void scheduleOneTimeTask(ScheduleOneTimeTaskRequest request, StreamObserver<Empty> responseObserver) {
        triggerScheduler.scheduleOneTimeTask(
            SchedulerProtoTimestamps.toInstant(request.getExecuteAt()),
            request.hasOutput() ? jsonBytes.readMap(request.getOutput()) : null, request.getJobId());

        replyEmpty(responseObserver);
    }

    @Override
    public void schedulePollingTrigger(WorkflowExecutionIdRequest request, StreamObserver<Empty> responseObserver) {
        triggerScheduler.schedulePollingTrigger(WorkflowExecutionId.parse(request.getWorkflowExecutionId()));

        replyEmpty(responseObserver);
    }

    @Override
    public void scheduleScheduleTrigger(
        ScheduleScheduleTriggerRequest request, StreamObserver<Empty> responseObserver) {

        triggerScheduler.scheduleScheduleTrigger(
            request.getPattern(), request.hasZoneId() ? request.getZoneId() : null,
            request.hasOutput() ? jsonBytes.readMap(request.getOutput()) : null,
            WorkflowExecutionId.parse(request.getWorkflowExecutionId()));

        replyEmpty(responseObserver);
    }

    private static void replyEmpty(StreamObserver<Empty> responseObserver) {
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.remote.client;

import com.bytechef.ee.platform.scheduler.remote.grpc.SchedulerProtoTimestamps;
import com.bytechef.ee.platform.scheduler.remote.grpc.proto.JobIdRequest;
import com.bytechef.ee.platform.scheduler.remote.grpc.proto.ScheduleDynamicWebhookTriggerRefreshRequest;
import com.bytechef.ee.platform.scheduler.remote.grpc.proto.ScheduleOneTimeTaskRequest;
import com.bytechef.ee.platform.scheduler.remote.grpc.proto.ScheduleScheduleTriggerRequest;
import com.bytechef.ee.platform.scheduler.remote.grpc.proto.TriggerSchedulerRemoteServiceGrpc;
import com.bytechef.ee.platform.scheduler.remote.grpc.proto.TriggerSchedulerRemoteServiceGrpc.TriggerSchedulerRemoteServiceBlockingStub;
import com.bytechef.ee.platform.scheduler.remote.grpc.proto.WorkflowExecutionIdRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.ee.remote.grpc.ProtoValues;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteTriggerSchedulerClient implements TriggerScheduler {

    private static final String SCHEDULER_APP = "scheduler-app";

    private final GrpcChannels grpcChannels;

    public RemoteTriggerSchedulerClient(GrpcChannels grpcChannels) {
        this.grpcChannels = grpcChannels;
    }

    @Override
    public void cancelDynamicWebhookTriggerRefresh(String workflowExecutionId) {
        getStub().cancelDynamicWebhookTriggerRefresh(toWorkflowExecutionIdRequest(workflowExecutionId));
    }

    @Override
    public void cancelOneTimeTask(long jobId) {
        getStub().cancelOneTimeTask(
            JobIdRequest.newBuilder()
                .setJobId(jobId)
                .build());
    }

    @Override
    public void cancelPollingTrigger(String workflowExecutionId) {
        getStub().cancelPollingTrigger(toWorkflowExecutionIdRequest(workflowExecutionId));
    }

    @Override
    public void cancelScheduleTrigger(String workflowExecutionId) {
        getStub().cancelScheduleTrigger(toWorkflowExecutionIdRequest(workflowExecutionId));
    }

    @Override
    public void scheduleDynamicWebhookTriggerRefresh(
        Instant webhookExpirationDate, String componentName, int componentVersion,
        WorkflowExecutionId workflowExecutionId, Long connectionId) {

        ScheduleDynamicWebhookTriggerRefreshRequest.Builder builder =
            ScheduleDynamicWebhookTriggerRefreshRequest.newBuilder()
                .setComponentName(componentName)
                .setComponentVersion(componentVersion)
                .setWorkflowExecutionId(workflowExecutionId.toString());

        if (webhookExpirationDate != null) {
            builder.setWebhookExpirationDate(SchedulerProtoTimestamps.toTimestamp(webhookExpirationDate));
        }

        if (connectionId != null) {
            builder.setConnectionId(connectionId);
        }

        getStub().scheduleDynamicWebhookTriggerRefresh(builder.build());
    }

    @Override
    public void schedulePollingTrigger(WorkflowExecutionId workflowExecutionId) {
        getStub().schedulePollingTrigger(toWorkflowExecutionIdRequest(workflowExecutionId.toString()));
    }

    @Override
    public void scheduleScheduleTrigger(
        String pattern, String zoneId, Map<String, Object> output, WorkflowExecutionId workflowExecutionId) {

        ScheduleScheduleTriggerRequest.Builder builder = ScheduleScheduleTriggerRequest.newBuilder()
            .setPattern(pattern)
            .setWorkflowExecutionId(workflowExecutionId.toString());

        if (zoneId != null) {
            builder.setZoneId(zoneId);
        }

        if (output != null) {
            builder.setOutput(ProtoValues.toStruct(output));
        }

        getStub().scheduleScheduleTrigger(builder.build());
    }

    @Override
    public void scheduleOneTimeTask(Instant executeAt, Map<String, ?> output, long jobId) {
        ScheduleOneTimeTaskRequest.Builder builder = ScheduleOneTimeTaskRequest.newBuilder()
            .setExecuteAt(SchedulerProtoTimestamps.toTimestamp(executeAt))
            .setJobId(jobId);

        if (output != null) {
            builder.setOutput(ProtoValues.toStruct(output));
        }

        getStub().scheduleOneTimeTask(builder.build());
    }

    private TriggerSchedulerRemoteServiceBlockingStub getStub() {
        return TriggerSchedulerRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(SCHEDULER_APP));
    }

    private static WorkflowExecutionIdRequest toWorkflowExecutionIdRequest(String workflowExecutionId) {
        return WorkflowExecutionIdRequest.newBuilder()
            .setWorkflowExecutionId(workflowExecutionId)
            .build();
    }
}

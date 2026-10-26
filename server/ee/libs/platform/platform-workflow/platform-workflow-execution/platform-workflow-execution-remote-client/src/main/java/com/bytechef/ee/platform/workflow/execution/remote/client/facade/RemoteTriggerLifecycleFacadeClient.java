/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.facade;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.PlatformWorkflowExecutionProtoMapper;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.TriggerLifecycleFacadeRemoteServiceGrpc;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.TriggerLifecycleRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteTriggerLifecycleFacadeClient implements TriggerLifecycleFacade {

    private static final String EXECUTION_APP = "execution-app";

    private final PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper;
    private final @Nullable Duration executionDeadline;
    private final GrpcChannels grpcChannels;

    public RemoteTriggerLifecycleFacadeClient(
        ApplicationProperties applicationProperties, GrpcChannels grpcChannels, ObjectMapper objectMapper) {

        ApplicationProperties.Remote remote = applicationProperties.getRemote();

        ApplicationProperties.Remote.Grpc grpc = remote.getGrpc();

        this.executionDeadline = grpc.getExecutionDeadline();
        this.platformWorkflowExecutionProtoMapper = new PlatformWorkflowExecutionProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public void executeTriggerDisable(
        String workflowId, WorkflowExecutionId workflowExecutionId, WorkflowNodeType triggerWorkflowNodeType,
        Map<String, ?> triggerParameters, Long connectionId) {

        getStub().executeTriggerDisable(
            toTriggerLifecycleRequest(
                workflowId, workflowExecutionId, triggerWorkflowNodeType, triggerParameters, connectionId, null, 0L));
    }

    @Override
    public void executeTriggerEnable(
        String workflowId, WorkflowExecutionId workflowExecutionId, WorkflowNodeType triggerWorkflowNodeType,
        Map<String, ?> triggerParameters, Long connectionId, String webhookUrl, long environmentId) {

        getStub().executeTriggerEnable(
            toTriggerLifecycleRequest(
                workflowId, workflowExecutionId, triggerWorkflowNodeType, triggerParameters, connectionId, webhookUrl,
                environmentId));
    }

    private TriggerLifecycleFacadeRemoteServiceGrpc.TriggerLifecycleFacadeRemoteServiceBlockingStub getStub() {
        TriggerLifecycleFacadeRemoteServiceGrpc.TriggerLifecycleFacadeRemoteServiceBlockingStub stub =
            TriggerLifecycleFacadeRemoteServiceGrpc.newBlockingStub(
                grpcChannels.getChannel(EXECUTION_APP));

        if (executionDeadline == null) {
            return stub;
        }

        return stub.withDeadlineAfter(executionDeadline.toMillis(), TimeUnit.MILLISECONDS);
    }

    private TriggerLifecycleRequest toTriggerLifecycleRequest(
        String workflowId, WorkflowExecutionId workflowExecutionId, WorkflowNodeType triggerWorkflowNodeType,
        Map<String, ?> triggerParameters, @Nullable Long connectionId, @Nullable String webhookUrl,
        long environmentId) {

        TriggerLifecycleRequest.Builder builder = TriggerLifecycleRequest.newBuilder()
            .setEnvironmentId(environmentId)
            .setTriggerParameters(platformWorkflowExecutionProtoMapper.toBytes(triggerParameters))
            .setTriggerWorkflowNodeType(platformWorkflowExecutionProtoMapper.toProto(triggerWorkflowNodeType))
            .setWorkflowExecutionId(workflowExecutionId.toString())
            .setWorkflowId(workflowId);

        if (connectionId != null) {
            builder.setConnectionId(connectionId);
        }

        if (webhookUrl != null) {
            builder.setWebhookUrl(webhookUrl);
        }

        return builder.build();
    }
}

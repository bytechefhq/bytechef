/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.facade;

import com.bytechef.ee.platform.workflow.execution.remote.grpc.PlatformWorkflowExecutionProtoMapper;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.SaveTriggerStateRequest;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.TriggerStateRemoteServiceGrpc;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.TriggerStateValue;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.WorkflowExecutionIdRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.facade.TriggerStateFacade;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteTriggerStateFacadeClient implements TriggerStateFacade {

    private static final String EXECUTION_APP = "execution-app";

    private final PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteTriggerStateFacadeClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.platformWorkflowExecutionProtoMapper = new PlatformWorkflowExecutionProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public void delete(WorkflowExecutionId workflowExecutionId) {
        getStub().delete(toWorkflowExecutionIdRequest(workflowExecutionId));
    }

    @Override
    public <T> Optional<T> fetchValue(WorkflowExecutionId workflowExecutionId) {
        TriggerStateValue triggerStateValue = getStub().fetchValue(toWorkflowExecutionIdRequest(workflowExecutionId));

        if (!triggerStateValue.hasValue()) {
            return Optional.empty();
        }

        return Optional.ofNullable((T) platformWorkflowExecutionProtoMapper.toObject(triggerStateValue.getValue()));
    }

    @Override
    public void save(WorkflowExecutionId workflowExecutionId, Object value) {
        getStub().save(
            SaveTriggerStateRequest.newBuilder()
                .setValue(platformWorkflowExecutionProtoMapper.toBytes(value))
                .setWorkflowExecutionId(workflowExecutionId.toString())
                .build());
    }

    private TriggerStateRemoteServiceGrpc.TriggerStateRemoteServiceBlockingStub getStub() {
        return TriggerStateRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(EXECUTION_APP));
    }

    private static WorkflowExecutionIdRequest toWorkflowExecutionIdRequest(WorkflowExecutionId workflowExecutionId) {
        return WorkflowExecutionIdRequest.newBuilder()
            .setWorkflowExecutionId(workflowExecutionId.toString())
            .build();
    }
}

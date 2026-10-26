/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.service;

import com.bytechef.ee.atlas.execution.remote.grpc.proto.IdRequest;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.PlatformWorkflowExecutionProtoMapper;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.TriggerExecutionRemoteServiceGrpc;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteTriggerExecutionServiceClient implements TriggerExecutionService {

    private static final String EXECUTION_APP = "execution-app";

    private final PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteTriggerExecutionServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.platformWorkflowExecutionProtoMapper = new PlatformWorkflowExecutionProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public TriggerExecution create(TriggerExecution triggerExecution) {
        return platformWorkflowExecutionProtoMapper
            .toTriggerExecution(getStub().create(platformWorkflowExecutionProtoMapper.toProto(triggerExecution)));
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteJobTriggerExecution(long jobId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TriggerExecution> fetchJobTriggerExecution(long jobId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TriggerExecution> getJobTriggerExecutions(List<Long> jobIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TriggerExecution> getTriggerExecutions(List<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TriggerExecution getTriggerExecution(long id) {
        return platformWorkflowExecutionProtoMapper.toTriggerExecution(
            getStub().getTriggerExecution(
                IdRequest.newBuilder()
                    .setId(id)
                    .build()));
    }

    @Override
    public TriggerExecution update(TriggerExecution triggerExecution) {
        return platformWorkflowExecutionProtoMapper
            .toTriggerExecution(getStub().update(platformWorkflowExecutionProtoMapper.toProto(triggerExecution)));
    }

    private TriggerExecutionRemoteServiceGrpc.TriggerExecutionRemoteServiceBlockingStub getStub() {
        return TriggerExecutionRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(EXECUTION_APP));
    }
}

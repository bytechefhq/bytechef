/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.facade;

import com.bytechef.ee.platform.workflow.execution.remote.grpc.PlatformWorkflowExecutionProtoMapper;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.JobResumeIdRequest;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.SaveTaskStateRequest;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.TaskStateRemoteServiceGrpc;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.facade.TaskStateFacade;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteTaskStateFacadeClient implements TaskStateFacade {

    private static final String EXECUTION_APP = "execution-app";

    private final GrpcChannels grpcChannels;
    private final PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper;

    public RemoteTaskStateFacadeClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.grpcChannels = grpcChannels;
        this.platformWorkflowExecutionProtoMapper = new PlatformWorkflowExecutionProtoMapper(objectMapper);
    }

    @Override
    public void delete(JobResumeId jobResumeId) {
        getStub().delete(toJobResumeIdRequest(jobResumeId));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> fetchValue(JobResumeId jobResumeId) {
        Object value = platformWorkflowExecutionProtoMapper.toTaskStateObject(
            getStub().fetchValue(toJobResumeIdRequest(jobResumeId)));

        return Optional.ofNullable((T) value);
    }

    @Override
    public <T> Optional<T> fetchUnlockedValue(JobResumeId jobResumeId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save(JobResumeId jobResumeId, Object value) {
        getStub().save(
            SaveTaskStateRequest.newBuilder()
                .setJobResumeId(jobResumeId.toString())
                .setValue(platformWorkflowExecutionProtoMapper.toTaskStateValue(value))
                .build());
    }

    private TaskStateRemoteServiceGrpc.TaskStateRemoteServiceBlockingStub getStub() {
        return TaskStateRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(EXECUTION_APP));
    }

    private static JobResumeIdRequest toJobResumeIdRequest(JobResumeId jobResumeId) {
        return JobResumeIdRequest.newBuilder()
            .setJobResumeId(jobResumeId.toString())
            .build();
    }
}

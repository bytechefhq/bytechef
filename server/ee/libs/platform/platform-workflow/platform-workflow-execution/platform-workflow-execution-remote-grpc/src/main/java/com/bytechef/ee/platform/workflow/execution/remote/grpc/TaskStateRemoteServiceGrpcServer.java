/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.grpc;

import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.JobResumeIdRequest;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.SaveTaskStateRequest;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.TaskStateRemoteServiceGrpc;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.TaskStateValue;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.facade.TaskStateFacade;
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class TaskStateRemoteServiceGrpcServer extends TaskStateRemoteServiceGrpc.TaskStateRemoteServiceImplBase {

    private final PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper;
    private final TaskStateFacade taskStateFacade;

    @SuppressFBWarnings("EI")
    public TaskStateRemoteServiceGrpcServer(
        TaskStateFacade taskStateFacade,
        PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper) {

        this.platformWorkflowExecutionProtoMapper = platformWorkflowExecutionProtoMapper;
        this.taskStateFacade = taskStateFacade;
    }

    @Override
    public void delete(JobResumeIdRequest request, StreamObserver<Empty> responseObserver) {
        taskStateFacade.delete(JobResumeId.parse(request.getJobResumeId()));

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void fetchValue(JobResumeIdRequest request, StreamObserver<TaskStateValue> responseObserver) {
        Optional<Object> valueOptional = taskStateFacade.fetchValue(JobResumeId.parse(request.getJobResumeId()));

        responseObserver.onNext(platformWorkflowExecutionProtoMapper.toTaskStateValue(valueOptional.orElse(null)));
        responseObserver.onCompleted();
    }

    @Override
    public void save(SaveTaskStateRequest request, StreamObserver<Empty> responseObserver) {
        taskStateFacade.save(
            JobResumeId.parse(request.getJobResumeId()),
            platformWorkflowExecutionProtoMapper.toTaskStateObject(request.getValue()));

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}

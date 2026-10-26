/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.grpc;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.BoolResponse;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.IdRequest;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.RemoteTaskExecution;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.RemoteTaskExecutions;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.TaskExecutionRemoteServiceGrpc;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.TaskExecutionResult;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.Optional;

/**
 * @version ee
 */
public class TaskExecutionRemoteServiceGrpcServer
    extends TaskExecutionRemoteServiceGrpc.TaskExecutionRemoteServiceImplBase {

    private final AtlasExecutionProtoMapper atlasExecutionProtoMapper;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI")
    public TaskExecutionRemoteServiceGrpcServer(
        TaskExecutionService taskExecutionService, AtlasExecutionProtoMapper atlasExecutionProtoMapper) {

        this.atlasExecutionProtoMapper = atlasExecutionProtoMapper;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void cancelIfUnfinished(IdRequest request, StreamObserver<BoolResponse> responseObserver) {
        responseObserver.onNext(
            BoolResponse.newBuilder()
                .setValue(taskExecutionService.cancelIfUnfinished(request.getId()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void completeIfNotCancelled(IdRequest request, StreamObserver<BoolResponse> responseObserver) {
        responseObserver.onNext(
            BoolResponse.newBuilder()
                .setValue(taskExecutionService.completeIfNotCancelled(request.getId()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void create(RemoteTaskExecution request, StreamObserver<RemoteTaskExecution> responseObserver) {
        responseObserver.onNext(atlasExecutionProtoMapper
            .toProto(taskExecutionService.create(atlasExecutionProtoMapper.toTaskExecution(request))));
        responseObserver.onCompleted();
    }

    @Override
    public void getJobTaskExecutions(IdRequest request, StreamObserver<RemoteTaskExecutions> responseObserver) {
        responseObserver.onNext(
            atlasExecutionProtoMapper
                .toRemoteTaskExecutions(taskExecutionService.getJobTaskExecutions(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void getParentTaskExecutions(IdRequest request, StreamObserver<RemoteTaskExecutions> responseObserver) {
        responseObserver.onNext(
            atlasExecutionProtoMapper
                .toRemoteTaskExecutions(taskExecutionService.getParentTaskExecutions(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void getTaskExecution(IdRequest request, StreamObserver<RemoteTaskExecution> responseObserver) {
        responseObserver
            .onNext(atlasExecutionProtoMapper.toProto(taskExecutionService.getTaskExecution(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void getTaskExecutionForUpdate(IdRequest request, StreamObserver<RemoteTaskExecution> responseObserver) {
        responseObserver
            .onNext(atlasExecutionProtoMapper.toProto(taskExecutionService.getTaskExecutionForUpdate(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void update(RemoteTaskExecution request, StreamObserver<RemoteTaskExecution> responseObserver) {
        responseObserver.onNext(atlasExecutionProtoMapper
            .toProto(taskExecutionService.update(atlasExecutionProtoMapper.toTaskExecution(request))));
        responseObserver.onCompleted();
    }

    @Override
    public void fetchLastJobTaskExecution(IdRequest request, StreamObserver<TaskExecutionResult> responseObserver) {
        Optional<TaskExecution> taskExecution = taskExecutionService.fetchLastJobTaskExecution(request.getId());

        TaskExecutionResult.Builder builder = TaskExecutionResult.newBuilder();

        taskExecution.ifPresent(
            curTaskExecution -> builder.setTaskExecution(atlasExecutionProtoMapper.toProto(curTaskExecution)));

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}

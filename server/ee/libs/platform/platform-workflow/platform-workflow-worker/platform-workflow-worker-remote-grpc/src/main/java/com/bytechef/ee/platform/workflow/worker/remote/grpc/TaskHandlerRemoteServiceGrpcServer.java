/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.worker.remote.grpc;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.ee.platform.workflow.worker.remote.grpc.proto.HandleTaskRequest;
import com.bytechef.ee.platform.workflow.worker.remote.grpc.proto.HandleTaskResponse;
import com.bytechef.ee.platform.workflow.worker.remote.grpc.proto.TaskHandlerRemoteServiceGrpc;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class TaskHandlerRemoteServiceGrpcServer extends TaskHandlerRemoteServiceGrpc.TaskHandlerRemoteServiceImplBase {

    private final TaskExecutionProtoMapper taskExecutionProtoMapper;
    private final TaskHandlerRegistry taskHandlerRegistry;

    @SuppressFBWarnings("EI")
    public TaskHandlerRemoteServiceGrpcServer(
        TaskHandlerRegistry taskHandlerRegistry, TaskExecutionProtoMapper taskExecutionProtoMapper) {

        this.taskExecutionProtoMapper = taskExecutionProtoMapper;
        this.taskHandlerRegistry = taskHandlerRegistry;
    }

    @Override
    public void handle(HandleTaskRequest request, StreamObserver<HandleTaskResponse> responseObserver) {
        TaskHandler<?> taskHandler = taskHandlerRegistry.getTaskHandler(request.getType());
        TaskExecution taskExecution = taskExecutionProtoMapper.toTaskExecution(request.getTaskExecution());

        Object output;

        try {
            output = taskHandler.handle(taskExecution);
        } catch (TaskExecutionException taskExecutionException) {
            responseObserver.onError(
                Status.INTERNAL.withDescription(taskExecutionException.getMessage())
                    .withCause(taskExecutionException)
                    .asRuntimeException());

            return;
        }

        HandleTaskResponse.Builder builder = HandleTaskResponse.newBuilder();

        if (output != null) {
            builder.setOutput(taskExecutionProtoMapper.toBytes(output));
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}

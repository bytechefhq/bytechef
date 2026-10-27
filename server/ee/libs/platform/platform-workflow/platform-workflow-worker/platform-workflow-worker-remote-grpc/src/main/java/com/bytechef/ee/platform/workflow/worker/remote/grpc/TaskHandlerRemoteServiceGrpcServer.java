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
import com.bytechef.ee.atlas.execution.remote.grpc.TaskExecutionProtoMapper;
import com.bytechef.ee.platform.workflow.worker.remote.grpc.proto.HandleTaskRequest;
import com.bytechef.ee.platform.workflow.worker.remote.grpc.proto.HandleTaskResponse;
import com.bytechef.ee.platform.workflow.worker.remote.grpc.proto.TaskHandlerRemoteServiceGrpc;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.core.task.TaskExecutor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class TaskHandlerRemoteServiceGrpcServer extends TaskHandlerRemoteServiceGrpc.TaskHandlerRemoteServiceImplBase {

    private final TaskExecutionProtoMapper taskExecutionProtoMapper;
    private final TaskHandlerRegistry taskHandlerRegistry;
    private final TaskExecutor workerExecutor;

    @SuppressFBWarnings("EI")
    public TaskHandlerRemoteServiceGrpcServer(
        TaskHandlerRegistry taskHandlerRegistry, TaskExecutionProtoMapper taskExecutionProtoMapper,
        TaskExecutor workerExecutor) {

        this.taskExecutionProtoMapper = taskExecutionProtoMapper;
        this.taskHandlerRegistry = taskHandlerRegistry;
        this.workerExecutor = workerExecutor;
    }

    @Override
    public void handle(HandleTaskRequest request, StreamObserver<HandleTaskResponse> responseObserver) {
        TaskHandler<?> taskHandler = taskHandlerRegistry.getTaskHandler(request.getType());

        if (taskHandler == null) {
            responseObserver.onError(
                Status.INVALID_ARGUMENT.withDescription("Unknown task type: " + request.getType())
                    .asRuntimeException());

            return;
        }

        TaskExecution taskExecution = taskExecutionProtoMapper.toTaskExecution(request.getTaskExecution());

        workerExecutor.execute(() -> handle(taskHandler, taskExecution, responseObserver));
    }

    private void handle(
        TaskHandler<?> taskHandler, TaskExecution taskExecution, StreamObserver<HandleTaskResponse> responseObserver) {

        Object output;

        try {
            output = taskHandler.handle(taskExecution);
        } catch (TaskExecutionException taskExecutionException) {
            responseObserver.onError(
                Status.INTERNAL.withDescription(taskExecutionException.getMessage())
                    .withCause(taskExecutionException)
                    .asRuntimeException());

            return;
        } catch (RuntimeException runtimeException) {
            Class<? extends RuntimeException> runtimeExceptionClass = runtimeException.getClass();

            responseObserver.onError(
                Status.INTERNAL
                    .withDescription(runtimeExceptionClass.getSimpleName() + ": " + runtimeException.getMessage())
                    .withCause(runtimeException)
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

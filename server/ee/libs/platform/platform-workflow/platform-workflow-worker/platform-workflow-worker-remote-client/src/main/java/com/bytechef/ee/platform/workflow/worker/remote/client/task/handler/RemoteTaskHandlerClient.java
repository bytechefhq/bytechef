/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.worker.remote.client.task.handler;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.exception.TaskExecutionException;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.discovery.util.WorkerDiscoveryUtils;
import com.bytechef.ee.platform.workflow.worker.remote.grpc.TaskExecutionProtoMapper;
import com.bytechef.ee.platform.workflow.worker.remote.grpc.proto.HandleTaskRequest;
import com.bytechef.ee.platform.workflow.worker.remote.grpc.proto.HandleTaskResponse;
import com.bytechef.ee.platform.workflow.worker.remote.grpc.proto.TaskHandlerRemoteServiceGrpc;
import com.bytechef.ee.platform.workflow.worker.remote.grpc.proto.TaskHandlerRemoteServiceGrpc.TaskHandlerRemoteServiceBlockingStub;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.ee.remote.grpc.ProtoValues;
import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Hands a task to the worker that offers its component, or to any worker when none advertises it.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteTaskHandlerClient {

    private static final String WORKER_APP = "worker-app";

    private final DiscoveryClient discoveryClient;
    private final Duration executionDeadline;
    private final GrpcChannels grpcChannels;
    private final TaskExecutionProtoMapper taskExecutionProtoMapper;

    public RemoteTaskHandlerClient(
        ApplicationProperties applicationProperties, GrpcChannels grpcChannels, DiscoveryClient discoveryClient,
        ObjectMapper objectMapper) {

        ApplicationProperties.Remote remote = applicationProperties.getRemote();
        ApplicationProperties.Remote.Grpc grpc = remote.getGrpc();

        this.discoveryClient = discoveryClient;
        this.executionDeadline = grpc.getExecutionDeadline();
        this.grpcChannels = grpcChannels;
        this.taskExecutionProtoMapper = new TaskExecutionProtoMapper(objectMapper);
    }

    public Object handle(String type, TaskExecution taskExecution) throws TaskExecutionException {
        HandleTaskRequest handleTaskRequest = HandleTaskRequest.newBuilder()
            .setType(type)
            .setTaskExecution(taskExecutionProtoMapper.toProto(taskExecution))
            .build();

        HandleTaskResponse handleTaskResponse;

        try {
            handleTaskResponse = getStub(type).handle(handleTaskRequest);
        } catch (StatusRuntimeException statusRuntimeException) {
            Status status = statusRuntimeException.getStatus();

            String description = status.getDescription();

            throw new TaskExecutionException(
                description == null ? statusRuntimeException.getMessage() : description, statusRuntimeException);
        }

        return handleTaskResponse.hasOutput() ? ProtoValues.toObject(handleTaskResponse.getOutput()) : null;
    }

    private Channel getChannel(String type) {
        Optional<ServiceInstance> serviceInstance = WorkerDiscoveryUtils.fetchServiceInstance(
            discoveryClient.getInstances(WORKER_APP), StringUtils.substringBefore(type, "/"));

        return serviceInstance.map(grpcChannels::getChannel)
            .orElseGet(() -> grpcChannels.getChannel(WORKER_APP));
    }

    private TaskHandlerRemoteServiceBlockingStub getStub(String type) {
        TaskHandlerRemoteServiceBlockingStub stub = TaskHandlerRemoteServiceGrpc.newBlockingStub(getChannel(type));

        if (executionDeadline == null) {
            return stub;
        }

        return stub.withDeadlineAfter(executionDeadline.toMillis(), TimeUnit.MILLISECONDS);
    }
}

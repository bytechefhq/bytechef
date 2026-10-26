/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc;

import com.bytechef.ee.platform.component.remote.grpc.proto.BoolResponse;
import com.bytechef.ee.platform.component.remote.grpc.proto.OutputResponseResult;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteOptions;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteProperties;
import com.bytechef.ee.platform.component.remote.grpc.proto.StringResponse;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.ExecuteTaskDispatcherDynamicPropertiesRequest;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.ExecuteTaskDispatcherOptionsRequest;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.ExecuteTaskDispatcherOutputRequest;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.GetTaskDispatcherDefinitionRequest;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.GetTaskDispatcherDefinitionVersionsRequest;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.IsDynamicOutputDefinedRequest;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.RemoteTaskDispatcherDefinition;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.RemoteTaskDispatcherDefinitions;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.TaskDispatcherDefinitionRemoteServiceGrpc;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.TaskDispatcherDefinitionResult;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class TaskDispatcherDefinitionRemoteServiceGrpcServer
    extends TaskDispatcherDefinitionRemoteServiceGrpc.TaskDispatcherDefinitionRemoteServiceImplBase {

    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;
    private final TaskDispatcherProtoMapper taskDispatcherProtoMapper;

    public TaskDispatcherDefinitionRemoteServiceGrpcServer(
        TaskDispatcherDefinitionService taskDispatcherDefinitionService,
        TaskDispatcherProtoMapper taskDispatcherProtoMapper) {

        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
        this.taskDispatcherProtoMapper = taskDispatcherProtoMapper;
    }

    @Override
    public void executeDynamicProperties(
        ExecuteTaskDispatcherDynamicPropertiesRequest request, StreamObserver<RemoteProperties> responseObserver) {

        responseObserver.onNext(
            taskDispatcherProtoMapper.toRemoteProperties(
                taskDispatcherDefinitionService.executeDynamicProperties(
                    request.getName(), request.getVersion(), request.getPropertyName(),
                    taskDispatcherProtoMapper.toMap(request.getInputParameters()))));
        responseObserver.onCompleted();
    }

    @Override
    public void executeOptions(
        ExecuteTaskDispatcherOptionsRequest request, StreamObserver<RemoteOptions> responseObserver) {

        responseObserver.onNext(
            taskDispatcherProtoMapper.toRemoteOptions(
                taskDispatcherDefinitionService.executeOptions(
                    request.getName(), request.getVersion(), request.getPropertyName(),
                    request.hasSearch() ? request.getSearch() : null)));
        responseObserver.onCompleted();
    }

    @Override
    public void executeOutput(
        ExecuteTaskDispatcherOutputRequest request, StreamObserver<OutputResponseResult> responseObserver) {

        responseObserver.onNext(
            taskDispatcherProtoMapper.toOutputResponseResult(
                taskDispatcherDefinitionService.executeOutput(
                    request.getName(), request.getVersion(),
                    taskDispatcherProtoMapper.toMap(request.getInputParameters()))));
        responseObserver.onCompleted();
    }

    @Override
    public void executeVariableProperties(
        ExecuteTaskDispatcherOutputRequest request, StreamObserver<OutputResponseResult> responseObserver) {

        responseObserver.onNext(
            taskDispatcherProtoMapper.toOutputResponseResult(
                taskDispatcherDefinitionService.executeVariableProperties(
                    request.getName(), request.getVersion(),
                    taskDispatcherProtoMapper.toMap(request.getInputParameters()))));
        responseObserver.onCompleted();
    }

    @Override
    public void executeWorkflowNodeDescription(
        ExecuteTaskDispatcherOutputRequest request, StreamObserver<StringResponse> responseObserver) {

        String workflowNodeDescription = taskDispatcherDefinitionService.executeWorkflowNodeDescription(
            request.getName(), request.getVersion(), taskDispatcherProtoMapper.toMap(request.getInputParameters()));

        StringResponse.Builder builder = StringResponse.newBuilder();

        if (workflowNodeDescription != null) {
            builder.setValue(workflowNodeDescription);
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void fetchTaskDispatcherDefinition(
        GetTaskDispatcherDefinitionRequest request, StreamObserver<TaskDispatcherDefinitionResult> responseObserver) {

        Optional<TaskDispatcherDefinition> taskDispatcherDefinition =
            taskDispatcherDefinitionService.fetchTaskDispatcherDefinition(
                request.getName(), request.hasVersion() ? request.getVersion() : null);

        TaskDispatcherDefinitionResult.Builder builder = TaskDispatcherDefinitionResult.newBuilder();

        taskDispatcherDefinition.ifPresent(
            curTaskDispatcherDefinition -> builder.setTaskDispatcherDefinition(
                taskDispatcherProtoMapper.toProto(curTaskDispatcherDefinition)));

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getTaskDispatcherDefinition(
        GetTaskDispatcherDefinitionRequest request, StreamObserver<RemoteTaskDispatcherDefinition> responseObserver) {

        responseObserver.onNext(
            taskDispatcherProtoMapper.toProto(
                taskDispatcherDefinitionService.getTaskDispatcherDefinition(
                    request.getName(), request.hasVersion() ? request.getVersion() : null)));
        responseObserver.onCompleted();
    }

    @Override
    public void getTaskDispatcherDefinitions(
        Empty request, StreamObserver<RemoteTaskDispatcherDefinitions> responseObserver) {

        responseObserver.onNext(
            taskDispatcherProtoMapper.toProto(taskDispatcherDefinitionService.getTaskDispatcherDefinitions()));
        responseObserver.onCompleted();
    }

    @Override
    public void getTaskDispatcherDefinitionVersions(
        GetTaskDispatcherDefinitionVersionsRequest request,
        StreamObserver<RemoteTaskDispatcherDefinitions> responseObserver) {

        responseObserver.onNext(
            taskDispatcherProtoMapper.toProto(
                taskDispatcherDefinitionService.getTaskDispatcherDefinitionVersions(request.getName())));
        responseObserver.onCompleted();
    }

    @Override
    public void isDynamicOutputDefined(
        IsDynamicOutputDefinedRequest request, StreamObserver<BoolResponse> responseObserver) {

        responseObserver.onNext(
            BoolResponse.newBuilder()
                .setValue(
                    taskDispatcherDefinitionService.isDynamicOutputDefined(request.getName(), request.getVersion()))
                .build());
        responseObserver.onCompleted();
    }
}

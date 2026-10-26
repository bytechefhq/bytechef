/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc;

import com.bytechef.ee.platform.component.remote.grpc.proto.OutputResponseResult;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.ExecuteTaskDispatcherOutputRequest;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.GetTaskDispatcherDefinitionRequest;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.GetTaskDispatcherDefinitionVersionsRequest;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.RemoteTaskDispatcherDefinition;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.RemoteTaskDispatcherDefinitions;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.TaskDispatcherDefinitionRemoteServiceGrpc;
import com.bytechef.ee.remote.grpc.ProtoValues;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;

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
    public void executeOutput(
        ExecuteTaskDispatcherOutputRequest request, StreamObserver<OutputResponseResult> responseObserver) {

        responseObserver.onNext(
            taskDispatcherProtoMapper.toOutputResponseResult(
                taskDispatcherDefinitionService.executeOutput(
                    request.getName(), request.getVersion(), ProtoValues.toMap(request.getInputParameters()))));
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
}

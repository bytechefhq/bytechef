/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.task.dispatcher.registry.remote.client.service;

import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.TaskDispatcherProtoMapper;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.ExecuteTaskDispatcherOutputRequest;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.GetTaskDispatcherDefinitionRequest;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.GetTaskDispatcherDefinitionVersionsRequest;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.TaskDispatcherDefinitionRemoteServiceGrpc;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.TaskDispatcherDefinitionRemoteServiceGrpc.TaskDispatcherDefinitionRemoteServiceBlockingStub;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.ee.remote.grpc.ProtoValues;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.domain.Option;
import com.bytechef.platform.workflow.task.dispatcher.domain.Property;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.google.protobuf.Empty;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteTaskDispatcherDefinitionServiceClient implements TaskDispatcherDefinitionService {

    private static final String COORDINATOR_APP = "coordinator-app";

    private final GrpcChannels grpcChannels;
    private final TaskDispatcherProtoMapper taskDispatcherProtoMapper;

    public RemoteTaskDispatcherDefinitionServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.grpcChannels = grpcChannels;
        this.taskDispatcherProtoMapper = new TaskDispatcherProtoMapper(objectMapper);
    }

    @Override
    public List<Property> executeDynamicProperties(
        String name, int version, String propertyName, Map<String, ?> inputParameters) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Option> executeOptions(String name, int version, String propertyName, String search) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputResponse executeOutput(String name, int version, Map<String, ?> inputParameters) {
        ExecuteTaskDispatcherOutputRequest executeTaskDispatcherOutputRequest =
            ExecuteTaskDispatcherOutputRequest.newBuilder()
                .setName(name)
                .setVersion(version)
                .setInputParameters(ProtoValues.toStruct(inputParameters))
                .build();

        return taskDispatcherProtoMapper.toOutputResponse(getStub().executeOutput(executeTaskDispatcherOutputRequest));
    }

    @Override
    public OutputResponse executeVariableProperties(String name, int version, Map<String, ?> inputParameters) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String executeWorkflowNodeDescription(String name, int version, Map<String, ?> inputParameters) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TaskDispatcherDefinition> fetchTaskDispatcherDefinition(String name, @Nullable Integer version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskDispatcherDefinition getTaskDispatcherDefinition(String name, @Nullable Integer version) {
        GetTaskDispatcherDefinitionRequest.Builder builder = GetTaskDispatcherDefinitionRequest.newBuilder()
            .setName(name);

        if (version != null) {
            builder.setVersion(version);
        }

        return taskDispatcherProtoMapper.toTaskDispatcherDefinition(
            getStub().getTaskDispatcherDefinition(builder.build()));
    }

    @Override
    public List<TaskDispatcherDefinition> getTaskDispatcherDefinitions() {
        return taskDispatcherProtoMapper.toTaskDispatcherDefinitions(
            getStub().getTaskDispatcherDefinitions(Empty.getDefaultInstance()));
    }

    @Override
    public List<TaskDispatcherDefinition> getTaskDispatcherDefinitionVersions(String name) {
        GetTaskDispatcherDefinitionVersionsRequest getTaskDispatcherDefinitionVersionsRequest =
            GetTaskDispatcherDefinitionVersionsRequest.newBuilder()
                .setName(name)
                .build();

        return taskDispatcherProtoMapper.toTaskDispatcherDefinitions(
            getStub().getTaskDispatcherDefinitionVersions(getTaskDispatcherDefinitionVersionsRequest));
    }

    @Override
    public boolean isDynamicOutputDefined(String componentName, int componentVersion) {
        throw new UnsupportedOperationException();
    }

    private TaskDispatcherDefinitionRemoteServiceBlockingStub getStub() {
        return TaskDispatcherDefinitionRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(COORDINATOR_APP));
    }
}

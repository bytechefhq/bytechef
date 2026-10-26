/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc;

import com.bytechef.ee.platform.component.remote.grpc.proto.OutputResponseResult;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteOutputResponse;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.RemoteTaskDispatcherDefinition;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.RemoteTaskDispatcherDefinitions;
import com.bytechef.ee.remote.grpc.ProtoJsonMapper;
import com.bytechef.platform.domain.BaseProperty;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.domain.Property;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.ObjectMapper;

/**
 * Maps task dispatcher definitions to their proto messages and back through {@link ProtoJsonMapper}, reading the
 * abstract {@link BaseProperty} as a task dispatcher {@link Property}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class TaskDispatcherProtoMapper {

    private final ProtoJsonMapper protoJsonMapper;

    public TaskDispatcherProtoMapper(ObjectMapper objectMapper) {
        this.protoJsonMapper = new ProtoJsonMapper(
            objectMapper.rebuild()
                .addMixIn(BaseProperty.class, TaskDispatcherBasePropertyMixIn.class)
                .build());
    }

    public @Nullable OutputResponse toOutputResponse(OutputResponseResult outputResponseResult) {
        return outputResponseResult.hasOutputResponse()
            ? protoJsonMapper.toObject(outputResponseResult.getOutputResponse(), OutputResponse.class) : null;
    }

    public OutputResponseResult toOutputResponseResult(@Nullable OutputResponse outputResponse) {
        OutputResponseResult.Builder builder = OutputResponseResult.newBuilder();

        if (outputResponse != null) {
            builder.setOutputResponse(
                protoJsonMapper.<RemoteOutputResponse>toProto(outputResponse, RemoteOutputResponse.newBuilder()));
        }

        return builder.build();
    }

    public RemoteTaskDispatcherDefinition toProto(TaskDispatcherDefinition taskDispatcherDefinition) {
        return protoJsonMapper.toProto(taskDispatcherDefinition, RemoteTaskDispatcherDefinition.newBuilder());
    }

    public RemoteTaskDispatcherDefinitions toProto(List<TaskDispatcherDefinition> taskDispatcherDefinitions) {
        RemoteTaskDispatcherDefinitions.Builder builder = RemoteTaskDispatcherDefinitions.newBuilder();

        for (TaskDispatcherDefinition taskDispatcherDefinition : taskDispatcherDefinitions) {
            builder.addTaskDispatcherDefinitions(toProto(taskDispatcherDefinition));
        }

        return builder.build();
    }

    public TaskDispatcherDefinition toTaskDispatcherDefinition(
        RemoteTaskDispatcherDefinition remoteTaskDispatcherDefinition) {

        return protoJsonMapper.toObject(remoteTaskDispatcherDefinition, TaskDispatcherDefinition.class);
    }

    public List<TaskDispatcherDefinition> toTaskDispatcherDefinitions(
        RemoteTaskDispatcherDefinitions remoteTaskDispatcherDefinitions) {

        return remoteTaskDispatcherDefinitions.getTaskDispatcherDefinitionsList()
            .stream()
            .map(this::toTaskDispatcherDefinition)
            .toList();
    }

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
    @JsonSubTypes(@JsonSubTypes.Type(Property.class))
    private abstract static class TaskDispatcherBasePropertyMixIn {
    }
}

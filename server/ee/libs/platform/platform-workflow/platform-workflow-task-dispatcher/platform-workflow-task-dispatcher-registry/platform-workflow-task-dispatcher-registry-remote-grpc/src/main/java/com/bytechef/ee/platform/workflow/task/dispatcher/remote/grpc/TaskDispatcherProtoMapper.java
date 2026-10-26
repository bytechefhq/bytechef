/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc;

import com.bytechef.ee.platform.component.remote.grpc.proto.OutputResponseResult;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteOption;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteOptions;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteOutputResponse;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteProperties;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteProperty;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.RemoteTaskDispatcherDefinition;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.proto.RemoteTaskDispatcherDefinitions;
import com.bytechef.ee.remote.grpc.JsonBytes;
import com.bytechef.ee.remote.grpc.ProtoJsonMapper;
import com.bytechef.platform.domain.BaseProperty;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.domain.Option;
import com.bytechef.platform.workflow.task.dispatcher.domain.Property;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Map;
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

    private final JsonBytes jsonBytes;
    private final ProtoJsonMapper protoJsonMapper;

    public TaskDispatcherProtoMapper(ObjectMapper objectMapper) {
        this.jsonBytes = new JsonBytes(objectMapper);
        this.protoJsonMapper = new ProtoJsonMapper(
            objectMapper.rebuild()
                .addMixIn(BaseProperty.class, TaskDispatcherBasePropertyMixIn.class)
                .build());
    }

    public ByteString toBytes(@Nullable Object value) {
        return jsonBytes.write(value);
    }

    public @Nullable Map<String, Object> toMap(ByteString bytes) {
        return jsonBytes.readMap(bytes);
    }

    public @Nullable Object toObject(ByteString bytes) {
        return jsonBytes.read(bytes);
    }

    public List<Option> toOptions(RemoteOptions remoteOptions) {
        return remoteOptions.getOptionsList()
            .stream()
            .map(remoteOption -> protoJsonMapper.toObject(remoteOption, Option.class))
            .toList();
    }

    public List<Property> toProperties(RemoteProperties remoteProperties) {
        return remoteProperties.getPropertiesList()
            .stream()
            .map(remoteProperty -> protoJsonMapper.toObject(remoteProperty, Property.class))
            .toList();
    }

    public RemoteOptions toRemoteOptions(List<Option> options) {
        RemoteOptions.Builder builder = RemoteOptions.newBuilder();

        for (Option option : options) {
            builder.addOptions(protoJsonMapper.<RemoteOption>toProto(option, RemoteOption.newBuilder()));
        }

        return builder.build();
    }

    public RemoteProperties toRemoteProperties(List<? extends Property> properties) {
        RemoteProperties.Builder builder = RemoteProperties.newBuilder();

        for (Property property : properties) {
            builder.addProperties(protoJsonMapper.<RemoteProperty>toProto(property, RemoteProperty.newBuilder()));
        }

        return builder.build();
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

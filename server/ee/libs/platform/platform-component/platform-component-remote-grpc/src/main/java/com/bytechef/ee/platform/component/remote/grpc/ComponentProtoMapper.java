/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.grpc;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.ee.platform.component.remote.grpc.proto.OutputResponseResult;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteActionDefinition;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteClusterElementDefinition;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteClusterElementType;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteComponentDefinition;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteConnectionDefinition;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteOption;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteOptions;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteOutputResponse;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteProperties;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteProperty;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteTriggerDefinition;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteTriggerOutput;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteWebhookEnableOutput;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteWebhookRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteWebhookTriggerFlags;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteWebhookValidateResponse;
import com.bytechef.ee.platform.component.remote.grpc.proto.WebhookEnableOutputResult;
import com.bytechef.ee.remote.grpc.JsonBytes;
import com.bytechef.ee.remote.grpc.ProtoJsonMapper;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.domain.BaseProperty;
import com.bytechef.platform.domain.OutputResponse;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.ObjectMapper;

/**
 * Maps component definitions and component execution results to their proto messages and back through
 * {@link ProtoJsonMapper}, reading the abstract {@link BaseProperty} as a component {@link Property}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ComponentProtoMapper {

    private final JsonBytes jsonBytes;
    private final ProtoJsonMapper protoJsonMapper;

    public ComponentProtoMapper(ObjectMapper objectMapper) {
        this.jsonBytes = new JsonBytes(objectMapper);
        this.protoJsonMapper = new ProtoJsonMapper(
            objectMapper.rebuild()
                .addMixIn(BaseProperty.class, ComponentBasePropertyMixIn.class)
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
            .map(this::toOption)
            .toList();
    }

    public @Nullable OutputResponse toOutputResponse(OutputResponseResult outputResponseResult) {
        return outputResponseResult.hasOutputResponse() ? toOutputResponse(outputResponseResult.getOutputResponse())
            : null;
    }

    public OutputResponseResult toOutputResponseResult(@Nullable OutputResponse outputResponse) {
        OutputResponseResult.Builder builder = OutputResponseResult.newBuilder();

        if (outputResponse != null) {
            builder.setOutputResponse(toProto(outputResponse));
        }

        return builder.build();
    }

    public List<Property> toProperties(RemoteProperties remoteProperties) {
        return remoteProperties.getPropertiesList()
            .stream()
            .map(this::toProperty)
            .toList();
    }

    public RemoteOptions toRemoteOptions(List<Option> options) {
        RemoteOptions.Builder builder = RemoteOptions.newBuilder();

        for (Option option : options) {
            builder.addOptions(toProto(option));
        }

        return builder.build();
    }

    public RemoteProperties toRemoteProperties(List<? extends Property> properties) {
        RemoteProperties.Builder builder = RemoteProperties.newBuilder();

        for (Property property : properties) {
            builder.addProperties(toProto(property));
        }

        return builder.build();
    }

    public @Nullable WebhookEnableOutput toWebhookEnableOutput(WebhookEnableOutputResult webhookEnableOutputResult) {
        return webhookEnableOutputResult.hasWebhookEnableOutput()
            ? toWebhookEnableOutput(webhookEnableOutputResult.getWebhookEnableOutput()) : null;
    }

    public WebhookEnableOutputResult toWebhookEnableOutputResult(@Nullable WebhookEnableOutput webhookEnableOutput) {
        WebhookEnableOutputResult.Builder builder = WebhookEnableOutputResult.newBuilder();

        if (webhookEnableOutput != null) {
            builder.setWebhookEnableOutput(toProto(webhookEnableOutput));
        }

        return builder.build();
    }

    public ActionDefinition toActionDefinition(RemoteActionDefinition remoteActionDefinition) {
        return toDomain(remoteActionDefinition, ActionDefinition.class);
    }

    public ClusterElementDefinition toClusterElementDefinition(
        RemoteClusterElementDefinition remoteClusterElementDefinition) {

        return toDomain(remoteClusterElementDefinition, ClusterElementDefinition.class);
    }

    public ClusterElementType toClusterElementType(RemoteClusterElementType remoteClusterElementType) {
        return toDomain(remoteClusterElementType, ClusterElementType.class);
    }

    public ComponentDefinition toComponentDefinition(RemoteComponentDefinition remoteComponentDefinition) {
        return toDomain(remoteComponentDefinition, ComponentDefinition.class);
    }

    public ConnectionDefinition toConnectionDefinition(RemoteConnectionDefinition remoteConnectionDefinition) {
        return toDomain(remoteConnectionDefinition, ConnectionDefinition.class);
    }

    public Option toOption(RemoteOption remoteOption) {
        return toDomain(remoteOption, Option.class);
    }

    public OutputResponse toOutputResponse(RemoteOutputResponse remoteOutputResponse) {
        return toDomain(remoteOutputResponse, OutputResponse.class);
    }

    public Property toProperty(RemoteProperty remoteProperty) {
        return toDomain(remoteProperty, Property.class);
    }

    public RemoteActionDefinition toProto(ActionDefinition actionDefinition) {
        return toProto(actionDefinition, RemoteActionDefinition.newBuilder());
    }

    public RemoteClusterElementDefinition toProto(ClusterElementDefinition clusterElementDefinition) {
        return toProto(clusterElementDefinition, RemoteClusterElementDefinition.newBuilder());
    }

    public RemoteClusterElementType toProto(ClusterElementType clusterElementType) {
        return toProto(clusterElementType, RemoteClusterElementType.newBuilder());
    }

    public RemoteComponentDefinition toProto(ComponentDefinition componentDefinition) {
        return toProto(componentDefinition, RemoteComponentDefinition.newBuilder());
    }

    public RemoteConnectionDefinition toProto(ConnectionDefinition connectionDefinition) {
        return toProto(connectionDefinition, RemoteConnectionDefinition.newBuilder());
    }

    public RemoteOption toProto(Option option) {
        return toProto(option, RemoteOption.newBuilder());
    }

    public RemoteOutputResponse toProto(OutputResponse outputResponse) {
        return toProto(outputResponse, RemoteOutputResponse.newBuilder());
    }

    public RemoteProperty toProto(Property property) {
        return toProto(property, RemoteProperty.newBuilder());
    }

    public RemoteTriggerDefinition toProto(TriggerDefinition triggerDefinition) {
        return toProto(triggerDefinition, RemoteTriggerDefinition.newBuilder());
    }

    public RemoteTriggerOutput toProto(TriggerOutput triggerOutput) {
        return toProto(triggerOutput, RemoteTriggerOutput.newBuilder());
    }

    public RemoteWebhookEnableOutput toProto(WebhookEnableOutput webhookEnableOutput) {
        return toProto(webhookEnableOutput, RemoteWebhookEnableOutput.newBuilder());
    }

    public RemoteWebhookRequest toProto(WebhookRequest webhookRequest) {
        return toProto(webhookRequest, RemoteWebhookRequest.newBuilder());
    }

    public RemoteWebhookTriggerFlags toProto(WebhookTriggerFlags webhookTriggerFlags) {
        return toProto(webhookTriggerFlags, RemoteWebhookTriggerFlags.newBuilder());
    }

    public RemoteWebhookValidateResponse toProto(WebhookValidateResponse webhookValidateResponse) {
        return toProto(webhookValidateResponse, RemoteWebhookValidateResponse.newBuilder());
    }

    public TriggerDefinition toTriggerDefinition(RemoteTriggerDefinition remoteTriggerDefinition) {
        return toDomain(remoteTriggerDefinition, TriggerDefinition.class);
    }

    public TriggerOutput toTriggerOutput(RemoteTriggerOutput remoteTriggerOutput) {
        return toDomain(remoteTriggerOutput, TriggerOutput.class);
    }

    public WebhookEnableOutput toWebhookEnableOutput(RemoteWebhookEnableOutput remoteWebhookEnableOutput) {
        return toDomain(remoteWebhookEnableOutput, WebhookEnableOutput.class);
    }

    public WebhookRequest toWebhookRequest(RemoteWebhookRequest remoteWebhookRequest) {
        return toDomain(remoteWebhookRequest, WebhookRequest.class);
    }

    public WebhookTriggerFlags toWebhookTriggerFlags(RemoteWebhookTriggerFlags remoteWebhookTriggerFlags) {
        return toDomain(remoteWebhookTriggerFlags, WebhookTriggerFlags.class);
    }

    public WebhookValidateResponse toWebhookValidateResponse(
        RemoteWebhookValidateResponse remoteWebhookValidateResponse) {

        return toDomain(remoteWebhookValidateResponse, WebhookValidateResponse.class);
    }

    private <T> T toDomain(MessageOrBuilder messageOrBuilder, Class<T> valueType) {
        return protoJsonMapper.toObject(messageOrBuilder, valueType);
    }

    private <M extends Message> M toProto(Object value, Message.Builder builder) {
        return protoJsonMapper.toProto(value, builder);
    }

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
    @JsonSubTypes(@JsonSubTypes.Type(Property.class))
    private abstract static class ComponentBasePropertyMixIn {
    }
}

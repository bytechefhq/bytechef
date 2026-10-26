/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.grpc;

import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteActionDefinition;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteComponentDefinition;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteConnectionDefinition;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteOption;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteOutputResponse;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteProperty;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteTriggerDefinition;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteTriggerOutput;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteWebhookEnableOutput;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteWebhookRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteWebhookTriggerFlags;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteWebhookValidateResponse;
import com.bytechef.platform.component.domain.ActionDefinition;
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
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.LongNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Maps component definitions and component execution results to their proto messages and back. Both directions go
 * through JSON: the proto schema fixes the wire contract and rejects any domain field it does not declare, while the
 * domain objects are read back the way Jackson builds them.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ComponentProtoMapper {

    private static final JsonFormat.Parser PARSER = JsonFormat.parser();
    private static final JsonFormat.Printer PRINTER = JsonFormat.printer()
        .alwaysPrintFieldsWithNoPresence();

    private final ObjectMapper objectMapper;

    public ComponentProtoMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.rebuild()
            .addMixIn(BaseProperty.class, ComponentBasePropertyMixIn.class)
            .build();
    }

    public ActionDefinition toActionDefinition(RemoteActionDefinition remoteActionDefinition) {
        return toDomain(remoteActionDefinition, ActionDefinition.class);
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
        try {
            JsonNode jsonNode = objectMapper.readTree(PRINTER.print(messageOrBuilder));

            return objectMapper.treeToValue(toIntegralNumbers(jsonNode), valueType);
        } catch (InvalidProtocolBufferException invalidProtocolBufferException) {
            throw new IllegalStateException(
                "Unable to read %s from its proto message".formatted(valueType.getSimpleName()),
                invalidProtocolBufferException);
        }
    }

    private static JsonNode toIntegralNumbers(JsonNode jsonNode) {
        if (jsonNode.isDouble()) {
            double number = jsonNode.doubleValue();

            if (number == Math.rint(number) && Math.abs(number) < 0x1p53) {
                long longValue = (long) number;

                return longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE
                    ? IntNode.valueOf((int) longValue) : LongNode.valueOf(longValue);
            }

            return jsonNode;
        }

        if (jsonNode instanceof ObjectNode objectNode) {
            for (Map.Entry<String, JsonNode> entry : objectNode.properties()) {
                entry.setValue(toIntegralNumbers(entry.getValue()));
            }
        } else if (jsonNode instanceof ArrayNode arrayNode) {
            for (int index = 0; index < arrayNode.size(); index++) {
                arrayNode.set(index, toIntegralNumbers(arrayNode.get(index)));
            }
        }

        return jsonNode;
    }

    @SuppressWarnings("unchecked")
    private <M extends Message> M toProto(Object value, Message.Builder builder) {
        try {
            PARSER.merge(objectMapper.writeValueAsString(value), builder);
        } catch (InvalidProtocolBufferException invalidProtocolBufferException) {
            throw new IllegalStateException(
                "Unable to write %s to its proto message".formatted(
                    value.getClass()
                        .getSimpleName()),
                invalidProtocolBufferException);
        }

        return (M) builder.build();
    }

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
    @JsonSubTypes(@JsonSubTypes.Type(Property.class))
    private abstract static class ComponentBasePropertyMixIn {
    }
}

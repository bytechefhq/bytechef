/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import com.bytechef.commons.util.EncodingUtils;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.function.UnaryOperator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

/**
 * Moves objects between Java and proto messages through JSON: the proto schema fixes the wire contract and rejects any
 * field it does not declare, while the Java objects are written and read the way Jackson handles them. A {@code bytes}
 * field carries the Jackson JSON of the property with the same name, so free-form values keep their Java types; an
 * empty one reads as {@code null}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ProtoJsonMapper {

    private static final JsonFormat.Parser PARSER = JsonFormat.parser();
    private static final JsonFormat.Printer PRINTER = JsonFormat.printer()
        .alwaysPrintFieldsWithNoPresence();

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI")
    public ProtoJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T toObject(MessageOrBuilder messageOrBuilder, Class<T> valueType) {
        try {
            JsonNode jsonNode = objectMapper.readTree(PRINTER.print(messageOrBuilder));

            return objectMapper.treeToValue(
                transformBytes(jsonNode, messageOrBuilder.getDescriptorForType(), this::decode), valueType);
        } catch (InvalidProtocolBufferException invalidProtocolBufferException) {
            throw new IllegalStateException(
                "Unable to read %s from its proto message".formatted(valueType.getSimpleName()),
                invalidProtocolBufferException);
        }
    }

    @SuppressWarnings("unchecked")
    public <M extends Message> M toProto(Object value, Message.Builder builder) {
        try {
            JsonNode jsonNode = objectMapper.valueToTree(value);

            PARSER.merge(
                objectMapper.writeValueAsString(
                    transformBytes(jsonNode, builder.getDescriptorForType(), this::encode)),
                builder);
        } catch (InvalidProtocolBufferException invalidProtocolBufferException) {
            throw new IllegalStateException(
                "Unable to write %s to its proto message".formatted(
                    value.getClass()
                        .getSimpleName()),
                invalidProtocolBufferException);
        }

        return (M) builder.build();
    }

    private JsonNode decode(JsonNode jsonNode) {
        String base64 = jsonNode.asString();

        if (base64.isEmpty()) {
            return NullNode.getInstance();
        }

        return objectMapper.readTree(EncodingUtils.base64Decode(base64));
    }

    private JsonNode encode(JsonNode jsonNode) {
        if (jsonNode.isNull()) {
            return StringNode.valueOf("");
        }

        return StringNode.valueOf(EncodingUtils.base64EncodeToString(objectMapper.writeValueAsBytes(jsonNode)));
    }

    private static JsonNode transformBytes(
        JsonNode jsonNode, Descriptors.Descriptor descriptor, UnaryOperator<JsonNode> bytesTransformer) {

        if (!(jsonNode instanceof ObjectNode objectNode)) {
            return jsonNode;
        }

        for (Descriptors.FieldDescriptor fieldDescriptor : descriptor.getFields()) {
            String name = fieldDescriptor.getJsonName();
            JsonNode fieldNode = objectNode.get(name);

            if (fieldNode == null) {
                continue;
            }

            if (fieldDescriptor.getType() == Descriptors.FieldDescriptor.Type.BYTES) {
                if (fieldDescriptor.isRepeated() && fieldNode instanceof ArrayNode arrayNode) {
                    for (int index = 0; index < arrayNode.size(); index++) {
                        arrayNode.set(index, bytesTransformer.apply(arrayNode.get(index)));
                    }
                } else {
                    JsonNode transformedNode = bytesTransformer.apply(fieldNode);

                    if (transformedNode.isNull()) {
                        objectNode.remove(name);
                    } else {
                        objectNode.set(name, transformedNode);
                    }
                }
            } else if (fieldDescriptor.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
                if (fieldDescriptor.isMapField()) {
                    Descriptors.FieldDescriptor valueFieldDescriptor = fieldDescriptor.getMessageType()
                        .findFieldByName("value");

                    if (valueFieldDescriptor.getType() == Descriptors.FieldDescriptor.Type.BYTES) {
                        for (Map.Entry<String, JsonNode> entry : fieldNode.properties()) {
                            entry.setValue(bytesTransformer.apply(entry.getValue()));
                        }
                    } else if (valueFieldDescriptor.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
                        for (Map.Entry<String, JsonNode> entry : fieldNode.properties()) {
                            transformBytes(entry.getValue(), valueFieldDescriptor.getMessageType(), bytesTransformer);
                        }
                    }
                } else if (fieldDescriptor.isRepeated()) {
                    for (JsonNode itemNode : fieldNode) {
                        transformBytes(itemNode, fieldDescriptor.getMessageType(), bytesTransformer);
                    }
                } else {
                    transformBytes(fieldNode, fieldDescriptor.getMessageType(), bytesTransformer);
                }
            }
        }

        return objectNode;
    }
}

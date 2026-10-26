/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.LongNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Moves objects between Java and proto messages through JSON: the proto schema fixes the wire contract and rejects any
 * field it does not declare, while the Java objects are written and read the way Jackson handles them. Integral numbers
 * in free-form values come back as {@link Integer} when they fit, else {@link Long}.
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

            return objectMapper.treeToValue(toIntegralNumbers(jsonNode), valueType);
        } catch (InvalidProtocolBufferException invalidProtocolBufferException) {
            throw new IllegalStateException(
                "Unable to read %s from its proto message".formatted(valueType.getSimpleName()),
                invalidProtocolBufferException);
        }
    }

    @SuppressWarnings("unchecked")
    public <M extends Message> M toProto(Object value, Message.Builder builder) {
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
}

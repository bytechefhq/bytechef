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
import java.math.BigInteger;
import java.util.Map;
import java.util.function.UnaryOperator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.LongNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

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

    private static final BigInteger MAX_EXACT_INTEGER = BigInteger.TWO.pow(53);
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

            return objectMapper.treeToValue(transformNumbers(jsonNode, ProtoJsonMapper::toIntegralNumber), valueType);
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
                objectMapper.writeValueAsString(transformNumbers(jsonNode, ProtoJsonMapper::toExactNumber)), builder);
        } catch (InvalidProtocolBufferException invalidProtocolBufferException) {
            throw new IllegalStateException(
                "Unable to write %s to its proto message".formatted(
                    value.getClass()
                        .getSimpleName()),
                invalidProtocolBufferException);
        }

        return (M) builder.build();
    }

    private static JsonNode toExactNumber(JsonNode jsonNode) {
        if (jsonNode.isIntegralNumber() && jsonNode.bigIntegerValue()
            .abs()
            .compareTo(MAX_EXACT_INTEGER) > 0) {

            return StringNode.valueOf(jsonNode.asString());
        }

        return jsonNode;
    }

    private static JsonNode toIntegralNumber(JsonNode jsonNode) {
        if (jsonNode.isDouble()) {
            double number = jsonNode.doubleValue();

            if (number == Math.rint(number) && Math.abs(number) <= 0x1p53) {
                long longValue = (long) number;

                return longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE
                    ? IntNode.valueOf((int) longValue) : LongNode.valueOf(longValue);
            }
        }

        return jsonNode;
    }

    private static JsonNode transformNumbers(JsonNode jsonNode, UnaryOperator<JsonNode> numberTransformer) {
        if (jsonNode.isNumber()) {
            return numberTransformer.apply(jsonNode);
        }

        if (jsonNode instanceof ObjectNode objectNode) {
            for (Map.Entry<String, JsonNode> entry : objectNode.properties()) {
                entry.setValue(transformNumbers(entry.getValue(), numberTransformer));
            }
        } else if (jsonNode instanceof ArrayNode arrayNode) {
            for (int index = 0; index < arrayNode.size(); index++) {
                arrayNode.set(index, transformNumbers(arrayNode.get(index), numberTransformer));
            }
        }

        return jsonNode;
    }
}

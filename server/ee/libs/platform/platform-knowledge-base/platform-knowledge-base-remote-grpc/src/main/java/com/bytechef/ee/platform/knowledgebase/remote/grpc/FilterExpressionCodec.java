/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.knowledgebase.remote.grpc;

import com.google.protobuf.ByteString;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.vectorstore.filter.Filter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class FilterExpressionCodec {

    private static final String CONTENT = "content";
    private static final String KIND = "kind";
    private static final String TYPE = "type";
    private static final String VALUE = "value";

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public FilterExpressionCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ByteString write(Filter.Expression filterExpression) {
        return ByteString.copyFrom(objectMapper.writeValueAsBytes(writeOperand(filterExpression)));
    }

    public Filter.Expression read(ByteString bytes) {
        Filter.Operand operand = readOperand(objectMapper.readTree(bytes.toByteArray()));

        if (operand instanceof Filter.Expression filterExpression) {
            return filterExpression;
        }

        throw new IllegalArgumentException("Filter is not an expression: " + operand);
    }

    private ObjectNode writeOperand(Filter.Operand operand) {
        ObjectNode objectNode = objectMapper.createObjectNode();

        switch (operand) {
            case Filter.Expression filterExpression -> {
                objectNode.put(KIND, "expression");
                objectNode.put(TYPE, filterExpression.type()
                    .name());
                objectNode.set("left", writeOperand(filterExpression.left()));

                if (filterExpression.right() != null) {
                    objectNode.set("right", writeOperand(filterExpression.right()));
                }
            }
            case Filter.Group group -> {
                objectNode.put(KIND, "group");
                objectNode.set(CONTENT, writeOperand(group.content()));
            }
            case Filter.Key key -> {
                objectNode.put(KIND, "key");
                objectNode.put("key", key.key());
            }
            case Filter.Value value -> {
                objectNode.put(KIND, VALUE);
                objectNode.set(VALUE, writeValue(value.value()));
            }
            default -> throw new IllegalArgumentException("Unsupported filter operand: " + operand);
        }

        return objectNode;
    }

    private ObjectNode writeValue(@Nullable Object value) {
        ObjectNode objectNode = objectMapper.createObjectNode();

        switch (value) {
            case null -> objectNode.put(TYPE, "null");
            case String string -> objectNode.put(TYPE, "string")
                .put(VALUE, string);
            case Boolean bool -> objectNode.put(TYPE, "boolean")
                .put(VALUE, bool);
            case Integer integer -> objectNode.put(TYPE, "integer")
                .put(VALUE, integer);
            case Long longValue -> objectNode.put(TYPE, "long")
                .put(VALUE, longValue);
            case Double doubleValue -> objectNode.put(TYPE, "double")
                .put(VALUE, doubleValue);
            case Float floatValue -> objectNode.put(TYPE, "float")
                .put(VALUE, floatValue);
            case List<?> list -> {
                ArrayNode arrayNode = objectNode.put(TYPE, "list")
                    .putArray(VALUE);

                for (Object item : list) {
                    arrayNode.add(writeValue(item));
                }
            }
            default -> throw new IllegalArgumentException(
                "Unsupported filter value type: " + value.getClass()
                    .getName());
        }

        return objectNode;
    }

    private Filter.Operand readOperand(JsonNode jsonNode) {
        String kind = jsonNode.required(KIND)
            .asString();

        return switch (kind) {
            case "expression" -> {
                Filter.ExpressionType type = Filter.ExpressionType.valueOf(
                    jsonNode.required(TYPE)
                        .asString());
                Filter.Operand left = readOperand(jsonNode.required("left"));
                JsonNode rightJsonNode = jsonNode.get("right");

                yield rightJsonNode == null
                    ? new Filter.Expression(type, left) : new Filter.Expression(type, left, readOperand(rightJsonNode));
            }
            case "group" -> new Filter.Group((Filter.Expression) readOperand(jsonNode.required(CONTENT)));
            case "key" -> new Filter.Key(jsonNode.required("key")
                .asString());
            case VALUE -> new Filter.Value(readValue(jsonNode.required(VALUE)));
            default -> throw new IllegalArgumentException("Unsupported filter operand kind: " + kind);
        };
    }

    private @Nullable Object readValue(JsonNode jsonNode) {
        String type = jsonNode.required(TYPE)
            .asString();
        JsonNode valueJsonNode = jsonNode.get(VALUE);

        return switch (type) {
            case "null" -> null;
            case "string" -> valueJsonNode.asString();
            case "boolean" -> valueJsonNode.asBoolean();
            case "integer" -> valueJsonNode.asInt();
            case "long" -> valueJsonNode.asLong();
            case "double" -> valueJsonNode.asDouble();
            case "float" -> valueJsonNode.floatValue();
            case "list" -> {
                List<@Nullable Object> items = new ArrayList<>();

                for (JsonNode itemJsonNode : valueJsonNode) {
                    items.add(readValue(itemJsonNode));
                }

                yield items;
            }
            default -> throw new IllegalArgumentException("Unsupported filter value type: " + type);
        };
    }
}

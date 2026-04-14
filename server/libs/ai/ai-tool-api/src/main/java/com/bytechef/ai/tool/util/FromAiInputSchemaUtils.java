/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.tool.util;

import com.bytechef.ai.tool.FromAiResult;
import java.util.List;
import org.springframework.ai.util.json.JsonParser;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Generates JSON schema for AI tool parameters defined via {@code fromAi} expressions.
 *
 * @author Ivica Cardic
 */
public class FromAiInputSchemaUtils {

    private static final String SCHEMA_DRAFT_2020_12 = "https://json-schema.org/draft/2020-12/schema";

    public static String generateInputSchema(List<FromAiResult> fromAiResults) {
        JsonMapper jsonMapper = JsonParser.getJsonMapper();

        ObjectNode schemaObjectNode = jsonMapper.createObjectNode();

        schemaObjectNode.put("$schema", SCHEMA_DRAFT_2020_12);
        schemaObjectNode.put("type", "object");

        ObjectNode propertiesObjectNode = schemaObjectNode.putObject("properties");
        ArrayNode requiredArray = schemaObjectNode.putArray("required");

        for (FromAiResult fromAiResult : fromAiResults) {
            ObjectNode parameterObjectNode = jsonMapper.createObjectNode();

            parameterObjectNode.put("type", getJsonSchemaType(fromAiResult.type()));

            String format = getJsonSchemaFormat(fromAiResult.type());

            if (format != null) {
                parameterObjectNode.put("format", format);
            }

            String description = fromAiResult.description();

            if (description != null && !description.isEmpty()) {
                parameterObjectNode.put("description", description);
            }

            Object defaultValue = fromAiResult.defaultValue();

            if (defaultValue != null) {
                addValueToObjectNode(parameterObjectNode, "default", coerceToType(defaultValue, fromAiResult.type()));
            }

            List<Object> options = fromAiResult.options();

            if (options != null && !options.isEmpty()) {
                ArrayNode enumArrayNode = parameterObjectNode.putArray("enum");

                for (Object option : options) {
                    addValueToArrayNode(enumArrayNode, coerceToType(option, fromAiResult.type()));
                }
            }

            propertiesObjectNode.set(fromAiResult.name(), parameterObjectNode);

            if (fromAiResult.required()) {
                requiredArray.add(fromAiResult.name());
            }
        }

        return schemaObjectNode.toPrettyString();
    }

    private static Object coerceToType(Object value, String type) {
        if (!(value instanceof String stringValue) || type == null) {
            return value;
        }

        String trimmedType = type.trim();

        return switch (trimmedType.toUpperCase()) {
            case "BOOLEAN" -> Boolean.parseBoolean(stringValue);
            case "INTEGER" -> {
                try {
                    yield Long.parseLong(stringValue);
                } catch (NumberFormatException e) {
                    yield value;
                }
            }
            case "NUMBER" -> {
                try {
                    yield Double.parseDouble(stringValue);
                } catch (NumberFormatException e) {
                    yield value;
                }
            }
            default -> value;
        };
    }

    private static void addValueToObjectNode(ObjectNode objectNode, String fieldName, Object value) {
        if (value instanceof Boolean booleanValue) {
            objectNode.put(fieldName, booleanValue);
        } else if (value instanceof Integer integerValue) {
            objectNode.put(fieldName, integerValue);
        } else if (value instanceof Long longValue) {
            objectNode.put(fieldName, longValue);
        } else if (value instanceof Double doubleValue) {
            objectNode.put(fieldName, doubleValue);
        } else if (value instanceof Float floatValue) {
            objectNode.put(fieldName, floatValue);
        } else if (value instanceof String stringValue) {
            objectNode.put(fieldName, stringValue);
        } else {
            objectNode.putPOJO(fieldName, value);
        }
    }

    private static void addValueToArrayNode(ArrayNode arrayNode, Object value) {
        if (value instanceof Boolean booleanValue) {
            arrayNode.add(booleanValue);
        } else if (value instanceof Integer integerValue) {
            arrayNode.add(integerValue);
        } else if (value instanceof Long longValue) {
            arrayNode.add(longValue);
        } else if (value instanceof Double doubleValue) {
            arrayNode.add(doubleValue);
        } else if (value instanceof Float floatValue) {
            arrayNode.add(floatValue);
        } else if (value instanceof String stringValue) {
            arrayNode.add(stringValue);
        } else {
            arrayNode.addPOJO(value);
        }
    }

    private static String getJsonSchemaType(String type) {
        if (type == null || type.isBlank()) {
            return "string";
        }

        return switch (type.trim()
            .toUpperCase()) {
            case "BOOLEAN" -> "boolean";
            case "INTEGER" -> "integer";
            case "NUMBER" -> "number";
            case "ARRAY" -> "array";
            case "OBJECT" -> "object";
            default -> "string";
        };
    }

    private static String getJsonSchemaFormat(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }

        return switch (type.trim()
            .toUpperCase()) {
            case "DATE" -> "date";
            case "TIME" -> "time";
            case "DATE_TIME" -> "date-time";
            default -> null;
        };
    }
}

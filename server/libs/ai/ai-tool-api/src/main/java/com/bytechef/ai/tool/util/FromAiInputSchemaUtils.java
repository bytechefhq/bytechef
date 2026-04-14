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
                if (defaultValue instanceof Boolean booleanValue) {
                    parameterObjectNode.put("default", booleanValue);
                } else if (defaultValue instanceof Integer integerValue) {
                    parameterObjectNode.put("default", integerValue);
                } else if (defaultValue instanceof Long longValue) {
                    parameterObjectNode.put("default", longValue);
                } else if (defaultValue instanceof Double doubleValue) {
                    parameterObjectNode.put("default", doubleValue);
                } else if (defaultValue instanceof Float floatValue) {
                    parameterObjectNode.put("default", floatValue);
                } else if (defaultValue instanceof String stringValue) {
                    parameterObjectNode.put("default", stringValue);
                } else {
                    parameterObjectNode.putPOJO("default", defaultValue);
                }
            }

            List<Object> options = fromAiResult.options();

            if (options != null && !options.isEmpty()) {
                ArrayNode enumArrayNode = parameterObjectNode.putArray("enum");

                for (Object option : options) {
                    if (option instanceof Boolean booleanValue) {
                        enumArrayNode.add(booleanValue);
                    } else if (option instanceof Integer integerValue) {
                        enumArrayNode.add(integerValue);
                    } else if (option instanceof Long longValue) {
                        enumArrayNode.add(longValue);
                    } else if (option instanceof Double doubleValue) {
                        enumArrayNode.add(doubleValue);
                    } else if (option instanceof Float floatValue) {
                        enumArrayNode.add(floatValue);
                    } else if (option instanceof String stringValue) {
                        enumArrayNode.add(stringValue);
                    } else {
                        enumArrayNode.addPOJO(option);
                    }
                }
            }

            propertiesObjectNode.set(fromAiResult.name(), parameterObjectNode);

            if (defaultValue == null) {
                requiredArray.add(fromAiResult.name());
            }
        }

        return schemaObjectNode.toPrettyString();
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

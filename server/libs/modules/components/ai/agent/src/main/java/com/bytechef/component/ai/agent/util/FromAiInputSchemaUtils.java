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

package com.bytechef.component.ai.agent.util;

import com.bytechef.platform.workflow.worker.ai.FromAiResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.springframework.ai.util.json.JsonParser;

/**
 * Generates JSON schema for AI tool parameters defined via {@code fromAi} expressions.
 *
 * @author Ivica Cardic
 */
public class FromAiInputSchemaUtils {

    private static final String SCHEMA_DRAFT_2020_12 = "https://json-schema.org/draft/2020-12/schema";

    public static String generateInputSchema(List<FromAiResult> fromAiResults) {
        ObjectMapper objectMapper = JsonParser.getObjectMapper();

        ObjectNode schemaObjectNode = objectMapper.createObjectNode();

        schemaObjectNode.put("$schema", SCHEMA_DRAFT_2020_12);
        schemaObjectNode.put("type", "object");

        ObjectNode propertiesObjectNode = schemaObjectNode.putObject("properties");
        ArrayNode requiredArray = schemaObjectNode.putArray("required");

        for (FromAiResult fromAiResult : fromAiResults) {
            ObjectNode parameterObjectNode = objectMapper.createObjectNode();

            parameterObjectNode.put("type", getJsonSchemaType(fromAiResult.type()));

            if (fromAiResult.description() != null && !fromAiResult.description()
                .isEmpty()) {
                parameterObjectNode.put("description", fromAiResult.description());
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

}

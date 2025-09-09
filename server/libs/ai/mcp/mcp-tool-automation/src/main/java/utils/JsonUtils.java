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

package utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Centralized utility class for JSON operations and type utilities.
 * Consolidates common JSON operations that were duplicated across multiple classes.
 */
public class JsonUtils {

    private JsonUtils() {
        // Utility class
    }

    /**
     * Gets the type of a JsonNode as a lowercase string.
     * Centralized implementation to replace duplicated getJsonNodeType methods.
     */
    public static String getJsonNodeType(JsonNode node) {
        if (node == null) {
            return "null";
        } else if (node.isTextual()) {
            return "string";
        } else if (node.isNumber()) {
            if (node.isInt()) {
                return "integer";
            } else {
                return "number";
            }
        } else if (node.isBoolean()) {
            return "boolean";
        } else if (node.isObject()) {
            return "object";
        } else if (node.isArray()) {
            return "array";
        } else if (node.isNull()) {
            return "null";
        } else {
            return "unknown";
        }
    }

    /**
     * Maps PropertyInfo type strings to standardized lowercase format.
     */
    public static String mapTypeToString(String propertyType) {
        if (propertyType == null) {
            return "unknown";
        }

        return propertyType.toLowerCase();
    }

    /**
     * Parses JSON string with error handling.
     */
    public static JsonNode parseJsonWithErrorHandling(String json, StringBuilder errors) {
        try {
            return WorkflowParser.parseJsonString(json);
        } catch (JsonProcessingException e) {
            errors.append("Invalid JSON format: ")
                .append(e.getMessage())
                .append("\n");
            return null;
        }
    }

    /**
     * Validates that a JsonNode is an object.
     */
    public static boolean validateNodeIsObject(JsonNode node, String nodeType, StringBuilder errors) {
        if (node == null) {
            return false;
        }

        if (!node.isObject()) {
            errors.append(nodeType).append(" must be an object");
            return false;
        }

        return true;
    }

    /**
     * Handles JsonProcessingException with context-aware error messages.
     */
    public static void handleJsonProcessingException(JsonProcessingException e, String json, StringBuilder errors) {
        if (e.getMessage() != null && json.contains("\"type\":") && json.contains("triggers")) {
            errors.append("Trigger must be an object\n");
        } else {
            errors.append("Invalid JSON format: ")
                .append(e.getMessage())
                .append("\n");
        }
    }

    /**
     * Removes objects with invalid display conditions from JSON.
     */
    public static String removeObjectsWithInvalidConditions(String jsonString) {
        try {
            String result = jsonString;

            // Remove objects that have metadata (display conditions)
            result = result.replaceAll("\"[^\"]+\"\\s*:\\s*\\{[^{}]*\"metadata\"\\s*:\\s*\"[^\"]*\"[^{}]*\\}", "");

            // Clean up any resulting JSON syntax issues
            result = WorkflowParser.cleanupJsonSyntax(result);

            return result;
        } catch (Exception e) {
            return jsonString; // Return original if cleaning fails
        }
    }
}

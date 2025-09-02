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
 * Utility class for handling JSON processing operations and error handling. Provides centralized JSON parsing with
 * standardized error handling.
 */
public class JsonProcessingHelper {

    private static final String INVALID_JSON_FORMAT = "Invalid JSON format: ";

    private JsonProcessingHelper() {
        // Utility class
    }

    /**
     * Parses a JSON string and handles JsonProcessingException with standardized error reporting.
     */
    public static JsonNode parseJsonWithErrorHandling(String json, StringBuilder errors) {
        try {
            return WorkflowParser.parseJsonString(json);
        } catch (JsonProcessingException e) {
            errors.append(INVALID_JSON_FORMAT)
                .append(e.getMessage())
                .append("\n");
            return null;
        }
    }

    /**
     * Handles JsonProcessingException with context-aware error messages.
     */
    public static void handleJsonProcessingException(JsonProcessingException e, String json, StringBuilder errors) {
        if (e.getMessage() != null && json.contains("\"type\":") && json.contains("triggers")) {
            errors.append("Trigger must be an object\n");
        } else {
            errors.append(INVALID_JSON_FORMAT)
                .append(e.getMessage())
                .append("\n");
        }
    }

    /**
     * Validates that a JsonNode is an object and reports appropriate error if not.
     */
    public static boolean validateNodeIsObject(JsonNode node, String nodeType, StringBuilder errors) {
        if (node == null) {
            return false;
        }

        if (!node.isObject()) {
            errors.append(nodeType)
                .append(" must be an object");
            return false;
        }

        return true;
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

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

package com.bytechef.platform.workflow.validator;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.lang.Nullable;

/**
 * Centralized utility class for JSON operations and type utilities. Consolidates common JSON operations that were
 * duplicated across multiple classes.
 *
 * @author Marko Kriskovic
 */
class JsonUtils {

    private JsonUtils() {
    }

    /**
     * Gets the type of JsonNode as a lowercase string. Centralized implementation to replace duplicated getJsonNodeType
     * methods.
     */
    public static String getJsonNodeType(@Nullable JsonNode jsonNode) {
        if (jsonNode == null) {
            return "null";
        } else if (jsonNode.isTextual()) {
            return "string";
        } else if (jsonNode.isNumber()) {
            if (jsonNode.isInt()) {
                return "integer";
            } else {
                return "number";
            }
        } else if (jsonNode.isBoolean()) {
            return "boolean";
        } else if (jsonNode.isObject()) {
            return "object";
        } else if (jsonNode.isArray()) {
            return "array";
        } else if (jsonNode.isNull()) {
            return "null";
        } else {
            return "unknown";
        }
    }

    /**
     * Parses JSON string with error handling.
     */
    @Nullable
    public static JsonNode parseJsonWithErrorHandling(String json, StringBuilder errors) {
        try {
            return com.bytechef.commons.util.JsonUtils.readTree(json);
        } catch (Exception e) {
            errors.append("Invalid JSON format: ")
                .append(e.getMessage())
                .append("\n");

            return null;
        }
    }

    /**
     * Validates that a JsonNode is an object.
     */
    public static boolean appendErrorNodeIsObject(@Nullable JsonNode jsonNode, String nodeType, StringBuilder errors) {
        if (jsonNode == null) {
            return false;
        }

        if (!jsonNode.isObject()) {
            errors.append(nodeType);
            errors.append(" must be an object");

            return false;
        }

        return true;
    }
}

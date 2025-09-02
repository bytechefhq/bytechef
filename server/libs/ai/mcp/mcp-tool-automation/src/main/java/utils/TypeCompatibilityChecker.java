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

/**
 * Utility class for checking type compatibility between data pill expressions and their target fields.
 */
public class TypeCompatibilityChecker {

    private TypeCompatibilityChecker() {
        // Utility class
    }

    /**
     * Determines the expected type of a field based on its path.
     */
    public static String getExpectedTypeFromFieldPath(String fieldPath) {
        String fieldName = extractFieldNameFromPath(fieldPath);
        return getExpectedTypeFromFieldName(fieldName);
    }

    /**
     * Checks if two types are compatible for data pill assignments.
     */
    public static boolean isTypeCompatible(String expectedType, String actualType) {
        if (expectedType == null || actualType == null) {
            return true;
        }
        return expectedType.equalsIgnoreCase(actualType);
    }

    private static String extractFieldNameFromPath(String fieldPath) {
        if (fieldPath.contains(".")) {
            String[] parts = fieldPath.split("\\.");
            return parts[parts.length - 1];
        }
        return fieldPath;
    }

    private static String getExpectedTypeFromFieldName(String fieldName) {
        return switch (fieldName) {
            case "active" -> "boolean";
            case "name" -> "string";
            default -> null;
        };
    }
}

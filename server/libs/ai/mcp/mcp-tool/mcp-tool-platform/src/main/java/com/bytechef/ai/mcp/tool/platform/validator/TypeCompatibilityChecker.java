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

package com.bytechef.ai.mcp.tool.platform.validator;

/**
 * Utility class for checking type compatibility between data pill expressions and their target fields.
 */
public class TypeCompatibilityChecker {

    private TypeCompatibilityChecker() {
    }

    /**
     * Checks if two types are compatible for data pill assignments.
     */
    public static boolean isTypeCompatible(String expectedType, String actualType) {
        if (actualType == null) {
            return true;
        }

        // Exact match
        if (expectedType.equalsIgnoreCase(actualType)) {
            return true;
        }

        // Integer and number types are compatible
        if ((expectedType.equalsIgnoreCase("integer") && actualType.equalsIgnoreCase("number")) ||
            (expectedType.equalsIgnoreCase("number") && actualType.equalsIgnoreCase("integer"))) {
            return true;
        }

        // Any type can be converted to string
        if (expectedType.equalsIgnoreCase("string")) {
            return true;
        }

        return false;
    }
}

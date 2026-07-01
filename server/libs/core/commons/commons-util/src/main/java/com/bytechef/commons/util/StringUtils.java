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

package com.bytechef.commons.util;

import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public class StringUtils {

    /**
     * Appends the given string to the provided StringBuilder, ensuring that it is preceded by a newline character if
     * the StringBuilder is not empty.
     *
     * @param string The string to append to the StringBuilder. Cannot be null.
     * @param sb     The StringBuilder to which the string will be appended. Cannot be null.
     */
    public static void appendWithNewline(String string, StringBuilder sb) {
        if (!sb.isEmpty()) {
            sb.append("\n");
        }

        sb.append(string);
    }

    /**
     * Sanitizes the given string by applying a set of transformations defined by the overloaded method
     * {@link #sanitize(String, int)} with a default length limit of -1. This method is useful for ensuring the input
     * string is free of unsafe or undesirable characters and formatted appropriately.
     *
     * @param value The input string to be sanitized. If null, the method returns null. If blank, it returns an empty
     *              string.
     * @return A sanitized version of the input string with no length restriction.
     */
    public static String sanitize(String value) {
        return sanitize(value, -1);
    }

    /**
     * Sanitizes the given string by removing or replacing unsafe characters and trimming whitespace. This method is
     * useful for preparing file names, identifiers, or other strings that require a specific format or should avoid
     * problematic characters.
     * <p>
     * The method applies the following transformations: 1. Trims leading and trailing whitespace from the input string.
     * 2. Replaces Windows line break characters ("\r") with "\\r". 3. Replaces Unix line break characters ("\n") with
     * "\\n". 4. Replaces Windows forbidden characters (e.g., \ / : * ? " < > |) with an underscore. 5. Replaces
     * sequences of whitespace with a single underscore. 6. Collapses consecutive underscores into a single underscore.
     * 7. Truncates the string to the specified length limit, if provided.
     *
     * @param value       The input string to be sanitized. If null, the method returns null. If blank, it returns an
     *                    empty string.
     * @param lengthLimit An integer specifying the maximum allowed length for the sanitized string. Use -1 for no
     *                    length restriction.
     * @return A sanitized version of
     */
    public static String sanitize(String value, int lengthLimit) {
        if (value == null) {
            return null;
        }

        if (org.apache.commons.lang3.StringUtils.isBlank(value)) {
            return "";
        }

        String sanitized = value.trim()
            .replaceAll("[\\\\/:*?\"<>|]", "_") // Windows forbidden characters
            .replaceAll("\\s+", "_") // Replace whitespace with underscores
            .replaceAll("_{2,}", "_"); // Replace multiple underscores with single;

        if (lengthLimit != -1 && sanitized.length() > lengthLimit) {
            sanitized = sanitized.substring(0, lengthLimit);
        }

        return sanitized;
    }

    public static @Nullable String asString(@Nullable Object value) {
        return value == null ? null : value.toString();
    }
}

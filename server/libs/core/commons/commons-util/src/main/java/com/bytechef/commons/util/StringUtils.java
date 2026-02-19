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

/**
 * @author Ivica Cardic
 */
public class StringUtils {

    public static String sanitize(String name, int lengthLimit) {
        if (org.apache.commons.lang3.StringUtils.isBlank(name)) {
            return "";
        }

        String sanitized = name.trim()
            .replaceAll("[\\\\/:*?\"<>|]", "_") // Windows forbidden characters
            .replaceAll("\\s+", "_") // Replace whitespace with underscores
            .replaceAll("_{2,}", "_"); // Replace multiple underscores with single

        if (lengthLimit != -1 && sanitized.length() > lengthLimit) {
            sanitized = sanitized.substring(0, lengthLimit);
        }

        // Remove trailing underscores or dots
        sanitized = sanitized.replaceAll("[_.]+$", "");

        if (sanitized.isEmpty()) {
            return "";
        }

        return sanitized;
    }

    /**
     * Appends a string with proper newline handling.
     */
    public static void appendWithNewline(String string, StringBuilder sb) {
        if (!sb.isEmpty()) {
            sb.append("\n");
        }

        sb.append(string);
    }
}

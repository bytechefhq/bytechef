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

package com.bytechef.automation.configuration.ownership;

import java.util.IllegalFormatException;
import java.util.regex.Pattern;

/**
 * @author Ivica Cardic
 */
final class OwnerConditionTemplate {

    private static final Pattern NAMED_PLACEHOLDER = Pattern.compile("(?<!:):[A-Za-z_]");
    private static final String PROBE_COLUMN = "owner_probe.id";

    private final String conditionTemplate;

    OwnerConditionTemplate(String conditionTemplate) {
        boolean hasNamedPlaceholder = NAMED_PLACEHOLDER.matcher(conditionTemplate)
            .find();

        if (conditionTemplate.indexOf('?') >= 0 || hasNamedPlaceholder) {
            throw new IllegalArgumentException(
                "The owner condition must not contain placeholders, the id column is formatted in: " +
                    conditionTemplate);
        }

        String probe;

        try {
            probe = conditionTemplate.formatted(PROBE_COLUMN);
        } catch (IllegalFormatException illegalFormatException) {
            throw new IllegalArgumentException(
                "The owner condition is not a valid template, use %1$s for the id column: " + conditionTemplate,
                illegalFormatException);
        }

        if (!probe.contains(PROBE_COLUMN)) {
            throw new IllegalArgumentException(
                "The owner condition must reference the id column as %1$s: " + conditionTemplate);
        }

        this.conditionTemplate = conditionTemplate;
    }

    String format(String idColumn) {
        if (!isColumnName(idColumn)) {
            throw new IllegalArgumentException("The id column is not a column name: " + idColumn);
        }

        return conditionTemplate.formatted(idColumn);
    }

    private static boolean isColumnName(String idColumn) {
        String[] parts = idColumn.split("\\.", -1);

        if (parts.length > 2) {
            return false;
        }

        for (String part : parts) {
            if (!isIdentifier(part)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isIdentifier(String value) {
        if (value.isEmpty() || !isIdentifierStart(value.charAt(0))) {
            return false;
        }

        for (int index = 1; index < value.length(); index++) {
            char character = value.charAt(index);

            if (!isIdentifierStart(character) && (character < '0' || character > '9')) {
                return false;
            }
        }

        return true;
    }

    private static boolean isIdentifierStart(char character) {
        return character == '_' || (character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z');
    }
}

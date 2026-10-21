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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * @author Ivica Cardic
 */
final class OwnerConditionTemplate {

    private static final String ID_COLUMN_SPECIFIER = "%1$s";
    private static final Pattern NAMED_PLACEHOLDER = Pattern.compile("(?<!:):[A-Za-z_]");
    private static final String PROBE_COLUMN = "owner_probe.id";
    private static final Set<String> TABLE_DECLARATION_KEYWORDS = Set.of("FROM", "JOIN", ",");
    private static final Set<String> RESERVED_KEYWORDS = Set.of(
        "AND", "AS", "CROSS", "EXCEPT", "FETCH", "FULL", "GROUP", "HAVING", "INNER", "INTERSECT", "JOIN", "LATERAL",
        "LEFT", "LIMIT", "NATURAL", "NOT", "OFFSET", "ON", "OR", "ORDER", "OUTER", "RIGHT", "TABLESAMPLE", "UNION",
        "USING", "WHERE", "WINDOW");
    private static final String UNSUPPORTED_QUOTING_CHARACTERS = "\"`$\\";

    private final String conditionTemplate;
    private final Map<String, String> formattedConditions = new ConcurrentHashMap<>();

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
    @SuppressWarnings("PMD.UnusedReturnValue")
    OwnerConditionTemplate(String conditionTemplate) {
        boolean hasNamedPlaceholder = NAMED_PLACEHOLDER.matcher(conditionTemplate)
            .find();

        if (conditionTemplate.indexOf('?') >= 0 || hasNamedPlaceholder) {
            throw new IllegalArgumentException(
                "The owner condition must not contain placeholders, the id column is formatted in: " +
                    conditionTemplate);
        }

        String conditionTemplateWithoutFormatSpecifiers = conditionTemplate.replace(ID_COLUMN_SPECIFIER, "")
            .replace("%%", "");

        if (conditionTemplateWithoutFormatSpecifiers.indexOf('%') >= 0) {
            throw new IllegalArgumentException(
                "The owner condition may only contain the %1$s and %% format specifiers: " + conditionTemplate);
        }

        if (conditionTemplateWithoutFormatSpecifiers.chars()
            .anyMatch(character -> UNSUPPORTED_QUOTING_CHARACTERS.indexOf(character) >= 0)) {

            throw new IllegalArgumentException(
                "The owner condition must not contain quoted identifiers, dollar quoted strings or backslash " +
                    "escapes: " + conditionTemplate);
        }

        if (!isSingleBalancedExpression(conditionTemplate)) {
            throw new IllegalArgumentException(
                "The owner condition must be one expression with balanced parentheses and string literals, and " +
                    "without comments or statement separators: " + conditionTemplate);
        }

        try {
            conditionTemplate.formatted(PROBE_COLUMN);
        } catch (IllegalFormatException illegalFormatException) {
            throw new IllegalArgumentException(
                "The owner condition is not a valid template, use %1$s for the id column: " + conditionTemplate,
                illegalFormatException);
        }

        if (!referencesIdColumnOutsideStringLiterals(conditionTemplate)) {
            throw new IllegalArgumentException(
                "The owner condition must reference the id column as %1$s outside string literals: " +
                    conditionTemplate);
        }

        this.conditionTemplate = conditionTemplate;
    }

    String format(String idColumn) {
        return formattedConditions.computeIfAbsent(idColumn, this::formatIdColumn);
    }

    private String formatIdColumn(String idColumn) {
        if (!isQualifiedColumnName(idColumn)) {
            throw new IllegalArgumentException(
                "The id column is not a table qualified column name, an unqualified column resolves to the " +
                    "subquery instead of the outer query: " + idColumn);
        }

        String tableName = idColumn.substring(0, idColumn.indexOf('.'));

        if (shadowsTable(tableName)) {
            throw new IllegalArgumentException(
                "The owner condition must alias the " + tableName + " table and must not use " + tableName +
                    " as an alias, otherwise " + idColumn + " resolves to the subquery instead of the outer query: " +
                    conditionTemplate);
        }

        return conditionTemplate.formatted(idColumn);
    }

    private boolean shadowsTable(String tableName) {
        List<String> tokens = tokenize(conditionTemplate.replace(ID_COLUMN_SPECIFIER, " "));

        for (int index = 0; index < tokens.size(); index++) {
            String[] nameParts = tokens.get(index)
                .split("\\.");

            if (!nameParts[nameParts.length - 1].equalsIgnoreCase(tableName)) {
                continue;
            }

            String previousToken = index > 0 ? tokens.get(index - 1) : "";

            if (TABLE_DECLARATION_KEYWORDS.contains(previousToken.toUpperCase(Locale.ROOT))) {
                if (!isFollowedByAlias(tokens, index)) {
                    return true;
                }
            } else if (nameParts.length == 1) {
                return true;
            }
        }

        return false;
    }

    private static boolean isFollowedByAlias(List<String> tokens, int index) {
        int aliasIndex = index + 1;

        if (aliasIndex < tokens.size() && tokens.get(aliasIndex)
            .equalsIgnoreCase("AS")) {

            aliasIndex++;
        }

        if (aliasIndex >= tokens.size()) {
            return false;
        }

        String alias = tokens.get(aliasIndex);

        return isIdentifier(alias) && !RESERVED_KEYWORDS.contains(alias.toUpperCase(Locale.ROOT));
    }

    private static List<String> tokenize(String sql) {
        List<String> tokens = new ArrayList<>();
        int index = 0;

        while (index < sql.length()) {
            char character = sql.charAt(index);

            if (character == '\'') {
                index = sql.indexOf('\'', index + 1) + 1;

                while (index < sql.length() && sql.charAt(index) == '\'') {
                    index = sql.indexOf('\'', index + 1) + 1;
                }

                tokens.add("''");
            } else if (isIdentifierStart(character)) {
                int start = index;

                while (index < sql.length()
                    && (isIdentifierPart(sql.charAt(index)) || isNamePartSeparator(sql, index))) {
                    index++;
                }

                tokens.add(sql.substring(start, index));
            } else {
                if (!Character.isWhitespace(character)) {
                    tokens.add(String.valueOf(character));
                }

                index++;
            }
        }

        return tokens;
    }

    private static boolean isNamePartSeparator(String sql, int index) {
        return sql.charAt(index) == '.' && index + 1 < sql.length() && isIdentifierStart(sql.charAt(index + 1));
    }

    private static boolean referencesIdColumnOutsideStringLiterals(String conditionTemplate) {
        boolean inStringLiteral = false;

        for (int index = 0; index < conditionTemplate.length(); index++) {
            char character = conditionTemplate.charAt(index);

            if (character == '\'') {
                inStringLiteral = !inStringLiteral;
            } else if (!inStringLiteral && conditionTemplate.startsWith(ID_COLUMN_SPECIFIER, index)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isSingleBalancedExpression(String conditionTemplate) {
        int depth = 0;
        boolean inStringLiteral = false;

        for (int index = 0; index < conditionTemplate.length(); index++) {
            char character = conditionTemplate.charAt(index);

            if (inStringLiteral) {
                if (character == '\'') {
                    inStringLiteral = false;
                }

                continue;
            }

            char nextCharacter = index + 1 < conditionTemplate.length() ? conditionTemplate.charAt(index + 1) : 0;

            if (character == ';' || (character == '-' && nextCharacter == '-') ||
                (character == '/' && nextCharacter == '*')) {

                return false;
            }

            if (character == '\'') {
                inStringLiteral = true;
            } else if (character == '(') {
                depth++;
            } else if (character == ')' && --depth < 0) {
                return false;
            }
        }

        return depth == 0 && !inStringLiteral;
    }

    private static boolean isQualifiedColumnName(String idColumn) {
        String[] parts = idColumn.split("\\.", -1);

        if (parts.length != 2) {
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

            if (!isIdentifierPart(character)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isIdentifierPart(char character) {
        return isIdentifierStart(character) || (character >= '0' && character <= '9');
    }

    private static boolean isIdentifierStart(char character) {
        return character == '_' || (character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z');
    }
}

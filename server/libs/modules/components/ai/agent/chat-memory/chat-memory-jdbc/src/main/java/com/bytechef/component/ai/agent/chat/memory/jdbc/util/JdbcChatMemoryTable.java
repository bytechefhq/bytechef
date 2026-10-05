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

package com.bytechef.component.ai.agent.chat.memory.jdbc.util;

import static com.bytechef.component.ai.agent.chat.memory.jdbc.constant.JdbcChatMemoryConstants.DEFAULT_TABLE_NAME;
import static com.bytechef.component.ai.agent.chat.memory.jdbc.constant.JdbcChatMemoryConstants.SCHEMA;
import static com.bytechef.component.ai.agent.chat.memory.jdbc.constant.JdbcChatMemoryConstants.TABLE;

import com.bytechef.component.definition.Parameters;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * The (optionally schema-qualified) table holding chat memory messages. Spring AI hardcodes
 * {@code SPRING_AI_CHAT_MEMORY} in its dialect SQL and schema scripts; this class rewrites those statements to target
 * the configured table instead. Identifiers are restricted to plain names and used unquoted, so they are
 * case-insensitive.
 *
 * @author Marko Krišković
 */
public record JdbcChatMemoryTable(@Nullable String schema, String table) {

    private static final Pattern DEFAULT_TABLE_PATTERN = Pattern.compile("\\b" + DEFAULT_TABLE_NAME + "\\b");
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    public JdbcChatMemoryTable {
        if (schema != null) {
            validateIdentifier(schema);
        }

        validateIdentifier(table);
    }

    public static JdbcChatMemoryTable of(Parameters inputParameters) {
        String schema = inputParameters.getString(SCHEMA);
        String table = inputParameters.getString(TABLE);

        return new JdbcChatMemoryTable(
            schema == null || schema.isBlank() ? null : schema.trim(),
            table == null || table.isBlank() ? DEFAULT_TABLE_NAME : table.trim());
    }

    public boolean isDefault() {
        return schema == null && table.equalsIgnoreCase(DEFAULT_TABLE_NAME);
    }

    /**
     * Replaces references to the default table in a SQL statement with this table.
     */
    public String rewriteSql(String sql) {
        if (isDefault()) {
            return sql;
        }

        Matcher matcher = DEFAULT_TABLE_PATTERN.matcher(sql);

        return matcher.replaceAll(Matcher.quoteReplacement(getQualifiedName()));
    }

    /**
     * Rewrites a Spring AI schema script: the table reference becomes this table, while index and constraint names are
     * prefixed with this table's name so that several chat memory tables can coexist in one database.
     */
    public String rewriteSchemaScript(String script) {
        if (isDefault()) {
            return script;
        }

        String rewrittenScript = script.replace(DEFAULT_TABLE_NAME + "_", table + "_")
            .replace("CONSTRAINT TYPE_CHECK", "CONSTRAINT " + table + "_TYPE_CHECK");

        return rewriteSql(rewrittenScript);
    }

    private String getQualifiedName() {
        return schema == null ? table : schema + "." + table;
    }

    private static void validateIdentifier(String identifier) {
        Matcher matcher = IDENTIFIER_PATTERN.matcher(identifier);

        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                "Invalid identifier '" + identifier + "': only letters, digits and underscores are allowed");
        }
    }
}

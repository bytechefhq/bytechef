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

package com.bytechef.platform.data.table.configuration.migration;

import com.bytechef.platform.data.table.domain.ReservedColumns;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class DataTableExternalIdColumnMigrator {

    private static final Logger log = LoggerFactory.getLogger(DataTableExternalIdColumnMigrator.class);

    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI")
    public DataTableExternalIdColumnMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int migrate() {
        return migrate(null);
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public int migrate(@Nullable String schemaName) {
        String schema = resolveSchema(schemaName);

        List<String> tableNames = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = ? AND table_type = 'BASE TABLE' "
                + "AND table_name LIKE 'dt\\_%'",
            String.class, schema);

        int altered = 0;

        for (String tableName : tableNames) {
            boolean hasExternalIdColumn = hasExternalIdColumn(schema, tableName);

            if (hasExternalIdColumn && hasExternalIdIndex(schema, tableName)) {
                continue;
            }

            String qualifiedName = quote(schema) + "." + quote(tableName);

            if (!hasExternalIdColumn) {
                jdbcTemplate.execute(
                    "ALTER TABLE " + qualifiedName + " ADD COLUMN IF NOT EXISTS " + quote(ReservedColumns.EXTERNAL_ID)
                        + " VARCHAR(255)");
            }

            jdbcTemplate.execute(
                "CREATE UNIQUE INDEX ON " + qualifiedName + " (" + quote(ReservedColumns.EXTERNAL_ID) + ")");

            altered++;
        }

        if (altered > 0) {
            log.info("Added external_id to {} data tables in schema {}", altered, schema);
        }

        return altered;
    }

    private boolean hasExternalIdIndex(String schema, String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pg_index pgIndex "
                + "JOIN pg_class tableClass ON tableClass.oid = pgIndex.indrelid "
                + "JOIN pg_namespace tableNamespace ON tableNamespace.oid = tableClass.relnamespace "
                + "WHERE tableNamespace.nspname = ? AND tableClass.relname = ? "
                + "AND pgIndex.indisunique AND pgIndex.indnkeyatts = 1 "
                + "AND (SELECT attribute.attname FROM pg_attribute attribute "
                + "WHERE attribute.attrelid = tableClass.oid AND attribute.attnum = pgIndex.indkey[0]) = ?",
            Integer.class, schema, tableName, ReservedColumns.EXTERNAL_ID);

        return count != null && count > 0;
    }

    private boolean hasExternalIdColumn(String schema, String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = ? AND table_name = ? AND column_name = ?",
            Integer.class, schema, tableName, ReservedColumns.EXTERNAL_ID);

        return count != null && count == 1;
    }

    private String resolveSchema(@Nullable String schemaName) {
        if (schemaName != null && !schemaName.isBlank()) {
            return schemaName;
        }

        return jdbcTemplate.queryForObject("SELECT current_schema()", String.class);
    }

    private String quote(String identifier) {
        Assert.hasText(identifier, "identifier must not be empty");

        String normalizedName = identifier.toLowerCase(Locale.ROOT);

        Assert.isTrue(normalizedName.matches("[a-z_][a-z0-9_]*"), "Invalid identifier: " + identifier);

        return '"' + normalizedName + '"';
    }
}

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
 * Adds the reserved owner columns to {@code dt_*} tables created before those columns existed. The table set is
 * discovered from {@code information_schema} rather than declared, which is why this is Java rather than plain
 * changeset XML -- {@link DataTableOwnerColumnChange} runs it as a Liquibase {@code customChange}, so it is still
 * recorded in {@code databasechangelog} and still runs once per tenant schema.
 *
 * <p>
 * Idempotent, and scoped to one schema per call. Deliberately not a Spring bean: Liquibase instantiates the change
 * reflectively, long before an application context exists.
 *
 * @author Ivica Cardic
 */
public class DataTableOwnerColumnMigrator {

    private static final Logger log = LoggerFactory.getLogger(DataTableOwnerColumnMigrator.class);

    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI")
    public DataTableOwnerColumnMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Migrates the connection's current schema.
     *
     * @return the number of tables altered; 0 when every table already carries the columns
     */
    public int migrate() {
        return migrate(null);
    }

    /**
     * Migrates one named schema, or the connection's current schema when {@code schemaName} is null.
     *
     * <p>
     * The schema is named rather than inherited because Liquibase's per-tenant run sets its own default schema without
     * necessarily moving the JDBC connection's {@code search_path}. Trusting {@code current_schema()} there would
     * quietly migrate the wrong schema and report success.
     *
     * @return the number of tables altered; 0 when every table already carries the columns
     */
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public int migrate(@Nullable String schemaName) {
        String schema = resolveSchema(schemaName);

        List<String> tableNames = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = ? AND table_type = 'BASE TABLE' AND table_name LIKE 'dt\\_%'",
            String.class, schema);

        int altered = 0;

        for (String tableName : tableNames) {
            if (hasOwnerColumns(schema, tableName)) {
                continue;
            }

            String qualifiedName = quote(schema) + "." + quote(tableName);

            jdbcTemplate.execute(
                "ALTER TABLE " + qualifiedName + " ADD COLUMN IF NOT EXISTS " + quote(ReservedColumns.OWNER_ID)
                    + " BIGINT, ADD COLUMN IF NOT EXISTS " + quote(ReservedColumns.OWNER_TYPE) + " INT");

            jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS " + quote("idx_" + tableName + "_owner") + " ON " + qualifiedName + " ("
                    + quote(ReservedColumns.OWNER_TYPE) + ", " + quote(ReservedColumns.OWNER_ID) + ")");

            altered++;
        }

        if (altered > 0) {
            log.info("Added owner columns to {} data tables in schema {}", altered, schema);
        }

        return altered;
    }

    private boolean hasOwnerColumns(String schema, String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = ? AND table_name = ? AND column_name IN (?, ?)",
            Integer.class, schema, tableName, ReservedColumns.OWNER_ID, ReservedColumns.OWNER_TYPE);

        return count != null && count == 2;
    }

    private String resolveSchema(@Nullable String schemaName) {
        if (schemaName != null && !schemaName.isBlank()) {
            return schemaName;
        }

        return jdbcTemplate.queryForObject("SELECT current_schema()", String.class);
    }

    /**
     * Schema and table names reach this from {@code information_schema} or from Liquibase and never from a user, but
     * they are still held to the identifier pattern the rest of the data table code enforces.
     */
    private String quote(String identifier) {
        Assert.hasText(identifier, "identifier must not be empty");

        String normalizedName = identifier.toLowerCase(Locale.ROOT);

        Assert.isTrue(normalizedName.matches("[a-z_][a-z0-9_]*"), "Invalid identifier: " + identifier);

        return '"' + normalizedName + '"';
    }
}

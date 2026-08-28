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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds the reserved owner columns to {@code dt_*} tables created before those columns existed. The table set is
 * discovered from {@code information_schema} rather than declared, which is why this is a Java migration and not a
 * Liquibase changeset.
 *
 * <p>
 * Idempotent, and scoped to the connection's current schema, so it migrates one tenant per call.
 *
 * @author Ivica Cardic
 */
@Component
public class DataTableOwnerColumnMigrator {

    private static final Logger log = LoggerFactory.getLogger(DataTableOwnerColumnMigrator.class);

    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI")
    public DataTableOwnerColumnMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * @return the number of tables altered; 0 when every table already carries the columns
     */
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public int migrate() {
        List<String> tableNames = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = current_schema() AND table_type = 'BASE TABLE' AND table_name LIKE 'dt\\_%'",
            String.class);

        int altered = 0;

        for (String tableName : tableNames) {
            if (hasOwnerColumns(tableName)) {
                continue;
            }

            jdbcTemplate.execute(
                "ALTER TABLE \"" + tableName + "\" ADD COLUMN IF NOT EXISTS \"" + ReservedColumns.OWNER_ID
                    + "\" BIGINT, ADD COLUMN IF NOT EXISTS \"" + ReservedColumns.OWNER_TYPE + "\" INT");

            jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS \"idx_" + tableName + "_owner\" ON \"" + tableName + "\" (\""
                    + ReservedColumns.OWNER_TYPE + "\", \"" + ReservedColumns.OWNER_ID + "\")");

            altered++;
        }

        if (altered > 0) {
            log.info("Added owner columns to {} data tables", altered);
        }

        return altered;
    }

    private boolean hasOwnerColumns(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = current_schema() AND table_name = ? AND column_name IN (?, ?)",
            Integer.class, tableName, ReservedColumns.OWNER_ID, ReservedColumns.OWNER_TYPE);

        return count != null && count == 2;
    }
}

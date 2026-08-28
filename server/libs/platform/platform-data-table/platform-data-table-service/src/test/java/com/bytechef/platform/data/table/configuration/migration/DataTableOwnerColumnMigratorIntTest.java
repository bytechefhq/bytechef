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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class DataTableOwnerColumnMigratorIntTest {

    @Autowired
    private DataTableOwnerColumnMigrator dataTableOwnerColumnMigrator;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testMigrateAddsOwnerColumnsToPreexistingTable() {
        jdbcTemplate.execute("CREATE TABLE \"dt_0_legacy\" (\"id\" BIGSERIAL PRIMARY KEY, \"title\" TEXT)");

        int altered = dataTableOwnerColumnMigrator.migrate();

        assertTrue(altered >= 1);
        assertTrue(hasColumn("dt_0_legacy", "owner_id"));
        assertTrue(hasColumn("dt_0_legacy", "owner_type"));
    }

    @Test
    void testMigrateIsIdempotent() {
        jdbcTemplate.execute("CREATE TABLE \"dt_0_legacy_two\" (\"id\" BIGSERIAL PRIMARY KEY)");

        dataTableOwnerColumnMigrator.migrate();

        assertEquals(0, dataTableOwnerColumnMigrator.migrate());
    }

    @Test
    void testMigrateIgnoresNonDataTables() {
        jdbcTemplate.execute("CREATE TABLE \"not_a_data_table\" (\"id\" BIGSERIAL PRIMARY KEY)");

        dataTableOwnerColumnMigrator.migrate();

        assertFalse(hasColumn("not_a_data_table", "owner_id"));
    }

    private boolean hasColumn(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?",
            Integer.class, tableName, columnName);

        return count != null && count > 0;
    }
}

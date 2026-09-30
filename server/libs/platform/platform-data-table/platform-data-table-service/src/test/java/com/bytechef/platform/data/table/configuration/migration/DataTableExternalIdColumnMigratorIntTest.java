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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class DataTableExternalIdColumnMigratorIntTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private DataTableExternalIdColumnMigrator dataTableExternalIdColumnMigrator;

    @BeforeEach
    void beforeEach() {
        dataTableExternalIdColumnMigrator = new DataTableExternalIdColumnMigrator(jdbcTemplate);
    }

    /**
     * Proves both halves of the index: a duplicate key is rejected, and rows with no key at all -- every row on the day
     * this migration runs -- coexist without colliding.
     */
    @Test
    void testMigrateAddsTheColumnAndTheIndexToAPreexistingTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_0_legacykey\"");
        jdbcTemplate.execute(
            "CREATE TABLE \"dt_0_legacykey\" (\"id\" BIGSERIAL PRIMARY KEY, "
                + "\"title\" TEXT)");
        jdbcTemplate.update("INSERT INTO \"dt_0_legacykey\" (\"title\") VALUES ('a'), ('b')");

        assertTrue(dataTableExternalIdColumnMigrator.migrate() >= 1);

        assertTrue(hasColumn("dt_0_legacykey", "external_id"));

        jdbcTemplate.update("INSERT INTO \"dt_0_legacykey\" (\"external_id\") VALUES ('k')");

        assertThrows(
            DuplicateKeyException.class,
            () -> jdbcTemplate.update("INSERT INTO \"dt_0_legacykey\" (\"external_id\") VALUES ('k')"));
    }

    /**
     * Before this branch {@code addColumn} validated no column names at all, so a customer column literally named
     * {@code external_id} was legal -- and plausible, since it means the same thing. A guard that skipped on the column
     * alone would leave such a table with no index at all, and every {@code upsertRow} against it would fail with
     * "there is no unique or exclusion constraint matching the ON CONFLICT specification".
     */
    @Test
    void testMigrateAddsTheMissingIndexToATableThatAlreadyHasTheColumn() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_0_legacyusercolumn\"");
        jdbcTemplate.execute(
            "CREATE TABLE \"dt_0_legacyusercolumn\" (\"id\" BIGSERIAL PRIMARY KEY, "
                + "\"external_id\" VARCHAR(255))");

        assertFalse(hasExternalIdIndex("dt_0_legacyusercolumn"));

        assertTrue(dataTableExternalIdColumnMigrator.migrate() >= 1);

        assertTrue(hasExternalIdIndex("dt_0_legacyusercolumn"));
    }

    /**
     * That the index is present in {@code pg_index} is not the same claim as that it is enforced, so the duplicate is
     * attempted rather than inferred.
     */
    @Test
    void testTheBackfilledIndexRejectsADuplicateExternalId() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_0_legacyusercolumn_two\"");
        jdbcTemplate.execute(
            "CREATE TABLE \"dt_0_legacyusercolumn_two\" (\"id\" BIGSERIAL PRIMARY KEY, "
                + "\"external_id\" VARCHAR(255))");

        dataTableExternalIdColumnMigrator.migrate();

        jdbcTemplate.update("INSERT INTO \"dt_0_legacyusercolumn_two\" (\"external_id\") VALUES ('k')");

        assertThrows(
            DuplicateKeyException.class,
            () -> jdbcTemplate.update("INSERT INTO \"dt_0_legacyusercolumn_two\" (\"external_id\") VALUES ('k')"));
    }

    @Test
    void testMigrateIsIdempotent() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_0_legacykey_two\"");
        jdbcTemplate.execute(
            "CREATE TABLE \"dt_0_legacykey_two\" (\"id\" BIGSERIAL PRIMARY KEY)");

        dataTableExternalIdColumnMigrator.migrate();

        assertEquals(0, dataTableExternalIdColumnMigrator.migrate());
    }

    @Test
    void testMigrateIgnoresNonDataTables() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"not_a_keyed_table\"");
        jdbcTemplate.execute("CREATE TABLE \"not_a_keyed_table\" (\"id\" BIGSERIAL PRIMARY KEY)");

        dataTableExternalIdColumnMigrator.migrate();

        assertFalse(hasColumn("not_a_keyed_table", "external_id"));
    }

    /**
     * Proves the customChange is wired, not merely harmless: Liquibase records a changeset only once it has run it, so
     * a mistyped class name or a missing interface method shows up here rather than as a silent no-op.
     */
    @Test
    void testLiquibaseRanTheBackfillChangeset() {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM databasechangelog WHERE id = ?", Integer.class, "20260902000001-1");

        assertEquals(1, count);
    }

    private boolean hasExternalIdIndex(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = current_schema() AND tablename = ? "
                + "AND indexdef LIKE '%UNIQUE%' AND indexdef LIKE '%(external_id)%'",
            Integer.class, tableName);

        return count != null && count > 0;
    }

    private boolean hasColumn(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?",
            Integer.class, tableName, columnName);

        return count != null && count > 0;
    }
}

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

package com.bytechef.platform.data.table.execution.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.domain.RowOwnerFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.owner.Owner;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The table is built with raw DDL rather than through {@code DataTableService.createTable}, which additionally requires
 * a {@code data_table} registry row. What is under test here is the row service's owner predicate.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class DataTableRowOwnerScopingIntTest {

    private static final long ENVIRONMENT_ID = 0;
    private static final String BASE_NAME = "conversations";

    private static final RowOwnerFilter ACCOUNT_A = RowOwnerFilter.ownedBy(Owner.connectedUser(1L));
    private static final RowOwnerFilter ACCOUNT_B = RowOwnerFilter.ownedBy(Owner.connectedUser(2L));

    @Autowired
    private DataTableRowService dataTableRowService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_0_conversations\"");
        jdbcTemplate.execute(
            "CREATE TABLE \"dt_0_conversations\" (\"id\" BIGSERIAL PRIMARY KEY, \"owner_id\" BIGINT, " +
                "\"owner_type\" INT, \"title\" TEXT)");
    }

    @Test
    void testAnAccountSeesOnlyItsOwnRows() {
        dataTableRowService.insertRow(BASE_NAME, Map.of("title", "a"), ENVIRONMENT_ID, ACCOUNT_A);
        dataTableRowService.insertRow(BASE_NAME, Map.of("title", "b"), ENVIRONMENT_ID, ACCOUNT_B);

        List<DataTableRow> dataTableRows = dataTableRowService.listRows(BASE_NAME, 100, 0, ENVIRONMENT_ID, ACCOUNT_A);

        assertEquals(1, dataTableRows.size());

        DataTableRow dataTableRow = dataTableRows.getFirst();

        Map<String, Object> values = dataTableRow.values();

        assertEquals("a", values.get("title"));
    }

    @Test
    void testAnAccountAlsoSeesUnownedVendorRows() {
        dataTableRowService.insertRow(
            BASE_NAME, Map.of("title", "shared"), ENVIRONMENT_ID, RowOwnerFilter.unrestricted());

        List<DataTableRow> dataTableRows = dataTableRowService.listRows(BASE_NAME, 100, 0, ENVIRONMENT_ID, ACCOUNT_A);

        assertEquals(1, dataTableRows.size());
    }

    @Test
    void testUnrestrictedSeesEveryRow() {
        dataTableRowService.insertRow(BASE_NAME, Map.of("title", "a"), ENVIRONMENT_ID, ACCOUNT_A);
        dataTableRowService.insertRow(BASE_NAME, Map.of("title", "b"), ENVIRONMENT_ID, ACCOUNT_B);

        List<DataTableRow> dataTableRows = dataTableRowService.listRows(
            BASE_NAME, 100, 0, ENVIRONMENT_ID, RowOwnerFilter.unrestricted());

        assertEquals(2, dataTableRows.size());
    }

    @Test
    void testInsertStampsTheOwner() {
        DataTableRow inserted = dataTableRowService.insertRow(
            BASE_NAME, Map.of("title", "a"), ENVIRONMENT_ID, ACCOUNT_A);

        Long ownerId = jdbcTemplate.queryForObject(
            "SELECT \"owner_id\" FROM \"dt_0_conversations\" WHERE \"id\" = ?", Long.class, inserted.id());

        assertEquals(1L, ownerId);
    }

    @Test
    void testAnAccountCannotReadAnotherAccountsRowById() {
        DataTableRow inserted = dataTableRowService.insertRow(
            BASE_NAME, Map.of("title", "b"), ENVIRONMENT_ID, ACCOUNT_B);

        assertNull(dataTableRowService.getRow(BASE_NAME, inserted.id(), ENVIRONMENT_ID, ACCOUNT_A));
    }

    @Test
    void testAnAccountCannotDeleteAnotherAccountsRow() {
        DataTableRow inserted = dataTableRowService.insertRow(
            BASE_NAME, Map.of("title", "b"), ENVIRONMENT_ID, ACCOUNT_B);

        assertFalse(dataTableRowService.deleteRow(BASE_NAME, inserted.id(), ENVIRONMENT_ID, ACCOUNT_A));

        assertTrue(dataTableRowService.deleteRow(BASE_NAME, inserted.id(), ENVIRONMENT_ID, ACCOUNT_B));
    }

    @Test
    void testAnAccountCannotUpdateAnotherAccountsRow() {
        DataTableRow inserted = dataTableRowService.insertRow(
            BASE_NAME, Map.of("title", "b"), ENVIRONMENT_ID, ACCOUNT_B);

        // The row matches no rows for this owner, and updateRow reports a missing row the same way it does for an id
        // that never existed -- so a cross-account update cannot be told apart from a nonexistent one.
        assertThrows(
            IllegalArgumentException.class,
            () -> dataTableRowService.updateRow(
                BASE_NAME, inserted.id(), Map.of("title", "hacked"), ENVIRONMENT_ID, ACCOUNT_A));

        String title = jdbcTemplate.queryForObject(
            "SELECT \"title\" FROM \"dt_0_conversations\" WHERE \"id\" = ?", String.class, inserted.id());

        assertEquals("b", title);
    }
}

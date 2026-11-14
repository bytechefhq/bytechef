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

package com.bytechef.platform.data.table.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.config.DataTableIntTestUtils;
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.test.config.h2.H2DataSourceConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(H2DataSourceConfiguration.class)
class DataTableServiceH2IntTest {

    private static final long DEV_ENVIRONMENT_ID = 0;
    private static final long STAGE_ENVIRONMENT_ID = 1;
    private static final long WORKSPACE_ID = 1L;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableRowService dataTableRowService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void beforeEach() {
        DataTableIntTestUtils.dropAll(jdbcTemplate);
    }

    @Test
    void testTwoWorkspacesCanOwnTheSameName() {
        long firstId = dataTableService.createTable(
            1L, "orders", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);
        long secondId = dataTableService.createTable(
            2L, "orders", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        dataTableRowService.insertRow(firstId, Map.of("title", "first"), DEV_ENVIRONMENT_ID);
        dataTableRowService.insertRow(secondId, Map.of("title", "second"), DEV_ENVIRONMENT_ID);

        assertThat(firstId).isNotEqualTo(secondId);
        assertThat(dataTableService.fetchDataTable(1L, "orders")).map(DataTable::getId)
            .contains(firstId);
        assertThat(dataTableService.fetchDataTable(2L, "orders")).map(DataTable::getId)
            .contains(secondId);
        assertThat(dataTableRowService.listRows(secondId, 10, 0, DEV_ENVIRONMENT_ID))
            .extracting(dataTableRow -> dataTableRow.values()
                .get("title"))
            .containsExactly("second");
    }

    @Test
    void testCreatingTheSameNameTwiceInOneEnvironmentIsRejected() {
        createTable("orders");

        assertThatThrownBy(() -> createTable("orders"))
            .isInstanceOf(DataTableException.class)
            .hasMessage("Data table 'orders' already exists in this environment");
    }

    @Test
    void testRenamingOntoAnExistingNameInTheWorkspaceIsRejected() {
        createTable("orders");

        long invoicesId = createTable("invoices");

        assertThatThrownBy(() -> dataTableService.renameTable(invoicesId, "orders"))
            .isInstanceOf(DataTableException.class)
            .hasMessage("Data table 'orders' already exists in this workspace");
    }

    @Test
    void testDuplicateLandsInTheSourceWorkspaceAndRejectsAnExistingName() {
        long ordersId = createTable("orders");

        long copyId = dataTableService.duplicateTable(ordersId, "orders_copy", DEV_ENVIRONMENT_ID);

        assertEquals(WORKSPACE_ID, dataTableService.getDataTable(copyId)
            .getWorkspaceId());
        assertThatThrownBy(() -> dataTableService.duplicateTable(ordersId, "orders_copy", DEV_ENVIRONMENT_ID))
            .isInstanceOf(DataTableException.class)
            .hasMessage("Data table 'orders_copy' already exists in this workspace");
    }

    @Test
    void testRenameKeepsEveryEnvironmentReachable() {
        long ordersId = createTable("orders");

        dataTableService.createTable(
            WORKSPACE_ID, "orders", null, List.of(new ColumnSpec("title", ColumnType.STRING)), STAGE_ENVIRONMENT_ID);

        dataTableRowService.insertRow(ordersId, Map.of("title", "staged"), STAGE_ENVIRONMENT_ID);

        dataTableService.renameTable(ordersId, "purchases");

        assertThat(dataTableService.fetchDataTable(WORKSPACE_ID, "purchases")).map(DataTable::getId)
            .contains(ordersId);
        assertThat(dataTableRowService.listRows(ordersId, 10, 0, STAGE_ENVIRONMENT_ID)).hasSize(1);
    }

    @Test
    void testCreateTableAndListTables() {
        long createdId = dataTableService.createTable(
            WORKSPACE_ID, "created", "a description", List.of(new ColumnSpec("title", ColumnType.STRING)),
            DEV_ENVIRONMENT_ID);

        List<DataTableInfo> dataTableInfos = dataTableService.listTables(WORKSPACE_ID, DEV_ENVIRONMENT_ID);

        assertTrue(
            dataTableInfos.stream()
                .anyMatch(dataTableInfo -> dataTableInfo.id() == createdId && "created".equals(dataTableInfo.name())));
    }

    @Test
    void testAddRenameAndRemoveColumn() {
        long columnsId = createTable("columns");

        dataTableService.addColumn(columnsId, new ColumnSpec("amount", ColumnType.NUMBER), DEV_ENVIRONMENT_ID);
        dataTableService.renameColumn(columnsId, "amount", "total", DEV_ENVIRONMENT_ID);

        DataTableRow dataTableRow = dataTableRowService.insertRow(
            columnsId, Map.of("title", "renamed", "total", 12), DEV_ENVIRONMENT_ID);

        assertTrue(dataTableRow.values()
            .containsKey("total"));

        dataTableService.removeColumn(columnsId, "total", DEV_ENVIRONMENT_ID);
    }

    @Test
    void testInsertUpdateAndReadRows() {
        long rowsId = createTable("rows");

        DataTableRow insertedDataTableRow = dataTableRowService.insertRow(
            rowsId, Map.of("title", "first"), DEV_ENVIRONMENT_ID);

        assertNotNull(insertedDataTableRow);
        assertEquals("first", insertedDataTableRow.values()
            .get("title"));

        DataTableRow updatedDataTableRow = dataTableRowService.updateRow(
            rowsId, insertedDataTableRow.id(), Map.of("title", "second"), DEV_ENVIRONMENT_ID);

        assertEquals("second", updatedDataTableRow.values()
            .get("title"));

        DataTableRow fetchedDataTableRow = Objects.requireNonNull(
            dataTableRowService.getRow(rowsId, insertedDataTableRow.id(), DEV_ENVIRONMENT_ID));

        assertEquals("second", fetchedDataTableRow.values()
            .get("title"));

        assertEquals(1, dataTableRowService.listRows(rowsId, 10, 0, DEV_ENVIRONMENT_ID)
            .size());

        assertTrue(dataTableRowService.deleteRow(rowsId, insertedDataTableRow.id(), DEV_ENVIRONMENT_ID));
    }

    @Test
    void testDuplicateAndRenameTable() {
        long sourceId = createTable("source");

        dataTableRowService.insertRow(sourceId, Map.of("title", "copied"), DEV_ENVIRONMENT_ID);

        long duplicateId = dataTableService.duplicateTable(sourceId, "duplicate", DEV_ENVIRONMENT_ID);

        assertEquals(1, dataTableRowService.listRows(duplicateId, 10, 0, DEV_ENVIRONMENT_ID)
            .size());

        dataTableService.renameTable(duplicateId, "renamed");

        assertEquals("renamed", dataTableService.getDataTable(duplicateId)
            .getName());
    }

    private long createTable(String name) {
        return dataTableService.createTable(
            WORKSPACE_ID, name, null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);
    }
}

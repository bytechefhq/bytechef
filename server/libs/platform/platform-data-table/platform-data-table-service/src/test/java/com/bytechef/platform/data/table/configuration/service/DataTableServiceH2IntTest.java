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
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.UpsertResult;
import com.bytechef.platform.data.table.execution.event.DataTableWebhookEvent;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.test.config.h2.H2DataSourceConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(H2DataSourceConfiguration.class)
@RecordApplicationEvents
class DataTableServiceH2IntTest {

    private static final long DEV_ENVIRONMENT_ID = 0;
    private static final long STAGE_ENVIRONMENT_ID = 1;
    private static final long WORKSPACE_ID = 1L;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableRowService dataTableRowService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void beforeEach() {
        DataTableServiceIntTest.dropAll(jdbcTemplate);
    }

    @Test
    void testTwoWorkspacesCanOwnTheSameName() {
        long firstId = dataTableService.createTable(
            1L, "orders", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);
        long secondId = dataTableService.createTable(
            2L, "orders", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        dataTableRowService.insertRow(new DataTableRef(firstId, DEV_ENVIRONMENT_ID), Map.of("title", "first"));
        dataTableRowService.insertRow(new DataTableRef(secondId, DEV_ENVIRONMENT_ID), Map.of("title", "second"));

        assertThat(firstId).isNotEqualTo(secondId);
        assertThat(dataTableService.fetchDataTable(1L, "orders")).map(DataTable::getId)
            .contains(firstId);
        assertThat(dataTableService.fetchDataTable(2L, "orders")).map(DataTable::getId)
            .contains(secondId);
        assertThat(dataTableRowService.listRows(new DataTableRef(secondId, DEV_ENVIRONMENT_ID), 10, 0))
            .extracting(dataTableRow -> dataTableRow.values()
                .get("title"))
            .containsExactly("second");
    }

    @Test
    void testCreatingTheSameNameTwiceInOneEnvironmentIsRejected() {
        dataTableService.createTable(
            1L, "orders", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        assertThatThrownBy(() -> dataTableService.createTable(
            1L, "orders", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID))
                .isInstanceOf(DataTableException.class)
                .hasMessage("Data table 'orders' already exists in this workspace");
    }

    @Test
    void testRenamingOntoAnExistingNameInTheWorkspaceIsRejected() {
        dataTableService.createTable(
            1L, "orders", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        long invoicesId = dataTableService.createTable(
            1L, "invoices", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        assertThatThrownBy(() -> dataTableService.renameTable(invoicesId, "orders"))
            .isInstanceOf(DataTableException.class)
            .hasMessage("Data table 'orders' already exists in this workspace");
    }

    @Test
    void testDuplicateLandsInTheSourceWorkspaceAndRejectsAnExistingName() {
        long ordersId = dataTableService.createTable(
            1L, "orders", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        long copyId = dataTableService.duplicateTable(ordersId, "orders_copy", DEV_ENVIRONMENT_ID);

        assertThat(dataTableService.getDataTable(copyId)
            .getWorkspaceId()).isEqualTo(1L);
        assertThatThrownBy(() -> dataTableService.duplicateTable(ordersId, "orders_copy", DEV_ENVIRONMENT_ID))
            .isInstanceOf(DataTableException.class)
            .hasMessage("Data table 'orders_copy' already exists in this workspace");
    }

    @Test
    void testRenameKeepsEveryEnvironmentReachable() {
        long ordersId = dataTableService.createTable(
            1L, "orders", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        dataTableService.createTable(
            1L, "orders", null, List.of(new ColumnSpec("title", ColumnType.STRING)), STAGE_ENVIRONMENT_ID);
        dataTableRowService.insertRow(new DataTableRef(ordersId, STAGE_ENVIRONMENT_ID), Map.of("title", "staged"));

        dataTableService.renameTable(ordersId, "purchases");

        assertThat(dataTableService.fetchDataTable(1L, "purchases")).map(DataTable::getId)
            .contains(ordersId);
        assertThat(dataTableRowService.listRows(new DataTableRef(ordersId, STAGE_ENVIRONMENT_ID), 10, 0)).hasSize(1);
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
        long columnsId = dataTableService.createTable(
            WORKSPACE_ID, "columns", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        dataTableService.addColumn(columnsId, new ColumnSpec("amount", ColumnType.NUMBER), DEV_ENVIRONMENT_ID);
        dataTableService.renameColumn(columnsId, "amount", "total", DEV_ENVIRONMENT_ID);

        DataTableRow dataTableRow = dataTableRowService.insertRow(
            new DataTableRef(columnsId, DEV_ENVIRONMENT_ID), Map.of("title", "renamed", "total", 12));

        assertTrue(dataTableRow.values()
            .containsKey("total"));

        dataTableService.removeColumn(columnsId, "total", DEV_ENVIRONMENT_ID);
    }

    @Test
    void testInsertUpdateAndReadRows() {
        long rowsId = dataTableService.createTable(
            WORKSPACE_ID, "rows", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        DataTableRef dataTableRef = new DataTableRef(rowsId, DEV_ENVIRONMENT_ID);

        DataTableRow insertedDataTableRow = dataTableRowService.insertRow(dataTableRef, Map.of("title", "first"));

        assertNotNull(insertedDataTableRow);
        assertEquals("first", insertedDataTableRow.values()
            .get("title"));

        DataTableRow updatedDataTableRow = dataTableRowService.updateRow(
            dataTableRef, insertedDataTableRow.id(), Map.of("title", "second"));

        assertEquals("second", updatedDataTableRow.values()
            .get("title"));

        DataTableRow fetchedDataTableRow = dataTableRowService.getRow(dataTableRef, insertedDataTableRow.id());

        assertEquals("second", fetchedDataTableRow.values()
            .get("title"));

        assertEquals(1, dataTableRowService.listRows(dataTableRef, 10, 0)
            .size());

        assertTrue(dataTableRowService.deleteRow(dataTableRef, insertedDataTableRow.id()));
    }

    @Test
    void testDuplicateAndRenameTable() {
        long sourceId = dataTableService.createTable(
            WORKSPACE_ID, "source", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        dataTableRowService.insertRow(new DataTableRef(sourceId, DEV_ENVIRONMENT_ID), Map.of("title", "copied"));

        long duplicateId = dataTableService.duplicateTable(sourceId, "duplicate", DEV_ENVIRONMENT_ID);

        assertEquals(1, dataTableRowService.listRows(new DataTableRef(duplicateId, DEV_ENVIRONMENT_ID), 10, 0)
            .size());

        dataTableService.renameTable(duplicateId, "renamed");

        assertEquals("renamed", dataTableService.getDataTable(duplicateId)
            .getName());
    }

    @Test
    void testInsertAndUpdateRowPublishTheirEvents() {
        DataTableRef dataTableRef = createTitledTable("evented");

        DataTableRow insertedDataTableRow = dataTableRowService.insertRow(dataTableRef, Map.of("title", "first"));

        dataTableRowService.updateRow(dataTableRef, insertedDataTableRow.id(), Map.of("title", "second"));

        assertThat(webhookEventTypes()).containsExactly(
            DataTableWebhookType.RECORD_CREATED, DataTableWebhookType.RECORD_UPDATED);
    }

    @Test
    void testUpsertRowInsertsAndThenUpdatesTheKeyedRow() {
        DataTableRef dataTableRef = createTitledTable("upserted");

        UpsertResult createdUpsertResult = dataTableRowService.upsertRow(dataTableRef, "k-1", Map.of("title", "first"));
        UpsertResult updatedUpsertResult = dataTableRowService.upsertRow(
            dataTableRef, "k-1", Map.of("title", "second"));

        DataTableRow createdDataTableRow = createdUpsertResult.row();
        DataTableRow updatedDataTableRow = updatedUpsertResult.row();

        assertThat(createdUpsertResult.created()).isTrue();
        assertThat(updatedUpsertResult.created()).isFalse();
        assertThat(updatedDataTableRow.id()).isEqualTo(createdDataTableRow.id());
        assertThat(updatedDataTableRow.values()).containsEntry("title", "second");
    }

    @Test
    void testDeleteRowsAndClearRowsPublishTheDeletedValues() {
        DataTableRef dataTableRef = createTitledTable("cleared");

        DataTableRow firstDataTableRow = dataTableRowService.insertRow(dataTableRef, Map.of("title", "a"));

        dataTableRowService.insertRow(dataTableRef, Map.of("title", "b"));
        dataTableRowService.insertRow(dataTableRef, Map.of("title", "c"));

        assertThat(dataTableRowService.deleteRows(dataTableRef, List.of(firstDataTableRow.id())))
            .isEqualTo(List.of(firstDataTableRow.id()));
        assertThat(dataTableRowService.clearRows(dataTableRef)).isEqualTo(2);

        assertThat(applicationEvents.stream(DataTableWebhookEvent.class)
            .filter(dataTableWebhookEvent -> dataTableWebhookEvent.getType() == DataTableWebhookType.RECORD_DELETED)
            .map(dataTableWebhookEvent -> String.valueOf(((Map<?, ?>) dataTableWebhookEvent.getPayload()
                .get("values")).get("title"))))
                    .containsExactly("a", "b", "c");
    }

    private DataTableRef createTitledTable(String name) {
        long dataTableId = dataTableService.createTable(
            WORKSPACE_ID, name, null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        return new DataTableRef(dataTableId, DEV_ENVIRONMENT_ID);
    }

    private List<DataTableWebhookType> webhookEventTypes() {
        return applicationEvents.stream(DataTableWebhookEvent.class)
            .map(DataTableWebhookEvent::getType)
            .toList();
    }
}

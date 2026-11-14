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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.config.DataTableIntTestUtils;
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.repository.DataTableRepository;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
class DataTableServiceIntTest {

    private static final long DEV_ENVIRONMENT_ID = 0;
    private static final long STAGE_ENVIRONMENT_ID = 1;
    private static final long WORKSPACE_ID = 1L;

    @Autowired
    private DataTableRepository dataTableRepository;

    @Autowired
    private DataTableRowService dataTableRowService;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

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
        createTable("orders", null, DEV_ENVIRONMENT_ID);

        DataTableException dataTableException = assertThrowsExactly(
            DataTableException.class, () -> createTable("orders", null, DEV_ENVIRONMENT_ID));

        assertEquals(DataTableErrorType.DATA_TABLE_ALREADY_EXISTS, dataTableException.getErrorType());
        assertEquals("Data table 'orders' already exists in this environment", dataTableException.getMessage());
    }

    @Test
    void testRenamingOntoAnExistingNameInTheWorkspaceIsRejected() {
        createTable("orders", null, DEV_ENVIRONMENT_ID);

        long invoicesId = createTable("invoices", null, DEV_ENVIRONMENT_ID);

        assertThatThrownBy(() -> dataTableService.renameTable(invoicesId, "orders"))
            .isInstanceOf(DataTableException.class)
            .hasMessage("Data table 'orders' already exists in this workspace");
    }

    @Test
    void testDuplicateLandsInTheSourceWorkspaceAndRejectsAnExistingName() {
        long ordersId = createTable("orders", null, DEV_ENVIRONMENT_ID);

        long copyId = dataTableService.duplicateTable(ordersId, "orders_copy", DEV_ENVIRONMENT_ID);

        assertEquals(WORKSPACE_ID, dataTableService.getDataTable(copyId)
            .getWorkspaceId());
        assertThatThrownBy(() -> dataTableService.duplicateTable(ordersId, "orders_copy", DEV_ENVIRONMENT_ID))
            .isInstanceOf(DataTableException.class)
            .hasMessage("Data table 'orders_copy' already exists in this workspace");
    }

    @Test
    void testRenameKeepsEveryEnvironmentReachable() {
        long ordersId = createTable("orders", null, DEV_ENVIRONMENT_ID);

        createTable("orders", null, STAGE_ENVIRONMENT_ID);

        dataTableRowService.insertRow(ordersId, Map.of("title", "staged"), STAGE_ENVIRONMENT_ID);

        dataTableService.renameTable(ordersId, "purchases");

        assertThat(dataTableService.fetchDataTable(WORKSPACE_ID, "purchases")).map(DataTable::getId)
            .contains(ordersId);
        assertThat(dataTableService.fetchDataTable(WORKSPACE_ID, "orders")).isEmpty();
        assertThat(dataTableRowService.listRows(ordersId, 10, 0, STAGE_ENVIRONMENT_ID)).hasSize(1);
    }

    @Test
    void testCreateTableRegistersTheTable() {
        long registeredId = createTable("registered", "a description", DEV_ENVIRONMENT_ID);

        assertEquals("registered", dataTableService.getDataTable(registeredId)
            .getName());
        assertTrue(
            listedIn(DEV_ENVIRONMENT_ID, registeredId),
            "A created table must be visible to listTables, which skips tables without a physical table");
        assertFalse(listedIn(STAGE_ENVIRONMENT_ID, registeredId));
    }

    @Test
    void testTheSameNameInASecondEnvironmentReusesTheRegistryRow() {
        long devId = createTable("shared", "a description", DEV_ENVIRONMENT_ID);
        long stageId = createTable("shared", "a description", STAGE_ENVIRONMENT_ID);

        assertEquals(devId, stageId);
        assertTrue(listedIn(DEV_ENVIRONMENT_ID, devId));
        assertTrue(listedIn(STAGE_ENVIRONMENT_ID, devId));
        assertEquals(1, dataTableService.listTables(WORKSPACE_ID, DEV_ENVIRONMENT_ID)
            .size());
    }

    @Test
    void testCreateTableRegistersAMixedCaseNameUnderItsLowercasedForm() {
        long registeredId = createTable("Registered", "a description", DEV_ENVIRONMENT_ID);

        assertEquals("registered", dataTableService.getDataTable(registeredId)
            .getName());
        assertThat(dataTableService.fetchDataTable(WORKSPACE_ID, "Registered")).map(DataTable::getId)
            .contains(registeredId);
        assertThat(dataTableService.fetchDataTable(WORKSPACE_ID, "registered")).map(DataTable::getId)
            .contains(registeredId);
    }

    @Test
    void testInvalidNamesAreRejected() {
        for (String name : List.of("", "dt_orders", "has space", "1orders")) {
            DataTableException dataTableException = assertThrowsExactly(
                DataTableException.class, () -> createTable(name, null, DEV_ENVIRONMENT_ID));

            assertEquals(DataTableErrorType.DATA_TABLE_NAME_INVALID, dataTableException.getErrorType());
        }
    }

    @Test
    void testATableWithoutAWorkspaceIsFoundOnlyWithoutAWorkspace() {
        long unscopedId = dataTableService.createTable(
            null, "unscoped", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        assertThat(dataTableService.fetchDataTable(null, "unscoped")).map(DataTable::getId)
            .contains(unscopedId);
        assertThat(dataTableService.fetchDataTable(WORKSPACE_ID, "unscoped")).isEmpty();
        assertEquals(
            unscopedId,
            dataTableService.createTable(
                null, "unscoped", null, List.of(new ColumnSpec("title", ColumnType.STRING)), STAGE_ENVIRONMENT_ID));
    }

    @Test
    void testDuplicateTableRegistersTheCopy() {
        long originalId = createTable("original", null, DEV_ENVIRONMENT_ID);

        long copyId = dataTableService.duplicateTable(originalId, "copy", DEV_ENVIRONMENT_ID);

        assertEquals("copy", dataTableService.getDataTable(copyId)
            .getName());

        dataTableRowService.insertRow(copyId, Map.of("title", "a"), DEV_ENVIRONMENT_ID);

        assertEquals(1, dataTableRowService.listRows(copyId, 100, 0, DEV_ENVIRONMENT_ID)
            .size());
    }

    @Test
    void testDuplicateTableCopiesTheExternalIds() {
        long originalId = createTable("original", null, DEV_ENVIRONMENT_ID);

        dataTableRowService.insertRow(originalId, Map.of("title", "a"), "ORD-1", DEV_ENVIRONMENT_ID);

        long copyId = dataTableService.duplicateTable(originalId, "copy", DEV_ENVIRONMENT_ID);

        Optional<DataTableRow> dataTableRowOptional = dataTableRowService.fetchRowByExternalId(
            copyId, "ORD-1", DEV_ENVIRONMENT_ID);

        assertTrue(dataTableRowOptional.isPresent());
        assertEquals(Map.of("title", "a"), dataTableRowOptional.get()
            .values());
    }

    @Test
    void testDropTableShouldDeleteMetadataWhenNoPhysicalTablesRemain() {
        long droppedId = createTable("dropped", null, DEV_ENVIRONMENT_ID);

        assertTrue(dataTableService.dropTable(droppedId, DEV_ENVIRONMENT_ID));
        assertFalse(physicalTableExists("dt_0_" + droppedId));
        assertFalse(
            dataTableRepository.existsById(droppedId),
            "The registry row must go once the last physical table is dropped");
        assertFalse(dataTableService.dropTable(droppedId, DEV_ENVIRONMENT_ID));
    }

    @Test
    void testDropTableShouldPreserveMetadataWhenPhysicalTablesExistInOtherEnvironments() {
        long droppedId = createTable("dropped", null, DEV_ENVIRONMENT_ID);

        createTable("dropped", null, STAGE_ENVIRONMENT_ID);

        dataTableService.dropTable(droppedId, DEV_ENVIRONMENT_ID);

        assertFalse(physicalTableExists("dt_0_" + droppedId));
        assertTrue(physicalTableExists("dt_1_" + droppedId));
        assertTrue(
            dataTableRepository.existsById(droppedId),
            "The registry row must survive while another environment still has the physical table");
    }

    @Test
    void testFetchDataTableInfoIsPerEnvironment() {
        long registeredId = createTable("registered", "a description", DEV_ENVIRONMENT_ID);

        DataTableInfo devDataTableInfo = dataTableService.fetchDataTableInfo(registeredId, DEV_ENVIRONMENT_ID)
            .orElseThrow();

        assertEquals("registered", devDataTableInfo.name());
        assertEquals("a description", devDataTableInfo.description());
        assertEquals(WORKSPACE_ID, devDataTableInfo.workspaceId());
        assertEquals(
            List.of("title"),
            devDataTableInfo.columns()
                .stream()
                .map(ColumnSpec::name)
                .toList());
        assertTrue(physicalColumnExists("dt_0_" + registeredId, "external_id"));
        assertTrue(
            dataTableService.fetchDataTableInfo(registeredId, STAGE_ENVIRONMENT_ID)
                .isEmpty());
        assertTrue(
            dataTableService.fetchDataTableInfo(registeredId + 1000, DEV_ENVIRONMENT_ID)
                .isEmpty());
    }

    @Test
    void testReservedColumnNamesAreRejected() {
        for (String columnName : List.of("id", "external_id", "EXTERNAL_ID")) {
            DataTableException createTableException = assertThrowsExactly(
                DataTableException.class, () -> dataTableService.createTable(
                    WORKSPACE_ID, "registered", null, List.of(new ColumnSpec(columnName, ColumnType.STRING)),
                    DEV_ENVIRONMENT_ID));

            assertEquals(DataTableErrorType.COLUMN_NAME_INVALID, createTableException.getErrorType());
        }

        long registeredId = createTable("registered", null, DEV_ENVIRONMENT_ID);

        DataTableException addColumnException = assertThrowsExactly(
            DataTableException.class,
            () -> dataTableService.addColumn(
                registeredId, new ColumnSpec("external_id", ColumnType.STRING), DEV_ENVIRONMENT_ID));

        assertEquals(DataTableErrorType.COLUMN_NAME_INVALID, addColumnException.getErrorType());
    }

    @Test
    void testColumnChangesReportTypedErrors() {
        long registeredId = dataTableService.createTable(
            WORKSPACE_ID, "registered", null,
            List.of(new ColumnSpec("title", ColumnType.STRING), new ColumnSpec("note", ColumnType.STRING)),
            DEV_ENVIRONMENT_ID);

        assertEquals(
            DataTableErrorType.COLUMN_ALREADY_EXISTS,
            assertThrowsExactly(
                DataTableException.class,
                () -> dataTableService.addColumn(
                    registeredId, new ColumnSpec("title", ColumnType.STRING), DEV_ENVIRONMENT_ID))
                        .getErrorType());
        assertEquals(
            DataTableErrorType.COLUMN_NOT_FOUND,
            assertThrowsExactly(
                DataTableException.class,
                () -> dataTableService.removeColumn(registeredId, "missing", DEV_ENVIRONMENT_ID))
                    .getErrorType());
        assertEquals(
            DataTableErrorType.COLUMN_NOT_FOUND,
            assertThrowsExactly(
                DataTableException.class,
                () -> dataTableService.renameColumn(registeredId, "missing", "other", DEV_ENVIRONMENT_ID))
                    .getErrorType());
        assertEquals(
            DataTableErrorType.COLUMN_ALREADY_EXISTS,
            assertThrowsExactly(
                DataTableException.class,
                () -> dataTableService.renameColumn(registeredId, "title", "note", DEV_ENVIRONMENT_ID))
                    .getErrorType());
        assertEquals(
            DataTableErrorType.DATA_TABLE_NOT_FOUND,
            assertThrowsExactly(
                DataTableException.class,
                () -> dataTableService.addColumn(
                    registeredId, new ColumnSpec("other", ColumnType.STRING), STAGE_ENVIRONMENT_ID))
                        .getErrorType());
    }

    @Test
    void testUpdateDescription() {
        long registeredId = createTable("registered", "before", DEV_ENVIRONMENT_ID);

        dataTableService.updateDescription(registeredId, "after");

        assertEquals(
            "after",
            dataTableService.fetchDataTableInfo(registeredId, DEV_ENVIRONMENT_ID)
                .map(DataTableInfo::description)
                .orElseThrow());

        dataTableService.updateDescription(registeredId, null);

        DataTableInfo dataTableInfo = dataTableService.fetchDataTableInfo(registeredId, DEV_ENVIRONMENT_ID)
            .orElseThrow();

        assertNull(dataTableInfo.description());
        assertEquals(
            DataTableErrorType.DATA_TABLE_NOT_FOUND,
            assertThrowsExactly(
                DataTableException.class, () -> dataTableService.updateDescription(registeredId + 1000, "x"))
                    .getErrorType());
    }

    @Test
    void testAConcurrentCreateOfTheSameNameIsTheWorkspaceConflict() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        try (ExecutorService executorService = Executors.newSingleThreadExecutor()) {
            Future<?> concurrentCreateFuture = transactionTemplate.execute(transactionStatus -> {
                createTable("orders", null, DEV_ENVIRONMENT_ID);

                Future<?> future = executorService.submit(() -> createTable("orders", null, DEV_ENVIRONMENT_ID));

                awaitABlockedInsert();

                return future;
            });

            assertThat(concurrentCreateFuture).isNotNull();

            ExecutionException executionException = assertThrows(
                ExecutionException.class, () -> concurrentCreateFuture.get(30, TimeUnit.SECONDS));

            assertThat(executionException.getCause())
                .isInstanceOf(DataTableException.class)
                .hasMessage("Data table 'orders' already exists in this workspace");
            assertEquals(
                DataTableErrorType.DATA_TABLE_ALREADY_EXISTS,
                ((DataTableException) executionException.getCause()).getErrorType());
        }
    }

    private void awaitABlockedInsert() {
        long deadline = System.currentTimeMillis() + 30_000;

        while (System.currentTimeMillis() < deadline) {
            jdbcTemplate.queryForList("SELECT pg_stat_clear_snapshot()");

            Integer blockedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock' "
                    + "AND query ILIKE 'INSERT INTO%data_table%'",
                Integer.class);

            if (blockedCount != null && blockedCount > 0) {
                return;
            }

            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));
        }

        throw new IllegalStateException("The concurrent create never blocked on the registry insert");
    }

    private long createTable(String name, @Nullable String description, long environmentId) {
        return dataTableService.createTable(
            WORKSPACE_ID, name, description, List.of(new ColumnSpec("title", ColumnType.STRING)), environmentId);
    }

    private boolean listedIn(long environmentId, long dataTableId) {
        return dataTableService.listTables(WORKSPACE_ID, environmentId)
            .stream()
            .anyMatch(dataTableInfo -> dataTableInfo.id() == dataTableId);
    }

    private boolean physicalColumnExists(String physicalName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = current_schema() "
                + "AND table_name = ? AND column_name = ?",
            Integer.class, physicalName, columnName);

        return count != null && count > 0;
    }

    private boolean physicalTableExists(String physicalName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = ?",
            Integer.class, physicalName);

        return count != null && count > 0;
    }
}

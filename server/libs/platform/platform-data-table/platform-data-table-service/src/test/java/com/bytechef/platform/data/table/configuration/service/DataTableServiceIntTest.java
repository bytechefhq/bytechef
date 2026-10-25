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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.domain.ReservedColumns;
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
    private DataTableService dataTableService;

    @Autowired
    private DataTableRowService dataTableRowService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void beforeEach() {
        dropAll(jdbcTemplate);
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
    void testCreateTableRegistersTheTable() {
        long registeredId = createTable("registered", "a description", DEV_ENVIRONMENT_ID);

        assertEquals("registered", dataTableService.getDataTable(registeredId)
            .getName());

        assertTrue(
            listedIn(DEV_ENVIRONMENT_ID, registeredId),
            "A created table must be visible to listTables, which skips tables without a physical table");

        DataTableException dataTableException = assertThrowsExactly(
            DataTableException.class, () -> createTable("registered", "a description", DEV_ENVIRONMENT_ID),
            "The same name twice in one environment is a registry decision, not the physical table colliding");

        assertEquals(DataTableErrorType.DATA_TABLE_ALREADY_EXISTS.getErrorKey(), dataTableException.getErrorKey());
    }

    @Test
    void testTheSameNameInASecondEnvironmentReusesTheRegistryRow() {
        long devId = createTable("shared", "a description", DEV_ENVIRONMENT_ID);
        long stageId = createTable("shared", "a description", STAGE_ENVIRONMENT_ID);

        assertEquals(devId, stageId);
        assertTrue(listedIn(DEV_ENVIRONMENT_ID, devId), "the table must remain visible in its first environment");
        assertTrue(listedIn(STAGE_ENVIRONMENT_ID, devId), "and be visible in the second environment");
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
    void testDuplicateTableRegistersTheCopy() {
        long originalId = createTable("original", null, DEV_ENVIRONMENT_ID);

        long copyId = dataTableService.duplicateTable(originalId, "copy", DEV_ENVIRONMENT_ID);

        assertEquals("copy", dataTableService.getDataTable(copyId)
            .getName());
    }

    @Test
    void testADuplicatedTableCanStillTakeRows() {
        long sourceId = createTable("source", null, DEV_ENVIRONMENT_ID);

        long duplicateId = dataTableService.duplicateTable(sourceId, "duplicate", DEV_ENVIRONMENT_ID);

        dataTableRowService.insertRow(new DataTableRef(duplicateId, DEV_ENVIRONMENT_ID), Map.of("title", "a"));

        assertEquals(
            1,
            dataTableRowService
                .listRows(new DataTableRef(duplicateId, DEV_ENVIRONMENT_ID), 100, 0)
                .size());
    }

    @Test
    void testCreateTwiceInOneEnvironmentIsATypedConflict() {
        createTable("registered", "a description", DEV_ENVIRONMENT_ID);

        DataTableException dataTableException = assertThrows(
            DataTableException.class, () -> createTable("registered", "a description", DEV_ENVIRONMENT_ID));

        assertEquals(DataTableErrorType.DATA_TABLE_ALREADY_EXISTS.getErrorKey(), dataTableException.getErrorKey());
    }

    @Test
    void testFetchDataTableInfoIsPerEnvironment() {
        long registeredId = createTable("registered", "a description", DEV_ENVIRONMENT_ID);

        Optional<DataTableInfo> devDataTableInfo = dataTableService.fetchDataTableInfo(
            registeredId, DEV_ENVIRONMENT_ID);
        Optional<DataTableInfo> stageDataTableInfo = dataTableService.fetchDataTableInfo(
            registeredId, STAGE_ENVIRONMENT_ID);

        assertTrue(devDataTableInfo.isPresent());
        assertEquals("a description", devDataTableInfo.get()
            .description());
        assertEquals(WORKSPACE_ID, devDataTableInfo.get()
            .workspaceId());
        assertTrue(devDataTableInfo.get()
            .columns()
            .stream()
            .noneMatch(columnSpec -> ReservedColumns.isReserved(columnSpec.name())));
        assertTrue(stageDataTableInfo.isEmpty());
    }

    @Test
    void testUpdateDescriptionWritesTheRegistry() {
        long registeredId = createTable("registered", "before", DEV_ENVIRONMENT_ID);

        dataTableService.updateDescription(registeredId, "after");

        assertEquals(
            "after",
            dataTableService.fetchDataTableInfo(registeredId, DEV_ENVIRONMENT_ID)
                .orElseThrow()
                .description());
    }

    @Test
    void testColumnErrorsAreTyped() {
        long registeredId = createTable("registered", null, DEV_ENVIRONMENT_ID);

        assertEquals(
            DataTableErrorType.COLUMN_ALREADY_EXISTS.getErrorKey(),
            assertThrows(DataTableException.class, () -> dataTableService.addColumn(
                registeredId, new ColumnSpec("title", ColumnType.STRING), DEV_ENVIRONMENT_ID))
                    .getErrorKey());
        assertEquals(
            DataTableErrorType.COLUMN_NAME_INVALID.getErrorKey(),
            assertThrows(DataTableException.class, () -> dataTableService.addColumn(
                registeredId, new ColumnSpec("external_id", ColumnType.STRING), DEV_ENVIRONMENT_ID))
                    .getErrorKey());
        assertEquals(
            DataTableErrorType.COLUMN_NOT_FOUND.getErrorKey(),
            assertThrows(DataTableException.class, () -> dataTableService.removeColumn(
                registeredId, "nosuch", DEV_ENVIRONMENT_ID))
                    .getErrorKey());
    }

    @Test
    void testAConcurrentCreateOfTheSameNameIsTheWorkspaceConflict() throws Exception {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        try {
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
                DataTableErrorType.DATA_TABLE_ALREADY_EXISTS.getErrorKey(),
                ((DataTableException) executionException.getCause()).getErrorKey());
        } finally {
            executorService.shutdownNow();
        }
    }

    static void dropAll(JdbcTemplate jdbcTemplate) {
        List<String> tableNames = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = current_schema() "
                + "AND table_name LIKE 'dt\\_%'",
            String.class);

        for (String tableName : tableNames) {
            jdbcTemplate.execute("DROP TABLE IF EXISTS \"" + tableName + "\"");
        }

        List<Long> tagIds = jdbcTemplate.queryForList("SELECT DISTINCT tag_id FROM data_table_tag", Long.class);

        jdbcTemplate.update("DELETE FROM data_table_webhook");
        jdbcTemplate.update("DELETE FROM data_table_tag");
        jdbcTemplate.update("DELETE FROM data_table");

        for (Long tagId : tagIds) {
            jdbcTemplate.update("DELETE FROM tag WHERE id = ?", tagId);
        }
    }

    private long createTable(String name, @Nullable String description, long environmentId) {
        return dataTableService.createTable(
            WORKSPACE_ID, name, description, List.of(new ColumnSpec("title", ColumnType.STRING)), environmentId);
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

    private boolean listedIn(long environmentId, long dataTableId) {
        return dataTableService.listTables(WORKSPACE_ID, environmentId)
            .stream()
            .anyMatch(dataTableInfo -> dataTableInfo.id() == dataTableId);
    }
}

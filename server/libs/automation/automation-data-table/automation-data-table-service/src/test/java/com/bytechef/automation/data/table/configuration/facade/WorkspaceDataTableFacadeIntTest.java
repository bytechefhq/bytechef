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

package com.bytechef.automation.data.table.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.automation.data.table.config.AutomationDataTableIntTestConfiguration;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.exception.DataTableStorageLimitExceededException;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.platform.data.table.execution.service.DataTableStorageService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AutomationDataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class WorkspaceDataTableFacadeIntTest {

    private static final long DEV_ENVIRONMENT_ID = 0;
    private static final long STAGE_ENVIRONMENT_ID = 1;
    private static final long WORKSPACE_ID = 9001;
    private static final long SECOND_WORKSPACE_ID = 9002;
    private static final List<ColumnSpec> COLUMN_SPECS = List.of(new ColumnSpec("title", ColumnType.STRING));

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private DataTableRowService dataTableRowService;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableStorageService dataTableStorageService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WorkspaceDataTableFacade workspaceDataTableFacade;

    private long originalMaxSizeBytes;

    @BeforeEach
    void beforeEach() {
        ApplicationProperties.DataTable dataTable = applicationProperties.getDataTable();

        originalMaxSizeBytes = dataTable.getMaxSizeBytes();

        dropTestTables();

        for (long workspaceId : List.of(WORKSPACE_ID, SECOND_WORKSPACE_ID)) {
            jdbcTemplate.update("DELETE FROM workspace WHERE id = ?", workspaceId);

            jdbcTemplate.update(
                "INSERT INTO workspace " +
                    "(id, name, created_date, created_by, last_modified_date, last_modified_by, version) " +
                    "VALUES (?, ?, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)",
                workspaceId, "data-table-workspace-" + workspaceId);
        }
    }

    @AfterEach
    void afterEach() {
        setLimit(originalMaxSizeBytes);

        dropTestTables();

        for (long workspaceId : List.of(WORKSPACE_ID, SECOND_WORKSPACE_ID)) {
            jdbcTemplate.update("DELETE FROM workspace WHERE id = ?", workspaceId);
        }
    }

    @Test
    void testCreateTableAssignsTableToWorkspace() {
        long dataTableId = workspaceDataTableFacade.createTable(
            "events", "Events", COLUMN_SPECS, WORKSPACE_ID, DEV_ENVIRONMENT_ID);

        assertThat(workspaceDataTableFacade.getWorkspaceId(dataTableId)).isEqualTo(WORKSPACE_ID);
        assertThat(workspaceDataTableFacade.listTables(WORKSPACE_ID, DEV_ENVIRONMENT_ID))
            .extracting(DataTableInfo::id, DataTableInfo::name)
            .containsExactly(tuple(dataTableId, "events"));
    }

    @Test
    void testTwoWorkspacesCreateTheSameName() {
        long firstDataTableId = workspaceDataTableFacade.createTable(
            "events", null, COLUMN_SPECS, WORKSPACE_ID, DEV_ENVIRONMENT_ID);
        long secondDataTableId = workspaceDataTableFacade.createTable(
            "events", null, COLUMN_SPECS, SECOND_WORKSPACE_ID, DEV_ENVIRONMENT_ID);

        assertThat(secondDataTableId).isNotEqualTo(firstDataTableId);
        assertThat(workspaceDataTableFacade.listTables(WORKSPACE_ID, DEV_ENVIRONMENT_ID))
            .extracting(DataTableInfo::id)
            .containsExactly(firstDataTableId);
        assertThat(workspaceDataTableFacade.listTables(SECOND_WORKSPACE_ID, DEV_ENVIRONMENT_ID))
            .extracting(DataTableInfo::id)
            .containsExactly(secondDataTableId);
    }

    @Test
    void testDropTableInTheLastEnvironmentRemovesTheTableFromTheWorkspace() {
        long dataTableId = workspaceDataTableFacade.createTable(
            "dropped", null, COLUMN_SPECS, WORKSPACE_ID, DEV_ENVIRONMENT_ID);

        workspaceDataTableFacade.dropTable(dataTableId, DEV_ENVIRONMENT_ID);

        assertThat(dataTableService.getWorkspaceDataTables(WORKSPACE_ID)).isEmpty();
        assertThatThrownBy(() -> workspaceDataTableFacade.getWorkspaceId(dataTableId))
            .isInstanceOf(DataTableException.class);
    }

    @Test
    void testDropTableKeepsTheTableWhileAnotherEnvironmentHasIt() {
        long dataTableId = workspaceDataTableFacade.createTable(
            "dropped", null, COLUMN_SPECS, WORKSPACE_ID, DEV_ENVIRONMENT_ID);
        long stageDataTableId = workspaceDataTableFacade.createTable(
            "dropped", null, COLUMN_SPECS, WORKSPACE_ID, STAGE_ENVIRONMENT_ID);

        assertThat(stageDataTableId).isEqualTo(dataTableId);

        workspaceDataTableFacade.dropTable(dataTableId, DEV_ENVIRONMENT_ID);

        assertThat(workspaceDataTableFacade.getWorkspaceId(dataTableId)).isEqualTo(WORKSPACE_ID);
        assertThat(workspaceDataTableFacade.listTables(WORKSPACE_ID, DEV_ENVIRONMENT_ID)).isEmpty();
        assertThat(workspaceDataTableFacade.listTables(WORKSPACE_ID, STAGE_ENVIRONMENT_ID))
            .extracting(DataTableInfo::name)
            .containsExactly("dropped");
    }

    @Test
    void testDuplicateTableIsListedInSourceWorkspace() {
        long originalDataTableId = workspaceDataTableFacade.createTable(
            "original", null, COLUMN_SPECS, WORKSPACE_ID, DEV_ENVIRONMENT_ID);

        long copyDataTableId = workspaceDataTableFacade.duplicateTable(originalDataTableId, "copy", DEV_ENVIRONMENT_ID);

        assertThat(copyDataTableId).isNotEqualTo(originalDataTableId);
        assertThat(workspaceDataTableFacade.getWorkspaceId(copyDataTableId)).isEqualTo(WORKSPACE_ID);
        assertThat(workspaceDataTableFacade.listTables(WORKSPACE_ID, DEV_ENVIRONMENT_ID))
            .extracting(DataTableInfo::name)
            .containsExactlyInAnyOrder("original", "copy");
        assertThat(workspaceDataTableFacade.listTables(SECOND_WORKSPACE_ID, DEV_ENVIRONMENT_ID)).isEmpty();
    }

    @Test
    void testDuplicateTableIsRejectedWhenTheCopyWouldExceedTheStorageLimit() {
        long originalDataTableId = workspaceDataTableFacade.createTable(
            "original", null, COLUMN_SPECS, WORKSPACE_ID, DEV_ENVIRONMENT_ID);

        dataTableRowService.insertRow(originalDataTableId, Map.of("title", "first"), DEV_ENVIRONMENT_ID);

        limitStorageToLiveBytesPlus(0);

        assertThatThrownBy(
            () -> workspaceDataTableFacade.duplicateTable(originalDataTableId, "copy", DEV_ENVIRONMENT_ID))
                .isInstanceOf(DataTableStorageLimitExceededException.class);

        assertThat(workspaceDataTableFacade.listTables(WORKSPACE_ID, DEV_ENVIRONMENT_ID))
            .extracting(DataTableInfo::name)
            .containsExactly("original");
    }

    @Test
    void testDuplicateTableWithoutSourceWorkspaceLeavesTheCopyWithoutWorkspace() {
        long eventsDataTableId = dataTableService.createTable(null, "events", null, COLUMN_SPECS, DEV_ENVIRONMENT_ID);

        long eventsCopyDataTableId = workspaceDataTableFacade.duplicateTable(
            eventsDataTableId, "events_copy", DEV_ENVIRONMENT_ID);

        DataTable eventsCopyDataTable = dataTableService.getDataTable(eventsCopyDataTableId);

        assertThat(eventsCopyDataTable.getWorkspaceId()).isNull();
        assertThat(dataTableService.listTables(null, DEV_ENVIRONMENT_ID))
            .extracting(DataTableInfo::name)
            .contains("events", "events_copy");
    }

    @Test
    void testRenameTableKeepsItsRowsAndWorkspace() {
        long dataTableId = workspaceDataTableFacade.createTable(
            "events", null, COLUMN_SPECS, WORKSPACE_ID, DEV_ENVIRONMENT_ID);

        dataTableRowService.insertRow(dataTableId, Map.of("title", "first"), DEV_ENVIRONMENT_ID);

        workspaceDataTableFacade.renameTable(dataTableId, "renamed_events");

        DataTableInfo dataTableInfo = workspaceDataTableFacade.getTable(dataTableId, DEV_ENVIRONMENT_ID);

        assertThat(dataTableInfo.name()).isEqualTo("renamed_events");
        assertThat(workspaceDataTableFacade.getWorkspaceId(dataTableId)).isEqualTo(WORKSPACE_ID);
        assertThat(workspaceDataTableFacade.listRows(dataTableId, 10, 0, DEV_ENVIRONMENT_ID)).hasSize(1);
    }

    @Test
    void testGetWorkspaceIdOfATableWithoutWorkspaceIsRejected() {
        long dataTableId = dataTableService.createTable(null, "events", null, COLUMN_SPECS, DEV_ENVIRONMENT_ID);

        assertThatThrownBy(() -> workspaceDataTableFacade.getWorkspaceId(dataTableId))
            .isInstanceOf(DataTableException.class);
    }

    @Test
    void testGetTagsByTableIdListsEveryWorkspaceTable() {
        long eventsDataTableId = workspaceDataTableFacade.createTable(
            "events", null, COLUMN_SPECS, WORKSPACE_ID, DEV_ENVIRONMENT_ID);
        long originalDataTableId = workspaceDataTableFacade.createTable(
            "original", null, COLUMN_SPECS, WORKSPACE_ID, DEV_ENVIRONMENT_ID);

        workspaceDataTableFacade.createTable("copy", null, COLUMN_SPECS, SECOND_WORKSPACE_ID, DEV_ENVIRONMENT_ID);

        Map<Long, List<Tag>> tagsByTableId = workspaceDataTableFacade.getTagsByTableId(WORKSPACE_ID);

        assertThat(tagsByTableId).containsOnlyKeys(eventsDataTableId, originalDataTableId);
    }

    @Test
    void testListTablesReturnsOnlyWorkspaceTables() {
        workspaceDataTableFacade.createTable("events", null, COLUMN_SPECS, WORKSPACE_ID, DEV_ENVIRONMENT_ID);
        workspaceDataTableFacade.createTable(
            "events_copy", null, COLUMN_SPECS, SECOND_WORKSPACE_ID, DEV_ENVIRONMENT_ID);

        assertThat(dataTableService.listAllTables(DEV_ENVIRONMENT_ID))
            .extracting(DataTableInfo::name)
            .contains("events", "events_copy");
        assertThat(workspaceDataTableFacade.listTables(WORKSPACE_ID, DEV_ENVIRONMENT_ID))
            .extracting(DataTableInfo::name)
            .containsExactly("events");
    }

    private void limitStorageToLiveBytesPlus(long headroomBytes) {
        List<String> physicalTableNames = jdbcTemplate.queryForList(
            "SELECT tablename FROM pg_tables WHERE schemaname = current_schema() AND tablename LIKE 'dt\\_%' " +
                "ESCAPE '\\'",
            String.class);

        long liveBytes = physicalTableNames.stream()
            .mapToLong(physicalTableName -> {
                String[] physicalTableNameParts = physicalTableName.split("_", 3);

                return dataTableStorageService.getTableBytes(
                    Long.parseLong(physicalTableNameParts[2]), Long.parseLong(physicalTableNameParts[1]));
            })
            .sum();

        setLimit(liveBytes + headroomBytes);
    }

    private void setLimit(long maxSizeBytes) {
        ApplicationProperties.DataTable dataTable = applicationProperties.getDataTable();

        dataTable.setMaxSizeBytes(maxSizeBytes);
    }

    private void dropTestTables() {
        List<DataTable> dataTables = new ArrayList<>(dataTableService.getWorkspaceDataTables(WORKSPACE_ID));

        dataTables.addAll(dataTableService.getWorkspaceDataTables(SECOND_WORKSPACE_ID));

        dataTableService.fetchDataTable(null, "events")
            .ifPresent(dataTables::add);
        dataTableService.fetchDataTable(null, "events_copy")
            .ifPresent(dataTables::add);

        for (DataTable dataTable : dataTables) {
            dataTableService.dropTable(dataTable.getId(), DEV_ENVIRONMENT_ID);
            dataTableService.dropTable(dataTable.getId(), STAGE_ENVIRONMENT_ID);
        }
    }
}

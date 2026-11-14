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

import com.bytechef.automation.data.table.config.AutomationDataTableIntTestConfiguration;
import com.bytechef.automation.data.table.configuration.domain.WorkspaceDataTable;
import com.bytechef.automation.data.table.configuration.service.WorkspaceDataTableService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.exception.DataTableStorageLimitExceededException;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.platform.data.table.execution.service.DataTableStorageService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
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

    @Autowired
    private WorkspaceDataTableService workspaceDataTableService;

    private long originalMaxSizeBytes;

    @BeforeEach
    void beforeEach() {
        ApplicationProperties.DataTable dataTable = applicationProperties.getDataTable();

        originalMaxSizeBytes = dataTable.getMaxSizeBytes();

        dropTestTables();

        for (long workspaceId : List.of(WORKSPACE_ID, SECOND_WORKSPACE_ID)) {
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
            jdbcTemplate.update("DELETE FROM workspace_data_table WHERE workspace_id = ?", workspaceId);
            jdbcTemplate.update("DELETE FROM workspace WHERE id = ?", workspaceId);
        }
    }

    @Test
    void testCreateTableAssignsTableToWorkspace() {
        workspaceDataTableFacade.createTable("events", "Events", COLUMN_SPECS, WORKSPACE_ID, DEV_ENVIRONMENT_ID);

        long dataTableId = dataTableService.getIdByBaseName("events");

        assertThat(workspaceDataTableService.getDataTableWorkspaceDataTables(dataTableId))
            .extracting(WorkspaceDataTable::getWorkspaceId)
            .containsExactly(WORKSPACE_ID);
        assertThat(workspaceDataTableFacade.listTables(WORKSPACE_ID, DEV_ENVIRONMENT_ID))
            .extracting(DataTableInfo::baseName)
            .containsExactly("events");
    }

    @Test
    void testDropTableRemovesWorkspaceAssignment() {
        workspaceDataTableFacade.createTable("dropped", null, COLUMN_SPECS, WORKSPACE_ID, DEV_ENVIRONMENT_ID);

        long dataTableId = dataTableService.getIdByBaseName("dropped");

        assertThat(workspaceDataTableService.getDataTableWorkspaceDataTables(dataTableId)).hasSize(1);

        dataTableService.dropTable("dropped", DEV_ENVIRONMENT_ID);

        assertThat(workspaceDataTableService.getDataTableWorkspaceDataTables(dataTableId)).isEmpty();
        assertThat(workspaceDataTableService.getWorkspaceDataTables(WORKSPACE_ID)).isEmpty();
    }

    @Test
    void testDropTableKeepsWorkspaceAssignmentWhileAnotherEnvironmentHasTheTable() {
        workspaceDataTableFacade.createTable("dropped", null, COLUMN_SPECS, WORKSPACE_ID, DEV_ENVIRONMENT_ID);
        workspaceDataTableFacade.createTable("dropped", null, COLUMN_SPECS, WORKSPACE_ID, STAGE_ENVIRONMENT_ID);

        long dataTableId = dataTableService.getIdByBaseName("dropped");

        dataTableService.dropTable("dropped", DEV_ENVIRONMENT_ID);

        assertThat(workspaceDataTableService.getDataTableWorkspaceDataTables(dataTableId))
            .extracting(WorkspaceDataTable::getWorkspaceId)
            .containsExactly(WORKSPACE_ID);
        assertThat(workspaceDataTableFacade.listTables(WORKSPACE_ID, STAGE_ENVIRONMENT_ID))
            .extracting(DataTableInfo::baseName)
            .containsExactly("dropped");
    }

    @Test
    void testDuplicateTableIsListedInSourceWorkspace() {
        workspaceDataTableFacade.createTable("original", null, COLUMN_SPECS, WORKSPACE_ID, DEV_ENVIRONMENT_ID);

        long originalDataTableId = dataTableService.getIdByBaseName("original");

        workspaceDataTableFacade.duplicateTable(originalDataTableId, "copy", DEV_ENVIRONMENT_ID);

        assertThat(workspaceDataTableFacade.listTables(WORKSPACE_ID, DEV_ENVIRONMENT_ID))
            .extracting(DataTableInfo::baseName)
            .containsExactlyInAnyOrder("original", "copy");
    }

    @Test
    void testDuplicateTableIsRejectedWhenTheCopyWouldExceedTheStorageLimit() {
        workspaceDataTableFacade.createTable("original", null, COLUMN_SPECS, WORKSPACE_ID, DEV_ENVIRONMENT_ID);

        dataTableRowService.insertRow("original", Map.of("title", "first"), DEV_ENVIRONMENT_ID);

        long originalDataTableId = dataTableService.getIdByBaseName("original");

        limitStorageToLiveBytesPlus(0);

        assertThatThrownBy(
            () -> workspaceDataTableFacade.duplicateTable(originalDataTableId, "copy", DEV_ENVIRONMENT_ID))
                .isInstanceOf(DataTableStorageLimitExceededException.class);

        assertThat(workspaceDataTableFacade.listTables(WORKSPACE_ID, DEV_ENVIRONMENT_ID))
            .extracting(DataTableInfo::baseName)
            .containsExactly("original");
    }

    @Test
    void testDuplicateTableAssignsCopyToSourceWorkspace() {
        workspaceDataTableFacade.createTable("events", null, COLUMN_SPECS, WORKSPACE_ID, DEV_ENVIRONMENT_ID);

        long eventsDataTableId = dataTableService.getIdByBaseName("events");

        workspaceDataTableFacade.duplicateTable(eventsDataTableId, "events_copy", DEV_ENVIRONMENT_ID);

        long eventsCopyDataTableId = dataTableService.getIdByBaseName("events_copy");

        assertThat(eventsCopyDataTableId).isNotEqualTo(eventsDataTableId);
        assertThat(workspaceDataTableService.getDataTableWorkspaceDataTables(eventsCopyDataTableId))
            .extracting(WorkspaceDataTable::getWorkspaceId)
            .containsExactly(WORKSPACE_ID);
    }

    @Test
    void testDuplicateTableAssignsCopyToEverySourceWorkspace() {
        workspaceDataTableFacade.createTable("events", null, COLUMN_SPECS, WORKSPACE_ID, DEV_ENVIRONMENT_ID);

        long eventsDataTableId = dataTableService.getIdByBaseName("events");

        workspaceDataTableService.assignDataTableToWorkspace(eventsDataTableId, SECOND_WORKSPACE_ID);

        workspaceDataTableFacade.duplicateTable(eventsDataTableId, "events_copy", DEV_ENVIRONMENT_ID);

        long eventsCopyDataTableId = dataTableService.getIdByBaseName("events_copy");

        assertThat(workspaceDataTableService.getDataTableWorkspaceDataTables(eventsCopyDataTableId))
            .extracting(WorkspaceDataTable::getWorkspaceId)
            .containsExactlyInAnyOrder(WORKSPACE_ID, SECOND_WORKSPACE_ID);
        assertThat(workspaceDataTableFacade.listTables(SECOND_WORKSPACE_ID, DEV_ENVIRONMENT_ID))
            .extracting(DataTableInfo::baseName)
            .containsExactlyInAnyOrder("events", "events_copy");
    }

    @Test
    void testDuplicateTableWithoutSourceWorkspaceAssignsNothing() {
        dataTableService.createTable("events", null, COLUMN_SPECS, DEV_ENVIRONMENT_ID);

        long eventsDataTableId = dataTableService.getIdByBaseName("events");

        workspaceDataTableFacade.duplicateTable(eventsDataTableId, "events_copy", DEV_ENVIRONMENT_ID);

        long eventsCopyDataTableId = dataTableService.getIdByBaseName("events_copy");

        assertThat(dataTableService.listTables(DEV_ENVIRONMENT_ID))
            .extracting(DataTableInfo::baseName)
            .contains("events", "events_copy");
        assertThat(workspaceDataTableService.getDataTableWorkspaceDataTables(eventsCopyDataTableId)).isEmpty();
    }

    @Test
    void testListTablesReturnsOnlyWorkspaceTables() {
        workspaceDataTableFacade.createTable("events", null, COLUMN_SPECS, WORKSPACE_ID, DEV_ENVIRONMENT_ID);
        workspaceDataTableFacade.createTable(
            "events_copy", null, COLUMN_SPECS, SECOND_WORKSPACE_ID, DEV_ENVIRONMENT_ID);

        assertThat(dataTableService.listTables(DEV_ENVIRONMENT_ID))
            .extracting(DataTableInfo::baseName)
            .contains("events", "events_copy");
        assertThat(workspaceDataTableFacade.listTables(WORKSPACE_ID, DEV_ENVIRONMENT_ID))
            .extracting(DataTableInfo::baseName)
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
                    physicalTableNameParts[2], Long.parseLong(physicalTableNameParts[1]));
            })
            .sum();

        setLimit(liveBytes + headroomBytes);
    }

    private void setLimit(long maxSizeBytes) {
        ApplicationProperties.DataTable dataTable = applicationProperties.getDataTable();

        dataTable.setMaxSizeBytes(maxSizeBytes);
    }

    private void dropTestTables() {
        for (String baseName : List.of("dropped", "original", "copy", "events", "events_copy")) {
            dataTableService.dropTable(baseName, DEV_ENVIRONMENT_ID);
            dataTableService.dropTable(baseName, STAGE_ENVIRONMENT_ID);
        }
    }
}

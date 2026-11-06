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

import com.bytechef.automation.data.table.config.AutomationDataTableIntTestConfiguration;
import com.bytechef.automation.data.table.configuration.domain.WorkspaceDataTable;
import com.bytechef.automation.data.table.configuration.service.WorkspaceDataTableService;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort;
import com.bytechef.platform.data.table.execution.domain.NewRow;
import com.bytechef.platform.data.table.execution.domain.UpsertResult;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
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
    private DataTableService dataTableService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WorkspaceDataTableFacade workspaceDataTableFacade;

    @Autowired
    private WorkspaceDataTableService workspaceDataTableService;

    @BeforeEach
    void beforeEach() {
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

    @Test
    void testRowOperationsResolveTheTableById() {
        workspaceDataTableFacade.createTable("events", null, COLUMN_SPECS, WORKSPACE_ID, DEV_ENVIRONMENT_ID);

        long dataTableId = dataTableService.getIdByBaseName("events");

        assertThat(workspaceDataTableFacade.getWorkspaceId(dataTableId)).isEqualTo(WORKSPACE_ID);
        assertThat(workspaceDataTableFacade.getTable(dataTableId, DEV_ENVIRONMENT_ID)
            .baseName()).isEqualTo("events");

        DataTableRow keyedDataTableRow = workspaceDataTableFacade.insertRow(
            dataTableId, Map.of("title", "a"), "EVT-1", DEV_ENVIRONMENT_ID);

        List<DataTableRow> insertedDataTableRows = workspaceDataTableFacade.insertRows(
            dataTableId, List.of(new NewRow(Map.of("title", "b"), null), new NewRow(Map.of("title", "c"), null)),
            CreateStrategy.INSERT, DEV_ENVIRONMENT_ID);

        assertThat(insertedDataTableRows).hasSize(2);

        Page<DataTableRow> page = workspaceDataTableFacade.listRows(
            dataTableId, DataTableRowConditions.matchAll(),
            List.of(new DataTableRowSort("title", DataTableRowSort.Direction.DESC)), 0, 2, DEV_ENVIRONMENT_ID);

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent())
            .extracting(dataTableRow -> dataTableRow.values()
                .get("title"))
            .containsExactly("c", "b");

        UpsertResult upsertResult = workspaceDataTableFacade.upsertRow(
            dataTableId, "EVT-1", Map.of("title", "z"), DEV_ENVIRONMENT_ID);

        assertThat(upsertResult.created()).isFalse();
        assertThat(upsertResult.row()
            .id()).isEqualTo(keyedDataTableRow.id());

        List<Long> deletedRowIds = workspaceDataTableFacade.deleteRows(
            dataTableId, List.of(insertedDataTableRows.getFirst()
                .id()),
            DEV_ENVIRONMENT_ID);

        assertThat(deletedRowIds).containsExactly(insertedDataTableRows.getFirst()
            .id());
        assertThat(workspaceDataTableFacade.deleteRowByExternalId(dataTableId, "EVT-1", DEV_ENVIRONMENT_ID)).isTrue();
        assertThat(workspaceDataTableFacade.deleteRowByExternalId(dataTableId, "EVT-1", DEV_ENVIRONMENT_ID)).isFalse();
        assertThat(workspaceDataTableFacade.clearRows(dataTableId, DEV_ENVIRONMENT_ID)).isEqualTo(1);
    }

    private void dropTestTables() {
        for (String baseName : List.of("dropped", "original", "copy", "events", "events_copy")) {
            dataTableService.dropTable(baseName, DEV_ENVIRONMENT_ID);
            dataTableService.dropTable(baseName, STAGE_ENVIRONMENT_ID);
        }
    }
}

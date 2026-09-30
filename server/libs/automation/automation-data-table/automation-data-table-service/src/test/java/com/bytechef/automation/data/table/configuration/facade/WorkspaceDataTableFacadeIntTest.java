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
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
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
    private static final long FIRST_WORKSPACE_ID = 9001;
    private static final long SECOND_WORKSPACE_ID = 9002;
    private static final List<ColumnSpec> COLUMN_SPECS = List.of(new ColumnSpec("title", ColumnType.STRING));

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WorkspaceDataTableFacade workspaceDataTableFacade;

    @BeforeEach
    void beforeEach() {
        for (long workspaceId : List.of(FIRST_WORKSPACE_ID, SECOND_WORKSPACE_ID)) {
            dropWorkspaceTables(workspaceId);

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
        for (long workspaceId : List.of(FIRST_WORKSPACE_ID, SECOND_WORKSPACE_ID)) {
            dropWorkspaceTables(workspaceId);

            jdbcTemplate.update("DELETE FROM workspace WHERE id = ?", workspaceId);
        }
    }

    @Test
    void testDropTableInTheLastEnvironmentRemovesTheTableFromTheWorkspace() {
        long dataTableId = workspaceDataTableFacade.createTable(
            "dropped", null, COLUMN_SPECS, FIRST_WORKSPACE_ID, DEV_ENVIRONMENT_ID);

        assertThat(workspaceDataTableFacade.getWorkspaceId(dataTableId)).isEqualTo(FIRST_WORKSPACE_ID);

        workspaceDataTableFacade.dropTable(dataTableId, DEV_ENVIRONMENT_ID);

        assertThat(dataTableService.getWorkspaceDataTables(FIRST_WORKSPACE_ID)).isEmpty();
    }

    @Test
    void testDropTableKeepsTheTableWhileAnotherEnvironmentHasIt() {
        long dataTableId = workspaceDataTableFacade.createTable(
            "dropped", null, COLUMN_SPECS, FIRST_WORKSPACE_ID, DEV_ENVIRONMENT_ID);
        long stageDataTableId = workspaceDataTableFacade.createTable(
            "dropped", null, COLUMN_SPECS, FIRST_WORKSPACE_ID, STAGE_ENVIRONMENT_ID);

        assertThat(stageDataTableId).isEqualTo(dataTableId);

        workspaceDataTableFacade.dropTable(dataTableId, DEV_ENVIRONMENT_ID);

        assertThat(workspaceDataTableFacade.getWorkspaceId(dataTableId)).isEqualTo(FIRST_WORKSPACE_ID);
        assertThat(workspaceDataTableFacade.listTables(FIRST_WORKSPACE_ID, STAGE_ENVIRONMENT_ID))
            .extracting(DataTableInfo::name)
            .containsExactly("dropped");
    }

    @Test
    void testDuplicateTableIsListedInSourceWorkspace() {
        long originalDataTableId = workspaceDataTableFacade.createTable(
            "original", null, COLUMN_SPECS, FIRST_WORKSPACE_ID, DEV_ENVIRONMENT_ID);

        long copyDataTableId = workspaceDataTableFacade.duplicateTable(originalDataTableId, "copy", DEV_ENVIRONMENT_ID);

        assertThat(workspaceDataTableFacade.getWorkspaceId(copyDataTableId)).isEqualTo(FIRST_WORKSPACE_ID);
        assertThat(workspaceDataTableFacade.listTables(FIRST_WORKSPACE_ID, DEV_ENVIRONMENT_ID))
            .extracting(DataTableInfo::name)
            .containsExactlyInAnyOrder("original", "copy");
        assertThat(workspaceDataTableFacade.listTables(SECOND_WORKSPACE_ID, DEV_ENVIRONMENT_ID)).isEmpty();
    }

    @Test
    void testTwoWorkspacesCreateTheSameName() {
        long firstId = workspaceDataTableFacade.createTable(
            "orders", null, List.of(new ColumnSpec("title", ColumnType.STRING)), FIRST_WORKSPACE_ID, 0L);
        long secondId = workspaceDataTableFacade.createTable(
            "orders", null, List.of(new ColumnSpec("title", ColumnType.STRING)), SECOND_WORKSPACE_ID, 0L);

        assertThat(workspaceDataTableFacade.listTables(FIRST_WORKSPACE_ID, 0L))
            .extracting(DataTableInfo::id)
            .containsExactly(firstId);
        assertThat(workspaceDataTableFacade.listTables(SECOND_WORKSPACE_ID, 0L))
            .extracting(DataTableInfo::id)
            .containsExactly(secondId);
    }

    private void dropWorkspaceTables(long workspaceId) {
        for (DataTable dataTable : dataTableService.getWorkspaceDataTables(workspaceId)) {
            dataTableService.dropTable(dataTable.getId(), DEV_ENVIRONMENT_ID);
            dataTableService.dropTable(dataTable.getId(), STAGE_ENVIRONMENT_ID);
        }
    }
}

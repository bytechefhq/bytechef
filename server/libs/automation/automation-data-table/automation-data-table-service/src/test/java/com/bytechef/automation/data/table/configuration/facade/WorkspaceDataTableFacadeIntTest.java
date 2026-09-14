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
    private static final long WORKSPACE_ID = 9001;
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

        jdbcTemplate.update(
            "INSERT INTO workspace " +
                "(id, name, created_date, created_by, last_modified_date, last_modified_by, version) " +
                "VALUES (?, 'data-table-workspace', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)",
            WORKSPACE_ID);
    }

    @AfterEach
    void afterEach() {
        dropTestTables();

        jdbcTemplate.update("DELETE FROM workspace_data_table WHERE workspace_id = ?", WORKSPACE_ID);
        jdbcTemplate.update("DELETE FROM workspace WHERE id = ?", WORKSPACE_ID);
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

    private void dropTestTables() {
        for (String baseName : List.of("dropped", "original", "copy")) {
            dataTableService.dropTable(baseName, DEV_ENVIRONMENT_ID);
            dataTableService.dropTable(baseName, STAGE_ENVIRONMENT_ID);
        }
    }
}

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

package com.bytechef.automation.data.table.configuration.service;

import static com.bytechef.platform.configuration.domain.Environment.DEVELOPMENT;
import static com.bytechef.platform.configuration.domain.Environment.PRODUCTION;
import static com.bytechef.platform.configuration.domain.Environment.STAGING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.data.table.configuration.config.DataTableIntTestConfiguration;
import com.bytechef.automation.data.table.configuration.domain.DataTableInfo;
import com.bytechef.automation.data.table.configuration.domain.WorkspaceDataTable;
import com.bytechef.automation.data.table.configuration.repository.DataTableRepository;
import com.bytechef.automation.data.table.configuration.repository.WorkspaceDataTableRepository;
import com.bytechef.automation.data.table.domain.ColumnSpec;
import com.bytechef.automation.data.table.domain.ColumnType;
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
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
public class DataTableServiceIntTest {

    @Autowired
    private DataTableRepository dataTableRepository;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WorkspaceDataTableRepository workspaceDataTableRepository;

    @BeforeEach
    public void beforeEach() {
        cleanupTables();
        setupTestWorkspaces();
    }

    @AfterEach
    public void afterEach() {
        cleanupTables();
    }

    private void setupTestWorkspaces() {
        jdbcTemplate.execute(
            "INSERT INTO workspace (id, name, created_date, created_by, last_modified_date, last_modified_by, version) "
                +
                "VALUES (100, 'Test Workspace 1', NOW(), 'system', NOW(), 'system', 0) " +
                "ON CONFLICT (id) DO NOTHING");
        jdbcTemplate.execute(
            "INSERT INTO workspace (id, name, created_date, created_by, last_modified_date, last_modified_by, version) "
                +
                "VALUES (200, 'Test Workspace 2', NOW(), 'system', NOW(), 'system', 0) " +
                "ON CONFLICT (id) DO NOTHING");
    }

    private void cleanupTables() {
        workspaceDataTableRepository.deleteAll();
        dataTableRepository.deleteAll();

        // Clean up any tables created during tests
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_0_orders\"");
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_0_products\"");
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_0_orphandata\"");
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_0_all_types\"");
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_1_tempdata\"");
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_1_invoices\"");
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_1_copied_table\"");
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_1_renamed_table\"");
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_2_invoices\"");
    }

    @Test
    public void testCreateTableBuildsPhysicalNameWithEnvironmentIndex() {
        dataTableService.createTable(
            "orders",
            List.of(new ColumnSpec("name", ColumnType.STRING), new ColumnSpec("age", ColumnType.INTEGER)),
            DEVELOPMENT.ordinal());

        List<DataTableInfo> dataTableInfos = dataTableService.listTables(DEVELOPMENT.ordinal());

        assertThat(dataTableInfos).hasSize(1);
        assertThat(dataTableInfos.getFirst()
            .baseName()).isEqualTo("orders");
        assertThat(dataTableInfos.getFirst()
            .columns()).hasSize(2);
    }

    @Test
    public void testCreateTableWithWorkspaceId() {
        long workspaceId = 100L;

        dataTableService.createTable(
            workspaceId,
            "products",
            List.of(new ColumnSpec("name", ColumnType.STRING)),
            DEVELOPMENT.ordinal());

        List<DataTableInfo> dataTableInfos = dataTableService.listTables(workspaceId, DEVELOPMENT.ordinal());

        assertThat(dataTableInfos).hasSize(1);
        assertThat(dataTableInfos.getFirst()
            .baseName()).isEqualTo("products");

        List<WorkspaceDataTable> workspaceDataTables = workspaceDataTableRepository.findAllByWorkspaceId(workspaceId);

        assertThat(workspaceDataTables).hasSize(1);
    }

    @Test
    public void testCreateTableRejectsBaseNameStartingWithDtPrefix() {
        assertThatThrownBy(() -> dataTableService.createTable(
            "dt_orders",
            List.of(new ColumnSpec("name", ColumnType.STRING)),
            DEVELOPMENT.ordinal()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testCreateTableRejectsIdColumn() {
        assertThatThrownBy(() -> dataTableService.createTable(
            "orders",
            List.of(new ColumnSpec("id", ColumnType.INTEGER)),
            DEVELOPMENT.ordinal()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reserved");
    }

    @Test
    public void testDropTableDeletesWorkspaceDataTableEntries() {
        long workspaceId = 100L;

        dataTableService.createTable(
            workspaceId,
            "tempdata",
            List.of(new ColumnSpec("value", ColumnType.STRING)),
            STAGING.ordinal());

        List<WorkspaceDataTable> beforeDelete = workspaceDataTableRepository.findAllByWorkspaceId(workspaceId);

        assertThat(beforeDelete).hasSize(1);

        dataTableService.dropTable("tempdata", STAGING.ordinal());

        List<WorkspaceDataTable> afterDelete = workspaceDataTableRepository.findAllByWorkspaceId(workspaceId);

        assertThat(afterDelete).isEmpty();

        List<DataTableInfo> tables = dataTableService.listTables(STAGING.ordinal());

        assertThat(tables).isEmpty();
    }

    @Test
    public void testDropTableWithNoWorkspaceEntriesStillDeletes() {
        dataTableService.createTable(
            "orphandata",
            List.of(new ColumnSpec("value", ColumnType.STRING)),
            DEVELOPMENT.ordinal());

        List<DataTableInfo> beforeDelete = dataTableService.listTables(DEVELOPMENT.ordinal());

        assertThat(beforeDelete).hasSize(1);

        dataTableService.dropTable("orphandata", DEVELOPMENT.ordinal());

        List<DataTableInfo> afterDelete = dataTableService.listTables(DEVELOPMENT.ordinal());

        assertThat(afterDelete).isEmpty();
    }

    @Test
    public void testAddColumn() {
        dataTableService.createTable(
            "invoices",
            List.of(new ColumnSpec("amount", ColumnType.NUMBER)),
            STAGING.ordinal());

        dataTableService.addColumn("invoices", new ColumnSpec("due_date", ColumnType.DATE), STAGING.ordinal());

        List<DataTableInfo> dataTableInfos = dataTableService.listTables(STAGING.ordinal());

        assertThat(dataTableInfos).hasSize(1);
        assertThat(dataTableInfos.getFirst()
            .columns()).hasSize(2);
        assertThat(dataTableInfos.getFirst()
            .columns())
                .extracting(ColumnSpec::name)
                .containsExactlyInAnyOrder("amount", "due_date");
    }

    @Test
    public void testRemoveColumn() {
        dataTableService.createTable(
            "invoices",
            List.of(new ColumnSpec("amount", ColumnType.NUMBER), new ColumnSpec("status", ColumnType.STRING)),
            PRODUCTION.ordinal());

        dataTableService.removeColumn("invoices", "status", PRODUCTION.ordinal());

        List<DataTableInfo> dataTableInfos = dataTableService.listTables(PRODUCTION.ordinal());

        assertThat(dataTableInfos).hasSize(1);
        assertThat(dataTableInfos.getFirst()
            .columns()).hasSize(1);
        assertThat(dataTableInfos.getFirst()
            .columns()
            .getFirst()
            .name()).isEqualTo("amount");
    }

    @Test
    public void testRenameColumn() {
        dataTableService.createTable(
            "invoices",
            List.of(new ColumnSpec("old_name", ColumnType.STRING)),
            STAGING.ordinal());

        dataTableService.renameColumn("invoices", "old_name", "new_name", STAGING.ordinal());

        List<DataTableInfo> dataTableInfos = dataTableService.listTables(STAGING.ordinal());

        assertThat(dataTableInfos).hasSize(1);
        assertThat(dataTableInfos.getFirst()
            .columns()
            .getFirst()
            .name()).isEqualTo("new_name");
    }

    @Test
    public void testRenameTable() {
        dataTableService.createTable(
            "tempdata",
            List.of(new ColumnSpec("value", ColumnType.STRING)),
            STAGING.ordinal());

        dataTableService.renameTable("tempdata", "renamed_table", STAGING.ordinal());

        List<DataTableInfo> dataTableInfos = dataTableService.listTables(STAGING.ordinal());

        assertThat(dataTableInfos).hasSize(1);
        assertThat(dataTableInfos.getFirst()
            .baseName()).isEqualTo("renamed_table");
    }

    @Test
    public void testDuplicateTable() {
        dataTableService.createTable(
            "tempdata",
            List.of(new ColumnSpec("value", ColumnType.STRING), new ColumnSpec("count", ColumnType.INTEGER)),
            STAGING.ordinal());

        dataTableService.duplicateTable("tempdata", "copied_table", STAGING.ordinal());

        List<DataTableInfo> dataTableInfos = dataTableService.listTables(STAGING.ordinal());

        assertThat(dataTableInfos).hasSize(2);

        DataTableInfo copiedTable = dataTableInfos.stream()
            .filter(info -> "copied_table".equals(info.baseName()))
            .findFirst()
            .orElseThrow();

        assertThat(copiedTable.columns()).hasSize(2);
        assertThat(copiedTable.columns())
            .extracting(ColumnSpec::name)
            .containsExactlyInAnyOrder("value", "count");
    }

    @Test
    public void testListTablesFiltersOtherEnvironments() {
        dataTableService.createTable(
            "orders",
            List.of(new ColumnSpec("name", ColumnType.STRING)),
            DEVELOPMENT.ordinal());

        dataTableService.createTable(
            "invoices",
            List.of(new ColumnSpec("amount", ColumnType.NUMBER)),
            STAGING.ordinal());

        List<DataTableInfo> developmentTables = dataTableService.listTables(DEVELOPMENT.ordinal());
        List<DataTableInfo> stagingTables = dataTableService.listTables(STAGING.ordinal());

        assertThat(developmentTables).hasSize(1);
        assertThat(developmentTables.getFirst()
            .baseName()).isEqualTo("orders");

        assertThat(stagingTables).hasSize(1);
        assertThat(stagingTables.getFirst()
            .baseName()).isEqualTo("invoices");
    }

    @Test
    public void testListTablesFiltersByWorkspace() {
        long workspaceId1 = 100L;
        long workspaceId2 = 200L;

        dataTableService.createTable(
            workspaceId1,
            "orders",
            List.of(new ColumnSpec("name", ColumnType.STRING)),
            DEVELOPMENT.ordinal());

        dataTableService.createTable(
            workspaceId2,
            "products",
            List.of(new ColumnSpec("name", ColumnType.STRING)),
            DEVELOPMENT.ordinal());

        List<DataTableInfo> workspace1Tables = dataTableService.listTables(workspaceId1, DEVELOPMENT.ordinal());
        List<DataTableInfo> workspace2Tables = dataTableService.listTables(workspaceId2, DEVELOPMENT.ordinal());

        assertThat(workspace1Tables).hasSize(1);
        assertThat(workspace1Tables.getFirst()
            .baseName()).isEqualTo("orders");

        assertThat(workspace2Tables).hasSize(1);
        assertThat(workspace2Tables.getFirst()
            .baseName()).isEqualTo("products");
    }

    @Test
    public void testGetBaseNameById() {
        dataTableService.createTable(
            "orders",
            List.of(new ColumnSpec("name", ColumnType.STRING)),
            DEVELOPMENT.ordinal());

        long id = dataTableService.getIdByBaseName("orders");
        String baseName = dataTableService.getBaseNameById(id);

        assertThat(baseName).isEqualTo("orders");
    }

    @Test
    public void testGetIdByBaseName() {
        dataTableService.createTable(
            "orders",
            List.of(new ColumnSpec("name", ColumnType.STRING)),
            DEVELOPMENT.ordinal());

        long id = dataTableService.getIdByBaseName("orders");

        assertThat(id).isPositive();
    }

    @Test
    public void testColumnTypes() {
        dataTableService.createTable(
            "all_types",
            List.of(
                new ColumnSpec("string_col", ColumnType.STRING),
                new ColumnSpec("number_col", ColumnType.NUMBER),
                new ColumnSpec("integer_col", ColumnType.INTEGER),
                new ColumnSpec("date_col", ColumnType.DATE),
                new ColumnSpec("datetime_col", ColumnType.DATE_TIME),
                new ColumnSpec("boolean_col", ColumnType.BOOLEAN)),
            DEVELOPMENT.ordinal());

        List<DataTableInfo> dataTableInfos = dataTableService.listTables(DEVELOPMENT.ordinal());

        assertThat(dataTableInfos).hasSize(1);
        assertThat(dataTableInfos.getFirst()
            .columns()).hasSize(6);
    }
}

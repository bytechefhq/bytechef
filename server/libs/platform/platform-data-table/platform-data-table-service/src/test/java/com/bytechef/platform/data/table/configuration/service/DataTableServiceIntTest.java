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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.repository.DataTableRepository;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
class DataTableServiceIntTest {

    private static final long DEV_ENVIRONMENT_ID = 0;
    private static final long STAGE_ENVIRONMENT_ID = 1;

    @Autowired
    private DataTableRepository dataTableRepository;

    @Autowired
    private DataTableRowService dataTableRowService;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void beforeEach() {
        for (String baseName : List.of("registered", "original", "copy", "dropped")) {
            dataTableService.dropTable(baseName, DEV_ENVIRONMENT_ID);
            dataTableService.dropTable(baseName, STAGE_ENVIRONMENT_ID);
        }
    }

    @Test
    void testCreateTableRegistersTheTable() {
        dataTableService.createTable(
            "registered", "a description", List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        assertEquals("registered", dataTableService.getBaseNameById(dataTableService.getIdByBaseName("registered")));

        List<DataTableInfo> dataTableInfos = dataTableService.listTables(DEV_ENVIRONMENT_ID);

        assertTrue(
            dataTableInfos.stream()
                .anyMatch(dataTableInfo -> "registered".equals(dataTableInfo.baseName())),
            "A created table must be visible to listTables, which skips unregistered physical tables");

        DataTableException dataTableException = assertThrowsExactly(
            DataTableException.class, () -> dataTableService.createTable(
                "registered", "a description", List.of(new ColumnSpec("title", ColumnType.STRING)),
                DEV_ENVIRONMENT_ID));

        assertEquals(DataTableErrorType.DATA_TABLE_ALREADY_EXISTS, dataTableException.getErrorType());

        dataTableService.createTable(
            "registered", "a description", List.of(new ColumnSpec("title", ColumnType.STRING)), STAGE_ENVIRONMENT_ID);

        dataTableInfos = dataTableService.listTables(DEV_ENVIRONMENT_ID);

        assertEquals(1, dataTableInfos.size());
    }

    @Test
    void testCreateTableRegistersAMixedCaseNameUnderItsLowercasedForm() {
        dataTableService.createTable(
            "Registered", "a description", List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        List<DataTableInfo> dataTableInfos = dataTableService.listTables(DEV_ENVIRONMENT_ID);

        assertTrue(
            dataTableInfos.stream()
                .anyMatch(dataTableInfo -> "registered".equals(dataTableInfo.baseName())),
            "listTables derives the base name from the lowercased physical table, so the registry must agree");

        assertEquals(dataTableService.getIdByBaseName("Registered"), dataTableService.getIdByBaseName("registered"));
    }

    @Test
    void testDuplicateTableRegistersTheCopy() {
        dataTableService.createTable(
            "original", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        dataTableService.duplicateTable("original", "copy", DEV_ENVIRONMENT_ID);

        assertEquals("copy", dataTableService.getBaseNameById(dataTableService.getIdByBaseName("copy")));
    }

    @Test
    void testDropTableShouldDeleteMetadataWhenNoPhysicalTablesRemain() {
        dataTableService.createTable(
            "dropped", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        dataTableService.dropTable("dropped", DEV_ENVIRONMENT_ID);

        assertFalse(physicalTableExists("dt_0_dropped"));
        assertTrue(
            dataTableRepository.findByName("dropped")
                .isEmpty(),
            "The registry row must go once the last physical table is dropped");
    }

    @Test
    void testDropTableShouldUseTheLowercasedNameForBothLayers() {
        dataTableService.createTable(
            "dropped", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        dataTableService.dropTable("Dropped", DEV_ENVIRONMENT_ID);

        assertFalse(physicalTableExists("dt_0_dropped"));
        assertTrue(
            dataTableRepository.findByName("dropped")
                .isEmpty(),
            "A mixed-case drop must remove the registry row stored under the lowercased name");
    }

    @Test
    void testDropTableShouldPreserveMetadataWhenPhysicalTablesExistInOtherEnvironments() {
        dataTableService.createTable(
            "dropped", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);
        dataTableService.createTable(
            "dropped", null, List.of(new ColumnSpec("title", ColumnType.STRING)), STAGE_ENVIRONMENT_ID);

        dataTableService.dropTable("dropped", DEV_ENVIRONMENT_ID);

        assertFalse(physicalTableExists("dt_0_dropped"));
        assertTrue(physicalTableExists("dt_1_dropped"));
        assertTrue(
            dataTableRepository.findByName("dropped")
                .isPresent(),
            "The registry row must survive while another environment still has the physical table");
    }

    @Test
    void testCreatedTableHidesTheReservedColumns() {
        dataTableService.createTable(
            "registered", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        Optional<DataTableInfo> dataTableInfoOptional = dataTableService.fetchDataTableInfo(
            "registered", DEV_ENVIRONMENT_ID);

        assertTrue(dataTableInfoOptional.isPresent());
        assertEquals(
            List.of("title"),
            dataTableInfoOptional.get()
                .columns()
                .stream()
                .map(ColumnSpec::name)
                .toList());
        assertTrue(physicalColumnExists("registered", "external_id"));
        assertTrue(
            dataTableService.fetchDataTableInfo("missing", DEV_ENVIRONMENT_ID)
                .isEmpty());
    }

    @Test
    void testReservedColumnNamesAreRejected() {
        for (String columnName : List.of("id", "external_id", "EXTERNAL_ID")) {
            DataTableException createTableException = assertThrowsExactly(
                DataTableException.class, () -> dataTableService.createTable(
                    "registered", null, List.of(new ColumnSpec(columnName, ColumnType.STRING)), DEV_ENVIRONMENT_ID));

            assertEquals(DataTableErrorType.COLUMN_NAME_INVALID, createTableException.getErrorType());
        }

        dataTableService.createTable(
            "registered", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        DataTableException addColumnException = assertThrowsExactly(
            DataTableException.class,
            () -> dataTableService.addColumn(
                "registered", new ColumnSpec("external_id", ColumnType.STRING), DEV_ENVIRONMENT_ID));

        assertEquals(DataTableErrorType.COLUMN_NAME_INVALID, addColumnException.getErrorType());
    }

    @Test
    void testColumnChangesReportTypedErrors() {
        dataTableService.createTable(
            "registered", null,
            List.of(new ColumnSpec("title", ColumnType.STRING), new ColumnSpec("note", ColumnType.STRING)),
            DEV_ENVIRONMENT_ID);

        assertEquals(
            DataTableErrorType.COLUMN_ALREADY_EXISTS,
            assertThrowsExactly(
                DataTableException.class,
                () -> dataTableService.addColumn(
                    "registered", new ColumnSpec("title", ColumnType.STRING), DEV_ENVIRONMENT_ID))
                        .getErrorType());
        assertEquals(
            DataTableErrorType.COLUMN_NOT_FOUND,
            assertThrowsExactly(
                DataTableException.class,
                () -> dataTableService.removeColumn("registered", "missing", DEV_ENVIRONMENT_ID))
                    .getErrorType());
        assertEquals(
            DataTableErrorType.COLUMN_NOT_FOUND,
            assertThrowsExactly(
                DataTableException.class,
                () -> dataTableService.renameColumn("registered", "missing", "other", DEV_ENVIRONMENT_ID))
                    .getErrorType());
        assertEquals(
            DataTableErrorType.COLUMN_ALREADY_EXISTS,
            assertThrowsExactly(
                DataTableException.class,
                () -> dataTableService.renameColumn("registered", "title", "note", DEV_ENVIRONMENT_ID))
                    .getErrorType());
    }

    @Test
    void testDuplicateTableCopiesTheExternalIds() {
        dataTableService.createTable(
            "original", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        dataTableRowService.insertRow("original", Map.of("title", "a"), "ORD-1", DEV_ENVIRONMENT_ID);

        dataTableService.duplicateTable("original", "copy", DEV_ENVIRONMENT_ID);

        Optional<DataTableRow> dataTableRowOptional = dataTableRowService.fetchRowByExternalId(
            "copy", "ORD-1", DEV_ENVIRONMENT_ID);

        assertTrue(dataTableRowOptional.isPresent());
        assertEquals(Map.of("title", "a"), dataTableRowOptional.get()
            .values());
    }

    @Test
    void testUpdateDescription() {
        dataTableService.createTable(
            "registered", "before", List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        dataTableService.updateDescription("registered", "after");

        assertEquals(
            "after",
            dataTableService.fetchDataTableInfo("registered", DEV_ENVIRONMENT_ID)
                .map(DataTableInfo::description)
                .orElseThrow());

        dataTableService.updateDescription("registered", null);

        DataTableInfo dataTableInfo = dataTableService.fetchDataTableInfo("registered", DEV_ENVIRONMENT_ID)
            .orElseThrow();

        assertNull(dataTableInfo.description());
        assertEquals(
            DataTableErrorType.DATA_TABLE_NOT_FOUND,
            assertThrowsExactly(
                DataTableException.class, () -> dataTableService.updateDescription("missing", "x"))
                    .getErrorType());
    }

    private boolean physicalColumnExists(String baseName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = current_schema() "
                + "AND table_name LIKE ? AND column_name = ?",
            Integer.class, "%" + baseName + "%", columnName);

        return count != null && count > 0;
    }

    private boolean physicalTableExists(String physicalName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = ?",
            Integer.class, physicalName);

        return count != null && count > 0;
    }
}

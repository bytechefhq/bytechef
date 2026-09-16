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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.domain.ReservedColumns;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class DataTableServiceIntTest {

    private static final long DEV_ENVIRONMENT_ID = 0;
    private static final long STAGE_ENVIRONMENT_ID = 1;

    private static final List<String> BASE_NAMES = List.of(
        "registered", "original", "copy", "shared", "source", "duplicate");

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableRowService dataTableRowService;

    @BeforeEach
    void beforeEach() {
        for (String baseName : BASE_NAMES) {
            dataTableService.dropTable(baseName, DEV_ENVIRONMENT_ID);
            dataTableService.dropTable(baseName, STAGE_ENVIRONMENT_ID);
        }
    }

    @Test
    void testCreateTableRegistersTheTable() {
        createTable("registered", "a description", DEV_ENVIRONMENT_ID);

        assertEquals(
            "registered",
            dataTableService.getBaseNameById(dataTableService.getIdByBaseName("registered")));

        assertTrue(
            listedIn(DEV_ENVIRONMENT_ID, "registered"),
            "A created table must be visible to listTables, which skips unregistered physical tables");

        DataTableException dataTableException = assertThrowsExactly(
            DataTableException.class, () -> createTable("registered", "a description", DEV_ENVIRONMENT_ID),
            "The same name twice in one environment is now a registry decision, not the physical table colliding");

        assertEquals(DataTableErrorType.DATA_TABLE_ALREADY_EXISTS.getErrorKey(), dataTableException.getErrorKey());
    }

    /**
     * The registry row is the LOGICAL table; each environment holds its own physical instance of it. dropTable already
     * treats it that way -- it removes the row only once no physical table for the base name remains in any environment
     * -- so creation has to reuse an existing row rather than insert a second one that uk_data_table_name forbids.
     */
    @Test
    void testTheSameNameCanBeCreatedInASecondEnvironment() {
        createTable("shared", "a description", DEV_ENVIRONMENT_ID);
        createTable("shared", "a description", STAGE_ENVIRONMENT_ID);

        assertTrue(
            listedIn(DEV_ENVIRONMENT_ID, "shared"),
            "the table must remain visible in the environment it was created in");
        assertTrue(listedIn(STAGE_ENVIRONMENT_ID, "shared"), "and be visible in the second environment");

        assertEquals(
            1, countNamed(DEV_ENVIRONMENT_ID, "shared"),
            "reusing the row must not make the table appear twice in the environment that already had it");
    }

    @Test
    void testCreateTableRegistersAMixedCaseNameUnderItsLowercasedForm() {
        createTable("Registered", "a description", DEV_ENVIRONMENT_ID);

        assertTrue(
            listedIn(DEV_ENVIRONMENT_ID, "registered"),
            "listTables derives the base name from the lowercased physical table, so the registry must agree");

        assertEquals(
            dataTableService.getIdByBaseName("Registered"),
            dataTableService.getIdByBaseName("registered"));
    }

    @Test
    void testDuplicateTableRegistersTheCopy() {
        createTable("original", null, DEV_ENVIRONMENT_ID);

        dataTableService.duplicateTable(
            "original", "copy", DEV_ENVIRONMENT_ID);

        assertEquals(
            "copy",
            dataTableService.getBaseNameById(dataTableService.getIdByBaseName("copy")));
    }

    @Test
    void testADuplicatedTableCanStillTakeRows() {
        createTable("source", null, DEV_ENVIRONMENT_ID);

        dataTableService.duplicateTable(
            "source", "duplicate", DEV_ENVIRONMENT_ID);

        // The duplicate is created through the same buildCreateTableSql as the original, so a column declared in one
        // path and forgotten in the other would surface here as a write into a table missing it.
        dataTableRowService.insertRow(
            dataTableRef("duplicate", DEV_ENVIRONMENT_ID), Map.of("title", "a"));

        assertEquals(
            1,
            dataTableRowService
                .listRows(dataTableRef("duplicate", DEV_ENVIRONMENT_ID), 100, 0)
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
        createTable("registered", "a description", DEV_ENVIRONMENT_ID);

        Optional<DataTableInfo> dev = dataTableService.fetchDataTableInfo(
            "registered", DEV_ENVIRONMENT_ID);
        Optional<DataTableInfo> stage = dataTableService.fetchDataTableInfo(
            "registered", STAGE_ENVIRONMENT_ID);

        assertTrue(dev.isPresent());
        assertEquals("a description", dev.get()
            .description());
        assertTrue(dev.get()
            .columns()
            .stream()
            .noneMatch(columnSpec -> ReservedColumns.isReserved(columnSpec.name())));
        assertTrue(stage.isEmpty());
    }

    @Test
    void testUpdateDescriptionWritesTheRegistry() {
        createTable("registered", "before", DEV_ENVIRONMENT_ID);

        dataTableService.updateDescription("registered", "after");

        assertEquals(
            "after",
            dataTableService.fetchDataTableInfo("registered", DEV_ENVIRONMENT_ID)
                .orElseThrow()
                .description());
    }

    @Test
    void testColumnErrorsAreTyped() {
        createTable("registered", null, DEV_ENVIRONMENT_ID);

        assertEquals(
            DataTableErrorType.COLUMN_ALREADY_EXISTS.getErrorKey(),
            assertThrows(DataTableException.class, () -> dataTableService.addColumn(
                "registered", new ColumnSpec("title", ColumnType.STRING), DEV_ENVIRONMENT_ID))
                    .getErrorKey());
        assertEquals(
            DataTableErrorType.COLUMN_NAME_INVALID.getErrorKey(),
            assertThrows(DataTableException.class, () -> dataTableService.addColumn(
                "registered", new ColumnSpec("external_id", ColumnType.STRING), DEV_ENVIRONMENT_ID))
                    .getErrorKey());
        assertEquals(
            DataTableErrorType.COLUMN_NOT_FOUND.getErrorKey(),
            assertThrows(DataTableException.class, () -> dataTableService.removeColumn(
                "registered", "nosuch", DEV_ENVIRONMENT_ID))
                    .getErrorKey());
    }

    private void createTable(String baseName, @Nullable String description, long environmentId) {

        dataTableService.createTable(
            baseName, description, List.of(new ColumnSpec("title", ColumnType.STRING)), environmentId);
    }

    private long countNamed(long environmentId, String baseName) {
        List<DataTableInfo> dataTableInfos = dataTableService.listTables(
            environmentId);

        return dataTableInfos.stream()
            .filter(dataTableInfo -> baseName.equals(dataTableInfo.baseName()))
            .count();
    }

    private boolean listedIn(long environmentId, String baseName) {
        return countNamed(environmentId, baseName) > 0;
    }

    /**
     * These tables are all created shared, so naming the shared physical form here states a fact about the fixture
     * rather than skipping resolution.
     */
    private static DataTableRef dataTableRef(String baseName, long environmentId) {
        return new DataTableRef(baseName, environmentId);
    }
}

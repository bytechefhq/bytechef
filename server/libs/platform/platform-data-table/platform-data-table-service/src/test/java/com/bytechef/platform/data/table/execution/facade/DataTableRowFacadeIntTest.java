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

package com.bytechef.platform.data.table.execution.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.exception.DataTableStorageLimitExceededException;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.platform.data.table.execution.service.DataTableStorageService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
class DataTableRowFacadeIntTest {

    private static final String BASE_NAME = "rows";
    private static final long DEV_ENVIRONMENT_ID = 0;
    private static final String TABLE_NAMES_SQL =
        "SELECT tablename FROM pg_tables WHERE schemaname = current_schema() AND tablename LIKE 'dt\\_%' ESCAPE '\\'";

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private DataTableRowFacade dataTableRowFacade;

    @Autowired
    private DataTableRowService dataTableRowService;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableStorageService dataTableStorageService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long originalMaxSizeBytes;

    @BeforeEach
    void beforeEach() {
        ApplicationProperties.DataTable dataTable = applicationProperties.getDataTable();

        originalMaxSizeBytes = dataTable.getMaxSizeBytes();

        dataTableService.dropTable(BASE_NAME, DEV_ENVIRONMENT_ID);
        dataTableService.createTable(
            BASE_NAME, null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);
    }

    @AfterEach
    void afterEach() {
        setLimit(originalMaxSizeBytes);

        dataTableService.dropTable(BASE_NAME, DEV_ENVIRONMENT_ID);
    }

    @Test
    void testInsertRowIsRejectedPastTheStorageLimit() {
        limitStorageToLiveBytesPlus(1);

        assertThrows(
            DataTableStorageLimitExceededException.class,
            () -> dataTableRowFacade.insertRow(BASE_NAME, Map.of("title", "first"), DEV_ENVIRONMENT_ID));

        assertTrue(listRows().isEmpty());
    }

    @Test
    void testInsertRowChecksTheEstimatedSizeOfTheRow() {
        limitStorageToLiveBytesPlus(100);

        assertThrows(
            DataTableStorageLimitExceededException.class,
            () -> dataTableRowFacade.insertRow(BASE_NAME, Map.of("title", "x".repeat(200)), DEV_ENVIRONMENT_ID));

        DataTableRow dataTableRow = dataTableRowFacade.insertRow(
            BASE_NAME, Map.of("title", "first"), DEV_ENVIRONMENT_ID);

        assertStoredRow(dataTableRow, "first");
        assertEquals(1, listRows().size());
    }

    @Test
    void testUpdateRowIsRejectedPastTheStorageLimit() {
        DataTableRow dataTableRow = dataTableRowFacade.insertRow(
            BASE_NAME, Map.of("title", "first"), DEV_ENVIRONMENT_ID);

        limitStorageToLiveBytesPlus(0);

        assertThrows(
            DataTableStorageLimitExceededException.class,
            () -> dataTableRowFacade.updateRow(
                BASE_NAME, dataTableRow.id(), Map.of("title", "second"), DEV_ENVIRONMENT_ID));

        assertStoredRow(dataTableRow, "first");
    }

    @Test
    void testUpdateRowNearTheStorageLimitChecksOnlyTheGrowthOfTheRow() {
        DataTableRow dataTableRow = dataTableRowFacade.insertRow(
            BASE_NAME, Map.of("title", "x".repeat(100)), DEV_ENVIRONMENT_ID);

        limitStorageToLiveBytesPlus(50);

        DataTableRow updatedDataTableRow = dataTableRowFacade.updateRow(
            BASE_NAME, dataTableRow.id(), Map.of("title", "y".repeat(140)), DEV_ENVIRONMENT_ID);

        assertStoredRow(updatedDataTableRow, "y".repeat(140));
    }

    @Test
    void testUpdateRowGrowingPastTheStorageLimitIsRejected() {
        DataTableRow dataTableRow = dataTableRowFacade.insertRow(
            BASE_NAME, Map.of("title", "x".repeat(100)), DEV_ENVIRONMENT_ID);

        limitStorageToLiveBytesPlus(50);

        assertThrows(
            DataTableStorageLimitExceededException.class,
            () -> dataTableRowFacade.updateRow(
                BASE_NAME, dataTableRow.id(), Map.of("title", "y".repeat(200)), DEV_ENVIRONMENT_ID));

        assertStoredRow(dataTableRow, "x".repeat(100));
    }

    @Test
    void testUpdateRowThatShrinksTheRowPassesAtTheStorageLimit() {
        DataTableRow dataTableRow = dataTableRowFacade.insertRow(
            BASE_NAME, Map.of("title", "x".repeat(250)), DEV_ENVIRONMENT_ID);

        limitStorageToLiveBytesPlus(0);

        DataTableRow updatedDataTableRow = dataTableRowFacade.updateRow(
            BASE_NAME, dataTableRow.id(), Map.of("title", "y"), DEV_ENVIRONMENT_ID);

        assertStoredRow(updatedDataTableRow, "y");
    }

    @Test
    void testImportCsvIsRejectedPastTheStorageLimit() {
        limitStorageToLiveBytesPlus(1);

        assertThrows(
            DataTableStorageLimitExceededException.class,
            () -> dataTableRowFacade.importCsv(BASE_NAME, "title\nfirst\n", DEV_ENVIRONMENT_ID));

        assertTrue(listRows().isEmpty());
    }

    @Test
    void testImportCsvWithinTheStorageLimitImportsTheRows() {
        limitStorageToLiveBytesPlus(1_000);

        dataTableRowFacade.importCsv(BASE_NAME, "title\nfirst\n", DEV_ENVIRONMENT_ID);

        List<DataTableRow> dataTableRows = listRows();

        assertEquals(1, dataTableRows.size());
        assertEquals("first", dataTableRows.getFirst()
            .values()
            .get("title"));
    }

    private void assertStoredRow(DataTableRow dataTableRow, String expectedTitle) {
        assertEquals(expectedTitle, dataTableRow.values()
            .get("title"));

        DataTableRow fetchedDataTableRow = Objects.requireNonNull(
            dataTableRowService.getRow(BASE_NAME, dataTableRow.id(), DEV_ENVIRONMENT_ID));

        assertEquals(expectedTitle, fetchedDataTableRow.values()
            .get("title"));
    }

    private void limitStorageToLiveBytesPlus(long headroomBytes) {
        List<String> physicalTableNames = jdbcTemplate.queryForList(TABLE_NAMES_SQL, String.class);

        long liveBytes = physicalTableNames.stream()
            .mapToLong(physicalTableName -> {
                String[] physicalTableNameParts = physicalTableName.split("_", 3);

                return dataTableStorageService.getTableBytes(
                    physicalTableNameParts[2], Long.parseLong(physicalTableNameParts[1]));
            })
            .sum();

        setLimit(liveBytes + headroomBytes);
    }

    private List<DataTableRow> listRows() {
        return dataTableRowService.listRows(BASE_NAME, 10, 0, DEV_ENVIRONMENT_ID);
    }

    private void setLimit(long maxSizeBytes) {
        ApplicationProperties.DataTable dataTable = applicationProperties.getDataTable();

        dataTable.setMaxSizeBytes(maxSizeBytes);
    }
}

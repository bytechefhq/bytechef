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

package com.bytechef.platform.data.table.execution.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.domain.RowFilter;
import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.NewRow;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class DataTableRowBatchIntTest {

    private static final long ENVIRONMENT_ID = 0;
    private static final DataTableRef REF = new DataTableRef("batched", ENVIRONMENT_ID);

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableRowService dataTableRowService;

    @BeforeEach
    void beforeEach() {
        dataTableService.dropTable("batched", ENVIRONMENT_ID);

        dataTableService.createTable(
            "batched", null,
            List.of(new ColumnSpec("title", ColumnType.STRING), new ColumnSpec("score", ColumnType.INTEGER)),
            ENVIRONMENT_ID);
    }

    @Test
    void testCountRowsHonoursFilters() {
        dataTableRowService.insertRow(REF, Map.of("title", "a", "score", 1));
        dataTableRowService.insertRow(REF, Map.of("title", "b", "score", 5));

        assertEquals(2, dataTableRowService.countRows(REF, List.of()));
        assertEquals(1,
            dataTableRowService.countRows(REF, List.of(new RowFilter("score", RowFilter.Operator.GT, "2"))));
    }

    @Test
    void testInsertRowsInsertsAll() {
        List<DataTableRow> rows = dataTableRowService.insertRows(
            REF, List.of(new NewRow(Map.of("title", "a"), null), new NewRow(Map.of("title", "b"), "k2")),
            CreateStrategy.INSERT);

        assertEquals(2, rows.size());
        assertEquals("k2", rows.get(1)
            .externalId());
        assertEquals(2, dataTableRowService.countRows(REF, List.of()));
    }

    @Test
    void testInsertRowsIsAllOrNothing() {
        dataTableRowService.insertRow(REF, Map.of("title", "taken"), "k1");

        assertThrows(
            DataTableException.class,
            () -> dataTableRowService.insertRows(
                REF, List.of(new NewRow(Map.of("title", "new"), null), new NewRow(Map.of("title", "dup"), "k1")),
                CreateStrategy.INSERT));

        assertEquals(1, dataTableRowService.countRows(REF, List.of()));
    }

    @Test
    void testUpsertStrategyRequiresAKeyOnEveryRow() {
        DataTableException dataTableException = assertThrows(
            DataTableException.class,
            () -> dataTableRowService.insertRows(
                REF, List.of(new NewRow(Map.of("title", "a"), null)), CreateStrategy.UPSERT));

        assertEquals(DataTableErrorType.ROW_EXTERNAL_ID_REQUIRED.getErrorKey(), dataTableException.getErrorKey());
    }

    @Test
    void testUpsertStrategyMergesExistingKeys() {
        dataTableRowService.insertRow(REF, Map.of("title", "old"), "k1");

        dataTableRowService.insertRows(
            REF, List.of(new NewRow(Map.of("title", "new"), "k1"), new NewRow(Map.of("title", "b"), "k2")),
            CreateStrategy.UPSERT);

        assertEquals(2, dataTableRowService.countRows(REF, List.of()));
        assertEquals("new", dataTableRowService.fetchRowByExternalId(REF, "k1")
            .orElseThrow()
            .values()
            .get("title"));
    }

    @Test
    void testDeleteRowsReturnsOnlyWhatItDeleted() {
        DataTableRow first = dataTableRowService.insertRow(REF, Map.of("title", "a"));
        DataTableRow second = dataTableRowService.insertRow(REF, Map.of("title", "b"));

        List<Long> deletedIds = dataTableRowService.deleteRows(REF, List.of(first.id(), second.id(), 999_999L));

        assertEquals(List.of(first.id(), second.id()), deletedIds);
        assertEquals(0, dataTableRowService.countRows(REF, List.of()));
    }

    @Test
    void testClearRowsEmptiesTheTable() {
        dataTableRowService.insertRow(REF, Map.of("title", "a"));
        dataTableRowService.insertRow(REF, Map.of("title", "b"));

        assertEquals(2, dataTableRowService.clearRows(REF));
        assertEquals(0, dataTableRowService.countRows(REF, List.of()));
    }

    /**
     * Also the guard on the header-mapping order: external_id is itself a member of ReservedColumns.ALL, so the
     * EXTERNAL_ID check has to come before the isReserved check. Reversed, every external_id header would map to null
     * and CSV import would stop honouring external ids entirely -- with no error, no warning and the right row count.
     * The fetchRowByExternalId below is what catches that.
     */
    @Test
    void testImportCsvCountsAndHonoursExternalId() {
        int imported = dataTableRowService.importCsv(REF, "title,score,external_id\na,1,k1\nb,2,\n");

        assertEquals(2, imported);
        assertEquals("a", dataTableRowService.fetchRowByExternalId(REF, "k1")
            .orElseThrow()
            .values()
            .get("title"));
    }

    /**
     * The four JSON write paths cap externalId at 255 in RowValuesValidator; a CSV field past that width used to reach
     * the VARCHAR(255) column, raise DataIntegrityViolationException and surface as a 500 where every sibling path
     * answers 400.
     */
    @Test
    void testImportCsvRejectsAnOversizeExternalId() {
        String oversizeExternalId = "k".repeat(256);

        DataTableException dataTableException = assertThrows(
            DataTableException.class,
            () -> dataTableRowService.importCsv(REF, "title,external_id\na," + oversizeExternalId + "\n"));

        assertEquals(DataTableErrorType.CSV_INVALID.getErrorKey(), dataTableException.getErrorKey());
        assertEquals(0, dataTableRowService.countRows(REF, List.of()));
    }

    @Test
    void testImportCsvAcceptsAnExternalIdAtTheCap() {
        String maximumExternalId = "k".repeat(255);

        assertEquals(1, dataTableRowService.importCsv(REF, "title,external_id\na," + maximumExternalId + "\n"));
    }

    @Test
    void testImportCsvRejectsAnUnknownHeader() {
        DataTableException dataTableException = assertThrows(
            DataTableException.class, () -> dataTableRowService.importCsv(REF, "title,nosuch\na,b\n"));

        assertEquals(DataTableErrorType.CSV_INVALID.getErrorKey(), dataTableException.getErrorKey());
    }

    /**
     * The reserved column 'id' must still be silently skipped under the strict header check, exactly as they were
     * before this task made an unrecognised header an error -- neither header may throw, and neither may reach the
     * row's values, while the real column beside them still does.
     */
    @Test
    void testImportCsvStillSkipsReservedHeaders() {
        int imported = dataTableRowService.importCsv(REF, "id,title\n999,a\n");

        assertEquals(1, imported);

        DataTableRow row = dataTableRowService.listRows(REF, 10, 0)
            .getFirst();

        assertEquals("a", row.values()
            .get("title"));
        assertNotEquals(999L, row.id(), "the 'id' header must be ignored, not used as the primary key");
    }
}

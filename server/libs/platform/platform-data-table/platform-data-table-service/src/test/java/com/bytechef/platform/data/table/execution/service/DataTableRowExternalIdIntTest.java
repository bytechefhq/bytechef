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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.ReservedColumns;
import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort;
import com.bytechef.platform.data.table.execution.domain.ExternalIdPatch;
import com.bytechef.platform.data.table.execution.domain.NewRow;
import com.bytechef.platform.data.table.execution.domain.UpsertResult;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
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
class DataTableRowExternalIdIntTest {

    private static final String BASE_NAME = "keyedrows";
    private static final long ENVIRONMENT_ID = 0;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableRowService dataTableRowService;

    @AfterEach
    void afterEach() {
        dataTableService.dropTable(BASE_NAME, ENVIRONMENT_ID);
    }

    @BeforeEach
    void beforeEach() {
        dataTableService.dropTable(BASE_NAME, ENVIRONMENT_ID);

        dataTableService.createTable(
            BASE_NAME, null,
            List.of(new ColumnSpec("title", ColumnType.STRING), new ColumnSpec("amount", ColumnType.INTEGER)),
            ENVIRONMENT_ID);
    }

    @Test
    void testInsertWithAnExternalIdReturnsIt() {
        DataTableRow dataTableRow = dataTableRowService.insertRow(
            BASE_NAME, Map.of("title", "a"), "ORD-1", ENVIRONMENT_ID);

        assertEquals("ORD-1", dataTableRow.externalId());
        assertEquals("a", dataTableRow.values()
            .get("title"));
    }

    @Test
    void testInsertWithoutAnExternalIdLeavesItNull() {
        DataTableRow dataTableRow = dataTableRowService.insertRow(BASE_NAME, Map.of("title", "a"), ENVIRONMENT_ID);

        assertNull(dataTableRow.externalId());
        assertFalse(dataTableRow.values()
            .containsKey(ReservedColumns.EXTERNAL_ID));
    }

    @Test
    void testReadsCarryTheExternalId() {
        DataTableRow inserted = dataTableRowService.insertRow(
            BASE_NAME, Map.of("title", "a"), "ORD-1", ENVIRONMENT_ID);

        DataTableRow read = dataTableRowService.getRow(BASE_NAME, inserted.id(), ENVIRONMENT_ID);

        assertEquals("ORD-1", read.externalId());

        List<DataTableRow> listed = dataTableRowService.listRows(BASE_NAME, 10, 0, ENVIRONMENT_ID);

        assertEquals("ORD-1", listed.getFirst()
            .externalId());
    }

    @Test
    void testListTablesHidesTheReservedColumn() {
        Optional<DataTableInfo> dataTableInfo = dataTableService.fetchDataTableInfo(BASE_NAME, ENVIRONMENT_ID);

        assertTrue(dataTableInfo.isPresent());
        assertTrue(dataTableInfo.get()
            .columns()
            .stream()
            .noneMatch(columnSpec -> ReservedColumns.isReserved(columnSpec.name())));
    }

    @Test
    void testFetchByExternalId() {
        dataTableRowService.insertRow(BASE_NAME, Map.of("title", "a"), "ORD-1", ENVIRONMENT_ID);

        Optional<DataTableRow> found = dataTableRowService.fetchRowByExternalId(BASE_NAME, "ORD-1", ENVIRONMENT_ID);

        assertTrue(found.isPresent());
        assertEquals("a", found.get()
            .values()
            .get("title"));
        assertTrue(dataTableRowService.fetchRowByExternalId(BASE_NAME, "missing", ENVIRONMENT_ID)
            .isEmpty());
    }

    @Test
    void testFilteringOnExternalIdReturnsTheKeyedRow() {
        dataTableRowService.insertRow(BASE_NAME, Map.of("title", "a"), "ORD-1", ENVIRONMENT_ID);
        dataTableRowService.insertRow(BASE_NAME, Map.of("title", "b"), "ORD-2", ENVIRONMENT_ID);

        DataTableRowConditions conditions = DataTableRowConditions.of(
            List.of(new DataTableRowFilter(ReservedColumns.EXTERNAL_ID, DataTableRowFilter.Operation.EQUALS, "ORD-1")));

        List<DataTableRow> rows = dataTableRowService.queryRows(
            BASE_NAME, conditions, List.of(), 10, 0, ENVIRONMENT_ID);

        assertEquals(1, rows.size());
        assertEquals("ORD-1", rows.getFirst()
            .externalId());
        assertEquals(1, dataTableRowService.countRows(BASE_NAME, conditions, ENVIRONMENT_ID));
    }

    @Test
    void testQueryRowsAppliesEverySortInOrder() {
        dataTableRowService.insertRow(BASE_NAME, Map.of("title", "b", "amount", 1), ENVIRONMENT_ID);
        dataTableRowService.insertRow(BASE_NAME, Map.of("title", "a", "amount", 2), ENVIRONMENT_ID);
        dataTableRowService.insertRow(BASE_NAME, Map.of("title", "a", "amount", 1), ENVIRONMENT_ID);

        List<DataTableRow> rows = dataTableRowService.queryRows(
            BASE_NAME, DataTableRowConditions.matchAll(),
            List.of(
                new DataTableRowSort("title", DataTableRowSort.Direction.ASC),
                new DataTableRowSort("amount", DataTableRowSort.Direction.DESC)),
            10, 0, ENVIRONMENT_ID);

        List<String> keys = rows.stream()
            .map(row -> row.values()
                .get("title") + "" +
                row.values()
                    .get("amount"))
            .toList();

        assertEquals(List.of("a2", "a1", "b1"), keys);
        assertEquals(3, dataTableRowService.countRows(BASE_NAME, DataTableRowConditions.matchAll(), ENVIRONMENT_ID));
    }

    @Test
    void testDuplicateExternalIdOnInsertIsATypedConflict() {
        dataTableRowService.insertRow(BASE_NAME, Map.of("title", "a"), "ORD-1", ENVIRONMENT_ID);

        DataTableException dataTableException = assertThrows(
            DataTableException.class,
            () -> dataTableRowService.insertRow(BASE_NAME, Map.of("title", "b"), "ORD-1", ENVIRONMENT_ID));

        assertEquals(DataTableErrorType.ROW_EXTERNAL_ID_CONFLICT.getErrorKey(), dataTableException.getErrorKey());
    }

    @Test
    void testUpdateCanSetAndClearTheExternalId() {
        DataTableRow inserted = dataTableRowService.insertRow(BASE_NAME, Map.of("title", "a"), ENVIRONMENT_ID);

        DataTableRow keyed = dataTableRowService.updateRow(
            BASE_NAME, inserted.id(), Map.of(), new ExternalIdPatch("ORD-9"), ENVIRONMENT_ID);

        assertEquals("ORD-9", keyed.externalId());

        DataTableRow untouched = dataTableRowService.updateRow(
            BASE_NAME, inserted.id(), Map.of("title", "b"), null, ENVIRONMENT_ID);

        assertEquals("ORD-9", untouched.externalId());
        assertEquals("b", untouched.values()
            .get("title"));

        DataTableRow cleared = dataTableRowService.updateRow(
            BASE_NAME, inserted.id(), Map.of(), new ExternalIdPatch(null), ENVIRONMENT_ID);

        assertNull(cleared.externalId());
    }

    @Test
    void testUpsertCreatesThenMerges() {
        UpsertResult first = dataTableRowService.upsertRow(
            BASE_NAME, "ORD-1", Map.of("title", "a", "amount", 1), ENVIRONMENT_ID);

        assertTrue(first.created());
        assertEquals("ORD-1", first.row()
            .externalId());

        UpsertResult second = dataTableRowService.upsertRow(
            BASE_NAME, "ORD-1", Map.of("title", "b"), ENVIRONMENT_ID);

        assertFalse(second.created());
        assertEquals(first.row()
            .id(),
            second.row()
                .id());
        assertEquals("b", second.row()
            .values()
            .get("title"));
        assertEquals(1L, ((Number) second.row()
            .values()
            .get("amount")).longValue());
        assertEquals(1, dataTableRowService.listRows(BASE_NAME, 10, 0, ENVIRONMENT_ID)
            .size());
    }

    @Test
    void testUpsertWithNoValuesReturnsTheExistingRowUnchanged() {
        dataTableRowService.upsertRow(BASE_NAME, "ORD-1", Map.of("title", "a"), ENVIRONMENT_ID);

        UpsertResult result = dataTableRowService.upsertRow(BASE_NAME, "ORD-1", Map.of(), ENVIRONMENT_ID);

        assertFalse(result.created());
        assertEquals("a", result.row()
            .values()
            .get("title"));
    }

    @Test
    void testBatchUpsertRequiresAnExternalIdOnEveryRow() {
        Map<String, Object> values = new HashMap<>();

        values.put("title", "a");

        DataTableException dataTableException = assertThrows(
            DataTableException.class,
            () -> dataTableRowService.insertRows(
                BASE_NAME, List.of(new NewRow(values, "ORD-1"), new NewRow(values, null)), CreateStrategy.UPSERT,
                ENVIRONMENT_ID));

        assertEquals(DataTableErrorType.ROW_EXTERNAL_ID_REQUIRED.getErrorKey(), dataTableException.getErrorKey());
        assertEquals(0, dataTableRowService.countRows(BASE_NAME, DataTableRowConditions.matchAll(), ENVIRONMENT_ID));
    }

    @Test
    void testBatchUpsertCreatesAndUpdates() {
        dataTableRowService.insertRow(BASE_NAME, Map.of("title", "old"), "ORD-1", ENVIRONMENT_ID);

        List<DataTableRow> rows = dataTableRowService.insertRows(
            BASE_NAME,
            List.of(new NewRow(Map.of("title", "new"), "ORD-1"), new NewRow(Map.of("title", "other"), "ORD-2")),
            CreateStrategy.UPSERT, ENVIRONMENT_ID);

        assertEquals(2, rows.size());
        assertEquals("new", rows.getFirst()
            .values()
            .get("title"));
        assertEquals(2, dataTableRowService.countRows(BASE_NAME, DataTableRowConditions.matchAll(), ENVIRONMENT_ID));
    }

    @Test
    void testCsvRoundTripsTheExternalId() {
        dataTableRowService.insertRow(BASE_NAME, Map.of("title", "a"), "ORD-1", ENVIRONMENT_ID);

        String csv = dataTableRowService.exportCsv(BASE_NAME, ENVIRONMENT_ID);

        assertTrue(csv.startsWith(ReservedColumns.EXTERNAL_ID + ","));

        dataTableRowService.deleteAllRows(BASE_NAME, ENVIRONMENT_ID);

        assertEquals(1, dataTableRowService.importCsv(BASE_NAME, csv, ENVIRONMENT_ID));
        assertTrue(dataTableRowService.fetchRowByExternalId(BASE_NAME, "ORD-1", ENVIRONMENT_ID)
            .isPresent());
    }
}

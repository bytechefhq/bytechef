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
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.domain.ReservedColumns;
import com.bytechef.platform.data.table.domain.RowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.ExternalIdPatch;
import com.bytechef.platform.data.table.execution.domain.UpsertResult;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class DataTableRowExternalIdIntTest {

    private static final long ENVIRONMENT_ID = 0;
    private static final long WORKSPACE_ID = 1L;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableRowService dataTableRowService;

    private DataTableRef dataTableRef;

    @BeforeEach
    void beforeEach() {
        dataTableService.fetchDataTable(WORKSPACE_ID, "keyedrows")
            .ifPresent(dataTable -> dataTableService.dropTable(dataTable.getId(), ENVIRONMENT_ID));

        long dataTableId = dataTableService.createTable(
            WORKSPACE_ID, "keyedrows", null, List.of(new ColumnSpec("title", ColumnType.STRING)), ENVIRONMENT_ID);

        dataTableRef = new DataTableRef(dataTableId, ENVIRONMENT_ID);
    }

    @Test
    void testInsertWithAnExternalIdReturnsIt() {
        DataTableRow dataTableRow = dataTableRowService.insertRow(dataTableRef, Map.of("title", "a"), "ORD-1");

        assertEquals("ORD-1", dataTableRow.externalId());
        assertEquals("a", dataTableRow.values()
            .get("title"));
    }

    @Test
    void testInsertWithoutAnExternalIdLeavesItNull() {
        DataTableRow dataTableRow = dataTableRowService.insertRow(dataTableRef, Map.of("title", "a"));

        assertNull(dataTableRow.externalId());
    }

    @Test
    void testReadsCarryTheExternalId() {
        DataTableRow inserted = dataTableRowService.insertRow(dataTableRef, Map.of("title", "a"), "ORD-1");

        DataTableRow read = dataTableRowService.getRow(dataTableRef, inserted.id());

        assertEquals("ORD-1", read.externalId());

        List<DataTableRow> listed = dataTableRowService.listRows(dataTableRef, 10, 0);

        assertEquals("ORD-1", listed.getFirst()
            .externalId());
    }

    @Test
    void testFetchByExternalId() {
        dataTableRowService.insertRow(dataTableRef, Map.of("title", "a"), "ORD-1");

        Optional<DataTableRow> found = dataTableRowService.fetchRowByExternalId(dataTableRef, "ORD-1");

        assertTrue(found.isPresent());
        assertEquals("a", found.get()
            .values()
            .get("title"));
        assertTrue(dataTableRowService.fetchRowByExternalId(dataTableRef, "missing")
            .isEmpty());
    }

    @Test
    void testFilteringOnExternalIdReturnsTheKeyedRow() {
        dataTableRowService.insertRow(dataTableRef, Map.of("title", "a"), "ORD-1");
        dataTableRowService.insertRow(dataTableRef, Map.of("title", "b"), "ORD-2");

        List<DataTableRow> rows = dataTableRowService.listRows(
            dataTableRef, 10, 0, List.of(new RowFilter(ReservedColumns.EXTERNAL_ID, RowFilter.Operator.EQ, "ORD-1")),
            List.of());

        assertEquals(1, rows.size());
        assertEquals("ORD-1", rows.getFirst()
            .externalId());
        assertEquals("a", rows.getFirst()
            .values()
            .get("title"));
    }

    @Test
    void testDuplicateExternalIdOnInsertIsATypedConflict() {
        dataTableRowService.insertRow(dataTableRef, Map.of("title", "a"), "ORD-1");

        DataTableException dataTableException = assertThrows(
            DataTableException.class, () -> dataTableRowService.insertRow(dataTableRef, Map.of("title", "b"), "ORD-1"));

        assertEquals(DataTableErrorType.ROW_EXTERNAL_ID_CONFLICT.getErrorKey(), dataTableException.getErrorKey());
    }

    @Test
    void testUpdateCanSetAndClearTheExternalId() {
        DataTableRow inserted = dataTableRowService.insertRow(dataTableRef, Map.of("title", "a"));

        DataTableRow keyed = dataTableRowService.updateRow(
            dataTableRef, inserted.id(), Map.of(), new ExternalIdPatch("ORD-9"));

        assertEquals("ORD-9", keyed.externalId());

        DataTableRow untouched = dataTableRowService.updateRow(dataTableRef, inserted.id(), Map.of("title", "b"), null);

        assertEquals("ORD-9", untouched.externalId());
        assertEquals("b", untouched.values()
            .get("title"));

        DataTableRow cleared = dataTableRowService.updateRow(
            dataTableRef, inserted.id(), Map.of(), new ExternalIdPatch(null));

        assertNull(cleared.externalId());
    }

    @Test
    void testUpsertCreatesThenMerges() {
        UpsertResult first = dataTableRowService.upsertRow(dataTableRef, "ORD-1", Map.of("title", "a"));

        assertTrue(first.created());
        assertEquals("ORD-1", first.row()
            .externalId());

        UpsertResult second = dataTableRowService.upsertRow(dataTableRef, "ORD-1", Map.of("title", "b"));

        assertFalse(second.created());
        assertEquals(first.row()
            .id(),
            second.row()
                .id());
        assertEquals("b", second.row()
            .values()
            .get("title"));
        assertEquals(1, dataTableRowService.listRows(dataTableRef, 10, 0)
            .size());
    }

    @Test
    void testUpsertWithNoValuesReturnsTheExistingRowUnchanged() {
        dataTableRowService.upsertRow(dataTableRef, "ORD-1", Map.of("title", "a"));

        UpsertResult result = dataTableRowService.upsertRow(dataTableRef, "ORD-1", Map.of());

        assertFalse(result.created());
        assertEquals("a", result.row()
            .values()
            .get("title"));
    }
}

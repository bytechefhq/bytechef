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

package com.bytechef.component.datatable.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.domain.RowFilter;
import com.bytechef.platform.data.table.domain.RowSort;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableFindRecordsActionTest extends AbstractDataTableActionTest {

    @Test
    void testNoFilterEntriesProduceNoFilters() {
        assertTrue(DataTableFindRecordsAction.toRowFilters(List.of())
            .isEmpty());
    }

    @Test
    void testAnEntryBecomesARowFilter() {
        List<RowFilter> rowFilters = DataTableFindRecordsAction.toRowFilters(
            List.of(Map.of("field", "title", "operator", "EQ", "value", "alpha")));

        RowFilter rowFilter = rowFilters.getFirst();

        assertEquals("title", rowFilter.field());
        assertEquals(RowFilter.Operator.EQ, rowFilter.operator());
        assertEquals("alpha", rowFilter.value());
    }

    @Test
    void testInSplitsItsValueOnCommas() {
        List<RowFilter> rowFilters = DataTableFindRecordsAction.toRowFilters(
            List.of(Map.of("field", "score", "operator", "IN", "value", "1, 2,3")));

        RowFilter rowFilter = rowFilters.getFirst();

        assertEquals(List.of("1", "2", "3"), rowFilter.value());
    }

    @Test
    void testBetweenSplitsItsValueOnCommas() {
        List<RowFilter> rowFilters = DataTableFindRecordsAction.toRowFilters(
            List.of(Map.of("field", "score", "operator", "BETWEEN", "value", "1,9")));

        RowFilter rowFilter = rowFilters.getFirst();

        assertEquals(List.of("1", "9"), rowFilter.value());
    }

    @Test
    void testAnEntryWithNoFieldIsSkipped() {
        assertTrue(
            DataTableFindRecordsAction.toRowFilters(List.of(Map.of("operator", "EQ", "value", "alpha")))
                .isEmpty());
    }

    @Test
    void testABlankFieldIsSkipped() {
        assertTrue(
            DataTableFindRecordsAction.toRowFilters(List.of(Map.of("field", "  ", "value", "alpha")))
                .isEmpty());
    }

    @Test
    void testAnEntryWithNoOperatorDefaultsToEquals() {
        List<RowFilter> rowFilters = DataTableFindRecordsAction.toRowFilters(
            List.of(Map.of("field", "title", "value", "alpha")));

        RowFilter rowFilter = rowFilters.getFirst();

        assertEquals(RowFilter.Operator.EQ, rowFilter.operator());
    }

    @Test
    void testNoSortEntriesProduceNoSorts() {
        assertTrue(DataTableFindRecordsAction.toRowSorts(List.of())
            .isEmpty());
    }

    @Test
    void testASortEntryBecomesARowSort() {
        List<RowSort> rowSorts = DataTableFindRecordsAction.toRowSorts(
            List.of(Map.of("field", "id", "direction", "DESC")));

        RowSort rowSort = rowSorts.getFirst();

        assertEquals("id", rowSort.field());
        assertEquals(RowSort.Direction.DESC, rowSort.direction());
    }

    @Test
    void testASortEntryWithNoDirectionDefaultsToAscending() {
        List<RowSort> rowSorts = DataTableFindRecordsAction.toRowSorts(
            List.of(Map.of("field", "score")));

        RowSort rowSort = rowSorts.getFirst();

        assertEquals(RowSort.Direction.ASC, rowSort.direction());
    }

    @Test
    void testASortEntryWithNoColumnIsSkipped() {
        assertTrue(
            DataTableFindRecordsAction.toRowSorts(List.of(Map.of("direction", "DESC")))
                .isEmpty());
    }

    @Test
    void testAMissingValueStaysNullSoItReadsAsIsNull() {
        List<RowFilter> rowFilters = DataTableFindRecordsAction.toRowFilters(
            List.of(Map.of("field", "title", "operator", "EQ")));

        RowFilter rowFilter = rowFilters.getFirst();

        assertEquals(null, rowFilter.value());
    }

    @Test
    void testPerformEmitsFlatRows() throws Exception {
        DataTableRef dataTableRef = stubResolvedDataTable();

        when(dataTableRowService.listRows(eq(dataTableRef), anyInt(), anyInt(), anyList(), anyList()))
            .thenReturn(
                List.of(
                    new DataTableRow(1, Map.of("status", "BOT")), new DataTableRow(2, Map.of("status", "CLOSED"))));

        ModifiableActionDefinition actionDefinition = DataTableFindRecordsAction.of(
            dataTableService, dataTableRowService);

        assertEquals(
            List.of(Map.of("id", 1L, "status", "BOT"), Map.of("id", 2L, "status", "CLOSED")),
            perform(actionDefinition, Map.of("table", BASE_NAME)));
    }
}

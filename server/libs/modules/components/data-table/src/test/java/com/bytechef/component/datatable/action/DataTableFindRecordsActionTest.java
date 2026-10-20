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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter.Operation;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort.Direction;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
class DataTableFindRecordsActionTest extends AbstractDataTableActionTest {

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<List<List<DataTableRowFilter>>> conditionsArgumentCaptor = ArgumentCaptor.forClass(
        List.class);
    private final ArgumentCaptor<DataTableRowSort> sortArgumentCaptor = ArgumentCaptor.forClass(
        DataTableRowSort.class);

    @Test
    void testPerformEmitsFlatRows() throws Exception {
        when(dataTableRowService.findRows(anyString(), anyList(), any(), anyInt(), anyInt(), anyLong()))
            .thenReturn(
                List.of(
                    new DataTableRow(1, Map.of("status", "BOT")), new DataTableRow(2, Map.of("status", "CLOSED"))));

        ModifiableActionDefinition actionDefinition = DataTableFindRecordsAction.of(
            dataTableService, dataTableRowService);

        assertEquals(
            List.of(Map.of("id", 1L, "status", "BOT"), Map.of("id", 2L, "status", "CLOSED")),
            perform(actionDefinition, Map.of("table", "conversations")));

        verify(dataTableRowService).findRows(
            eq("conversations"), conditionsArgumentCaptor.capture(), sortArgumentCaptor.capture(), eq(100), eq(0),
            eq(1L));

        assertEquals(List.of(), conditionsArgumentCaptor.getValue());
        assertNull(sortArgumentCaptor.getValue());
    }

    @Test
    void testPerformPassesConditionsSortAndPagination() throws Exception {
        ModifiableActionDefinition actionDefinition = DataTableFindRecordsAction.of(
            dataTableService, dataTableRowService);

        perform(
            actionDefinition,
            Map.of(
                "table", "conversations",
                "conditions", List.of(
                    List.of(
                        Map.of("column", "status", "value", "BOT"),
                        Map.of("column", "title", "operation", "CONTAINS", "value", "help")),
                    List.of(
                        Map.of("column", "priority", "operation", "IN", "values", List.of("1", "2")),
                        Map.of("column", "score", "operation", "BETWEEN", "value", "1", "valueTo", "5"),
                        Map.of("column", "closedAt", "operation", "EMPTY"))),
                "sortColumn", "score",
                "sortDirection", "DESC",
                "limit", 10,
                "offset", 20));

        verify(dataTableRowService).findRows(
            eq("conversations"), conditionsArgumentCaptor.capture(), sortArgumentCaptor.capture(), eq(10), eq(20),
            eq(1L));

        assertEquals(
            List.of(
                List.of(
                    new DataTableRowFilter("status", Operation.EQUALS, "BOT"),
                    new DataTableRowFilter("title", Operation.CONTAINS, "help")),
                List.of(
                    new DataTableRowFilter("priority", Operation.IN, List.of("1", "2")),
                    new DataTableRowFilter("score", Operation.BETWEEN, Arrays.asList("1", "5")),
                    new DataTableRowFilter("closedAt", Operation.EMPTY, null))),
            conditionsArgumentCaptor.getValue());
        assertEquals(new DataTableRowSort("score", Direction.DESC), sortArgumentCaptor.getValue());
    }
}

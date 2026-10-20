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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter.Operation;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableDeleteRecordsActionTest extends AbstractDataTableActionTest {

    private final ModifiableActionDefinition actionDefinition = DataTableDeleteRecordsAction.of(
        dataTableService, dataTableRowService);

    @Test
    void testPerformDeletesByRecordIdsByDefault() throws Exception {
        when(dataTableRowService.deleteRow("conversations", 1L, 1L))
            .thenReturn(true);
        when(dataTableRowService.deleteRow("conversations", 2L, 1L))
            .thenReturn(false);

        assertEquals(
            Map.of("deletedCount", 1, "deletedIds", List.of(1L)),
            perform(actionDefinition, Map.of("table", "conversations", "ids", List.of(1, "2"))));

        verify(dataTableRowService, never()).findRows(any(), any(), any(), anyInt(), anyInt(), anyLong());
    }

    @Test
    void testPerformDeletesRecordsMatchingConditions() throws Exception {
        List<List<DataTableRowFilter>> conditions = List.of(
            List.of(new DataTableRowFilter("status", Operation.EQUALS, "CLOSED")));

        when(dataTableRowService.findRows("conversations", conditions, null, Integer.MAX_VALUE, 0, 1L))
            .thenReturn(List.of(new DataTableRow(3, Map.of()), new DataTableRow(4, Map.of())));
        when(dataTableRowService.deleteRow(eq("conversations"), anyLong(), eq(1L)))
            .thenReturn(true);

        assertEquals(
            Map.of("deletedCount", 2, "deletedIds", List.of(3L, 4L)),
            perform(
                actionDefinition,
                Map.of(
                    "table", "conversations",
                    "deleteBy", "CONDITIONS",
                    "conditions", List.of(List.of(Map.of("column", "status", "value", "CLOSED"))))));

        verify(dataTableRowService).deleteRow("conversations", 3L, 1L);
        verify(dataTableRowService).deleteRow("conversations", 4L, 1L);
    }

    @Test
    void testPerformRejectsDeleteByConditionsWithoutCondition() {
        assertThrows(
            IllegalArgumentException.class,
            () -> perform(
                actionDefinition,
                Map.of("table", "conversations", "deleteBy", "CONDITIONS", "conditions", List.of(List.of()))));

        verifyNoInteractions(dataTableRowService);
    }
}

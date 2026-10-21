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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableCreateRecordsActionTest extends AbstractDataTableActionTest {

    @Test
    void testPerformInsertsEveryRecordInOneStepAndEmitsFlatRows() throws Exception {
        List<Map<String, Object>> records = List.of(Map.of("status", "BOT"), Map.of("status", "HUMAN"));

        when(dataTableRowService.insertRows("conversations", records, 1L))
            .thenReturn(
                List.of(new DataTableRow(1, Map.of("status", "BOT")), new DataTableRow(2, Map.of("status", "HUMAN"))));

        ModifiableActionDefinition actionDefinition = DataTableCreateRecordsAction.of(
            dataTableService, dataTableRowService);

        assertEquals(
            List.of(Map.of("id", 1L, "status", "BOT"), Map.of("id", 2L, "status", "HUMAN")),
            perform(actionDefinition, Map.of("table", "conversations", "records", Map.of("values", records))));

        verify(dataTableRowService, times(1)).insertRows("conversations", records, 1L);
        verify(dataTableRowService, never()).insertRow(any(), any(), anyLong());
    }

    @Test
    void testPerformRejectsARecordThatIsNotAnObject() {
        ModifiableActionDefinition actionDefinition = DataTableCreateRecordsAction.of(
            dataTableService, dataTableRowService);

        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class,
            () -> perform(
                actionDefinition,
                Map.of(
                    "table", "conversations", "records",
                    Map.of("values", List.of(Map.of("status", "BOT"), "status=HUMAN")))));

        assertEquals(
            "Record 2 must be an object of column values; no record was inserted",
            illegalArgumentException.getMessage());

        verify(dataTableRowService, never()).insertRows(any(), anyList(), anyLong());
    }

    @Test
    void testPerformRejectsRecordsThatAreNotAList() {
        ModifiableActionDefinition actionDefinition = DataTableCreateRecordsAction.of(
            dataTableService, dataTableRowService);

        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class,
            () -> perform(
                actionDefinition,
                Map.of("table", "conversations", "records", Map.of("values", "[{\"status\": \"BOT\"}]"))));

        assertEquals(
            "Records must be a list of records; no record was inserted", illegalArgumentException.getMessage());

        verify(dataTableRowService, never()).insertRows(any(), anyList(), anyLong());
    }

    @Test
    void testPerformRejectsMissingRecords() {
        ModifiableActionDefinition actionDefinition = DataTableCreateRecordsAction.of(
            dataTableService, dataTableRowService);

        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class,
            () -> perform(actionDefinition, Map.of("table", "conversations", "records", Map.of())));

        assertEquals(
            "Records must be a list of records; no record was inserted", illegalArgumentException.getMessage());

        verify(dataTableRowService, never()).insertRows(any(), anyList(), anyLong());
    }
}

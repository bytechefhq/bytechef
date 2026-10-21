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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter.Operation;
import java.util.HashMap;
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
    void testPerformDeletesByRecordIdsByDefaultInOneStatement() throws Exception {
        when(dataTableRowService.deleteRows("conversations", idsCondition(1L, 2L), 1L))
            .thenReturn(List.of(new DataTableRow(1, Map.of())));

        assertEquals(
            Map.of("deletedCount", 1, "deletedIds", List.of(1L)),
            perform(actionDefinition, Map.of("table", "conversations", "ids", List.of(1, "2", 1.0))));

        verify(dataTableRowService, never()).deleteRow(any(), anyLong(), anyLong());
    }

    @Test
    void testPerformWithoutRecordIdsDeletesNothing() throws Exception {
        assertEquals(
            Map.of("deletedCount", 0, "deletedIds", List.of()),
            perform(actionDefinition, Map.of("table", "conversations", "ids", List.of())));

        verifyNoInteractions(dataTableRowService);
    }

    @Test
    void testPerformDeletesRecordsMatchingConditions() throws Exception {
        DataTableRowConditions conditions = DataTableRowConditions.of(
            List.of(new DataTableRowFilter("status", Operation.EQUALS, "CLOSED")));

        when(dataTableRowService.deleteRows("conversations", conditions, 1L))
            .thenReturn(List.of(new DataTableRow(3, Map.of()), new DataTableRow(4, Map.of())));

        assertEquals(
            Map.of("deletedCount", 2, "deletedIds", List.of(3L, 4L)),
            perform(
                actionDefinition,
                Map.of(
                    "table", "conversations",
                    "deleteBy", "CONDITIONS",
                    "conditions", List.of(List.of(Map.of("column", "status", "value", "CLOSED"))))));

        verify(dataTableRowService, never()).deleteRow(any(), anyLong(), anyLong());
    }

    @Test
    void testPerformDeletesByConditionsWhenOnlyConditionsAreGiven() throws Exception {
        DataTableRowConditions conditions = DataTableRowConditions.of(
            List.of(new DataTableRowFilter("status", Operation.EQUALS, "CLOSED")));

        when(dataTableRowService.deleteRows("conversations", conditions, 1L))
            .thenReturn(List.of(new DataTableRow(3, Map.of())));

        Map<String, Object> conditionsParameter = Map.of(
            "conditions", List.of(List.of(Map.of("column", "status", "value", "CLOSED"))));

        assertEquals(
            Map.of("deletedCount", 1, "deletedIds", List.of(3L)),
            perform(actionDefinition, merge(Map.of("table", "conversations"), conditionsParameter)));
        assertEquals(
            Map.of("deletedCount", 1, "deletedIds", List.of(3L)),
            perform(
                actionDefinition,
                merge(Map.of("table", "conversations", "deleteBy", "conditions"), conditionsParameter)));
    }

    @Test
    void testPerformDeletesByConditionsWhenDeleteByIsConditionsAndRecordIdsAreAlsoGiven() throws Exception {
        DataTableRowConditions conditions = DataTableRowConditions.of(
            List.of(new DataTableRowFilter("status", Operation.EQUALS, "CLOSED")));

        when(dataTableRowService.deleteRows("conversations", conditions, 1L))
            .thenReturn(List.of(new DataTableRow(3, Map.of())));

        assertEquals(
            Map.of("deletedCount", 1, "deletedIds", List.of(3L)),
            perform(
                actionDefinition,
                Map.of(
                    "table", "conversations",
                    "deleteBy", "CONDITIONS",
                    "ids", List.of(7),
                    "conditions", List.of(List.of(Map.of("column", "status", "value", "CLOSED"))))));

        verify(dataTableRowService, never()).deleteRows("conversations", idsCondition(7L), 1L);
    }

    @Test
    void testPerformDeletesByRecordIdsWhenDeleteByIsIdsAndConditionsAreAlsoGiven() throws Exception {
        when(dataTableRowService.deleteRows("conversations", idsCondition(7L), 1L))
            .thenReturn(List.of(new DataTableRow(7, Map.of())));

        assertEquals(
            Map.of("deletedCount", 1, "deletedIds", List.of(7L)),
            perform(
                actionDefinition,
                Map.of(
                    "table", "conversations",
                    "deleteBy", "IDS",
                    "ids", List.of(7),
                    "conditions", List.of(List.of(Map.of("column", "status", "value", "CLOSED"))))));

        verify(dataTableRowService, never()).deleteRow(any(), anyLong(), anyLong());
    }

    @Test
    void testPerformDeletesByRecordIdsWhenConditionsAreEmptyWithoutDeleteBy() throws Exception {
        when(dataTableRowService.deleteRows("conversations", idsCondition(7L), 1L))
            .thenReturn(List.of(new DataTableRow(7, Map.of())));

        Map<String, Object> idsParameters = Map.of("table", "conversations", "ids", List.of(7));
        Map<String, Object> nullConditions = new HashMap<>();

        nullConditions.put("conditions", null);

        assertEquals(
            Map.of("deletedCount", 1, "deletedIds", List.of(7L)),
            perform(actionDefinition, merge(idsParameters, Map.of("conditions", List.of()))));
        assertEquals(
            Map.of("deletedCount", 1, "deletedIds", List.of(7L)),
            perform(actionDefinition, merge(idsParameters, nullConditions)));
    }

    @Test
    void testPerformRejectsRecordIdsThatAreNotNumbers() {
        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class,
            () -> perform(actionDefinition, Map.of("table", "conversations", "ids", List.of("abc"))));

        assertEquals("Record ID 'abc' is not a number", illegalArgumentException.getMessage());

        verifyNoInteractions(dataTableRowService);
    }

    @Test
    void testPerformRejectsRecordIdsThatAreNotWholeNumbers() {
        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class,
            () -> perform(actionDefinition, Map.of("table", "conversations", "ids", List.of(3.9))));

        assertEquals("Record ID '3.9' is not a whole number", illegalArgumentException.getMessage());

        verifyNoInteractions(dataTableRowService);
    }

    @Test
    void testPerformRejectsRecordIdsOutOfRange() {
        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class,
            () -> perform(actionDefinition, Map.of("table", "conversations", "ids", List.of("1e30"))));

        assertEquals("Record ID '1e30' is out of range", illegalArgumentException.getMessage());

        verifyNoInteractions(dataTableRowService);
    }

    @Test
    void testPerformRejectsUnknownDeleteBy() {
        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class,
            () -> perform(actionDefinition, Map.of("table", "conversations", "deleteBy", "FILTER")));

        assertEquals(
            "Unknown Delete By 'FILTER'; expected one of IDS, CONDITIONS", illegalArgumentException.getMessage());

        verifyNoInteractions(dataTableRowService);
    }

    @Test
    void testPerformRejectsDeleteByConditionsWithoutCondition() {
        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class,
            () -> perform(
                actionDefinition,
                Map.of("table", "conversations", "deleteBy", "CONDITIONS", "conditions", List.of())));

        assertEquals(
            "Delete by conditions requires at least one condition; use Clear Table to delete every record",
            illegalArgumentException.getMessage());

        verifyNoInteractions(dataTableRowService);
    }

    @Test
    void testPerformRejectsIdsAndConditionsWithoutDeleteBy() {
        Map<String, Object> inputParameters = Map.of(
            "table", "conversations", "ids", List.of(),
            "conditions", List.of(List.of(Map.of("column", "status", "value", "CLOSED"))));

        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class, () -> perform(actionDefinition, inputParameters));

        assertEquals(
            "Delete Records has both Record IDs and OR Conditions; set Delete By to IDS or CONDITIONS",
            illegalArgumentException.getMessage());

        IllegalArgumentException blankDeleteByException = assertThrows(
            IllegalArgumentException.class,
            () -> perform(actionDefinition, merge(inputParameters, Map.of("deleteBy", "  "))));

        assertEquals(illegalArgumentException.getMessage(), blankDeleteByException.getMessage());

        verifyNoInteractions(dataTableRowService);
    }

    private static DataTableRowConditions idsCondition(Long... ids) {
        return DataTableRowConditions.of(List.of(new DataTableRowFilter("id", Operation.IN, List.of(ids))));
    }

    private static Map<String, Object> merge(Map<String, Object> first, Map<String, Object> second) {
        Map<String, Object> merged = new HashMap<>(first);

        merged.putAll(second);

        return merged;
    }
}

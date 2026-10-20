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

package com.bytechef.component.datatable.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter.Operation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableUtilsTest {

    @Test
    void testFlattenRowLiftsColumnValuesNextToId() {
        Map<String, Object> values = new HashMap<>();

        values.put("phone", "385916039814");
        values.put("staff_reply", "on my way");

        Map<String, Object> flattenedRow = DataTableUtils.flattenRow(new DataTableRow(7, values));

        assertEquals(Map.of("id", 7L, "phone", "385916039814", "staff_reply", "on my way"), flattenedRow);
    }

    @Test
    void testFlattenRowKeepsNullColumnsAsPresentKeys() {
        Map<String, Object> values = new HashMap<>();

        values.put("staff_reply", null);

        Map<String, Object> flattenedRow = DataTableUtils.flattenRow(new DataTableRow(7, values));

        assertTrue(flattenedRow.containsKey("staff_reply"));
        assertNull(flattenedRow.get("staff_reply"));
    }

    @Test
    void testFlattenRowOfMissingRecordStaysNull() {
        assertNull(DataTableUtils.flattenRow(null));
    }

    @Test
    void testFlattenRowsFlattensEveryRow() {
        List<Map<String, Object>> flattenedRows = DataTableUtils.flattenRows(
            List.of(new DataTableRow(1, Map.of("status", "BOT")), new DataTableRow(2, Map.of("status", "CLOSED"))));

        assertEquals(
            List.of(Map.of("id", 1L, "status", "BOT"), Map.of("id", 2L, "status", "CLOSED")), flattenedRows);
    }

    @Test
    void testFlattenPayloadFlattensWebhookRowPayload() {
        Map<String, Object> payload = Map.of("id", 7, "values", Map.of("staff_reply", "on my way"));

        assertEquals(Map.of("id", 7, "staff_reply", "on my way"), DataTableUtils.flattenPayload(payload));
    }

    @Test
    void testFlattenPayloadWithoutValuesIsReturnedAsIs() {
        Map<String, Object> payload = Map.of("id", 7);

        assertEquals(Map.of("id", 7), DataTableUtils.flattenPayload(payload));
    }

    @Test
    void testFlattenPayloadOfUnknownShapeIsReturnedUnchanged() {
        Object payload = List.of("unexpected");

        assertSame(payload, DataTableUtils.flattenPayload(payload));
    }

    @Test
    void testGetConditionsDefaultsAndNormalizesOperations() {
        Map<String, Object> conditionWithNullOperation = new HashMap<>();

        conditionWithNullOperation.put("column", "status");
        conditionWithNullOperation.put("operation", null);
        conditionWithNullOperation.put("value", "BOT");

        DataTableRowConditions conditions = DataTableUtils.getConditions(
            MockParametersFactory.create(
                Map.of(
                    "conditions",
                    List.of(
                        List.of(
                            conditionWithNullOperation,
                            Map.of("column", "title", "operation", "contains", "value", "help"))))));

        assertEquals(
            DataTableRowConditions.of(
                List.of(
                    new DataTableRowFilter("status", Operation.EQUALS, "BOT"),
                    new DataTableRowFilter("title", Operation.CONTAINS, "help"))),
            conditions);
    }

    @Test
    void testGetConditionsRejectsConditionsThatAreNotNestedLists() {
        for (Object conditions : List.of(List.of(Map.of("column", "status", "value", "BOT")), "status = BOT")) {
            IllegalArgumentException illegalArgumentException = assertThrows(
                IllegalArgumentException.class,
                () -> DataTableUtils.getConditions(MockParametersFactory.create(Map.of("conditions", conditions))));

            assertEquals(
                "OR Conditions must be a list of AND Condition lists, each holding condition objects",
                illegalArgumentException.getMessage());
        }
    }

    @Test
    void testGetConditionsRejectsEmptyOrConditions() {
        for (List<?> andConditions : Arrays.asList(List.of(), null)) {
            IllegalArgumentException illegalArgumentException = assertThrows(
                IllegalArgumentException.class,
                () -> DataTableUtils.getConditions(
                    MockParametersFactory.create(
                        Map.of(
                            "conditions",
                            Arrays.asList(List.of(Map.of("column", "status", "value", "BOT")), andConditions)))));

            assertEquals(
                "OR Condition 2 is empty; add a condition to it or remove it", illegalArgumentException.getMessage());
        }
    }

    @Test
    void testGetConditionsRejectsInvalidConditions() {
        IllegalArgumentException unknownOperationException = assertThrows(
            IllegalArgumentException.class,
            () -> DataTableUtils.getConditions(
                MockParametersFactory.create(
                    Map.of(
                        "conditions",
                        List.of(List.of(Map.of("column", "status", "operation", "LIKE", "value", "x")))))));

        assertTrue(unknownOperationException.getMessage()
            .startsWith("Condition 1 of OR Condition 1: Unknown Operation 'LIKE'; expected one of EQUALS"));

        IllegalArgumentException emptyConditionException = assertThrows(
            IllegalArgumentException.class,
            () -> DataTableUtils.getConditions(
                MockParametersFactory.create(
                    Map.of("conditions", List.of(Arrays.asList(Map.of("column", "status", "value", "x"), null))))));

        assertEquals("Condition 2 of OR Condition 1 is empty", emptyConditionException.getMessage());

        IllegalArgumentException blankColumnException = assertThrows(
            IllegalArgumentException.class,
            () -> DataTableUtils.getConditions(
                MockParametersFactory.create(
                    Map.of("conditions", List.of(List.of(Map.of("column", " ", "value", "x")))))));

        assertEquals("Condition 1 of OR Condition 1: Column is required", blankColumnException.getMessage());

        IllegalArgumentException blankOperationException = assertThrows(
            IllegalArgumentException.class,
            () -> DataTableUtils.getConditions(
                MockParametersFactory.create(
                    Map.of(
                        "conditions",
                        List.of(List.of(Map.of("column", "status", "operation", " ", "value", "active")))))));

        assertEquals(
            "Condition 1 of OR Condition 1: Operation must not be blank", blankOperationException.getMessage());

        IllegalArgumentException missingBoundException = assertThrows(
            IllegalArgumentException.class,
            () -> DataTableUtils.getConditions(
                MockParametersFactory.create(
                    Map.of(
                        "conditions",
                        List.of(List.of(Map.of("column", "amount", "operation", "BETWEEN", "value", "1")))))));

        assertEquals(
            "Condition 1 of OR Condition 1: Filter on column 'amount' with operation BETWEEN requires every value to be set",
            missingBoundException.getMessage());
    }

    @Test
    void testGetConditionsRejectsUnknownKeys() {
        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class,
            () -> DataTableUtils.getConditions(
                MockParametersFactory.create(
                    Map.of(
                        "conditions",
                        List.of(List.of(Map.of("column", "status", "operator", "NOT_EQUALS", "value", "done")))))));

        assertEquals(
            "Condition 1 of OR Condition 1: Unknown condition key(s) operator; expected column, operation, value, valueTo, "
                +
                "values",
            illegalArgumentException.getMessage());
    }
}

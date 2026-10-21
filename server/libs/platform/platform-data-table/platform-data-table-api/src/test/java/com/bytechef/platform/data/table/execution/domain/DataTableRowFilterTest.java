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

package com.bytechef.platform.data.table.execution.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter.Operation;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter.Operation.Arity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableRowFilterTest {

    @Test
    void testRejectsBlankColumn() {
        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class, () -> new DataTableRowFilter(" ", Operation.EQUALS, "x"));

        assertEquals("column must not be blank", illegalArgumentException.getMessage());
    }

    @Test
    void testDropsValueOfOperationsWithoutValue() {
        DataTableRowFilter dataTableRowFilter = new DataTableRowFilter("title", Operation.EMPTY, "ignored");

        assertNull(dataTableRowFilter.value());
        assertEquals(List.of(), dataTableRowFilter.values());
    }

    @Test
    void testCopiesListValues() {
        List<Object> values = new ArrayList<>(List.of("a", "b"));

        DataTableRowFilter dataTableRowFilter = new DataTableRowFilter("title", Operation.IN, values);

        values.add("c");

        assertEquals(List.of("a", "b"), dataTableRowFilter.value());
        assertEquals(List.of("a", "b"), dataTableRowFilter.values());
    }

    @Test
    void testValuesOfSingleValueOperation() {
        assertEquals(List.of(5), new DataTableRowFilter("amount", Operation.GREATER, 5).values());
    }

    @Test
    void testSingleValueOnlyForSingleValueOperations() {
        assertEquals(5, new DataTableRowFilter("amount", Operation.GREATER, 5).singleValue());

        IllegalStateException illegalStateException = assertThrows(
            IllegalStateException.class,
            () -> new DataTableRowFilter("amount", Operation.IN, List.of(5)).singleValue());

        assertEquals("Operation IN does not take a single value", illegalStateException.getMessage());
    }

    @Test
    void testRejectsMissingAndBlankValues() {
        assertMessageContains(
            "requires a value; use Empty or Not Empty", () -> new DataTableRowFilter("title", Operation.EQUALS, " "));
        assertMessageContains(
            "requires a value", () -> new DataTableRowFilter("title", Operation.CONTAINS, null));
        assertMessageContains(
            "requires every value to be set",
            () -> new DataTableRowFilter("amount", Operation.BETWEEN, Arrays.asList(1, null)));
    }

    @Test
    void testRejectsValuesOfTheWrongShape() {
        assertMessageContains(
            "requires a list of values", () -> new DataTableRowFilter("amount", Operation.IN, 1));
        assertMessageContains(
            "requires at least one value", () -> new DataTableRowFilter("amount", Operation.IN, List.of()));
        assertMessageContains(
            "requires exactly two values, got 3",
            () -> new DataTableRowFilter("amount", Operation.BETWEEN, List.of(1, 2, 3)));
        assertMessageContains(
            "requires an ordered list of a lower and an upper bound",
            () -> new DataTableRowFilter("amount", Operation.BETWEEN, Set.of(10, 1)));
        assertMessageContains(
            "requires single values, not lists or objects",
            () -> new DataTableRowFilter("amount", Operation.IN, List.of(List.of(1))));
        assertMessageContains(
            "requires a single value, not a list or an object",
            () -> new DataTableRowFilter("title", Operation.EQUALS, Map.of("a", "b")));
        assertMessageContains(
            "requires a single value, not a list or an object",
            () -> new DataTableRowFilter("title", Operation.EQUALS, new String[] {
                "a"
            }));
        assertMessageContains(
            "requires single values, not lists or objects",
            () -> new DataTableRowFilter("amount", Operation.IN, List.of(new int[] {
                1
            })));
    }

    @Test
    void testOperationArityAndTextOnly() {
        assertEquals(Arity.LIST, Operation.IN.arity());
        assertEquals(Arity.PAIR, Operation.BETWEEN.arity());
        assertEquals(Arity.NONE, Operation.NOT_EMPTY.arity());
        assertEquals(Arity.ONE, Operation.NOT_EQUALS.arity());
        assertEquals(
            List.of(
                Operation.EQUALS_IGNORE_CASE, Operation.CONTAINS, Operation.NOT_CONTAINS, Operation.STARTS_WITH,
                Operation.ENDS_WITH),
            Arrays.stream(Operation.values())
                .filter(Operation::textOnly)
                .toList());
    }

    private static void assertMessageContains(String expectedMessagePart, Runnable runnable) {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, runnable::run);

        String message = illegalArgumentException.getMessage();

        assertTrue(
            message.contains(expectedMessagePart),
            () -> "Expected message to contain '%s' but was '%s'".formatted(expectedMessagePart, message));
    }
}

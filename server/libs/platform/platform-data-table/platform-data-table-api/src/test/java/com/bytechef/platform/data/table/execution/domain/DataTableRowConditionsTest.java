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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter.Operation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableRowConditionsTest {

    private static final DataTableRowFilter FILTER = new DataTableRowFilter("title", Operation.EQUALS, "x");

    @Test
    void testRejectsEmptyGroup() {
        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class, () -> new DataTableRowConditions(List.of(List.of(FILTER), List.of())));

        assertEquals("Condition group 2 is empty", illegalArgumentException.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new DataTableRowConditions(List.of(List.of())));
    }

    @Test
    void testKeepsNonEmptyGroups() {
        DataTableRowConditions dataTableRowConditions = new DataTableRowConditions(
            List.of(List.of(FILTER), List.of(FILTER, FILTER)));

        assertEquals(List.of(List.of(FILTER), List.of(FILTER, FILTER)), dataTableRowConditions.groups());
        assertFalse(dataTableRowConditions.isMatchAll());
    }

    @Test
    void testNoGroupsMatchAll() {
        assertTrue(DataTableRowConditions.matchAll()
            .isMatchAll());
        assertTrue(new DataTableRowConditions(List.of()).isMatchAll());
    }

    @Test
    void testOfRejectsNoFilters() {
        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class, () -> DataTableRowConditions.of(List.of()));

        assertEquals(
            "At least one filter is required; use matchAll() to match every row",
            illegalArgumentException.getMessage());
        assertThrows(NullPointerException.class, () -> DataTableRowConditions.of(null));
    }

    @Test
    void testCopiesGroups() {
        List<DataTableRowFilter> group = new ArrayList<>(List.of(FILTER));

        DataTableRowConditions dataTableRowConditions = DataTableRowConditions.of(group);

        group.clear();

        assertEquals(List.of(List.of(FILTER)), dataTableRowConditions.groups());
    }

    @Test
    void testRejectsNullGroupsAndFilters() {
        assertThrows(NullPointerException.class,
            () -> new DataTableRowConditions(Arrays.asList(List.of(FILTER), null)));
        assertThrows(NullPointerException.class, () -> DataTableRowConditions.of(Arrays.asList(FILTER, null)));
    }
}

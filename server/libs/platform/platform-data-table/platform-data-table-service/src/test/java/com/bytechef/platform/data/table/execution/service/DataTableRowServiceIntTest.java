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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter.Operation;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort.Direction;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
class DataTableRowServiceIntTest {

    private static final long DEV_ENVIRONMENT_ID = 0;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableRowService dataTableRowService;

    private final List<Long> itemIds = new ArrayList<>();

    @BeforeEach
    void beforeEach() {
        dataTableService.dropTable("rows", DEV_ENVIRONMENT_ID);
        dataTableService.createTable(
            "rows", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        dataTableService.dropTable("items", DEV_ENVIRONMENT_ID);
        dataTableService.createTable(
            "items", null,
            List.of(new ColumnSpec("title", ColumnType.STRING), new ColumnSpec("amount", ColumnType.INTEGER)),
            DEV_ENVIRONMENT_ID);

        itemIds.clear();

        insertItem("Apple", 5);
        insertItem("apricot", 10);
        insertItem("Banana", 20);
        insertItem("100%_off", 30);
        insertItem(null, 15);
    }

    @Test
    void testInsertRowReturnsTheStoredValues() {
        DataTableRow dataTableRow = dataTableRowService.insertRow(
            "rows", Map.of("title", "first"), DEV_ENVIRONMENT_ID);

        assertNotNull(dataTableRow);
        assertEquals("first", dataTableRow.values()
            .get("title"));
    }

    @Test
    void testUpdateRowReturnsTheUpdatedValues() {
        DataTableRow insertedDataTableRow = dataTableRowService.insertRow(
            "rows", Map.of("title", "first"), DEV_ENVIRONMENT_ID);

        DataTableRow updatedDataTableRow = dataTableRowService.updateRow(
            "rows", insertedDataTableRow.id(), Map.of("title", "second"), DEV_ENVIRONMENT_ID);

        assertEquals(insertedDataTableRow.id(), updatedDataTableRow.id());
        assertEquals("second", updatedDataTableRow.values()
            .get("title"));

        DataTableRow fetchedDataTableRow = dataTableRowService.getRow(
            "rows", insertedDataTableRow.id(), DEV_ENVIRONMENT_ID);

        assertEquals("second", fetchedDataTableRow.values()
            .get("title"));
    }

    @Test
    void testDeleteRowRemovesTheRow() {
        DataTableRow dataTableRow = dataTableRowService.insertRow(
            "rows", Map.of("title", "first"), DEV_ENVIRONMENT_ID);

        assertTrue(dataTableRowService.deleteRow("rows", dataTableRow.id(), DEV_ENVIRONMENT_ID));

        assertNull(dataTableRowService.getRow("rows", dataTableRow.id(), DEV_ENVIRONMENT_ID));

        assertTrue(dataTableRowService.listRows("rows", 10, 0, DEV_ENVIRONMENT_ID)
            .isEmpty());
    }

    @Test
    void testDeleteRowOfMissingRowReturnsFalse() {
        assertFalse(dataTableRowService.deleteRow("rows", Long.MAX_VALUE, DEV_ENVIRONMENT_ID));
    }

    @Test
    void testFindRowsWithoutFiltersReturnsAllRowsOrderedById() {
        assertEquals(itemIds, findItemIds(List.of(), null));
    }

    @Test
    void testFindRowsEquals() {
        assertEquals(
            List.of(itemIds.get(2)), findItemIds(List.of(filter("title", Operation.EQUALS, "Banana")), null));
    }

    @Test
    void testFindRowsNotEqualsIncludesRowsWithoutValue() {
        assertEquals(
            List.of(itemIds.get(0), itemIds.get(1), itemIds.get(3), itemIds.get(4)),
            findItemIds(List.of(filter("title", Operation.NOT_EQUALS, "Banana")), null));
    }

    @Test
    void testFindRowsTextOperationsAreCaseSensitive() {
        assertEquals(
            List.of(itemIds.get(1)), findItemIds(List.of(filter("title", Operation.STARTS_WITH, "ap")), null));
        assertEquals(
            List.of(itemIds.get(2)), findItemIds(List.of(filter("title", Operation.ENDS_WITH, "na")), null));
        assertEquals(
            List.of(itemIds.get(0), itemIds.get(1)),
            findItemIds(List.of(filter("title", Operation.CONTAINS, "p")), null));
        assertEquals(List.of(), findItemIds(List.of(filter("title", Operation.CONTAINS, "P")), null));
        assertEquals(
            List.of(itemIds.get(2), itemIds.get(3), itemIds.get(4)),
            findItemIds(List.of(filter("title", Operation.NOT_CONTAINS, "p")), null));
        assertEquals(List.of(), findItemIds(List.of(filter("title", Operation.EQUALS, "banana")), null));
        assertEquals(
            List.of(itemIds.get(2)),
            findItemIds(List.of(filter("title", Operation.EQUALS_IGNORE_CASE, "banana")), null));
    }

    @Test
    void testFindRowsContainsMatchesWildcardCharactersLiterally() {
        assertEquals(
            List.of(itemIds.get(3)), findItemIds(List.of(filter("title", Operation.CONTAINS, "%_")), null));
    }

    @Test
    void testFindRowsComparesNumbersGivenAsText() {
        assertEquals(
            List.of(itemIds.get(2), itemIds.get(3), itemIds.get(4)),
            findItemIds(List.of(filter("amount", Operation.GREATER, "10")), null));
        assertEquals(
            List.of(itemIds.get(1), itemIds.get(2), itemIds.get(3), itemIds.get(4)),
            findItemIds(List.of(filter("amount", Operation.GREATER_EQUALS, 10)), null));
        assertEquals(
            List.of(itemIds.get(0)), findItemIds(List.of(filter("amount", Operation.LESS, 10)), null));
        assertEquals(
            List.of(itemIds.get(0), itemIds.get(1)),
            findItemIds(List.of(filter("amount", Operation.LESS_EQUALS, "10")), null));
    }

    @Test
    void testFindRowsInAndBetween() {
        assertEquals(
            List.of(itemIds.get(0), itemIds.get(2)),
            findItemIds(List.of(filter("amount", Operation.IN, List.of(5, "20"))), null));
        assertEquals(List.of(), findItemIds(List.of(filter("amount", Operation.IN, List.of())), null));
        assertEquals(
            List.of(itemIds.get(1), itemIds.get(2), itemIds.get(4)),
            findItemIds(List.of(filter("amount", Operation.BETWEEN, List.of("10", "20"))), null));
    }

    @Test
    void testFindRowsIsEmptyAndIsNotEmpty() {
        assertEquals(List.of(itemIds.get(4)), findItemIds(List.of(filter("title", Operation.EMPTY, null)), null));
        assertEquals(
            List.of(itemIds.get(0), itemIds.get(1), itemIds.get(2), itemIds.get(3)),
            findItemIds(List.of(filter("title", Operation.NOT_EMPTY, null)), null));
    }

    @Test
    void testFindRowsCombinesFiltersWithAnd() {
        assertEquals(
            List.of(itemIds.get(1)),
            findItemIds(
                List.of(filter("title", Operation.STARTS_WITH, "a"), filter("amount", Operation.GREATER, 5)),
                null));
    }

    @Test
    void testFindRowsMatchesAnyAndCondition() {
        assertEquals(
            List.of(itemIds.get(0), itemIds.get(2)),
            findItemIdsMatchingAny(
                List.of(
                    List.of(filter("title", Operation.EQUALS, "Banana")),
                    List.of(filter("amount", Operation.LESS, 10)))));
        assertEquals(
            List.of(itemIds.get(1), itemIds.get(2)),
            findItemIdsMatchingAny(
                List.of(
                    List.of(filter("title", Operation.STARTS_WITH, "a"), filter("amount", Operation.GREATER, 5)),
                    List.of(filter("title", Operation.EQUALS, "Banana")))));
    }

    @Test
    void testFindRowsIgnoresEmptyAndConditions() {
        assertEquals(
            List.of(itemIds.get(2)),
            findItemIdsMatchingAny(List.of(List.of(), List.of(filter("title", Operation.EQUALS, "Banana")))));
        assertEquals(itemIds, findItemIdsMatchingAny(List.of(List.of())));
    }

    @Test
    void testFindRowsFiltersById() {
        assertEquals(
            List.of(itemIds.get(1)), findItemIds(List.of(filter("id", Operation.EQUALS, itemIds.get(1))), null));
    }

    @Test
    void testFindRowsSortsAndPaginates() {
        assertEquals(
            List.of(itemIds.get(3), itemIds.get(2), itemIds.get(4), itemIds.get(1), itemIds.get(0)),
            findItemIds(List.of(), new DataTableRowSort("amount", Direction.DESC)));

        List<DataTableRow> dataTableRows = dataTableRowService.findRows(
            "items", List.of(List.of(filter("amount", Operation.GREATER, 5))),
            new DataTableRowSort("amount", Direction.ASC), 2, 1, DEV_ENVIRONMENT_ID);

        assertEquals(
            List.of(itemIds.get(4), itemIds.get(2)),
            dataTableRows.stream()
                .map(DataTableRow::id)
                .toList());
    }

    @Test
    void testFindRowsRejectsInvalidFilters() {
        assertThrows(
            IllegalArgumentException.class,
            () -> findItemIds(List.of(filter("missing", Operation.EQUALS, "x")), null));
        assertThrows(
            IllegalArgumentException.class,
            () -> findItemIds(List.of(), new DataTableRowSort("missing", Direction.ASC)));
        assertThrows(
            IllegalArgumentException.class,
            () -> findItemIds(List.of(filter("amount", Operation.CONTAINS, "1")), null));
        assertThrows(
            IllegalArgumentException.class, () -> findItemIds(List.of(filter("amount", Operation.EQUALS, null)), null));
        assertThrows(
            IllegalArgumentException.class,
            () -> findItemIds(List.of(filter("amount", Operation.BETWEEN, List.of(1))), null));
        assertThrows(
            IllegalArgumentException.class, () -> findItemIds(List.of(filter("amount", Operation.IN, 1)), null));
    }

    private static DataTableRowFilter filter(String column, Operation operation, Object value) {
        return new DataTableRowFilter(column, operation, value);
    }

    private List<Long> findItemIds(List<DataTableRowFilter> filters, DataTableRowSort sort) {
        List<DataTableRow> dataTableRows = dataTableRowService.findRows(
            "items", List.of(filters), sort, 100, 0, DEV_ENVIRONMENT_ID);

        return dataTableRows.stream()
            .map(DataTableRow::id)
            .toList();
    }

    private List<Long> findItemIdsMatchingAny(List<List<DataTableRowFilter>> conditions) {
        List<DataTableRow> dataTableRows = dataTableRowService.findRows(
            "items", conditions, null, 100, 0, DEV_ENVIRONMENT_ID);

        return dataTableRows.stream()
            .map(DataTableRow::id)
            .toList();
    }

    private void insertItem(String title, int amount) {
        Map<String, Object> values = new HashMap<>();

        values.put("title", title);
        values.put("amount", amount);

        DataTableRow dataTableRow = dataTableRowService.insertRow("items", values, DEV_ENVIRONMENT_ID);

        itemIds.add(dataTableRow.id());
    }
}

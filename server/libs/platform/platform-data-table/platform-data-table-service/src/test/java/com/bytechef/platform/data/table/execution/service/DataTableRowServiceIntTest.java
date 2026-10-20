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
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter.Operation;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort.Direction;
import com.bytechef.platform.data.table.execution.event.DataTableWebhookEvent;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@RecordApplicationEvents
class DataTableRowServiceIntTest {

    private static final long DEV_ENVIRONMENT_ID = 0;
    private static final long STAGE_ENVIRONMENT_ID = 1;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableRowService dataTableRowService;

    private final List<Long> itemIds = new ArrayList<>();
    private final List<Long> typedIds = new ArrayList<>();

    @AfterEach
    void afterEach() {
        for (String baseName : List.of("rows", "items", "typed")) {
            dataTableService.dropTable(baseName, DEV_ENVIRONMENT_ID);
        }

        dataTableService.dropTable("items", STAGE_ENVIRONMENT_ID);
    }

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

        dataTableService.dropTable("typed", DEV_ENVIRONMENT_ID);
        dataTableService.createTable(
            "typed", null,
            List.of(
                new ColumnSpec("active", ColumnType.BOOLEAN), new ColumnSpec("price", ColumnType.NUMBER),
                new ColumnSpec("due", ColumnType.DATE), new ColumnSpec("created", ColumnType.DATE_TIME)),
            DEV_ENVIRONMENT_ID);

        typedIds.clear();

        insertTyped(true, "1.5", "2026-01-10", "2026-01-10T08:00:00");
        insertTyped(false, "2.5", "2026-02-10", "2026-02-10T08:00:00");
        insertTyped(null, null, null, null);
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

        DataTableRow fetchedDataTableRow = Objects.requireNonNull(
            dataTableRowService.getRow("rows", insertedDataTableRow.id(), DEV_ENVIRONMENT_ID));

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
    void testDeleteRowPublishesTheDeletedRowValues() {
        DataTableRow dataTableRow = dataTableRowService.insertRow(
            "rows", Map.of("title", "first"), DEV_ENVIRONMENT_ID);

        dataTableRowService.deleteRow("rows", dataTableRow.id(), DEV_ENVIRONMENT_ID);

        List<DataTableWebhookEvent> dataTableWebhookEvents = getWebhookEvents(DataTableWebhookType.RECORD_DELETED);

        assertEquals(1, dataTableWebhookEvents.size());
        assertEquals(
            List.of(Map.of("id", dataTableRow.id(), "values", dataTableRow.values())),
            dataTableWebhookEvents.getFirst()
                .getPayloads());
    }

    @Test
    void testDeleteRowOfMissingRowReturnsFalseAndPublishesNothing() {
        assertFalse(dataTableRowService.deleteRow("rows", Long.MAX_VALUE, DEV_ENVIRONMENT_ID));

        assertTrue(getWebhookEvents(DataTableWebhookType.RECORD_DELETED).isEmpty());
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
        assertEquals(
            List.of(itemIds.get(1), itemIds.get(2), itemIds.get(4)),
            findItemIds(List.of(filter("amount", Operation.BETWEEN, List.of("10", "20"))), null));
        assertEquals(List.of(), findItemIds(List.of(filter("amount", Operation.BETWEEN, List.of(20, 10))), null));
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
    void testFindRowsRejectsAnEmptyAndCondition() {
        assertThrows(
            IllegalArgumentException.class,
            () -> findItemIdsMatchingAny(List.of(List.of(), List.of(filter("title", Operation.EQUALS, "Banana")))));
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
            "items", DataTableRowConditions.of(List.of(filter("amount", Operation.GREATER, 5))),
            new DataTableRowSort("amount", Direction.ASC), 2, 1, DEV_ENVIRONMENT_ID);

        assertEquals(
            List.of(itemIds.get(4), itemIds.get(2)),
            dataTableRows.stream()
                .map(DataTableRow::id)
                .toList());
    }

    @Test
    void testFindRowsRejectsInvalidFilters() {
        assertMessageContains(
            "Unknown column: missing", () -> findItemIds(List.of(filter("missing", Operation.EQUALS, "x")), null));
        assertMessageContains(
            "Unknown column: missing",
            () -> findItemIds(List.of(), new DataTableRowSort("missing", Direction.ASC)));
        assertMessageContains(
            "only supported on text columns",
            () -> findItemIds(List.of(filter("amount", Operation.CONTAINS, "1")), null));
        assertMessageContains(
            "requires a value", () -> findItemIds(List.of(filter("amount", Operation.EQUALS, null)), null));
        assertMessageContains(
            "requires exactly two values",
            () -> findItemIds(List.of(filter("amount", Operation.BETWEEN, List.of(1))), null));
        assertMessageContains(
            "requires every value to be set",
            () -> findItemIds(List.of(filter("amount", Operation.BETWEEN, Arrays.asList(1, null))), null));
        assertMessageContains(
            "requires a list of values", () -> findItemIds(List.of(filter("amount", Operation.IN, 1)), null));
        assertMessageContains(
            "requires at least one value", () -> findItemIds(List.of(filter("amount", Operation.IN, List.of())), null));
        assertMessageContains(
            "requires a single value, not a list",
            () -> findItemIds(List.of(filter("title", Operation.NOT_EQUALS, List.of("a", "b"))), null));
        assertMessageContains(
            "requires single values, not lists or objects",
            () -> findItemIds(List.of(filter("title", Operation.IN, List.of(List.of("a")))), null));
    }

    @Test
    void testFindRowsRejectsNegativePaging() {
        assertMessageContains(
            "must not be negative",
            () -> dataTableRowService.findRows("items", DataTableRowConditions.matchAll(), null, -1, 0,
                DEV_ENVIRONMENT_ID));
        assertMessageContains(
            "must not be negative",
            () -> dataTableRowService.findRows("items", DataTableRowConditions.matchAll(), null, 10, -1,
                DEV_ENVIRONMENT_ID));
    }

    @Test
    void testFindRowsRejectsWholeNumbersForDateColumns() {
        assertMessageContains(
            "Value '2025' is not a valid date for column 'due'; use an ISO format",
            () -> findTypedIds(List.of(filter("due", Operation.GREATER, "2025"))));
        assertMessageContains(
            "Value '20250101' is not a valid date_time for column 'created'",
            () -> findTypedIds(List.of(filter("created", Operation.LESS, "20250101"))));
        assertMessageContains(
            "Value '1760000000' is not a valid date for column 'due'; use an ISO format",
            () -> findTypedIds(List.of(filter("due", Operation.GREATER, 1760000000L))));
        assertMessageContains(
            "Value '20250101' is not a valid date_time for column 'created'",
            () -> findTypedIds(List.of(filter("created", Operation.LESS, 20250101))));
        assertMessageContains(
            "Value '1760000000' is not a valid date for column 'due'",
            () -> findTypedIds(List.of(filter("due", Operation.IN, List.of("2026-01-10", 1760000000)))));
    }

    @Test
    void testDeleteRowsAndUpdateRowsRejectNumbersForDateColumnsWithoutChangingRows() {
        DataTableRowConditions conditions = DataTableRowConditions.of(
            List.of(filter("due", Operation.GREATER, 1760000000L)));

        assertMessageContains(
            "is not a valid date for column 'due'",
            () -> dataTableRowService.deleteRows("typed", conditions, DEV_ENVIRONMENT_ID));
        assertMessageContains(
            "is not a valid date for column 'due'",
            () -> dataTableRowService.updateRows(
                "typed", DataTableRowConditions.of(List.of(filter("price", Operation.NOT_EMPTY, null))),
                Map.of("due", 1760000000L), DEV_ENVIRONMENT_ID));

        assertEquals(typedIds, findTypedIds(List.of()));
        assertEquals(List.of(typedIds.get(1)), findTypedIds(List.of(filter("due", Operation.EQUALS, "2026-02-10"))));
    }

    @Test
    void testFindRowsRejectsDatesThatDoNotExist() {
        assertMessageContains(
            "Value '2026-02-30' is not a valid date for column 'due'",
            () -> findTypedIds(List.of(filter("due", Operation.EQUALS, "2026-02-30"))));
        assertMessageContains(
            "Value '2026-2-30' is not a valid date for column 'due'",
            () -> findTypedIds(List.of(filter("due", Operation.EQUALS, "2026-2-30"))));
        assertMessageContains(
            "Value '2026-02-30 08:00:00' is not a valid date_time for column 'created'",
            () -> findTypedIds(List.of(filter("created", Operation.EQUALS, "2026-02-30 08:00:00"))));
        assertMessageContains(
            "Value '2026-04-31T08:00:00' is not a valid date_time for column 'created'",
            () -> findTypedIds(List.of(filter("created", Operation.LESS, "2026-04-31T08:00:00"))));
    }

    @Test
    void testDeleteRowsAndUpdateRowsRejectDatesThatDoNotExistWithoutChangingRows() {
        assertMessageContains(
            "Value '2026-02-30' is not a valid date for column 'due'",
            () -> dataTableRowService.deleteRows(
                "typed", DataTableRowConditions.of(List.of(filter("due", Operation.EQUALS, "2026-02-30"))),
                DEV_ENVIRONMENT_ID));
        assertMessageContains(
            "Value '2026-02-30' is not a valid date for column 'due'",
            () -> dataTableRowService.updateRows(
                "typed", DataTableRowConditions.of(List.of(filter("price", Operation.NOT_EMPTY, null))),
                Map.of("due", "2026-02-30"), DEV_ENVIRONMENT_ID));

        assertEquals(typedIds, findTypedIds(List.of()));
        assertEquals(List.of(typedIds.get(1)), findTypedIds(List.of(filter("due", Operation.EQUALS, "2026-02-10"))));
    }

    @Test
    void testFindRowsComparesDateColumnsWithTheDateWrittenInADateTime() {
        assertEquals(
            List.of(typedIds.get(0)),
            findTypedIds(List.of(filter("due", Operation.EQUALS, "2026-01-10T23:30:00-05:00"))));
        assertEquals(
            List.of(typedIds.get(1)),
            findTypedIds(List.of(filter("due", Operation.EQUALS, "2026-02-10T00:30:00+05:00"))));
        assertEquals(
            List.of(typedIds.get(0)), findTypedIds(List.of(filter("due", Operation.EQUALS, "2026-01-10 23:30"))));
    }

    @Test
    void testRowsAreChangedAndReadInASingleStatement() {
        DataTableRowService targetDataTableRowService = AopTestUtils.getUltimateTargetObject(dataTableRowService);

        assertEquals(Boolean.TRUE, ReflectionTestUtils.invokeMethod(targetDataTableRowService, "isReturningSupported"));
    }

    @Test
    void testFindRowsTreatsEmptyTextAsEmpty() {
        insertItem("", 50);

        assertEquals(
            List.of(itemIds.get(4), itemIds.get(5)),
            findItemIds(List.of(filter("title", Operation.EMPTY, null)), null));
        assertEquals(
            List.of(itemIds.get(0), itemIds.get(1), itemIds.get(2), itemIds.get(3)),
            findItemIds(List.of(filter("title", Operation.NOT_EMPTY, null)), null));
    }

    @Test
    void testFindRowsRejectsBlankValues() {
        for (Operation operation : List.of(
            Operation.EQUALS, Operation.EQUALS_IGNORE_CASE, Operation.NOT_EQUALS, Operation.CONTAINS,
            Operation.NOT_CONTAINS, Operation.STARTS_WITH, Operation.ENDS_WITH)) {

            assertMessageContains(
                "requires a value", () -> findItemIds(List.of(filter("title", operation, "")), null));
            assertMessageContains(
                "requires a value", () -> findItemIds(List.of(filter("title", operation, "   ")), null));
        }

        assertMessageContains(
            "requires every value to be set",
            () -> findItemIds(List.of(filter("title", Operation.IN, List.of("Apple", ""))), null));
    }

    @Test
    void testFindRowsRejectsValuesInvalidForTheColumnType() {
        assertMessageContains(
            "Value 'abc' is not a valid integer for column 'amount'",
            () -> findItemIds(List.of(filter("amount", Operation.GREATER, "abc")), null));
        assertMessageContains(
            "Value '10.5' is not a valid integer",
            () -> findItemIds(List.of(filter("amount", Operation.LESS, "10.5")), null));
        assertMessageContains(
            "Value '5.5' is not a valid integer",
            () -> findItemIds(List.of(filter("id", Operation.EQUALS, 5.5)), null));
        assertMessageContains(
            "Value '99999999999999999999' is not a valid integer",
            () -> findItemIds(List.of(filter("amount", Operation.EQUALS, "99999999999999999999")), null));
        assertMessageContains(
            "Value 'ture' is not a valid boolean for column 'active'",
            () -> findTypedIds(List.of(filter("active", Operation.EQUALS, "ture"))));
        assertMessageContains(
            "Value '31/12/2026' is not a valid date for column 'due'",
            () -> findTypedIds(List.of(filter("due", Operation.GREATER, "31/12/2026"))));
        assertMessageContains(
            "Value 'cheap' is not a valid number for column 'price'",
            () -> findTypedIds(List.of(filter("price", Operation.LESS, "cheap"))));
    }

    @Test
    void testFindRowsComparesIntegersGivenAsWholeDecimals() {
        assertEquals(List.of(itemIds.get(1)), findItemIds(List.of(filter("amount", Operation.EQUALS, "10.0")), null));
    }

    @Test
    void testFindRowsFiltersBooleanNumberDateAndDateTimeColumns() {
        assertEquals(List.of(typedIds.get(0)), findTypedIds(List.of(filter("active", Operation.EQUALS, "yes"))));
        assertEquals(List.of(typedIds.get(1)), findTypedIds(List.of(filter("active", Operation.EQUALS, false))));
        assertEquals(
            List.of(typedIds.get(0), typedIds.get(2)),
            findTypedIds(List.of(filter("active", Operation.NOT_EQUALS, "false"))));
        assertEquals(List.of(typedIds.get(0)), findTypedIds(List.of(filter("price", Operation.LESS, "2.5"))));
        assertEquals(
            List.of(typedIds.get(1)), findTypedIds(List.of(filter("due", Operation.GREATER, "2026-01-31"))));
        assertEquals(
            List.of(typedIds.get(0), typedIds.get(1)),
            findTypedIds(List.of(filter("due", Operation.BETWEEN, List.of("2026-01-10", "2026-02-10")))));
        assertEquals(
            List.of(typedIds.get(1)),
            findTypedIds(List.of(filter("created", Operation.GREATER_EQUALS, "2026-02-10T08:00:00"))));
        assertEquals(List.of(typedIds.get(2)), findTypedIds(List.of(filter("due", Operation.EMPTY, null))));
        assertEquals(
            List.of(typedIds.get(0), typedIds.get(1)),
            findTypedIds(List.of(filter("price", Operation.NOT_EMPTY, null))));
    }

    @Test
    void testFindRowsSortsRowsWithoutValueLastAndBreaksTiesById() {
        insertItem("Cherry", 10);

        assertEquals(
            itemIds.get(4), findItemIds(List.of(), new DataTableRowSort("title", Direction.ASC)).getLast());
        assertEquals(
            itemIds.get(4), findItemIds(List.of(), new DataTableRowSort("title", Direction.DESC)).getLast());
        assertEquals(
            List.of(itemIds.get(0), itemIds.get(1), itemIds.get(5), itemIds.get(4), itemIds.get(2), itemIds.get(3)),
            findItemIds(List.of(), new DataTableRowSort("amount", Direction.ASC)));
        assertEquals(
            List.of(itemIds.get(3), itemIds.get(2), itemIds.get(4), itemIds.get(1), itemIds.get(5), itemIds.get(0)),
            findItemIds(List.of(), new DataTableRowSort("amount", Direction.DESC)));
    }

    @Test
    void testFindRowsBindsParametersOfSeveralGroupsInOrder() {
        List<DataTableRow> dataTableRows = dataTableRowService.findRows(
            "items",
            new DataTableRowConditions(
                List.of(
                    List.of(
                        filter("amount", Operation.IN, List.of(5, 20)), filter("title", Operation.NOT_EMPTY, null)),
                    List.of(filter("amount", Operation.BETWEEN, List.of(25, 35))))),
            new DataTableRowSort("amount", Direction.DESC), 2, 1, DEV_ENVIRONMENT_ID);

        assertEquals(
            List.of(itemIds.get(2), itemIds.get(0)),
            dataTableRows.stream()
                .map(DataTableRow::id)
                .toList());
    }

    @Test
    void testFindRowsKeepsSpacesInTextMatchValues() {
        insertItem("Big Apple", 40);

        assertEquals(
            List.of(itemIds.get(5)), findItemIds(List.of(filter("title", Operation.CONTAINS, " Apple")), null));
        assertEquals(
            List.of(itemIds.get(0), itemIds.get(5)),
            findItemIds(List.of(filter("title", Operation.ENDS_WITH, "Apple")), null));
        assertEquals(List.of(), findItemIds(List.of(filter("title", Operation.STARTS_WITH, "Apple ")), null));
    }

    @Test
    void testFindRowsRejectsNonFiniteAndArrayValues() {
        assertMessageContains(
            "Value 'NaN' is not a valid number for column 'price'",
            () -> findTypedIds(List.of(filter("price", Operation.EQUALS, Double.NaN))));
        assertMessageContains(
            "Value 'Infinity' is not a valid number for column 'price'",
            () -> findTypedIds(List.of(filter("price", Operation.GREATER, Double.POSITIVE_INFINITY))));
        assertMessageContains(
            "requires a single value, not a list or an object",
            () -> findItemIds(List.of(filter("title", Operation.EQUALS, new String[] {
                "Apple"
            })), null));
    }

    @Test
    void testFindRowsMatchesBackslashLiterally() {
        insertItem("a\\b", 40);

        assertEquals(
            List.of(itemIds.get(5)), findItemIds(List.of(filter("title", Operation.CONTAINS, "\\")), null));
    }

    @Test
    void testDeleteRowsDeletesEveryMatchingRow() {
        List<DataTableRow> deletedDataTableRows = dataTableRowService.deleteRows(
            "items", DataTableRowConditions.of(List.of(filter("amount", Operation.GREATER, 10))), DEV_ENVIRONMENT_ID);

        assertEquals(
            List.of(itemIds.get(2), itemIds.get(3), itemIds.get(4)),
            deletedDataTableRows.stream()
                .map(DataTableRow::id)
                .toList());
        assertEquals("Banana", deletedDataTableRows.getFirst()
            .values()
            .get("title"));
        assertEquals(List.of(itemIds.get(0), itemIds.get(1)), findItemIds(List.of(), null));

        List<DataTableWebhookEvent> dataTableWebhookEvents = getWebhookEvents(DataTableWebhookType.RECORD_DELETED);

        assertEquals(1, dataTableWebhookEvents.size());

        DataTableWebhookEvent dataTableWebhookEvent = dataTableWebhookEvents.getFirst();

        assertEquals("items", dataTableWebhookEvent.getBaseName());
        assertEquals(DEV_ENVIRONMENT_ID, dataTableWebhookEvent.getEnvironmentId());

        List<Map<String, Object>> payloads = dataTableWebhookEvent.getPayloads();

        assertEquals(
            List.of(itemIds.get(2), itemIds.get(3), itemIds.get(4)),
            payloads.stream()
                .map(payload -> payload.get("id"))
                .toList());
        assertEquals(
            deletedDataTableRows.getFirst()
                .values(),
            payloads.getFirst()
                .get("values"));
    }

    @Test
    void testDeleteRowsAndUpdateRowsLeaveOtherEnvironmentsUnchanged() {
        dataTableService.dropTable("items", STAGE_ENVIRONMENT_ID);
        dataTableService.createTable(
            "items", null,
            List.of(new ColumnSpec("title", ColumnType.STRING), new ColumnSpec("amount", ColumnType.INTEGER)),
            STAGE_ENVIRONMENT_ID);

        DataTableRow stageDataTableRow = dataTableRowService.insertRow(
            "items", Map.of("title", "Stage", "amount", 50), STAGE_ENVIRONMENT_ID);

        DataTableRowConditions conditions = DataTableRowConditions.of(List.of(filter("amount", Operation.GREATER, 0)));

        dataTableRowService.updateRows("items", conditions, Map.of("title", "changed"), DEV_ENVIRONMENT_ID);
        dataTableRowService.deleteRows("items", conditions, DEV_ENVIRONMENT_ID);

        assertEquals(List.of(), findItemIds(List.of(), null));

        List<DataTableRow> stageDataTableRows = dataTableRowService.listRows("items", 10, 0, STAGE_ENVIRONMENT_ID);

        assertEquals(List.of(stageDataTableRow), stageDataTableRows);
    }

    @Test
    void testDeleteRowsWithoutMatchDeletesNothing() {
        assertEquals(
            List.of(),
            dataTableRowService.deleteRows(
                "items", DataTableRowConditions.of(List.of(filter("amount", Operation.GREATER, 100))),
                DEV_ENVIRONMENT_ID));
        assertEquals(itemIds, findItemIds(List.of(), null));
        assertEquals(List.of(), getWebhookEvents(DataTableWebhookType.RECORD_DELETED));
    }

    @Test
    void testDeleteRowsRejectsMissingConditionsAndInvalidFilters() {
        assertMessageContains(
            "At least one condition is required",
            () -> dataTableRowService.deleteRows("items", DataTableRowConditions.matchAll(), DEV_ENVIRONMENT_ID));
        assertMessageContains(
            "requires a value",
            () -> dataTableRowService.deleteRows(
                "items", DataTableRowConditions.of(List.of(filter("title", Operation.CONTAINS, ""))),
                DEV_ENVIRONMENT_ID));
        assertMessageContains(
            "Unknown column: missing",
            () -> dataTableRowService.deleteRows(
                "items", DataTableRowConditions.of(List.of(filter("missing", Operation.EQUALS, "x"))),
                DEV_ENVIRONMENT_ID));
        assertMessageContains(
            "only supported on text columns",
            () -> dataTableRowService.deleteRows(
                "items", DataTableRowConditions.of(List.of(filter("amount", Operation.STARTS_WITH, "1"))),
                DEV_ENVIRONMENT_ID));
        assertMessageContains(
            "Value 'abc' is not a valid integer for column 'amount'",
            () -> dataTableRowService.deleteRows(
                "items", DataTableRowConditions.of(List.of(filter("amount", Operation.GREATER, "abc"))),
                DEV_ENVIRONMENT_ID));

        assertEquals(itemIds, findItemIds(List.of(), null));
        assertEquals(List.of(), getWebhookEvents(DataTableWebhookType.RECORD_DELETED));
    }

    @Test
    void testUpdateRowsUpdatesEveryMatchingRow() {
        List<DataTableRow> updatedDataTableRows = dataTableRowService.updateRows(
            "items", DataTableRowConditions.of(List.of(filter("amount", Operation.GREATER_EQUALS, 20))),
            Map.of("title", "sold"),
            DEV_ENVIRONMENT_ID);

        assertEquals(
            List.of(itemIds.get(2), itemIds.get(3)),
            updatedDataTableRows.stream()
                .map(DataTableRow::id)
                .toList());
        assertTrue(updatedDataTableRows.stream()
            .allMatch(dataTableRow -> "sold".equals(dataTableRow.values()
                .get("title"))));
        assertEquals(
            List.of(itemIds.get(2), itemIds.get(3)),
            findItemIds(List.of(filter("title", Operation.EQUALS, "sold")), null));
        assertEquals(
            "Apple", Objects.requireNonNull(dataTableRowService.getRow("items", itemIds.get(0), DEV_ENVIRONMENT_ID))
                .values()
                .get("title"));

        List<DataTableWebhookEvent> dataTableWebhookEvents = getWebhookEvents(DataTableWebhookType.RECORD_UPDATED);

        assertEquals(1, dataTableWebhookEvents.size());

        DataTableWebhookEvent dataTableWebhookEvent = dataTableWebhookEvents.getFirst();

        assertEquals("items", dataTableWebhookEvent.getBaseName());
        assertEquals(DEV_ENVIRONMENT_ID, dataTableWebhookEvent.getEnvironmentId());

        List<Map<String, Object>> payloads = dataTableWebhookEvent.getPayloads();

        assertEquals(
            List.of(itemIds.get(2), itemIds.get(3)),
            payloads.stream()
                .map(payload -> payload.get("id"))
                .toList());
        assertEquals(
            updatedDataTableRows.getFirst()
                .values(),
            payloads.getFirst()
                .get("values"));
    }

    @Test
    void testUpdateRowsWithoutMatchUpdatesNothing() {
        assertEquals(
            List.of(),
            dataTableRowService.updateRows(
                "items", DataTableRowConditions.of(List.of(filter("amount", Operation.GREATER, 100))),
                Map.of("title", "sold"),
                DEV_ENVIRONMENT_ID));
        assertEquals(List.of(), getWebhookEvents(DataTableWebhookType.RECORD_UPDATED));
    }

    @Test
    void testUpdateRowsRejectsInvalidInputWithoutChangingRows() {
        DataTableRowConditions conditions = DataTableRowConditions.of(List.of(filter("amount", Operation.GREATER, 0)));

        assertMessageContains(
            "At least one condition is required",
            () -> dataTableRowService.updateRows("items", DataTableRowConditions.matchAll(), Map.of("title", "x"),
                DEV_ENVIRONMENT_ID));
        assertMessageContains(
            "requires a value for at least one column",
            () -> dataTableRowService.updateRows("items", conditions, Map.of(), DEV_ENVIRONMENT_ID));
        assertMessageContains(
            "Unknown column(s): stauts",
            () -> dataTableRowService.updateRows("items", conditions, Map.of("stauts", "x"), DEV_ENVIRONMENT_ID));
        assertMessageContains(
            "Value 'abc' is not a valid integer for column 'amount'",
            () -> dataTableRowService.updateRows("items", conditions, Map.of("amount", "abc"), DEV_ENVIRONMENT_ID));
        assertMessageContains(
            "The id column cannot be updated",
            () -> dataTableRowService.updateRows(
                "items", conditions, Map.of("id", 5, "title", "x"), DEV_ENVIRONMENT_ID));
        assertMessageContains(
            "Value for column 'amount' is empty",
            () -> dataTableRowService.updateRows("items", conditions, Map.of("amount", "  "), DEV_ENVIRONMENT_ID));
        assertMessageContains(
            "Value for column 'title' is empty",
            () -> dataTableRowService.updateRows("items", conditions, nullValue("title"), DEV_ENVIRONMENT_ID));
        assertMessageContains(
            "must be a single value, not a list",
            () -> dataTableRowService.updateRows(
                "items", conditions, Map.of("title", List.of("a")), DEV_ENVIRONMENT_ID));
        assertMessageContains(
            "must be a single value, not a list",
            () -> dataTableRowService.updateRows(
                "items", conditions, Map.of("title", new String[] {
                    "a"
                }), DEV_ENVIRONMENT_ID));
        assertMessageContains(
            "Value 'NaN' is not a valid number for column 'price'",
            () -> dataTableRowService.updateRows(
                "typed", DataTableRowConditions.of(List.of(filter("price", Operation.NOT_EMPTY, null))),
                Map.of("price", Double.NaN), DEV_ENVIRONMENT_ID));
        assertMessageContains(
            "Column 'title' is given more than once",
            () -> dataTableRowService.updateRows(
                "items", conditions, Map.of("title", "x", "Title", "x"), DEV_ENVIRONMENT_ID));

        assertEquals(
            List.of(itemIds.get(1)), findItemIds(List.of(filter("amount", Operation.EQUALS, 10)), null));
        assertEquals(
            List.of(itemIds.get(0)), findItemIds(List.of(filter("title", Operation.EQUALS, "Apple")), null));
        assertEquals(List.of(), findItemIds(List.of(filter("title", Operation.EQUALS, "x")), null));
        assertEquals(List.of(), getWebhookEvents(DataTableWebhookType.RECORD_UPDATED));
    }

    @Test
    void testUpdateRowsSetsSeveralColumnsOnRowsMatchingAnyGroup() {
        List<DataTableRow> updatedDataTableRows = dataTableRowService.updateRows(
            "items",
            new DataTableRowConditions(
                List.of(
                    List.of(filter("amount", Operation.IN, List.of(5, 20))),
                    List.of(
                        filter("amount", Operation.BETWEEN, List.of(25, 35)),
                        filter("title", Operation.STARTS_WITH, "100")))),
            Map.of("title", "sold", "amount", 1), DEV_ENVIRONMENT_ID);

        assertEquals(
            List.of(itemIds.get(0), itemIds.get(2), itemIds.get(3)),
            updatedDataTableRows.stream()
                .map(DataTableRow::id)
                .toList());
        assertEquals(
            List.of(itemIds.get(0), itemIds.get(2), itemIds.get(3)),
            findItemIds(
                List.of(filter("title", Operation.EQUALS, "sold"), filter("amount", Operation.EQUALS, 1)), null));
        assertEquals(
            List.of(itemIds.get(1), itemIds.get(4)),
            findItemIds(List.of(filter("amount", Operation.NOT_EQUALS, 1)), null));
    }

    @Test
    void testUpdateRowWithoutColumnValuesReturnsTheRow() {
        DataTableRow dataTableRow = dataTableRowService.updateRow(
            "items", itemIds.get(2), Map.of("id", 1), DEV_ENVIRONMENT_ID);

        assertEquals(itemIds.get(2), dataTableRow.id());
        assertEquals("Banana", dataTableRow.values()
            .get("title"));

        assertMessageContains(
            "Row not found",
            () -> dataTableRowService.updateRow("items", Long.MAX_VALUE, Map.of(), DEV_ENVIRONMENT_ID));
    }

    @Test
    void testUpdateRowRejectsUnknownColumnsAndInvalidValues() {
        assertMessageContains(
            "Unknown column(s): stauts",
            () -> dataTableRowService.updateRow("items", itemIds.get(0), Map.of("stauts", "x"), DEV_ENVIRONMENT_ID));
        assertMessageContains(
            "Value 'ture' is not a valid boolean",
            () -> dataTableRowService.updateRow(
                "typed", typedIds.get(0), Map.of("active", "ture"), DEV_ENVIRONMENT_ID));
    }

    @Test
    void testUpdateRowClearsColumnsGivenNullOrBlankValues() {
        DataTableRow dataTableRow = dataTableRowService.updateRow(
            "items", itemIds.get(0), Map.of("amount", "  ", "title", ""), DEV_ENVIRONMENT_ID);

        assertNull(dataTableRow.values()
            .get("amount"));
        assertEquals("", dataTableRow.values()
            .get("title"));

        dataTableRow = dataTableRowService.updateRow("items", itemIds.get(1), nullValue("amount"), DEV_ENVIRONMENT_ID);

        assertNull(dataTableRow.values()
            .get("amount"));
    }

    @Test
    void testInsertRowRejectsUnknownColumnsAndInvalidValues() {
        assertMessageContains(
            "Unknown column(s): stauts",
            () -> dataTableRowService.insertRow("items", Map.of("stauts", "x"), DEV_ENVIRONMENT_ID));
        assertMessageContains(
            "Value 'abc' is not a valid integer for column 'amount'",
            () -> dataTableRowService.insertRow("items", Map.of("amount", "abc"), DEV_ENVIRONMENT_ID));

        assertEquals(itemIds, findItemIds(List.of(), null));
    }

    @Test
    void testInsertRowIgnoresId() {
        DataTableRow dataTableRow = dataTableRowService.insertRow(
            "items", Map.of("id", itemIds.getFirst(), "title", "copy"), DEV_ENVIRONMENT_ID);

        assertFalse(itemIds.contains(dataTableRow.id()));
        assertEquals("copy", dataTableRow.values()
            .get("title"));
    }

    @Test
    void testImportCsvImportsRowsAndLeavesBlankCellsEmpty() {
        dataTableRowService.importCsv("rows", "id,title,unknown\n7,first,x\n8,,y\n", DEV_ENVIRONMENT_ID);

        List<DataTableRow> dataTableRows = dataTableRowService.listRows("rows", 10, 0, DEV_ENVIRONMENT_ID);

        assertEquals(2, dataTableRows.size());
        assertEquals("first", dataTableRows.getFirst()
            .values()
            .get("title"));
        assertNull(dataTableRows.getLast()
            .values()
            .get("title"));
    }

    @Test
    void testImportCsvRejectsInvalidCellWithoutImportingAnyRow() {
        assertMessageContains(
            "CSV line 3: Value 'abc' is not a valid integer for column 'amount'; no row was imported",
            () -> dataTableRowService.importCsv(
                "items", "title,amount\nPear,1\nPlum,abc\nFig,2\n", DEV_ENVIRONMENT_ID));

        assertEquals(itemIds, findItemIds(List.of(), null));
    }

    @Test
    void testImportCsvRejectsHeaderThatMatchesNoColumnWithoutImportingAnyRow() {
        int createdEventCount = getWebhookEvents(DataTableWebhookType.RECORD_CREATED).size();

        assertMessageContains(
            "CSV header [title;amount] matches no column of table 'items'",
            () -> dataTableRowService.importCsv("items", "title;amount\nPear;1\nPlum;2\n", DEV_ENVIRONMENT_ID));

        assertEquals(itemIds, findItemIds(List.of(), null));
        assertEquals(createdEventCount, getWebhookEvents(DataTableWebhookType.RECORD_CREATED).size());
    }

    @Test
    void testUpdateRowsSetsTypedColumns() {
        List<DataTableRow> updatedDataTableRows = dataTableRowService.updateRows(
            "typed", DataTableRowConditions.of(List.of(filter("active", Operation.NOT_EMPTY, null))),
            Map.of("active", "no", "price", "3.25", "due", "2026-03-01", "created", "2026-03-01T09:30:00"),
            DEV_ENVIRONMENT_ID);

        List<Long> expectedIds = typedIds.subList(0, 2);

        assertEquals(
            expectedIds, updatedDataTableRows.stream()
                .map(DataTableRow::id)
                .toList());

        List<DataTableRow> matchingDataTableRows = dataTableRowService.findRows(
            "typed",
            DataTableRowConditions.of(
                List.of(
                    filter("active", Operation.EQUALS, false), filter("price", Operation.EQUALS, "3.25"),
                    filter("due", Operation.EQUALS, "2026-03-01"),
                    filter("created", Operation.EQUALS, "2026-03-01T09:30:00"))),
            null, 100, 0, DEV_ENVIRONMENT_ID);

        assertEquals(
            expectedIds, matchingDataTableRows.stream()
                .map(DataTableRow::id)
                .toList());
    }

    @Test
    void testExportCsvCanBeImportedAgain() {
        String csv = dataTableRowService.exportCsv("typed", DEV_ENVIRONMENT_ID);

        dataTableRowService.importCsv("typed", csv, DEV_ENVIRONMENT_ID);

        List<DataTableRow> dataTableRows = dataTableRowService.listRows("typed", 10, 0, DEV_ENVIRONMENT_ID);

        assertEquals(5, dataTableRows.size());
        assertEquals(
            dataTableRows.get(0)
                .values(),
            dataTableRows.get(3)
                .values());
        assertEquals(
            dataTableRows.get(1)
                .values(),
            dataTableRows.get(4)
                .values());
    }

    private static void assertMessageContains(String expectedMessagePart, Executable executable) {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, executable);

        String message = illegalArgumentException.getMessage();

        assertTrue(
            message.contains(expectedMessagePart),
            () -> "Expected message to contain '%s' but was '%s'".formatted(expectedMessagePart, message));
    }

    private List<DataTableWebhookEvent> getWebhookEvents(DataTableWebhookType dataTableWebhookType) {
        return applicationEvents.stream(DataTableWebhookEvent.class)
            .filter(dataTableWebhookEvent -> dataTableWebhookEvent.getType() == dataTableWebhookType)
            .toList();
    }

    private static Map<String, Object> nullValue(String columnName) {
        Map<String, Object> values = new HashMap<>();

        values.put(columnName, null);

        return values;
    }

    private static DataTableRowFilter filter(String column, Operation operation, Object value) {
        return new DataTableRowFilter(column, operation, value);
    }

    private List<Long> findItemIds(List<DataTableRowFilter> filters, DataTableRowSort sort) {
        List<DataTableRow> dataTableRows = dataTableRowService.findRows(
            "items", toConditions(filters), sort, 100, 0, DEV_ENVIRONMENT_ID);

        return dataTableRows.stream()
            .map(DataTableRow::id)
            .toList();
    }

    private List<Long> findItemIdsMatchingAny(List<List<DataTableRowFilter>> conditions) {
        List<DataTableRow> dataTableRows = dataTableRowService.findRows(
            "items", new DataTableRowConditions(conditions), null, 100, 0, DEV_ENVIRONMENT_ID);

        return dataTableRows.stream()
            .map(DataTableRow::id)
            .toList();
    }

    private List<Long> findTypedIds(List<DataTableRowFilter> filters) {
        List<DataTableRow> dataTableRows = dataTableRowService.findRows(
            "typed", toConditions(filters), null, 100, 0, DEV_ENVIRONMENT_ID);

        return dataTableRows.stream()
            .map(DataTableRow::id)
            .toList();
    }

    private void insertTyped(Boolean active, String price, String due, String created) {
        Map<String, Object> values = new HashMap<>();

        values.put("active", active);
        values.put("price", price);
        values.put("due", due);
        values.put("created", created);

        DataTableRow dataTableRow = dataTableRowService.insertRow("typed", values, DEV_ENVIRONMENT_ID);

        typedIds.add(dataTableRow.id());
    }

    private void insertItem(String title, int amount) {
        Map<String, Object> values = new HashMap<>();

        values.put("title", title);
        values.put("amount", amount);

        DataTableRow dataTableRow = dataTableRowService.insertRow("items", values, DEV_ENVIRONMENT_ID);

        itemIds.add(dataTableRow.id());
    }

    private static DataTableRowConditions toConditions(List<DataTableRowFilter> filters) {
        return filters.isEmpty() ? DataTableRowConditions.matchAll() : DataTableRowConditions.of(filters);
    }
}

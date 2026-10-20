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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

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
            "must be a single value, not a list",
            () -> findItemIds(List.of(filter("title", Operation.IN, List.of(List.of("a")))), null));
    }

    @Test
    void testFindRowsRejectsNegativePaging() {
        assertMessageContains(
            "must not be negative", () -> dataTableRowService.findRows("items", List.of(), null, -1, 0,
                DEV_ENVIRONMENT_ID));
        assertMessageContains(
            "must not be negative", () -> dataTableRowService.findRows("items", List.of(), null, 10, -1,
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
    }

    @Test
    void testFindRowsBindsParametersOfSeveralGroupsInOrder() {
        List<DataTableRow> dataTableRows = dataTableRowService.findRows(
            "items",
            List.of(
                List.of(filter("amount", Operation.IN, List.of(5, 20)), filter("title", Operation.NOT_EMPTY, null)),
                List.of(filter("amount", Operation.BETWEEN, List.of(25, 35)))),
            new DataTableRowSort("amount", Direction.DESC), 2, 1, DEV_ENVIRONMENT_ID);

        assertEquals(
            List.of(itemIds.get(2), itemIds.get(0)),
            dataTableRows.stream()
                .map(DataTableRow::id)
                .toList());
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
            "items", List.of(List.of(filter("amount", Operation.GREATER, 10))), DEV_ENVIRONMENT_ID);

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

        assertEquals(
            List.of(itemIds.get(2), itemIds.get(3), itemIds.get(4)),
            dataTableWebhookEvents.stream()
                .map(dataTableWebhookEvent -> dataTableWebhookEvent.getPayload()
                    .get("id"))
                .toList());
        assertEquals(
            deletedDataTableRows.getFirst()
                .values(),
            dataTableWebhookEvents.getFirst()
                .getPayload()
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

        List<List<DataTableRowFilter>> conditions = List.of(List.of(filter("amount", Operation.GREATER, 0)));

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
                "items", List.of(List.of(filter("amount", Operation.GREATER, 100))), DEV_ENVIRONMENT_ID));
        assertEquals(itemIds, findItemIds(List.of(), null));
        assertEquals(List.of(), getWebhookEvents(DataTableWebhookType.RECORD_DELETED));
    }

    @Test
    void testDeleteRowsRejectsMissingConditionsAndInvalidFilters() {
        assertMessageContains(
            "At least one condition is required",
            () -> dataTableRowService.deleteRows("items", List.of(), DEV_ENVIRONMENT_ID));
        assertMessageContains(
            "At least one condition is required",
            () -> dataTableRowService.deleteRows("items", List.of(List.of()), DEV_ENVIRONMENT_ID));
        assertMessageContains(
            "requires a value",
            () -> dataTableRowService.deleteRows(
                "items", List.of(List.of(filter("title", Operation.CONTAINS, ""))), DEV_ENVIRONMENT_ID));

        assertEquals(itemIds, findItemIds(List.of(), null));
    }

    @Test
    void testUpdateRowsUpdatesEveryMatchingRow() {
        List<DataTableRow> updatedDataTableRows = dataTableRowService.updateRows(
            "items", List.of(List.of(filter("amount", Operation.GREATER_EQUALS, 20))), Map.of("title", "sold"),
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
            "Apple", dataTableRowService.getRow("items", itemIds.get(0), DEV_ENVIRONMENT_ID)
                .values()
                .get("title"));

        List<DataTableWebhookEvent> dataTableWebhookEvents = getWebhookEvents(DataTableWebhookType.RECORD_UPDATED);

        assertEquals(
            List.of(itemIds.get(2), itemIds.get(3)),
            dataTableWebhookEvents.stream()
                .map(dataTableWebhookEvent -> dataTableWebhookEvent.getPayload()
                    .get("id"))
                .toList());
        assertEquals(
            updatedDataTableRows.getFirst()
                .values(),
            dataTableWebhookEvents.getFirst()
                .getPayload()
                .get("values"));
    }

    @Test
    void testUpdateRowsWithoutMatchUpdatesNothing() {
        assertEquals(
            List.of(),
            dataTableRowService.updateRows(
                "items", List.of(List.of(filter("amount", Operation.GREATER, 100))), Map.of("title", "sold"),
                DEV_ENVIRONMENT_ID));
        assertEquals(List.of(), getWebhookEvents(DataTableWebhookType.RECORD_UPDATED));
    }

    @Test
    void testUpdateRowsRejectsInvalidInputWithoutChangingRows() {
        List<List<DataTableRowFilter>> conditions = List.of(List.of(filter("amount", Operation.GREATER, 0)));

        assertMessageContains(
            "At least one condition is required",
            () -> dataTableRowService.updateRows("items", List.of(), Map.of("title", "x"), DEV_ENVIRONMENT_ID));
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

        assertEquals(
            List.of(itemIds.get(1)), findItemIds(List.of(filter("amount", Operation.EQUALS, 10)), null));
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

    private List<Long> findTypedIds(List<DataTableRowFilter> filters) {
        List<DataTableRow> dataTableRows = dataTableRowService.findRows(
            "typed", List.of(filters), null, 100, 0, DEV_ENVIRONMENT_ID);

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
}

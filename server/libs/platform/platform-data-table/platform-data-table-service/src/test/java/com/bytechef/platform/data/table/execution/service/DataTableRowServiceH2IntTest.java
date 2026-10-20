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
import com.bytechef.test.config.h2.H2DataSourceConfiguration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
@Import(H2DataSourceConfiguration.class)
@RecordApplicationEvents
class DataTableRowServiceH2IntTest {

    private static final long DEV_ENVIRONMENT_ID = 0;
    private static final long STAGE_ENVIRONMENT_ID = 1;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableRowService dataTableRowService;

    @AfterEach
    void afterEach() {
        dataTableService.dropTable("items", DEV_ENVIRONMENT_ID);
        dataTableService.dropTable("items", STAGE_ENVIRONMENT_ID);
        dataTableService.dropTable("typed", DEV_ENVIRONMENT_ID);
    }

    @BeforeEach
    void beforeEach() {
        dataTableService.dropTable("items", DEV_ENVIRONMENT_ID);
        dataTableService.createTable(
            "items", null,
            List.of(new ColumnSpec("title", ColumnType.STRING), new ColumnSpec("amount", ColumnType.INTEGER)),
            DEV_ENVIRONMENT_ID);
    }

    @Test
    void testRowsAreReadAndChangedInSeparateStatements() {
        DataTableRowService targetDataTableRowService = AopTestUtils.getUltimateTargetObject(dataTableRowService);

        assertEquals(
            Boolean.FALSE, ReflectionTestUtils.invokeMethod(targetDataTableRowService, "isReturningSupported"));
    }

    @Test
    void testFindRowsFiltersAndSorts() {
        long appleId = insertItem("Apple", 5);
        long offerId = insertItem("100%_off", 30);
        long untitledId = insertItem(null, null);

        assertEquals(
            List.of(appleId, untitledId),
            findItemIds(List.of(new DataTableRowFilter("title", Operation.NOT_EQUALS, "100%_off")), null));
        assertEquals(
            List.of(offerId), findItemIds(List.of(new DataTableRowFilter("title", Operation.CONTAINS, "%_")), null));
        assertEquals(
            List.of(appleId),
            findItemIds(List.of(new DataTableRowFilter("title", Operation.STARTS_WITH, "App")), null));
        assertEquals(
            List.of(appleId, offerId),
            findItemIds(List.of(new DataTableRowFilter("amount", Operation.IN, List.of("5", 30))), null));
        assertEquals(
            List.of(appleId),
            findItemIds(List.of(new DataTableRowFilter("amount", Operation.BETWEEN, List.of(1, 10))), null));
        assertEquals(
            List.of(untitledId), findItemIds(List.of(new DataTableRowFilter("title", Operation.EMPTY, null)), null));
        assertEquals(
            List.of(appleId),
            findItemIds(List.of(new DataTableRowFilter("title", Operation.EQUALS_IGNORE_CASE, "APPLE")), null));
        assertEquals(
            List.of(offerId, appleId, untitledId),
            findItemIds(List.of(), new DataTableRowSort("amount", Direction.DESC)));
    }

    @Test
    void testUpdateRowsAndDeleteRowsByConditions() {
        long appleId = insertItem("Apple", 5);
        long bananaId = insertItem("Banana", 20);
        long cherryId = insertItem("Cherry", 30);

        DataTableRowConditions conditions = DataTableRowConditions.of(
            List.of(new DataTableRowFilter("amount", Operation.GREATER, 10)));

        List<DataTableRow> updatedDataTableRows = dataTableRowService.updateRows(
            "items", conditions, Map.of("title", "sold"), DEV_ENVIRONMENT_ID);

        assertEquals(List.of(bananaId, cherryId), getIds(updatedDataTableRows));
        assertEquals("sold", updatedDataTableRows.getFirst()
            .values()
            .get("title"));
        assertEquals(
            "Apple", Objects.requireNonNull(dataTableRowService.getRow("items", appleId, DEV_ENVIRONMENT_ID))
                .values()
                .get("title"));

        List<DataTableRow> deletedDataTableRows = dataTableRowService.deleteRows(
            "items", conditions, DEV_ENVIRONMENT_ID);

        assertEquals(List.of(bananaId, cherryId), getIds(deletedDataTableRows));
        assertEquals(List.of(appleId), getIds(dataTableRowService.listRows("items", 100, 0, DEV_ENVIRONMENT_ID)));
        assertEquals(List.of(), dataTableRowService.deleteRows("items", conditions, DEV_ENVIRONMENT_ID));

        assertEquals(List.of(bananaId, cherryId), getWebhookEventIds(DataTableWebhookType.RECORD_UPDATED));
        assertEquals(List.of(bananaId, cherryId), getWebhookEventIds(DataTableWebhookType.RECORD_DELETED));
    }

    @Test
    void testUpdateRowsWithoutMatchUpdatesNothing() {
        long appleId = insertItem("Apple", 5);

        assertEquals(
            List.of(),
            dataTableRowService.updateRows(
                "items", DataTableRowConditions.of(List.of(new DataTableRowFilter("amount", Operation.GREATER, 100))),
                Map.of("title", "sold"), DEV_ENVIRONMENT_ID));
        assertEquals(
            "Apple", Objects.requireNonNull(dataTableRowService.getRow("items", appleId, DEV_ENVIRONMENT_ID))
                .values()
                .get("title"));
        assertEquals(List.of(), getWebhookEventIds(DataTableWebhookType.RECORD_UPDATED));
    }

    @Test
    void testUpdateRowsSetsSeveralColumnsOnRowsMatchingAnyGroup() {
        long appleId = insertItem("Apple", 5);
        long apricotId = insertItem("apricot", 10);
        long bananaId = insertItem("Banana", 20);
        long offerId = insertItem("100%_off", 30);

        List<DataTableRow> updatedDataTableRows = dataTableRowService.updateRows(
            "items",
            new DataTableRowConditions(
                List.of(
                    List.of(new DataTableRowFilter("amount", Operation.IN, List.of(5, 20))),
                    List.of(
                        new DataTableRowFilter("amount", Operation.BETWEEN, List.of(25, 35)),
                        new DataTableRowFilter("title", Operation.STARTS_WITH, "100")))),
            Map.of("title", "sold", "amount", 1), DEV_ENVIRONMENT_ID);

        assertEquals(List.of(appleId, bananaId, offerId), getIds(updatedDataTableRows));
        assertEquals(
            List.of(appleId, bananaId, offerId),
            findItemIds(
                List.of(
                    new DataTableRowFilter("title", Operation.EQUALS, "sold"),
                    new DataTableRowFilter("amount", Operation.EQUALS, 1)),
                null));
        assertEquals(
            List.of(apricotId), findItemIds(List.of(new DataTableRowFilter("amount", Operation.NOT_EQUALS, 1)), null));
    }

    @Test
    void testFindRowsSortsRowsWithoutValueLastInBothDirections() {
        long untitledId = insertItem(null, null);
        long appleId = insertItem("Apple", 5);
        long cherryId = insertItem("Cherry", 30);

        assertEquals(
            List.of(appleId, cherryId, untitledId),
            findItemIds(List.of(), new DataTableRowSort("amount", Direction.ASC)));
        assertEquals(
            List.of(cherryId, appleId, untitledId),
            findItemIds(List.of(), new DataTableRowSort("amount", Direction.DESC)));
    }

    @Test
    void testFindRowsTextAndComparisonOperations() {
        long appleId = insertItem("Apple", 5);
        long pathId = insertItem("C:\\temp", 10);
        long untitledId = insertItem(null, 20);

        assertEquals(
            List.of(pathId, untitledId),
            findItemIds(List.of(new DataTableRowFilter("title", Operation.NOT_CONTAINS, "pp")), null));
        assertEquals(
            List.of(appleId), findItemIds(List.of(new DataTableRowFilter("title", Operation.ENDS_WITH, "ple")), null));
        assertEquals(
            List.of(pathId), findItemIds(List.of(new DataTableRowFilter("title", Operation.CONTAINS, "\\")), null));
        assertEquals(
            List.of(appleId, pathId),
            findItemIds(List.of(new DataTableRowFilter("title", Operation.NOT_EMPTY, null)), null));
        assertEquals(
            List.of(appleId), findItemIds(List.of(new DataTableRowFilter("amount", Operation.LESS, 10)), null));
        assertEquals(
            List.of(appleId, pathId),
            findItemIds(List.of(new DataTableRowFilter("amount", Operation.LESS_EQUALS, 10)), null));
        assertEquals(
            List.of(pathId, untitledId),
            findItemIds(List.of(new DataTableRowFilter("amount", Operation.GREATER_EQUALS, 10)), null));
    }

    @Test
    void testFindRowsFiltersTypedColumns() {
        dataTableService.dropTable("typed", DEV_ENVIRONMENT_ID);
        dataTableService.createTable(
            "typed", null,
            List.of(
                new ColumnSpec("active", ColumnType.BOOLEAN), new ColumnSpec("price", ColumnType.NUMBER),
                new ColumnSpec("due", ColumnType.DATE), new ColumnSpec("created", ColumnType.DATE_TIME)),
            DEV_ENVIRONMENT_ID);

        long firstId = insertTyped(true, "1.5", "2026-01-10", "2026-01-10T08:00:00");
        long secondId = insertTyped(false, "2.5", "2026-02-10", "2026-02-10T08:00:00");

        assertEquals(List.of(firstId), findTypedIds(new DataTableRowFilter("active", Operation.EQUALS, "yes")));
        assertEquals(List.of(firstId), findTypedIds(new DataTableRowFilter("price", Operation.LESS, "2.5")));
        assertEquals(
            List.of(secondId), findTypedIds(new DataTableRowFilter("due", Operation.GREATER, "2026-01-31")));
        assertEquals(
            List.of(secondId),
            findTypedIds(new DataTableRowFilter("created", Operation.GREATER_EQUALS, "2026-02-10T08:00:00")));

        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class,
            () -> findTypedIds(new DataTableRowFilter("due", Operation.GREATER, 1760000000L)));

        assertTrue(illegalArgumentException.getMessage()
            .startsWith("Value '1760000000' is not a valid date for column 'due'"));
    }

    @Test
    void testDeleteRowsAndUpdateRowsLeaveOtherEnvironmentsUnchanged() {
        insertItem("Apple", 5);

        dataTableService.dropTable("items", STAGE_ENVIRONMENT_ID);
        dataTableService.createTable(
            "items", null,
            List.of(new ColumnSpec("title", ColumnType.STRING), new ColumnSpec("amount", ColumnType.INTEGER)),
            STAGE_ENVIRONMENT_ID);

        DataTableRow stageDataTableRow = dataTableRowService.insertRow(
            "items", Map.of("title", "Stage", "amount", 50), STAGE_ENVIRONMENT_ID);

        DataTableRowConditions conditions = DataTableRowConditions.of(
            List.of(new DataTableRowFilter("amount", Operation.GREATER, 0)));

        dataTableRowService.updateRows("items", conditions, Map.of("title", "changed"), DEV_ENVIRONMENT_ID);
        dataTableRowService.deleteRows("items", conditions, DEV_ENVIRONMENT_ID);

        assertEquals(List.of(), dataTableRowService.listRows("items", 10, 0, DEV_ENVIRONMENT_ID));
        assertEquals(List.of(stageDataTableRow), dataTableRowService.listRows("items", 10, 0, STAGE_ENVIRONMENT_ID));
    }

    @Test
    void testImportCsvRejectsHeaderThatMatchesNoColumnWithoutImportingAnyRow() {
        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class,
            () -> dataTableRowService.importCsv("items", "title;amount\nPear;1\n", DEV_ENVIRONMENT_ID));

        assertTrue(illegalArgumentException.getMessage()
            .startsWith("CSV header [title;amount] matches no column of table 'items'"));
        assertTrue(illegalArgumentException.getMessage()
            .endsWith("; no row was imported"));
        assertEquals(List.of(), dataTableRowService.listRows("items", 10, 0, DEV_ENVIRONMENT_ID));
    }

    @Test
    void testImportCsvRejectsInvalidCellWithoutImportingAnyRow() {
        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class,
            () -> dataTableRowService.importCsv("items", "title,amount\nPear,1\nPlum,abc\n", DEV_ENVIRONMENT_ID));

        assertTrue(illegalArgumentException.getMessage()
            .startsWith("CSV line 3: Value 'abc' is not a valid integer for column 'amount'"));
        assertEquals(List.of(), dataTableRowService.listRows("items", 10, 0, DEV_ENVIRONMENT_ID));
    }

    @Test
    void testExportCsvCanBeImportedAgain() {
        dataTableService.dropTable("typed", DEV_ENVIRONMENT_ID);
        dataTableService.createTable(
            "typed", null,
            List.of(
                new ColumnSpec("active", ColumnType.BOOLEAN), new ColumnSpec("price", ColumnType.NUMBER),
                new ColumnSpec("due", ColumnType.DATE), new ColumnSpec("created", ColumnType.DATE_TIME)),
            DEV_ENVIRONMENT_ID);

        insertTyped(true, "1.5", "2026-01-10", "2026-01-10T08:00:00");
        insertTyped(false, "2.5", "2026-02-10", "2026-02-10T08:00:30.250");

        String csv = dataTableRowService.exportCsv("typed", DEV_ENVIRONMENT_ID);

        dataTableRowService.importCsv("typed", csv, DEV_ENVIRONMENT_ID);

        List<DataTableRow> dataTableRows = dataTableRowService.listRows("typed", 10, 0, DEV_ENVIRONMENT_ID);

        assertEquals(4, dataTableRows.size());
        assertEquals(
            dataTableRows.get(0)
                .values(),
            dataTableRows.get(2)
                .values());
        assertEquals(
            dataTableRows.get(1)
                .values(),
            dataTableRows.get(3)
                .values());
    }

    @Test
    void testDeleteRowPublishesTheDeletedRowValues() {
        DataTableRow dataTableRow = dataTableRowService.insertRow(
            "items", Map.of("title", "Apple", "amount", 5), DEV_ENVIRONMENT_ID);

        assertTrue(dataTableRowService.deleteRow("items", dataTableRow.id(), DEV_ENVIRONMENT_ID));

        List<DataTableWebhookEvent> dataTableWebhookEvents = getWebhookEvents(DataTableWebhookType.RECORD_DELETED);

        assertEquals(1, dataTableWebhookEvents.size());
        assertEquals(
            List.of(Map.of("id", dataTableRow.id(), "values", dataTableRow.values())),
            dataTableWebhookEvents.getFirst()
                .getPayloads());
    }

    @Test
    void testDeleteRowOfMissingRowReturnsFalseAndPublishesNothing() {
        assertFalse(dataTableRowService.deleteRow("items", Long.MAX_VALUE, DEV_ENVIRONMENT_ID));

        assertTrue(getWebhookEvents(DataTableWebhookType.RECORD_DELETED).isEmpty());
    }

    @Test
    void testUpdateRowsReturnsRowsThatNoLongerMatchTheConditions() {
        insertItem("Apple", 5);

        long bananaId = insertItem("Banana", 20);
        long cherryId = insertItem("Cherry", 30);

        List<DataTableRow> updatedDataTableRows = dataTableRowService.updateRows(
            "items", DataTableRowConditions.of(List.of(new DataTableRowFilter("amount", Operation.GREATER, 10))),
            Map.of("amount", 0), DEV_ENVIRONMENT_ID);

        assertEquals(List.of(bananaId, cherryId), getIds(updatedDataTableRows));
        assertTrue(updatedDataTableRows.stream()
            .allMatch(dataTableRow -> dataTableRow.values()
                .get("amount") instanceof Number amount && amount.longValue() == 0));
    }

    private static List<Long> getIds(List<DataTableRow> dataTableRows) {
        return dataTableRows.stream()
            .map(DataTableRow::id)
            .toList();
    }

    private List<Long> findItemIds(List<DataTableRowFilter> filters, DataTableRowSort sort) {
        return getIds(
            dataTableRowService.findRows(
                "items", toConditions(filters), sort, 100, 0, DEV_ENVIRONMENT_ID));
    }

    private List<Long> findTypedIds(DataTableRowFilter filter) {
        return getIds(
            dataTableRowService.findRows(
                "typed", DataTableRowConditions.of(List.of(filter)), null, 100, 0, DEV_ENVIRONMENT_ID));
    }

    private List<DataTableWebhookEvent> getWebhookEvents(DataTableWebhookType dataTableWebhookType) {
        return applicationEvents.stream(DataTableWebhookEvent.class)
            .filter(dataTableWebhookEvent -> dataTableWebhookEvent.getType() == dataTableWebhookType)
            .toList();
    }

    private List<Long> getWebhookEventIds(DataTableWebhookType dataTableWebhookType) {
        return applicationEvents.stream(DataTableWebhookEvent.class)
            .filter(dataTableWebhookEvent -> dataTableWebhookEvent.getType() == dataTableWebhookType)
            .flatMap(dataTableWebhookEvent -> dataTableWebhookEvent.getPayloads()
                .stream())
            .map(payload -> (Long) payload.get("id"))
            .toList();
    }

    private long insertTyped(Boolean active, String price, String due, String created) {
        Map<String, Object> values = new HashMap<>();

        values.put("active", active);
        values.put("price", price);
        values.put("due", due);
        values.put("created", created);

        DataTableRow dataTableRow = dataTableRowService.insertRow("typed", values, DEV_ENVIRONMENT_ID);

        return dataTableRow.id();
    }

    private long insertItem(String title, Integer amount) {
        Map<String, Object> values = new HashMap<>();

        values.put("title", title);
        values.put("amount", amount);

        DataTableRow dataTableRow = dataTableRowService.insertRow("items", values, DEV_ENVIRONMENT_ID);

        return dataTableRow.id();
    }

    private static DataTableRowConditions toConditions(List<DataTableRowFilter> filters) {
        return filters.isEmpty() ? DataTableRowConditions.matchAll() : DataTableRowConditions.of(filters);
    }
}

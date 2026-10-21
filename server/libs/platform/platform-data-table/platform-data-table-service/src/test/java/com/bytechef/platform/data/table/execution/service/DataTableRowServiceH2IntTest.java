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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mockingDetails;

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
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

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
    void testHostileColumnNamesAreRejected() {
        long appleId = insertItem("Apple", 5);

        for (String columnName : List.of("title\" OR \"1\"=\"1", "title; DROP TABLE items")) {
            assertThrows(
                IllegalArgumentException.class,
                () -> findItemIds(List.of(new DataTableRowFilter(columnName, Operation.EQUALS, "x")), null));
            assertThrows(
                IllegalArgumentException.class,
                () -> findItemIds(List.of(), new DataTableRowSort(columnName, Direction.ASC)));
            IllegalArgumentException updateValuesException = assertThrows(
                IllegalArgumentException.class,
                () -> dataTableRowService.updateRows(
                    "items", toConditions(List.of(new DataTableRowFilter("title", Operation.EQUALS, "Apple"))),
                    Map.of(columnName, "x"), DEV_ENVIRONMENT_ID));

            assertTrue(
                updateValuesException.getMessage()
                    .contains("Unknown column"),
                updateValuesException.getMessage());
        }

        assertEquals(
            List.of(appleId), findItemIds(List.of(new DataTableRowFilter("title", Operation.EQUALS, "Apple")), null));
        assertTrue(getWebhookEventIds(DataTableWebhookType.RECORD_UPDATED).isEmpty());
    }

    @Test
    void testInsertRowsRejectsInvalidRowWithoutInsertingAnyRow() {
        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class,
            () -> dataTableRowService.insertRows(
                "items", List.of(Map.of("title", "Pear", "amount", 1), Map.of("title", "Plum", "amount", "abc")),
                DEV_ENVIRONMENT_ID));

        assertTrue(
            illegalArgumentException.getMessage()
                .startsWith("Record 2: "),
            illegalArgumentException.getMessage());
        assertEquals(List.of(), findItemIds(List.of(), null));
        assertTrue(getWebhookEventIds(DataTableWebhookType.RECORD_CREATED).isEmpty());
    }

    @Test
    void testInsertRowsNamesTheRecordThatTheDatabaseRejectsWithoutInsertingAnyRow() {
        DataIntegrityViolationException dataIntegrityViolationException = assertThrows(
            DataIntegrityViolationException.class,
            () -> dataTableRowService.insertRows(
                "items", List.of(Map.of("title", "Pear"), Map.of("title", "x".repeat(300))), DEV_ENVIRONMENT_ID));

        String message = dataIntegrityViolationException.getMessage();

        assertTrue(message.startsWith("Record 2: "), message);
        assertTrue(message.endsWith("; no record was inserted"), message);
        assertEquals(List.of(), findItemIds(List.of(), null));
        assertTrue(getWebhookEventIds(DataTableWebhookType.RECORD_CREATED).isEmpty());
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
        assertEquals(1, getWebhookEvents(DataTableWebhookType.RECORD_DELETED).size());
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
        assertEquals(List.of(), getWebhookEvents(DataTableWebhookType.RECORD_UPDATED));
    }

    @Test
    void testUpdateRowsSetsSeveralColumnsOnRowsMatchingAnyGroup() {
        long appleId = insertItem("Apple", 5);
        long apricotId = insertItem("apricot", 10);
        long bananaId = insertItem("Banana", 20);
        long offerId = insertItem("100%_off", 30);

        List<DataTableRow> updatedDataTableRows = dataTableRowService.updateRows(
            "items",
            DataTableRowConditions.anyOf(
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
        createTypedTable();

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
    void testFindRowsInMatchesEveryColumnType() {
        createTypedTable();

        long appleId = insertItem("Apple", 5);

        insertItem("Banana", 20);

        long firstId = insertTyped(true, "1.5", "2026-01-10", "2026-01-10T08:00:00");
        long secondId = insertTyped(false, "2.5", "2026-02-10", "2026-02-10T08:00:00");

        assertEquals(
            List.of(appleId),
            findItemIds(List.of(new DataTableRowFilter("title", Operation.IN, List.of("Apple", "Cherry"))), null));
        assertEquals(List.of(secondId), findTypedIds(new DataTableRowFilter("active", Operation.IN, List.of("no"))));
        assertEquals(
            List.of(firstId, secondId),
            findTypedIds(new DataTableRowFilter("price", Operation.IN, List.of("1.5", 2.5))));
        assertEquals(
            List.of(secondId),
            findTypedIds(new DataTableRowFilter("due", Operation.IN, List.of("2026-02-10", "2026-03-10"))));
        assertEquals(
            List.of(firstId),
            findTypedIds(new DataTableRowFilter("created", Operation.IN, List.of("2026-01-10T08:00:00"))));
    }

    @Test
    void testFindRowsUpdateRowsAndDeleteRowsAcceptMoreInValuesThanTheBindParameterLimit() {
        long appleId = insertItem("Apple", 5);
        long bananaId = insertItem("Banana", 20);
        long cherryId = insertItem("Cherry", 30);

        List<Object> ids = new ArrayList<>(LongStream.rangeClosed(1_000_000, 1_070_000)
            .boxed()
            .toList());

        ids.add(appleId);
        ids.add(cherryId);

        List<DataTableRowFilter> filters = List.of(new DataTableRowFilter("id", Operation.IN, ids));

        assertEquals(List.of(appleId, cherryId), findItemIds(filters, null));
        assertEquals(
            List.of(appleId, cherryId),
            getIds(
                dataTableRowService.updateRows(
                    "items", DataTableRowConditions.of(filters), Map.of("amount", 1), DEV_ENVIRONMENT_ID)));
        assertEquals(
            List.of(appleId, cherryId),
            getIds(dataTableRowService.deleteRows("items", DataTableRowConditions.of(filters), DEV_ENVIRONMENT_ID)));
        assertEquals(List.of(bananaId), findItemIds(List.of(), null));
    }

    @Test
    void testInValuesOverSeveralBindParametersStillNeedTheOtherConditionsOfTheGroup() {
        long appleId = insertItem("Apple", 5);
        long bananaId = insertItem("Banana", 20);
        long cherryId = insertItem("Cherry", 30);

        List<Object> ids = new ArrayList<>(List.of(appleId, cherryId));

        ids.addAll(
            LongStream.rangeClosed(1_000_000, 1_070_000)
                .boxed()
                .toList());

        DataTableRowConditions conditions = DataTableRowConditions.of(
            List.of(
                new DataTableRowFilter("id", Operation.IN, ids),
                new DataTableRowFilter("title", Operation.EQUALS, "Apple")));

        assertEquals(
            List.of(appleId),
            getIds(dataTableRowService.updateRows("items", conditions, Map.of("amount", 1), DEV_ENVIRONMENT_ID)));
        assertEquals(List.of(appleId), getIds(dataTableRowService.deleteRows("items", conditions, DEV_ENVIRONMENT_ID)));
        assertEquals(List.of(bananaId, cherryId), findItemIds(List.of(), null));
        assertEquals(
            List.of(cherryId), findItemIds(List.of(new DataTableRowFilter("amount", Operation.EQUALS, 30)), null));
    }

    @Test
    void testFindRowsEmptyAndNotEmptyTreatAnEmptyTextAsEmpty() {
        long appleId = insertItem("Apple", 5);
        long blankId = insertItem("", 10);
        long nullTitleId = insertItem(null, null);

        assertEquals(
            List.of(blankId, nullTitleId),
            findItemIds(List.of(new DataTableRowFilter("title", Operation.EMPTY, null)), null));
        assertEquals(
            List.of(appleId), findItemIds(List.of(new DataTableRowFilter("title", Operation.NOT_EMPTY, null)), null));
        assertEquals(
            List.of(nullTitleId), findItemIds(List.of(new DataTableRowFilter("amount", Operation.EMPTY, null)), null));
        assertEquals(
            List.of(appleId, blankId),
            findItemIds(List.of(new DataTableRowFilter("amount", Operation.NOT_EMPTY, null)), null));
    }

    @Test
    void testFindRowsComparesBooleansAndMatchesRowsWithoutValueForNotEquals() {
        createTypedTable();

        long activeId = insertTyped(true, "1.5", "2026-01-10", "2026-01-10T08:00:00");
        long inactiveId = insertTyped(false, "2.5", "2026-02-10", "2026-02-10T08:00:00");
        long unknownId = insertTyped(null, "3.5", "2026-03-10", "2026-03-10T08:00:00");

        assertEquals(List.of(activeId), findTypedIds(new DataTableRowFilter("active", Operation.EQUALS, true)));
        assertEquals(
            List.of(inactiveId, unknownId), findTypedIds(new DataTableRowFilter("active", Operation.NOT_EQUALS, true)));
    }

    @Test
    void testFindRowsBetweenIncludesBothDateBounds() {
        createTypedTable();

        insertTyped(true, "1.5", "2026-01-10", "2026-01-10T08:00:00");

        long secondId = insertTyped(false, "2.5", "2026-02-10", "2026-02-10T08:00:00");
        long thirdId = insertTyped(true, "3.5", "2026-03-10", "2026-03-10T08:00:00");

        assertEquals(
            List.of(secondId, thirdId),
            findTypedIds(new DataTableRowFilter("due", Operation.BETWEEN, List.of("2026-02-10", "2026-03-10"))));
        assertEquals(
            List.of(secondId),
            findTypedIds(
                new DataTableRowFilter(
                    "created", Operation.BETWEEN, List.of("2026-02-10T08:00:00", "2026-03-10T07:59:59"))));
    }

    @Test
    void testDateColumnsConvertDateTimesWithAnOffsetToUtcBeforeTakingTheDate() {
        createTypedTable();

        insertTyped(true, "1.5", "2026-01-31", "2026-01-31T08:00:00");

        long februaryId = insertTyped(false, "2.5", "2026-02-01", "2026-02-01T08:00:00");

        assertEquals(
            List.of(februaryId),
            findTypedIds(new DataTableRowFilter("due", Operation.EQUALS, "2026-01-31T23:30:00-05:00")));
        assertEquals(
            List.of(februaryId), findTypedIds(new DataTableRowFilter("due", Operation.EQUALS, "2026-02-01T10:00:00")));
    }

    @Test
    void testFindRowsWithLimitZeroReturnsNoRows() {
        insertItem("Apple", 5);

        assertEquals(
            List.of(),
            dataTableRowService.findRows("items", DataTableRowConditions.matchAll(), null, 0, 0, DEV_ENVIRONMENT_ID));
    }

    @Test
    void testUpdateRowsAndDeleteRowsChangeMoreRowsThanTheBindParameterLimit() {
        int rowCount = 50_001;

        StringBuilder csv = new StringBuilder("title,amount\n");

        for (int index = 0; index < rowCount; index++) {
            csv.append("Item ")
                .append(index)
                .append(",1\n");
        }

        dataTableRowService.importCsv("items", csv.toString(), DEV_ENVIRONMENT_ID);

        long bananaId = insertItem("Banana", 20);

        List<DataTableRow> updatedDataTableRows = dataTableRowService.updateRows(
            "items", toConditions(List.of(new DataTableRowFilter("amount", Operation.EQUALS, 1))),
            Map.of("amount", 2), DEV_ENVIRONMENT_ID);

        assertEquals(rowCount, updatedDataTableRows.size());

        List<DataTableRow> deletedDataTableRows = dataTableRowService.deleteRows(
            "items", toConditions(List.of(new DataTableRowFilter("amount", Operation.EQUALS, 2))),
            DEV_ENVIRONMENT_ID);

        assertEquals(rowCount, deletedDataTableRows.size());
        assertEquals(List.of(bananaId), findItemIds(List.of(), null));
    }

    @Test
    void testInsertRowRejectsWholeNumbersTooSmallToBeEpochMilliseconds() {
        createTypedTable();

        IllegalArgumentException compactDateException = assertThrows(
            IllegalArgumentException.class,
            () -> dataTableRowService.insertRow("typed", Map.of("due", "20260131"), DEV_ENVIRONMENT_ID));

        assertTrue(
            compactDateException.getMessage()
                .contains("Value '20260131' is not a valid date for column 'due'"),
            compactDateException.getMessage());

        IllegalArgumentException epochSecondsException = assertThrows(
            IllegalArgumentException.class,
            () -> dataTableRowService.insertRow("typed", Map.of("created", 1769846400L), DEV_ENVIRONMENT_ID));

        assertTrue(
            epochSecondsException.getMessage()
                .contains("Value '1769846400' is not a valid date_time for column 'created'"),
            epochSecondsException.getMessage());
        assertEquals(List.of(), findTypedIds(new DataTableRowFilter("due", Operation.NOT_EMPTY, null)));
    }

    @Test
    void testMissingTableIsReportedByItsName() {
        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class,
            () -> dataTableRowService.findRows(
                "missing", DataTableRowConditions.matchAll(), null, 100, 0, DEV_ENVIRONMENT_ID));

        assertEquals("Table 'missing' not found", illegalArgumentException.getMessage());
    }

    @Test
    void testFindRowsMatchesTheLastInValueAtEveryChunkBoundary() {
        long appleId = insertItem("Apple", 5);

        insertItem("Banana", 20);

        int chunkLength = DataTableRowServiceImpl.MAX_ARRAY_PARAMETER_LENGTH;

        for (int valueCount : List.of(chunkLength, chunkLength + 1, chunkLength * 2, chunkLength * 2 + 1)) {
            List<Object> ids = new ArrayList<>(LongStream.range(1_000_000, 1_000_000 + valueCount - 1)
                .boxed()
                .toList());

            ids.add(appleId);

            assertEquals(
                List.of(appleId), findItemIds(List.of(new DataTableRowFilter("id", Operation.IN, ids)), null),
                "IN with " + valueCount + " values");
        }
    }

    @Test
    void testFindRowsMatchesTheLastInValueBeyondOneChunkForTextAndDateColumns() {
        int valueCount = DataTableRowServiceImpl.MAX_ARRAY_PARAMETER_LENGTH + 1;

        long appleId = insertItem("Apple", 5);

        insertItem("Banana", 20);

        List<Object> titles = new ArrayList<>(IntStream.range(0, valueCount - 1)
            .mapToObj(index -> "Missing " + index)
            .toList());

        titles.add("Apple");

        assertEquals(List.of(appleId),
            findItemIds(List.of(new DataTableRowFilter("title", Operation.IN, titles)), null));

        createTypedTable();

        long typedId = insertTyped(true, "1.5", "2026-01-10", "2026-01-10T08:00:00");

        insertTyped(false, "2.5", "2026-02-10", "2026-02-10T08:00:00");

        LocalDate firstDate = LocalDate.of(1800, 1, 1);

        List<Object> dates = new ArrayList<>(IntStream.range(0, valueCount - 1)
            .mapToObj(index -> String.valueOf(firstDate.plusDays(index)))
            .toList());

        dates.add("2026-01-10");

        assertEquals(List.of(typedId), findTypedIds(new DataTableRowFilter("due", Operation.IN, dates)));

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        LocalDateTime firstDateTime = LocalDateTime.of(1990, 1, 1, 0, 0);

        List<Object> dateTimes = new ArrayList<>(IntStream.range(0, valueCount - 1)
            .mapToObj(index -> dateTimeFormatter.format(firstDateTime.plusSeconds(index)))
            .toList());

        dateTimes.add("2026-01-10T08:00:00");

        assertEquals(List.of(typedId), findTypedIds(new DataTableRowFilter("created", Operation.IN, dateTimes)));
    }

    @Test
    void testInsertRowsOfNoRowsInsertsNothingAndPublishesNoEvent() {
        assertEquals(List.of(), dataTableRowService.insertRows("items", List.of(), DEV_ENVIRONMENT_ID));
        assertEquals(0, applicationEvents.stream(DataTableWebhookEvent.class)
            .count());
    }

    @Test
    void testDateTimesWithAnOffsetAreConvertedToUtc() {
        createTypedTable();

        long firstId = insertTyped(true, "1.5", "2026-01-10", "2026-01-10T08:00:00");
        long secondId = insertTyped(false, "2.5", "2026-02-10", "2026-02-10T08:00:00");

        assertEquals(
            List.of(secondId),
            findTypedIds(new DataTableRowFilter("created", Operation.EQUALS, "2026-02-10T10:00:00+02:00")));
        assertEquals(
            List.of(secondId),
            findTypedIds(new DataTableRowFilter("created", Operation.EQUALS, "2026-02-10T08:00:00Z")));

        dataTableRowService.updateRows(
            "typed", DataTableRowConditions.of(List.of(new DataTableRowFilter("active", Operation.EQUALS, true))),
            Map.of("created", "2026-03-01T11:30:00+02:00"), DEV_ENVIRONMENT_ID);

        assertEquals(
            List.of(firstId), findTypedIds(new DataTableRowFilter("created", Operation.EQUALS, "2026-03-01T09:30:00")));
    }

    @Test
    void testDateTimesWithAnOffsetAreConvertedToUtcInADaylightSavingGapOfTheDefaultZone() {
        createTypedTable();

        long firstId = insertTyped(true, "1.5", "2026-01-10", "2026-01-10T08:00:00");

        TimeZone defaultTimeZone = TimeZone.getDefault();

        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));

            dataTableRowService.updateRows(
                "typed", DataTableRowConditions.of(List.of(new DataTableRowFilter("active", Operation.EQUALS, true))),
                Map.of("created", "2026-03-29T02:30:00Z"), DEV_ENVIRONMENT_ID);

            assertEquals(
                List.of(firstId),
                findTypedIds(new DataTableRowFilter("created", Operation.LESS, "2026-03-29T03:15:00Z")));
            assertEquals(
                List.of(firstId),
                findTypedIds(new DataTableRowFilter("created", Operation.IN, List.of("2026-03-29T02:30:00Z"))));
        } finally {
            TimeZone.setDefault(defaultTimeZone);
        }
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
    void testImportCsvNamesTheLineThatTheDatabaseRejectsWithoutImportingAnyRow() {
        DataIntegrityViolationException dataIntegrityViolationException = assertThrows(
            DataIntegrityViolationException.class,
            () -> dataTableRowService.importCsv(
                "items", "title,amount\nPear,1\n" + "x".repeat(300) + ",2\n", DEV_ENVIRONMENT_ID));

        String message = dataIntegrityViolationException.getMessage();

        assertTrue(message.startsWith("CSV line 3: "), message);
        assertTrue(message.endsWith("; no row was imported"), message);
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
            List.of(dataTableRow), dataTableWebhookEvents.getFirst()
                .getRows());
    }

    @Test
    void testInsertRowReturnsAndPublishesTheStoredValues() {
        createTypedTable();

        DataTableRow dataTableRow = dataTableRowService.insertRow(
            "typed", Map.of("active", true, "price", "1.5", "due", "2026-01-10", "created", "2026-01-10T08:00:00"),
            DEV_ENVIRONMENT_ID);

        Map<String, Object> values = dataTableRow.values();

        assertEquals(Set.of("active", "price", "due", "created"), values.keySet());
        assertEquals(true, values.get("active"));
        assertEquals(0, new BigDecimal("1.5").compareTo(new BigDecimal(String.valueOf(values.get("price")))));
        assertEquals(dataTableRowService.getRow("typed", dataTableRow.id(), DEV_ENVIRONMENT_ID), dataTableRow);

        List<DataTableWebhookEvent> dataTableWebhookEvents = getWebhookEvents(DataTableWebhookType.RECORD_CREATED);

        assertEquals(
            List.of(dataTableRow), dataTableWebhookEvents.getLast()
                .getRows());
    }

    @Test
    void testDeleteAllRowsDeletesEveryRowAndPublishesOneEvent() {
        List<Long> ids = List.of(insertItem("Apple", 5), insertItem("Banana", 20));

        List<DataTableRow> deletedDataTableRows = dataTableRowService.deleteAllRows("items", DEV_ENVIRONMENT_ID);

        assertEquals(ids, getIds(deletedDataTableRows));
        assertEquals(List.of(), dataTableRowService.listRows("items", 10, 0, DEV_ENVIRONMENT_ID));

        List<DataTableWebhookEvent> dataTableWebhookEvents = getWebhookEvents(DataTableWebhookType.RECORD_DELETED);

        assertEquals(1, dataTableWebhookEvents.size());
        assertEquals(
            ids, dataTableWebhookEvents.getFirst()
                .getRows()
                .stream()
                .map(DataTableRow::id)
                .toList());
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

    @Test
    void testUpdateRowReturnsTheUpdatedValues() {
        long appleId = insertItem("Apple", 5);

        insertItem("Banana", 20);

        DataTableRow updatedDataTableRow = dataTableRowService.updateRow(
            "items", appleId, Map.of("title", "Green Apple"), DEV_ENVIRONMENT_ID);

        assertEquals(appleId, updatedDataTableRow.id());
        assertEquals("Green Apple", updatedDataTableRow.values()
            .get("title"));
        assertEquals(5L, ((Number) updatedDataTableRow.values()
            .get("amount")).longValue());

        DataTableRow fetchedDataTableRow = Objects.requireNonNull(
            dataTableRowService.getRow("items", appleId, DEV_ENVIRONMENT_ID));

        assertEquals("Green Apple", fetchedDataTableRow.values()
            .get("title"));
        assertEquals(List.of(appleId), getWebhookEventIds(DataTableWebhookType.RECORD_UPDATED));
    }

    @Test
    void testUpdateRowClearsColumnsGivenNullOrBlankValues() {
        long appleId = insertItem("Apple", 5);
        long bananaId = insertItem("Banana", 20);

        DataTableRow dataTableRow = dataTableRowService.updateRow(
            "items", appleId, Map.of("amount", "  ", "title", ""), DEV_ENVIRONMENT_ID);

        assertNull(dataTableRow.values()
            .get("amount"));
        assertEquals("", dataTableRow.values()
            .get("title"));

        Map<String, Object> values = new HashMap<>();

        values.put("amount", null);

        dataTableRow = dataTableRowService.updateRow("items", bananaId, values, DEV_ENVIRONMENT_ID);

        assertNull(dataTableRow.values()
            .get("amount"));
        assertEquals("Banana", dataTableRow.values()
            .get("title"));
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
            .flatMap(dataTableWebhookEvent -> dataTableWebhookEvent.getRows()
                .stream())
            .map(DataTableRow::id)
            .toList();
    }

    private void createTypedTable() {
        dataTableService.dropTable("typed", DEV_ENVIRONMENT_ID);
        dataTableService.createTable(
            "typed", null,
            List.of(
                new ColumnSpec("active", ColumnType.BOOLEAN), new ColumnSpec("price", ColumnType.NUMBER),
                new ColumnSpec("due", ColumnType.DATE), new ColumnSpec("created", ColumnType.DATE_TIME)),
            DEV_ENVIRONMENT_ID);
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

    @Nested
    class WithJdbcTemplateSpy {

        @MockitoSpyBean
        private JdbcTemplate jdbcTemplate;

        @Test
        void testRowsAreReadAndChangedInSeparateStatements() {
            clearInvocations(jdbcTemplate);

            long appleId = insertItem("Apple", 5);

            assertTrue(countUpdates() > 0);

            clearInvocations(jdbcTemplate);

            dataTableRowService.updateRow("items", appleId, Map.of("title", "Banana"), DEV_ENVIRONMENT_ID);

            assertTrue(countUpdates() > 0);

            clearInvocations(jdbcTemplate);

            dataTableRowService.deleteRow("items", appleId, DEV_ENVIRONMENT_ID);

            assertTrue(countUpdates() > 0);
        }

        private long countUpdates() {
            return mockingDetails(jdbcTemplate).getInvocations()
                .stream()
                .filter(invocation -> {
                    Method method = invocation.getMethod();

                    return "update".equals(method.getName());
                })
                .count();
        }
    }
}

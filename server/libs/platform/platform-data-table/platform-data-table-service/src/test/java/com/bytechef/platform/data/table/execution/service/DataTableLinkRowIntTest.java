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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.config.DataTableIntTestUtils;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.LinkDeleteRule;
import com.bytechef.platform.data.table.domain.LinkSpec;
import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter.Operation;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort.Direction;
import com.bytechef.platform.data.table.execution.domain.NewRow;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class DataTableLinkRowIntTest {

    private static final long ENVIRONMENT_ID = 0;
    private static final long MISSING_ID = 987_654_321L;
    private static final long WORKSPACE_ID = 1L;

    @Autowired
    private DataTableRowService dataTableRowService;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long customersId;
    private long firstCustomerId;
    private long ordersId;
    private long secondCustomerId;

    @BeforeEach
    void beforeEach() {
        DataTableIntTestUtils.dropAll(jdbcTemplate);

        customersId = dataTableService.createTable(
            WORKSPACE_ID, "customers", null, List.of(new ColumnSpec("name", ColumnType.STRING)), ENVIRONMENT_ID);
        ordersId = dataTableService.createTable(
            WORKSPACE_ID, "orders", null, List.of(new ColumnSpec("title", ColumnType.STRING)), ENVIRONMENT_ID);

        dataTableService.addColumn(ordersId, customerLink("customer", LinkDeleteRule.BLOCK), ENVIRONMENT_ID);

        firstCustomerId = dataTableRowService.insertRow(
            customersId, Map.of("name", "Ann"), "c-1", ENVIRONMENT_ID)
            .id();
        secondCustomerId = dataTableRowService.insertRow(
            customersId, Map.of("name", "Bob"), "c-2", ENVIRONMENT_ID)
            .id();
    }

    @Test
    void testInsertRowLinksById() {
        DataTableRow dataTableRow = dataTableRowService.insertRow(
            ordersId, Map.of("title", "o1", "customer", firstCustomerId), ENVIRONMENT_ID);

        assertEquals(firstCustomerId, dataTableRow.values()
            .get("customer"));
    }

    @Test
    void testInsertRowLinksByExternalId() {
        DataTableRow dataTableRow = dataTableRowService.insertRow(
            ordersId, Map.of("title", "o1", "customer", Map.of("externalId", "c-1")), ENVIRONMENT_ID);

        assertEquals(firstCustomerId, dataTableRow.values()
            .get("customer"));
    }

    @Test
    void testInsertRowLinksByNumericString() {
        DataTableRow dataTableRow = dataTableRowService.insertRow(
            ordersId, Map.of("title", "o1", "customer", String.valueOf(secondCustomerId)), ENVIRONMENT_ID);

        assertEquals(secondCustomerId, dataTableRow.values()
            .get("customer"));
    }

    @Test
    void testInsertRowRejectsNonNumericLinkValue() {
        assertErrorType(
            DataTableErrorType.ROW_VALUE_INVALID,
            () -> dataTableRowService.insertRow(
                ordersId, Map.of("title", "o1", "customer", "not-a-number"), ENVIRONMENT_ID));
        assertErrorType(
            DataTableErrorType.ROW_VALUE_INVALID,
            () -> dataTableRowService.insertRow(
                ordersId, Map.of("title", "o1", "customer", Map.of("id", 1)), ENVIRONMENT_ID));
        assertEquals(0, countOrders());
    }

    @Test
    void testInsertRowRejectsMissingTargetId() {
        assertErrorType(
            DataTableErrorType.LINKED_ROW_NOT_FOUND,
            () -> dataTableRowService.insertRow(
                ordersId, Map.of("title", "o1", "customer", MISSING_ID), ENVIRONMENT_ID));
        assertEquals(0, countOrders());
    }

    @Test
    void testInsertRowMissingTargetMessageNamesColumnAndValue() {
        DataTableException dataTableException = assertThrows(
            DataTableException.class,
            () -> dataTableRowService.insertRow(
                ordersId, Map.of("title", "o1", "customer", MISSING_ID), ENVIRONMENT_ID));

        assertEquals(DataTableErrorType.LINKED_ROW_NOT_FOUND, dataTableException.getErrorType());
        assertThat(dataTableException.getMessage()).contains("customer=" + MISSING_ID);
    }

    @Test
    void testInsertRowRejectsUnknownExternalId() {
        assertErrorType(
            DataTableErrorType.LINKED_ROW_NOT_FOUND,
            () -> dataTableRowService.insertRow(
                ordersId, Map.of("title", "o1", "customer", Map.of("externalId", "missing")), ENVIRONMENT_ID));
        assertEquals(0, countOrders());
    }

    @Test
    void testInsertRowsWithOneMissingTargetInsertsNothing() {
        for (CreateStrategy createStrategy : List.of(CreateStrategy.INSERT, CreateStrategy.UPSERT)) {
            DataTableException missingIdException = assertThrows(
                DataTableException.class,
                () -> dataTableRowService.insertRows(
                    ordersId,
                    List.of(
                        new NewRow(Map.of("title", "o1", "customer", firstCustomerId), "o-1"),
                        new NewRow(Map.of("title", "o2", "customer", MISSING_ID), "o-2")),
                    createStrategy, ENVIRONMENT_ID));

            assertEquals(DataTableErrorType.LINKED_ROW_NOT_FOUND, missingIdException.getErrorType());
            assertThat(missingIdException.getMessage()).contains("Record 2");
            assertEquals(0, countOrders());

            DataTableException unknownExternalIdException = assertThrows(
                DataTableException.class,
                () -> dataTableRowService.insertRows(
                    ordersId,
                    List.of(
                        new NewRow(Map.of("title", "o1", "customer", firstCustomerId), "o-1"),
                        new NewRow(Map.of("title", "o2", "customer", Map.of("externalId", "missing")), "o-2")),
                    createStrategy, ENVIRONMENT_ID));

            assertEquals(DataTableErrorType.LINKED_ROW_NOT_FOUND, unknownExternalIdException.getErrorType());
            assertThat(unknownExternalIdException.getMessage()).contains("Record 2");
            assertEquals(0, countOrders());
        }
    }

    @Test
    void testInsertRowsResolvesExternalIdsInOneBatch() {
        List<DataTableRow> dataTableRows = dataTableRowService.insertRows(
            ordersId,
            List.of(
                Map.of("title", "o1", "customer", Map.of("externalId", "c-1")),
                Map.of("title", "o2", "customer", Map.of("externalId", "c-2")),
                Map.of("title", "o3", "customer", secondCustomerId)),
            ENVIRONMENT_ID);

        assertThat(dataTableRows)
            .extracting(dataTableRow -> dataTableRow.values()
                .get("customer"))
            .containsExactly(firstCustomerId, secondCustomerId, secondCustomerId);
    }

    @Test
    void testUpdateRowAndUpsertRowResolveLinks() {
        DataTableRow dataTableRow = dataTableRowService.insertRow(
            ordersId, Map.of("title", "o1", "customer", firstCustomerId), ENVIRONMENT_ID);

        DataTableRow updatedDataTableRow = dataTableRowService.updateRow(
            ordersId, dataTableRow.id(), Map.of("customer", Map.of("externalId", "c-2")), ENVIRONMENT_ID);

        assertEquals(secondCustomerId, updatedDataTableRow.values()
            .get("customer"));

        assertEquals(
            firstCustomerId,
            dataTableRowService.upsertRow(
                ordersId, "o-9", Map.of("title", "o9", "customer", Map.of("externalId", "c-1")), ENVIRONMENT_ID)
                .row()
                .values()
                .get("customer"));
        assertEquals(
            secondCustomerId,
            dataTableRowService.upsertRow(
                ordersId, "o-9", Map.of("customer", Map.of("externalId", "c-2")), ENVIRONMENT_ID)
                .row()
                .values()
                .get("customer"));

        List<DataTableRow> bulkUpdatedDataTableRows = dataTableRowService.updateRows(
            ordersId, DataTableRowConditions.of(List.of(new DataTableRowFilter("title", Operation.EQUALS, "o1"))),
            Map.of("customer", Map.of("externalId", "c-1")), ENVIRONMENT_ID);

        assertThat(bulkUpdatedDataTableRows)
            .extracting(bulkUpdatedRow -> bulkUpdatedRow.values()
                .get("customer"))
            .containsExactly(firstCustomerId);

        assertErrorType(
            DataTableErrorType.LINKED_ROW_NOT_FOUND,
            () -> dataTableRowService.updateRow(
                ordersId, dataTableRow.id(), Map.of("customer", MISSING_ID), ENVIRONMENT_ID));
    }

    @Test
    void testDeleteLinkedRowIsBlocked() {
        insertOrder("o1", firstCustomerId);
        insertOrder("o2", firstCustomerId);
        insertOrder("o3", firstCustomerId);

        DataTableException dataTableException = assertThrows(
            DataTableException.class,
            () -> dataTableRowService.deleteRow(customersId, firstCustomerId, ENVIRONMENT_ID));

        assertEquals(DataTableErrorType.ROW_STILL_LINKED, dataTableException.getErrorType());
        assertThat(dataTableException.getMessage()).contains("orders", "customer");
        assertThat(dataTableException.getMessage())
            .contains("Row " + firstCustomerId + " is linked from 3 rows in orders.customer");
        assertEquals(2, countCustomers());

        assertThat(dataTableRowService.deleteRow(customersId, secondCustomerId, ENVIRONMENT_ID)).isTrue();
        assertEquals(1, countCustomers());
    }

    @Test
    void testDeleteRowsByConditionsIsBlockedAndDeletesNothing() {
        insertOrder("o1", firstCustomerId);

        DataTableException dataTableException = assertThrows(
            DataTableException.class,
            () -> dataTableRowService.deleteRows(
                customersId,
                DataTableRowConditions.of(List.of(new DataTableRowFilter("name", Operation.NOT_EMPTY, null))),
                ENVIRONMENT_ID));

        assertEquals(DataTableErrorType.ROW_STILL_LINKED, dataTableException.getErrorType());
        assertThat(dataTableException.getMessage()).contains("orders", "customer");
        assertEquals(2, countCustomers());
    }

    @Test
    void testDeleteAllRowsIsBlockedAndDeletesNothing() {
        insertOrder("o1", firstCustomerId);

        DataTableException dataTableException = assertThrows(
            DataTableException.class, () -> dataTableRowService.deleteAllRows(customersId, ENVIRONMENT_ID));

        assertEquals(DataTableErrorType.ROW_STILL_LINKED, dataTableException.getErrorType());
        assertThat(dataTableException.getMessage()).contains("orders", "customer");
        assertEquals(2, countCustomers());
    }

    @Test
    void testClearRuleSetsLinksToNull() {
        long invoicesId = dataTableService.createTable(
            WORKSPACE_ID, "invoices", null, List.of(new ColumnSpec("number", ColumnType.STRING)), ENVIRONMENT_ID);

        dataTableService.addColumn(invoicesId, customerLink("client", LinkDeleteRule.CLEAR), ENVIRONMENT_ID);

        DataTableRow invoiceDataTableRow = dataTableRowService.insertRow(
            invoicesId, Map.of("number", "i1", "client", secondCustomerId), ENVIRONMENT_ID);

        assertEquals(secondCustomerId, invoiceDataTableRow.values()
            .get("client"));

        assertThat(dataTableRowService.deleteRow(customersId, secondCustomerId, ENVIRONMENT_ID)).isTrue();

        DataTableRow clearedDataTableRow = dataTableRowService.getRow(
            invoicesId, invoiceDataTableRow.id(), ENVIRONMENT_ID);

        assertThat(clearedDataTableRow).isNotNull();
        assertThat(clearedDataTableRow.values()
            .get("client")).isNull();
    }

    @Test
    void testFilterByLinkEqualsAndIn() {
        long firstOrderId = insertOrder("o1", firstCustomerId);
        long secondOrderId = insertOrder("o2", secondCustomerId);
        long thirdOrderId = dataTableRowService.insertRow(ordersId, Map.of("title", "o3"), ENVIRONMENT_ID)
            .id();

        assertThat(findOrderIds(new DataTableRowFilter("customer", Operation.EQUALS, firstCustomerId)))
            .containsExactly(firstOrderId);
        assertThat(findOrderIds(new DataTableRowFilter("customer", Operation.EQUALS, String.valueOf(secondCustomerId))))
            .containsExactly(secondOrderId);
        assertThat(
            findOrderIds(
                new DataTableRowFilter("customer", Operation.IN, List.of(firstCustomerId, secondCustomerId))))
                    .containsExactly(firstOrderId, secondOrderId);
        assertThat(findOrderIds(new DataTableRowFilter("customer", Operation.NOT_EQUALS, firstCustomerId)))
            .containsExactly(secondOrderId, thirdOrderId);
        assertThat(findOrderIds(new DataTableRowFilter("customer", Operation.EMPTY, null)))
            .containsExactly(thirdOrderId);
        assertThat(findOrderIds(new DataTableRowFilter("customer", Operation.NOT_EMPTY, null)))
            .containsExactly(firstOrderId, secondOrderId);
    }

    @Test
    void testFilterByLinkRejectsGreater() {
        insertOrder("o1", firstCustomerId);

        for (Operation operation : List.of(Operation.GREATER, Operation.LESS_EQUALS, Operation.CONTAINS)) {
            DataTableException dataTableException = assertThrows(
                DataTableException.class,
                () -> findOrderIds(new DataTableRowFilter("customer", operation, "1")));

            assertEquals(DataTableErrorType.FILTER_INVALID, dataTableException.getErrorType());
        }
    }

    @Test
    void testSortByLink() {
        long firstOrderId = insertOrder("o1", secondCustomerId);
        long secondOrderId = insertOrder("o2", firstCustomerId);
        long thirdOrderId = dataTableRowService.insertRow(ordersId, Map.of("title", "o3"), ENVIRONMENT_ID)
            .id();

        assertThat(
            dataTableRowService.findRows(
                ordersId, DataTableRowConditions.matchAll(),
                List.of(new DataTableRowSort("customer", Direction.ASC)), 100, 0, ENVIRONMENT_ID))
                    .extracting(DataTableRow::id)
                    .containsExactly(secondOrderId, firstOrderId, thirdOrderId);
        assertThat(
            dataTableRowService.findRows(
                ordersId, DataTableRowConditions.matchAll(),
                List.of(new DataTableRowSort("customer", Direction.DESC)), 100, 0, ENVIRONMENT_ID))
                    .extracting(DataTableRow::id)
                    .containsExactly(firstOrderId, secondOrderId, thirdOrderId);
    }

    private ColumnSpec customerLink(String columnName, LinkDeleteRule linkDeleteRule) {
        return new ColumnSpec(columnName, ColumnType.LINK, new LinkSpec(customersId, linkDeleteRule, "name"));
    }

    private long countCustomers() {
        return dataTableRowService.countRows(customersId, DataTableRowConditions.matchAll(), ENVIRONMENT_ID);
    }

    private long countOrders() {
        return dataTableRowService.countRows(ordersId, DataTableRowConditions.matchAll(), ENVIRONMENT_ID);
    }

    private List<Long> findOrderIds(DataTableRowFilter dataTableRowFilter) {
        return dataTableRowService.findRows(
            ordersId, DataTableRowConditions.of(List.of(dataTableRowFilter)), List.of(), 100, 0, ENVIRONMENT_ID)
            .stream()
            .map(DataTableRow::id)
            .toList();
    }

    private long insertOrder(String title, long customerId) {
        DataTableRow dataTableRow = dataTableRowService.insertRow(
            ordersId, Map.of("title", title, "customer", customerId), ENVIRONMENT_ID);

        return dataTableRow.id();
    }

    private static void assertErrorType(DataTableErrorType expectedErrorType, Runnable runnable) {
        DataTableException dataTableException = assertThrows(DataTableException.class, runnable::run);

        assertEquals(expectedErrorType, dataTableException.getErrorType());
    }
}

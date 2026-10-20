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
import com.bytechef.test.config.h2.H2DataSourceConfiguration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(H2DataSourceConfiguration.class)
class DataTableRowServiceH2IntTest {

    private static final long DEV_ENVIRONMENT_ID = 0;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableRowService dataTableRowService;

    @AfterEach
    void afterEach() {
        dataTableService.dropTable("items", DEV_ENVIRONMENT_ID);
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

        List<List<DataTableRowFilter>> conditions = List.of(
            List.of(new DataTableRowFilter("amount", Operation.GREATER, 10)));

        List<DataTableRow> updatedDataTableRows = dataTableRowService.updateRows(
            "items", conditions, Map.of("title", "sold"), DEV_ENVIRONMENT_ID);

        assertEquals(List.of(bananaId, cherryId), getIds(updatedDataTableRows));
        assertEquals("sold", updatedDataTableRows.getFirst()
            .values()
            .get("title"));
        assertEquals(
            "Apple", dataTableRowService.getRow("items", appleId, DEV_ENVIRONMENT_ID)
                .values()
                .get("title"));

        List<DataTableRow> deletedDataTableRows = dataTableRowService.deleteRows(
            "items", conditions, DEV_ENVIRONMENT_ID);

        assertEquals(List.of(bananaId, cherryId), getIds(deletedDataTableRows));
        assertEquals(List.of(appleId), getIds(dataTableRowService.listRows("items", 100, 0, DEV_ENVIRONMENT_ID)));
        assertEquals(List.of(), dataTableRowService.deleteRows("items", conditions, DEV_ENVIRONMENT_ID));
    }

    @Test
    void testUpdateRowsReturnsRowsThatNoLongerMatchTheConditions() {
        insertItem("Apple", 5);

        long bananaId = insertItem("Banana", 20);
        long cherryId = insertItem("Cherry", 30);

        List<DataTableRow> updatedDataTableRows = dataTableRowService.updateRows(
            "items", List.of(List.of(new DataTableRowFilter("amount", Operation.GREATER, 10))),
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
        return getIds(dataTableRowService.findRows("items", List.of(filters), sort, 100, 0, DEV_ENVIRONMENT_ID));
    }

    private long insertItem(String title, Integer amount) {
        Map<String, Object> values = new HashMap<>();

        values.put("title", title);
        values.put("amount", amount);

        DataTableRow dataTableRow = dataTableRowService.insertRow("items", values, DEV_ENVIRONMENT_ID);

        return dataTableRow.id();
    }
}

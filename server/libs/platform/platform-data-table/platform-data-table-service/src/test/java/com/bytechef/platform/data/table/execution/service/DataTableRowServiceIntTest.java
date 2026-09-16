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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
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

    private static final DataTableRef DATA_TABLE_REF =
        new DataTableRef("rows", DEV_ENVIRONMENT_ID);

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableRowService dataTableRowService;

    @BeforeEach
    void beforeEach() {
        dataTableService.dropTable("rows", DEV_ENVIRONMENT_ID);

        dataTableService.createTable(
            "rows", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);
    }

    @Test
    void testInsertRowReturnsTheStoredValues() {
        DataTableRow dataTableRow = dataTableRowService.insertRow(DATA_TABLE_REF, Map.of("title", "first"));

        assertNotNull(dataTableRow);
        assertEquals("first", dataTableRow.values()
            .get("title"));
    }

    @Test
    void testUpdateRowReturnsTheUpdatedValues() {
        DataTableRow insertedDataTableRow = dataTableRowService.insertRow(DATA_TABLE_REF, Map.of("title", "first"));

        DataTableRow updatedDataTableRow = dataTableRowService.updateRow(
            DATA_TABLE_REF, insertedDataTableRow.id(), Map.of("title", "second"));

        assertEquals(insertedDataTableRow.id(), updatedDataTableRow.id());
        assertEquals("second", updatedDataTableRow.values()
            .get("title"));

        DataTableRow fetchedDataTableRow = dataTableRowService.getRow(DATA_TABLE_REF, insertedDataTableRow.id());

        assertEquals("second", fetchedDataTableRow.values()
            .get("title"));
    }

    @Test
    void testDeleteRowRemovesTheRow() {
        DataTableRow dataTableRow = dataTableRowService.insertRow(DATA_TABLE_REF, Map.of("title", "first"));

        assertTrue(dataTableRowService.deleteRow(DATA_TABLE_REF, dataTableRow.id()));

        assertNull(dataTableRowService.getRow(DATA_TABLE_REF, dataTableRow.id()));

        assertTrue(dataTableRowService.listRows(DATA_TABLE_REF, 10, 0)
            .isEmpty());
    }

    @Test
    void testDeleteRowOfMissingRowReturnsFalse() {
        assertFalse(dataTableRowService.deleteRow(DATA_TABLE_REF, Long.MAX_VALUE));
    }
}

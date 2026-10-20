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

package com.bytechef.platform.data.table.configuration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.test.config.h2.H2DataSourceConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(H2DataSourceConfiguration.class)
class DataTableServiceH2IntTest {

    private static final long DEV_ENVIRONMENT_ID = 0;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableRowService dataTableRowService;

    @Test
    void testCreateTableAndListTables() {
        dataTableService.createTable(
            "created", "a description", List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        List<DataTableInfo> dataTableInfos = dataTableService.listTables(DEV_ENVIRONMENT_ID);

        assertTrue(
            dataTableInfos.stream()
                .anyMatch(dataTableInfo -> "created".equals(dataTableInfo.baseName())));
    }

    @Test
    void testAddRenameAndRemoveColumn() {
        dataTableService.createTable(
            "columns", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        dataTableService.addColumn("columns", new ColumnSpec("amount", ColumnType.NUMBER), DEV_ENVIRONMENT_ID);
        dataTableService.renameColumn("columns", "amount", "total", DEV_ENVIRONMENT_ID);

        DataTableRow dataTableRow = dataTableRowService.insertRow(
            "columns", Map.of("title", "renamed", "total", 12), DEV_ENVIRONMENT_ID);

        assertTrue(dataTableRow.values()
            .containsKey("total"));

        dataTableService.removeColumn("columns", "total", DEV_ENVIRONMENT_ID);
    }

    @Test
    void testInsertUpdateAndReadRows() {
        dataTableService.createTable(
            "rows", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        DataTableRow insertedDataTableRow = dataTableRowService.insertRow(
            "rows", Map.of("title", "first"), DEV_ENVIRONMENT_ID);

        assertNotNull(insertedDataTableRow);
        assertEquals("first", insertedDataTableRow.values()
            .get("title"));

        DataTableRow updatedDataTableRow = dataTableRowService.updateRow(
            "rows", insertedDataTableRow.id(), Map.of("title", "second"), DEV_ENVIRONMENT_ID);

        assertEquals("second", updatedDataTableRow.values()
            .get("title"));

        DataTableRow fetchedDataTableRow = dataTableRowService.getRow(
            "rows", insertedDataTableRow.id(), DEV_ENVIRONMENT_ID);

        assertEquals("second", fetchedDataTableRow.values()
            .get("title"));

        assertEquals(1, dataTableRowService.listRows("rows", 10, 0, DEV_ENVIRONMENT_ID)
            .size());

        assertTrue(dataTableRowService.deleteRow("rows", insertedDataTableRow.id(), DEV_ENVIRONMENT_ID));
    }

    @Test
    void testDuplicateAndRenameTable() {
        dataTableService.createTable(
            "source", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        dataTableRowService.insertRow("source", Map.of("title", "copied"), DEV_ENVIRONMENT_ID);

        dataTableService.duplicateTable("source", "duplicate", DEV_ENVIRONMENT_ID);

        assertEquals(1, dataTableRowService.listRows("duplicate", 10, 0, DEV_ENVIRONMENT_ID)
            .size());

        dataTableService.renameTable("duplicate", "renamed", DEV_ENVIRONMENT_ID);

        assertEquals("renamed", dataTableService.getBaseNameById(dataTableService.getIdByBaseName("renamed")));
    }
}

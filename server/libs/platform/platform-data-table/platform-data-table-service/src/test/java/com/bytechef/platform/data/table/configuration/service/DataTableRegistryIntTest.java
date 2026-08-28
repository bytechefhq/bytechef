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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.RowOwnerFilter;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.platform.owner.Owner;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
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
@Import(PostgreSQLContainerConfiguration.class)
class DataTableRegistryIntTest {

    private static final long ENVIRONMENT_ID = 0;
    private static final long OTHER_ENVIRONMENT_ID = 1;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableRowService dataTableRowService;

    @Test
    void testCreateTableRegistersTheTable() {
        dataTableService.createTable(
            "registered", "a description", List.of(new ColumnSpec("title", ColumnType.STRING)), ENVIRONMENT_ID);

        assertEquals("registered", dataTableService.getBaseNameById(dataTableService.getIdByBaseName("registered")));

        List<DataTableInfo> dataTableInfos = dataTableService.listTables(ENVIRONMENT_ID);

        assertTrue(
            dataTableInfos.stream()
                .anyMatch(dataTableInfo -> "registered".equals(dataTableInfo.baseName())),
            "A created table must be visible to listTables, which skips unregistered physical tables");
    }

    /**
     * The registry row is the LOGICAL table; each environment holds its own physical instance of it. dropTable already
     * treats it that way -- it removes the row only once no physical table for the base name remains in any environment
     * -- so creation has to reuse an existing row rather than insert a second one that uk_data_table_name forbids.
     */
    @Test
    void testTheSameNameCanBeCreatedInASecondEnvironment() {
        dataTableService.createTable(
            "shared", "a description", List.of(new ColumnSpec("title", ColumnType.STRING)), ENVIRONMENT_ID);
        dataTableService.createTable(
            "shared", "a description", List.of(new ColumnSpec("title", ColumnType.STRING)), OTHER_ENVIRONMENT_ID);

        assertTrue(
            listedIn(ENVIRONMENT_ID, "shared"), "the table must remain visible in the environment it was created in");
        assertTrue(listedIn(OTHER_ENVIRONMENT_ID, "shared"), "and be visible in the second environment");
    }

    private boolean listedIn(long environmentId, String baseName) {
        List<DataTableInfo> dataTableInfos = dataTableService.listTables(environmentId);

        return dataTableInfos.stream()
            .anyMatch(dataTableInfo -> baseName.equals(dataTableInfo.baseName()));
    }

    @Test
    void testDuplicateTableRegistersTheCopy() {
        dataTableService.createTable(
            "original", null, List.of(new ColumnSpec("title", ColumnType.STRING)), ENVIRONMENT_ID);

        dataTableService.duplicateTable("original", "copy", ENVIRONMENT_ID);

        assertEquals("copy", dataTableService.getBaseNameById(dataTableService.getIdByBaseName("copy")));
    }

    @Test
    void testADuplicatedTableCanStillTakeOwnedRows() {
        dataTableService.createTable(
            "source", null, List.of(new ColumnSpec("title", ColumnType.STRING)), ENVIRONMENT_ID);

        dataTableService.duplicateTable("source", "duplicate", ENVIRONMENT_ID);

        // Fails with "column owner_id does not exist" if duplicateTable omits the reserved owner columns.
        dataTableRowService.insertRow(
            "duplicate", Map.of("title", "a"), ENVIRONMENT_ID, RowOwnerFilter.ownedBy(Owner.connectedUser(1L)));

        assertEquals(
            1,
            dataTableRowService.listRows(
                "duplicate", 100, 0, ENVIRONMENT_ID, RowOwnerFilter.ownedBy(Owner.connectedUser(1L)))
                .size());
    }
}

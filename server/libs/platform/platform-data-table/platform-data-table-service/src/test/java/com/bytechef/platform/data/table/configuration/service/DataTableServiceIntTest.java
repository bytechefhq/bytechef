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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.repository.DataTableRepository;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class DataTableServiceIntTest {

    private static final long DEV_ENVIRONMENT_ID = 0;
    private static final long STAGE_ENVIRONMENT_ID = 1;

    @Autowired
    private DataTableRepository dataTableRepository;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void beforeEach() {
        for (String baseName : List.of("registered", "original", "copy", "dropped")) {
            dataTableService.dropTable(baseName, DEV_ENVIRONMENT_ID);
            dataTableService.dropTable(baseName, STAGE_ENVIRONMENT_ID);
        }
    }

    @Test
    void testCreateTableRegistersTheTable() {
        dataTableService.createTable(
            "registered", "a description", List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        assertEquals("registered", dataTableService.getBaseNameById(dataTableService.getIdByBaseName("registered")));

        List<DataTableInfo> dataTableInfos = dataTableService.listTables(DEV_ENVIRONMENT_ID);

        assertTrue(
            dataTableInfos.stream()
                .anyMatch(dataTableInfo -> "registered".equals(dataTableInfo.baseName())),
            "A created table must be visible to listTables, which skips unregistered physical tables");

        assertThrowsExactly(BadSqlGrammarException.class, () -> dataTableService.createTable(
            "registered", "a description", List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID));

        dataTableService.createTable(
            "registered", "a description", List.of(new ColumnSpec("title", ColumnType.STRING)), STAGE_ENVIRONMENT_ID);

        dataTableInfos = dataTableService.listTables(DEV_ENVIRONMENT_ID);

        assertEquals(1, dataTableInfos.size());
    }

    @Test
    void testCreateTableRegistersAMixedCaseNameUnderItsLowercasedForm() {
        dataTableService.createTable(
            "Registered", "a description", List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        List<DataTableInfo> dataTableInfos = dataTableService.listTables(DEV_ENVIRONMENT_ID);

        assertTrue(
            dataTableInfos.stream()
                .anyMatch(dataTableInfo -> "registered".equals(dataTableInfo.baseName())),
            "listTables derives the base name from the lowercased physical table, so the registry must agree");

        assertEquals(dataTableService.getIdByBaseName("Registered"), dataTableService.getIdByBaseName("registered"));
    }

    @Test
    void testDuplicateTableRegistersTheCopy() {
        dataTableService.createTable(
            "original", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        dataTableService.duplicateTable("original", "copy", DEV_ENVIRONMENT_ID);

        assertEquals("copy", dataTableService.getBaseNameById(dataTableService.getIdByBaseName("copy")));
    }

    @Test
    void testDropTableShouldDeleteMetadataWhenNoPhysicalTablesRemain() {
        dataTableService.createTable(
            "dropped", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        dataTableService.dropTable("dropped", DEV_ENVIRONMENT_ID);

        assertFalse(physicalTableExists("dt_0_dropped"));
        assertTrue(
            dataTableRepository.findByName("dropped")
                .isEmpty(),
            "The registry row must go once the last physical table is dropped");
    }

    @Test
    void testDropTableShouldUseTheLowercasedNameForBothLayers() {
        dataTableService.createTable(
            "dropped", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        dataTableService.dropTable("Dropped", DEV_ENVIRONMENT_ID);

        assertFalse(physicalTableExists("dt_0_dropped"));
        assertTrue(
            dataTableRepository.findByName("dropped")
                .isEmpty(),
            "A mixed-case drop must remove the registry row stored under the lowercased name");
    }

    @Test
    void testDropTableShouldPreserveMetadataWhenPhysicalTablesExistInOtherEnvironments() {
        dataTableService.createTable(
            "dropped", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);
        dataTableService.createTable(
            "dropped", null, List.of(new ColumnSpec("title", ColumnType.STRING)), STAGE_ENVIRONMENT_ID);

        dataTableService.dropTable("dropped", DEV_ENVIRONMENT_ID);

        assertFalse(physicalTableExists("dt_0_dropped"));
        assertTrue(physicalTableExists("dt_1_dropped"));
        assertTrue(
            dataTableRepository.findByName("dropped")
                .isPresent(),
            "The registry row must survive while another environment still has the physical table");
    }

    private boolean physicalTableExists(String physicalName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = ?",
            Integer.class, physicalName);

        return count != null && count > 0;
    }
}

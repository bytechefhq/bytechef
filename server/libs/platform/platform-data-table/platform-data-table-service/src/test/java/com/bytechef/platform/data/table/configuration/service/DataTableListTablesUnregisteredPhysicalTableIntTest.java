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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * {@code listTables} reads the registry and then asks each environment whether the row's physical table exists. A
 * physical table no registry row claims is never reported, whatever id its name spells.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class DataTableListTablesUnregisteredPhysicalTableIntTest {

    private static final long ENVIRONMENT_ID = 0;
    private static final long UNREGISTERED_DATA_TABLE_ID = 999999L;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS dt_0_999999");
    }

    @Test
    void testAPhysicalTableNoRegistryRowClaimsIsSkipped() {
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS dt_0_999999 (\"id\" BIGSERIAL PRIMARY KEY, \"title\" VARCHAR(255))");

        assertThat(dataTableService.listTables(1L, ENVIRONMENT_ID))
            .extracting(DataTableInfo::id)
            .as("a physical table without a registry row must not be listed")
            .doesNotContain(UNREGISTERED_DATA_TABLE_ID);
        assertThat(dataTableService.listAllTables(ENVIRONMENT_ID))
            .extracting(DataTableInfo::id)
            .doesNotContain(UNREGISTERED_DATA_TABLE_ID);
    }
}

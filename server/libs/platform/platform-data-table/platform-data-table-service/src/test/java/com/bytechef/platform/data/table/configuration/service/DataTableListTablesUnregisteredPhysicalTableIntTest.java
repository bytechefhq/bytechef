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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * {@code listTables} reads the physical tables out of {@code information_schema} and then asks the registry what each
 * one is. A physical table no registry row claims is skipped with a warning rather than reported under whatever base
 * name its remainder happens to spell.
 *
 * <p>
 * Not hypothetical. Leftover physical tables exist in databases built by earlier schemas, and their remainder past the
 * environment prefix reads as a base name ({@code 5_connecteduser_orders}). Reporting them would put an identifier no
 * workflow can name in the console's table list.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class DataTableListTablesUnregisteredPhysicalTableIntTest {

    private static final long ENVIRONMENT_ID = 0;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testAPhysicalTableNoRegistryRowClaimsIsSkipped() {
        jdbcTemplate.execute(
            "CREATE TABLE dt_0_5_connecteduser_leftovers (\"id\" BIGSERIAL PRIMARY KEY, "
                + "\"title\" VARCHAR(255))");

        List<DataTableInfo> dataTableInfos = dataTableService.listTables(ENVIRONMENT_ID);

        assertThat(dataTableInfos)
            .extracting(DataTableInfo::baseName)
            .as("a leftover physical table must not be listed under the name its remainder spells")
            .doesNotContain("5_connecteduser_leftovers", "leftovers");
    }
}

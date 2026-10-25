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
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class DataTableExternalIdIndexIntTest {

    private static final long ENVIRONMENT_ID = 0;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void beforeEach() {
        dataTableService.dropTable("keyed", ENVIRONMENT_ID);

        dataTableService.createTable(
            "keyed", null, List.of(new ColumnSpec("title", ColumnType.STRING)), ENVIRONMENT_ID);
    }

    @Test
    void testTheSameExternalIdTwiceIsRejected() {
        jdbcTemplate.update("INSERT INTO \"dt_0_keyed\" (\"external_id\", \"title\") VALUES ('k1', 'a')");

        assertThrows(
            DuplicateKeyException.class,
            () -> jdbcTemplate.update("INSERT INTO \"dt_0_keyed\" (\"external_id\", \"title\") VALUES ('k1', 'b')"));
    }

    @Test
    void testRowsWithoutAnExternalIdCoexist() {
        jdbcTemplate.update("INSERT INTO \"dt_0_keyed\" (\"title\") VALUES ('a')");
        jdbcTemplate.update("INSERT INTO \"dt_0_keyed\" (\"title\") VALUES ('b')");

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM \"dt_0_keyed\"", Integer.class);

        assertEquals(2, count);
    }
}

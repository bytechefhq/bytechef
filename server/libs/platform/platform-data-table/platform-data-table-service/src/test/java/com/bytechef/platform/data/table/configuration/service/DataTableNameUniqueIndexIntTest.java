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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The shape of the registry key, asserted against the database rather than against the changelog text.
 *
 * <p>
 * A name identifies one row, and the service takes that at its word: {@code fetchDataTable} asks the repository for a
 * single row by name, so a second row of the same name would not resolve ambiguously -- it would make every read of
 * that name throw. The key is what makes the singular lookup legitimate.
 *
 * <p>
 * Rows are written with raw SQL on purpose, because the point is what the database refuses rather than what the domain
 * declines to attempt.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class DataTableNameUniqueIndexIntTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testOnePoolCannotHoldTheSameNameTwice() {
        insert("uqinvoices");

        assertThatThrownBy(() -> insert("uqinvoices"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insert(String name) {
        jdbcTemplate.update(
            "INSERT INTO data_table (name, created_date, created_by, " +
                "last_modified_date, last_modified_by, version) " +
                "VALUES (?, now(), 'test', now(), 'test', 0)",
            name);
    }
}

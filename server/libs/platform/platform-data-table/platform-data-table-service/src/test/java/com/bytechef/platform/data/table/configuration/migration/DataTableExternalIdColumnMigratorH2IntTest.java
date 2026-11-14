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

package com.bytechef.platform.data.table.configuration.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.test.config.h2.H2DataSourceConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(H2DataSourceConfiguration.class)
@SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
class DataTableExternalIdColumnMigratorH2IntTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private DataTableExternalIdColumnMigrator dataTableExternalIdColumnMigrator;

    @BeforeEach
    void beforeEach() {
        dataTableExternalIdColumnMigrator = new DataTableExternalIdColumnMigrator(jdbcTemplate);
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_0_legacyh2key\"");
    }

    @Test
    void testMigrateAddsTheColumnAndTheIndexToALegacyTable() {
        jdbcTemplate
            .execute("CREATE TABLE \"dt_0_legacyh2key\" (\"id\" BIGSERIAL PRIMARY KEY, \"title\" VARCHAR(255))");
        jdbcTemplate.update("INSERT INTO \"dt_0_legacyh2key\" (\"title\") VALUES ('a'), ('b')");

        assertThat(dataTableExternalIdColumnMigrator.migrate()).isEqualTo(1);

        jdbcTemplate.update("INSERT INTO \"dt_0_legacyh2key\" (\"external_id\") VALUES ('k')");

        assertThatThrownBy(
            () -> jdbcTemplate.update("INSERT INTO \"dt_0_legacyh2key\" (\"external_id\") VALUES ('k')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testMigrateTwiceIsANoOp() {
        jdbcTemplate
            .execute("CREATE TABLE \"dt_0_legacyh2key\" (\"id\" BIGSERIAL PRIMARY KEY, \"title\" VARCHAR(255))");

        assertThat(dataTableExternalIdColumnMigrator.migrate()).isEqualTo(1);
        assertThat(dataTableExternalIdColumnMigrator.migrate()).isZero();
    }
}

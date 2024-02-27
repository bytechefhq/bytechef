/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.component.registry.jdbc.operation;

import static com.bytechef.platform.component.registry.jdbc.constant.JdbcConstants.COLUMNS;
import static com.bytechef.platform.component.registry.jdbc.constant.JdbcConstants.ROWS;
import static com.bytechef.platform.component.registry.jdbc.constant.JdbcConstants.SCHEMA;
import static com.bytechef.platform.component.registry.jdbc.constant.JdbcConstants.TABLE;
import static com.bytechef.platform.component.registry.jdbc.constant.JdbcConstants.UPDATE_KEY;

import com.bytechef.platform.component.registry.config.JacksonConfiguration;
import com.bytechef.platform.component.registry.jdbc.DataSourceFactory;
import com.bytechef.platform.component.registry.jdbc.JdbcExecutor;
import com.bytechef.platform.component.registry.jdbc.operation.config.JdbcOperationIntTestConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = JdbcOperationIntTestConfiguration.class)
@Import({
    JacksonConfiguration.class, PostgreSQLContainerConfiguration.class
})
public class UpdateJdbcOperationIntTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UpdateJdbcOperation updateJdbcOperation;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void beforeEach() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS test;");

        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute(
            """
                    CREATE TABLE test (
                        id   varchar(256) not null primary key,
                        name varchar(256) not null
                    );
                    INSERT INTO test VALUES('id1', 'name1');
                    INSERT INTO test VALUES('id2', 'name2');
                    INSERT INTO test VALUES('id3', 'name3');
                    INSERT INTO test VALUES('id4', 'name4');
                """);
    }

    @Test
    public void testUpdate() {
        Map<String, ?> inputParameters = Map.of(
            COLUMNS, List.of("name"),
            ROWS, List.of(Map.of("id", "id2", "name", "name3")),
            SCHEMA, "public",
            TABLE, "test",
            UPDATE_KEY, "id");

        Map<String, Integer> result = updateJdbcOperation.execute(inputParameters, Map.of());

        Assertions.assertEquals(1, result.get("rows"));
        Assertions.assertEquals(
            "name3", jdbcTemplate.queryForObject("SELECT name FROM test WHERE id='id2'", String.class));
    }

    @TestConfiguration
    public static class UpdateJdbcActionIntTestConfiguration {

        @Autowired
        private DataSource dataSource;

        @Bean
        UpdateJdbcOperation updateJdbcOperation() {
            return new UpdateJdbcOperation(new JdbcExecutor(
                null,
                new DataSourceFactory() {

                    @Override
                    public DataSource getDataSource(
                        Map<String, ?> connectionParameters, String databaseJdbcName, String jdbcDriverClassName) {

                        return dataSource;
                    }
                },
                null));
        }
    }
}

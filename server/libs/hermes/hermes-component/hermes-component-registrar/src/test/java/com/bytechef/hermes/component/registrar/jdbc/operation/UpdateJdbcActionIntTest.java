
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.component.registrar.jdbc.operation;

import static com.bytechef.hermes.component.jdbc.constants.JdbcConstants.COLUMNS;
import static com.bytechef.hermes.component.jdbc.constants.JdbcConstants.ROWS;
import static com.bytechef.hermes.component.jdbc.constants.JdbcConstants.SCHEMA;
import static com.bytechef.hermes.component.jdbc.constants.JdbcConstants.TABLE;
import static com.bytechef.hermes.component.jdbc.constants.JdbcConstants.UPDATE_KEY;

import com.bytechef.hermes.component.Connection;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.jdbc.DataSourceFactory;
import com.bytechef.hermes.component.jdbc.JdbcExecutor;
import com.bytechef.hermes.component.jdbc.operation.UpdateJdbcOperation;
import com.bytechef.hermes.component.registrar.config.JdbcComponentRegistrarIntTestConfiguration;
import com.bytechef.test.annotation.EmbeddedSql;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@EmbeddedSql
@SpringBootTest(classes = JdbcComponentRegistrarIntTestConfiguration.class)
public class UpdateJdbcActionIntTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UpdateJdbcOperation updateJdbcOperation;

    private JdbcTemplate jdbcTemplate;

    @AfterEach
    public void afterEach() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS test;");
    }

    @BeforeEach
    public void beforeEach() {
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
        ExecutionParameters executionParameters = Mockito.mock(ExecutionParameters.class);

        Mockito.when(executionParameters.getList(COLUMNS, String.class, List.of()))
            .thenReturn(List.of("name"));
        Mockito.when(executionParameters.getList(ROWS, Map.class, List.of()))
            .thenReturn(List.of(Map.of("id", "id2", "name", "name3")));
        Mockito.when(executionParameters.getString(SCHEMA, "public"))
            .thenReturn("public");
        Mockito.when(executionParameters.getRequiredString(TABLE))
            .thenReturn("test");
        Mockito.when(executionParameters.getString(UPDATE_KEY, "id"))
            .thenReturn("id");

        Map<String, Integer> result = updateJdbcOperation.execute(Mockito.mock(Context.class), executionParameters);

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
                        Connection connection, String databaseJdbcName, String jdbcDriverClassName) {
                        return dataSource;
                    }
                },
                null));
        }
    }
}

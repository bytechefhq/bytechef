
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

import static com.bytechef.hermes.component.registrar.jdbc.constant.JdbcConstants.COLUMNS;
import static com.bytechef.hermes.component.registrar.jdbc.constant.JdbcConstants.ROWS;
import static com.bytechef.hermes.component.registrar.jdbc.constant.JdbcConstants.SCHEMA;
import static com.bytechef.hermes.component.registrar.jdbc.constant.JdbcConstants.TABLE;
import static com.bytechef.hermes.component.registrar.jdbc.constant.JdbcConstants.UPDATE_KEY;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.registrar.jdbc.DataSourceFactory;
import com.bytechef.hermes.component.registrar.jdbc.JdbcExecutor;
import com.bytechef.hermes.component.registrar.jdbc.operation.config.JdbcActionIntTestConfiguration;
import com.bytechef.test.annotation.EmbeddedSql;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
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
@SpringBootTest(classes = JdbcActionIntTestConfiguration.class)
public class UpdateJdbcActionIntTest {

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
        Context context = Mockito.mock(Context.class);

        Mockito.when(context.fetchConnection())
            .thenReturn(Optional.of(Mockito.mock(Context.Connection.class)));

        InputParameters inputParameters = Mockito.mock(InputParameters.class);

        Mockito.when(inputParameters.getList(COLUMNS, String.class, List.of()))
            .thenReturn(List.of("name"));
        Mockito.when(inputParameters.getList(ROWS, Map.class, List.of()))
            .thenReturn(List.of(Map.of("id", "id2", "name", "name3")));
        Mockito.when(inputParameters.getString(SCHEMA, "public"))
            .thenReturn("public");
        Mockito.when(inputParameters.getRequiredString(TABLE))
            .thenReturn("test");
        Mockito.when(inputParameters.getString(UPDATE_KEY, "id"))
            .thenReturn("id");

        Map<String, Integer> result = updateJdbcOperation.execute(context, inputParameters);

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
                        Context.Connection connection, String databaseJdbcName, String jdbcDriverClassName) {
                        return dataSource;
                    }
                },
                null));
        }
    }
}

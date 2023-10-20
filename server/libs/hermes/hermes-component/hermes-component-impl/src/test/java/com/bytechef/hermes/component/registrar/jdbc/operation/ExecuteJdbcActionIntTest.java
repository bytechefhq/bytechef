
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

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.Context.Connection;
import com.bytechef.hermes.component.registrar.jdbc.sql.DataSourceFactory;
import com.bytechef.hermes.component.registrar.jdbc.executor.JdbcExecutor;
import com.bytechef.hermes.component.registrar.jdbc.constant.JdbcConstants;
import com.bytechef.hermes.component.registrar.jdbc.operation.config.JdbcActionIntTestConfiguration;
import com.bytechef.test.annotation.EmbeddedSql;
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
public class ExecuteJdbcActionIntTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ExecuteJdbcOperation executeJdbcOperation;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void beforeEach() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS test;");

        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    public void testExecute() {
        Context context = Mockito.mock(Context.class);

        Mockito.when(context.fetchConnection())
            .thenReturn(Optional.of(Mockito.mock(Connection.class)));

        Map<String, ?> inputParameters = Map.of(
            JdbcConstants.EXECUTE,
            """
                    CREATE TABLE IF NOT EXISTS test (
                        id   varchar(256) not null primary key,
                        name varchar(256) not null
                    )
                """);

        executeJdbcOperation.execute(context, inputParameters);

        Assertions.assertEquals(0, jdbcTemplate.queryForObject("SELECT count(*) FROM test", Integer.class));

        inputParameters = Map.of(
            JdbcConstants.PARAMETERS, Map.of("id", "id1", "name", "name1"),
            JdbcConstants.EXECUTE, "INSERT INTO test VALUES(:id, :name)");

        executeJdbcOperation.execute(context, inputParameters);

        Assertions.assertEquals(1, jdbcTemplate.queryForObject("SELECT count(*) FROM test", Integer.class));
    }

    @TestConfiguration
    public static class ExecuteJdbcActionIntTestConfiguration {

        @Autowired
        private DataSource dataSource;

        @Bean
        ExecuteJdbcOperation executeJdbcOperation() {
            return new ExecuteJdbcOperation(new JdbcExecutor(
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

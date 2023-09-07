
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

package com.bytechef.hermes.component.jdbc.operation;

import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.definition.Context.Connection;
import com.bytechef.hermes.component.jdbc.sql.DataSourceFactory;
import com.bytechef.hermes.component.jdbc.executor.JdbcExecutor;
import com.bytechef.hermes.component.jdbc.constant.JdbcConstants;
import com.bytechef.hermes.component.jdbc.operation.config.JdbcOperationIntTestConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;

import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
@Import(PostgreSQLContainerConfiguration.class)
public class QueryJdbcOperationIntTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private QueryJdbcOperation queryJdbcOperation;

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
    public void testQuery() {
        Context context = Mockito.mock(Context.class);

        Mockito.when(context.fetchConnection())
            .thenReturn(Optional.of(Mockito.mock(Connection.class)));

        Map<String, ?> inputParameters = Map.of(
            JdbcConstants.PARAMETERS, Map.of("id", "id2"),
            JdbcConstants.QUERY, "SELECT count(*) FROM test where id=:id");

        List<Map<String, Object>> result = queryJdbcOperation.execute(context, inputParameters);

        Assertions.assertEquals(1, result.size());
    }

    @TestConfiguration
    public static class InsertJdbcActionIntTestConfiguration {

        @Autowired
        private DataSource dataSource;

        @Bean
        QueryJdbcOperation queryJdbcOperation() {
            return new QueryJdbcOperation(new JdbcExecutor(
                null,
                new DataSourceFactory() {

                    @Override
                    public DataSource getDataSource(
                        Connection connection, String databaseJdbcName, String jdbcDriverClassNamee) {

                        return dataSource;
                    }
                },
                null));
        }
    }
}

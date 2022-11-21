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

package com.bytechef.hermes.component.registrar.jdbc.executor.operation;

import com.bytechef.hermes.component.ConnectionParameters;
import com.bytechef.hermes.component.registrar.jdbc.config.JdbcComponentRegistrarIntTestConfiguration;
import com.bytechef.hermes.component.registrar.jdbc.executor.DataSourceFactory;
import com.bytechef.hermes.component.registrar.jdbc.executor.JdbcExecutor;
import com.bytechef.hermes.component.test.mock.MockContext;
import com.bytechef.hermes.component.test.mock.MockExecutionParameters;
import com.bytechef.test.annotation.EmbeddedSql;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
public class QueryJdbcActionIntTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private QueryJdbcOperation queryJdbcOperation;

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
    public void testQuery() {
        MockExecutionParameters parameters = new MockExecutionParameters(
                Map.of("query", "SELECT count(*) FROM test where id=:id", "parameters", Map.of("id", "id2")));

        queryJdbcOperation.execute(new MockContext(), parameters);

        List<Map<String, Object>> result = queryJdbcOperation.execute(new MockContext(), parameters);

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
                                ConnectionParameters connectionParameters,
                                String databaseJdbcName,
                                String jdbcDriverClassNamee) {
                            return dataSource;
                        }
                    },
                    null));
        }
    }
}

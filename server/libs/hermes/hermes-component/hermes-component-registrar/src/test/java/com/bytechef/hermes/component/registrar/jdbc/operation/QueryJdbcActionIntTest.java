
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
import com.bytechef.hermes.component.Parameters;
import com.bytechef.hermes.component.registrar.jdbc.DataSourceFactory;
import com.bytechef.hermes.component.registrar.jdbc.JdbcExecutor;
import com.bytechef.hermes.component.registrar.jdbc.constant.JdbcConstants;
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
public class QueryJdbcActionIntTest {

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
            .thenReturn(Optional.of(Mockito.mock(Context.Connection.class)));

        Parameters parameters = Mockito.mock(Parameters.class);

        Mockito.when(parameters.getMap(JdbcConstants.PARAMETERS, Map.of()))
            .thenReturn(Map.of("id", "id2"));
        Mockito.when(parameters.getRequiredString(JdbcConstants.QUERY))
            .thenReturn("SELECT count(*) FROM test where id=:id");

        List<Map<String, Object>> result = queryJdbcOperation.execute(context, parameters);

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
                        Context.Connection connection, String databaseJdbcName, String jdbcDriverClassNamee) {
                        return dataSource;
                    }
                },
                null));
        }
    }
}

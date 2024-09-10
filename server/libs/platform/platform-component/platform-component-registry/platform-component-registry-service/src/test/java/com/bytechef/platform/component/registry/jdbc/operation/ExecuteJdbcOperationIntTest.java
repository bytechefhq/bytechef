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

import com.bytechef.platform.component.registry.config.JacksonConfiguration;
import com.bytechef.platform.component.registry.jdbc.constant.JdbcConstants;
import com.bytechef.platform.component.registry.jdbc.operation.config.JdbcOperationIntTestConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = JdbcOperationIntTestConfiguration.class)
@Import({
    JacksonConfiguration.class, PostgreSQLContainerConfiguration.class
})
public class ExecuteJdbcOperationIntTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void beforeEach() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS test;");

        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    public void testPerform() throws SQLException {
        Map<String, ?> inputParameters = Map.of(
            JdbcConstants.EXECUTE,
            """
                    CREATE TABLE IF NOT EXISTS test (
                        id   varchar(256) not null primary key,
                        name varchar(256) not null
                    )
                """);

        new ExecuteJdbcOperation().execute(
            inputParameters, new SingleConnectionDataSource(dataSource.getConnection(), false));

        Assertions.assertEquals(0, jdbcTemplate.queryForObject("SELECT count(*) FROM test", Integer.class));

        inputParameters = Map.of(
            JdbcConstants.PARAMETERS, Map.of("id", "id1", "name", "name1"),
            JdbcConstants.EXECUTE, "INSERT INTO test VALUES(:id, :name)");

        new ExecuteJdbcOperation().execute(
            inputParameters, new SingleConnectionDataSource(dataSource.getConnection(), false));

        Assertions.assertEquals(1, jdbcTemplate.queryForObject("SELECT count(*) FROM test", Integer.class));
    }
}

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

package com.bytechef.platform.component.jdbc.operation;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.jdbc.constant.JdbcConstants;
import com.bytechef.platform.component.jdbc.operation.config.JdbcOperationIntTestConfiguration;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = JdbcOperationIntTestConfiguration.class)
public class QueryJdbcOperationIntTest {

    @Autowired
    private DataSource dataSource;

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
    public void testQuery() throws SQLException {
        Map<String, ?> inputParameters = Map.of(
            JdbcConstants.PARAMETERS, Map.of("id", "id2"),
            JdbcConstants.QUERY, "SELECT count(*) FROM test where id=:id");

        try (SingleConnectionDataSource singleConnectionDataSource = new SingleConnectionDataSource(
            dataSource.getConnection(), false)) {

            List<Map<String, Object>> result = new QueryJdbcOperation().execute(
                inputParameters, singleConnectionDataSource);

            Assertions.assertEquals(1, result.size());
        }
    }

    @Test
    public void testQueryWithinMaxRows() throws SQLException {
        Map<String, ?> inputParameters = Map.of(
            JdbcConstants.MAX_ROWS, 4,
            JdbcConstants.QUERY, "SELECT * FROM test");

        try (SingleConnectionDataSource singleConnectionDataSource = new SingleConnectionDataSource(
            dataSource.getConnection(), false)) {

            List<Map<String, Object>> result = new QueryJdbcOperation().execute(
                inputParameters, singleConnectionDataSource);

            Assertions.assertEquals(4, result.size());
        }
    }

    @Test
    public void testQueryExceedingMaxRows() throws SQLException {
        Map<String, ?> inputParameters = Map.of(
            JdbcConstants.MAX_ROWS, 3,
            JdbcConstants.QUERY, "SELECT * FROM test");

        try (SingleConnectionDataSource singleConnectionDataSource = new SingleConnectionDataSource(
            dataSource.getConnection(), false)) {

            ConfigurationException configurationException = Assertions.assertThrows(
                ConfigurationException.class,
                () -> new QueryJdbcOperation().execute(inputParameters, singleConnectionDataSource));

            Assertions.assertEquals(
                "The query returned more than 3 rows. Narrow the query or increase Max Rows.",
                configurationException.getMessage());
        }
    }

    @Test
    public void testQueryUsesDefaultMaxRows() throws SQLException {
        jdbcTemplate.execute(
            "INSERT INTO test SELECT 'gen' || i, 'name' || i FROM generate_series(1, " +
                QueryJdbcOperation.DEFAULT_MAX_ROWS + ") AS i");

        Map<String, ?> inputParameters = Map.of(JdbcConstants.QUERY, "SELECT * FROM test");

        try (SingleConnectionDataSource singleConnectionDataSource = new SingleConnectionDataSource(
            dataSource.getConnection(), false)) {

            Assertions.assertThrows(
                ConfigurationException.class,
                () -> new QueryJdbcOperation().execute(inputParameters, singleConnectionDataSource));
        }
    }

    @Test
    public void testQueryStopsReadingRowsAfterMaxRowsPlusOne() throws SQLException {
        jdbcTemplate.execute(
            "CREATE OR REPLACE FUNCTION query_test_fail_after(value INTEGER, threshold INTEGER) RETURNS INTEGER " +
                "AS $$ BEGIN IF value > threshold THEN RAISE EXCEPTION 'row % read', value; END IF; " +
                "RETURN value; END $$ LANGUAGE plpgsql;");

        Map<String, ?> inputParameters = Map.of(
            JdbcConstants.MAX_ROWS, 3,
            JdbcConstants.QUERY, "SELECT query_test_fail_after(i, 4) AS id FROM generate_series(1, 100) AS i");

        try (SingleConnectionDataSource singleConnectionDataSource = new SingleConnectionDataSource(
            dataSource.getConnection(), false)) {

            Assertions.assertThrows(
                ConfigurationException.class,
                () -> new QueryJdbcOperation().execute(inputParameters, singleConnectionDataSource));
        }
    }
}

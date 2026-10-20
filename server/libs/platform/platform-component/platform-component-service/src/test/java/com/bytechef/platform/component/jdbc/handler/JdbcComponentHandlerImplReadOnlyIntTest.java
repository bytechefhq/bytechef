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

package com.bytechef.platform.component.jdbc.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.component.jdbc.constant.JdbcConstants;
import com.bytechef.platform.component.jdbc.operation.QueryJdbcOperation;
import com.bytechef.platform.component.jdbc.operation.QueryJdbcOperation.QueryColumn;
import com.bytechef.platform.component.jdbc.operation.QueryJdbcOperation.QueryResult;
import com.bytechef.platform.component.jdbc.operation.config.JdbcOperationIntTestConfiguration;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = JdbcOperationIntTestConfiguration.class)
class JdbcComponentHandlerImplReadOnlyIntTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS test;");
        jdbcTemplate.execute(
            """
                    CREATE TABLE test (
                        id   varchar(256) not null primary key,
                        name varchar(256) not null
                    );
                    INSERT INTO test VALUES('id1', 'name1');
                    INSERT INTO test VALUES('id2', 'name2');
                """);
    }

    @Test
    void executeInReadOnlyTransactionRefusesADataModifyingStatementAndLeavesRowsUntouched() throws SQLException {
        Connection connection = dataSource.getConnection();
        SingleConnectionDataSource singleConnectionDataSource = new SingleConnectionDataSource(connection, false);

        Map<String, ?> inputParameters = Map.of(
            JdbcConstants.QUERY, "WITH deleted AS (DELETE FROM test RETURNING *) SELECT * FROM deleted");

        DataAccessException exception = assertThrows(
            DataAccessException.class,
            () -> JdbcComponentHandlerImpl.executeInReadOnlyTransaction(
                singleConnectionDataSource,
                () -> new QueryJdbcOperation().executeWithColumns(inputParameters, singleConnectionDataSource)));

        assertTrue(String.valueOf(exception.getMessage())
            .contains("read-only"), exception.getMessage());
        assertEquals(2, jdbcTemplate.queryForObject("SELECT count(*) FROM test", Integer.class));
        assertTrue(connection.getAutoCommit());
    }

    @Test
    void executeInReadOnlyTransactionRefusesAnUpdateStatement() throws SQLException {
        Connection connection = dataSource.getConnection();
        SingleConnectionDataSource singleConnectionDataSource = new SingleConnectionDataSource(connection, false);

        Map<String, ?> inputParameters = Map.of(JdbcConstants.QUERY, "UPDATE test SET name = 'changed'");

        assertThrows(
            DataAccessException.class,
            () -> JdbcComponentHandlerImpl.executeInReadOnlyTransaction(
                singleConnectionDataSource,
                () -> new QueryJdbcOperation().executeWithColumns(inputParameters, singleConnectionDataSource)));

        assertEquals(0, jdbcTemplate.queryForObject("SELECT count(*) FROM test WHERE name = 'changed'", Integer.class));
    }

    @Test
    void executeInReadOnlyTransactionReturnsColumnsForASelectAndRestoresTheConnection() throws SQLException {
        Connection connection = dataSource.getConnection();
        SingleConnectionDataSource singleConnectionDataSource = new SingleConnectionDataSource(connection, false);

        Map<String, ?> inputParameters = Map.of(
            JdbcConstants.PARAMETERS, Map.of("id", "missing"),
            JdbcConstants.QUERY, "SELECT id, name FROM test WHERE id = :id");

        QueryResult result = JdbcComponentHandlerImpl.executeInReadOnlyTransaction(
            singleConnectionDataSource,
            () -> new QueryJdbcOperation().executeWithColumns(inputParameters, singleConnectionDataSource));

        assertEquals(
            List.of("id", "name"), result.columns()
                .stream()
                .map(QueryColumn::name)
                .toList());
        assertTrue(result.rows()
            .isEmpty());
        assertTrue(connection.getAutoCommit());
    }
}

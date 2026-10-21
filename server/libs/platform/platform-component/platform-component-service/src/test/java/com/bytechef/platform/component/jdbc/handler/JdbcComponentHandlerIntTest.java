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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Property.Type;
import com.bytechef.component.definition.Resources;
import com.bytechef.platform.component.definition.JdbcComponentDefinition;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.jdbc.constant.JdbcConstants;
import com.bytechef.platform.component.jdbc.operation.QueryJdbcOperation;
import com.bytechef.platform.component.jdbc.operation.QueryJdbcOperation.QueryColumn;
import com.bytechef.platform.component.jdbc.operation.QueryJdbcOperation.QueryResult;
import com.bytechef.platform.component.jdbc.operation.config.JdbcOperationIntTestConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SpringBootTest(classes = JdbcOperationIntTestConfiguration.class)
class JdbcComponentHandlerIntTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Nested
    class QueryOutput {

        @Test
        void hasConnectionParametersRequiresHostAndDatabase() {
            assertTrue(JdbcComponentHandlerImpl.hasConnectionParameters(
                ParametersFactory.create(Map.of("host", "localhost", "database", "bytechef"))));
            assertFalse(JdbcComponentHandlerImpl.hasConnectionParameters(ParametersFactory.create(Map.of())));
            assertFalse(JdbcComponentHandlerImpl.hasConnectionParameters(
                ParametersFactory.create(Map.of("host", "localhost"))));
            assertFalse(JdbcComponentHandlerImpl.hasConnectionParameters(
                ParametersFactory.create(Map.of("host", " ", "database", "bytechef"))));
        }

        @Test
        void queryOutputReturnsNullWithoutConnectionParameters() throws Exception {
            JdbcComponentHandlerImpl jdbcComponentHandler =
                new JdbcComponentHandlerImpl(new JdbcComponentDefinition() {

                    @Override
                    public String getUrlTemplate() {
                        return "jdbc:postgresql://{host}:{port}/{database}";
                    }

                    @Override
                    public String getJdbcDriverClassName() {
                        return "org.postgresql.Driver";
                    }

                    @Override
                    public Optional<String> getDescription() {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<String> getIcon() {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<Resources> getResources() {
                        return Optional.empty();
                    }

                    @Override
                    public String getName() {
                        return "postgresql";
                    }

                    @Override
                    public Optional<String> getTitle() {
                        return Optional.empty();
                    }

                    @Override
                    public int getVersion() {
                        return 1;
                    }
                });

            assertNull(
                jdbcComponentHandler.queryOutput(
                    ParametersFactory.create(Map.of("query", "SELECT 1")), ParametersFactory.create(Map.of()),
                    mock(ActionContext.class)));
        }

        @Test
        void toColumnPropertyMapsJdbcTypesToPropertyTypes() {
            assertEquals(Type.BOOLEAN, JdbcComponentHandlerImpl.toColumnProperty("active", Types.BOOLEAN)
                .getType());
            assertEquals(Type.INTEGER, JdbcComponentHandlerImpl.toColumnProperty("id", Types.BIGINT)
                .getType());
            assertEquals(Type.NUMBER, JdbcComponentHandlerImpl.toColumnProperty("price", Types.NUMERIC)
                .getType());
            assertEquals(Type.DATE, JdbcComponentHandlerImpl.toColumnProperty("born", Types.DATE)
                .getType());
            assertEquals(Type.TIME, JdbcComponentHandlerImpl.toColumnProperty("at", Types.TIME)
                .getType());
            assertEquals(Type.DATE_TIME, JdbcComponentHandlerImpl.toColumnProperty("created", Types.TIMESTAMP)
                .getType());
            assertEquals(Type.ARRAY, JdbcComponentHandlerImpl.toColumnProperty("tags", Types.ARRAY)
                .getType());
            assertEquals(Type.STRING, JdbcComponentHandlerImpl.toColumnProperty("email", Types.VARCHAR)
                .getType());
            assertEquals(Type.STRING, JdbcComponentHandlerImpl.toColumnProperty("payload", Types.OTHER)
                .getType());
        }
    }

    @Nested
    class ReadOnlyTransaction {

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
        void executeInReadOnlyTransactionRefusesADataModifyingStatementAndLeavesRowsUntouched()
            throws SQLException {

            try (Connection connection = dataSource.getConnection()) {
                SingleConnectionDataSource singleConnectionDataSource =
                    new SingleConnectionDataSource(connection, false);

                Map<String, ?> inputParameters = Map.of(
                    JdbcConstants.QUERY, "WITH deleted AS (DELETE FROM test RETURNING *) SELECT * FROM deleted");

                DataAccessException exception = assertThrows(
                    DataAccessException.class,
                    () -> JdbcComponentHandlerImpl.executeInReadOnlyTransaction(
                        singleConnectionDataSource,
                        () -> new QueryJdbcOperation().executeWithColumns(inputParameters,
                            singleConnectionDataSource)));

                assertTrue(String.valueOf(exception.getMessage())
                    .contains("read-only"), exception.getMessage());
                assertEquals(2, jdbcTemplate.queryForObject("SELECT count(*) FROM test", Integer.class));
                assertTrue(connection.getAutoCommit());
            }
        }

        @Test
        void executeInReadOnlyTransactionRefusesAnUpdateStatement() throws SQLException {
            try (Connection connection = dataSource.getConnection()) {
                SingleConnectionDataSource singleConnectionDataSource =
                    new SingleConnectionDataSource(connection, false);

                Map<String, ?> inputParameters = Map.of(JdbcConstants.QUERY, "UPDATE test SET name = 'changed'");

                assertThrows(
                    DataAccessException.class,
                    () -> JdbcComponentHandlerImpl.executeInReadOnlyTransaction(
                        singleConnectionDataSource,
                        () -> new QueryJdbcOperation().executeWithColumns(inputParameters,
                            singleConnectionDataSource)));

                assertEquals(
                    0, jdbcTemplate.queryForObject("SELECT count(*) FROM test WHERE name = 'changed'", Integer.class));
            }
        }

        @Test
        void executeInReadOnlyTransactionReturnsColumnsForASelectAndRestoresTheConnection() throws SQLException {
            try (Connection connection = dataSource.getConnection()) {
                SingleConnectionDataSource singleConnectionDataSource =
                    new SingleConnectionDataSource(connection, false);

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
    }
}

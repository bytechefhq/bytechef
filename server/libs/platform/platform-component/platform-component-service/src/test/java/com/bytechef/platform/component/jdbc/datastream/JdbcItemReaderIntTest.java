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

package com.bytechef.platform.component.jdbc.datastream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.datastream.ExecutionContext;
import com.bytechef.component.definition.datastream.FieldDefinition;
import com.bytechef.platform.component.jdbc.JdbcExecutor;
import com.bytechef.platform.component.jdbc.operation.config.JdbcOperationIntTestConfiguration;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
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
public class JdbcItemReaderIntTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Parameters inputParameters;
    private ClusterElementContext clusterContext;
    private Context context;
    private ExecutionContext executionContext;

    @BeforeEach
    public void beforeEach() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS test_reader;");
        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute(
            """
                    CREATE TABLE test_reader (
                        id          INTEGER PRIMARY KEY,
                        name        VARCHAR(256) NOT NULL,
                        amount      DECIMAL(10,2),
                        active      BOOLEAN,
                        created_at  TIMESTAMP
                    );
                    INSERT INTO test_reader VALUES(1, 'name1', 100.50, true, '2024-01-01 10:00:00');
                    INSERT INTO test_reader VALUES(2, 'name2', 200.75, false, '2024-01-02 11:00:00');
                """);

        inputParameters = mock(Parameters.class);
        clusterContext = mock(ClusterElementContext.class);
        context = mock(Context.class);
        executionContext = mock(ExecutionContext.class);

        when(inputParameters.getString(eq("schema"), eq("public"))).thenReturn("public");
        when(inputParameters.getRequiredString(eq("table"))).thenReturn("test_reader");
    }

    @Test
    public void testGetFieldsReturnsColumnMetadata() throws SQLException {
        TestableJdbcItemReader reader = new TestableJdbcItemReader(dataSource);

        List<FieldDefinition> fields = reader.getFields(inputParameters, null, clusterContext);

        assertThat(fields).hasSize(5);

        FieldDefinition idField = fields.stream()
            .filter(field -> "id".equals(field.name()))
            .findFirst()
            .orElseThrow();

        assertThat(idField.type()).isEqualTo(Integer.class);

        FieldDefinition nameField = fields.stream()
            .filter(field -> "name".equals(field.name()))
            .findFirst()
            .orElseThrow();

        assertThat(nameField.type()).isEqualTo(String.class);

        FieldDefinition amountField = fields.stream()
            .filter(field -> "amount".equals(field.name()))
            .findFirst()
            .orElseThrow();

        assertThat(amountField.type()).isEqualTo(BigDecimal.class);

        FieldDefinition activeField = fields.stream()
            .filter(field -> "active".equals(field.name()))
            .findFirst()
            .orElseThrow();

        assertThat(activeField.type()).isEqualTo(Boolean.class);

        FieldDefinition createdAtField = fields.stream()
            .filter(field -> "created_at".equals(field.name()))
            .findFirst()
            .orElseThrow();

        assertThat(createdAtField.type()).isEqualTo(LocalDateTime.class);
    }

    @Test
    public void testReadReturnsRowData() throws SQLException {
        TestableJdbcItemReader reader = new TestableJdbcItemReader(dataSource);

        when(inputParameters.getString(eq("condition"))).thenReturn(null);
        when(inputParameters.getString(eq("orderBy"))).thenReturn("id");
        when(inputParameters.getString(eq("orderDirection"), eq("ASC"))).thenReturn("ASC");

        reader.open(inputParameters, null, context, executionContext);

        Map<String, Object> row1 = reader.read();

        assertThat(row1).isNotNull();
        assertThat(row1.get("id")).isEqualTo(1);
        assertThat(row1.get("name")).isEqualTo("name1");

        Map<String, Object> row2 = reader.read();

        assertThat(row2).isNotNull();
        assertThat(row2.get("id")).isEqualTo(2);
        assertThat(row2.get("name")).isEqualTo("name2");

        Map<String, Object> row3 = reader.read();

        assertThat(row3).isNull();

        reader.close();
    }

    @Test
    public void testReadWithCondition() throws SQLException {
        TestableJdbcItemReader reader = new TestableJdbcItemReader(dataSource);

        when(inputParameters.getString(eq("condition"))).thenReturn("id = 2");
        when(inputParameters.getString(eq("orderBy"))).thenReturn(null);
        when(inputParameters.getString(eq("orderDirection"), eq("ASC"))).thenReturn("ASC");

        reader.open(inputParameters, null, context, executionContext);

        Map<String, Object> row = reader.read();

        assertThat(row).isNotNull();
        assertThat(row.get("id")).isEqualTo(2);
        assertThat(row.get("name")).isEqualTo("name2");

        assertThat(reader.read()).isNull();

        reader.close();
    }

    @Test
    public void testReadWithOrderByDescending() throws SQLException {
        TestableJdbcItemReader reader = new TestableJdbcItemReader(dataSource);

        when(inputParameters.getString(eq("condition"))).thenReturn(null);
        when(inputParameters.getString(eq("orderBy"))).thenReturn("id");
        when(inputParameters.getString(eq("orderDirection"), eq("ASC"))).thenReturn("DESC");

        reader.open(inputParameters, null, context, executionContext);

        Map<String, Object> row1 = reader.read();

        assertThat(row1).isNotNull();
        assertThat(row1.get("id")).isEqualTo(2);

        Map<String, Object> row2 = reader.read();

        assertThat(row2).isNotNull();
        assertThat(row2.get("id")).isEqualTo(1);

        reader.close();
    }

    /**
     * A testable version of JdbcItemReader that uses an injected DataSource instead of creating one from connection
     * parameters.
     */
    private static class TestableJdbcItemReader {

        private final DataSource testDataSource;
        private SingleConnectionDataSource dataSource;
        private Iterator<Map<String, Object>> iterator;

        TestableJdbcItemReader(DataSource testDataSource) {
            this.testDataSource = testDataSource;
        }

        public List<FieldDefinition> getFields(
            Parameters inputParameters, Parameters connectionParameters, ClusterElementContext context)
            throws SQLException {

            List<FieldDefinition> fieldDefinitions = new ArrayList<>();

            try (SingleConnectionDataSource singleConnectionDataSource =
                new SingleConnectionDataSource(testDataSource.getConnection(), false)) {

                String schema = inputParameters.getString("schema", "public");
                String table = inputParameters.getRequiredString("table");

                String query = "SELECT * FROM %s.%s WHERE 1=0".formatted(schema, table);

                JdbcExecutor.query(
                    query, Map.of(), (ResultSet resultSet, int rowNum) -> {
                        try {
                            ResultSetMetaData metaData = resultSet.getMetaData();
                            int columnCount = metaData.getColumnCount();

                            for (int i = 1; i <= columnCount; i++) {
                                String columnName = metaData.getColumnName(i);
                                int columnType = metaData.getColumnType(i);
                                Class<?> javaType = getJavaType(columnType);

                                fieldDefinitions.add(new FieldDefinition(columnName, columnName, javaType));
                            }
                        } catch (SQLException exception) {
                            throw new RuntimeException(exception);
                        }

                        return null;
                    }, singleConnectionDataSource);

                if (fieldDefinitions.isEmpty()) {
                    try (var connection = singleConnectionDataSource.getConnection();
                        var columnsResultSet = connection.getMetaData()
                            .getColumns(null, schema, table, null)) {

                        while (columnsResultSet.next()) {
                            String columnName = columnsResultSet.getString("COLUMN_NAME");
                            int columnType = columnsResultSet.getInt("DATA_TYPE");
                            Class<?> javaType = getJavaType(columnType);

                            fieldDefinitions.add(new FieldDefinition(columnName, columnName, javaType));
                        }
                    }
                }
            }

            return fieldDefinitions;
        }

        public void open(
            Parameters inputParameters, Parameters connectionParameters, Context context,
            ExecutionContext executionContext) throws SQLException {

            this.dataSource = new SingleConnectionDataSource(testDataSource.getConnection(), false);

            String query = buildSelectQuery(inputParameters);

            List<Map<String, Object>> rows = JdbcExecutor.query(
                query, Map.of(), (ResultSet resultSet, int rowNum) -> {
                    Map<String, Object> row = new HashMap<>();

                    try {
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        int columnCount = metaData.getColumnCount();

                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);

                            row.put(columnName, resultSet.getObject(i));
                        }
                    } catch (SQLException exception) {
                        throw new RuntimeException(exception);
                    }

                    return row;
                }, dataSource);

            this.iterator = rows.iterator();
        }

        public Map<String, Object> read() {
            if (iterator != null && iterator.hasNext()) {
                return iterator.next();
            }

            return null;
        }

        public void close() {
            if (dataSource != null) {
                try {
                    dataSource.close();
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }
        }

        private String buildSelectQuery(Parameters inputParameters) {
            String schema = inputParameters.getString("schema", "public");
            String table = inputParameters.getRequiredString("table");
            String condition = inputParameters.getString("condition");
            String orderBy = inputParameters.getString("orderBy");
            String orderDirection = inputParameters.getString("orderDirection", "ASC");

            StringBuilder queryBuilder = new StringBuilder();

            queryBuilder.append("SELECT * FROM ")
                .append(schema)
                .append(".")
                .append(table);

            if (condition != null && !condition.isBlank()) {
                queryBuilder.append(" WHERE ")
                    .append(condition);
            }

            if (orderBy != null && !orderBy.isBlank()) {
                queryBuilder.append(" ORDER BY ")
                    .append(orderBy)
                    .append(" ")
                    .append(orderDirection);
            }

            return queryBuilder.toString();
        }

        private Class<?> getJavaType(int sqlType) {
            return switch (sqlType) {
                case Types.BIGINT -> Long.class;
                case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY -> byte[].class;
                case Types.BIT, Types.BOOLEAN -> Boolean.class;
                case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR ->
                    String.class;
                case Types.DATE -> java.time.LocalDate.class;
                case Types.DECIMAL, Types.NUMERIC -> BigDecimal.class;
                case Types.DOUBLE, Types.FLOAT -> Double.class;
                case Types.INTEGER -> Integer.class;
                case Types.REAL -> Float.class;
                case Types.SMALLINT -> Short.class;
                case Types.TIME, Types.TIME_WITH_TIMEZONE -> java.time.LocalTime.class;
                case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> LocalDateTime.class;
                case Types.TINYINT -> Byte.class;
                default -> Object.class;
            };
        }
    }
}

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

import static com.bytechef.component.definition.ComponentDsl.clusterElement;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.CONDITION;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.ORDER_BY;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.ORDER_DIRECTION;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.SCHEMA;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.TABLE;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.datastream.ExecutionContext;
import com.bytechef.component.definition.datastream.FieldDefinition;
import com.bytechef.component.definition.datastream.ItemReader;
import com.bytechef.platform.component.jdbc.DataSourceFactory;
import com.bytechef.platform.component.jdbc.JdbcExecutor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * @author Ivica Cardic
 */
public class JdbcItemReader implements ItemReader {

    static final int FETCH_SIZE = 1000;

    private Connection connection;
    private SingleConnectionDataSource dataSource;
    private final String jdbcDriverClassName;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;
    private final String urlTemplate;

    public JdbcItemReader(String urlTemplate, String jdbcDriverClassName) {
        this.urlTemplate = urlTemplate;
        this.jdbcDriverClassName = jdbcDriverClassName;
    }

    public static ModifiableClusterElementDefinition<?> clusterElementDefinition(
        String urlTemplate, String jdbcDriverClassName) {

        return clusterElement("reader")
            .title("Read table rows")
            .description("Reads rows from a table.")
            .type(SOURCE)
            .object(() -> new JdbcItemReader(urlTemplate, jdbcDriverClassName))
            .properties(
                string(SCHEMA)
                    .label("Schema")
                    .description("Name of the schema the table belongs to.")
                    .required(true)
                    .defaultValue("public"),
                string(TABLE)
                    .label("Table")
                    .description("Name of the table to read data from.")
                    .required(true),
                string(CONDITION)
                    .label("Condition")
                    .description("Optional WHERE clause condition (without the WHERE keyword).")
                    .required(false),
                string(ORDER_BY)
                    .label("Order By")
                    .description("Column name to order the results by.")
                    .required(false),
                string(ORDER_DIRECTION)
                    .label("Order Direction")
                    .description("Direction of the ordering.")
                    .options(
                        option("Ascending", "ASC"),
                        option("Descending", "DESC"))
                    .defaultValue("ASC")
                    .required(false));
    }

    @Override
    public void close() {
        try {
            if (resultSet != null) {
                resultSet.close();
            }

            if (preparedStatement != null) {
                preparedStatement.close();
            }

            if (connection != null && !connection.getAutoCommit()) {
                connection.rollback();
            }
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        } finally {
            if (dataSource != null) {
                dataSource.close();
            }
        }
    }

    @Override
    @SuppressFBWarnings(value = "SQL_INJECTION_JDBC", justification = "The condition is user-supplied SQL by design")
    public void open(
        Parameters inputParameters, Parameters connectionParameters, Context context,
        ExecutionContext executionContext) {

        this.dataSource = DataSourceFactory.getDataSource(connectionParameters, urlTemplate, jdbcDriverClassName);

        String query = buildSelectQuery(inputParameters);

        try {
            this.connection = dataSource.getConnection();

            connection.setAutoCommit(false);

            this.preparedStatement = connection.prepareStatement(
                query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

            int fetchSize = jdbcDriverClassName.startsWith("com.mysql") ? Integer.MIN_VALUE : FETCH_SIZE;

            preparedStatement.setFetchSize(fetchSize);

            this.resultSet = preparedStatement.executeQuery();
        } catch (SQLException sqlException) {
            close();

            throw new RuntimeException("Failed to read table rows", sqlException);
        }
    }

    @Override
    public Map<String, Object> read() {
        try {
            if (!resultSet.next()) {
                return null;
            }

            Map<String, Object> row = new HashMap<>();

            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);

                row.put(columnName, resultSet.getObject(i));
            }

            return row;
        } catch (SQLException sqlException) {
            throw new RuntimeException("Failed to read table row", sqlException);
        }
    }

    @Override
    public List<FieldDefinition> getFields(
        Parameters inputParameters, Parameters connectionParameters, ClusterElementContext context) {

        List<FieldDefinition> fieldDefinitions = new ArrayList<>();

        try (SingleConnectionDataSource dataSource =
            DataSourceFactory.getDataSource(connectionParameters, urlTemplate, jdbcDriverClassName)) {

            String schema = inputParameters.getString(SCHEMA, "public");
            String table = inputParameters.getRequiredString(TABLE);

            String query = "SELECT * FROM %s.%s WHERE 1=0".formatted(schema, table);

            JdbcExecutor.query(
                query, Map.of(), (ResultSet resultSet, int rowNum) -> {
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        int columnType = metaData.getColumnType(i);
                        Class<?> javaType = getJavaType(columnType);

                        fieldDefinitions.add(new FieldDefinition(columnName, columnName, javaType));
                    }

                    return null;
                }, dataSource);

            if (fieldDefinitions.isEmpty()) {
                try (var connection = dataSource.getConnection();
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
        } catch (SQLException sqlException) {
            throw new RuntimeException("Failed to get table column metadata", sqlException);
        }

        return fieldDefinitions;
    }

    private String buildSelectQuery(Parameters inputParameters) {
        String schema = inputParameters.getString(SCHEMA, "public");
        String table = inputParameters.getRequiredString(TABLE);
        String condition = inputParameters.getString(CONDITION);
        String orderBy = inputParameters.getString(ORDER_BY);
        String orderDirection = inputParameters.getString(ORDER_DIRECTION, "ASC");

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
            case Types.DATE -> LocalDate.class;
            case Types.DECIMAL, Types.NUMERIC -> BigDecimal.class;
            case Types.DOUBLE, Types.FLOAT -> Double.class;
            case Types.INTEGER -> Integer.class;
            case Types.REAL -> Float.class;
            case Types.SMALLINT -> Short.class;
            case Types.TIME, Types.TIME_WITH_TIMEZONE -> LocalTime.class;
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> LocalDateTime.class;
            case Types.TINYINT -> Byte.class;
            default -> Object.class;
        };
    }
}

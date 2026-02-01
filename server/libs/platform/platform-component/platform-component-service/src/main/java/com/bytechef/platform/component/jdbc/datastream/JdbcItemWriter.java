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

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.clusterElement;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.COLUMNS;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.NAME;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.ROWS;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.SCHEMA;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.TABLE;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.TYPE;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.VALUES;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.definition.datastream.ExecutionContext;
import com.bytechef.component.definition.datastream.FieldDefinition;
import com.bytechef.component.definition.datastream.ItemWriter;
import com.bytechef.platform.component.jdbc.DataSourceFactory;
import com.bytechef.platform.component.jdbc.operation.InsertJdbcOperation;
import com.bytechef.platform.component.util.SqlUtils;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * @author Ivica Cardic
 */
public class JdbcItemWriter implements ItemWriter {

    private final InsertJdbcOperation insertJdbcOperation;
    private Parameters inputParameters;
    private final String urlTemplate;
    private SingleConnectionDataSource dataSource;
    private final String jdbcDriverClassName;

    public JdbcItemWriter(String urlTemplate, String jdbcDriverClassName) {
        this.urlTemplate = urlTemplate;
        this.jdbcDriverClassName = jdbcDriverClassName;
        this.insertJdbcOperation = new InsertJdbcOperation();
    }

    public static ModifiableClusterElementDefinition<?> clusterElementDefinition(
        String urlTemplate, String jdbcDriverClassName) {

        return clusterElement("writer")
            .title("Write table rows")
            .description("Writes a list of rows to a table.")
            .type(DESTINATION)
            .object(() -> new JdbcItemWriter(urlTemplate, jdbcDriverClassName))
            .properties(
                string(SCHEMA)
                    .label("Schema")
                    .description("Name of the schema the table belongs to.")
                    .required(true)
                    .defaultValue("public"),
                string(TABLE)
                    .label("Table")
                    .description("Name of the table in which to insert data to.")
                    .required(true),
                array(COLUMNS)
                    .label("Columns")
                    .description("The list of the table column names where corresponding values would be updated.")
                    .items(
                        object()
                            .properties(
                                string(NAME)
                                    .label("Column Name")
                                    .description("Name of the column.")
                                    .required(true),
                                string(TYPE)
                                    .label("Type")
                                    .description("Type of the column.")
                                    .options(SqlUtils.getTypeOptions())
                                    .defaultValue("STRING")
                                    .required(true))));
    }

    @Override
    public void close() {
        if (dataSource != null) {

            try {
                dataSource.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void open(
        Parameters inputParameters, Parameters connectionParameters, Context context,
        ExecutionContext executionContext) {

        this.inputParameters = inputParameters;
        this.dataSource = DataSourceFactory.getDataSource(connectionParameters, urlTemplate, jdbcDriverClassName);
    }

    @Override
    public List<FieldDefinition> getFields(
        Parameters inputParameters, Parameters connectionParameters, ClusterElementContext context) {

        List<Map<String, Object>> columns = inputParameters.getList(COLUMNS, new TypeReference<>() {}, List.of());

        return columns.stream()
            .map(column -> {
                String name = MapUtils.getRequiredString(column, NAME);
                String type = MapUtils.getString(column, TYPE, "STRING");

                Class<?> javaType = switch (type) {
                    case "BIGINT", "INTEGER" -> Long.class;
                    case "DECIMAL", "DOUBLE", "FLOAT", "REAL" -> Double.class;
                    case "BOOLEAN" -> Boolean.class;
                    case "DATE" -> java.time.LocalDate.class;
                    case "TIME" -> java.time.LocalTime.class;
                    case "TIMESTAMP" -> java.time.LocalDateTime.class;
                    default -> String.class;
                };

                return new FieldDefinition(name, name, javaType);
            })
            .toList();
    }

    @Override
    public void write(List<? extends Map<String, Object>> items) throws Exception {
        insertJdbcOperation.execute(MapUtils.concat(inputParameters, Map.of(VALUES, Map.of(ROWS, items))), dataSource);
    }
}

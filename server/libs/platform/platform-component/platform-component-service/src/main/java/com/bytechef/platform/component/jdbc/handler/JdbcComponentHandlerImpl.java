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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.nullable;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.ALL_ROWS;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.COLUMNS;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.CONDITION;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.DATABASE;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.EXECUTE;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.HOST;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.LAST_ITEM;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.NAME;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.ORDER_BY;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.ORDER_DIRECTION;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.PARAMETERS;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.PASSWORD;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.PORT;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.QUERY;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.ROWS;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.SCHEMA;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.TABLE;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.TYPE;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.USERNAME;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.VALUES;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.Property.Type;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.platform.component.definition.JdbcComponentDefinition;
import com.bytechef.platform.component.jdbc.DataSourceFactory;
import com.bytechef.platform.component.jdbc.JdbcExecutor;
import com.bytechef.platform.component.jdbc.datastream.JdbcItemWriter;
import com.bytechef.platform.component.jdbc.operation.DeleteJdbcOperation;
import com.bytechef.platform.component.jdbc.operation.ExecuteJdbcOperation;
import com.bytechef.platform.component.jdbc.operation.InsertJdbcOperation;
import com.bytechef.platform.component.jdbc.operation.QueryJdbcOperation;
import com.bytechef.platform.component.jdbc.operation.UpdateJdbcOperation;
import com.bytechef.platform.component.util.SqlUtils;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 * @author Monika Kušter
 */
public class JdbcComponentHandlerImpl implements ComponentHandler {

    private static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .properties(
            string(HOST)
                .label("Host")
                .required(true),
            integer(PORT)
                .label("Port")
                .required(true),
            string(DATABASE)
                .label("Database")
                .required(true))
        .authorizations(
            authorization(AuthorizationType.CUSTOM)
                .properties(
                    string(USERNAME)
                        .label("Username")
                        .required(true),
                    string(PASSWORD)
                        .label("Password")
                        .controlType(ControlType.PASSWORD)
                        .required(true)));

    private final List<ModifiableActionDefinition> actionDefinitions = List.of(
        action(QUERY)
            .title("Query")
            .description("Execute an SQL query.")
            .properties(
                string(QUERY)
                    .label("Query")
                    .description(
                        "The raw SQL query to execute. You can use :property1 and :property2 in conjunction with " +
                            "parameters.")
                    .placeholder("SELECT id, name FROM customer WHERE age > :age AND height <= :height")
                    .controlType(ControlType.TEXT_AREA)
                    .required(true),
                object(PARAMETERS)
                    .label("Parameters")
                    .description("The list of properties which should be used as query parameters.")
                    .additionalProperties(bool(), dateTime(), number(), string()))
            .output()
            .perform(this::performQuery),
        action("insert")
            .title("Insert")
            .description("Insert rows in database.")
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
                    .description("The list of the table column names where corresponding values would be inserted.")
                    .items(
                        object()
                            .properties(
                                string(NAME)
                                    .label("Field Name")
                                    .description("Name of the column.")
                                    .required(true),
                                string(TYPE)
                                    .label("Type")
                                    .description("Type of the column.")
                                    .options(SqlUtils.getTypeOptions())
                                    .defaultValue("STRING")
                                    .required(true))),
                dynamicProperties(VALUES)
                    .properties(JdbcComponentHandlerImpl::createProperties)
                    .propertiesLookupDependsOn(COLUMNS))
            .output()
            .perform(this::performInsert),
        action("update")
            .title("Update")
            .description("Update rows in database.")
            .properties(
                string(SCHEMA)
                    .label("Schema")
                    .description("Name of the schema the table belongs to.")
                    .required(true)
                    .defaultValue("public"),
                string(TABLE)
                    .label("Table")
                    .description("Name of the table in which to update data in.")
                    .required(true),
                string(CONDITION)
                    .label("Condition")
                    .description("Condition that will be checked in the column. Example: column1=5")
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
                                    .required(true))),
                dynamicProperties(VALUES)
                    .properties(JdbcComponentHandlerImpl::createProperties)
                    .propertiesLookupDependsOn(COLUMNS))
            .output()
            .perform(this::performUpdate),
        action("delete")
            .title("Delete")
            .description("Delete rows from database.")
            .properties(
                string(SCHEMA)
                    .label("Schema")
                    .description("Name of the schema the table belongs to.")
                    .required(true)
                    .defaultValue("public"),
                string(TABLE)
                    .label("Table")
                    .description("Name of the table in which to update data in.")
                    .required(true),
                string(CONDITION)
                    .label("Condition")
                    .description("Condition that will be checked in the column. Example: column1=5")
                    .required(true))
            .output()
            .perform(this::performDelete),
        action("execute")
            .title("Execute")
            .description("Execute an SQL DML or DML statement.")
            .properties(
                string(EXECUTE)
                    .label("Execute")
                    .description(
                        "The raw DML or DDL statement to execute. You can use :property1 and :property2 in " +
                            "conjunction with parameters.")
                    .placeholder("UPDATE TABLE product set name = :name WHERE product > :product AND price <= :price")
                    .controlType(ControlType.TEXT_AREA)
                    .required(true),
                array(COLUMNS)
                    .label("Fields to select")
                    .description("List of fields to select from.")
                    .items(object().additionalProperties(
                        array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(),
                        string(), time())),
                object(PARAMETERS)
                    .label("Parameters")
                    .description(
                        "The list of values which should be used to replace corresponding criteria parameters.")
                    .additionalProperties(bool(), dateTime(), number(), string()))
            .output()
            .perform(this::performExecute));

    private final List<ModifiableTriggerDefinition> triggerDefinitions = List.of(
        trigger("newRow")
            .title("New Row")
            .description("Triggers when new row is added.")
            .type(TriggerType.POLLING)
            .properties(
                string(SCHEMA)
                    .label("Schema")
                    .description("Name of the schema the table belongs to.")
                    .required(true)
                    .defaultValue("public"),
                string(TABLE)
                    .label("Table")
                    .description("Name of the table in which to update data in.")
                    .required(true),
                string(ORDER_BY)
                    .label("Colum To Order By")
                    .description("Use something like a created timestamp or an auto-incrementing ID.")
                    .required(true),
                string(ORDER_DIRECTION)
                    .label("Order Direction")
                    .description("The direction to sort by such that the newest rows are fetched first.")
                    .options(
                        option("Ascending", "ASC"),
                        option("Descending", "DESC"))
                    .defaultValue("DESC")
                    .required(true))
            .output()
            .poll(this::poll));

    private final ComponentDefinition componentDefinition;
    private final String urlTemplate;
    private final DeleteJdbcOperation deleteJdbcOperation;
    private final ExecuteJdbcOperation executeJdbcOperation;
    private final InsertJdbcOperation insertJdbcOperation;
    private final String jdbcDriverClassName;
    private final QueryJdbcOperation queryJdbcOperation;
    private final UpdateJdbcOperation updateJdbcOperation;

    public JdbcComponentHandlerImpl(JdbcComponentDefinition jdbcComponentDefinition) {
        this.urlTemplate = jdbcComponentDefinition.getUrlTemplate();
        this.jdbcDriverClassName = jdbcComponentDefinition.getJdbcDriverClassName();

        this.componentDefinition = getComponentDefinition(
            OptionalUtils.orElse(jdbcComponentDefinition.getDescription(), null), jdbcComponentDefinition.getName(),
            OptionalUtils.orElse(jdbcComponentDefinition.getIcon(), null),
            OptionalUtils.orElse(jdbcComponentDefinition.getTitle(), null), jdbcDriverClassName);

        this.deleteJdbcOperation = new DeleteJdbcOperation();
        this.executeJdbcOperation = new ExecuteJdbcOperation();
        this.insertJdbcOperation = new InsertJdbcOperation();
        this.queryJdbcOperation = new QueryJdbcOperation();
        this.updateJdbcOperation = new UpdateJdbcOperation();
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    protected Map<String, Integer> performDelete(
        Map<String, ?> inputParameters, Map<String, ?> connectionParameters, ActionContext context) {

        try (SingleConnectionDataSource dataSource = getDataSource(connectionParameters)) {
            return deleteJdbcOperation.execute(inputParameters, dataSource);
        }
    }

    protected Map<String, Integer> performExecute(
        Map<String, ?> inputParameters, Map<String, ?> connectionParameters, ActionContext context) {

        try (SingleConnectionDataSource dataSource = getDataSource(connectionParameters)) {
            return executeJdbcOperation.execute(inputParameters, dataSource);
        }

    }

    protected Map<String, Integer> performInsert(
        Map<String, ?> inputParameters, Map<String, ?> connectionParameters, ActionContext context) {

        try (SingleConnectionDataSource dataSource = getDataSource(connectionParameters)) {
            return insertJdbcOperation.execute(inputParameters, dataSource);
        }
    }

    protected List<Map<String, Object>> performQuery(
        Map<String, ?> inputParameters, Map<String, ?> connectionParameters, ActionContext context) {

        try (SingleConnectionDataSource dataSource = getDataSource(connectionParameters)) {
            return queryJdbcOperation.execute(inputParameters, dataSource);
        }
    }

    protected Map<String, Integer> performUpdate(
        Map<String, ?> inputParameters, Map<String, ?> connectionParameters, ActionContext context) {

        try (SingleConnectionDataSource dataSource = getDataSource(connectionParameters)) {
            return updateJdbcOperation.execute(inputParameters, dataSource);
        }
    }

    private ComponentDefinition getComponentDefinition(
        String description, String name, String icon, String title, String jdbcDriverClassName) {

        return component(name)
            .description(description)
            .icon(icon)
            .title(title)
            .connection(CONNECTION_DEFINITION)
            .actions(actionDefinitions)
            .triggers(triggerDefinitions)
            .clusterElements(JdbcItemWriter.clusterElementDefinition(urlTemplate, jdbcDriverClassName));
    }

    protected PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext triggerContext) {

        List<Map<String, Object>> allRows = closureParameters.getList(ALL_ROWS, new TypeReference<>() {}, List.of());
        List<Map<String, Object>> allRowsUpdate = new ArrayList<>();
        List<Map<String, Object>> newRows = new ArrayList<>();

        try (SingleConnectionDataSource dataSource = getDataSource(connectionParameters)) {
            String schema = MapUtils.getString(inputParameters, SCHEMA, "public");
            String table = MapUtils.getRequiredString(inputParameters, TABLE);
            String orderBy = MapUtils.getRequiredString(inputParameters, ORDER_BY);
            String orderDirection = MapUtils.getRequiredString(inputParameters, ORDER_DIRECTION);

            Object lastItem = closureParameters.get(LAST_ITEM);
            String queryStatement;

            if (lastItem == null) {
                queryStatement = "SELECT * FROM %s.%s ORDER BY %s %s".formatted(schema, table, orderBy, orderDirection);
            } else {
                if (Objects.equals(orderDirection, "ASC")) {
                    queryStatement = "SELECT * FROM %s.%s WHERE %s < %s ORDER BY %s ASC".formatted(
                        schema, table, orderBy, lastItem, orderBy);
                } else {
                    queryStatement = "SELECT * FROM %s.%s WHERE %s > %s ORDER BY %s DESC".formatted(
                        schema, table, orderBy, lastItem, orderBy);
                }
            }

            List<Map<String, Object>> query = JdbcExecutor.query(
                queryStatement, Map.of(), (ResultSet rs, int rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    ResultSetMetaData rsMetaData = rs.getMetaData();

                    int columnCount = rsMetaData.getColumnCount();

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = rsMetaData.getColumnName(i);

                        row.put(columnName, rs.getObject(i));
                    }

                    return row;
                }, dataSource);

            for (Map<String, Object> row : query) {
                allRowsUpdate.add(row);

                if (!allRows.contains(row)) {
                    newRows.add(row);
                }
            }

            Map<String, Object> last = allRowsUpdate.getLast();

            return new PollOutput(
                lastItem == null ? List.of() : newRows, Map.of(ALL_ROWS, allRowsUpdate, LAST_ITEM, last.get(orderBy)),
                false);
        }
    }

    private SingleConnectionDataSource getDataSource(Map<String, ?> connectionParameters) {
        return DataSourceFactory.getDataSource(connectionParameters, urlTemplate, jdbcDriverClassName);
    }

    public static List<ValueProperty<?>> createProperties(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        ActionContext actionContext) {

        List<Column> columns = inputParameters.getRequiredList(COLUMNS, Column.class);
        List<ValueProperty<?>> properties = new ArrayList<>();

        for (Column column : columns) {
            String name = column.name();

            if (name != null) {
                switch (Type.valueOf(column.type())) {
                    case ARRAY -> properties.add(
                        array(name)
                            .label(name)
                            .required(true));
                    case BOOLEAN -> properties.add(
                        bool(name)
                            .label(name)
                            .required(true));
                    case DATE -> properties.add(
                        date(name)
                            .label(name)
                            .required(true));
                    case DATE_TIME -> properties.add(
                        dateTime(name)
                            .label(name)
                            .required(true));
                    case INTEGER -> properties.add(
                        integer(name)
                            .label(name)
                            .required(true));
                    case NUMBER -> properties.add(
                        number(name)
                            .label(name)
                            .required(true));
                    case OBJECT -> properties.add(
                        object(name)
                            .label(name)
                            .required(true));
                    case STRING -> properties.add(
                        string(name)
                            .label(name)
                            .required(true));
                    case TIME -> properties.add(
                        time(name)
                            .label(name)
                            .required(true));
                    default -> throw new RuntimeException("");
                }
            }
        }

        return List.of(
            array(ROWS)
                .label("Values")
                .description("List of field values for corresponding field names.")
                .items(
                    object()
                        .properties(properties)));
    }

    private record Column(String name, String type) {
    }
}

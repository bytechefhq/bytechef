
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

package com.bytechef.hermes.component.jdbc.handler;

import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.connection;
import static com.bytechef.hermes.component.definition.ComponentDSL.dateTime;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.number;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.definition.DefinitionDSL.any;
import static com.bytechef.hermes.definition.DefinitionDSL.date;
import static com.bytechef.hermes.definition.DefinitionDSL.nullable;
import static com.bytechef.hermes.definition.DefinitionDSL.time;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.JdbcComponentDefinition;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.OutputSchemaFunction;
import com.bytechef.hermes.component.jdbc.sql.DataSourceFactory;
import com.bytechef.hermes.component.jdbc.executor.JdbcExecutor;
import com.bytechef.hermes.component.jdbc.operation.DeleteJdbcOperation;
import com.bytechef.hermes.component.jdbc.operation.ExecuteJdbcOperation;
import com.bytechef.hermes.component.jdbc.operation.InsertJdbcOperation;
import com.bytechef.hermes.component.jdbc.operation.QueryJdbcOperation;
import com.bytechef.hermes.component.jdbc.operation.UpdateJdbcOperation;
import com.bytechef.hermes.component.jdbc.constant.JdbcConstants;
import com.bytechef.hermes.definition.Property;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class JdbcComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition;
    private final String databaseJdbcName;
    private DeleteJdbcOperation deleteJdbcOperation;
    private ExecuteJdbcOperation executeJdbcOperation;
    private InsertJdbcOperation insertJdbcOperation;
    private final String jdbcDriverClassName;
    private QueryJdbcOperation queryJdbcOperation;
    private UpdateJdbcOperation updateJdbcOperation;

    public JdbcComponentHandler(JdbcComponentDefinition jdbcComponentDefinition) {
        this.componentDefinition = getComponentDefinition(
            OptionalUtils.orElse(jdbcComponentDefinition.getDescription(), null), jdbcComponentDefinition.getName(),
            jdbcComponentDefinition.getIcon(), jdbcComponentDefinition.getTitle());
        this.databaseJdbcName = jdbcComponentDefinition.getDatabaseJdbcName();
        this.jdbcDriverClassName = jdbcComponentDefinition.getJdbcDriverClassName();
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
        JdbcExecutor jdbcExecutor = new JdbcExecutor(databaseJdbcName, dataSourceFactory, jdbcDriverClassName);

        this.deleteJdbcOperation = new DeleteJdbcOperation(jdbcExecutor);
        this.executeJdbcOperation = new ExecuteJdbcOperation(jdbcExecutor);
        this.insertJdbcOperation = new InsertJdbcOperation(jdbcExecutor);
        this.queryJdbcOperation = new QueryJdbcOperation(jdbcExecutor);
        this.updateJdbcOperation = new UpdateJdbcOperation(jdbcExecutor);
    }

    protected Map<String, Integer> performDelete(Context context, Map<String, ?> inputParameters) {
        return deleteJdbcOperation.execute(context, inputParameters);
    }

    protected Map<String, Integer> performExecute(Context context, Map<String, ?> inputParameters) {
        return executeJdbcOperation.execute(context, inputParameters);
    }

    protected Map<String, Integer> performInsert(Context context, Map<String, ?> inputParameters) {
        return insertJdbcOperation.execute(context, inputParameters);
    }

    protected List<Map<String, Object>> performQuery(Context context, Map<String, ?> inputParameters) {
        return queryJdbcOperation.execute(context, inputParameters);
    }

    protected Map<String, Integer> performUpdate(Context context, Map<String, ?> inputParameters) {
        return updateJdbcOperation.execute(context, inputParameters);
    }

    protected static OutputSchemaFunction getOutputSchemaFunction() {
        // TODO
        return (connection, inputParameters) -> null;
    }

    private ComponentDefinition getComponentDefinition(String description, String name, String icon, String title) {
        return component(name)
            .description(description)
            .icon(icon)
            .title(title)
            .connection(
                connection()
                    .properties(
                        string(JdbcConstants.HOST).label("Host")
                            .required(true),
                        integer(JdbcConstants.PORT).label("Port")
                            .required(true),
                        string(JdbcConstants.DATABASE).label("Database")
                            .required(true),
                        string(JdbcConstants.USERNAME).label("Username")
                            .required(true),
                        string(JdbcConstants.PASSWORD)
                            .label("Password")
                            .controlType(Property.ControlType.INPUT_PASSWORD)
                            .required(true)))
            .actions(
                action(JdbcConstants.QUERY)
                    .title("Query")
                    .description("Execute an SQL query.")
                    .properties(
                        string(JdbcConstants.QUERY)
                            .label("Query")
                            .description(
                                "The raw SQL query to execute. You can use :property1 and :property2 in conjunction with parameters.")
                            .placeholder(
                                "SELECT id, name FROM customer WHERE age > :age AND height <= :height")
                            .required(true),
                        object(JdbcConstants.PARAMETERS)
                            .label("Parameters")
                            .description(
                                "The list of properties which should be used as query parameters.")
                            .additionalProperties(bool(), dateTime(), number(), string()))
                    .outputSchema(getOutputSchemaFunction(), any())
                    .perform((inputParameters, context) -> performQuery(context, inputParameters)),
                action(JdbcConstants.INSERT)
                    .title("Insert")
                    .description("Insert rows in database.")
                    .properties(
                        string(JdbcConstants.SCHEMA)
                            .label("Schema")
                            .description("Name of the schema the table belongs to.")
                            .required(true)
                            .defaultValue("public"),
                        string(JdbcConstants.TABLE)
                            .label("Table")
                            .description("Name of the table in which to insert data to.")
                            .required(true),
                        array(JdbcConstants.COLUMNS)
                            .label("Columns")
                            .description(
                                "The list of the properties which should used as columns for the new rows.")
                            .items(string()),
                        array(JdbcConstants.ROWS)
                            .label("Rows")
                            .description("List of rows.")
                            .items(object().additionalProperties(
                                array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(),
                                string(), time())))
                    .outputSchema(object().properties(integer("rows")))
                    .perform((inputParameters1, context1) -> performInsert(context1, inputParameters1)),
                action(JdbcConstants.UPDATE)
                    .title("Update")
                    .description("Update rows in database.")
                    .properties(
                        string(JdbcConstants.SCHEMA)
                            .label("Schema")
                            .description("Name of the schema the table belongs to.")
                            .required(true)
                            .defaultValue("public"),
                        string(JdbcConstants.TABLE)
                            .label("Table")
                            .description("Name of the table in which to update data in.")
                            .required(true),
                        array(JdbcConstants.COLUMNS)
                            .label("Columns")
                            .description(
                                "The list of the properties which should used as columns for the updated rows.")
                            .items(string()),
                        string(JdbcConstants.UPDATE_KEY)
                            .label("Update Key")
                            .description(
                                "The name of the property which decides which rows in the database should be updated.")
                            .placeholder("id"),
                        array(JdbcConstants.ROWS)
                            .label("Rows")
                            .description("List of rows.")
                            .items(object().additionalProperties(
                                array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(),
                                string(), time())))
                    .outputSchema(object().properties(integer("rows")))
                    .perform((inputParameters2, context2) -> performUpdate(context2, inputParameters2)),
                action(JdbcConstants.DELETE)
                    .title("Delete")
                    .description("Delete rows from database.")
                    .properties(
                        string(JdbcConstants.SCHEMA)
                            .label("Schema")
                            .description("Name of the schema the table belongs to.")
                            .required(true)
                            .defaultValue("public"),
                        string(JdbcConstants.TABLE)
                            .label("Table")
                            .description("Name of the table in which to update data in.")
                            .required(true),
                        string(JdbcConstants.DELETE_KEY)
                            .label("Update Key")
                            .description(
                                "Name of the property which decides which rows in the database should be deleted.")
                            .placeholder("id"),
                        array(JdbcConstants.ROWS)
                            .label("Rows")
                            .description("List of rows.")
                            .items(object().additionalProperties(
                                array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(),
                                string(), time())))
                    .outputSchema(object().properties(integer("rows")))
                    .perform((inputParameters3, context3) -> performDelete(context3, inputParameters3)),
                action(JdbcConstants.EXECUTE)
                    .title("Execute")
                    .description("Execute an SQL DML or DML statement.")
                    .properties(
                        string(JdbcConstants.EXECUTE)
                            .label("Execute")
                            .description(
                                "The raw DML or DDL statement to execute. You can use :property1 and :property2 in conjunction with parameters.")
                            .placeholder(
                                "UPDATE TABLE product set name = :name WHERE product > :product AND price <= :price")
                            .required(true),
                        array(JdbcConstants.ROWS)
                            .label("Rows")
                            .description("List of rows.")
                            .items(object().additionalProperties(
                                array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(),
                                string(), time())),
                        object(JdbcConstants.PARAMETERS)
                            .label("Parameters")
                            .description("The list of properties which should be used as parameters.")
                            .additionalProperties(bool(), dateTime(), number(), string()))
                    .outputSchema(object().properties(integer("rows")))
                    .perform((inputParameters4, context4) -> performExecute(context4, inputParameters4)));
    }
}

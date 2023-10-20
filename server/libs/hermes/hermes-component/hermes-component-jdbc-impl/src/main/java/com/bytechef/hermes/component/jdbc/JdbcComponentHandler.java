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

package com.bytechef.hermes.component.jdbc;

import static com.bytechef.hermes.component.ComponentDSL.action;
import static com.bytechef.hermes.component.ComponentDSL.array;
import static com.bytechef.hermes.component.ComponentDSL.createConnection;
import static com.bytechef.hermes.component.ComponentDSL.display;
import static com.bytechef.hermes.component.ComponentDSL.integer;
import static com.bytechef.hermes.component.ComponentDSL.object;
import static com.bytechef.hermes.component.ComponentDSL.string;
import static com.bytechef.hermes.component.jdbc.constants.JdbcConstants.COLUMNS;
import static com.bytechef.hermes.component.jdbc.constants.JdbcConstants.DATABASE;
import static com.bytechef.hermes.component.jdbc.constants.JdbcConstants.DELETE;
import static com.bytechef.hermes.component.jdbc.constants.JdbcConstants.DELETE_KEY;
import static com.bytechef.hermes.component.jdbc.constants.JdbcConstants.EXECUTE;
import static com.bytechef.hermes.component.jdbc.constants.JdbcConstants.HOST;
import static com.bytechef.hermes.component.jdbc.constants.JdbcConstants.INSERT;
import static com.bytechef.hermes.component.jdbc.constants.JdbcConstants.PARAMETERS;
import static com.bytechef.hermes.component.jdbc.constants.JdbcConstants.PASSWORD;
import static com.bytechef.hermes.component.jdbc.constants.JdbcConstants.PORT;
import static com.bytechef.hermes.component.jdbc.constants.JdbcConstants.QUERY;
import static com.bytechef.hermes.component.jdbc.constants.JdbcConstants.ROWS;
import static com.bytechef.hermes.component.jdbc.constants.JdbcConstants.SCHEMA;
import static com.bytechef.hermes.component.jdbc.constants.JdbcConstants.TABLE;
import static com.bytechef.hermes.component.jdbc.constants.JdbcConstants.UPDATE;
import static com.bytechef.hermes.component.jdbc.constants.JdbcConstants.UPDATE_KEY;
import static com.bytechef.hermes.component.jdbc.constants.JdbcConstants.USERNAME;

import com.bytechef.hermes.component.ComponentDSL;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.jdbc.definition.JdbcComponentDefinition;
import com.bytechef.hermes.component.jdbc.operation.DeleteJdbcOperation;
import com.bytechef.hermes.component.jdbc.operation.ExecuteJdbcOperation;
import com.bytechef.hermes.component.jdbc.operation.InsertJdbcOperation;
import com.bytechef.hermes.component.jdbc.operation.QueryJdbcOperation;
import com.bytechef.hermes.component.jdbc.operation.UpdateJdbcOperation;
import com.bytechef.hermes.definition.Display;

/**
 * @author Ivica Cardic
 */
public class JdbcComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition;
    private final DeleteJdbcOperation deleteJdbcOperation;
    private final ExecuteJdbcOperation executeJdbcOperation;
    private final InsertJdbcOperation insertJdbcOperation;
    private final QueryJdbcOperation queryJdbcOperation;
    private final UpdateJdbcOperation updateJdbcOperation;

    public JdbcComponentHandler(JdbcExecutor jdbcExecutor, JdbcComponentDefinition jdbcComponentDefinition) {
        this.deleteJdbcOperation = new DeleteJdbcOperation(jdbcExecutor);
        this.executeJdbcOperation = new ExecuteJdbcOperation(jdbcExecutor);
        this.insertJdbcOperation = new InsertJdbcOperation(jdbcExecutor);
        this.queryJdbcOperation = new QueryJdbcOperation(jdbcExecutor);
        this.updateJdbcOperation = new UpdateJdbcOperation(jdbcExecutor);
        this.componentDefinition =
                getComponentDefinition(jdbcComponentDefinition.getName(), jdbcComponentDefinition.getDisplay());
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    protected Object delete(Context context, ExecutionParameters executionParameters) {
        return deleteJdbcOperation.execute(context, executionParameters);
    }

    protected Object execute(Context context, ExecutionParameters executionParameters) {
        return executeJdbcOperation.execute(context, executionParameters);
    }

    protected Object insert(Context context, ExecutionParameters executionParameters) {
        return insertJdbcOperation.execute(context, executionParameters);
    }

    protected Object query(Context context, ExecutionParameters executionParameters) {
        return queryJdbcOperation.execute(context, executionParameters);
    }

    protected Object update(Context context, ExecutionParameters executionParameters) {
        return updateJdbcOperation.execute(context, executionParameters);
    }

    private ComponentDefinition getComponentDefinition(String name, Display display) {
        return ComponentDSL.createComponent(name)
                .display(display(display.getLabel()).icon(display.getIcon()))
                .connections(createConnection(name)
                        .display(display)
                        .properties(
                                string(HOST).label("Host").required(true),
                                integer(PORT).label("Port").required(true),
                                string(DATABASE).label("Database").required(true),
                                string(USERNAME).label("Username").required(true),
                                string(PASSWORD).label("Password").required(true)))
                .actions(
                        action(QUERY)
                                .display(display("Query").description("Execute an SQL query."))
                                .inputs(
                                        string(QUERY)
                                                .label("Query")
                                                .description(
                                                        "The raw SQL query to execute. You can use :property1 and :property2 in conjunction with parameters.")
                                                .placeholder(
                                                        "SELECT id, name FROM customer WHERE age > :age AND height <= :height")
                                                .required(true),
                                        object(PARAMETERS)
                                                .label("Parameters")
                                                .description(
                                                        "The list of properties which should be used as query parameters.")
                                                .properties(
                                                        ComponentDSL.bool(),
                                                        ComponentDSL.dateTime(),
                                                        ComponentDSL.number(),
                                                        ComponentDSL.string()))
                                .outputSchema()
                                .performFunction(this::query),
                        action(INSERT)
                                .display(display("Insert").description("Insert rows in database."))
                                .inputs(
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
                                                .description(
                                                        "The list of the properties which should used as columns for the new rows.")
                                                .items(ComponentDSL.string()),
                                        array(ROWS)
                                                .label("Rows")
                                                .description("List of rows.")
                                                .items(ComponentDSL.object().additionalProperties(true)))
                                .outputSchema()
                                .performFunction(this::insert),
                        action(UPDATE)
                                .display(display("Update").description("Update rows in database."))
                                .inputs(
                                        string(SCHEMA)
                                                .label("Schema")
                                                .description("Name of the schema the table belongs to.")
                                                .required(true)
                                                .defaultValue("public"),
                                        string(TABLE)
                                                .label("Table")
                                                .description("Name of the table in which to update data in.")
                                                .required(true),
                                        array(COLUMNS)
                                                .label("Columns")
                                                .description(
                                                        "The list of the properties which should used as columns for the updated rows.")
                                                .items(ComponentDSL.string()),
                                        string(UPDATE_KEY)
                                                .label("Update Key")
                                                .description(
                                                        "The name of the property which decides which rows in the database should be updated.")
                                                .placeholder("id"),
                                        array(ROWS)
                                                .label("Rows")
                                                .description("List of rows.")
                                                .items(ComponentDSL.object().additionalProperties(true)))
                                .outputSchema()
                                .performFunction(this::update),
                        action(DELETE)
                                .display(display("Delete").description("Delete rows from database."))
                                .inputs(
                                        string(SCHEMA)
                                                .label("Schema")
                                                .description("Name of the schema the table belongs to.")
                                                .required(true)
                                                .defaultValue("public"),
                                        string(TABLE)
                                                .label("Table")
                                                .description("Name of the table in which to update data in.")
                                                .required(true),
                                        string(DELETE_KEY)
                                                .label("Update Key")
                                                .description(
                                                        "Name of the property which decides which rows in the database should be deleted.")
                                                .placeholder("id"),
                                        array(ROWS)
                                                .label("Rows")
                                                .description("List of rows.")
                                                .items(ComponentDSL.object().additionalProperties(true)))
                                .outputSchema()
                                .performFunction(this::delete),
                        action(EXECUTE)
                                .display(display("Execute").description("Execute an SQL DML or DML statement."))
                                .inputs(
                                        string(EXECUTE)
                                                .label("Execute")
                                                .description(
                                                        "The raw DML or DDL statement to execute. You can use :property1 and :property2 in conjunction with parameters.")
                                                .placeholder(
                                                        "UPDATE TABLE product set name = :name WHERE product > :product AND price <= :price")
                                                .required(true),
                                        array(ROWS)
                                                .label("Rows")
                                                .description("List of rows.")
                                                .items(ComponentDSL.object().additionalProperties(true)),
                                        object(PARAMETERS)
                                                .label("Parameters")
                                                .description(
                                                        "The list of properties which should be used as parameters.")
                                                .properties(
                                                        ComponentDSL.bool(),
                                                        ComponentDSL.dateTime(),
                                                        ComponentDSL.number(),
                                                        ComponentDSL.string()))
                                .outputSchema()
                                .performFunction(this::execute));
    }
}

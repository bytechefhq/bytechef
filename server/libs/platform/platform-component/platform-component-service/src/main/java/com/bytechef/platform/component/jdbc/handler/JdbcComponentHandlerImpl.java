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

package com.bytechef.platform.component.jdbc.handler;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.nullable;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Property;
import com.bytechef.platform.component.definition.JdbcComponentDefinition;
import com.bytechef.platform.component.jdbc.DataSourceFactory;
import com.bytechef.platform.component.jdbc.constant.JdbcConstants;
import com.bytechef.platform.component.jdbc.datastream.JdbcItemWriter;
import com.bytechef.platform.component.jdbc.operation.DeleteJdbcOperation;
import com.bytechef.platform.component.jdbc.operation.ExecuteJdbcOperation;
import com.bytechef.platform.component.jdbc.operation.InsertJdbcOperation;
import com.bytechef.platform.component.jdbc.operation.QueryJdbcOperation;
import com.bytechef.platform.component.jdbc.operation.UpdateJdbcOperation;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public class JdbcComponentHandlerImpl implements ComponentHandler {

    private static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .properties(
            string(JdbcConstants.HOST).label("Host")
                .required(true),
            integer(JdbcConstants.PORT).label("Port")
                .required(true),
            string(JdbcConstants.DATABASE).label("Database")
                .required(true))
        .authorizations(
            authorization(Authorization.AuthorizationType.CUSTOM).properties(
                string(JdbcConstants.USERNAME)
                    .label("Username")
                    .required(true),
                string(JdbcConstants.PASSWORD)
                    .label("Password")
                    .controlType(Property.ControlType.PASSWORD)
                    .required(true)));

    private final List<ModifiableActionDefinition> actionDefinitions = List.of(
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
            .output()
            .perform(this::performQuery),
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
                    .label("Fields")
                    .description(
                        "The list of the table field names where corresponding values would be inserted.")
                    .items(string()),
                array(JdbcConstants.ROWS)
                    .label("Values")
                    .description("List of field values for corresponding field names")
                    .items(object().additionalProperties(
                        array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(),
                        string(), time())))
            .output()
            .perform(this::performInsert),
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
                    .label("Fields")
                    .description(
                        "The list of the table field names whose values would be updated.")
                    .items(string()),
                string(JdbcConstants.UPDATE_KEY)
                    .label("Update Key")
                    .description(
                        "The field name used as criteria to decide which rows in the database should be updated.")
                    .placeholder("id"),
                array(JdbcConstants.ROWS)
                    .label("Values")
                    .description("List of field values for corresponding field names.")
                    .items(object().additionalProperties(
                        array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(),
                        string(), time())))
            .output()
            .perform(this::performUpdate),
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
                    .label("Delete Key")
                    .description(
                        "Name of the field which decides which rows in the database should be deleted.")
                    .placeholder("id"),
                array(JdbcConstants.ROWS)
                    .label("Criteria Values")
                    .description("List of values that are used to test delete key.")
                    .items(object().additionalProperties(
                        array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(),
                        string(), time())))
            .output()
            .perform(this::performDelete),
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
                array(JdbcConstants.COLUMNS)
                    .label("Fields to select")
                    .description("List of fields to select from.")
                    .items(object().additionalProperties(
                        array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(),
                        string(), time())),
                object(JdbcConstants.PARAMETERS)
                    .label("Parameters")
                    .description(
                        "The list of values which should be used to replace corresponding criteria parameters.")
                    .additionalProperties(bool(), dateTime(), number(), string()))
            .output()
            .perform(this::performExecute));

    private final ComponentDefinition componentDefinition;
    private final String databaseJdbcName;
    private final DeleteJdbcOperation deleteJdbcOperation;
    private final ExecuteJdbcOperation executeJdbcOperation;
    private final InsertJdbcOperation insertJdbcOperation;
    private final String jdbcDriverClassName;
    private final QueryJdbcOperation queryJdbcOperation;
    private final UpdateJdbcOperation updateJdbcOperation;

    public JdbcComponentHandlerImpl(JdbcComponentDefinition jdbcComponentDefinition) {
        this.databaseJdbcName = jdbcComponentDefinition.getDatabaseJdbcName();
        this.jdbcDriverClassName = jdbcComponentDefinition.getJdbcDriverClassName();

        this.componentDefinition = getComponentDefinition(
            OptionalUtils.orElse(jdbcComponentDefinition.getDescription(), null), jdbcComponentDefinition.getName(),
            OptionalUtils.orElse(jdbcComponentDefinition.getIcon(), null),
            OptionalUtils.orElse(jdbcComponentDefinition.getTitle(), null), databaseJdbcName, jdbcDriverClassName);

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
        String description, String name, String icon, String title, String databaseJdbcName,
        String jdbcDriverClassName) {

        return component(name)
            .description(description)
            .icon(icon)
            .title(title)
            .connection(CONNECTION_DEFINITION)
            .actions(actionDefinitions)
            .clusterElements(JdbcItemWriter.clusterElementDefinition(databaseJdbcName, jdbcDriverClassName));
    }

    private SingleConnectionDataSource getDataSource(Map<String, ?> connectionParameters) {
        return DataSourceFactory.getDataSource(connectionParameters, databaseJdbcName, jdbcDriverClassName);
    }
}

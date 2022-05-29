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

package com.integri.atlas.task.handler.jdbc;

import static com.integri.atlas.task.definition.dsl.DSL.ARRAY_PROPERTY;
import static com.integri.atlas.task.definition.dsl.DSL.BOOLEAN_PROPERTY;
import static com.integri.atlas.task.definition.dsl.DSL.DATE_TIME_PROPERTY;
import static com.integri.atlas.task.definition.dsl.DSL.NUMBER_PROPERTY;
import static com.integri.atlas.task.definition.dsl.DSL.OBJECT_PROPERTY;
import static com.integri.atlas.task.definition.dsl.DSL.OPERATION;
import static com.integri.atlas.task.definition.dsl.DSL.STRING_PROPERTY;

import com.integri.atlas.task.definition.dsl.TaskOperation;

/**
 * @author Ivica Cardic
 */
public final class JdbcTaskConstants {

    public static final String PROPERTY_TABLE = "table";
    public static final String OPERATION_QUERY = "query";
    public static final String OPERATION_INSERT = "insert";
    public static final String OPERATION_UPDATE = "update";
    public static final String OPERATION_DELETE = "delete";
    public static final String OPERATION_EXECUTE = "execute";
    public static final String PROPERTY_QUERY = "query";
    public static final String PROPERTY_PARAMETERS = "parameters";
    public static final String PROPERTY_SCHEMA = "schema";
    public static final String PROPERTY_COLUMNS = "columns";
    public static final String PROPERTY_ROWS = "rows";
    public static final String PROPERTY_DELETE_KEY = "deleteKey";
    public static final String PROPERTY_EXECUTE = "execute";
    public static final String PROPERTY_UPDATE_KEY = "updateKey";

    public static TaskOperation[] TASK_OPERATIONS = {
        OPERATION(OPERATION_QUERY)
            .displayName("Query")
            .description("Execute an SQL query.")
            .inputs(
                STRING_PROPERTY(PROPERTY_QUERY)
                    .displayName("Query")
                    .description(
                        "The raw SQL query to execute. You can use expressions or :property1 and :property2 in conjunction with parameters."
                    )
                    .placeholder("SELECT id, name FROM customer WHERE age > :age AND height <= :height")
                    .required(true),
                OBJECT_PROPERTY(PROPERTY_PARAMETERS)
                    .displayName("Parameters")
                    .description("The list of properties which should be used as query parameters.")
                    .properties(BOOLEAN_PROPERTY(), DATE_TIME_PROPERTY(), NUMBER_PROPERTY(), STRING_PROPERTY())
            ),
        OPERATION(OPERATION_INSERT)
            .displayName("Insert")
            .description("Insert rows in database.")
            .inputs(
                STRING_PROPERTY(PROPERTY_SCHEMA)
                    .displayName("Schema")
                    .description("Name of the schema the table belongs to.")
                    .required(true)
                    .defaultValue("public"),
                STRING_PROPERTY(PROPERTY_TABLE)
                    .displayName("Table")
                    .description("Name of the table in which to insert data to.")
                    .required(true),
                ARRAY_PROPERTY(PROPERTY_COLUMNS)
                    .displayName("Columns")
                    .description("The list of the properties which should used as columns for the new rows.")
                    .items(STRING_PROPERTY()),
                ARRAY_PROPERTY(PROPERTY_ROWS)
                    .displayName("Rows")
                    .description("List of rows.")
                    .items(OBJECT_PROPERTY().additionalProperties(true))
            ),
        OPERATION(OPERATION_UPDATE)
            .displayName("Update")
            .description("Update rows in database.")
            .inputs(
                STRING_PROPERTY(PROPERTY_SCHEMA)
                    .displayName("Schema")
                    .description("Name of the schema the table belongs to.")
                    .required(true)
                    .defaultValue("public"),
                STRING_PROPERTY(PROPERTY_TABLE)
                    .displayName("Table")
                    .description("Name of the table in which to update data in.")
                    .required(true),
                ARRAY_PROPERTY(PROPERTY_COLUMNS)
                    .displayName("Columns")
                    .description("The list of the properties which should used as columns for the updated rows.")
                    .items(STRING_PROPERTY()),
                STRING_PROPERTY(PROPERTY_UPDATE_KEY)
                    .displayName("Update Key")
                    .description("The name of the property which decides which rows in the database should be updated.")
                    .placeholder("id"),
                ARRAY_PROPERTY(PROPERTY_ROWS)
                    .displayName("Rows")
                    .description("List of rows.")
                    .items(OBJECT_PROPERTY().additionalProperties(true))
            ),
        OPERATION(OPERATION_DELETE)
            .displayName("Delete")
            .description("Delete rows from database.")
            .inputs(
                STRING_PROPERTY(PROPERTY_SCHEMA)
                    .displayName("Schema")
                    .description("Name of the schema the table belongs to.")
                    .required(true)
                    .defaultValue("public"),
                STRING_PROPERTY(PROPERTY_TABLE)
                    .displayName("Table")
                    .description("Name of the table in which to update data in.")
                    .required(true),
                STRING_PROPERTY(PROPERTY_DELETE_KEY)
                    .displayName("Update Key")
                    .description("Name of the property which decides which rows in the database should be deleted.")
                    .placeholder("id"),
                ARRAY_PROPERTY(PROPERTY_ROWS)
                    .displayName("Rows")
                    .description("List of rows.")
                    .items(OBJECT_PROPERTY().additionalProperties(true))
            ),
        OPERATION(OPERATION_EXECUTE)
            .displayName("Execute")
            .description("Execute an SQL DML or DML statement.")
            .inputs(
                STRING_PROPERTY(PROPERTY_EXECUTE)
                    .displayName("Execute")
                    .description(
                        "The raw DML or DDL statement to execute. You can use expressions or :property1 and :property2 in conjunction with parameters."
                    )
                    .placeholder("UPDATE TABLE product set name = :name WHERE product > :product AND price <= :price")
                    .required(true),
                ARRAY_PROPERTY(PROPERTY_ROWS)
                    .displayName("Rows")
                    .description("List of rows.")
                    .items(OBJECT_PROPERTY().additionalProperties(true)),
                OBJECT_PROPERTY(PROPERTY_PARAMETERS)
                    .displayName("Parameters")
                    .description("The list of properties which should be used as parameters.")
                    .properties(BOOLEAN_PROPERTY(), DATE_TIME_PROPERTY(), NUMBER_PROPERTY(), STRING_PROPERTY())
            ),
    };

    private JdbcTaskConstants() {}
}

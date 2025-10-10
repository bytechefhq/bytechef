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

package com.bytechef.component.snowflake.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableDynamicPropertiesProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.snowflake.util.SnowflakePropertiesUtils;
import com.bytechef.component.snowflake.util.SnowflakeUtils;

/**
 * @author Nikolina Spehar
 */
public class SnowflakeConstants {

    public static final String ACCOUNT_IDENTIFIER = "account_identifier";
    public static final String CONDITION = "condition";
    public static final String DATABASE = "database";
    public static final String DATATYPE = "datatype";
    public static final String NAME = "name";
    public static final String SCHEMA = "schema";
    public static final String STATEMENT = "statement";
    public static final String TABLE = "table";
    public static final String VALUES = "values";

    public static final ModifiableStringProperty DATABASE_PROPERTY = string(DATABASE)
        .label("Database")
        .options((OptionsFunction<String>) SnowflakeUtils::getDatabaseNameOptions)
        .required(true);

    public static final ModifiableStringProperty SCHEMA_PROPERTY = string(SCHEMA)
        .label("Schema")
        .options((OptionsFunction<String>) SnowflakeUtils::getSchemaNameOptions)
        .optionsLookupDependsOn(DATABASE)
        .required(true);

    public static final ModifiableStringProperty TABLE_PROPERTY = string(TABLE)
        .label("Table")
        .options((OptionsFunction<String>) SnowflakeUtils::getTableNameOptions)
        .optionsLookupDependsOn(SCHEMA, DATABASE)
        .required(true);

    public static final ModifiableDynamicPropertiesProperty VALUES_DYNAMIC_PROPERTY = dynamicProperties(VALUES)
        .propertiesLookupDependsOn(TABLE)
        .properties(SnowflakePropertiesUtils::createPropertiesForColumn)
        .required(true);

    public static final ModifiableObjectProperty SQL_STATEMENT_RESPONSE = object()
        .properties(
            object("resultSetMetaData")
                .properties(
                    integer("numRows")
                        .description("Number of rows inserted."),
                    string("format"),
                    array("rowType")
                        .items(
                            string("name")
                                .description("Name of the column."),
                            string("database")
                                .description("Database to which row was inserted."),
                            string("schema")
                                .description("Schema to which row was inserted."),
                            string("table")
                                .description("Table to which row was inserted."),
                            object("scale"),
                            object("precision"),
                            integer("length"),
                            string("type"),
                            bool("nullable"),
                            integer("byteLength"),
                            object("collation")),
                    array("partitionInfo")
                        .items(
                            integer("rowCount"),
                            integer("uncompressedSize"))),
            array("data"),
            string("code"),
            string("statementStatusUrl"),
            string("sqlState"),
            string("statementHandle"),
            string("message"),
            date("createdOn"),
            array("stats"));

    private SnowflakeConstants() {
    }
}

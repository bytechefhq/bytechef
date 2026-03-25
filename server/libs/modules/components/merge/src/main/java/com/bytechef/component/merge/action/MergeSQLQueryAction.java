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

package com.bytechef.component.merge.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.merge.constant.MergeConstants.INPUTS;
import static com.bytechef.component.merge.constant.MergeConstants.SQL_QUERY;
import static com.bytechef.component.merge.util.MergeUtils.flatten;
import static com.bytechef.component.merge.util.SQLUtils.createTable;
import static com.bytechef.component.merge.util.SQLUtils.executeQuery;
import static com.bytechef.component.merge.util.SQLUtils.insertData;
import static com.bytechef.component.merge.util.SQLUtils.validateSQL;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;

/**
 * @author Ivona Pavela
 */
public class MergeSQLQueryAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sqlQuery")
        .title("SQL Query")
        .description(
            "Write SQL Query to merge the data with DuckDB.")
        .properties(
            array(INPUTS)
                .label("Inputs")
                .description("A collection of objects, arrays, or nested structures to be merged.")
                .minItems(2)
                .items(
                    object()
                        .properties(
                            string("tableName")
                                .label("Table Name")
                                .description("The name of the table to insert data into.")
                                .required(true),
                            object("value"))
                        .expressionEnabled(false)
                        .required(true))
                .expressionEnabled(false)
                .required(true),
            string(SQL_QUERY)
                .label("SQL Query")
                .description("The SQL query to execute.")
                .placeholder("SELECT * FROM tableName;")
                .required(true))
        .output()
        .help("", "https://docs.bytechef.io/reference/components/merge_v1#merge-sql-query")
        .perform(MergeSQLQueryAction::perform);

    private MergeSQLQueryAction() {
    }

    public static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        List<TableConfiguration> inputs = inputParameters.getList(INPUTS, TableConfiguration.class, List.of());
        String query = inputParameters.getRequiredString(SQL_QUERY);

        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:")) {

            for (TableConfiguration input : inputs) {
                String tableName = input.tableName;
                List<Map<String, Object>> rows = flatten(input.value);

                createTable(conn, tableName, rows);
                insertData(conn, tableName, rows);
            }

            validateSQL(conn, query);

            return executeQuery(conn, query);

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute SQL query: " + e.getMessage(), e);
        }
    }

    private record TableConfiguration(String tableName, Object value) {
    }
}

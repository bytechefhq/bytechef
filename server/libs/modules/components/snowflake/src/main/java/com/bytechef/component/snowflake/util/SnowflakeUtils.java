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

package com.bytechef.component.snowflake.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.DATABASE;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.SCHEMA;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.STATEMENT;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.TABLE;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class SnowflakeUtils {

    public static Object executeStatement(Context context, String sqlStatement) {
        return context.http(http -> http.post("/api/v2/statements"))
            .body(Http.Body.of(Map.of(STATEMENT, sqlStatement)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }

    public static List<Option<String>> getColumnOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        String columnString = getTableColumns(inputParameters, context);

        List<String> columnList = new ArrayList<>(Arrays.asList(columnString.split(",")));

        List<Option<String>> options = new ArrayList<>();

        for (String column : columnList) {
            options.add(option(column, column));
        }

        return options;
    }

    public static String getColumnUpdateStatement(String columns, String values) {
        String[] columnArray = columns.split(",");
        String[] valueArray = values.split(",");

        if (columnArray.length != valueArray.length) {
            throw new IllegalArgumentException("Columns and values do not match.");
        }

        StringBuilder updateStatementBuilder = new StringBuilder();

        for (int i = 0; i < columnArray.length; i++) {
            updateStatementBuilder.append(columnArray[i])
                .append("=")
                .append(valueArray[i])
                .append(",");
        }

        updateStatementBuilder.deleteCharAt(updateStatementBuilder.length() - 1);
        return updateStatementBuilder.toString();
    }

    public static List<Option<String>> getDatabaseNameOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Map<String, Object>> result = context.http(http -> http.get("/api/v2/databases"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(result);
    }

    public static List<Option<String>> getSchemaNameOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Map<String, Object>> result = context.http(http -> http.get(
            "/api/v2/databases/%s/schemas".formatted(
                inputParameters.getRequiredString(DATABASE))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(result);
    }

    public static String getTableColumns(Parameters inputParameters, Context context) {
        Map<String, Object> table = context
            .http(http -> http.get(
                "/api/v2/databases/%s/schemas/%s/tables/%s".formatted(
                    inputParameters.getRequiredString(DATABASE),
                    inputParameters.getRequiredString(SCHEMA),
                    inputParameters.getRequiredString(TABLE))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        StringBuilder stringBuilder = new StringBuilder();

        if (table.get("columns") instanceof List<?> columnList) {
            for (Object element : columnList) {
                if (element instanceof Map<?, ?> column) {
                    stringBuilder.append(column.get("name"))
                        .append(",");
                }
            }
        }

        stringBuilder.deleteCharAt(stringBuilder.length() - 1);

        return stringBuilder.toString();
    }

    public static List<Option<String>> getTableNameOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Map<String, Object>> result = context.http(http -> http.get(
            "/api/v2/databases/%s/schemas/%s/tables".formatted(
                inputParameters.getRequiredString(DATABASE), inputParameters.getRequiredString(SCHEMA))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(result);
    }

    private static List<Option<String>> getOptions(List<Map<String, Object>> dataList) {
        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> data : dataList) {
            String name = (String) data.get("name");

            options.add(option(name, name));
        }

        return options;
    }
}

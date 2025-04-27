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

package com.bytechef.component.snowflake.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.DATABASE;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.DATATYPE;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.NAME;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Nikolina Spehar
 */
public class SnowflakeUtils {

    public static Object executeStatement(Context context, String sqlStatement) {
        return context.http(http -> http.post("/statements"))
            .body(Http.Body.of(Map.of(STATEMENT, sqlStatement)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }

    private static String extractBaseDataType(String sqlType) {
        Pattern pattern = Pattern.compile("^([A-Za-z]+)");
        Matcher matcher = pattern.matcher(sqlType);

        if (matcher.find()) {
            return matcher.group(1)
                .toUpperCase();
        }
        return sqlType.toUpperCase();
    }

    public static List<Option<String>> getDatabaseNameOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Map<String, Object>> result = context.http(http -> http.get("/databases"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(result);
    }

    public static List<Option<String>> getSchemaNameOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Map<String, Object>> result = context
            .http(http -> http.get("/databases/%s/schemas".formatted(inputParameters.getRequiredString(DATABASE))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(result);
    }

    public static List<Map<String, String>> getTableColumns(Parameters inputParameters, Context context) {
        Map<String, Object> table = context
            .http(http -> http.get(
                "/databases/%s/schemas/%s/tables/%s".formatted(
                    inputParameters.getRequiredString(DATABASE),
                    inputParameters.getRequiredString(SCHEMA),
                    inputParameters.getRequiredString(TABLE))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Map<String, String>> columns = new ArrayList<>();

        if (table.get("columns") instanceof List<?> columnList) {
            for (Object element : columnList) {
                if (element instanceof Map<?, ?> column) {
                    Map<String, String> columnMap = new HashMap<>();

                    columnMap.put(NAME, (String) column.get(NAME));
                    columnMap.put(DATATYPE, extractBaseDataType((String) column.get(DATATYPE)));

                    columns.add(columnMap);
                }
            }
        }

        return columns;
    }

    public static List<Option<String>> getTableNameOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Map<String, Object>> result = context
            .http(http -> http.get(
                "/databases/%s/schemas/%s/tables".formatted(
                    inputParameters.getRequiredString(DATABASE), inputParameters.getRequiredString(SCHEMA))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(result);
    }

    private static List<Option<String>> getOptions(List<Map<String, Object>> dataList) {
        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> data : dataList) {
            String name = (String) data.get(NAME);

            options.add(option(name, name));
        }

        return options;
    }
}

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

package com.bytechef.component.nocodb.util;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.BASE_ID;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.RECORDS;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.TABLE_COLUMNS;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.TABLE_ID;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.TITLE;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.WORKSPACE_ID;

import com.bytechef.component.definition.ActionDefinition.PropertiesFunction;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.nocodb.constant.ColumnType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class NocoDbUtils {

    private NocoDbUtils() {
    }

    public static PropertiesFunction createPropertiesForRecord(boolean isNewRecord) {
        return (inputParameters, connectionParameters, dependencyPaths, context) -> {

            Map<String, Object> body = context
                .http(http -> http.get("/api/v2/meta/tables/" + inputParameters.getRequiredString(TABLE_ID)))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            List<ValueProperty<?>> properties = new ArrayList<>();

            if (body.get("columns") instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> map) {
                        String uidt = (String) map.get("uidt");

                        ColumnType columnType = ColumnType.getColumnType(uidt);

                        if (columnType != null) {
                            String name = (String) map.get(TITLE);
                            String label = name.replaceAll(" ", "");

                            switch (columnType) {
                                case CHECKBOX -> properties.add(
                                    bool(name)
                                        .label(label)
                                        .required(false));
                                case SINGLE_LINE_TEXT, PHONE_NUMBER, EMAIL, URL -> properties.add(
                                    string(name)
                                        .label(label)
                                        .required(false));
                                case LONG_TEXT -> properties.add(
                                    string(name)
                                        .label(label)
                                        .controlType(ControlType.TEXT_AREA)
                                        .required(false));
                                case NUMBER, DECIMAL, PERCENT, RATING, CURRENCY, YEAR -> properties.add(
                                    number(name)
                                        .label(label)
                                        .required(false));
                                case MULTISELECT -> properties.add(
                                    array(name)
                                        .label(label)
                                        .items(string())
                                        .options(getColumnOptions(map))
                                        .required(false));
                                case SINGLE_SELECT -> properties.add(
                                    string(name)
                                        .label(label)
                                        .options(getColumnOptions(map))
                                        .required(false));
                                case DATE -> properties.add(
                                    date(name)
                                        .label(label)
                                        .required(false));
                                case TIME -> properties.add(
                                    time(name)
                                        .label(label)
                                        .required(false));
                                case DATETIME -> properties.add(
                                    dateTime(name)
                                        .label(label)
                                        .required(false));
                                default -> {
                                }
                            }
                        }
                    }
                }
            }

            if (!isNewRecord) {
                properties.addFirst(
                    integer("Id")
                        .description("Id of the record to update.")
                        .required(true));
            }

            return List.of(
                array(RECORDS)
                    .label("Records")
                    .description("Records to be created/updated.")
                    .items(object().properties(properties))
                    .minItems(1)
                    .required(true));
        };
    }

    public static List<Option<String>> getBaseIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        Map<String, Object> body = context
            .http(http -> http.get(
                "/api/v2/meta/workspaces/" + inputParameters.getRequiredString(WORKSPACE_ID) + "/bases"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body);
    }

    public static List<Option<String>> getFieldNameOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        Map<String, Object> body = context
            .http(http -> http.get("/api/v2/meta/tables/" + inputParameters.getRequiredString(TABLE_ID)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("columns") instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    String name = (String) map.get(TITLE);

                    options.add(option(name, name));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getTableIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        Map<String, Object> body = context
            .http(http -> http.get("/api/v2/meta/bases/" + inputParameters.getRequiredString(BASE_ID) + "/tables"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body);
    }

    public static List<Option<String>> getWorkspaceIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        Map<String, Object> body = context.http(http -> http.get("/api/v1/workspaces"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body);
    }

    public static List<Map<String, Object>> transformRecordsForInsertion(Parameters inputParameters) {
        Map<String, List<Map<String, Object>>> tableColumns = inputParameters.getMap(
            TABLE_COLUMNS, new TypeReference<>() {});

        List<Map<String, Object>> records = tableColumns.get(RECORDS);
        List<Map<String, Object>> newRecords = new ArrayList<>();

        for (Map<String, Object> record : records) {
            Map<String, Object> valueMap = new HashMap<>();

            for (Map.Entry<String, Object> entry : record.entrySet()) {
                Object value = entry.getValue();

                if (value instanceof List<?> list) {
                    List<String> valueList = new ArrayList<>();

                    for (Object item : list) {
                        valueList.add(item.toString());
                    }

                    String join = String.join(",", valueList);

                    valueMap.put(entry.getKey(), join);
                } else {
                    valueMap.put(entry.getKey(), value);
                }
            }

            newRecords.add(valueMap);
        }

        return newRecords;
    }

    private static List<Option<String>> getColumnOptions(Map<?, ?> map) {
        List<Option<String>> options = new ArrayList<>();

        if (map.get("colOptions") instanceof Map<?, ?> colOptions &&
            colOptions.get("options") instanceof List<?> list) {

            for (Object option : list) {
                if (option instanceof Map<?, ?> optionMap) {
                    String title = (String) optionMap.get(TITLE);

                    options.add(option(title, title));
                }
            }
        }

        return options;
    }

    private static List<Option<String>> getOptions(Map<String, Object> body) {
        List<Option<String>> options = new ArrayList<>();

        if (body.get("list") instanceof List<?> list) {
            for (Object object : list) {
                if (object instanceof Map<?, ?> map) {
                    options.add(option((String) map.get(TITLE), (String) map.get("id")));
                }
            }
        }

        return options;
    }
}

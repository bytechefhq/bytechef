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

package com.bytechef.component.microsoft.excel.util;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.COLUMN;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.ID;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.IS_THE_FIRST_ROW_HEADER;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.NAME;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.ROW;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.ROW_NUMBER;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.UPDATE_WHOLE_ROW;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.VALUE;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.VALUES;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKBOOK_ID;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKSHEET_NAME;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Monika Domiter
 */
public class MicrosoftExcelUtils {

    private MicrosoftExcelUtils() {
    }

    public static List<Property.ValueProperty<?>> createPropertiesForNewRow(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        ActionContext actionContext) {

        boolean isFirstRowHeader = inputParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER);

        if (isFirstRowHeader) {
            ModifiableObjectProperty updatedRow = object(VALUES)
                .label("Values")
                .properties(createPropertiesBasedOnHeader(inputParameters, actionContext))
                .required(true);

            return List.of(updatedRow);
        } else {
            ModifiableArrayProperty updatedRow = array(VALUES)
                .label("Values")
                .items(bool(), number(), string())
                .required(true);

            return List.of(updatedRow);
        }
    }

    public static List<Property.ValueProperty<?>> createPropertiesToUpdateRow(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        ActionContext actionContext) {

        boolean isFirstRowHeader = inputParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER);
        boolean updateWholeRow = inputParameters.getRequiredBoolean(UPDATE_WHOLE_ROW);

        if (isFirstRowHeader) {
            if (updateWholeRow) {
                return List.of(
                    object(VALUES)
                        .label("Values")
                        .properties(createPropertiesBasedOnHeader(inputParameters, actionContext))
                        .required(true));
            } else {
                return List.of(
                    array(VALUES)
                        .label("Values")
                        .items(
                            object()
                                .properties(
                                    string(COLUMN)
                                        .label("Column")
                                        .description("Column to update.")
                                        .options(getColumnOptions(inputParameters, actionContext))
                                        .required(true),
                                    string(VALUE)
                                        .label("Column Value")
                                        .defaultValue("")
                                        .required(true)))
                        .required(true));
            }
        } else {
            if (updateWholeRow) {
                return List.of(
                    array(VALUES)
                        .label("Values")
                        .items(bool(), number(), string())
                        .required(true));
            } else {
                return List.of(
                    array(VALUES)
                        .label("Values")
                        .items(
                            object()
                                .properties(
                                    string(COLUMN)
                                        .label("Column Label")
                                        .description("Label of the column to update. Example: A, B, C, ...")
                                        .exampleValue("A")
                                        .required(true),
                                    string(VALUE)
                                        .label("Column Value")
                                        .defaultValue("")
                                        .required(true))));
            }
        }
    }

    public static String getLastUsedColumnLabel(Parameters inputParameters, Context context) {
        Map<String, Object> body = context
            .http(http -> http.get(
                "/me/drive/items/%s/workbook/worksheets/%s/usedRange"
                    .formatted(
                        inputParameters.getRequiredString(WORKBOOK_ID),
                        inputParameters.getRequiredString(WORKSHEET_NAME))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("columnCount") instanceof Integer integer) {
            return columnToLabel(integer, false);
        }

        throw new IllegalStateException("Failed to get last used column");
    }

    public static Integer getLastUsedRowIndex(Parameters inputParameters, Context context) {
        Map<String, Object> body = context
            .http(http -> http.get("/me/drive/items/%s/workbook/worksheets/%s/usedRange"
                .formatted(inputParameters.getRequiredString(WORKBOOK_ID),
                    inputParameters.getRequiredString(WORKSHEET_NAME))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("rowCount") instanceof Integer integer) {
            if (integer == 1 && body.get(VALUES) instanceof List<?> list) {
                return list.stream()
                    .filter(obj -> obj instanceof List<?> innerList && innerList.stream()
                        .anyMatch(innerObj -> !((String) innerObj).isEmpty()))
                    .findFirst()
                    .map(ignore -> integer)
                    .orElse(0);
            }

            return integer;
        }

        throw new IllegalStateException("Failed to get last used row");
    }

    public static Map<String, Object> getMapOfValuesForRow(
        Parameters inputParameters, Context context, List<Object> row) {

        Map<String, Object> valuesMap;

        if (inputParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER)) {
            List<Object> firstRow = MicrosoftExcelRowUtils.getRowFromWorksheet(inputParameters, context, 1);

            valuesMap = IntStream.range(0, row.size())
                .boxed()
                .collect(
                    Collectors.toMap(i -> String.valueOf(firstRow.get(i)),
                        i -> {
                            Object value = row.get(i);

                            return value == null ? "" : String.valueOf(value);
                        }, (a, b) -> b, LinkedHashMap::new));
        } else {
            valuesMap = IntStream.range(0, row.size())
                .boxed()
                .collect(
                    Collectors.toMap(
                        i -> columnToLabel(i + 1, true), i -> String.valueOf(row.get(i)),
                        (a, b) -> b,
                        LinkedHashMap::new));
        }

        return valuesMap;
    }

    public static List<Object> getRowValues(Parameters inputParameters) {
        List<Object> row = new ArrayList<>();

        Map<String, Object> rowMap = inputParameters.getRequiredMap(ROW, Object.class);

        if (rowMap.get(VALUES) instanceof Map<?, ?> map) {
            row.addAll(map.values());
        } else if (rowMap.get(VALUES) instanceof List<?> list) {
            row.addAll(list);
        }

        return row;
    }

    public static List<Object> getUpdatedRowValues(Parameters inputParameters, Context context) {
        List<Object> row = new ArrayList<>();

        if (inputParameters.get(ROW) instanceof Map<?, ?> rowMap) {
            Object values = rowMap.get(VALUES);

            if (values instanceof Map<?, ?> map) {
                row = map.values()
                    .stream()
                    .map(value -> Objects.requireNonNullElse(value, ""))
                    .toList();
            } else if (values instanceof List<?> list) {
                if (inputParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER)) {

                    List<Object> firstRow =
                        MicrosoftExcelRowUtils.getRowFromWorksheet(inputParameters, context, 1);
                    List<Object> rowToUpdate = MicrosoftExcelRowUtils.getRowFromWorksheet(inputParameters,
                        context, inputParameters.getRequiredInteger(ROW_NUMBER));

                    for (Object o : list) {
                        if (o instanceof Map<?, ?> map) {
                            int indexOfColumnToUpdate = firstRow.indexOf(map.get(COLUMN));

                            rowToUpdate.set(indexOfColumnToUpdate, map.get(VALUE));
                        }
                    }

                    return rowToUpdate;
                } else {
                    if (inputParameters.getRequiredBoolean(UPDATE_WHOLE_ROW)) {
                        row = list.stream()
                            .map(item -> Objects.requireNonNullElse(item, ""))
                            .toList();

                    } else {
                        List<Object> rowToUpdate = MicrosoftExcelRowUtils.getRowFromWorksheet(inputParameters,
                            context, inputParameters.getRequiredInteger(ROW_NUMBER));

                        for (Object o : list) {
                            if (o instanceof Map<?, ?> map) {
                                int indexOfColumnToUpdate = labelToColumn((String) map.get(COLUMN)) - 1;

                                if (indexOfColumnToUpdate >= rowToUpdate.size()) {
                                    for (int i = rowToUpdate.size(); i <= indexOfColumnToUpdate; i++) {
                                        rowToUpdate.add("");
                                    }
                                }

                                rowToUpdate.set(indexOfColumnToUpdate, map.get(VALUE));
                            }
                        }

                        return rowToUpdate;
                    }
                }
            }
        }

        return row;
    }

    public static List<Option<String>> getWorkbookIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, Object> body = context
            .http(http -> http.get("/me/drive/items/root/search(q='.xlsx')"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body, ID);
    }

    public static List<Option<String>> getWorksheetNameOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, Object> body = context
            .http(http -> http
                .get("/me/drive/items/%s//workbook/worksheets/"
                    .formatted(inputParameters.getRequiredString(WORKBOOK_ID))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body, NAME);
    }

    protected static String columnToLabel(int columnNumber, boolean addColumnPrefix) {
        StringBuilder columnName = new StringBuilder();

        while (columnNumber > 0) {
            int modulo = (columnNumber - 1) % 26;
            columnName.insert(0, (char) (65 + modulo));
            columnNumber = (columnNumber - modulo) / 26;
        }

        return addColumnPrefix ? "column_" + columnName : columnName.toString();
    }

    private static Integer labelToColumn(String label) {
        int columnNumber = 0;

        for (int i = 0; i < label.length(); i++) {
            columnNumber = columnNumber * 26 + label.charAt(i) - 'A' + 1;
        }

        return columnNumber;
    }

    private static List<ModifiableValueProperty<?, ?>> createPropertiesBasedOnHeader(
        Parameters inputParameters, ActionContext actionContext) {

        List<Object> firstRow = MicrosoftExcelRowUtils.getRowFromWorksheet(inputParameters, actionContext, 1);

        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        for (Object item : firstRow) {
            String label = item.toString();

            if (!label.isEmpty()) {
                properties.add(
                    string(label.replaceAll(" ", "_"))
                        .label(label)
                        .defaultValue(""));
            }
        }

        return properties;
    }

    private static List<Option<String>> getColumnOptions(Parameters inputParameters, ActionContext actionContext) {
        List<Object> firstRow = MicrosoftExcelRowUtils.getRowFromWorksheet(inputParameters, actionContext, 1);

        List<Option<String>> options = new ArrayList<>();

        for (Object item : firstRow) {
            String string = item.toString();
            if (!string.isEmpty()) {
                options.add(option(string, string));
            }
        }
        return options;
    }

    private static List<Option<String>> getOptions(Map<String, Object> body, String value) {
        List<Option<String>> options = new ArrayList<>();

        if (body.get(VALUE) instanceof List<?> list) {
            for (Object object : list) {
                if (object instanceof Map<?, ?> map) {
                    options.add(option((String) map.get(NAME), (String) map.get(value)));
                }
            }
        }

        return options;
    }
}

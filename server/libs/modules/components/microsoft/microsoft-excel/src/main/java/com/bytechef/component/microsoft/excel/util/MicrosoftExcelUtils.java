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

package com.bytechef.component.microsoft.excel.util;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.BASE_URL;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.ID;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.IS_THE_FIRST_ROW_HEADER;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.NAME;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.ROW;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.VALUE;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.VALUES;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKBOOK_ID;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKBOOK_WORKSHEETS_PATH;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKSHEET_NAME;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableValueProperty;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Monika Domiter
 */
public class MicrosoftExcelUtils {

    private MicrosoftExcelUtils() {
    }

    public static List<Property.ValueProperty<?>> createInputPropertyForRow(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        ActionContext context) {

        if (inputParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER)) {
            Map<String, Object> body = context
                .http(http -> http
                    .get(BASE_URL + "/" + inputParameters.getRequiredString(WORKBOOK_ID) + WORKBOOK_WORKSHEETS_PATH
                        + inputParameters.getRequiredString(WORKSHEET_NAME) + "/usedRange(valuesOnly=true)"))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

            if ((body.get(VALUES) instanceof List<?> values) && (values.getFirst() instanceof List<?> list)) {
                for (Object item : list) {
                    properties.add(
                        string(item.toString())
                            .defaultValue(""));
                }
            }

            ModifiableObjectProperty updatedRow = object(VALUES)
                .label("Values")
                .properties(properties)
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

    public static String getLastUsedColumnLabel(Parameters inputParameters, ActionContext context) {
        Map<String, Object> body = context
            .http(http -> http.get(BASE_URL + "/" + inputParameters.getRequiredString(WORKBOOK_ID)
                + WORKBOOK_WORKSHEETS_PATH + inputParameters.getRequiredString(WORKSHEET_NAME) + "/usedRange"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("columnCount") instanceof Integer integer) {
            return columnToLabel(integer, false);
        }

        throw new IllegalStateException("Failed to get last used column");
    }

    public static Integer getLastUsedRowIndex(Parameters inputParameters, ActionContext context) {
        Map<String, Object> body = context
            .http(http -> http.get(BASE_URL + "/" + inputParameters.getRequiredString(WORKBOOK_ID)
                + WORKBOOK_WORKSHEETS_PATH + inputParameters.getRequiredString(WORKSHEET_NAME) + "/usedRange"))
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
        Parameters inputParameters, ActionContext context, List<Object> row) {

        Map<String, Object> valuesMap;

        if (inputParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER)) {
            List<Object> firstRow = MicrosoftExcelRowUtils.getRowFromWorksheet(inputParameters, context, 1);

            valuesMap = IntStream.range(0, row.size())
                .boxed()
                .collect(
                    Collectors.toMap(i -> String.valueOf(firstRow.get(i)),
                        i -> String.valueOf(row.get(i)), (a, b) -> b, LinkedHashMap::new));
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

    public static List<Object> getRowInputValues(Parameters inputParameters) {
        List<Object> row = new ArrayList<>();

        Map<String, Object> rowMap = inputParameters.getRequiredMap(ROW, Object.class);

        if (rowMap.get(VALUES) instanceof Map<?, ?> map) {
            row.addAll(map.values());
        } else if (rowMap.get(VALUES) instanceof List<?> list) {
            row.addAll(list);
        }

        return row;
    }

    public static List<Option<String>> getWorkbookIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, Object> body = context
            .http(http -> http.get(BASE_URL + "/root/search(q='.xlsx')"))
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
                .get(BASE_URL + "/" + inputParameters.getRequiredString(WORKBOOK_ID) + WORKBOOK_WORKSHEETS_PATH))
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

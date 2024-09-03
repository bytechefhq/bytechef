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

package com.bytechef.component.google.sheets.util;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.INCLUDE_ITEMS_FROM_ALL_DRIVES;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.ROW;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.VALUES;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableValueProperty;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.PropertiesDataSource.ActionPropertiesFunction;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Monika Kušter
 */
public class GoogleSheetsUtils {

    private GoogleSheetsUtils() {
    }

    public static void appendValues(
        Sheets sheets, String spreadsheetId, String range, ValueRange valueRange, String valueInputOption)
        throws IOException {

        sheets.spreadsheets()
            .values()
            .append(spreadsheetId, range, valueRange)
            .setValueInputOption(valueInputOption)
            .execute();
    }

    public static String createRange(String sheetName, Integer rowNumber) {
        if (rowNumber == null) {
            return sheetName;
        }

        return sheetName + "!" + rowNumber + ":" + rowNumber;
    }

    public static ActionPropertiesFunction createPropertiesForNewRows(boolean insertOneRow) {
        return (inputParameters, connectionParameters, dependencyPaths, context) -> {

            boolean isFirstRowHeader = inputParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER);

            if (isFirstRowHeader) {
                List<Object> firstRow = GoogleSheetsRowUtils.getRowValues(
                    GoogleServices.getSheets(connectionParameters), inputParameters.getRequiredString(SPREADSHEET_ID),
                    inputParameters.getRequiredString(SHEET_NAME), 1);

                List<ModifiableValueProperty<?, ?>> list = new ArrayList<>();

                for (Object value : firstRow) {
                    String label = value.toString();
                    list.add(
                        string(label.replaceAll(" ", "_"))
                            .label(label)
                            .defaultValue(""));
                }

                ModifiableObjectProperty updatedRow = object(VALUES)
                    .label("Values")
                    .properties(list)
                    .required(true);

                if (insertOneRow) {
                    return List.of(updatedRow);
                } else {
                    ModifiableArrayProperty rows = array(VALUES)
                        .label("Rows")
                        .items(updatedRow)
                        .required(true);
                    return List.of(rows);
                }
            } else {
                ModifiableArrayProperty updatedRow = array(VALUES)
                    .label("Values")
                    .items(bool(), number(), string())
                    .required(true);

                if (insertOneRow) {
                    return List.of(updatedRow);
                } else {
                    ModifiableArrayProperty rows = array(VALUES)
                        .label("Rows")
                        .items(updatedRow)
                        .required(true);

                    return List.of(rows);
                }
            }
        };
    }

    public static Map<String, Object> getMapOfValuesForRow(Parameters inputParameters, Sheets sheets, List<Object> row)
        throws IOException {

        Map<String, Object> valuesMap;

        if (inputParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER)) {
            List<Object> firstRow = GoogleSheetsRowUtils.getRowValues(
                sheets, inputParameters.getRequiredString(SPREADSHEET_ID),
                inputParameters.getRequiredString(SHEET_NAME), 1);

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
                        i -> "column_" + columnToLabel(i + 1), i -> String.valueOf(row.get(i)), (a, b) -> b,
                        LinkedHashMap::new));
        }

        return valuesMap;
    }

    public static List<Map<String, Object>> getMapOfValuesForRowAndColumn(
        Parameters inputParameters, Sheets sheets, List<List<Object>> values, int currentRowNum, int newRowNum)
        throws IOException {

        List<Map<String, Object>> list = new ArrayList<>();

        for (int i = currentRowNum; i < newRowNum; i++) {
            list.add(getMapOfValuesForRow(inputParameters, sheets, values.get(i)));
        }

        return list;
    }

    public static List<Object> getRowValues(Parameters inputParameters) {
        List<Object> row = new ArrayList<>();

        Map<String, Object> rowMap = inputParameters.getRequiredMap(ROW, Object.class);

        Object values = rowMap.get(VALUES);

        if (values instanceof Map<?, ?> map) {
            row = new ArrayList<>(map.values());
        } else if (values instanceof List<?> list) {
            row.addAll(list);
        }

        return row;
    }

    public static List<Option<String>> getSheetIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) throws Exception {

        List<Option<String>> options = new ArrayList<>();

        List<Sheet> sheetsList = GoogleServices.getSheets(connectionParameters)
            .spreadsheets()
            .get(inputParameters.getRequiredString(SPREADSHEET_ID))
            .execute()
            .getSheets();

        for (Sheet sheet : sheetsList) {
            SheetProperties sheetProperties = sheet.getProperties();

            options.add(option(sheetProperties.getTitle(), String.valueOf(sheetProperties.getSheetId())));
        }

        return options;
    }

    public static List<Option<String>> getSheetNameOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) throws Exception {

        List<Option<String>> options = new ArrayList<>();

        List<Sheet> sheetsList = GoogleServices.getSheets(connectionParameters)
            .spreadsheets()
            .get(inputParameters.getRequiredString(SPREADSHEET_ID))
            .execute()
            .getSheets();

        for (Sheet sheet : sheetsList) {
            SheetProperties sheetProperties = sheet.getProperties();

            String sheetTitle = sheetProperties.getTitle();

            options.add(option(sheetTitle, sheetTitle));
        }

        return options;
    }

    public static List<Option<String>> getSpreadsheetIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) throws IOException {

        List<File> files = GoogleServices.getDrive(connectionParameters)
            .files()
            .list()
            .setQ("mimeType='application/vnd.google-apps.spreadsheet'")
            .setIncludeItemsFromAllDrives(inputParameters.getBoolean(INCLUDE_ITEMS_FROM_ALL_DRIVES))
            .setSupportsAllDrives(true)
            .execute()
            .getFiles();

        List<Option<String>> options = new ArrayList<>();

        for (File file : files) {
            options.add(option(file.getName(), file.getId()));
        }

        return options;
    }

    public static List<List<Object>> getSpreadsheetValues(Sheets sheets, String spreadSheetId, String sheetName)
        throws IOException {

        return sheets.spreadsheets()
            .values()
            .get(spreadSheetId, sheetName)
            .setValueRenderOption("UNFORMATTED_VALUE")
            .setDateTimeRenderOption("FORMATTED_STRING")
            .setMajorDimension("ROWS")
            .execute()
            .getValues();
    }

    /**
     * Returns column name in <code>A,B,C,..,AA,AB</code> naming convention.
     *
     * @param columnNumber column order number in column sequence
     * @return column name in <code>column_A</code> format
     */
    public static String columnToLabel(int columnNumber) {
        StringBuilder columnName = new StringBuilder();

        while (columnNumber > 0) {
            int modulo = (columnNumber - 1) % 26;
            columnName.insert(0, (char) (65 + modulo));
            columnNumber = (columnNumber - modulo) / 26;
        }

        return columnName.toString();
    }
}

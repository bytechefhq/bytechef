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

package com.bytechef.component.google.sheets.util;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.COLUMN;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.ROW;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.ROW_NUMBER;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.UPDATE_WHOLE_ROW;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.VALUE;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.VALUES;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.PropertiesDataSource.ActionPropertiesFunction;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.ValueRange;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Monika Kušter
 */
public class GoogleSheetsUtils {

    private GoogleSheetsUtils() {
    }

    public static void appendValues(
        Sheets sheets, String spreadsheetId, String range, ValueRange valueRange, String valueInputOption) {

        try {
            sheets.spreadsheets()
                .values()
                .append(spreadsheetId, range, valueRange)
                .setValueInputOption(valueInputOption)
                .execute();
        } catch (IOException e) {
            throw GoogleUtils.translateGoogleIOException(e);
        }
    }

    public static String createRange(String sheetName, Integer rowNumber) {
        if (rowNumber == null) {
            return sheetName;
        }

        return sheetName + "!" + rowNumber + ":" + rowNumber;
    }

    public static List<ValueProperty<?>> createPropertiesToUpdateRow(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        ActionContext actionContext) {

        boolean isFirstRowHeader = inputParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER);
        boolean updateWholeRow = inputParameters.getRequiredBoolean(UPDATE_WHOLE_ROW);

        if (isFirstRowHeader) {
            List<ModifiableValueProperty<?, ?>> propertiesBasedOnHeader =
                createPropertiesBasedOnHeader(inputParameters, connectionParameters);

            if (updateWholeRow) {
                return List.of(
                    object(VALUES)
                        .label("Values")
                        .properties(propertiesBasedOnHeader)
                        .required(true));
            } else
                return List.of(
                    array(VALUES)
                        .label("Values")
                        .items(
                            object()
                                .properties(
                                    string(COLUMN)
                                        .label("Column")
                                        .description("Column to update.")
                                        .options(getColumnOptions(inputParameters, connectionParameters))
                                        .required(true),
                                    string(VALUE)
                                        .label("Column Value")
                                        .defaultValue("")
                                        .required(true)))
                        .required(true));
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

    private static List<ModifiableValueProperty<?, ?>> createPropertiesBasedOnHeader(
        Parameters inputParameters, Parameters connectionParameters) {

        List<Object> firstRow = GoogleSheetsRowUtils.getRowValues(
            GoogleServices.getSheets(connectionParameters), inputParameters.getRequiredString(SPREADSHEET_ID),
            inputParameters.getRequiredString(SHEET_NAME), 1);

        List<ModifiableValueProperty<?, ?>> list = new ArrayList<>();

        for (Object value : firstRow) {
            String label = value.toString();
            list.add(
                string(label)
                    .label(label)
                    .defaultValue(""));
        }
        return list;
    }

    public static ActionPropertiesFunction createPropertiesForNewRows(boolean insertOneRow) {
        return (inputParameters, connectionParameters, lookupDependsOnPaths, context) -> {

            boolean isFirstRowHeader = inputParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER);

            if (isFirstRowHeader) {
                List<ModifiableValueProperty<?, ?>> list =
                    createPropertiesBasedOnHeader(inputParameters, connectionParameters);

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

    public static void deleteDimension(
        Parameters inputParameters, Parameters connectionParameters, Integer index, String dimension) {

        Sheets sheets = GoogleServices.getSheets(connectionParameters);

        DimensionRange dimensionRange = new DimensionRange()
            .setSheetId(inputParameters.getRequiredInteger(SHEET_ID))
            .setDimension(dimension)
            .setStartIndex(index - 1)
            .setEndIndex(index);

        Request request = new Request()
            .setDeleteDimension(
                new DeleteDimensionRequest()
                    .setRange(dimensionRange));

        BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest()
            .setRequests(List.of(request));

        try {
            sheets.spreadsheets()
                .batchUpdate(inputParameters.getRequiredString(SPREADSHEET_ID), batchUpdateSpreadsheetRequest)
                .execute();
        } catch (IOException e) {
            throw GoogleUtils.translateGoogleIOException(e);
        }
    }

    public static Map<String, Object>
        getMapOfValuesForRow(Parameters inputParameters, Sheets sheets, List<Object> row) {
        Map<String, Object> valuesMap;

        if (inputParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER)) {
            List<Object> firstRow = GoogleSheetsRowUtils.getRowValues(
                sheets, inputParameters.getRequiredString(SPREADSHEET_ID),
                inputParameters.getRequiredString(SHEET_NAME), 1);

            valuesMap = IntStream.range(0, row.size())
                .boxed()
                .collect(
                    Collectors.toMap(i -> String.valueOf(firstRow.get(i)),
                        i -> {
                            Object value = row.get(i);

                            return value == null ? "" : value;
                        }, (a, b) -> b, LinkedHashMap::new));
        } else {
            valuesMap = IntStream.range(0, row.size())
                .boxed()
                .collect(
                    Collectors.toMap(
                        i -> "column_" + GoogleSheetsColumnConverterUtils.columnToLabel(i + 1),
                        i -> String.valueOf(row.get(i)),
                        (a, b) -> b,
                        LinkedHashMap::new));
        }

        return valuesMap;
    }

    public static List<Map<String, Object>> getMapOfValuesForRowAndColumn(
        Parameters inputParameters, Sheets sheets, List<List<Object>> values, int currentRowNum, int newRowNum) {

        List<Map<String, Object>> list = new ArrayList<>();

        for (int i = currentRowNum; i < newRowNum; i++) {
            list.add(getMapOfValuesForRow(inputParameters, sheets, values.get(i)));
        }

        return list;
    }

    public static List<Object> getRowValues(Parameters inputParameters) {
        List<Object> row = new ArrayList<>();

        if (inputParameters.get(ROW) instanceof Map<?, ?> rowMap) {
            Object values = rowMap.get(VALUES);

            if (values instanceof Map<?, ?> map) {
                row = map.values()
                    .stream()
                    .map(value -> Objects.requireNonNullElse(value, ""))
                    .toList();
            } else if (values instanceof List<?> list) {
                row = list.stream()
                    .map(item -> Objects.requireNonNullElse(item, ""))
                    .toList();
            }
        }

        return row;
    }

    public static List<Option<String>> getSheetIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, ActionContext context) {

        List<Option<String>> options = new ArrayList<>();

        List<Sheet> sheets = getSheets(inputParameters, connectionParameters);

        for (Sheet sheet : sheets) {
            SheetProperties sheetProperties = sheet.getProperties();

            options.add(option(sheetProperties.getTitle(), String.valueOf(sheetProperties.getSheetId())));
        }

        return options;
    }

    public static List<Option<String>> getSheetNameOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        List<Sheet> sheets = getSheets(inputParameters, connectionParameters);

        for (Sheet sheet : sheets) {
            SheetProperties sheetProperties = sheet.getProperties();

            String sheetTitle = sheetProperties.getTitle();

            options.add(option(sheetTitle, sheetTitle));
        }

        return options;
    }

    private static List<Sheet> getSheets(Parameters inputParameters, Parameters connectionParameters) {
        List<Sheet> sheets;

        try {
            sheets = GoogleServices.getSheets(connectionParameters)
                .spreadsheets()
                .get(inputParameters.getRequiredString(SPREADSHEET_ID))
                .execute()
                .getSheets();
        } catch (IOException e) {
            throw GoogleUtils.translateGoogleIOException(e);
        }

        return sheets;
    }

    public static List<List<Object>> getSpreadsheetValues(Sheets sheets, String spreadSheetId, String sheetName) {
        try {
            return sheets.spreadsheets()
                .values()
                .get(spreadSheetId, sheetName)
                .setValueRenderOption("UNFORMATTED_VALUE")
                .setDateTimeRenderOption("FORMATTED_STRING")
                .setMajorDimension("ROWS")
                .execute()
                .getValues();
        } catch (IOException e) {
            throw GoogleUtils.translateGoogleIOException(e);
        }
    }

    public static List<Object> getUpdatedRowValues(Parameters inputParameters, Parameters connectionParameters) {
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
                    Sheets sheets = GoogleServices.getSheets(connectionParameters);
                    String spreadSheetId = inputParameters.getRequiredString(SPREADSHEET_ID);
                    String sheetName = inputParameters.getRequiredString(SHEET_NAME);

                    List<Object> firstRow = GoogleSheetsRowUtils.getRowValues(sheets, spreadSheetId, sheetName, 1);
                    List<Object> rowToUpdate = GoogleSheetsRowUtils.getRowValues(
                        sheets, spreadSheetId, sheetName, inputParameters.getRequiredInteger(ROW_NUMBER));

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
                        List<Object> rowToUpdate = GoogleSheetsRowUtils.getRowValues(
                            GoogleServices.getSheets(connectionParameters),
                            inputParameters.getRequiredString(SPREADSHEET_ID),
                            inputParameters.getRequiredString(SHEET_NAME),
                            inputParameters.getRequiredInteger(ROW_NUMBER));

                        for (Object o : list) {
                            if (o instanceof Map<?, ?> map) {
                                int indexOfColumnToUpdate =
                                    GoogleSheetsColumnConverterUtils.labelToColumn((String) map.get(COLUMN)) - 1;

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

    private static List<Option<String>> getColumnOptions(Parameters inputParameters, Parameters connectionParameters) {
        List<Option<String>> options = new ArrayList<>();

        List<Object> firstRow = GoogleSheetsRowUtils.getRowValues(
            GoogleServices.getSheets(connectionParameters), inputParameters.getRequiredString(SPREADSHEET_ID),
            inputParameters.getRequiredString(SHEET_NAME), 1);

        for (Object value : firstRow) {
            String label = value.toString();
            options.add(option(label, label));
        }

        return options;
    }

    @SuppressFBWarnings("EI")
    public record SheetRecord(String spreadsheetId, Integer sheetId, String sheetName, List<Object> headers) {
    }
}

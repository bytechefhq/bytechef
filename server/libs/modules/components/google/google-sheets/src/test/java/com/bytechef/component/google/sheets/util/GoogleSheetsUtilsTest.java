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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.PropertiesDataSource.ActionPropertiesFunction;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleSheetsUtilsTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Sheets.Spreadsheets.Values.Append mockedAppend = mock(Sheets.Spreadsheets.Values.Append.class);
    private final Sheets.Spreadsheets.Get mockedGet = mock(Sheets.Spreadsheets.Get.class);
    private Parameters mockedParameters;
    private final Sheets mockedSheets = mock(Sheets.class);
    private final Spreadsheet mockedSpreadsheet = mock(Spreadsheet.class);
    private final Sheets.Spreadsheets mockedSpreadsheets = mock(Sheets.Spreadsheets.class);
    private final Sheets.Spreadsheets.Values mockedValues = mock(Sheets.Spreadsheets.Values.class);
    private final Sheets.Spreadsheets.Values.Get mockedValuesGet = mock(Sheets.Spreadsheets.Values.Get.class);
    private final ValueRange mockedValueRange = mock(ValueRange.class);
    private final ArgumentCaptor<Integer> integerArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    private final ArgumentCaptor<Sheets> sheetsArgumentCaptor = ArgumentCaptor.forClass(Sheets.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<BatchUpdateSpreadsheetRequest> batchUpdateSpreadsheetRequestArgumentCaptor =
        ArgumentCaptor.forClass(BatchUpdateSpreadsheetRequest.class);
    private final BatchUpdateSpreadsheetResponse mockedBatchUpdateSpreadsheetResponse =
        mock(BatchUpdateSpreadsheetResponse.class);
    private final Sheets.Spreadsheets.BatchUpdate mockedBatchUpdate = mock(Sheets.Spreadsheets.BatchUpdate.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final ArgumentCaptor<ValueRange> valueRangeArgumentCaptor = ArgumentCaptor.forClass(ValueRange.class);

    @Test
    void appendValues() throws IOException {
        when(mockedSheets.spreadsheets())
            .thenReturn(mockedSpreadsheets);
        when(mockedSpreadsheets.values())
            .thenReturn(mockedValues);
        when(mockedValues.append(
            stringArgumentCaptor.capture(), stringArgumentCaptor.capture(), valueRangeArgumentCaptor.capture()))
                .thenReturn(mockedAppend);
        when(mockedAppend.setValueInputOption(stringArgumentCaptor.capture()))
            .thenReturn(mockedAppend);

        ValueRange valueRange = new ValueRange();
        GoogleSheetsUtils.appendValues(mockedSheets, "abc", "range", valueRange, "RAW");

        assertEquals(List.of("abc", "range", "RAW"), stringArgumentCaptor.getAllValues());
        assertEquals(valueRange, valueRangeArgumentCaptor.getValue());
    }

    @Test
    void createPropertiesToUpdateRowWhenFirstRowIsHeaderAndUpdatingWholeRow() throws Exception {
        mockedParameters = MockParametersFactory.create(
            Map.of(IS_THE_FIRST_ROW_HEADER, true, UPDATE_WHOLE_ROW, true, SPREADSHEET_ID, "spreadsheetId", SHEET_NAME,
                "sheetName"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleSheetsRowUtils> googleSheetsRowUtilsMockedStatic =
                mockStatic(GoogleSheetsRowUtils.class)) {

            googleServicesMockedStatic.when(() -> GoogleServices.getSheets(parametersArgumentCaptor.capture()))
                .thenReturn(mockedSheets);
            googleSheetsRowUtilsMockedStatic
                .when(() -> GoogleSheetsRowUtils.getRowValues(
                    sheetsArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                    stringArgumentCaptor.capture(), integerArgumentCaptor.capture()))
                .thenReturn(List.of("header 1", "header2", "header3"));

            List<ValueProperty<?>> propertiesToUpdateRow = GoogleSheetsUtils
                .createPropertiesToUpdateRow(mockedParameters, mockedParameters, Map.of(), mockedActionContext);

            List<ModifiableObjectProperty> expectedProperties = List.of(
                object(VALUES)
                    .label("Values")
                    .properties(
                        string("header 1")
                            .label("header 1")
                            .defaultValue(""),
                        string("header2")
                            .label("header2")
                            .defaultValue(""),
                        string("header3")
                            .label("header3")
                            .defaultValue(""))
                    .required(true));

            assertEquals(expectedProperties, propertiesToUpdateRow);

            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(mockedSheets, sheetsArgumentCaptor.getValue());
            assertEquals(List.of("spreadsheetId", "sheetName"), stringArgumentCaptor.getAllValues());
            assertEquals(1, integerArgumentCaptor.getValue());
        }
    }

    @Test
    void createPropertiesToUpdateRowWhenFirstRowIsHeaderAndUpdatingSelectedColumns() throws Exception {
        mockedParameters = MockParametersFactory.create(Map.of(
            IS_THE_FIRST_ROW_HEADER, true, UPDATE_WHOLE_ROW, false, SPREADSHEET_ID, "spreadsheetId", SHEET_NAME,
            "sheetName"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleSheetsRowUtils> googleSheetsRowUtilsMockedStatic =
                mockStatic(GoogleSheetsRowUtils.class)) {

            googleServicesMockedStatic.when(() -> GoogleServices.getSheets(parametersArgumentCaptor.capture()))
                .thenReturn(mockedSheets);
            googleSheetsRowUtilsMockedStatic
                .when(() -> GoogleSheetsRowUtils.getRowValues(
                    sheetsArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                    stringArgumentCaptor.capture(), integerArgumentCaptor.capture()))
                .thenReturn(List.of("header 1", "header2", "header3"));

            List<ValueProperty<?>> propertiesToUpdateRow = GoogleSheetsUtils
                .createPropertiesToUpdateRow(mockedParameters, mockedParameters, Map.of(), mockedActionContext);

            List<ModifiableArrayProperty> expectedProperties = List.of(
                array(VALUES)
                    .label("Values")
                    .items(
                        object()
                            .properties(
                                string(COLUMN)
                                    .label("Column")
                                    .description("Column to update.")
                                    .options(
                                        option("header 1", "header 1"),
                                        option("header2", "header2"),
                                        option("header3", "header3"))
                                    .required(true),
                                string(VALUE)
                                    .label("Column Value")
                                    .defaultValue("")
                                    .required(true)))
                    .required(true));

            assertEquals(expectedProperties, propertiesToUpdateRow);

            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(mockedSheets, sheetsArgumentCaptor.getValue());
            assertEquals(List.of("spreadsheetId", "sheetName", "spreadsheetId", "sheetName"),
                stringArgumentCaptor.getAllValues());
            assertEquals(1, integerArgumentCaptor.getValue());
        }
    }

    @Test
    void createPropertiesToUpdateRowWhenFirstRowIsNotHeaderAndUpdatingWholeRow() throws Exception {
        mockedParameters = MockParametersFactory.create(Map.of(IS_THE_FIRST_ROW_HEADER, false, UPDATE_WHOLE_ROW, true));

        List<ValueProperty<?>> propertiesToUpdateRow = GoogleSheetsUtils
            .createPropertiesToUpdateRow(mockedParameters, mockedParameters, Map.of(), mockedActionContext);

        List<ModifiableArrayProperty> expectedProperties = List.of(
            array(VALUES)
                .label("Values")
                .items(bool(), number(), string())
                .required(true));

        assertEquals(expectedProperties, propertiesToUpdateRow);
    }

    @Test
    void createPropertiesToUpdateRowWhenFirstRowIsNotHeaderAndUpdatingSelectedColumns() throws Exception {
        mockedParameters =
            MockParametersFactory.create(Map.of(IS_THE_FIRST_ROW_HEADER, false, UPDATE_WHOLE_ROW, false));

        List<ValueProperty<?>> propertiesToUpdateRow = GoogleSheetsUtils
            .createPropertiesToUpdateRow(mockedParameters, mockedParameters, Map.of(), mockedActionContext);

        List<ModifiableArrayProperty> expectedProperties = List.of(
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

        assertEquals(expectedProperties, propertiesToUpdateRow);
    }

    @Test
    void testCreateRangeForRowNumberNotNull() {
        String actualRange = GoogleSheetsUtils.createRange("sheetName", 5);

        assertEquals("sheetName!5:5", actualRange);
    }

    @Test
    void testCreateRangeForRowNumberNull() {
        String actualRange = GoogleSheetsUtils.createRange("sheetName", null);

        assertEquals("sheetName", actualRange);
    }

    @Test
    void testCreatePropertiesForNewRowsForOneRowAndWhenFirstRowIsHeader() throws Exception {
        mockedParameters = MockParametersFactory.create(
            Map.of(IS_THE_FIRST_ROW_HEADER, true, SPREADSHEET_ID, "spreadsheetId", SHEET_NAME, "sheetName"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleSheetsRowUtils> googleSheetsRowUtilsMockedStatic =
                mockStatic(GoogleSheetsRowUtils.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parametersArgumentCaptor.capture()))
                .thenReturn(mockedSheets);
            googleSheetsRowUtilsMockedStatic
                .when(() -> GoogleSheetsRowUtils.getRowValues(
                    sheetsArgumentCaptor.capture(), stringArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                    integerArgumentCaptor.capture()))
                .thenReturn(List.of("header1", "header2", "header3"));

            ActionPropertiesFunction arrayPropertyForRow = GoogleSheetsUtils.createPropertiesForNewRows(true);

            List<? extends ValueProperty<?>> result = arrayPropertyForRow.apply(
                mockedParameters, mockedParameters, Map.of(), mockedActionContext);

            ModifiableObjectProperty expectedProperty = object(VALUES)
                .label("Values")
                .properties(
                    string("header1")
                        .label("header1")
                        .defaultValue(""),
                    string("header2")
                        .label("header2")
                        .defaultValue(""),
                    string("header3")
                        .label("header3")
                        .defaultValue(""))
                .required(true);

            assertEquals(List.of(expectedProperty), result);

            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(mockedSheets, sheetsArgumentCaptor.getValue());
            assertEquals(List.of("spreadsheetId", "sheetName"), stringArgumentCaptor.getAllValues());
            assertEquals(1, integerArgumentCaptor.getValue());
        }
    }

    @Test
    void testCreatePropertiesForNewRowsForMultipleRowsAndWhenFirstRowIsHeader() throws Exception {
        mockedParameters = MockParametersFactory.create(
            Map.of(IS_THE_FIRST_ROW_HEADER, true, SPREADSHEET_ID, "spreadsheetId", SHEET_NAME, "sheetName"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleSheetsRowUtils> googleSheetsRowUtilsMockedStatic = mockStatic(
                GoogleSheetsRowUtils.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parametersArgumentCaptor.capture()))
                .thenReturn(mockedSheets);
            googleSheetsRowUtilsMockedStatic
                .when(() -> GoogleSheetsRowUtils.getRowValues(
                    sheetsArgumentCaptor.capture(), stringArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                    integerArgumentCaptor.capture()))
                .thenReturn(List.of("header1", "header2", "header3"));

            ActionPropertiesFunction propertiesForNewRows = GoogleSheetsUtils.createPropertiesForNewRows(false);

            List<? extends ValueProperty<?>> result = propertiesForNewRows.apply(
                mockedParameters, mockedParameters, Map.of(), mockedActionContext);

            ModifiableArrayProperty expectedProperty = array(VALUES)
                .label("Rows")
                .items(object(VALUES)
                    .label("Values")
                    .properties(
                        string("header1")
                            .label("header1")
                            .defaultValue(""),
                        string("header2")
                            .label("header2")
                            .defaultValue(""),
                        string("header3")
                            .label("header3")
                            .defaultValue(""))
                    .required(true))
                .required(true);

            assertEquals(List.of(expectedProperty), result);

            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(mockedSheets, sheetsArgumentCaptor.getValue());
            assertEquals(List.of("spreadsheetId", "sheetName"), stringArgumentCaptor.getAllValues());
            assertEquals(1, integerArgumentCaptor.getValue());
        }
    }

    @Test
    void testCreatePropertiesForNewRowsForMultipleRowsAndWhenFirstRowIsNotHeader() throws Exception {
        mockedParameters = MockParametersFactory.create(
            Map.of(IS_THE_FIRST_ROW_HEADER, false, SPREADSHEET_ID, "spreadsheetId", SHEET_NAME, "sheetName"));

        ActionPropertiesFunction propertiesForNewRows = GoogleSheetsUtils.createPropertiesForNewRows(false);

        List<? extends ValueProperty<?>> result = propertiesForNewRows.apply(
            mockedParameters, mockedParameters, Map.of(), mockedActionContext);

        ModifiableArrayProperty expectedProperty = array(VALUES)
            .label("Rows")
            .items(
                array(VALUES)
                    .label("Values")
                    .items(bool(), number(), string())
                    .required(true))
            .required(true);

        assertEquals(List.of(expectedProperty), result);
    }

    @Test
    void testCreatePropertiesForNewRowsForOneRowAndWhenFirstRowIsNotHeader() throws Exception {
        mockedParameters = MockParametersFactory.create(
            Map.of(IS_THE_FIRST_ROW_HEADER, false, SPREADSHEET_ID, "spreadsheetId", SHEET_NAME, "sheetName"));

        ActionPropertiesFunction propertiesForNewRows = GoogleSheetsUtils.createPropertiesForNewRows(true);

        List<? extends ValueProperty<?>> result = propertiesForNewRows.apply(
            mockedParameters, mockedParameters, Map.of(), mockedActionContext);

        ModifiableArrayProperty expectedProperty = array(VALUES)
            .label("Values")
            .items(bool(), number(), string())
            .required(true);

        assertEquals(List.of(expectedProperty), result);
    }

    @Test
    void testDeleteDimensionWhenColumn() throws Exception {
        mockedParameters = MockParametersFactory.create(Map.of(SPREADSHEET_ID, "spreadsheetId", SHEET_ID, 123));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parametersArgumentCaptor.capture()))
                .thenReturn(mockedSheets);

            when(mockedSheets.spreadsheets())
                .thenReturn(mockedSpreadsheets);
            when(mockedSpreadsheets.batchUpdate(
                stringArgumentCaptor.capture(), batchUpdateSpreadsheetRequestArgumentCaptor.capture()))
                    .thenReturn(mockedBatchUpdate);
            when(mockedBatchUpdate.execute())
                .thenReturn(mockedBatchUpdateSpreadsheetResponse);

            GoogleSheetsUtils.deleteDimension(mockedParameters, mockedParameters, 2, "COLUMNS");

            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals("spreadsheetId", stringArgumentCaptor.getValue());

            BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest()
                .setRequests(List.of(
                    new Request()
                        .setDeleteDimension(
                            new DeleteDimensionRequest()
                                .setRange(
                                    new DimensionRange()
                                        .setSheetId(123)
                                        .setDimension("COLUMNS")
                                        .setStartIndex(1)
                                        .setEndIndex(2)))));

            assertEquals(batchUpdateSpreadsheetRequest, batchUpdateSpreadsheetRequestArgumentCaptor.getValue());
        }
    }

    @Test
    void testDeleteDimensionWhenRow() throws Exception {
        mockedParameters = MockParametersFactory.create(Map.of(SPREADSHEET_ID, "spreadsheetId", SHEET_ID, 123));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parametersArgumentCaptor.capture()))
                .thenReturn(mockedSheets);

            when(mockedSheets.spreadsheets())
                .thenReturn(mockedSpreadsheets);
            when(mockedSpreadsheets.batchUpdate(
                stringArgumentCaptor.capture(), batchUpdateSpreadsheetRequestArgumentCaptor.capture()))
                    .thenReturn(mockedBatchUpdate);
            when(mockedBatchUpdate.execute())
                .thenReturn(mockedBatchUpdateSpreadsheetResponse);

            GoogleSheetsUtils.deleteDimension(mockedParameters, mockedParameters, 2, "ROWS");

            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals("spreadsheetId", stringArgumentCaptor.getValue());

            BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest()
                .setRequests(List.of(
                    new Request()
                        .setDeleteDimension(
                            new DeleteDimensionRequest()
                                .setRange(
                                    new DimensionRange()
                                        .setSheetId(123)
                                        .setDimension("ROWS")
                                        .setStartIndex(1)
                                        .setEndIndex(2)))));

            assertEquals(batchUpdateSpreadsheetRequest, batchUpdateSpreadsheetRequestArgumentCaptor.getValue());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetMapOfValuesForRowWhereFirstRowHeaders() throws IOException {
        List<Object> mockedRow = mock(List.class);
        List<Object> mockedFirstRow = mock(List.class);

        mockedParameters = MockParametersFactory.create(
            Map.of(IS_THE_FIRST_ROW_HEADER, true, SPREADSHEET_ID, "spreadsheetId", SHEET_NAME, "sheetName"));

        try (MockedStatic<GoogleSheetsRowUtils> sheetsRowUtilsMockedStatic = mockStatic(GoogleSheetsRowUtils.class)) {
            sheetsRowUtilsMockedStatic
                .when(() -> GoogleSheetsRowUtils.getRowValues(
                    any(Sheets.class), stringArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                    integerArgumentCaptor.capture()))
                .thenReturn(mockedFirstRow);

            when(mockedFirstRow.get(integerArgumentCaptor.capture()))
                .thenReturn("header1", "header2", "header3");
            when(mockedRow.size())
                .thenReturn(3);
            when(mockedRow.get(integerArgumentCaptor.capture()))
                .thenReturn("value1", "value2", "value3");

            Map<String, Object> result = GoogleSheetsUtils.getMapOfValuesForRow(
                mockedParameters, mockedSheets, mockedRow);

            Map<String, Object> expected = new LinkedHashMap<>();

            expected.put("header1", "value1");
            expected.put("header2", "value2");
            expected.put("header3", "value3");

            assertEquals(expected, result);
            assertEquals(List.of("spreadsheetId", "sheetName"), stringArgumentCaptor.getAllValues());
            assertEquals(2, integerArgumentCaptor.getValue());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetMapOfValuesForRowWhereFirstRowNotHeaders() throws IOException {
        mockedParameters = MockParametersFactory.create(Map.of(IS_THE_FIRST_ROW_HEADER, false));

        List<Object> mockedRow = mock(List.class);

        when(mockedRow.size())
            .thenReturn(3);
        when(mockedRow.get(integerArgumentCaptor.capture()))
            .thenReturn("value1", "value2", "value3");

        Map<String, Object> result = GoogleSheetsUtils.getMapOfValuesForRow(mockedParameters, mockedSheets, mockedRow);

        Map<String, Object> expected = new LinkedHashMap<>();

        expected.put("column_A", "value1");
        expected.put("column_B", "value2");
        expected.put("column_C", "value3");

        assertEquals(expected, result);
        assertEquals(2, integerArgumentCaptor.getValue());
    }

    @Test
    void testGetRowValuesWhereFirstSpreadsheetRowValuesHeaders() {
        Map<String, Object> rowMap = Map.of(VALUES, Map.of("name", "name", "email", "email"));

        mockedParameters = MockParametersFactory.create(Map.of(ROW, rowMap));

        List<Object> rowValues = GoogleSheetsUtils.getRowValues(mockedParameters);

        Assertions.assertThat(List.<Object>of("name", "email"))
            .hasSameElementsAs(rowValues);
    }

    @Test
    void testGetRowValuesWhereFirstSpreadsheetRowValuesNotHeaders() {
        List<Object> rowList = List.of("name", 1233, false);

        Map<String, Object> rowMap = Map.of(VALUES, rowList);

        mockedParameters = MockParametersFactory.create(Map.of(ROW, rowMap));

        List<Object> rowValues = GoogleSheetsUtils.getRowValues(mockedParameters);

        assertEquals(rowList, rowValues);
    }

    @Test
    void testGetSheetIdOptions() throws Exception {
        List<Sheet> sheetsList = getSheets();

        mockedParameters = MockParametersFactory.create(Map.of(SPREADSHEET_ID, "spreadsheetId"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parametersArgumentCaptor.capture()))
                .thenReturn(mockedSheets);

            when(mockedSheets.spreadsheets())
                .thenReturn(mockedSpreadsheets);
            when(mockedSpreadsheets.get(stringArgumentCaptor.capture()))
                .thenReturn(mockedGet);
            when(mockedGet.execute())
                .thenReturn(mockedSpreadsheet);
            when(mockedSpreadsheet.getSheets())
                .thenReturn(sheetsList);

            List<Option<String>> sheetIdOptions = GoogleSheetsUtils.getSheetIdOptions(
                mockedParameters, mockedParameters, Map.of(), anyString(), mockedActionContext);

            List<Option<String>> expectedOptions = List.of(
                option("Sheet 1", "1234567890"), option("Sheet 2", "98765432"));

            assertEquals(expectedOptions, sheetIdOptions);
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals("spreadsheetId", stringArgumentCaptor.getValue());
        }
    }

    @Test
    void testGetSheetNameOptions() throws Exception {
        List<Sheet> sheetsList = getSheets();

        mockedParameters = MockParametersFactory.create(Map.of(SPREADSHEET_ID, "spreadsheetId"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parametersArgumentCaptor.capture()))
                .thenReturn(mockedSheets);

            when(mockedSheets.spreadsheets())
                .thenReturn(mockedSpreadsheets);
            when(mockedSpreadsheets.get(stringArgumentCaptor.capture()))
                .thenReturn(mockedGet);
            when(mockedGet.execute())
                .thenReturn(mockedSpreadsheet);
            when(mockedSpreadsheet.getSheets())
                .thenReturn(sheetsList);

            List<Option<String>> sheetNameOptions = GoogleSheetsUtils.getSheetNameOptions(
                mockedParameters, mockedParameters, Map.of(), anyString(), mockedActionContext);

            List<Option<String>> expectedOptions = List.of(
                option("Sheet 1", "Sheet 1"), option("Sheet 2", "Sheet 2"));

            assertEquals(expectedOptions, sheetNameOptions);
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals("spreadsheetId", stringArgumentCaptor.getValue());
        }
    }

    @Test
    void testGetSpreadsheetValues() throws IOException {
        when(mockedSheets.spreadsheets())
            .thenReturn(mockedSpreadsheets);
        when(mockedSpreadsheets.values())
            .thenReturn(mockedValues);
        when(mockedValues.get(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedValuesGet);
        when(mockedValuesGet.setValueRenderOption(stringArgumentCaptor.capture()))
            .thenReturn(mockedValuesGet);
        when(mockedValuesGet.setDateTimeRenderOption(stringArgumentCaptor.capture()))
            .thenReturn(mockedValuesGet);
        when(mockedValuesGet.setMajorDimension(stringArgumentCaptor.capture()))
            .thenReturn(mockedValuesGet);
        when(mockedValuesGet.execute())
            .thenReturn(mockedValueRange);
        when(mockedValueRange.getValues())
            .thenReturn(List.of(List.of()));

        List<List<Object>> result = GoogleSheetsUtils.getSpreadsheetValues(mockedSheets, "spreadsheetId", "sheetName");

        assertEquals(List.of(List.of()), result);

        assertEquals(
            List.of("spreadsheetId", "sheetName", "UNFORMATTED_VALUE", "FORMATTED_STRING", "ROWS"),
            stringArgumentCaptor.getAllValues());
    }

    @Test
    void testGetUpdatedRowValuesWhenFirstRowIsHeaderAndUpdatingWholeRow() throws Exception {
        mockedParameters = MockParametersFactory.create(Map.of(ROW, Map.of(VALUES, Map.of("name", "abc"))));

        List<Object> rowValues = GoogleSheetsUtils.getUpdatedRowValues(mockedParameters, mockedParameters);

        assertEquals(List.of("abc"), rowValues);
    }

    @Test
    void testGetUpdatedRowValuesWhenFirstRowIsHeaderAndUpdatingSelectedColumns() throws Exception {
        mockedParameters = MockParametersFactory.create(Map.of(
            SPREADSHEET_ID, "spreadsheetId", SHEET_NAME, "sheetName", ROW_NUMBER, 5,
            IS_THE_FIRST_ROW_HEADER, true,
            ROW, Map.of(VALUES, List.of(
                Map.of(COLUMN, "header1", VALUE, "abc"),
                Map.of(COLUMN, "header3", VALUE, false)))));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleSheetsRowUtils> googleSheetsRowUtilsMockedStatic =
                mockStatic(GoogleSheetsRowUtils.class)) {

            googleServicesMockedStatic.when(() -> GoogleServices.getSheets(parametersArgumentCaptor.capture()))
                .thenReturn(mockedSheets);

            List<Object> rowToUpdate = new ArrayList<>(List.of("cde", 345, true));
            googleSheetsRowUtilsMockedStatic.when(() -> GoogleSheetsRowUtils.getRowValues(
                sheetsArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                stringArgumentCaptor.capture(),
                integerArgumentCaptor.capture()))
                .thenReturn(List.of("header1", "header2", "header3"), rowToUpdate);

            List<Object> result = GoogleSheetsUtils.getUpdatedRowValues(mockedParameters, mockedParameters);

            assertEquals(List.of("abc", 345, false), result);

            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(List.of(mockedSheets, mockedSheets), sheetsArgumentCaptor.getAllValues());
            assertEquals(
                List.of("spreadsheetId", "sheetName", "spreadsheetId", "sheetName"),
                stringArgumentCaptor.getAllValues());
            assertEquals(List.of(1, 5), integerArgumentCaptor.getAllValues());
        }
    }

    @Test
    void testGetUpdatedRowValuesWhenFirstRowIsNotHeaderAndUpdatingWholeRow() throws Exception {
        mockedParameters = MockParametersFactory.create(Map.of(
            IS_THE_FIRST_ROW_HEADER, false, UPDATE_WHOLE_ROW, true,
            ROW, Map.of(VALUES, List.of("abc", 345, false))));

        List<Object> result = GoogleSheetsUtils.getUpdatedRowValues(mockedParameters, mockedParameters);

        assertEquals(List.of("abc", 345, false), result);
    }

    @Test
    void testGetUpdatedRowValuesWhenFirstRowIsNotHeaderAndUpdatingSelectedColumns() throws Exception {
        mockedParameters = MockParametersFactory.create(Map.of(
            SPREADSHEET_ID, "spreadsheetId", SHEET_NAME, "sheetName", ROW_NUMBER, 5,
            IS_THE_FIRST_ROW_HEADER, false, UPDATE_WHOLE_ROW, false,
            ROW, Map.of(VALUES, List.of(
                Map.of(COLUMN, "A", VALUE, "abc"),
                Map.of(COLUMN, "C", VALUE, false)))));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleSheetsRowUtils> googleSheetsRowUtilsMockedStatic =
                mockStatic(GoogleSheetsRowUtils.class)) {

            googleServicesMockedStatic.when(() -> GoogleServices.getSheets(parametersArgumentCaptor.capture()))
                .thenReturn(mockedSheets);

            List<Object> rowToUpdate = new ArrayList<>(List.of("cde", 345, true));
            googleSheetsRowUtilsMockedStatic.when(() -> GoogleSheetsRowUtils.getRowValues(
                sheetsArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                stringArgumentCaptor.capture(), integerArgumentCaptor.capture()))
                .thenReturn(rowToUpdate);

            List<Object> result = GoogleSheetsUtils.getUpdatedRowValues(mockedParameters, mockedParameters);

            assertEquals(List.of("abc", 345, false), result);

            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(mockedSheets, sheetsArgumentCaptor.getValue());
            assertEquals(List.of("spreadsheetId", "sheetName"), stringArgumentCaptor.getAllValues());
            assertEquals(5, integerArgumentCaptor.getValue());
        }
    }

    private static List<Sheet> getSheets() {
        Sheet sheet1 = createSheet("Sheet 1", 1234567890);
        Sheet sheet2 = createSheet("Sheet 2", 98765432);

        return Arrays.asList(sheet1, sheet2);
    }

    private static Sheet createSheet(String title, Integer sheetId) {
        Sheet sheet = new Sheet();

        SheetProperties sheetProperties1 = new SheetProperties();

        sheetProperties1.setTitle(title);
        sheetProperties1.setSheetId(sheetId);

        sheet.setProperties(sheetProperties1);

        return sheet;
    }
}

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    private final ArgumentCaptor<Integer> rowNumberArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    private final ArgumentCaptor<Sheets> sheetsArgumentCaptor = ArgumentCaptor.forClass(Sheets.class);
    private final ArgumentCaptor<String> sheetNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> spreadsheetIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> valueRenderArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> dateTimeRenderArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> majorDimensionArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<BatchUpdateSpreadsheetRequest> batchUpdateSpreadsheetRequestArgumentCaptor =
        ArgumentCaptor.forClass(BatchUpdateSpreadsheetRequest.class);
    private final BatchUpdateSpreadsheetResponse mockedBatchUpdateSpreadsheetResponse =
        mock(BatchUpdateSpreadsheetResponse.class);
    private final Sheets.Spreadsheets.BatchUpdate mockedBatchUpdate = mock(Sheets.Spreadsheets.BatchUpdate.class);

    @Test
    void appendValues() throws IOException {
        when(mockedSheets.spreadsheets())
            .thenReturn(mockedSpreadsheets);
        when(mockedSpreadsheets.values())
            .thenReturn(mockedValues);
        when(mockedValues.append(anyString(), anyString(), any(ValueRange.class)))
            .thenReturn(mockedAppend);
        when(mockedAppend.setValueInputOption(anyString()))
            .thenReturn(mockedAppend);

        ValueRange valueRange = new ValueRange();
        GoogleSheetsUtils.appendValues(mockedSheets, "abc", "range", valueRange, "RAW");

        verify(mockedSheets, times(1)).spreadsheets();
        verify(mockedSpreadsheets, times(1)).values();
        verify(mockedValues, times(1)).append("abc", "range", valueRange);
        verify(mockedAppend, times(1)).execute();
    }

    @Test
    void createPropertiesToUpdateRowWhenFirstRowIsHeaderAndUpdatingWholeRow() throws Exception {
        mockedParameters = MockParametersFactory.create(
            Map.of(IS_THE_FIRST_ROW_HEADER, true, UPDATE_WHOLE_ROW, true, SPREADSHEET_ID, "spreadsheetId", SHEET_NAME,
                "sheetName"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleSheetsRowUtils> googleSheetsRowUtilsMockedStatic =
                mockStatic(GoogleSheetsRowUtils.class)) {

            googleServicesMockedStatic.when(() -> GoogleServices.getSheets(mockedParameters))
                .thenReturn(mockedSheets);
            googleSheetsRowUtilsMockedStatic
                .when(() -> GoogleSheetsRowUtils.getRowValues(
                    sheetsArgumentCaptor.capture(), spreadsheetIdArgumentCaptor.capture(),
                    sheetNameArgumentCaptor.capture(), rowNumberArgumentCaptor.capture()))
                .thenReturn(List.of("header 1", "header2", "header3"));

            List<ValueProperty<?>> propertiesToUpdateRow = GoogleSheetsUtils
                .createPropertiesToUpdateRow(mockedParameters, mockedParameters, Map.of(), mockedActionContext);

            List<ModifiableObjectProperty> expectedProperties = List.of(
                object(VALUES)
                    .label("Values")
                    .properties(
                        string("header_1")
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

            assertEquals(mockedSheets, sheetsArgumentCaptor.getValue());
            assertEquals("spreadsheetId", spreadsheetIdArgumentCaptor.getValue());
            assertEquals("sheetName", sheetNameArgumentCaptor.getValue());
            assertEquals(1, rowNumberArgumentCaptor.getValue());
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

            googleServicesMockedStatic.when(() -> GoogleServices.getSheets(mockedParameters))
                .thenReturn(mockedSheets);
            googleSheetsRowUtilsMockedStatic
                .when(() -> GoogleSheetsRowUtils.getRowValues(sheetsArgumentCaptor.capture(),
                    spreadsheetIdArgumentCaptor.capture(),
                    sheetNameArgumentCaptor.capture(), rowNumberArgumentCaptor.capture()))
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

            assertEquals(mockedSheets, sheetsArgumentCaptor.getValue());
            assertEquals("spreadsheetId", spreadsheetIdArgumentCaptor.getValue());
            assertEquals("sheetName", sheetNameArgumentCaptor.getValue());
            assertEquals(1, rowNumberArgumentCaptor.getValue());
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
        mockedParameters = MockParametersFactory
            .create(Map.of(IS_THE_FIRST_ROW_HEADER, true, SPREADSHEET_ID, "spreadsheetId", SHEET_NAME, "sheetName"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(mockedParameters))
                .thenReturn(mockedSheets);
            try (MockedStatic<GoogleSheetsRowUtils> googleSheetsRowUtilsMockedStatic = mockStatic(
                GoogleSheetsRowUtils.class)) {

                googleSheetsRowUtilsMockedStatic
                    .when(() -> GoogleSheetsRowUtils.getRowValues(
                        any(Sheets.class), anyString(), anyString(), anyInt()))
                    .thenReturn(List.of("header1", "header2", "header3"));

                ActionPropertiesFunction arrayPropertyForRow =
                    GoogleSheetsUtils.createPropertiesForNewRows(true);

                List<? extends ValueProperty<?>> result = arrayPropertyForRow.apply(
                    mockedParameters, mockedParameters, Map.of(), mockedActionContext);

                assertEquals(1, result.size());

                ModifiableObjectProperty first = (ModifiableObjectProperty) result.getFirst();

                assertEquals(VALUES, first.getName());
                assertEquals("Values", first.getLabel()
                    .get());

                List<? extends ValueProperty<?>> properties = first.getProperties()
                    .get();

                assertEquals(3, properties.size());

                for (int i = 0; i < properties.size(); i++) {
                    assertEquals("header" + (i + 1), properties.get(i)
                        .getName());
                    assertEquals("header" + (i + 1), properties.get(i)
                        .getLabel()
                        .get());
                    assertEquals("", properties.get(i)
                        .getDefaultValue()
                        .get());
                }
            }
        }
    }

    @Test
    void testCreatePropertiesForNewRowsForMultipleRowsAndWhenFirstRowIsHeader() throws Exception {
        mockedParameters = MockParametersFactory
            .create(Map.of(IS_THE_FIRST_ROW_HEADER, true, SPREADSHEET_ID, "spreadsheetId", SHEET_NAME, "sheetName"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(mockedParameters))
                .thenReturn(mockedSheets);
            try (MockedStatic<GoogleSheetsRowUtils> googleSheetsRowUtilsMockedStatic = mockStatic(
                GoogleSheetsRowUtils.class)) {

                googleSheetsRowUtilsMockedStatic
                    .when(() -> GoogleSheetsRowUtils.getRowValues(
                        any(Sheets.class), anyString(), anyString(), anyInt()))
                    .thenReturn(List.of("header1", "header2", "header3"));

                ActionPropertiesFunction propertiesForNewRows =
                    GoogleSheetsUtils.createPropertiesForNewRows(false);

                List<? extends ValueProperty<?>> result = propertiesForNewRows.apply(
                    mockedParameters, mockedParameters, Map.of(), mockedActionContext);

                assertEquals(1, result.size());

                ModifiableArrayProperty first = (ModifiableArrayProperty) result.getFirst();

                assertEquals(VALUES, first.getName());
                assertEquals("Rows", first.getLabel()
                    .get());
                assertEquals(true, first.getRequired()
                    .get());

                List<? extends ValueProperty<?>> items = first.getItems()
                    .get();

                assertEquals(1, items.size());

                ModifiableObjectProperty first1 = (ModifiableObjectProperty) items.getFirst();

                List<? extends ValueProperty<?>> valueProperties = first1.getProperties()
                    .get();

                for (int i = 0; i < valueProperties.size(); i++) {
                    assertEquals("header" + (i + 1), valueProperties.get(i)
                        .getName());
                    assertEquals("header" + (i + 1), valueProperties.get(i)
                        .getLabel()
                        .get());
                    assertEquals("", valueProperties.get(i)
                        .getDefaultValue()
                        .get());
                }
            }
        }
    }

    @Test
    void testCreatePropertiesForNewRowsForMultipleRowsAndWhenFirstRowIsNotHeader() throws Exception {
        mockedParameters = MockParametersFactory
            .create(Map.of(IS_THE_FIRST_ROW_HEADER, false, SPREADSHEET_ID, "spreadsheetId", SHEET_NAME, "sheetName"));

        ActionPropertiesFunction propertiesForNewRows =
            GoogleSheetsUtils.createPropertiesForNewRows(false);

        List<? extends ValueProperty<?>> result = propertiesForNewRows.apply(
            mockedParameters, mockedParameters, Map.of(), mockedActionContext);

        assertEquals(1, result.size());

        ModifiableArrayProperty first = (ModifiableArrayProperty) result.getFirst();

        assertEquals(VALUES, first.getName());
        assertEquals("Rows", first.getLabel()
            .get());
        assertEquals(true, first.getRequired()
            .get());

        List<? extends ValueProperty<?>> items = first.getItems()
            .get();

        assertEquals(1, items.size());

        ModifiableArrayProperty first1 = (ModifiableArrayProperty) items.getFirst();

        List<? extends ValueProperty<?>> valueProperties = first1.getItems()
            .get();

        assertEquals(3, valueProperties.size());

        assertEquals(List.of(bool(), number(), string()), valueProperties);
    }

    @Test
    void testCreatePropertiesForNewRowsForOneRowAndWhenFirstRowIsNotHeader() throws Exception {
        mockedParameters = MockParametersFactory
            .create(Map.of(IS_THE_FIRST_ROW_HEADER, false, SPREADSHEET_ID, "spreadsheetId", SHEET_NAME, "sheetName"));

        ActionPropertiesFunction propertiesForNewRows =
            GoogleSheetsUtils.createPropertiesForNewRows(true);

        List<? extends ValueProperty<?>> result = propertiesForNewRows.apply(
            mockedParameters, mockedParameters, Map.of(), mockedActionContext);

        assertEquals(1, result.size());

        ValueProperty<?> array = result.getFirst();

        assertEquals(3, ((ModifiableArrayProperty) array).getItems()
            .get()
            .size());

        assertEquals(VALUES, array.getName());
        assertEquals("Values", array.getLabel()
            .get());
        assertEquals(true, array.getRequired()
            .get());
    }

    @Test
    void testDeleteDimensionWhenColumn() throws Exception {

        Parameters parameters = MockParametersFactory.create(
            Map.of(SPREADSHEET_ID, "spreadsheetId", SHEET_ID, 123));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parameters))
                .thenReturn(mockedSheets);

            when(mockedSheets.spreadsheets())
                .thenReturn(mockedSpreadsheets);
            when(mockedSpreadsheets.batchUpdate(sheetNameArgumentCaptor.capture(),
                batchUpdateSpreadsheetRequestArgumentCaptor.capture()))
                    .thenReturn(mockedBatchUpdate);
            when(mockedBatchUpdate.execute())
                .thenReturn(mockedBatchUpdateSpreadsheetResponse);

            GoogleSheetsUtils.deleteDimension(parameters, parameters, 2, "COLUMNS");

            assertEquals("spreadsheetId", sheetNameArgumentCaptor.getValue());

            BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest =
                batchUpdateSpreadsheetRequestArgumentCaptor.getValue();

            List<Request> requests = batchUpdateSpreadsheetRequest.getRequests();

            assertEquals(1, requests.size());

            Request request = requests.getFirst();

            DeleteDimensionRequest deleteDimensionRequest = request.getDeleteDimension();

            DimensionRange dimensionRange = deleteDimensionRequest.getRange();

            assertEquals(123, dimensionRange.getSheetId());
            assertEquals("COLUMNS", dimensionRange.getDimension());
            assertEquals(1, dimensionRange.getStartIndex());
            assertEquals(2, dimensionRange.getEndIndex());
        }
    }

    @Test
    void testDeleteDimensionWhenRow() throws Exception {

        Parameters parameters = MockParametersFactory.create(
            Map.of(SPREADSHEET_ID, "spreadsheetId", SHEET_ID, 123));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parameters))
                .thenReturn(mockedSheets);

            when(mockedSheets.spreadsheets())
                .thenReturn(mockedSpreadsheets);
            when(mockedSpreadsheets.batchUpdate(sheetNameArgumentCaptor.capture(),
                batchUpdateSpreadsheetRequestArgumentCaptor.capture()))
                    .thenReturn(mockedBatchUpdate);
            when(mockedBatchUpdate.execute())
                .thenReturn(mockedBatchUpdateSpreadsheetResponse);

            GoogleSheetsUtils.deleteDimension(parameters, parameters, 2, "ROWS");

            assertEquals("spreadsheetId", sheetNameArgumentCaptor.getValue());

            BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest =
                batchUpdateSpreadsheetRequestArgumentCaptor.getValue();

            List<Request> requests = batchUpdateSpreadsheetRequest.getRequests();

            assertEquals(1, requests.size());

            Request request = requests.getFirst();

            DeleteDimensionRequest deleteDimensionRequest = request.getDeleteDimension();

            DimensionRange dimensionRange = deleteDimensionRequest.getRange();

            assertEquals(123, dimensionRange.getSheetId());
            assertEquals("ROWS", dimensionRange.getDimension());
            assertEquals(1, dimensionRange.getStartIndex());
            assertEquals(2, dimensionRange.getEndIndex());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetMapOfValuesForRowWhereFirstRowHeaders() throws IOException {
        List<Object> mockedRow = mock(List.class);
        List<Object> mockedFirstRow = mock(List.class);

        mockedParameters = MockParametersFactory
            .create(Map.of(IS_THE_FIRST_ROW_HEADER, true, SPREADSHEET_ID, "spreadsheetId", SHEET_NAME, "sheetName"));

        try (MockedStatic<GoogleSheetsRowUtils> sheetsRowUtilsMockedStatic = mockStatic(GoogleSheetsRowUtils.class)) {
            sheetsRowUtilsMockedStatic
                .when(() -> GoogleSheetsRowUtils.getRowValues(
                    any(Sheets.class), spreadsheetIdArgumentCaptor.capture(), sheetNameArgumentCaptor.capture(),
                    rowNumberArgumentCaptor.capture()))
                .thenReturn(mockedFirstRow);

            when(mockedFirstRow.get(anyInt())).thenReturn("header1", "header2", "header3");
            when(mockedRow.size()).thenReturn(3);
            when(mockedRow.get(anyInt())).thenReturn("value1", "value2", "value3");

            Map<String, Object> result = GoogleSheetsUtils.getMapOfValuesForRow(
                mockedParameters, mockedSheets, mockedRow);

            Map<String, Object> expected = new LinkedHashMap<>();

            expected.put("header1", "value1");
            expected.put("header2", "value2");
            expected.put("header3", "value3");

            assertEquals(expected, result);
            assertEquals("spreadsheetId", spreadsheetIdArgumentCaptor.getValue());
            assertEquals("sheetName", sheetNameArgumentCaptor.getValue());
            assertEquals(1, rowNumberArgumentCaptor.getValue());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetMapOfValuesForRowWhereFirstRowNotHeaders() throws IOException {
        mockedParameters = MockParametersFactory.create(Map.of(IS_THE_FIRST_ROW_HEADER, false));

        List<Object> mockedRow = mock(List.class);

        when(mockedRow.size()).thenReturn(3);
        when(mockedRow.get(anyInt())).thenReturn("value1", "value2", "value3");

        Map<String, Object> result = GoogleSheetsUtils.getMapOfValuesForRow(mockedParameters, mockedSheets, mockedRow);

        Map<String, Object> expected = new LinkedHashMap<>();

        expected.put("column_A", "value1");
        expected.put("column_B", "value2");
        expected.put("column_C", "value3");

        assertEquals(expected, result);
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
        List<Sheet> sheetsList = getSheetList();

        mockedParameters = MockParametersFactory.create(Map.of(SPREADSHEET_ID, "spreadsheetId"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(mockedParameters))
                .thenReturn(mockedSheets);

            when(mockedSheets.spreadsheets())
                .thenReturn(mockedSpreadsheets);
            when(mockedSpreadsheets.get(spreadsheetIdArgumentCaptor.capture()))
                .thenReturn(mockedGet);
            when(mockedGet.execute())
                .thenReturn(mockedSpreadsheet);
            when(mockedSpreadsheet.getSheets())
                .thenReturn(sheetsList);

            List<Option<String>> sheetIdOptions = GoogleSheetsUtils.getSheetIdOptions(
                mockedParameters, mockedParameters, Map.of(), anyString(), mockedActionContext);

            assertEquals("spreadsheetId", spreadsheetIdArgumentCaptor.getValue());
            assertNotNull(sheetIdOptions);
            assertEquals(2, sheetIdOptions.size());

            Option<String> sheetIdOptionsFirst = sheetIdOptions.getFirst();

            assertEquals("Sheet 1", sheetIdOptionsFirst.getLabel());
            assertEquals("1234567890", sheetIdOptionsFirst.getValue());

            Option<String> option = sheetIdOptions.get(1);

            assertEquals("Sheet 2", option.getLabel());
            assertEquals("98765432", option.getValue());
        }
    }

    @Test
    void testGetSheetNameOptions() throws Exception {
        List<Sheet> sheetsList = getSheetList();

        mockedParameters = MockParametersFactory.create(Map.of(SPREADSHEET_ID, "spreadsheetId"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(mockedParameters))
                .thenReturn(mockedSheets);

            when(mockedSheets.spreadsheets())
                .thenReturn(mockedSpreadsheets);
            when(mockedSpreadsheets.get(spreadsheetIdArgumentCaptor.capture()))
                .thenReturn(mockedGet);
            when(mockedGet.execute())
                .thenReturn(mockedSpreadsheet);
            when(mockedSpreadsheet.getSheets())
                .thenReturn(sheetsList);

            List<Option<String>> sheetNameOptions = GoogleSheetsUtils.getSheetNameOptions(
                mockedParameters, mockedParameters, Map.of(), anyString(), mockedActionContext);

            assertNotNull(sheetNameOptions);
            assertEquals(2, sheetNameOptions.size());

            Option<String> sheetNameOptionsFirst = sheetNameOptions.getFirst();

            assertEquals("Sheet 1", sheetNameOptionsFirst.getLabel());
            assertEquals("Sheet 1", sheetNameOptionsFirst.getValue());

            Option<String> option = sheetNameOptions.get(1);

            assertEquals("Sheet 2", option.getLabel());
            assertEquals("Sheet 2", option.getValue());
        }
    }

    @Test
    void testGetSpreadsheetValues() throws IOException {
        when(mockedSheets.spreadsheets())
            .thenReturn(mockedSpreadsheets);
        when(mockedSpreadsheets.values())
            .thenReturn(mockedValues);
        when(mockedValues.get(spreadsheetIdArgumentCaptor.capture(), sheetNameArgumentCaptor.capture()))
            .thenReturn(mockedValuesGet);
        when(mockedValuesGet.setValueRenderOption(valueRenderArgumentCaptor.capture()))
            .thenReturn(mockedValuesGet);
        when(mockedValuesGet.setDateTimeRenderOption(dateTimeRenderArgumentCaptor.capture()))
            .thenReturn(mockedValuesGet);
        when(mockedValuesGet.setMajorDimension(majorDimensionArgumentCaptor.capture()))
            .thenReturn(mockedValuesGet);
        when(mockedValuesGet.execute())
            .thenReturn(mockedValueRange);
        when(mockedValueRange.getValues())
            .thenReturn(List.of(List.of()));

        List<List<Object>> result = GoogleSheetsUtils.getSpreadsheetValues(mockedSheets, "spreadsheetId", "sheetName");

        assertEquals(List.of(List.of()), result);

        assertEquals("spreadsheetId", spreadsheetIdArgumentCaptor.getValue());
        assertEquals("sheetName", sheetNameArgumentCaptor.getValue());
        assertEquals("UNFORMATTED_VALUE", valueRenderArgumentCaptor.getValue());
        assertEquals("FORMATTED_STRING", dateTimeRenderArgumentCaptor.getValue());
        assertEquals("ROWS", majorDimensionArgumentCaptor.getValue());
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

            googleServicesMockedStatic.when(() -> GoogleServices.getSheets(mockedParameters))
                .thenReturn(mockedSheets);

            List<Object> rowToUpdate = new ArrayList<>(List.of("cde", 345, true));
            googleSheetsRowUtilsMockedStatic.when(() -> GoogleSheetsRowUtils.getRowValues(
                sheetsArgumentCaptor.capture(), spreadsheetIdArgumentCaptor.capture(),
                sheetNameArgumentCaptor.capture(),
                rowNumberArgumentCaptor.capture()))
                .thenReturn(List.of("header1", "header2", "header3"), rowToUpdate);

            List<Object> result = GoogleSheetsUtils.getUpdatedRowValues(mockedParameters, mockedParameters);

            assertEquals(List.of("abc", 345, false), result);

            assertEquals(List.of(mockedSheets, mockedSheets), sheetsArgumentCaptor.getAllValues());
            assertEquals(List.of("spreadsheetId", "spreadsheetId"), spreadsheetIdArgumentCaptor.getAllValues());
            assertEquals(List.of("sheetName", "sheetName"), sheetNameArgumentCaptor.getAllValues());
            assertEquals(List.of(1, 5), rowNumberArgumentCaptor.getAllValues());
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

            googleServicesMockedStatic.when(() -> GoogleServices.getSheets(mockedParameters))
                .thenReturn(mockedSheets);

            List<Object> rowToUpdate = new ArrayList<>(List.of("cde", 345, true));
            googleSheetsRowUtilsMockedStatic.when(() -> GoogleSheetsRowUtils.getRowValues(
                sheetsArgumentCaptor.capture(), spreadsheetIdArgumentCaptor.capture(),
                sheetNameArgumentCaptor.capture(),
                rowNumberArgumentCaptor.capture()))
                .thenReturn(rowToUpdate);

            List<Object> result = GoogleSheetsUtils.getUpdatedRowValues(mockedParameters, mockedParameters);

            assertEquals(List.of("abc", 345, false), result);

            assertEquals(mockedSheets, sheetsArgumentCaptor.getValue());
            assertEquals("spreadsheetId", spreadsheetIdArgumentCaptor.getValue());
            assertEquals("sheetName", sheetNameArgumentCaptor.getValue());
            assertEquals(5, rowNumberArgumentCaptor.getValue());
        }
    }

    private static List<Sheet> getSheetList() {
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

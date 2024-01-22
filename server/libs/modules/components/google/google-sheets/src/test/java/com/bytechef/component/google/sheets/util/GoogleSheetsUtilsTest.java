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

import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.INCLUDE_ITEMS_FROM_ALL_DRIVES;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Monika Domiter
 */
class GoogleSheetsUtilsTest {

    private final ArgumentCaptor<Boolean> includeItemsFromAllDrivesArgumentCaptor =
        ArgumentCaptor.forClass(Boolean.class);
    private final ArgumentCaptor<Boolean> supportsAllDrivesArgumentCaptor = ArgumentCaptor.forClass(Boolean.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Drive mockedDrive = mock(Drive.class);
    private final Drive.Files mockedFiles = mock(Drive.Files.class);
    private final Sheets.Spreadsheets.Get mockedGet = Mockito.mock(Sheets.Spreadsheets.Get.class);
    private final Drive.Files.List mockedList = mock(Drive.Files.List.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Sheets mockedSheets = mock(Sheets.class);
    private final Spreadsheet mockedSpreadsheet = Mockito.mock(Spreadsheet.class);
    private final Sheets.Spreadsheets mockedSpreadsheets = Mockito.mock(Sheets.Spreadsheets.class);
    private final ArgumentCaptor<String> qArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Integer> rowNumberArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    private final ArgumentCaptor<Integer> sheetIdArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    private final ArgumentCaptor<String> spreadsheetIdArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testCreateRangeForRowNumberNotNull() {
        GoogleSheetsUtils.sheetIdMap.put(1, "Sheet1");
        GoogleSheetsUtils.sheetIdMap.put(2, "Sheet2");

        String actualRange = GoogleSheetsUtils.createRange(1, 5);

        assertEquals("Sheet1!5:5", actualRange);
    }

    @Test
    void testCreateRangeForRowNumberNull() {
        GoogleSheetsUtils.sheetIdMap.put(1, "Sheet1");
        GoogleSheetsUtils.sheetIdMap.put(2, "Sheet2");

        String actualRange = GoogleSheetsUtils.createRange(1, null);

        assertEquals("Sheet1", actualRange);
    }

    @Test
    void testCreateArrayPropertyForRowWhereFirstRowIsHeader() throws IOException {

        when(mockedParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER))
            .thenReturn(true);
        when(mockedParameters.getRequiredString(SPREADSHEET_ID))
            .thenReturn("spreadsheetId");
        when(mockedParameters.getRequiredInteger(SHEET_ID))
            .thenReturn(123);

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(mockedParameters))
                .thenReturn(mockedSheets);
            try (MockedStatic<GoogleSheetsRowUtils> googleSheetsRowUtilsMockedStatic = mockStatic(GoogleSheetsRowUtils.class)) {
                googleSheetsRowUtilsMockedStatic
                    .when(() -> GoogleSheetsRowUtils.getRow(any(Sheets.class), anyString(), anyInt(), anyInt())).thenReturn(List.of("header1", "header2", "header3"));

                List<Property.ArrayProperty> result = GoogleSheetsUtils.createArrayPropertyForRow(mockedParameters, mockedParameters, mockedContext);

                assertEquals(1, result.size());
                assertEquals(3, result.getFirst().getItems().get().size());
                assertEquals("header1", result.getFirst().getItems().get().getFirst().getName());
                assertEquals("header2", result.getFirst().getItems().get().get(1).getName());
                assertEquals("header3", result.getFirst().getItems().get().getLast().getName());
            }
        }
    }

    @Test
    void testGetMapOfValuesForRowWhereFirstRowHeaders() throws IOException {
        List<Object> mockedRow = mock(List.class);
        List<Object> mockedFirstRow = mock(List.class);

        when(mockedParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER))
            .thenReturn(true);
        when(mockedParameters.getRequiredString(SPREADSHEET_ID))
            .thenReturn("spreadsheetId");
        when(mockedParameters.getRequiredInteger(SHEET_ID))
            .thenReturn(123);

        try (MockedStatic<GoogleSheetsRowUtils> sheetsRowUtilsMockedStatic =
            mockStatic(GoogleSheetsRowUtils.class)) {
            sheetsRowUtilsMockedStatic
                .when(() -> GoogleSheetsRowUtils.getRow(any(Sheets.class), spreadsheetIdArgumentCaptor.capture(),
                    sheetIdArgumentCaptor.capture(), rowNumberArgumentCaptor.capture()))
                .thenReturn(mockedFirstRow);

            when(mockedFirstRow.get(anyInt())).thenReturn("header1", "header2", "header3");
            when(mockedRow.size()).thenReturn(3);
            when(mockedRow.get(anyInt())).thenReturn("value1", "value2", "value3");

            Map<String, Object> result =
                GoogleSheetsUtils.getMapOfValuesForRow(mockedParameters, mockedSheets, mockedRow);

            Map<String, Object> expected = new LinkedHashMap<>();

            expected.put("header1", "value1");
            expected.put("header2", "value2");
            expected.put("header3", "value3");

            assertEquals(expected, result);
            assertEquals("spreadsheetId", spreadsheetIdArgumentCaptor.getValue());
            assertEquals(123, sheetIdArgumentCaptor.getValue());
            assertEquals(1, rowNumberArgumentCaptor.getValue());
        }
    }

    @Test
    void testGetMapOfValuesForRowWhereFirstRowNotHeaders() throws IOException {
        List<Object> mockedRow = mock(List.class);

        when(mockedParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER))
            .thenReturn(false);

        when(mockedRow.size()).thenReturn(3);
        when(mockedRow.get(anyInt())).thenReturn("value1", "value2", "value3");

        Map<String, Object> result =
            GoogleSheetsUtils.getMapOfValuesForRow(mockedParameters, mockedSheets, mockedRow);

        Map<String, Object> expected = new LinkedHashMap<>();

        expected.put("column A", "value1");
        expected.put("column B", "value2");
        expected.put("column C", "value3");

        assertEquals(expected, result);
    }

    @Test
    void testGetSheetIdOptions() throws IOException {
        List<Sheet> sheetsList = getSheetList();

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
            List<Option<String>> sheetIdOptions =
                GoogleSheetsUtils.getSheetIdOptions(mockedParameters, mockedParameters, anyString(), mockedContext);

            assertNotNull(sheetIdOptions);
            assertEquals(2, sheetIdOptions.size());
            assertEquals("Sheet 1", sheetIdOptions.getFirst()
                .getLabel());
            assertEquals("1234567890", sheetIdOptions.getFirst()
                .getValue());
            assertEquals("Sheet 2", sheetIdOptions.get(1)
                .getLabel());
            assertEquals("98765432", sheetIdOptions.get(1)
                .getValue());
        }
    }

    @Test
    void testGetSpreadsheetIdOptions() throws IOException {
        File file1 = new File();
        file1.setName("Spreadsheet 1");
        file1.setId("1234567890");

        File file2 = new File();
        file2.setName("Spreadsheet 2");
        file2.setId("0987654321");

        List<File> files = Arrays.asList(file1, file2);

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getDrive(mockedParameters))
                .thenReturn(mockedDrive);

            when(mockedParameters.getBoolean(INCLUDE_ITEMS_FROM_ALL_DRIVES))
                .thenReturn(true);
            when(mockedDrive.files())
                .thenReturn(mockedFiles);
            when(mockedFiles.list())
                .thenReturn(mockedList);
            when(mockedList.setQ(qArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.setIncludeItemsFromAllDrives(includeItemsFromAllDrivesArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.setSupportsAllDrives(supportsAllDrivesArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.execute())
                .thenReturn(new FileList().setFiles(files));

            List<Option<String>> spreadsheetIdOptions =
                GoogleSheetsUtils.getSpreadsheetIdOptions(mockedParameters, mockedParameters, anyString(),
                    mockedContext);

            assertNotNull(spreadsheetIdOptions);
            assertEquals(2, spreadsheetIdOptions.size());

            assertEquals("Spreadsheet 1", spreadsheetIdOptions.getFirst()
                .getLabel());
            assertEquals("1234567890", spreadsheetIdOptions.getFirst()
                .getValue());
            assertEquals("Spreadsheet 2", spreadsheetIdOptions.get(1)
                .getLabel());
            assertEquals("0987654321", spreadsheetIdOptions.get(1)
                .getValue());
            assertEquals("mimeType='application/vnd.google-apps.spreadsheet'", qArgumentCaptor.getValue());
            assertEquals(true, includeItemsFromAllDrivesArgumentCaptor.getValue());
            assertEquals(true, supportsAllDrivesArgumentCaptor.getValue());
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

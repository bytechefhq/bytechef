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

package com.bytechef.component.google.sheets.action;

import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.sheets.util.GoogleSheetsUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleSheetsGetRowsActionTest {

    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final Context mockedContext = mock(Context.class);
    private final Spreadsheets.Values.Get mockedGet = mock(Spreadsheets.Values.Get.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(SPREADSHEET_ID, "spreadsheetId", SHEET_NAME, "sheetName", IS_THE_FIRST_ROW_HEADER, true));
    private final Sheets mockedSheets = mock(Sheets.class);
    private final Spreadsheets mockedSpreadsheets = mock(Spreadsheets.class);
    private final Spreadsheets.Values mockedValues = mock(Spreadsheets.Values.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final ArgumentCaptor<Sheets> sheetsArgumentCaptor = ArgumentCaptor.forClass(Sheets.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    @SuppressWarnings("unchecked")
    void testPerform() throws Exception {
        Map<String, Object> rowMap = Map.of("first-name", "John", "last-name", "Doe", "year", 2023);
        List<Object> firstRow = List.of("John", "Doe", 2023);

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class)) {

            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.getMapOfValuesForRow(
                    parametersArgumentCaptor.capture(), sheetsArgumentCaptor.capture(), listArgumentCaptor.capture()))
                .thenReturn(rowMap);
            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parametersArgumentCaptor.capture()))
                .thenReturn(mockedSheets);
            when(mockedSheets.spreadsheets())
                .thenReturn(mockedSpreadsheets);
            when(mockedSpreadsheets.values())
                .thenReturn(mockedValues);
            when(mockedValues.get(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(mockedGet);
            when(mockedGet.execute())
                .thenReturn(
                    new ValueRange()
                        .setValues(List.of(List.of("first-name", "last-name", "year"), firstRow)));

            Object result = GoogleSheetsGetRowsAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(List.of(rowMap), result);
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(List.of("spreadsheetId", "sheetName"), stringArgumentCaptor.getAllValues());
            assertEquals(List.of(firstRow), listArgumentCaptor.getAllValues());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPerformForNullValues() throws Exception {
        Map<String, Object> rowMap = Map.of("first-name", "John", "last-name", "Doe", "year", 2023);

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class)) {

            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.getMapOfValuesForRow(
                    parametersArgumentCaptor.capture(), sheetsArgumentCaptor.capture(), listArgumentCaptor.capture()))
                .thenReturn(rowMap);
            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parametersArgumentCaptor.capture()))
                .thenReturn(mockedSheets);
            when(mockedSheets.spreadsheets())
                .thenReturn(mockedSpreadsheets);
            when(mockedSpreadsheets.values())
                .thenReturn(mockedValues);
            when(mockedValues.get(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(mockedGet);
            when(mockedGet.execute())
                .thenReturn(new ValueRange());

            Object result = GoogleSheetsGetRowsAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(List.of(), result);
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(List.of("spreadsheetId", "sheetName"), stringArgumentCaptor.getAllValues());
            assertEquals(List.of(), listArgumentCaptor.getAllValues());
        }
    }
}

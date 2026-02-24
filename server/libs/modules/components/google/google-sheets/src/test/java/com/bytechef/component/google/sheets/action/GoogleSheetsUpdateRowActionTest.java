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

import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.ROW_NUMBER;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.sheets.util.GoogleSheetsUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Update;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleSheetsUpdateRowActionTest {

    private final ArgumentCaptor<Integer> integerArgumentCaptorArgumentCaptor = forClass(Integer.class);
    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> listArgumentCaptor = forClass(List.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Sheets mockedSheets = mock(Sheets.class);
    private final Spreadsheets mockedSpreadsheets = mock(Spreadsheets.class);
    private final Update mockedUpdate = mock(Update.class);
    private final Values mockedValues = mock(Values.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(SPREADSHEET_ID, "spreadsheetId", SHEET_NAME, "sheetName", ROW_NUMBER, 2));
    private final ArgumentCaptor<Sheets> sheetsArgumentCaptor = forClass(Sheets.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);
    private final ArgumentCaptor<ValueRange> valueRangeArgumentCaptor = forClass(ValueRange.class);

    @Test
    @SuppressWarnings("unchecked")
    void testPerform() throws Exception {
        Map<Object, Object> responseMap = Map.of();
        List<Object> row = List.of("abc", "sheetName", false);

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parametersArgumentCaptor.capture()))
                .thenReturn(mockedSheets);
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.getUpdatedRowValues(
                    parametersArgumentCaptor.capture(), parametersArgumentCaptor.capture()))
                .thenReturn(row);
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.createRange(
                    stringArgumentCaptor.capture(), integerArgumentCaptorArgumentCaptor.capture()))
                .thenReturn("range");
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.getMapOfValuesForRow(
                    parametersArgumentCaptor.capture(), sheetsArgumentCaptor.capture(), listArgumentCaptor.capture()))
                .thenReturn(responseMap);

            when(mockedSheets.spreadsheets())
                .thenReturn(mockedSpreadsheets);
            when(mockedSpreadsheets.values())
                .thenReturn(mockedValues);
            when(mockedValues.update(
                stringArgumentCaptor.capture(), stringArgumentCaptor.capture(), valueRangeArgumentCaptor.capture()))
                    .thenReturn(mockedUpdate);
            when(mockedUpdate.setValueInputOption(stringArgumentCaptor.capture()))
                .thenReturn(mockedUpdate);

            Map<String, Object> result = GoogleSheetsUpdateRowAction.perform(
                mockedParameters, mockedParameters, mockedActionContext);

            assertEquals(result, responseMap);

            assertEquals(
                List.of(mockedParameters, mockedParameters, mockedParameters, mockedParameters),
                parametersArgumentCaptor.getAllValues());
            assertEquals(
                List.of("sheetName", "spreadsheetId", "range", "USER_ENTERED"),
                stringArgumentCaptor.getAllValues());
            assertEquals(2, integerArgumentCaptorArgumentCaptor.getValue());
            assertEquals(mockedSheets, sheetsArgumentCaptor.getValue());
            assertEquals(row, listArgumentCaptor.getValue());

            ValueRange valueRange = new ValueRange()
                .setValues(List.of(row))
                .setMajorDimension("ROWS");

            assertEquals(valueRange, valueRangeArgumentCaptor.getValue());
        }
    }
}

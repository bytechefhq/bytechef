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

package com.bytechef.component.google.sheets.action;

import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.ROWS;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.VALUES;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.VALUE_INPUT_OPTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.sheets.util.GoogleSheetsUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleSheetsInsertMultipleRowsActionTest {

    private final ArgumentCaptor<Integer> integerArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Sheets.Spreadsheets.Values.Append mockedAppend = mock(Sheets.Spreadsheets.Values.Append.class);
    private final Sheets mockedSheets = mock(Sheets.class);
    private final Sheets.Spreadsheets mockedSpreadsheets = mock(Sheets.Spreadsheets.class);
    private final Sheets.Spreadsheets.Values mockedValues = mock(Sheets.Spreadsheets.Values.class);
    private final ArgumentCaptor<Sheets> sheetsArgumentCaptor = ArgumentCaptor.forClass(Sheets.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final ArgumentCaptor<ValueRange> valueRangeArgumentCaptor = ArgumentCaptor.forClass(ValueRange.class);

    @Test
    @SuppressWarnings("unchecked")
    void perform() throws Exception {
        Map<Object, Object> responseMap = Map.of();
        List<Object> row = List.of("abc", "sheetName", false);

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(SPREADSHEET_ID, "spreadsheetId", SHEET_NAME, "sheetName", VALUE_INPUT_OPTION, "USER_ENTERED", ROWS,
                Map.of(VALUES, List.of(row))));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class)) {

            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parametersArgumentCaptor.capture()))
                .thenReturn(mockedSheets);
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.createRange(
                    stringArgumentCaptor.capture(), integerArgumentCaptor.capture()))
                .thenReturn("range");
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.getMapOfValuesForRow(
                    parametersArgumentCaptor.capture(), sheetsArgumentCaptor.capture(), listArgumentCaptor.capture()))
                .thenReturn(responseMap);

            when(mockedSheets.spreadsheets())
                .thenReturn(mockedSpreadsheets);
            when(mockedSpreadsheets.values())
                .thenReturn(mockedValues);
            when(mockedValues.append(
                stringArgumentCaptor.capture(), stringArgumentCaptor.capture(), valueRangeArgumentCaptor.capture()))
                    .thenReturn(mockedAppend);
            when(mockedAppend.setValueInputOption(stringArgumentCaptor.capture()))
                .thenReturn(mockedAppend);

            List<Map<String, Object>> result = GoogleSheetsInsertMultipleRowsAction.perform(
                mockedParameters, mockedParameters, mockedActionContext);

            assertEquals(List.of(responseMap), result);
            assertEquals(List.of(mockedParameters, mockedParameters), parametersArgumentCaptor.getAllValues());
            assertEquals(mockedSheets, sheetsArgumentCaptor.getValue());
            assertEquals(
                List.of("sheetName", "spreadsheetId", "range", "USER_ENTERED"),
                stringArgumentCaptor.getAllValues());
            assertNull(integerArgumentCaptor.getValue());
            assertEquals(row, listArgumentCaptor.getValue());

            ValueRange valueRange = new ValueRange()
                .setValues(List.of(row))
                .setMajorDimension("ROWS");

            assertEquals(valueRange, valueRangeArgumentCaptor.getValue());
        }
    }
}

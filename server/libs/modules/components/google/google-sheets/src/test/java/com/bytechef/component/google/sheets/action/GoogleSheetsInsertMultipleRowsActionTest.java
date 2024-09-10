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

    private final Sheets.Spreadsheets.Values.Append mockedAppend = mock(Sheets.Spreadsheets.Values.Append.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final ArgumentCaptor<String> sheetNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> valueInputOptionArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> rangeArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Integer> rowNumberArgumentCapture = ArgumentCaptor.forClass(Integer.class);
    private final ArgumentCaptor<ValueRange> valueRangeArgumentCaptor = ArgumentCaptor.forClass(ValueRange.class);
    private final Sheets mockedSheets = mock(Sheets.class);
    private final Sheets.Spreadsheets mockedSpreadsheets = mock(Sheets.Spreadsheets.class);
    private final Sheets.Spreadsheets.Values mockedValues = mock(Sheets.Spreadsheets.Values.class);
    private final ArgumentCaptor<String> spreadsheetIdArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void perform() throws Exception {
        Map<Object, Object> responseMap = Map.of();
        List<Object> row = List.of("abc", "sheetName", false);

        Parameters parameters = MockParametersFactory.create(
            Map.of(SPREADSHEET_ID, "spreadsheetId", SHEET_NAME, "sheetName", VALUE_INPUT_OPTION, "USER_ENTERED", ROWS,
                Map.of(VALUES, List.of(row))));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class)) {

            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parameters))
                .thenReturn(mockedSheets);
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.createRange(sheetNameArgumentCaptor.capture(),
                    rowNumberArgumentCapture.capture()))
                .thenReturn("range");
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.getMapOfValuesForRow(parameters, mockedSheets, row))
                .thenReturn(responseMap);

            when(mockedSheets.spreadsheets())
                .thenReturn(mockedSpreadsheets);
            when(mockedSpreadsheets.values())
                .thenReturn(mockedValues);
            when(mockedValues.append(spreadsheetIdArgumentCaptor.capture(), rangeArgumentCaptor.capture(),
                valueRangeArgumentCaptor.capture()))
                    .thenReturn(mockedAppend);
            when(mockedAppend.setValueInputOption(valueInputOptionArgumentCaptor.capture()))
                .thenReturn(mockedAppend);

            List<Map<String, Object>> result = GoogleSheetsInsertMultipleRowsAction.perform(
                parameters, parameters, mockedContext);

            assertEquals(List.of(responseMap), result);

            assertEquals("sheetName", sheetNameArgumentCaptor.getValue());
            assertNull(rowNumberArgumentCapture.getValue());
            assertEquals("spreadsheetId", spreadsheetIdArgumentCaptor.getValue());
            assertEquals("range", rangeArgumentCaptor.getValue());
            assertEquals("USER_ENTERED", valueInputOptionArgumentCaptor.getValue());

            ValueRange valueRange = valueRangeArgumentCaptor.getValue();

            assertEquals("ROWS", valueRange.getMajorDimension());

            List<List<Object>> valueRangeValues = valueRange.getValues();

            assertEquals(row, valueRangeValues.getFirst());
        }
    }
}

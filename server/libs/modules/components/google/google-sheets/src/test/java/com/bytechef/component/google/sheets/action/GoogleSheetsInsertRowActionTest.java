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

import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.VALUE_INPUT_OPTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.google.sheets.util.GoogleSheetsUtils;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
class GoogleSheetsInsertRowActionTest extends AbstractGoogleSheetsActionTest {

    private final Sheets.Spreadsheets.Values.Append mockedAppend = mock(Sheets.Spreadsheets.Values.Append.class);
    private final Map<String, Object> mockedMap = mock(Map.class);
    private final Sheets.Spreadsheets mockedSpreadsheets = mock(Sheets.Spreadsheets.class);
    private final Sheets.Spreadsheets.Values mockedValues = mock(Sheets.Spreadsheets.Values.class);
    private final ArgumentCaptor<String> sheetNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> valueInputOptionArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<ValueRange> valueRangeArgumentCaptor = ArgumentCaptor.forClass(ValueRange.class);

    @Test
    void perform() throws Exception {
        List<Object> row = List.of("abc", "sheetName", false);

        when(mockedParameters.getRequiredString(VALUE_INPUT_OPTION))
            .thenReturn("USER_ENTERED");

        when(mockedSheets.spreadsheets())
            .thenReturn(mockedSpreadsheets);
        when(mockedSpreadsheets.values())
            .thenReturn(mockedValues);
        when(
            mockedValues.append(spreadsheetIdArgumentCaptor.capture(), anyString(), valueRangeArgumentCaptor.capture()))
                .thenReturn(mockedAppend);
        when(mockedAppend.setValueInputOption(valueInputOptionArgumentCaptor.capture()))
            .thenReturn(mockedAppend);

        try (MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class)) {
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.getRowValues(mockedParameters))
                .thenReturn(row);
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.createRange(sheetNameArgumentCaptor.capture(), any()))
                .thenReturn("range");
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.getMapOfValuesForRow(mockedParameters, mockedSheets, row))
                .thenReturn(mockedMap);

            Map<String, Object> result = GoogleSheetsInsertRowAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            assertEquals(mockedMap, result);

            assertEquals("spreadsheetId", spreadsheetIdArgumentCaptor.getValue());
            assertEquals("sheetName", sheetNameArgumentCaptor.getValue());
            assertEquals("USER_ENTERED", valueInputOptionArgumentCaptor.getValue());

            ValueRange valueRange = valueRangeArgumentCaptor.getValue();

            assertEquals("ROWS", valueRange.getMajorDimension());

            List<List<Object>> valueRangeValues = valueRange.getValues();

            assertEquals(row, valueRangeValues.getFirst());
        }
    }
}

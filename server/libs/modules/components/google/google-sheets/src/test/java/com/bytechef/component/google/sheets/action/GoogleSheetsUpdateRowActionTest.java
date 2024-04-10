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

import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.ROW_NUMBER;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.VALUES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.google.sheets.util.GoogleSheetsUtils;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
class GoogleSheetsUpdateRowActionTest extends AbstractGoogleSheetsActionTest {

    @SuppressWarnings("unchecked")
    private final Map<String, Object> mockedMap = mock(Map.class);
    private final Sheets.Spreadsheets mockedSpreadsheets = mock(Sheets.Spreadsheets.class);
    private final Sheets.Spreadsheets.Values.Update mockedUpdate = mock(Sheets.Spreadsheets.Values.Update.class);
    private final Sheets.Spreadsheets.Values mockedValues = mock(Sheets.Spreadsheets.Values.class);
    protected ArgumentCaptor<String> sheetNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> valueInputOptionArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<ValueRange> valueRangeArgumentCaptor = ArgumentCaptor.forClass(ValueRange.class);

    @Test
    void testPerformWhereFirstRowNotHeader() throws IOException {
        List<Object> values = List.of("abc", "sheetName", false);

        when(mockedParameters.getRequiredInteger(ROW_NUMBER))
            .thenReturn(2);
        when(mockedParameters.getRequired(VALUES))
            .thenReturn(List.of());
        when(mockedParameters.getRequiredList(VALUES, Object.class))
            .thenReturn(values);

        try (MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class)) {
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.createRange(sheetNameArgumentCaptor.capture(), any()))
                .thenReturn("range");
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.getMapOfValuesForRow(mockedParameters, mockedSheets, values))
                .thenReturn(mockedMap);

            when(mockedSheets.spreadsheets())
                .thenReturn(mockedSpreadsheets);
            when(mockedSpreadsheets.values())
                .thenReturn(mockedValues);
            when(
                mockedValues.update(
                    spreadsheetIdArgumentCaptor.capture(), anyString(), valueRangeArgumentCaptor.capture()))
                        .thenReturn(mockedUpdate);
            when(mockedUpdate.setValueInputOption(valueInputOptionArgumentCaptor.capture()))
                .thenReturn(mockedUpdate);

            Map<String, Object> result =
                GoogleSheetsUpdateRowAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(result, mockedMap);
            assertEquals("spreadsheetId", spreadsheetIdArgumentCaptor.getValue());
            assertEquals("sheetName", sheetNameArgumentCaptor.getValue());
            assertEquals("USER_ENTERED", valueInputOptionArgumentCaptor.getValue());

            ValueRange valueRange = valueRangeArgumentCaptor.getValue();

            assertEquals("ROWS", valueRange.getMajorDimension());

            List<List<Object>> valueRangeValues = valueRange.getValues();

            assertEquals(values, valueRangeValues.getFirst());
        }
    }

    @Test
    void testPerformWhereFirstRowIsHeader() throws IOException {
        LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<>();

        linkedHashMap.put("header1", "abc");
        linkedHashMap.put("header2", 123);
        linkedHashMap.put("header3", false);

        ArrayList<Object> values = new ArrayList<>(linkedHashMap.values());

        when(mockedParameters.getRequiredInteger(ROW_NUMBER))
            .thenReturn(2);
        when(mockedParameters.getRequired(VALUES))
            .thenReturn(linkedHashMap);

        try (MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class)) {
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.createRange(sheetNameArgumentCaptor.capture(), any()))
                .thenReturn("range");
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.getMapOfValuesForRow(mockedParameters, mockedSheets, values))
                .thenReturn(mockedMap);

            when(mockedSheets.spreadsheets())
                .thenReturn(mockedSpreadsheets);
            when(mockedSpreadsheets.values())
                .thenReturn(mockedValues);
            when(
                mockedValues.update(
                    spreadsheetIdArgumentCaptor.capture(), anyString(), valueRangeArgumentCaptor.capture()))
                        .thenReturn(mockedUpdate);
            when(mockedUpdate.setValueInputOption(valueInputOptionArgumentCaptor.capture()))
                .thenReturn(mockedUpdate);

            Map<String, Object> result =
                GoogleSheetsUpdateRowAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(result, mockedMap);
            assertEquals("spreadsheetId", spreadsheetIdArgumentCaptor.getValue());
            assertEquals("sheetName", sheetNameArgumentCaptor.getValue());
            assertEquals("USER_ENTERED", valueInputOptionArgumentCaptor.getValue());

            ValueRange valueRange = valueRangeArgumentCaptor.getValue();

            assertEquals("ROWS", valueRange.getMajorDimension());

            List<List<Object>> valueRangeValues = valueRange.getValues();

            assertEquals(values, valueRangeValues.getFirst());

        }
    }
}

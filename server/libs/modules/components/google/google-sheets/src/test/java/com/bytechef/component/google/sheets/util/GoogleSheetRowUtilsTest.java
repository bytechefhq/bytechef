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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleSheetRowUtilsTest {

    private final ArgumentCaptor<Integer> integerArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final Sheets.Spreadsheets.Values.BatchGet mockedBatchGet = mock(Sheets.Spreadsheets.Values.BatchGet.class);
    private final Sheets mockedSheets = mock(Sheets.class);
    private final Sheets.Spreadsheets mockedSpreadsheets = mock(Sheets.Spreadsheets.class);
    private final Sheets.Spreadsheets.Values mockedValues = mock(Sheets.Spreadsheets.Values.class);
    private final ValueRange mockedValueRange = mock(ValueRange.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testGetRow() throws IOException {
        testTemplate(List.of("value1", "value2", "value3"));
    }

    @Test
    void testGetRowEmptySheet() throws IOException {
        testTemplate(null);
    }

    @SuppressWarnings("unchecked")
    private void testTemplate(List<String> values) throws IOException {
        String range = "range";

        try (MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class)) {
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.createRange(
                    stringArgumentCaptor.capture(), integerArgumentCaptor.capture()))
                .thenReturn(range);

            when(mockedSheets.spreadsheets())
                .thenReturn(mockedSpreadsheets);
            when(mockedSpreadsheets.values())
                .thenReturn(mockedValues);
            when(mockedValues.batchGet(stringArgumentCaptor.capture()))
                .thenReturn(mockedBatchGet);
            when(mockedBatchGet.setRanges(listArgumentCaptor.capture()))
                .thenReturn(mockedBatchGet);
            when(mockedBatchGet.setValueRenderOption(stringArgumentCaptor.capture()))
                .thenReturn(mockedBatchGet);
            when(mockedBatchGet.setDateTimeRenderOption(stringArgumentCaptor.capture()))
                .thenReturn(mockedBatchGet);
            when(mockedBatchGet.setMajorDimension(stringArgumentCaptor.capture()))
                .thenReturn(mockedBatchGet);
            when(mockedBatchGet.execute())
                .thenReturn(new BatchGetValuesResponse().setValueRanges(List.of(mockedValueRange)));

            when(mockedValueRange.getValues())
                .thenReturn(values == null ? null : List.of(List.of(values)));

            List<Object> result = GoogleSheetsRowUtils.getRowValues(mockedSheets, "spreadsheetId", "sheetName", 1);

            assertEquals(values == null ? List.of() : List.of(values), result);
            assertEquals(
                List.of("spreadsheetId", "sheetName", "UNFORMATTED_VALUE", "FORMATTED_STRING", "ROWS"),
                stringArgumentCaptor.getAllValues());
            assertEquals(1, integerArgumentCaptor.getValue());
            assertEquals(List.of(range), listArgumentCaptor.getValue());
        }
    }
}

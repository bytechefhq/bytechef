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
import org.mockito.Mockito;

/**
 * @author Monika Domiter
 */
class GoogleSheetRowUtilsTest {

    private final Sheets mockedSheets = mock(Sheets.class);
    private final Sheets.Spreadsheets mockedSpreadsheets = Mockito.mock(Sheets.Spreadsheets.class);
    private final Sheets.Spreadsheets.Values mockedValues = Mockito.mock(Sheets.Spreadsheets.Values.class);
    private final ValueRange mockedValueRange = Mockito.mock(ValueRange.class);
    private final Sheets.Spreadsheets.Values.BatchGet mockedBatchGet =
        Mockito.mock(Sheets.Spreadsheets.Values.BatchGet.class);
    private final ArgumentCaptor<String> valueRenderOptionArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> dateTimeRenderOptionArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> majorDimensionArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testGetRow() throws IOException {
        String spreadSheetId = "spreadsheetId";
        String sheetName = "sheetName";
        Integer rowNumber = 1;
        String range = "range";

        try (MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class)) {
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.createRange(sheetName, rowNumber))
                .thenReturn(range);

            when(mockedSheets.spreadsheets())
                .thenReturn(mockedSpreadsheets);
            when(mockedSpreadsheets.values())
                .thenReturn(mockedValues);
            when(mockedValues.batchGet(spreadSheetId))
                .thenReturn(mockedBatchGet);
            when(mockedBatchGet.setRanges(List.of(range)))
                .thenReturn(mockedBatchGet);
            when(mockedBatchGet.setValueRenderOption(valueRenderOptionArgumentCaptor.capture()))
                .thenReturn(mockedBatchGet);
            when(mockedBatchGet.setDateTimeRenderOption(dateTimeRenderOptionArgumentCaptor.capture()))
                .thenReturn(mockedBatchGet);
            when(mockedBatchGet.setMajorDimension(majorDimensionArgumentCaptor.capture()))
                .thenReturn(mockedBatchGet);
            when(mockedBatchGet.execute())
                .thenReturn(new BatchGetValuesResponse().setValueRanges(List.of(mockedValueRange)));

            List<String> values = List.of("value1", "value2", "value3");

            when(mockedValueRange.getValues())
                .thenReturn(List.of(List.of(values)));

            List<Object> result = GoogleSheetsRowUtils.getRow(mockedSheets, spreadSheetId, sheetName, rowNumber);

            assertEquals(List.of(values), result);
            assertEquals("UNFORMATTED_VALUE", valueRenderOptionArgumentCaptor.getValue());
            assertEquals("FORMATTED_STRING", dateTimeRenderOptionArgumentCaptor.getValue());
            assertEquals("ROWS", majorDimensionArgumentCaptor.getValue());

        }
    }
}

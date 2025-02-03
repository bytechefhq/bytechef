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

import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.HEADERS;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.SheetRecord;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.sheets.util.GoogleSheetsUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AddSheetResponse;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.Response;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;

/**
 * @author Monika Kušter
 */
class GoogleSheetsCreateSheetActionTest {

    private final ArgumentCaptor<BatchUpdateSpreadsheetRequest> batchUpdateSpreadsheetRequestArgumentCaptor =
        ArgumentCaptor.forClass(BatchUpdateSpreadsheetRequest.class);
    private final Sheets.Spreadsheets.BatchUpdate mockedBatchUpdate = mock(Sheets.Spreadsheets.BatchUpdate.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Sheets mockedSheets = mock(Sheets.class);
    private final Sheets.Spreadsheets mockedSpreadsheets = mock(Sheets.Spreadsheets.class);
    private final ArgumentCaptor<String> rangeArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> sheetNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> spreadsheetIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> valueInputOptionArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<ValueRange> valueRangeArgumentCaptor = ArgumentCaptor.forClass(ValueRange.class);

    @Test
    void perform() throws Exception {
        List<Object> headers = List.of("header1", "header2");
        Parameters parameters = MockParametersFactory.create(
            Map.of(SPREADSHEET_ID, "spreadsheetId", SHEET_ID, 123, SHEET_NAME, "sheetName", HEADERS, headers));

        BatchUpdateSpreadsheetResponse batchUpdateSpreadsheetResponse = new BatchUpdateSpreadsheetResponse()
            .setReplies(
                List.of(
                    new Response().setAddSheet(
                        new AddSheetResponse()
                            .setProperties(
                                new SheetProperties()
                                    .setTitle("sheetName")
                                    .setSheetId(123)))));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parameters))
                .thenReturn(mockedSheets);
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.createRange(sheetNameArgumentCaptor.capture(), any()))
                .thenReturn("range");
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.appendValues(any(Sheets.class), spreadsheetIdArgumentCaptor.capture(),
                    rangeArgumentCaptor.capture(), valueRangeArgumentCaptor.capture(),
                    valueInputOptionArgumentCaptor.capture()))
                .thenAnswer((Answer<Void>) invocation -> null);

            when(mockedSheets.spreadsheets())
                .thenReturn(mockedSpreadsheets);
            when(mockedSpreadsheets.batchUpdate(
                spreadsheetIdArgumentCaptor.capture(), batchUpdateSpreadsheetRequestArgumentCaptor.capture()))
                    .thenReturn(mockedBatchUpdate);
            when(mockedBatchUpdate.execute())
                .thenReturn(batchUpdateSpreadsheetResponse);

            SheetRecord expectedResponse = new SheetRecord("spreadsheetId", 123, "sheetName", headers);

            SheetRecord result = GoogleSheetsCreateSheetAction.perform(parameters, parameters, mockedContext);

            assertEquals(expectedResponse, result);
            assertEquals("sheetName", sheetNameArgumentCaptor.getValue());
            assertEquals("spreadsheetId", spreadsheetIdArgumentCaptor.getValue());
            assertEquals("USER_ENTERED", valueInputOptionArgumentCaptor.getValue());

            ValueRange valueRange = valueRangeArgumentCaptor.getValue();

            assertEquals("ROWS", valueRange.getMajorDimension());
            assertEquals(List.of(headers), valueRange.getValues());
        }
    }
}

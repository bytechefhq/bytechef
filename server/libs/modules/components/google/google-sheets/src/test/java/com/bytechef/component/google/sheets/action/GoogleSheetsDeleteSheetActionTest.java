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

import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.DeleteSheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
class GoogleSheetsDeleteSheetActionTest {

    private final ArgumentCaptor<BatchUpdateSpreadsheetRequest> batchUpdateSpreadsheetRequestArgumentCaptor =
        ArgumentCaptor.forClass(BatchUpdateSpreadsheetRequest.class);
    private final BatchUpdateSpreadsheetResponse mockedBatchUpdateSpreadsheetResponse =
        mock(BatchUpdateSpreadsheetResponse.class);
    private final Sheets.Spreadsheets.BatchUpdate mockedBatchUpdate = mock(Sheets.Spreadsheets.BatchUpdate.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Sheets mockedSheets = mock(Sheets.class);
    private final Sheets.Spreadsheets mockedSpreadsheets = mock(Sheets.Spreadsheets.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(SPREADSHEET_ID, "spreadsheetId", SHEET_ID, 123));
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void perform() throws Exception {
        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parametersArgumentCaptor.capture()))
                .thenReturn(mockedSheets);

            when(mockedSheets.spreadsheets())
                .thenReturn(mockedSpreadsheets);
            when(mockedSpreadsheets.batchUpdate(
                stringArgumentCaptor.capture(), batchUpdateSpreadsheetRequestArgumentCaptor.capture()))
                    .thenReturn(mockedBatchUpdate);
            when(mockedBatchUpdate.execute())
                .thenReturn(mockedBatchUpdateSpreadsheetResponse);

            Object result = GoogleSheetsDeleteSheetAction.perform(
                mockedParameters, mockedParameters, mockedActionContext);

            assertNull(result);
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals("spreadsheetId", stringArgumentCaptor.getValue());

            BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest()
                .setRequests(List.of(
                    new Request()
                        .setDeleteSheet(
                            new DeleteSheetRequest()
                                .setSheetId(123))));

            assertEquals(batchUpdateSpreadsheetRequest, batchUpdateSpreadsheetRequestArgumentCaptor.getValue());
        }
    }
}

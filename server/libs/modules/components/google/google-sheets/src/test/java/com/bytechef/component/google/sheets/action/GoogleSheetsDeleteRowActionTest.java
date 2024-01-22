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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.Request;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Domiter
 */
class GoogleSheetsDeleteRowActionTest extends AbstractGoogleSheetsActionTest {

    private final ArgumentCaptor<BatchUpdateSpreadsheetRequest> batchUpdateSpreadsheetRequestArgumentCaptor =
        ArgumentCaptor.forClass(BatchUpdateSpreadsheetRequest.class);
    private final BatchUpdateSpreadsheetResponse mockedBatchUpdateSpreadsheetResponse =
        mock(BatchUpdateSpreadsheetResponse.class);
    private final Sheets.Spreadsheets.BatchUpdate mockedBatchUpdate = mock(Sheets.Spreadsheets.BatchUpdate.class);
    private final Sheets.Spreadsheets mockedSpreadsheets = mock(Sheets.Spreadsheets.class);

    @Test
    void perform() throws IOException {

        when(mockedParameters.getRequiredInteger(ROW_NUMBER))
            .thenReturn(2);

        when(mockedSheets.spreadsheets())
            .thenReturn(mockedSpreadsheets);
        when(mockedSpreadsheets.batchUpdate(spreadsheetIdArgumentCaptor.capture(),
            batchUpdateSpreadsheetRequestArgumentCaptor.capture()))
            .thenReturn(mockedBatchUpdate);
        when(mockedBatchUpdate.execute())
            .thenReturn(mockedBatchUpdateSpreadsheetResponse);

        GoogleSheetsDeleteRowAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals("spreadsheetId", spreadsheetIdArgumentCaptor.getValue());

        BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest =
            batchUpdateSpreadsheetRequestArgumentCaptor.getValue();

        List<Request> requests = batchUpdateSpreadsheetRequest.getRequests();

        assertEquals(1, requests.size());

        Request request = requests.getFirst();

        DeleteDimensionRequest deleteDimensionRequest = request.getDeleteDimension();

        DimensionRange dimensionRange = deleteDimensionRequest.getRange();

        assertEquals(123, dimensionRange.getSheetId());
        assertEquals("ROWS", dimensionRange.getDimension());
        assertEquals(1, dimensionRange.getStartIndex());
        assertEquals(2, dimensionRange.getEndIndex());
    }
}

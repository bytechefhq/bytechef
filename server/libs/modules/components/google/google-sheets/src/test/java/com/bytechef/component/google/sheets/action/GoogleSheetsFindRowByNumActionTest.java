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
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.sheets.util.GoogleSheetsRowUtils;
import com.bytechef.component.google.sheets.util.GoogleSheetsUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleSheetsFindRowByNumActionTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Sheets mockedSheets = mock(Sheets.class);
    private final ArgumentCaptor<Integer> rowNumberArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    private final ArgumentCaptor<String> sheetNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> spreadsheetIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final Parameters parameters = MockParametersFactory.create(
        Map.of(SPREADSHEET_ID, "spreadsheetId", SHEET_NAME, "sheetName", ROW_NUMBER, 2));

    @Test
    void perform() throws Exception {
        Map<Object, Object> responseMap = Map.of();
        List<Object> row = List.of("1", 2, false);

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleSheetsRowUtils> googleSheetsRowUtilsMockedStatic =
                mockStatic(GoogleSheetsRowUtils.class);
            MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class)) {

            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parameters))
                .thenReturn(mockedSheets);
            googleSheetsRowUtilsMockedStatic
                .when(() -> GoogleSheetsRowUtils.getRowValues(
                    any(Sheets.class),
                    spreadsheetIdArgumentCaptor.capture(),
                    sheetNameArgumentCaptor.capture(),
                    rowNumberArgumentCaptor.capture()))
                .thenReturn(row);
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.getMapOfValuesForRow(parameters, mockedSheets, row))
                .thenReturn(responseMap);

            Map<String, Object> result = GoogleSheetsFindRowByNumAction.perform(
                parameters, parameters, mockedContext);

            assertEquals(responseMap, result);
            assertEquals(2, rowNumberArgumentCaptor.getValue());
            assertEquals("spreadsheetId", spreadsheetIdArgumentCaptor.getValue());
            assertEquals("sheetName", sheetNameArgumentCaptor.getValue());
        }
    }
}

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
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
class GoogleSheetsListSheetsActionTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Sheets mockedSheets = mock(Sheets.class);
    private final Parameters parameters = MockParametersFactory.create(
        Map.of(SPREADSHEET_ID, "spreadsheetId"));
    private final Spreadsheets mockedSpreadsheets = mock(Spreadsheets.class);
    private final Spreadsheets.Get mockedGet = mock(Spreadsheets.Get.class);
    private final Spreadsheet mockedSpreadsheet = mock(Spreadsheet.class);

    @Test
    void perform() throws Exception {
        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {

            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parameters))
                .thenReturn(mockedSheets);

            List<Sheet> expectedSheets = List.of(
                new Sheet().setProperties(
                    new SheetProperties()
                        .setTitle("sheetName1")
                        .setSheetId(1)),
                new Sheet().setProperties(
                    new SheetProperties()
                        .setTitle("sheetName2")
                        .setSheetId(2)),
                new Sheet().setProperties(
                    new SheetProperties()
                        .setTitle("sheetName3")
                        .setSheetId(3)));

            when(mockedSheets.spreadsheets())
                .thenReturn(mockedSpreadsheets);
            when(mockedSpreadsheets.get(parameters.getString(SPREADSHEET_ID)))
                .thenReturn(mockedGet);
            when(mockedGet.execute())
                .thenReturn(mockedSpreadsheet);
            when(mockedSpreadsheet.getSheets())
                .thenReturn(expectedSheets);

            List<Map<String, Object>> result = GoogleSheetsListSheetsAction.perform(
                parameters, parameters, mockedContext);

            assertEquals(3, result.size());

            assertEquals("spreadsheetId", result.getFirst()
                .get(SPREADSHEET_ID));
            assertEquals(1, result.getFirst()
                .get(SHEET_ID));
            assertEquals("sheetName1", result.getFirst()
                .get(SHEET_NAME));

            assertEquals("spreadsheetId", result.get(1)
                .get(SPREADSHEET_ID));
            assertEquals(2, result.get(1)
                .get(SHEET_ID));
            assertEquals("sheetName2", result.get(1)
                .get(SHEET_NAME));

            assertEquals("spreadsheetId", result.get(2)
                .get(SPREADSHEET_ID));
            assertEquals(3, result.get(2)
                .get(SHEET_ID));
            assertEquals("sheetName3", result.get(2)
                .get(SHEET_NAME));
        }
    }
}

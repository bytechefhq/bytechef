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

import static com.bytechef.component.google.sheets.action.GoogleSheetsCreateColumnAction.COLUMN_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.SheetRecord;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.sheets.util.GoogleSheetsRowUtils;
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
import org.mockito.stubbing.Answer;

/**
 * @author Monika Kušter
 */
class GoogleSheetsCreateColumnActionTest {

    private final ArgumentCaptor<Integer> integerArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(SPREADSHEET_ID, "spreadsheetId", SHEET_NAME, "name", COLUMN_NAME, "new column"));
    private final Sheets mockedSheets = mock(Sheets.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final ArgumentCaptor<Sheets> sheetsArgumentCaptor = ArgumentCaptor.forClass(Sheets.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<ValueRange> valueRangeArgumentCaptor = ArgumentCaptor.forClass(ValueRange.class);

    @Test
    void perform() throws Exception {
        List<Object> newHeaders = List.of("header1", "header2", "header3", "new column");

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class);
            MockedStatic<GoogleSheetsRowUtils> googleSheetsRowUtilsMockedStatic =
                mockStatic(GoogleSheetsRowUtils.class)) {

            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parametersArgumentCaptor.capture()))
                .thenReturn(mockedSheets);
            googleSheetsRowUtilsMockedStatic
                .when(() -> GoogleSheetsRowUtils.getRowValues(
                    sheetsArgumentCaptor.capture(), stringArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                    integerArgumentCaptor.capture()))
                .thenReturn(List.of("header1", "header2", "header3"), newHeaders);
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.appendValues(
                    sheetsArgumentCaptor.capture(), stringArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                    valueRangeArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenAnswer((Answer<Void>) invocation -> null);

            SheetRecord expectedResponse = new SheetRecord("spreadsheetId", null, "name", newHeaders);

            SheetRecord result = GoogleSheetsCreateColumnAction.perform(
                mockedParameters, mockedParameters, mockedActionContext);

            assertEquals(expectedResponse, result);
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(List.of(mockedSheets, mockedSheets, mockedSheets), sheetsArgumentCaptor.getAllValues());
            assertEquals(
                List.of("spreadsheetId", "name", "spreadsheetId", "name!D1", "USER_ENTERED", "spreadsheetId", "name"),
                stringArgumentCaptor.getAllValues());
            assertEquals(1, integerArgumentCaptor.getValue());

            ValueRange expectedValueRange = new ValueRange()
                .setValues(List.of(List.of("new column")))
                .setMajorDimension("COLUMNS");

            assertEquals(expectedValueRange, valueRangeArgumentCaptor.getValue());
        }
    }
}

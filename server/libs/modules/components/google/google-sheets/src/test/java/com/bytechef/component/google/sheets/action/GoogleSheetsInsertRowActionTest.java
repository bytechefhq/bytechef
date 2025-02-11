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

import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.VALUE_INPUT_OPTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
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
class GoogleSheetsInsertRowActionTest {

    private final ArgumentCaptor<Integer> integerArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Sheets mockedSheets = mock(Sheets.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(SPREADSHEET_ID, "spreadsheetId", SHEET_NAME, "sheetName", VALUE_INPUT_OPTION, "USER_ENTERED"));
    private final ArgumentCaptor<Sheets> sheetsArgumentCaptor = ArgumentCaptor.forClass(Sheets.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final ArgumentCaptor<ValueRange> valueRangeArgumentCaptor = ArgumentCaptor.forClass(ValueRange.class);

    @Test
    @SuppressWarnings("unchecked")
    void perform() throws Exception {
        Map<Object, Object> responseMap = Map.of();
        List<Object> row = List.of("abc", "sheetName", false);

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class)) {

            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parametersArgumentCaptor.capture()))
                .thenReturn(mockedSheets);
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.getRowValues(parametersArgumentCaptor.capture()))
                .thenReturn(row);
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.createRange(
                    stringArgumentCaptor.capture(), integerArgumentCaptor.capture()))
                .thenReturn("range");
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.appendValues(
                    sheetsArgumentCaptor.capture(), stringArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                    valueRangeArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenAnswer((Answer<Void>) invocation -> null);
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.getMapOfValuesForRow(
                    parametersArgumentCaptor.capture(), sheetsArgumentCaptor.capture(), listArgumentCaptor.capture()))
                .thenReturn(responseMap);

            Map<String, Object> result = GoogleSheetsInsertRowAction.perform(
                mockedParameters, mockedParameters, mockedActionContext);

            assertEquals(responseMap, result);

            assertEquals(
                List.of(mockedParameters, mockedParameters, mockedParameters),
                parametersArgumentCaptor.getAllValues());
            assertEquals(
                List.of("sheetName", "spreadsheetId", "range", "USER_ENTERED"),
                stringArgumentCaptor.getAllValues());
            assertNull(integerArgumentCaptor.getValue());
            assertEquals(List.of(mockedSheets, mockedSheets), sheetsArgumentCaptor.getAllValues());

            ValueRange valueRange = new ValueRange()
                .setValues(List.of(row))
                .setMajorDimension("ROWS");

            assertEquals(valueRange, valueRangeArgumentCaptor.getValue());
            assertEquals(row, listArgumentCaptor.getValue());
        }
    }
}

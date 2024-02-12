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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.google.sheets.util.GoogleSheetsRowUtils;
import com.bytechef.component.google.sheets.util.GoogleSheetsUtils;
import com.google.api.services.sheets.v4.Sheets;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
class GoogleSheetsFindRowByNumActionTest extends AbstractGoogleSheetsActionTest {

    private final ArgumentCaptor<Integer> rowNumberArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    private final Map<String, Object> mockedMap = mock(Map.class);
    private final ArgumentCaptor<Integer> sheetIdArgumentCaptor = ArgumentCaptor.forClass(Integer.class);

    @Test
    void perform() throws IOException {
        List<Object> row = List.of("1", 2, false);

        when(mockedParameters.getRequiredInteger(ROW_NUMBER))
            .thenReturn(2);

        try (MockedStatic<GoogleSheetsRowUtils> googleSheetsRowUtilsMockedStatic =
            mockStatic(GoogleSheetsRowUtils.class)) {
            googleSheetsRowUtilsMockedStatic
                .when(() -> GoogleSheetsRowUtils.getRow(
                    any(Sheets.class),
                    spreadsheetIdArgumentCaptor.capture(),
                    sheetIdArgumentCaptor.capture(),
                    rowNumberArgumentCaptor.capture()))
                .thenReturn(row);

            try (MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class)) {
                googleSheetsUtilsMockedStatic
                    .when(() -> GoogleSheetsUtils.getMapOfValuesForRow(mockedParameters, mockedSheets, row))
                    .thenReturn(mockedMap);

                Map<String, Object> result = GoogleSheetsFindRowByNumAction.perform(
                    mockedParameters, mockedParameters, mockedContext);

                assertEquals(mockedMap, result);
                assertEquals(2, rowNumberArgumentCaptor.getValue());
                assertEquals("spreadsheetId", spreadsheetIdArgumentCaptor.getValue());
                assertEquals(123, sheetIdArgumentCaptor.getValue());
            }
        }
    }
}

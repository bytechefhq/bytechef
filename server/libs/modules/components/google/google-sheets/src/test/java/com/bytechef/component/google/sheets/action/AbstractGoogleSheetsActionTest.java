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

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
public abstract class AbstractGoogleSheetsActionTest {

    protected MockedStatic<GoogleServices> googleServicesMockedStatic;
    protected ActionContext mockedContext = mock(ActionContext.class);
    protected Sheets mockedSheets = mock(Sheets.class);
    protected Parameters mockedParameters = mock(Parameters.class);
    protected ArgumentCaptor<String> spreadsheetIdArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @BeforeEach
    public void beforeEach() {
        googleServicesMockedStatic = mockStatic(GoogleServices.class);

        when(mockedParameters.getRequiredString(ACCESS_TOKEN))
            .thenReturn("accessToken");
        when(mockedParameters.getRequiredString(SPREADSHEET_ID))
            .thenReturn("spreadsheetId");
        when(mockedParameters.getRequiredInteger(SHEET_ID))
            .thenReturn(123);
        when(mockedParameters.getRequiredString(SHEET_NAME))
            .thenReturn("sheetName");

        googleServicesMockedStatic
            .when(() -> GoogleServices.getSheets(mockedParameters))
            .thenReturn(mockedSheets);
    }

    @AfterEach
    public void afterEach() {
        googleServicesMockedStatic.close();
    }
}

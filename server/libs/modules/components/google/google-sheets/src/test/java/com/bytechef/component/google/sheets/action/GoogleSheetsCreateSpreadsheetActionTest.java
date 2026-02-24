/*
 * Copyright 2025 ByteChef
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

import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.FOLDER_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.TITLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.Drive.Files.Get;
import com.google.api.services.drive.Drive.Files.Update;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets.Create;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Marija Horvat
 */
class GoogleSheetsCreateSpreadsheetActionTest {

    private final File file = new File().setParents(List.of("previous-folder-id"));
    private final Create mockCreateRequest = mock(Create.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Drive mockedDrive = mock(Drive.class);
    private final Files mockedFiles = mock(Files.class);
    private final Get mockedGet = mock(Get.class);
    private Parameters mockedParameters;
    private final Sheets mockedSheets = mock(Sheets.class);
    private final Spreadsheets mockedSpreadsheets = mock(Spreadsheets.class);
    private final Update mockedUpdate = mock(Update.class);
    private final Spreadsheet newSpreadSheet = new Spreadsheet().setSpreadsheetId("123");
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);
    private final ArgumentCaptor<Spreadsheet> spreadsheetArgumentCaptor = forClass(Spreadsheet.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void performWhenFolderIdIsNotSet() throws Exception {
        mockedParameters = MockParametersFactory.create(Map.of(TITLE, "spreadsheetName"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parametersArgumentCaptor.capture()))
                .thenReturn(mockedSheets);

            when(mockedSheets.spreadsheets())
                .thenReturn(mockedSpreadsheets);
            when(mockedSpreadsheets.create(spreadsheetArgumentCaptor.capture()))
                .thenReturn(mockCreateRequest);
            when(mockCreateRequest.execute())
                .thenReturn(newSpreadSheet);

            Object result = GoogleSheetsCreateSpreadsheetAction.perform(
                mockedParameters, mockedParameters, mockedActionContext);

            assertEquals(newSpreadSheet, result);
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());

            Spreadsheet expectedSpreadsheet = new Spreadsheet()
                .setProperties(
                    new SpreadsheetProperties()
                        .setTitle("spreadsheetName"));

            assertEquals(expectedSpreadsheet, spreadsheetArgumentCaptor.getValue());
        }
    }

    @Test
    void performWhenFolderIdIsSet() throws Exception {
        mockedParameters = MockParametersFactory.create(Map.of(TITLE, "spreadsheetName", FOLDER_ID, "folder_id"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parametersArgumentCaptor.capture()))
                .thenReturn(mockedSheets);

            when(mockedSheets.spreadsheets())
                .thenReturn(mockedSpreadsheets);
            when(mockedSpreadsheets.create(spreadsheetArgumentCaptor.capture()))
                .thenReturn(mockCreateRequest);
            when(mockCreateRequest.execute())
                .thenReturn(newSpreadSheet);

            googleServicesMockedStatic
                .when(() -> GoogleServices.getDrive(mockedParameters))
                .thenReturn(mockedDrive);

            when(mockedDrive.files())
                .thenReturn(mockedFiles);
            when(mockedFiles.get(stringArgumentCaptor.capture()))
                .thenReturn(mockedGet);
            when(mockedGet.setFields(stringArgumentCaptor.capture()))
                .thenReturn(mockedGet);
            when(mockedGet.execute())
                .thenReturn(file);

            when(mockedFiles.update(stringArgumentCaptor.capture(), any()))
                .thenReturn(mockedUpdate);
            when(mockedUpdate.setAddParents(stringArgumentCaptor.capture()))
                .thenReturn(mockedUpdate);
            when(mockedUpdate.setRemoveParents(stringArgumentCaptor.capture()))
                .thenReturn(mockedUpdate);
            when(mockedUpdate.setFields(stringArgumentCaptor.capture()))
                .thenReturn(mockedUpdate);
            when(mockedUpdate.execute())
                .thenReturn(file);

            Object result = GoogleSheetsCreateSpreadsheetAction.perform(
                mockedParameters, mockedParameters, mockedActionContext);

            assertEquals(newSpreadSheet, result);
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());

            Spreadsheet expectedSpreadsheet = new Spreadsheet()
                .setProperties(
                    new SpreadsheetProperties()
                        .setTitle("spreadsheetName"));

            assertEquals(expectedSpreadsheet, spreadsheetArgumentCaptor.getValue());

            assertEquals(
                List.of("123", "parents", "123", "folder_id", "previous-folder-id", "id, parents"),
                stringArgumentCaptor.getAllValues());
        }
    }
}

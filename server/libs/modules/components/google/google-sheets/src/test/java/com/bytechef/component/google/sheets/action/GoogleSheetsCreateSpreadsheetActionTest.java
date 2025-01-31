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

import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.FOLDER_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.TITLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Marija Horvat
 */
class GoogleSheetsCreateSpreadsheetActionTest {

    private final ArgumentCaptor<Spreadsheet> spreadsheetCaptor = ArgumentCaptor.forClass(Spreadsheet.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Sheets mockedSheets = mock(Sheets.class);
    private final Sheets.Spreadsheets mockedSpreadsheets = mock(Sheets.Spreadsheets.class);
    private final Sheets.Spreadsheets.Create mockCreateRequest = mock(Sheets.Spreadsheets.Create.class);
    private final Drive mockedDrive = mock(Drive.class);
    private final Drive.Files mockedDriveFiles = mock(Drive.Files.class);
    private final Drive.Files.Update mockDriveUpdateRequest = mock(Drive.Files.Update.class);
    private final Drive.Files.Get mockDriveGetRequest = mock(Drive.Files.Get.class);

    @Test
    void perform() throws Exception {
        Parameters parameters = MockParametersFactory.create(
            Map.of(TITLE, "spreadsheetName", FOLDER_ID, "folder_id"));

        Parameters connectionParameters = MockParametersFactory.create(Map.of());

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(connectionParameters))
                .thenReturn(mockedSheets);

            when(mockedSheets.spreadsheets())
                .thenReturn(mockedSpreadsheets);
            when(mockedSpreadsheets.create(any(Spreadsheet.class)))
                .thenReturn(mockCreateRequest);
            when(mockCreateRequest.execute())
                .thenReturn(new Spreadsheet().setSpreadsheetId(SPREADSHEET_ID));

            googleServicesMockedStatic
                .when(() -> GoogleServices.getDrive(connectionParameters))
                .thenReturn(mockedDrive);

            when(mockedDrive.files())
                .thenReturn(mockedDriveFiles);
            when(mockedDriveFiles.get(SPREADSHEET_ID))
                .thenReturn(mockDriveGetRequest);
            when(mockDriveGetRequest.setFields(anyString()))
                .thenReturn(mockDriveGetRequest);
            when(mockDriveGetRequest.execute())
                .thenReturn(new File().setParents(List.of("previous-folder-id")));

            when(mockedDriveFiles.update(anyString(), any()))
                .thenReturn(mockDriveUpdateRequest);
            when(mockDriveUpdateRequest.setAddParents(anyString()))
                .thenReturn(mockDriveUpdateRequest);
            when(mockDriveUpdateRequest.setRemoveParents(anyString()))
                .thenReturn(mockDriveUpdateRequest);
            when(mockDriveUpdateRequest.setFields(anyString()))
                .thenReturn(mockDriveUpdateRequest);
            when(mockDriveUpdateRequest.execute())
                .thenReturn(new File().setId(SPREADSHEET_ID));

            Object result =
                GoogleSheetsCreateSpreadsheetAction.perform(parameters, connectionParameters, mockedContext);

            assertEquals(new File().setId(SPREADSHEET_ID), result);

            verify(mockedSpreadsheets).create(spreadsheetCaptor.capture());
            assertEquals("spreadsheetName", spreadsheetCaptor.getValue()
                .getProperties()
                .getTitle());

            verify(mockDriveUpdateRequest).setAddParents("folder_id");
            verify(mockDriveUpdateRequest).setRemoveParents("previous-folder-id");

        }
    }
}

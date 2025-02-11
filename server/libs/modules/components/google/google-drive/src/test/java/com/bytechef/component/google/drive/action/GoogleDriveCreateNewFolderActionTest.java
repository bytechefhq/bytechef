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

package com.bytechef.component.google.drive.action;

import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.APPLICATION_VND_GOOGLE_APPS_FOLDER;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FOLDER_NAME;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FOLDER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
class GoogleDriveCreateNewFolderActionTest {

    private final ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);
    private final Drive.Files.Create mockedCreate = mock(Drive.Files.Create.class);
    private final Drive mockedDrive = mock(Drive.class);
    private final Drive.Files mockedFiles = mock(Drive.Files.class);
    private final File mockedGoogleFile = mock(File.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FOLDER_NAME, "folderName", FOLDER_ID, "parentFolder"));
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);

    @Test
    void testPerform() throws IOException {
        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getDrive(parametersArgumentCaptor.capture()))
                .thenReturn(mockedDrive);
            when(mockedDrive.files())
                .thenReturn(mockedFiles);
            when(mockedFiles.create(fileArgumentCaptor.capture()))
                .thenReturn(mockedCreate);
            when(mockedCreate.execute())
                .thenReturn(mockedGoogleFile);

            File result =
                GoogleDriveCreateNewFolderAction.perform(mockedParameters, mockedParameters, mock(ActionContext.class));

            assertEquals(mockedGoogleFile, result);
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());

            File expectedFile = new File()
                .setName("folderName")
                .setMimeType(APPLICATION_VND_GOOGLE_APPS_FOLDER)
                .setParents(List.of("parentFolder"));

            assertEquals(expectedFile, fileArgumentCaptor.getValue());
        }
    }
}

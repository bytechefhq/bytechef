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

package com.bytechef.component.google.drive.action;

import static com.bytechef.google.commons.constant.GoogleCommonsContants.FOLDER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Marija Horvat
 */
class GoogleDriveShareFolderActionTest {

    private final Drive.Files.Get mockedGet = mock(Drive.Files.Get.class);
    private final Drive mockedDrive = mock(Drive.class);
    private final Drive.Files mockedFiles = mock(Drive.Files.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(FOLDER_ID, "testId"));
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    protected ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    private final File testFile = new File()
        .setId("testFolderId")
        .setName("folderName")
        .setMimeType("application/vnd.google-apps.folder")
        .setKind("drive#file")
        .set("webViewLink", "https://drive.google.com/drive/folders/testFolderId");

    @Test
    void testPerform() throws IOException {
        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getDrive(parametersArgumentCaptor.capture()))
                .thenReturn(mockedDrive);
            when(mockedDrive.files())
                .thenReturn(mockedFiles);
            when(mockedFiles.get(stringArgumentCaptor.capture()))
                .thenReturn(mockedGet);
            when(mockedGet.setFields("webViewLink"))
                .thenReturn(mockedGet);
            when(mockedGet.execute())
                .thenReturn(testFile);

            String sharedLink = GoogleDriveShareFolderAction.perform(
                mockedParameters, mockedParameters, mock(ActionContext.class));

            verify(mockedDrive.files()).get("testId");
            verify(mockedGet).setFields("webViewLink");

            assertEquals("https://drive.google.com/drive/folders/testFolderId", sharedLink);
            assertEquals("testId", stringArgumentCaptor.getValue());
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
        }
    }
}

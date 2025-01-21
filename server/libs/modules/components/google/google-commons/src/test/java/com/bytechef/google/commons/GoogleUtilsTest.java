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

package com.bytechef.google.commons;

import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_ID;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_NAME;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FOLDER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleUtilsTest {

    private final ArgumentCaptor<String> fileIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final Drive.Files.Copy mockedCopy = mock(Drive.Files.Copy.class);
    private final Drive mockedDrive = mock(Drive.class);
    private final Drive.Files.Get mockedGet = mock(Drive.Files.Get.class);
    private final Drive.Files mockedFiles = mock(Drive.Files.class);

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FILE_ID, "originalFileId", FILE_NAME, "newFileName", FOLDER_ID, "newFolderId"));
    private final File testFile = new File()
        .setName("newFileName")
        .setParents(Collections.singletonList("newFolderId"))
        .setMimeType("application/pdf");

    @Test
    void test() throws IOException {
        when(mockedDrive.files())
            .thenReturn(mockedFiles);
        when(mockedFiles.get(fileIdArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.execute())
            .thenReturn(testFile);
        when(mockedFiles.copy("originalFileId", testFile))
            .thenReturn(mockedCopy);
        when(mockedCopy.execute())
            .thenReturn(testFile);

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getDrive(mockedParameters))
                .thenReturn(mockedDrive);

            File resultFile = GoogleUtils.copyFileOnGoogleDrive(mockedParameters, mockedParameters);

            verify(mockedDrive.files()).get("originalFileId");
            verify(mockedFiles).copy("originalFileId", testFile);

            assertEquals("newFileName", resultFile.getName());
            assertEquals(Collections.singletonList("newFolderId"), resultFile.getParents());
            assertEquals("application/pdf", resultFile.getMimeType());
        }
    }
}

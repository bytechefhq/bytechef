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

import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FILE_ID;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FILE_NAME;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.PARENT_FOLDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.google.api.services.drive.model.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Mayank Madan
 */
public class GoogleDriveCopyFileActionTest extends AbstractGoogleDriveActionTest {
    private final Parameters mockInputParameters = MockParametersFactory.create(Map.of(FILE_ID, "originalFileId",
        FILE_NAME, "newFileName", PARENT_FOLDER, "newFolderId"));
    private final File testFile =
        new File().setName("newFileName")
            .setParents(Collections.singletonList("newFolderId"))
            .setMimeType("application" +
                "/pdf");

    @Test
    public void testPerform() throws IOException {

        when(mockedFiles.get(fileIdArgumentCaptor.capture())).thenReturn(mockedGet);
        when(mockedGet.execute()).thenReturn(testFile);
        when(mockedFiles.copy("originalFileId", testFile)).thenReturn(mockedCopy);
        when(mockedCopy.execute()).thenReturn(testFile);

        File copiedFile = GoogleDriveCopyFileAction.perform(mockInputParameters, mockInputParameters, mockedContext);

        verify(mockedDrive.files()).get("originalFileId");
        verify(mockedFiles).copy("originalFileId", testFile);

        assertEquals("newFileName", copiedFile.getName());
        assertEquals(Collections.singletonList("newFolderId"), copiedFile.getParents());
        assertEquals("application/pdf", copiedFile.getMimeType());
    }
}

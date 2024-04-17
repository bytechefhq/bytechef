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

import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FILE_ENTRY;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.PARENT_FOLDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.FileEntry;
import com.google.api.services.drive.model.File;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Mario Cvjetojevic
 * @author Monika Domiter
 */
class GoogleDriveUploadFileActionTest extends AbstractGoogleDriveActionTest {

    private final java.io.File mockedFile = mock(java.io.File.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);

    @Test
    void testPerform() throws IOException {
       when(mockedParameters.getRequiredFileEntry(FILE_ENTRY))
            .thenReturn(mockedFileEntry);
        when(mockedParameters.getString(PARENT_FOLDER))
            .thenReturn("parentFolder");
        when(mockedFileEntry.getName())
            .thenReturn("name");
        when(mockedFileEntry.getMimeType())
            .thenReturn("mimeType");
        when(mockedContext.file(any()))
            .thenReturn(mockedFile);

        when(mockedFiles.create(fileArgumentCaptor.capture(), abstractInputStreamContentArgumentCaptor.capture()))
            .thenReturn(mockedCreate);
        when(mockedCreate.execute())
            .thenReturn(mockedGoogleFile);

        File result = GoogleDriveUploadFileAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedGoogleFile, result);

        File file = fileArgumentCaptor.getValue();

        assertEquals("name", file.getName());
        assertEquals(List.of("parentFolder"), file.getParents());
    }
}

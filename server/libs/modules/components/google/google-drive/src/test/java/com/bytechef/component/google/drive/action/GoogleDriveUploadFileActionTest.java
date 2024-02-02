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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.FileEntry;
import java.io.File;
import org.junit.jupiter.api.Test;

/**
 * @author Mario Cvjetojevic
 */
public class GoogleDriveUploadFileActionTest extends AbstractGoogleDriveCreateActionTest {

    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final File mockedFile = mock(File.class);

    @Test
    public void testPerform() throws Exception {
        when(mockedParameters.getRequiredFileEntry(FILE_ENTRY))
            .thenReturn(mockedFileEntry);
        when(mockedFileEntry.getName())
            .thenReturn("fileName");
        when(mockedFileEntry.getMimeType())
            .thenReturn("mimeType");

        when(mockedContext.file(any()))
            .thenReturn(mockedFile);

        GoogleDriveUploadFileAction.perform(mockedParameters, mockedParameters, mockedContext);

        verify(mockedFiles, times(1))
            .create(fileArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());

        assertEquals("fileName", fileArgumentCaptor.getValue().getName());
        assertEquals("mimeType", inputStreamArgumentCaptor.getValue().getType());
    }
}

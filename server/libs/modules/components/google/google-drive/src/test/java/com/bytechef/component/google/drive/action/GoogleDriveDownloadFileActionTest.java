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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
class GoogleDriveDownloadFileActionTest extends AbstractGoogleDriveActionTest {

    private final InputStream mockedInputStream = mock(InputStream.class);
    private final Drive.Files.List mockedList = mock(Drive.Files.List.class);
    private final FileList mockedFileList = mock(FileList.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(FILE_ID, "fileId"));
    private final ArgumentCaptor<String> qArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() throws IOException {
        when(mockedFiles.get(fileIdArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.executeMediaAsInputStream())
            .thenReturn(mockedInputStream);

        when(mockedFiles.list())
            .thenReturn(mockedList);
        when(mockedList.setQ(qArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.execute())
            .thenReturn(mockedFileList);
        when(mockedFileList.getFiles())
            .thenReturn(List.of(new File().setId("fileId")
                .setName("fileName")));

        when(mockedContext.file(any()))
            .thenReturn(mockedFileEntry);

        FileEntry result = GoogleDriveDownloadFileAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedFileEntry, result);

        assertEquals("fileId", fileIdArgumentCaptor.getValue());
        assertEquals("mimeType != 'application/vnd.google-apps.folder'", qArgumentCaptor.getValue());
    }
}

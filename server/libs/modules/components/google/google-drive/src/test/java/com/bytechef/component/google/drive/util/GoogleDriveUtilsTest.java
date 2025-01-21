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

package com.bytechef.component.google.drive.util;

import static com.bytechef.component.google.drive.util.GoogleDriveUtils.LAST_TIME_CHECKED;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FOLDER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Mario Cvjetojevic
 * @author Ivica Cardic
 * @author Monika Kušter
 */
class GoogleDriveUtilsTest {

    private final List<File> files = new ArrayList<>();
    private MockedStatic<GoogleServices> googleServicesMockedStatic;
    private final Drive mockedDrive = mock(Drive.class);
    private final FileList mockedFileList = mock(FileList.class);
    private final Drive.Files mockedFiles = mock(Drive.Files.class);
    private final Drive.Files.List mockedList = mock(Drive.Files.List.class);
    private final Parameters mockedParameters =
        MockParametersFactory
            .create(Map.of(FOLDER_ID, "parent", LAST_TIME_CHECKED, LocalDateTime.of(2000, 1, 1, 1, 1, 1)));
    private final ArgumentCaptor<String> qArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> fieldsArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> orderByArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @BeforeEach
    void beforeEach() throws IOException {
        googleServicesMockedStatic = mockStatic(GoogleServices.class);

        googleServicesMockedStatic
            .when(() -> GoogleServices.getDrive(any(Parameters.class)))
            .thenReturn(mockedDrive);

        files.add(
            new File()
                .setName("name")
                .setId("id"));

        when(mockedDrive.files())
            .thenReturn(mockedFiles);
        when(mockedFiles.list())
            .thenReturn(mockedList);
        when(mockedList.setQ(qArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.execute())
            .thenReturn(mockedFileList);
        when(mockedFileList.getFiles())
            .thenReturn(files);
    }

    @AfterEach
    void afterEach() {
        googleServicesMockedStatic.close();
    }

    @Test
    void testGetPollOutput() {
        when(mockedList.setFields(fieldsArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setOrderBy(orderByArgumentCaptor.capture()))
            .thenReturn(mockedList);

        PollOutput pollOutput =
            GoogleDriveUtils.getPollOutput(mockedParameters, mockedParameters, mockedParameters, false);

        assertEquals(files, pollOutput.records());
        assertFalse(pollOutput.pollImmediately());

        assertEquals(
            "mimeType = 'application/vnd.google-apps.folder' and 'parent' in parents and trashed = false and createdTime > '2000-01-01T01:01:01'",
            qArgumentCaptor.getValue());
        assertEquals("files(id, name, mimeType, webViewLink, kind)", fieldsArgumentCaptor.getValue());
        assertEquals("createdTime asc", orderByArgumentCaptor.getValue());
    }
}

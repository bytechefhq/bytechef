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

package com.bytechef.component.google.drive.util;

import static com.bytechef.component.google.drive.util.GoogleDriveUtils.LAST_TIME_CHECKED;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FOLDER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.Drive.Files.Get;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final ArgumentCaptor<Integer> integerArgumentCaptor = forClass(Integer.class);
    private final Drive mockedDrive = mock(Drive.class);
    private final Files mockedFiles = mock(Files.class);
    private final Get mockedGet = mock(Get.class);
    private final Files.List mockedList = mock(Files.List.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FOLDER_ID, "parent", LAST_TIME_CHECKED, Instant.parse("2000-01-01T01:01:01Z")));
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);

    @Test
    void testGetFileWebViewLink() throws IOException {
        File testFile = new File()
            .setId("testFolderId")
            .setName("folderName")
            .setMimeType("application/vnd.google-apps.folder")
            .setKind("drive#file")
            .setWebViewLink("https://drive.google.com/drive/folders/testFolderId");

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getDrive(parametersArgumentCaptor.capture()))
                .thenReturn(mockedDrive);
            when(mockedDrive.files())
                .thenReturn(mockedFiles);
            when(mockedFiles.get(stringArgumentCaptor.capture()))
                .thenReturn(mockedGet);
            when(mockedGet.setFields(stringArgumentCaptor.capture()))
                .thenReturn(mockedGet);
            when(mockedGet.execute())
                .thenReturn(testFile);

            String sharedLink = GoogleDriveUtils.getFileWebViewLink(mockedParameters, "testId");

            assertEquals("https://drive.google.com/drive/folders/testFolderId", sharedLink);
            assertEquals(List.of("testId", "webViewLink"), stringArgumentCaptor.getAllValues());
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
        }
    }

    @Test
    void testGetPollOutput() throws IOException {
        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getDrive(parametersArgumentCaptor.capture()))
                .thenReturn(mockedDrive);
            when(mockedDrive.files())
                .thenReturn(mockedFiles);
            when(mockedFiles.list())
                .thenReturn(mockedList);
            when(mockedList.setQ(stringArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.setFields(stringArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.setOrderBy(stringArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.setPageSize(integerArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.setPageToken(stringArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.execute())
                .thenReturn(new FileList().setFiles(files));

            PollOutput pollOutput = GoogleDriveUtils.getPollOutput(
                mockedParameters, mockedParameters, mockedParameters, mockedTriggerContext, false);

            assertEquals(files, pollOutput.records());
            assertFalse(pollOutput.pollImmediately());

            List<String> strings = new ArrayList<>();

            strings.add(
                "mimeType = 'application/vnd.google-apps.folder' and 'parent' in parents and trashed = false and createdTime > '2000-01-01T01:01:01Z'");
            strings.add("files(id, name, mimeType, webViewLink, kind)");
            strings.add("createdTime desc");
            strings.add(null);

            assertEquals(strings, stringArgumentCaptor.getAllValues());
            assertEquals(1000, integerArgumentCaptor.getValue());
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
        }
    }

    @Test
    void testGetPollOutputWithLastTimeCheckedAsStringFractionalNanos() throws IOException {
        Parameters closureParameters = MockParametersFactory.create(
            Map.of(LAST_TIME_CHECKED, "2025-12-05T16:55:49.682110827"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getDrive(parametersArgumentCaptor.capture()))
                .thenReturn(mockedDrive);
            when(mockedDrive.files()).thenReturn(mockedFiles);
            when(mockedFiles.list()).thenReturn(mockedList);
            when(mockedList.setQ(stringArgumentCaptor.capture())).thenReturn(mockedList);
            when(mockedList.setFields(stringArgumentCaptor.capture())).thenReturn(mockedList);
            when(mockedList.setOrderBy(stringArgumentCaptor.capture())).thenReturn(mockedList);
            when(mockedList.setPageSize(integerArgumentCaptor.capture())).thenReturn(mockedList);
            when(mockedList.setPageToken(stringArgumentCaptor.capture())).thenReturn(mockedList);
            when(mockedList.execute()).thenReturn(new FileList().setFiles(files));

            PollOutput pollOutput = GoogleDriveUtils.getPollOutput(
                mockedParameters, mockedParameters, closureParameters, mockedTriggerContext, false);

            assertEquals(files, pollOutput.records());
            assertFalse(pollOutput.pollImmediately());

            List<String> strings = new ArrayList<>();

            strings.add(
                "mimeType = 'application/vnd.google-apps.folder' and 'parent' in parents and trashed = false and " +
                    "createdTime > '2025-12-05T16:55:49.682110827Z'");
            strings.add("files(id, name, mimeType, webViewLink, kind)");
            strings.add("createdTime desc");
            strings.add(null);

            assertEquals(strings, stringArgumentCaptor.getAllValues());
            assertEquals(1000, integerArgumentCaptor.getValue());
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
        }
    }

    @Test
    void testListFiles() throws IOException {
        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getDrive(parametersArgumentCaptor.capture()))
                .thenReturn(mockedDrive);

            when(mockedDrive.files())
                .thenReturn(mockedFiles);
            when(mockedFiles.list())
                .thenReturn(mockedList);
            when(mockedList.setQ(stringArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.setPageSize(integerArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.setPageToken(stringArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.execute())
                .thenReturn(new FileList().setFiles(files));

            List<File> result = GoogleDriveUtils.listFiles("folderId", true, mockedParameters);

            assertEquals(files, result);

            List<String> strings = new ArrayList<>();

            strings
                .add("mimeType = 'application/vnd.google-apps.folder' and trashed = false and parents in 'folderId'");
            strings.add(null);

            assertEquals(strings, stringArgumentCaptor.getAllValues());
            assertEquals(1000, integerArgumentCaptor.getValue());
        }
    }
}

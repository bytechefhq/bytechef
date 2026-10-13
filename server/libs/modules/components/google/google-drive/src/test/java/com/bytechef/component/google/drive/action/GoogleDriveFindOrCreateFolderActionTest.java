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

import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.APPLICATION_VND_GOOGLE_APPS_FOLDER;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FOLDER_NAME;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FOLDER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.Drive.Files.Create;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Igor Beslic
 */
class GoogleDriveFindOrCreateFolderActionTest {

    private final ArgumentCaptor<File> fileArgumentCaptor = forClass(File.class);
    private MockedStatic<GoogleServices> googleServicesMockedStatic;
    private final Create mockedCreate = mock(Create.class);
    private final Drive mockedDrive = mock(Drive.class);
    private final Files mockedFiles = mock(Files.class);
    private final Files.List mockedList = mock(Files.List.class);
    private final ArgumentCaptor<String> queryArgumentCaptor = forClass(String.class);

    @BeforeEach
    void beforeEach() throws IOException {
        googleServicesMockedStatic = mockStatic(GoogleServices.class);

        googleServicesMockedStatic
            .when(() -> GoogleServices.getDrive(any(Parameters.class)))
            .thenReturn(mockedDrive);

        when(mockedDrive.files())
            .thenReturn(mockedFiles);
        when(mockedFiles.list())
            .thenReturn(mockedList);
        when(mockedList.setQ(queryArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setFields(anyString()))
            .thenReturn(mockedList);
        when(mockedList.setOrderBy(anyString()))
            .thenReturn(mockedList);
        when(mockedList.setPageSize(anyInt()))
            .thenReturn(mockedList);
        when(mockedFiles.create(fileArgumentCaptor.capture()))
            .thenReturn(mockedCreate);
    }

    @AfterEach
    void afterEach() {
        googleServicesMockedStatic.close();
    }

    @Test
    void testPerformReturnsExistingFolder() throws IOException {
        File existingFolder = new File()
            .setId("existingFolderId")
            .setName("folderName");
        File newerFolder = new File()
            .setId("newerFolderId")
            .setName("folderName");

        when(mockedList.execute())
            .thenReturn(new FileList().setFiles(List.of(existingFolder, newerFolder)));

        Parameters parameters = MockParametersFactory.create(
            Map.of(FOLDER_NAME, "folderName", FOLDER_ID, "parentFolder"));

        File result = GoogleDriveFindOrCreateFolderAction.perform(parameters, parameters, mock(ActionContext.class));

        assertEquals(existingFolder, result);
        assertEquals(
            "mimeType = 'application/vnd.google-apps.folder' and name = 'folderName' and 'parentFolder' in parents " +
                "and trashed = false",
            queryArgumentCaptor.getValue());

        verify(mockedList).setOrderBy("createdTime");
        verify(mockedFiles, never()).create(any(File.class));
    }

    @Test
    void testPerformCreatesFolderWhenMissing() throws IOException {
        File createdFolder = new File()
            .setId("createdFolderId")
            .setName("folderName");

        when(mockedList.execute())
            .thenReturn(new FileList().setFiles(List.of()));
        when(mockedCreate.execute())
            .thenReturn(createdFolder);

        Parameters parameters = MockParametersFactory.create(
            Map.of(FOLDER_NAME, "folderName", FOLDER_ID, "parentFolder"));

        File result = GoogleDriveFindOrCreateFolderAction.perform(parameters, parameters, mock(ActionContext.class));

        assertEquals(createdFolder, result);

        File expectedFile = new File()
            .setName("folderName")
            .setMimeType(APPLICATION_VND_GOOGLE_APPS_FOLDER)
            .setParents(List.of("parentFolder"));

        assertEquals(expectedFile, fileArgumentCaptor.getValue());
    }

    @Test
    void testPerformUsesRootFolderWhenParentFolderNotSet() throws IOException {
        File createdFolder = new File()
            .setId("createdFolderId")
            .setName("folderName");

        when(mockedList.execute())
            .thenReturn(new FileList());
        when(mockedCreate.execute())
            .thenReturn(createdFolder);

        Parameters parameters = MockParametersFactory.create(Map.of(FOLDER_NAME, "folderName", FOLDER_ID, " "));

        File result = GoogleDriveFindOrCreateFolderAction.perform(parameters, parameters, mock(ActionContext.class));

        assertEquals(createdFolder, result);
        assertEquals(
            "mimeType = 'application/vnd.google-apps.folder' and name = 'folderName' and 'root' in parents " +
                "and trashed = false",
            queryArgumentCaptor.getValue());

        File expectedFile = new File()
            .setName("folderName")
            .setMimeType(APPLICATION_VND_GOOGLE_APPS_FOLDER);

        assertEquals(expectedFile, fileArgumentCaptor.getValue());
    }

    @Test
    void testPerformEscapesFolderNameInQuery() throws IOException {
        File existingFolder = new File()
            .setId("existingFolderId")
            .setName("Igor's \\ folder");

        when(mockedList.execute())
            .thenReturn(new FileList().setFiles(List.of(existingFolder)));

        Parameters parameters = MockParametersFactory.create(Map.of(FOLDER_NAME, "Igor's \\ folder"));

        File result = GoogleDriveFindOrCreateFolderAction.perform(parameters, parameters, mock(ActionContext.class));

        assertEquals(existingFolder, result);
        assertEquals(
            "mimeType = 'application/vnd.google-apps.folder' and name = 'Igor\\'s \\\\ folder' and 'root' in " +
                "parents and trashed = false",
            queryArgumentCaptor.getValue());
    }

    @Test
    void testPerformTranslatesIOException() throws IOException {
        when(mockedList.execute())
            .thenThrow(new IOException("connection reset"));

        Parameters parameters = MockParametersFactory.create(Map.of(FOLDER_NAME, "folderName"));

        assertThrows(
            ProviderException.class,
            () -> GoogleDriveFindOrCreateFolderAction.perform(parameters, parameters, mock(ActionContext.class)));

        verify(mockedFiles, never()).create(any(File.class));
    }
}

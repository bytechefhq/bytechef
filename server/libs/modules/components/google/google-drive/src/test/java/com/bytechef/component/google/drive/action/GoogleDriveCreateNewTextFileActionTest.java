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
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.AUTO_CREATE_FOLDER;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FOLDER_NAME;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.MIME_TYPE;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.TEXT;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_NAME;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FOLDER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.Drive.Files.Create;
import com.google.api.services.drive.model.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Mario Cvjetojevic
 * @author Monika KUšter
 */
class GoogleDriveCreateNewTextFileActionTest {

    private static final String CREATED_FOLDER_ID = "createdFolder";
    private static final String CREATED_FOLDER_NAME = "Reports";
    private static final String EXISTING_FOLDER_ID = "existingFolder";
    private static final String MISSING_FOLDER_ID = "missingFolder";
    private static final String REPLACEMENT_FOLDER_NAME = "Invoices";

    private final ArgumentCaptor<AbstractInputStreamContent> abstractInputStreamContentArgumentCaptor = forClass(
        AbstractInputStreamContent.class);
    private final ArgumentCaptor<File> fileArgumentCaptor = forClass(File.class);
    private final ArgumentCaptor<File> folderArgumentCaptor = forClass(File.class);
    private final ActionContext actionContext = mock(ActionContext.class);
    private final Create mockedCreate = mock(Create.class);
    private final Drive mockedDrive = mock(Drive.class);
    private final Files mockedFiles = mock(Files.class);
    private final File mockedGoogleFile = mock(File.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);

    @BeforeAll
    static void loadActionDefinitions() {
        String textFileActionName = GoogleDriveCreateNewTextFileAction.ACTION_DEFINITION.getName();
        String folderActionName = GoogleDriveCreateNewFolderAction.ACTION_DEFINITION.getName();

        assertEquals("createNewTextFile", textFileActionName);
        assertEquals("createNewFolder", folderActionName);
    }

    @Test
    void testFolderNameProperty() {
        List<? extends Property> properties = GoogleDriveCreateNewTextFileAction.ACTION_DEFINITION.getProperties()
            .orElseThrow();

        Property.StringProperty autoCreateFolderProperty = findStringProperty(properties, AUTO_CREATE_FOLDER);
        List<? extends Option<String>> autoCreateFolderOptions = autoCreateFolderProperty.getOptions()
            .orElseThrow();

        assertEquals(Property.ControlType.SELECT, autoCreateFolderProperty.getControlType());
        assertEquals(Optional.of("no"), autoCreateFolderProperty.getDefaultValue());
        assertEquals(Optional.of(false), autoCreateFolderProperty.getRequired());
        assertEquals(2, autoCreateFolderOptions.size());
        assertEquals("Yes", autoCreateFolderOptions.get(0)
            .getLabel());
        assertEquals("yes", autoCreateFolderOptions.get(0)
            .getValue());
        assertEquals("No", autoCreateFolderOptions.get(1)
            .getLabel());
        assertEquals("no", autoCreateFolderOptions.get(1)
            .getValue());

        Property.StringProperty folderNameProperty = findStringProperty(properties, FOLDER_NAME);

        assertEquals(Property.ControlType.TEXT, folderNameProperty.getControlType());
        assertEquals(Optional.of("Folder Name"), folderNameProperty.getLabel());
        assertEquals(Optional.of("%s == '%s'".formatted(AUTO_CREATE_FOLDER, "yes")),
            folderNameProperty.getDisplayCondition());
        assertEquals(Optional.of(true), folderNameProperty.getRequired());
        assertEquals(Optional.empty(), folderNameProperty.getDefaultValue());
    }

    @Test
    void testPerformNoFolderId() throws IOException {
        Parameters parameters = MockParametersFactory.create(
            Map.of(FILE_NAME, "fileName", MIME_TYPE, "mimeType", TEXT, "text"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleUtils> googleUtilsMockedStatic = mockStatic(GoogleUtils.class)) {

            stubTextFileCreate(googleServicesMockedStatic);

            File result = GoogleDriveCreateNewTextFileAction.perform(parameters, parameters, actionContext);

            assertCreatedTextFile(result, parameters, null);

            googleUtilsMockedStatic.verify(
                () -> GoogleUtils.checkIfFolderExists(any(), nullable(String.class)), never());
            verify(mockedFiles, never())
                .create(any(File.class));
        }
    }

    @Test
    void testPerformNonExistentFolderId() throws IOException {
        Parameters parameters = MockParametersFactory.create(
            Map.of(
                AUTO_CREATE_FOLDER, "no", FILE_NAME, "fileName", FOLDER_ID, MISSING_FOLDER_ID, MIME_TYPE,
                "mimeType",
                TEXT, "text"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleUtils> googleUtilsMockedStatic = mockStatic(GoogleUtils.class)) {

            stubTextFileCreate(googleServicesMockedStatic);
            googleUtilsMockedStatic
                .when(() -> GoogleUtils.checkIfFolderExists(parameters, MISSING_FOLDER_ID))
                .thenReturn(false);

            IllegalArgumentException illegalArgumentException = assertThrows(
                IllegalArgumentException.class,
                () -> GoogleDriveCreateNewTextFileAction.perform(parameters, parameters, actionContext));

            String expectedMessage = "The specified folder " + MISSING_FOLDER_ID + " does not exist.";

            assertEquals(expectedMessage, illegalArgumentException.getMessage());
            assertEquals(parameters, parametersArgumentCaptor.getValue());

            verify(mockedFiles, never())
                .create(any(File.class));
            verify(mockedFiles, never())
                .create(any(File.class), any(AbstractInputStreamContent.class));
        }
    }

    @Test
    void testPerformExistingFolderIdWithAutoCreateFolder() throws IOException {
        Parameters parameters = MockParametersFactory.create(
            Map.of(
                AUTO_CREATE_FOLDER, "yes", FILE_NAME, "fileName", FOLDER_ID, EXISTING_FOLDER_ID, FOLDER_NAME,
                CREATED_FOLDER_NAME, MIME_TYPE, "mimeType", TEXT, "text"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleUtils> googleUtilsMockedStatic = mockStatic(GoogleUtils.class)) {

            stubTextFileCreate(googleServicesMockedStatic);
            googleUtilsMockedStatic
                .when(() -> GoogleUtils.checkIfFolderExists(parameters, EXISTING_FOLDER_ID))
                .thenReturn(true);

            File result = GoogleDriveCreateNewTextFileAction.perform(parameters, parameters, actionContext);

            assertCreatedTextFile(result, parameters, List.of(EXISTING_FOLDER_ID));

            verify(mockedFiles, never())
                .create(any(File.class));
        }
    }

    @Test
    void testPerformNoFolderIdWithAutoCreateFolder() throws IOException {
        Parameters parameters = MockParametersFactory.create(
            Map.of(
                AUTO_CREATE_FOLDER, "yes", FILE_NAME, "fileName", FOLDER_NAME, CREATED_FOLDER_NAME, MIME_TYPE,
                "mimeType", TEXT, "text"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleUtils> googleUtilsMockedStatic = mockStatic(GoogleUtils.class)) {

            stubTextFileCreate(googleServicesMockedStatic);
            stubFolderCreate();

            File result = GoogleDriveCreateNewTextFileAction.perform(parameters, parameters, actionContext);

            assertCreatedTextFile(result, parameters, List.of(CREATED_FOLDER_ID));
            assertCreatedFolder(CREATED_FOLDER_NAME);

            googleUtilsMockedStatic.verify(
                () -> GoogleUtils.checkIfFolderExists(any(), nullable(String.class)), never());
        }
    }

    @Test
    void testPerformNonExistentFolderIdWithAutoCreateFolder() throws IOException {
        Parameters parameters = MockParametersFactory.create(
            Map.of(
                AUTO_CREATE_FOLDER, "yes", FILE_NAME, "fileName", FOLDER_ID, MISSING_FOLDER_ID, FOLDER_NAME,
                REPLACEMENT_FOLDER_NAME, MIME_TYPE, "mimeType", TEXT, "text"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleUtils> googleUtilsMockedStatic = mockStatic(GoogleUtils.class)) {

            stubTextFileCreate(googleServicesMockedStatic);
            stubFolderCreate();
            googleUtilsMockedStatic
                .when(() -> GoogleUtils.checkIfFolderExists(parameters, MISSING_FOLDER_ID))
                .thenReturn(false);

            File result = GoogleDriveCreateNewTextFileAction.perform(parameters, parameters, actionContext);

            assertCreatedTextFile(result, parameters, List.of(CREATED_FOLDER_ID));
            assertCreatedFolder(REPLACEMENT_FOLDER_NAME);
        }
    }

    @Test
    void testPerformNonExistentFolderIdWithAutoCreateFolderWithoutFolderName() throws IOException {
        Parameters parameters = MockParametersFactory.create(
            Map.of(
                AUTO_CREATE_FOLDER, "yes", FILE_NAME, "fileName", FOLDER_ID, MISSING_FOLDER_ID, MIME_TYPE,
                "mimeType", TEXT, "text"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleUtils> googleUtilsMockedStatic = mockStatic(GoogleUtils.class)) {

            stubTextFileCreate(googleServicesMockedStatic);
            googleUtilsMockedStatic
                .when(() -> GoogleUtils.checkIfFolderExists(parameters, MISSING_FOLDER_ID))
                .thenReturn(false);

            NullPointerException nullPointerException = assertThrows(
                NullPointerException.class,
                () -> GoogleDriveCreateNewTextFileAction.perform(parameters, parameters, actionContext));

            assertEquals("Unknown value for : " + FOLDER_NAME, nullPointerException.getMessage());

            verify(mockedFiles, never())
                .create(any(File.class));
            verify(mockedFiles, never())
                .create(any(File.class), any(AbstractInputStreamContent.class));
        }
    }

    private void assertCreatedFolder(String folderName) {
        File expectedFolder = new File()
            .setName(folderName)
            .setMimeType(APPLICATION_VND_GOOGLE_APPS_FOLDER);

        assertEquals(expectedFolder, folderArgumentCaptor.getValue());
    }

    private Property.StringProperty findStringProperty(List<? extends Property> properties, String propertyName) {
        Property property = properties.stream()
            .filter(currentProperty -> propertyName.equals(currentProperty.getName()))
            .findFirst()
            .orElseThrow();

        assertInstanceOf(Property.StringProperty.class, property);

        return (Property.StringProperty) property;
    }

    private void assertCreatedTextFile(File result, Parameters parameters, List<String> parentFolderIds)
        throws IOException {

        assertEquals(mockedGoogleFile, result);
        assertEquals(parameters, parametersArgumentCaptor.getValue());

        File expectedFile = new File()
            .setName("fileName")
            .setParents(parentFolderIds);

        assertEquals(expectedFile, fileArgumentCaptor.getValue());

        AbstractInputStreamContent abstractInputStreamContent = abstractInputStreamContentArgumentCaptor.getValue();

        assertEquals("mimeType", abstractInputStreamContent.getType());

        try (InputStreamReader inputStreamReader = new InputStreamReader(
            abstractInputStreamContent.getInputStream(), StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            Stream<String> lines = bufferedReader.lines();
            String textContent = lines.collect(Collectors.joining("\n"));

            assertEquals("text", textContent);
        }
    }

    private void stubTextFileCreate(MockedStatic<GoogleServices> googleServicesMockedStatic) throws IOException {
        googleServicesMockedStatic
            .when(() -> GoogleServices.getDrive(parametersArgumentCaptor.capture()))
            .thenReturn(mockedDrive);
        when(mockedDrive.files())
            .thenReturn(mockedFiles);
        when(mockedFiles.create(fileArgumentCaptor.capture(), abstractInputStreamContentArgumentCaptor.capture()))
            .thenReturn(mockedCreate);
        when(mockedCreate.execute())
            .thenReturn(mockedGoogleFile);
    }

    private void stubFolderCreate() throws IOException {
        Create mockedFolderCreate = mock(Create.class);

        when(mockedFiles.create(folderArgumentCaptor.capture()))
            .thenReturn(mockedFolderCreate);
        when(mockedFolderCreate.execute())
            .thenReturn(new File().setId(CREATED_FOLDER_ID));
    }
}

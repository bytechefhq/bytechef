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

import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.MIME_TYPE;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.TEXT;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_NAME;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FOLDER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Mario Cvjetojevic
 * @author Monika KUšter
 */
class GoogleDriveCreateNewTextFileActionTest {

    private final ArgumentCaptor<AbstractInputStreamContent> abstractInputStreamContentArgumentCaptor =
        ArgumentCaptor.forClass(AbstractInputStreamContent.class);
    private final ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);
    private final Drive.Files.Create mockedCreate = mock(Drive.Files.Create.class);
    private final Drive mockedDrive = mock(Drive.class);
    private final Drive.Files mockedFiles = mock(Drive.Files.class);
    private final File mockedGoogleFile = mock(File.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FILE_NAME, "fileName", FOLDER_ID, "parentFolder", TEXT, "text", MIME_TYPE, "mimeType"));
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);

    @Test
    void testPerform() throws IOException {
        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getDrive(parametersArgumentCaptor.capture()))
                .thenReturn(mockedDrive);
            when(mockedDrive.files())
                .thenReturn(mockedFiles);
            when(mockedFiles.create(fileArgumentCaptor.capture(), abstractInputStreamContentArgumentCaptor.capture()))
                .thenReturn(mockedCreate);
            when(mockedCreate.execute())
                .thenReturn(mockedGoogleFile);

            File result = GoogleDriveCreateNewTextFileAction.perform(
                mockedParameters, mockedParameters, mock(ActionContext.class));

            assertEquals(mockedGoogleFile, result);
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());

            File expectedFile = new File()
                .setName("fileName")
                .setParents(List.of("parentFolder"));

            assertEquals(expectedFile, fileArgumentCaptor.getValue());

            AbstractInputStreamContent abstractInputStreamContent = abstractInputStreamContentArgumentCaptor.getValue();

            assertEquals("mimeType", abstractInputStreamContent.getType());

            try (InputStreamReader inputStreamReader = new InputStreamReader(
                abstractInputStreamContent.getInputStream(), StandardCharsets.UTF_8);

                BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

                Stream<String> lines = bufferedReader.lines();

                assertEquals("text", lines.collect(Collectors.joining("\n")));
            }
        }
    }
}

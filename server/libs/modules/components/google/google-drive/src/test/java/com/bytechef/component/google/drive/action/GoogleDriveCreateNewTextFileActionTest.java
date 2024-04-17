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

import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FILE_NAME;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.MIME_TYPE;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.PARENT_FOLDER;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.services.drive.model.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * @author Mario Cvjetojevic
 * @author Monika Domiter
 */
class GoogleDriveCreateNewTextFileActionTest extends AbstractGoogleDriveActionTest {

    @Test
    void testPerform() throws IOException {
        when(mockedParameters.getRequiredString(FILE_NAME))
            .thenReturn("fileName");
        when(mockedParameters.getString(PARENT_FOLDER))
            .thenReturn("parentFolder");
        when(mockedParameters.getRequiredString(TEXT))
            .thenReturn("text");
        when(mockedParameters.getRequiredString(MIME_TYPE))
            .thenReturn("mimeType");

        when(mockedFiles.create(fileArgumentCaptor.capture(), abstractInputStreamContentArgumentCaptor.capture()))
            .thenReturn(mockedCreate);
        when(mockedCreate.execute())
            .thenReturn(mockedGoogleFile);

        File result = GoogleDriveCreateNewTextFileAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedGoogleFile, result);

        File file = fileArgumentCaptor.getValue();

        assertEquals("fileName", file.getName());
        assertEquals(List.of("parentFolder"), file.getParents());

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

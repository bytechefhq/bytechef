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
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.AbstractInputStreamContent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Mario Cvjetojevic
 */
public class GoogleDriveCreateNewTextFileActionTest extends AbstractGoogleDriveCreateActionTest {

    @Disabled
    @Test
    public void testPerform() throws Exception {
        when(mockedParameters.getRequiredString(FILE_NAME))
            .thenReturn("fileName");
        when(mockedParameters.getString(TEXT))
            .thenReturn("text");
        when(mockedParameters.getString(MIME_TYPE))
            .thenReturn("mimeType");

        GoogleDriveCreateNewTextFileAction.perform(mockedParameters, mockedParameters, mockedContext);

        verify(mockedFiles, times(1))
            .create(fileArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());

        assertEquals("fileName", fileArgumentCaptor.getValue().getName());
        assertEquals("mimeType", inputStreamArgumentCaptor.getValue().getType());

        AbstractInputStreamContent content = inputStreamArgumentCaptor.getValue();

        try (InputStreamReader inputStreamReader = new InputStreamReader(
                content.getInputStream(), StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            assertEquals("text", bufferedReader.lines()
                    .collect(Collectors.joining("\n")));
        }
    }
}

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

package com.bytechef.component.google.slides.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleSlidesUtilsTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Drive mockedDrive = mock(Drive.class);
    private final Drive.Files mockedFiles = mock(Drive.Files.class);
    private final Drive.Files.List mockedList = mock(Drive.Files.List.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final ArgumentCaptor<String> qArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testGetPresentationIdOptions() throws IOException {
        File file1 = new File();

        file1.setName("Presentation 1");
        file1.setId("1234567890");

        File file2 = new File();

        file2.setName("Presentation 2");
        file2.setId("0987654321");

        List<File> files = Arrays.asList(file1, file2);

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getDrive(mockedParameters))
                .thenReturn(mockedDrive);

            when(mockedDrive.files())
                .thenReturn(mockedFiles);
            when(mockedFiles.list())
                .thenReturn(mockedList);
            when(mockedList.setQ(qArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.execute())
                .thenReturn(new FileList().setFiles(files));

            List<Option<String>> presentationIdOptions = GoogleSlidesUtils.getPresentationIdOptions(
                mockedParameters, mockedParameters, Map.of(), anyString(), mockedActionContext);

            assertNotNull(presentationIdOptions);
            assertEquals(2, presentationIdOptions.size());

            Option<String> presentationIdOptionsFirst = presentationIdOptions.getFirst();

            assertEquals("Presentation 1", presentationIdOptionsFirst.getLabel());
            assertEquals("1234567890", presentationIdOptionsFirst.getValue());

            Option<String> option = presentationIdOptions.get(1);

            assertEquals("Presentation 2", option.getLabel());
            assertEquals("0987654321", option.getValue());
            assertEquals("mimeType = 'application/vnd.google-apps.presentation' and trashed = false",
                qArgumentCaptor.getValue());
        }
    }
}

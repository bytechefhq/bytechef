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

package com.bytechef.component.google.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.forms.util.GoogleFormsUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleFormUtilsTest {

    private final List<Option<String>> expectedOptions = List.of(option("name", "id"));
    private final List<File> files = new ArrayList<>();
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Drive mockedDrive = mock(Drive.class);
    private final FileList mockedFileList = mock(FileList.class);
    private final Drive.Files mockedFiles = mock(Drive.Files.class);
    private final Drive.Files.List mockedList = mock(Drive.Files.List.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of());
    private final ArgumentCaptor<String> qArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testGetFormOptions() throws IOException {
        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic.when(() -> GoogleServices.getDrive(mockedParameters))
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

            assertEquals(expectedOptions,
                GoogleFormsUtils.getFormOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));

            assertEquals("mimeType = 'application/vnd.google-apps.form' and trashed = false",
                qArgumentCaptor.getValue());
        }
    }
}

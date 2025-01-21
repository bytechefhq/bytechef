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

import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.google.api.services.drive.model.File;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Arina Kolodeznikova
 */
class GoogleDriveGetFileActionTest extends AbstractGoogleDriveActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(FILE_ID, "testId"));

    private final File testFile = new File()
        .setId("testFileId")
        .setName("fileName")
        .setMimeType("application/pdf")
        .setKind("drive#file");

    @Test
    void testPerform() throws IOException {
        when(mockedFiles.get(fileIdArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.execute())
            .thenReturn(testFile);

        File retrievedFile = GoogleDriveGetFileAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        verify(mockedDrive.files()).get("testId");

        assertEquals(testFile, retrievedFile);
        assertEquals("testId", fileIdArgumentCaptor.getValue());
    }
}

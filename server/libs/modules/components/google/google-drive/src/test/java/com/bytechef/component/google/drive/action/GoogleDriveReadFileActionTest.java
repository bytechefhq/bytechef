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

import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FILE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Mario Cvjetojevic
 * @author Monika Domiter
 */
@Disabled
class GoogleDriveReadFileActionTest extends AbstractGoogleDriveActionTest {

    private final InputStream mockedInputStream = mock(InputStream.class);

    @Test
    void testPerform() throws IOException {
        when(mockedParameters.getRequiredString(FILE_ID))
            .thenReturn("fileId");

        when(mockedFiles.get(fileIdArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.executeMediaAsInputStream())
            .thenReturn(mockedInputStream);

        GoogleDriveReadFileAction.perform(mockedParameters, mockedParameters, mockedContext);

        String fileId = fileIdArgumentCaptor.getValue();

        assertEquals("fileId", fileId);
    }
}

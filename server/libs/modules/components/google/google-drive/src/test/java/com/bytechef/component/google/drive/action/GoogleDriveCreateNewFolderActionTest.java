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

import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FOLDER_NAME;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.PARENT_FOLDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.google.api.services.drive.model.File;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Mario Cvjetojevic
 * @author Monika Domiter
 */
class GoogleDriveCreateNewFolderActionTest extends AbstractGoogleDriveActionTest {

    @Test
    void testPerform() throws IOException {
        when(mockedParameters.getRequiredString(FOLDER_NAME))
            .thenReturn("folderName");
        when(mockedParameters.getString(PARENT_FOLDER))
            .thenReturn("parentFolder");

        when(mockedFiles.create(fileArgumentCaptor.capture()))
            .thenReturn(mockedCreate);
        when(mockedCreate.execute())
            .thenReturn(mockedGoogleFile);

        File result = GoogleDriveCreateNewFolderAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedGoogleFile, result);

        File file = fileArgumentCaptor.getValue();

        assertEquals("folderName", file.getName());
        assertEquals("application/vnd.google-apps.folder", file.getMimeType());
        assertEquals(List.of("parentFolder"), file.getParents());
    }
}

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

import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.APPLICATION_VND_GOOGLE_APPS_FOLDER;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FOLDER_NAME;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.PARENT_FOLDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.google.api.services.drive.model.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
class GoogleDriveCreateNewFolderActionTest extends AbstractGoogleDriveActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FOLDER_NAME, "folderName", PARENT_FOLDER, "parentFolder"));

    @Test
    void testPerform() throws IOException {
        when(mockedFiles.create(fileArgumentCaptor.capture()))
            .thenReturn(mockedCreate);
        when(mockedCreate.execute())
            .thenReturn(mockedGoogleFile);

        File result = GoogleDriveCreateNewFolderAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals(mockedGoogleFile, result);

        File file = fileArgumentCaptor.getValue();

        assertEquals("folderName", file.getName());
        assertEquals(APPLICATION_VND_GOOGLE_APPS_FOLDER, file.getMimeType());
        assertEquals(List.of("parentFolder"), file.getParents());
    }
}

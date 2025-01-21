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
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_NAME;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FOLDER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.drive.model.File;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Mayank Madan
 * @author Monika Kušter
 */
class GoogleDriveCopyFileActionTest extends AbstractGoogleDriveActionTest {
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FILE_ID, "originalFileId", FILE_NAME, "newFileName", FOLDER_ID, "newFolderId"));
    private final File mockedFile = mock(File.class);

    @Test
    void testPerform() throws IOException {
        try (MockedStatic<GoogleUtils> googleUtilsMockedStatic = mockStatic(GoogleUtils.class);) {
            googleUtilsMockedStatic
                .when(() -> GoogleUtils.copyFileOnGoogleDrive(mockedParameters, mockedParameters))
                .thenReturn(mockedFile);

            File result = GoogleDriveCopyFileAction.perform(mockedParameters, mockedParameters, mockedActionContext);

            assertEquals(mockedFile, result);
        }
    }
}

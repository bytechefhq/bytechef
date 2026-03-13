/*
 * Copyright 2025 ByteChef
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

import static com.bytechef.google.commons.constant.GoogleCommonsContants.FOLDER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.drive.util.GoogleDriveUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.google.api.services.drive.model.File;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleDriveListFoldersActionTest {

    private final ArgumentCaptor<Boolean> booleanArgumentCaptor = forClass(Boolean.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(FOLDER_ID, "testId"));
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    private final File testFile = new File()
        .setId("testFileId")
        .setName("fileName")
        .setMimeType("application/pdf")
        .setKind("drive#file");

    @Test
    void testPerform() {
        try (MockedStatic<GoogleDriveUtils> googleDriveUtilsMockedStatic = mockStatic(GoogleDriveUtils.class)) {
            googleDriveUtilsMockedStatic.when(() -> GoogleDriveUtils.listFiles(
                stringArgumentCaptor.capture(), booleanArgumentCaptor.capture(), parametersArgumentCaptor.capture()))
                .thenReturn(List.of(testFile));

            List<File> result = GoogleDriveListFoldersAction.perform(
                mockedParameters, mockedParameters, mock(ActionContext.class));

            assertEquals(List.of(testFile), result);
            assertEquals("testId", stringArgumentCaptor.getValue());
            assertEquals(true, booleanArgumentCaptor.getValue());
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
        }
    }
}

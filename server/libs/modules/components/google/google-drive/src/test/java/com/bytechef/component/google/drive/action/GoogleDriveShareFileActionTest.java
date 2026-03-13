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

import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.drive.util.GoogleDriveUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Marija Horvat
 */
class GoogleDriveShareFileActionTest {

    private final Parameters mockedConnectionParameters = mock(Parameters.class);
    private final Parameters mockedInputParameters = MockParametersFactory.create(Map.of(FILE_ID, "testId"));
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform() throws IOException {
        try (MockedStatic<GoogleDriveUtils> googleDriveUtilsMockedStatic = mockStatic(GoogleDriveUtils.class)) {
            googleDriveUtilsMockedStatic
                .when(() -> GoogleDriveUtils.getFileWebViewLink(
                    parametersArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn("link");

            String sharedLink = GoogleDriveShareFileAction.perform(
                mockedInputParameters, mockedConnectionParameters, mock(ActionContext.class));

            assertEquals("link", sharedLink);
            assertEquals("testId", stringArgumentCaptor.getValue());
            assertEquals(mockedConnectionParameters, parametersArgumentCaptor.getValue());
        }
    }
}

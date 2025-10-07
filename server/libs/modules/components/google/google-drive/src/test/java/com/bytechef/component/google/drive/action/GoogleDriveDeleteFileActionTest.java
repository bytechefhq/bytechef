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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Mayank Madan
 */
class GoogleDriveDeleteFileActionTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Drive.Files.Delete mockedDelete = mock(Drive.Files.Delete.class);
    private final Drive mockedDrive = mock(Drive.class);
    private final Drive.Files mockedFiles = mock(Drive.Files.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(FILE_ID, "testId"));
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() throws IOException {
        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getDrive(parametersArgumentCaptor.capture()))
                .thenReturn(mockedDrive);
            when(mockedDrive.files())
                .thenReturn(mockedFiles);
            when(mockedFiles.delete(stringArgumentCaptor.capture()))
                .thenReturn(mockedDelete);

            doNothing()
                .when(mockedDelete)
                .execute();

            GoogleDriveDeleteFileAction.perform(mockedParameters, mockedParameters, mockedActionContext);

            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals("testId", stringArgumentCaptor.getValue());

            verify(mockedDelete).execute();
        }
    }

    @Test
    void testPerformThrowsIOException() throws IOException {
        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getDrive(parametersArgumentCaptor.capture()))
                .thenReturn(mockedDrive);
            when(mockedDrive.files())
                .thenReturn(mockedFiles);
            when(mockedFiles.delete(stringArgumentCaptor.capture()))
                .thenReturn(mockedDelete);

            doThrow(new IOException("Error deleting file"))
                .when(mockedDelete)
                .execute();

            assertThrows(ProviderException.class,
                () -> GoogleDriveDeleteFileAction.perform(mockedParameters, mockedParameters, mockedActionContext));

            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals("testId", stringArgumentCaptor.getValue());
        }
    }
}

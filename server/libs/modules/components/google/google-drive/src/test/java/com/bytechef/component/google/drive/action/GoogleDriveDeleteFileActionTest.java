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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.google.api.services.drive.Drive;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Mayank Madan
 */
class GoogleDriveDeleteFileActionTest extends AbstractGoogleDriveActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(FILE_ID, "testId"));
    private final Drive.Files.Delete mockedDelete = mock(Drive.Files.Delete.class);

    @Test
    void testPerform() throws IOException {
        when(mockedFiles.delete(fileIdArgumentCaptor.capture()))
            .thenReturn(mockedDelete);

        doNothing()
            .when(mockedDelete)
            .execute();

        GoogleDriveDeleteFileAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals("testId", fileIdArgumentCaptor.getValue());

        verify(mockedDelete).execute();
    }

    @Test
    void testPerformThrowsIOException() throws IOException {
        when(mockedFiles.delete(fileIdArgumentCaptor.capture()))
            .thenReturn(mockedDelete);

        doThrow(new IOException("Error deleting file"))
            .when(mockedDelete)
            .execute();

        assertThrows(IOException.class,
            () -> GoogleDriveDeleteFileAction.perform(mockedParameters, mockedParameters, mockedActionContext));

        assertEquals("testId", fileIdArgumentCaptor.getValue());
    }
}

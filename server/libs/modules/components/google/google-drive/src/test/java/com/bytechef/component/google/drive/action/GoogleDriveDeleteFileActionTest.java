package com.bytechef.component.google.drive.action;
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

 import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FILE_ID;
 import static org.junit.jupiter.api.Assertions.assertThrows;
 import static org.mockito.Mockito.doNothing;
 import static org.mockito.Mockito.doThrow;
 import static org.mockito.Mockito.mock;
 import static org.mockito.Mockito.verify;
 import static org.mockito.Mockito.when;
 
 import java.io.IOException;
 
 import org.junit.jupiter.api.Test;
 
 import com.bytechef.component.definition.Parameters;
 import com.google.api.services.drive.Drive;
 
public class GoogleDriveDeleteFileActionTest extends AbstractGoogleDriveActionTest {

    @Test
    public void testPerform() throws IOException {

        String fileId = "testID";
        Parameters inputParameters = mock(Parameters.class);
        Parameters connectionParameters = mock(Parameters.class);

        when(inputParameters.getRequiredString(FILE_ID)).thenReturn(fileId);

        Drive.Files.Delete mockedDelete = mock(Drive.Files.Delete.class);
        when(mockedFiles.delete(fileId)).thenReturn(mockedDelete);

        doNothing().when(mockedDelete).execute();

        GoogleDriveDeleteFileAction.perform(inputParameters, connectionParameters, mockedContext);

        verify(mockedFiles).delete(fileId); // Verify the delete method was called with the correct file ID
        verify(mockedDelete).execute();

    }

    @Test
    public void testPerform_ThrowsIOException() throws IOException {
        String fileId = "testID";
        Parameters inputParameters = mock(Parameters.class);
        Parameters connectionParameters = mock(Parameters.class);

        when(inputParameters.getRequiredString(FILE_ID)).thenReturn(fileId);

        Drive.Files.Delete mockedDelete = mock(Drive.Files.Delete.class);
        when(mockedFiles.delete(fileId)).thenReturn(mockedDelete);

        doThrow(new IOException("Error deleting file")).when(mockedDelete).execute();

        assertThrows(IOException.class, () -> {
            GoogleDriveDeleteFileAction.perform(inputParameters, connectionParameters, mockedContext);
        });
    }
}

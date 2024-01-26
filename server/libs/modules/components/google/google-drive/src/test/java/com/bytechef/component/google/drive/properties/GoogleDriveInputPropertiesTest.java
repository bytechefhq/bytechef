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

package com.bytechef.component.google.drive.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.drive.util.GoogleDriveUtils;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.DriveList;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class GoogleDriveInputPropertiesTest {
    private static final String SEARCH_TEXT = "12345";
    private final ActionContext mockedContext = mock(ActionContext.class);
    private MockedStatic<GoogleDriveUtils> mockedGoogleDriveUtils;
    private final Drive mockedDrive = mock(Drive.class);
    private final Drive.Drives mockedDrives = mock(Drive.Drives.class);
    private final Drive.Drives.List mockedList = mock(Drive.Drives.List.class);
    private final DriveList mockedDriveList = mock(DriveList.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final List<com.google.api.services.drive.model.Drive> mockedSubDriveArrayList = Arrays.asList(
        mock(com.google.api.services.drive.model.Drive.class),
        mock(com.google.api.services.drive.model.Drive.class),
        mock(com.google.api.services.drive.model.Drive.class),
        mock(com.google.api.services.drive.model.Drive.class));

    @Test
    public void testGetDriveOptions() throws IOException {

        mockedGoogleDriveUtils = mockStatic(GoogleDriveUtils.class);

        mockedGoogleDriveUtils.when(() -> GoogleDriveUtils.getDrive(mockedParameters))
            .thenReturn(mockedDrive);
        when(mockedDrive.drives())
            .thenReturn(mockedDrives);
        when(mockedDrives.list())
            .thenReturn(mockedList);
        when(mockedList.execute())
            .thenReturn(mockedDriveList);
        when(mockedDriveList.getDrives())
            .thenReturn(mockedSubDriveArrayList);

        mockedSubDriveArrayList.forEach(drive -> when(drive.getName())
            .thenReturn("NOT searched text"));

        com.google.api.services.drive.model.Drive drive = mockedSubDriveArrayList.getFirst();

        when(drive.getName())
            .thenReturn(SEARCH_TEXT + " more text");

        List<Option<String>> options = GoogleDriveInputProperties.getDriveOptions(
            mockedParameters, mockedParameters, SEARCH_TEXT, mockedContext);

        verify(mockedDriveList, times(1))
            .getDrives();

        assertEquals(1, options.size());

        options.forEach(option -> assertTrue(StringUtils.startsWith(option.getLabel(), SEARCH_TEXT)));

        mockedGoogleDriveUtils.close();
    }
}

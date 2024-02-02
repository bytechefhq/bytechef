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

package com.bytechef.component.google.drive.util;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.DriveList;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class GoogleDriveUtilsTest {

    @Test
    void testGetDrive() {
        Parameters mockedParameters = mock(Parameters.class);

        when(mockedParameters.getRequiredString(ACCESS_TOKEN))
            .thenReturn("accessToken");

        Drive drive = GoogleDriveUtils.getDrive(mockedParameters);

        assertEquals("Google Drive Component", drive.getApplicationName());
    }

    @Disabled
    @Test
    public void testGetDriveOptions() throws IOException {
        Drive mockedDrive = mock(Drive.class);
        Drive.Drives mockedDrives = mock(Drive.Drives.class);
        Drive.Drives.List mockedList = mock(Drive.Drives.List.class);
        DriveList mockedDriveList = mock(DriveList.class);
        List<com.google.api.services.drive.model.Drive> mockedSubDriveArrayList = Arrays.asList(
            mock(com.google.api.services.drive.model.Drive.class),
            mock(com.google.api.services.drive.model.Drive.class),
            mock(com.google.api.services.drive.model.Drive.class),
            mock(com.google.api.services.drive.model.Drive.class));

        try (MockedStatic<GoogleDriveUtils> mockedGoogleDriveUtils = mockStatic(GoogleDriveUtils.class)) {
            mockedGoogleDriveUtils
                .when(() -> GoogleDriveUtils.getDrive(any()))
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

            String searchText = "12345  more text";

            when(drive.getName())
                .thenReturn(searchText);

            List<Option<String>> options = GoogleDriveUtils.getDriveOptions(
                any(), any(), any(), any());

            verify(mockedDriveList, times(1))
                .getDrives();

            assertEquals(1, options.size());

            Option<String> option = options.getFirst();

            assertTrue(StringUtils.equals(option.getLabel(), searchText));
        }
    }
}

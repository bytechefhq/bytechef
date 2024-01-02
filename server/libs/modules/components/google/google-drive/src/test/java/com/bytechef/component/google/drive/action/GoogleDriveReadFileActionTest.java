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

import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.ACKNOWLEDGE_ABUSE;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FILE_ID;
import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.INCLUDE_LABELS;
import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.INCLUDE_PERMISSIONS_FOR_VIEW;
import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.SUPPORTS_ALL_DRIVES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.services.drive.Drive;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Mario Cvjetojevic
 */
public class GoogleDriveReadFileActionTest extends AbstractGoogleDriveActionTest {

    private final Drive.Files.Get mockedGet = mock(Drive.Files.Get.class);
    private final ArgumentCaptor<String> getArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Boolean> acknowledgeAbuseArgumentCaptor = ArgumentCaptor.forClass(Boolean.class);
    private final ArgumentCaptor<Boolean> supportsAllDrivesArgumentCaptor = ArgumentCaptor.forClass(Boolean.class);
    private final ArgumentCaptor<String> includePermissionsForViewArgumentCaptor =
        ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> includeLabelsArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    public void testPerform() throws Exception {
        when(mockedParameters.getRequiredString(FILE_ID))
            .thenReturn("fileId");
        when(mockedParameters.getBoolean(ACKNOWLEDGE_ABUSE))
            .thenReturn(true);
        when(mockedParameters.getBoolean(SUPPORTS_ALL_DRIVES))
            .thenReturn(true);
        when(mockedParameters.getString(INCLUDE_PERMISSIONS_FOR_VIEW))
            .thenReturn("includePermissionsForViewStub");
        when(mockedParameters.getString(INCLUDE_LABELS))
            .thenReturn("includeLabelsStub");

        when(mockedFiles.get(getArgumentCaptor.capture()))
            .thenReturn(mockedGet);

        when(mockedGet.setAcknowledgeAbuse(acknowledgeAbuseArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.setSupportsAllDrives(supportsAllDrivesArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.setIncludePermissionsForView(includePermissionsForViewArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.setIncludeLabels(includeLabelsArgumentCaptor.capture()))
            .thenReturn(mockedGet);

        GoogleDriveReadFileAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals("fileId", getArgumentCaptor.getValue());
        assertEquals(true, acknowledgeAbuseArgumentCaptor.getValue());
        assertEquals(true, supportsAllDrivesArgumentCaptor.getValue());
        assertEquals("includePermissionsForViewStub", includePermissionsForViewArgumentCaptor.getValue());
        assertEquals("includeLabelsStub", includeLabelsArgumentCaptor.getValue());

        verify(mockedGet, times(1))
            .execute();

    }
}

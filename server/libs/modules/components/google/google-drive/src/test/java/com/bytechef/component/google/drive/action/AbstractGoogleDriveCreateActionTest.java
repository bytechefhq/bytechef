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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import java.io.IOException;
import java.util.HashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;

/**
 * @author Mario Cvjetojevic
 */
public abstract class AbstractGoogleDriveCreateActionTest extends AbstractGoogleDriveActionTest {

    protected Drive.Files.Create mockedCreate = mock(Drive.Files.Create.class);
    protected ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);
    protected ArgumentCaptor<AbstractInputStreamContent> inputStreamArgumentCaptor =
        ArgumentCaptor.forClass(AbstractInputStreamContent.class);

    protected HashMap<String, ArgumentCaptor<?>> argumentCaptorMap = new HashMap<>() {
        {
            put("driveId", ArgumentCaptor.forClass(String.class));
            put("ignoreDefaultVisibility", ArgumentCaptor.forClass(Boolean.class));
            put("keepRevisionForever", ArgumentCaptor.forClass(Boolean.class));
            put("ocrLanguage", ArgumentCaptor.forClass(String.class));
            put("supportsAllDrives", ArgumentCaptor.forClass(Boolean.class));
            put("useContentAsIndexableText", ArgumentCaptor.forClass(Boolean.class));
            put("includePermissionsForView", ArgumentCaptor.forClass(String.class));
            put("includeLabels", ArgumentCaptor.forClass(String.class));
        }
    };

    @BeforeEach
    protected void beforeTestPerform() throws IOException {
        when(mockedParameters.containsKey("driveId"))
            .thenReturn(true);
        when(mockedParameters.getString("driveId"))
            .thenReturn("driveIdStub");
        when(mockedParameters.getBoolean("ignoreDefaultVisibility"))
            .thenReturn(true);
        when(mockedParameters.getBoolean("keepRevisionForever"))
            .thenReturn(true);
        when(mockedParameters.getString("ocrLanguage"))
            .thenReturn("ocrLanguageStub");
        when(mockedParameters.getBoolean("supportsAllDrives"))
            .thenReturn(true);
        when(mockedParameters.getBoolean("useContentAsIndexableText"))
            .thenReturn(true);
        when(mockedParameters.getString("includePermissionsForView"))
            .thenReturn("includePermissionsForViewStub");
        when(mockedParameters.getString("includeLabels"))
            .thenReturn("includeLabelsStub");
        when(mockedFiles.create(any(), any()))
            .thenReturn(mockedCreate);
        when(mockedCreate.setFields("id"))
            .thenReturn(mockedCreate);
        when(mockedCreate.setIgnoreDefaultVisibility(
            (Boolean) argumentCaptorMap.get("ignoreDefaultVisibility").capture()))
            .thenReturn(mockedCreate);
        when(mockedCreate.setKeepRevisionForever(
            (Boolean) argumentCaptorMap.get("keepRevisionForever").capture()))
            .thenReturn(mockedCreate);
        when(mockedCreate.setOcrLanguage(
            (String) argumentCaptorMap.get("ocrLanguage").capture()))
            .thenReturn(mockedCreate);
        when(mockedCreate.setSupportsAllDrives(
            (Boolean) argumentCaptorMap.get("supportsAllDrives").capture()))
            .thenReturn(mockedCreate);
        when(mockedCreate.setUseContentAsIndexableText(
            (Boolean) argumentCaptorMap.get("useContentAsIndexableText").capture()))
            .thenReturn(mockedCreate);
        when(mockedCreate.setIncludePermissionsForView(
            (String) argumentCaptorMap.get("includePermissionsForView").capture()))
            .thenReturn(mockedCreate);
        when(mockedCreate.setIncludeLabels(
            (String) argumentCaptorMap.get("includeLabels").capture()))
            .thenReturn(mockedCreate);
    }

    @AfterEach
    protected void afterTestPerform() throws IOException {
        assertEquals(true, argumentCaptorMap.get("ignoreDefaultVisibility")
            .getValue());
        assertEquals(true, argumentCaptorMap.get("keepRevisionForever")
            .getValue());
        assertEquals("ocrLanguageStub", argumentCaptorMap.get("ocrLanguage")
            .getValue());
        assertEquals(true, argumentCaptorMap.get("supportsAllDrives")
            .getValue());
        assertEquals(true, argumentCaptorMap.get("useContentAsIndexableText")
            .getValue());
        assertEquals("includePermissionsForViewStub", argumentCaptorMap.get("includePermissionsForView")
            .getValue());
        assertEquals("includeLabelsStub", argumentCaptorMap.get("includeLabels")
            .getValue());

        verify(mockedCreate, times(1))
            .execute();
    }
}

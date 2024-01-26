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

import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.DRIVE_ID;
import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.IGNORE_DEFAULT_VISIBILITY;
import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.INCLUDE_LABELS;
import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.INCLUDE_PERMISSIONS_FOR_VIEW;
import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.KEEP_REVISION_FOREVER;
import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.OCR_LANGUAGE;
import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.SUPPORTS_ALL_DRIVES;
import static com.bytechef.component.google.drive.properties.GoogleDriveInputProperties.USE_CONTENT_AS_INDEXABLE_TEXT;
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
            put(DRIVE_ID, ArgumentCaptor.forClass(String.class));
            put(IGNORE_DEFAULT_VISIBILITY, ArgumentCaptor.forClass(Boolean.class));
            put(KEEP_REVISION_FOREVER, ArgumentCaptor.forClass(Boolean.class));
            put(OCR_LANGUAGE, ArgumentCaptor.forClass(String.class));
            put(SUPPORTS_ALL_DRIVES, ArgumentCaptor.forClass(Boolean.class));
            put(USE_CONTENT_AS_INDEXABLE_TEXT, ArgumentCaptor.forClass(Boolean.class));
            put(INCLUDE_PERMISSIONS_FOR_VIEW, ArgumentCaptor.forClass(String.class));
            put(INCLUDE_LABELS, ArgumentCaptor.forClass(String.class));
        }
    };

    @BeforeEach
    protected void beforeTestPerform() throws IOException {
        when(mockedParameters.containsKey(DRIVE_ID))
            .thenReturn(true);
        when(mockedParameters.getString(DRIVE_ID))
            .thenReturn("driveIdStub");
        when(mockedParameters.getBoolean(IGNORE_DEFAULT_VISIBILITY))
            .thenReturn(true);
        when(mockedParameters.getBoolean(KEEP_REVISION_FOREVER))
            .thenReturn(true);
        when(mockedParameters.getString(OCR_LANGUAGE))
            .thenReturn("ocrLanguageStub");
        when(mockedParameters.getBoolean(SUPPORTS_ALL_DRIVES))
            .thenReturn(true);
        when(mockedParameters.getBoolean(USE_CONTENT_AS_INDEXABLE_TEXT))
            .thenReturn(true);
        when(mockedParameters.getString(INCLUDE_PERMISSIONS_FOR_VIEW))
            .thenReturn("includePermissionsForViewStub");
        when(mockedParameters.getString(INCLUDE_LABELS))
            .thenReturn("includeLabelsStub");
        when(mockedFiles.create(any(), any()))
            .thenReturn(mockedCreate);
        when(mockedCreate.setFields("id"))
            .thenReturn(mockedCreate);
        when(mockedCreate.setIgnoreDefaultVisibility(
            (Boolean) argumentCaptorMap.get(IGNORE_DEFAULT_VISIBILITY).capture()))
            .thenReturn(mockedCreate);
        when(mockedCreate.setKeepRevisionForever(
            (Boolean) argumentCaptorMap.get(KEEP_REVISION_FOREVER).capture()))
            .thenReturn(mockedCreate);
        when(mockedCreate.setOcrLanguage(
            (String) argumentCaptorMap.get(OCR_LANGUAGE).capture()))
            .thenReturn(mockedCreate);
        when(mockedCreate.setSupportsAllDrives(
            (Boolean) argumentCaptorMap.get(SUPPORTS_ALL_DRIVES).capture()))
            .thenReturn(mockedCreate);
        when(mockedCreate.setUseContentAsIndexableText(
            (Boolean) argumentCaptorMap.get(USE_CONTENT_AS_INDEXABLE_TEXT).capture()))
            .thenReturn(mockedCreate);
        when(mockedCreate.setIncludePermissionsForView(
            (String) argumentCaptorMap.get(INCLUDE_PERMISSIONS_FOR_VIEW).capture()))
            .thenReturn(mockedCreate);
        when(mockedCreate.setIncludeLabels(
            (String) argumentCaptorMap.get(INCLUDE_LABELS).capture()))
            .thenReturn(mockedCreate);
    }

    @AfterEach
    protected void afterTestPerform() throws IOException {
        assertEquals(true, argumentCaptorMap.get(IGNORE_DEFAULT_VISIBILITY)
            .getValue());
        assertEquals(true, argumentCaptorMap.get(KEEP_REVISION_FOREVER)
            .getValue());
        assertEquals("ocrLanguageStub", argumentCaptorMap.get(OCR_LANGUAGE)
            .getValue());
        assertEquals(true, argumentCaptorMap.get(SUPPORTS_ALL_DRIVES)
            .getValue());
        assertEquals(true, argumentCaptorMap.get(USE_CONTENT_AS_INDEXABLE_TEXT)
            .getValue());
        assertEquals("includePermissionsForViewStub", argumentCaptorMap.get(INCLUDE_PERMISSIONS_FOR_VIEW)
            .getValue());
        assertEquals("includeLabelsStub", argumentCaptorMap.get(INCLUDE_LABELS)
            .getValue());

        verify(mockedCreate, times(1))
            .execute();
    }
}

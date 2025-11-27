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

package com.bytechef.google.commons;

import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_ID;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_NAME;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FOLDER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Setting;
import com.google.api.services.calendar.model.Settings;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleUtilsTest {

    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Integer> integerArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    private final Context mockedContext = mock(Context.class);
    private final Drive.Files.Copy mockedCopy = mock(Drive.Files.Copy.class);
    private final Drive mockedDrive = mock(Drive.class);
    private final Drive.Files.Get mockedGet = mock(Drive.Files.Get.class);
    private final Drive.Files mockedFiles = mock(Drive.Files.class);
    private final Drive.Files.List mockedList = mock(Drive.Files.List.class);
    private final Calendar mockedCalendar = mock(Calendar.class);
    private final Calendar.Settings mockedSettings = mock(Calendar.Settings.class);
    private final Calendar.Settings.List mockedSettingsList = mock(Calendar.Settings.List.class);

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FILE_ID, "originalFileId", FILE_NAME, "newFileName", FOLDER_ID, "newFolderId"));
    private final File testFile = new File()
        .setName("newFileName")
        .setParents(Collections.singletonList("newFolderId"))
        .setMimeType("application/pdf");

    @Test
    void testCopyFileOnGoogleDrive() throws IOException {
        when(mockedDrive.files())
            .thenReturn(mockedFiles);
        when(mockedFiles.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.execute())
            .thenReturn(testFile);
        when(mockedFiles.copy("originalFileId", testFile))
            .thenReturn(mockedCopy);
        when(mockedCopy.execute())
            .thenReturn(testFile);

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getDrive(mockedParameters))
                .thenReturn(mockedDrive);

            File resultFile = GoogleUtils.copyFileOnGoogleDrive(mockedParameters, mockedParameters);

            verify(mockedDrive.files()).get("originalFileId");
            verify(mockedFiles).copy("originalFileId", testFile);

            assertEquals("newFileName", resultFile.getName());
            assertEquals(Collections.singletonList("newFolderId"), resultFile.getParents());
            assertEquals("application/pdf", resultFile.getMimeType());
        }
    }

    @Test
    void testGetAllFiles() throws IOException {
        when(mockedDrive.files())
            .thenReturn(mockedFiles);
        when(mockedFiles.list())
            .thenReturn(mockedList);
        when(mockedList.setQ(stringArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setPageSize(integerArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setPageToken(stringArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.execute())
            .thenReturn(new FileList().setFiles(List.of(testFile))
                .setNextPageToken(null));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getDrive(mockedParameters))
                .thenReturn(mockedDrive);

            List<String> strings = new ArrayList<>();

            strings.add("query");
            strings.add(null);

            List<File> files = GoogleUtils.getAllFiles(mockedParameters, "query");

            assertEquals(List.of(testFile), files);
            assertEquals(strings, stringArgumentCaptor.getAllValues());
            assertEquals(1000, integerArgumentCaptor.getValue());
        }
    }

    @Test
    void testGetCalendarTimezoneThrowsExceptionIfNotFound() throws IOException {
        Settings settings = new Settings()
            .setItems(List.of(new Setting().setId("other")
                .setValue("value")))
            .setNextPageToken(null);

        when(mockedCalendar.settings())
            .thenReturn(mockedSettings);
        when(mockedSettings.list())
            .thenReturn(mockedSettingsList);
        when(mockedSettingsList.setMaxResults(integerArgumentCaptor.capture()))
            .thenReturn(mockedSettingsList);
        when(mockedSettingsList.setPageToken(stringArgumentCaptor.capture()))
            .thenReturn(mockedSettingsList);
        when(mockedSettingsList.execute())
            .thenReturn(settings);

        ProviderException exception = assertThrows(
            ProviderException.class, () -> GoogleUtils.getCalendarTimezone(mockedCalendar));

        assertEquals("Timezone setting not found.", exception.getMessage());
        assertNull(stringArgumentCaptor.getValue());
        assertEquals(250, integerArgumentCaptor.getValue());
    }

    @Test
    void testGetCalendarTimezone() throws IOException {
        Settings settings = new Settings()
            .setItems(List.of(new Setting().setId("timezone")
                .setValue("test timezone")))
            .setNextPageToken(null);

        when(mockedCalendar.settings())
            .thenReturn(mockedSettings);
        when(mockedSettings.list())
            .thenReturn(mockedSettingsList);
        when(mockedSettingsList.setMaxResults(integerArgumentCaptor.capture()))
            .thenReturn(mockedSettingsList);
        when(mockedSettingsList.setPageToken(stringArgumentCaptor.capture()))
            .thenReturn(mockedSettingsList);
        when(mockedSettingsList.execute())
            .thenReturn(settings);

        String calendarTimezone = GoogleUtils.getCalendarTimezone(mockedCalendar);

        assertEquals("test timezone", calendarTimezone);
        assertNull(stringArgumentCaptor.getValue());
        assertEquals(250, integerArgumentCaptor.getValue());
    }

    @Test
    void processErrorResponseParseGoogleStyleErrorMessage() {
        when(mockedContext.json(any()))
            .thenReturn(Map.of("error", Map.of("message", "Rate limit exceeded")));

        ProviderException providerException = GoogleUtils.processErrorResponse(
            429, "{\"error\":{\"message\":\"...\"}}", mockedContext);

        assertEquals(429, providerException.getStatusCode());
        assertEquals("Rate limit exceeded", providerException.getMessage());
    }

    @Test
    void processErrorResponseFallbackToBodyToString() {
        when(mockedContext.json(any()))
            .thenReturn("not a map");

        ProviderException providerException = GoogleUtils.processErrorResponse(500, "raw-body-text", mockedContext);

        assertEquals(500, providerException.getStatusCode());
        assertEquals("raw-body-text", providerException.getMessage());
    }

    @Test
    void translateGoogleIOExceptionReturnProviderException() {
        GoogleJsonError googleJsonError = new GoogleJsonError();

        googleJsonError.setMessage("Not Found");

        HttpResponseException.Builder builder = new HttpResponseException.Builder(
            404, "Not Found", new HttpHeaders());

        GoogleJsonResponseException googleJsonResponseException = new GoogleJsonResponseException(
            builder, googleJsonError);

        ProviderException providerException = GoogleUtils.translateGoogleIOException(googleJsonResponseException);

        assertEquals(404, providerException.getStatusCode());
        assertEquals("Not Found", providerException.getMessage());
    }

    @Test
    void translateGoogleIOExceptionUseExceptionMessage() {
        HttpResponseException.Builder builder = new HttpResponseException.Builder(
            400, "Bad Request", new HttpHeaders())
                .setMessage("Some error occurred");

        GoogleJsonResponseException googleJsonResponseException = new GoogleJsonResponseException(builder, null);

        ProviderException providerException = GoogleUtils.translateGoogleIOException(googleJsonResponseException);

        assertEquals(400, providerException.getStatusCode());
        assertEquals("Some error occurred", providerException.getMessage());
    }

    @Test
    void translateGoogleIOExceptionWrapGenericIOException() {
        IOException ioException = new IOException("network down");

        ProviderException providerException = GoogleUtils.translateGoogleIOException(ioException);

        assertNull(providerException.getStatusCode());
        assertInstanceOf(IOException.class, providerException.getCause());
        assertEquals(ioException, providerException.getCause());
    }
}

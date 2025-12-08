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

package com.bytechef.component.google.photos.action;

import static com.bytechef.component.google.photos.constant.GooglePhotosConstants.ALBUM_ID;
import static com.bytechef.component.google.photos.constant.GooglePhotosConstants.FILE_BINARY;
import static com.bytechef.component.google.photos.constant.GooglePhotosConstants.MEDIA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class GooglePhotosUploadMediaActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Response mockedResponse = mock(Response.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Object mockedObject = mock(Object.class);

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);

    @Test
    void testGetUploadToken() {

        GooglePhotosUploadMediaAction.Media media =
            new GooglePhotosUploadMediaAction.Media("name123", mockedFileEntry);

        when(mockedParameters.getRequiredFileEntry(FILE_BINARY))
            .thenReturn(mockedFileEntry);
        when(mockedFileEntry.getName())
            .thenReturn("name");
        when(mockedFileEntry.getMimeType())
            .thenReturn("mimeType");
        when(mockedParameters.getRequiredString(ALBUM_ID))
            .thenReturn("album123");
        when(mockedParameters.getRequiredList(eq(MEDIA), eq(GooglePhotosUploadMediaAction.Media.class)))
            .thenReturn(List.of(media));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn("UPLOAD_TOKEN_123");
        when(mockedResponse.getBody())
            .thenReturn(mockedObject);

        Object result =
            GooglePhotosUploadMediaAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedObject, result);

        Body body = bodyArgumentCaptor.getValue();

        List<Map<String, Object>> mediaItems = List.of(
            Map.of(
                "simpleMediaItem",
                Map.of(
                    "uploadToken", "UPLOAD_TOKEN_123",
                    "fileName", "name123")));

        assertEquals(Map.of(
            ALBUM_ID, "album123",
            "newMediaItems", mediaItems), body.getContent());

    }
}

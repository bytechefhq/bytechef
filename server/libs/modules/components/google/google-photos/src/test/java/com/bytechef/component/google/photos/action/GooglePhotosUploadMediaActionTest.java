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
import static com.bytechef.component.google.photos.constant.GooglePhotosConstants.MEDIA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
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

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor =
        forClass(ConfigurationBuilder.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Http mockedHttp = mock(Http.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Response mockedResponse = mock(Response.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<Map<String, List<String>>> mapArgumentCaptor = forClass(Map.class);

    @Test
    void testPerform() {
        GooglePhotosUploadMediaAction.Media media = new GooglePhotosUploadMediaAction.Media(
            "name123", mockedFileEntry);

        when(mockedParameters.getRequiredList(MEDIA, GooglePhotosUploadMediaAction.Media.class))
            .thenReturn(List.of(media));
        when(mockedParameters.getRequiredFileEntry("fileEntry"))
            .thenReturn(mockedFileEntry);
        when(mockedFileEntry.getName())
            .thenReturn("name");
        when(mockedFileEntry.getMimeType())
            .thenReturn("mimeType");
        when(mockedParameters.getRequiredString(ALBUM_ID))
            .thenReturn("abc");

        when(mockedContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Http, Http.Executor> value = httpFunctionArgumentCaptor.getValue();

                return value.apply(mockedHttp);
            });
        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(mapArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn("UPLOAD_TOKEN_123");
        when(mockedResponse.getBody())
            .thenReturn(mockedObject);

        Object result = GooglePhotosUploadMediaAction.perform(mockedParameters, null, mockedContext);

        assertEquals(mockedObject, result);

        List<ContextFunction<Http, Executor>> allCapturedFunctions = httpFunctionArgumentCaptor.getAllValues();

        assertNotNull(allCapturedFunctions);
        assertEquals(2, allCapturedFunctions.size());

        List<ConfigurationBuilder> configurationBuilders = configurationBuilderArgumentCaptor.getAllValues();

        assertEquals(2, configurationBuilders.size());
        ConfigurationBuilder configurationBuilder = configurationBuilders.getFirst();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.TEXT, responseType.getType());

        ConfigurationBuilder configurationBuilder2 = configurationBuilders.getLast();
        Http.Configuration configuration2 = configurationBuilder2.build();
        Http.ResponseType responseType2 = configuration2.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType2.getType());
        assertEquals(List.of("/uploads", "/mediaItems:batchCreate"), stringArgumentCaptor.getAllValues());

        assertEquals(
            List.of(
                Http.Body.of(mockedFileEntry),
                Http.Body.of(
                    Map.of(
                        ALBUM_ID, "abc",
                        "newMediaItems", List.of(
                            Map.of(
                                "simpleMediaItem",
                                Map.of("uploadToken", "UPLOAD_TOKEN_123", "fileName", "name123")))),
                    Http.BodyContentType.JSON)),
            bodyArgumentCaptor.getAllValues());

        assertEquals(
            Map.of(
                "Content-Type", List.of("application/octet-stream"),
                "X-Goog-Upload-Content-Type", List.of("mimeType"),
                "X-Goog-Upload-Protocol", List.of("raw")),
            mapArgumentCaptor.getValue());
    }
}

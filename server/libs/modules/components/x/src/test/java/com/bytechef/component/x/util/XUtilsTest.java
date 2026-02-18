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

package com.bytechef.component.x.util;

import static com.bytechef.component.definition.Context.ContextFunction;
import static com.bytechef.component.definition.Context.Http;
import static com.bytechef.component.x.constant.XConstants.DATA;
import static com.bytechef.component.x.constant.XConstants.ID;
import static com.bytechef.component.x.constant.XConstants.MEDIA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Encoder;
import com.bytechef.component.definition.Context.File;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.TypeReference;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class XUtilsTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final ArgumentCaptor<byte[]> bytesArgumentCaptor = forClass(byte[].class);
    private final ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor =
        forClass(ConfigurationBuilder.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Encoder, Http.Executor>> encoderFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final ArgumentCaptor<FileEntry> fileEntryArgumentCaptor = forClass(FileEntry.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<File, Http.Executor>> fileFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Encoder mockedEncoder = mock(Encoder.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http mockedHttp = mock(Http.class);
    private final File mockedFile = mock(File.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testGetAuthenticatedUserId() {
        when(mockedActionContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Http, Http.Executor> value = httpFunctionArgumentCaptor.getValue();

                return value.apply(mockedHttp);
            });
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(DATA, Map.of(ID, "abc")));

        String result = XUtils.getAuthenticatedUserId(mockedActionContext);

        assertEquals("abc", result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/users/me", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetUserIdByUsername() {
        when(mockedActionContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Http, Http.Executor> value = httpFunctionArgumentCaptor.getValue();

                return value.apply(mockedHttp);
            });
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(DATA, Map.of(ID, "abc")));

        String result = XUtils.getUserIdByUsername(mockedActionContext, "test");

        assertEquals("abc", result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/users/by/username/test", stringArgumentCaptor.getValue());
    }

    @Test
    void testUploadMedia() throws IOException {
        when(mockedActionContext.file(fileFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<File, Http.Executor> value = fileFunctionArgumentCaptor.getValue();

                return value.apply(mockedFile);
            });

        byte[] bytes = {
            1, 2, 3
        };
        when(mockedFile.readAllBytes(fileEntryArgumentCaptor.capture()))
            .thenReturn(bytes);
        when(mockedActionContext.encoder(encoderFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Encoder, Http.Executor> value = encoderFunctionArgumentCaptor.getValue();

                return value.apply(mockedEncoder);
            });
        when(mockedEncoder.base64Encode(bytesArgumentCaptor.capture()))
            .thenReturn("def");

        when(mockedActionContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Http, Http.Executor> value = httpFunctionArgumentCaptor.getValue();

                return value.apply(mockedHttp);
            });
        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(DATA, Map.of(ID, "abc")));

        String result = XUtils.uploadMedia(mockedActionContext, mockedFileEntry, "testCategory");

        assertEquals("abc", result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();
        ContextFunction<Encoder, Http.Executor> encoderContextFunction = encoderFunctionArgumentCaptor.getValue();
        ContextFunction<File, Http.Executor> fileContextFunction = fileFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);
        assertNotNull(fileContextFunction);
        assertNotNull(encoderContextFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals(mockedFileEntry, fileEntryArgumentCaptor.getValue());
        assertEquals(bytes, bytesArgumentCaptor.getValue());
        assertEquals("/media/upload", stringArgumentCaptor.getValue());
        assertEquals(
            Http.Body.of(Map.of(MEDIA, "def", "media_category", "testCategory"), Http.BodyContentType.JSON),
            bodyArgumentCaptor.getValue());
    }
}

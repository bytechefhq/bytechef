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

package com.bytechef.component.dropbox.util;

import static com.bytechef.component.dropbox.constant.DropboxConstants.AUTORENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.FILENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.FROM_PATH;
import static com.bytechef.component.dropbox.constant.DropboxConstants.MUTE;
import static com.bytechef.component.dropbox.constant.DropboxConstants.PATH;
import static com.bytechef.component.dropbox.constant.DropboxConstants.STRICT_CONFLICT;
import static com.bytechef.component.dropbox.constant.DropboxConstants.TO_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Context.Json;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class DropboxUtilsTest {

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Json, Executor>> jsonFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<Map<String, ?>> mapArgumentCaptor = forClass(Map.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Object mockedObject = mock(Object.class);
    private final Json mockedJson = mock(Json.class);
    private Parameters mockedParameters;
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testCopy(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        mockedParameters = MockParametersFactory.create(
            Map.of(FROM_PATH, "/folder1/test.txt", TO_PATH, "/folder2/test2.txt", AUTORENAME, false));

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(mockedObject);

        Object result = DropboxUtils.copy(mockedParameters, mockedContext);

        assertEquals(mockedObject, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("https://api.dropboxapi.com/2/files/copy_v2", stringArgumentCaptor.getValue());
        assertEquals(
            Body.of(
                Map.of(FROM_PATH, "/folder1/test.txt", TO_PATH, "/folder2/test2.txt", AUTORENAME, false),
                BodyContentType.JSON),
            bodyArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }

    @Test
    void testDelete(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        mockedParameters = MockParametersFactory.create(Map.of(PATH, "/folder1/sourceFile.txt"));

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(mockedObject);

        Object result = DropboxUtils.delete(mockedParameters, mockedContext);

        assertEquals(mockedObject, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("https://api.dropboxapi.com/2/files/delete_v2", stringArgumentCaptor.getValue());
        assertEquals(
            Body.of(Map.of(PATH, "/folder1/sourceFile.txt"), BodyContentType.JSON), bodyArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }

    @Test
    void testMove(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        mockedParameters = MockParametersFactory.create(
            Map.of(FROM_PATH, "/folder1/test.txt", TO_PATH, "/folder2/test2.txt", AUTORENAME, false));

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(mockedObject);

        Object result = DropboxUtils.move(mockedParameters, mockedContext);

        assertEquals(mockedObject, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("https://api.dropboxapi.com/2/files/move_v2", stringArgumentCaptor.getValue());
        assertEquals(
            Body.of(
                Map.of(FROM_PATH, "/folder1/test.txt", TO_PATH, "/folder2/test2.txt", AUTORENAME, false),
                BodyContentType.JSON),
            bodyArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUploadFile(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        mockedParameters = MockParametersFactory.create(
            Map.of(AUTORENAME, false, MUTE, false, PATH, "/test", FILENAME, "test.txt", STRICT_CONFLICT, false));

        when(mockedContext.json(jsonFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Json, Executor> value = jsonFunctionArgumentCaptor.getValue();

                return value.apply(mockedJson);
            });
        when(mockedJson.write(mapArgumentCaptor.capture()))
            .thenReturn("jsonString");
        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers((Map<String, List<String>>) mapArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(mockedObject);

        Object result = DropboxUtils.uploadFile(mockedParameters, mockedContext, mockedFileEntry);

        assertEquals(mockedObject, result);
        assertNotNull(jsonFunctionArgumentCaptor.getValue());
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("https://content.dropboxapi.com/2/files/upload", stringArgumentCaptor.getValue());
        assertEquals(Body.of(mockedFileEntry, "application/octet-stream"), bodyArgumentCaptor.getValue());

        Map<String, List<String>> expectedHeaders = Map.of("Dropbox-API-Arg", List.of("jsonString"));

        assertEquals(List.of(Map.of(
            AUTORENAME, false,
            "mode", "add",
            MUTE, false,
            PATH, "/test/test.txt", STRICT_CONFLICT, false), expectedHeaders), mapArgumentCaptor.getAllValues());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }
}

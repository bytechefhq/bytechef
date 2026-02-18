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

package com.bytechef.component.youtube.action;

import static com.bytechef.component.youtube.constant.YouTubeConstants.CATEGORY_ID;
import static com.bytechef.component.youtube.constant.YouTubeConstants.DESCRIPTION;
import static com.bytechef.component.youtube.constant.YouTubeConstants.FILE;
import static com.bytechef.component.youtube.constant.YouTubeConstants.LOCATION;
import static com.bytechef.component.youtube.constant.YouTubeConstants.PRIVACY_STATUS;
import static com.bytechef.component.youtube.constant.YouTubeConstants.SNIPPET;
import static com.bytechef.component.youtube.constant.YouTubeConstants.STATUS;
import static com.bytechef.component.youtube.constant.YouTubeConstants.TAGS;
import static com.bytechef.component.youtube.constant.YouTubeConstants.TITLE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
class YouTubeUploadVideoActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FILE, mockedFileEntry, TITLE, "testTitle", DESCRIPTION, "testDescription", PRIVACY_STATUS,
            "private", TAGS, List.of(), CATEGORY_ID, "2"));
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Object[]> queryArgumentCaptor = ArgumentCaptor.forClass(Object[].class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedHttp.put(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getHeaders())
            .thenReturn(Map.of(LOCATION, List.of("url")));
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(SNIPPET, Map.of()));

        Object result = YouTubeUploadVideoAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(Map.of(), result);

        List<String> expectedStrings = List.of(
            "https://www.googleapis.com/upload/youtube/v3/videos", "Content-Type", "application/octet-stream", "url",
            "Content-Type", "application/octet-stream");

        assertEquals(expectedStrings, stringArgumentCaptor.getAllValues());

        Object[] queryArguments = queryArgumentCaptor.getValue();
        Object[] expectedQueryArguments = {
            "uploadType", "resumable", "part", "snippet,status"
        };

        assertArrayEquals(expectedQueryArguments, queryArguments);

        Map<String, Object> body1 = Map.of(SNIPPET, Map.of(
            CATEGORY_ID, "2",
            DESCRIPTION, "testDescription",
            TITLE, "testTitle",
            TAGS, List.of()),
            STATUS, Map.of(PRIVACY_STATUS, "private"));

        List<Body> bodies = bodyArgumentCaptor.getAllValues();

        assertEquals(body1, bodies.get(0)
            .getContent());
        assertEquals(mockedFileEntry, bodies.get(1)
            .getContent());

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Http.Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
    }
}

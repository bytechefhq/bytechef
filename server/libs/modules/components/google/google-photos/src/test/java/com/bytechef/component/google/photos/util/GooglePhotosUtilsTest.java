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

package com.bytechef.component.google.photos.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.google.photos.constant.GooglePhotosConstants.PAGE_SIZE;
import static com.bytechef.component.google.photos.constant.GooglePhotosConstants.PAGE_TOKEN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class GooglePhotosUtilsTest {

    private final ArgumentCaptor<Configuration.ConfigurationBuilder> configurationBuilderArgumentCaptor =
        forClass(Configuration.ConfigurationBuilder.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final List<Option<String>> expectedOptions = List.of(
        option("test1", "1"),
        option("test2", "2"));
    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http mockedHttp = mock(Http.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testGetConferenceRecordsOptions() {
        when(mockedContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                Context.ContextFunction<Http, Http.Executor> value = httpFunctionArgumentCaptor.getValue();

                return value.apply(mockedHttp);
            });
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of(
                    "albums", List.of(Map.of("title", "test1", "id", "1")),
                    "nextPageToken", "t1"),
                Map.of("albums", List.of(Map.of("title", "test2", "id", "2"))));

        List<Option<String>> result = GooglePhotosUtils.getAlbumsOptions(
            mockedParameters, null, null, null, mockedContext);

        assertThat(result, Matchers.containsInAnyOrder(expectedOptions.toArray()));

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals(List.of("/albums", "/albums"), stringArgumentCaptor.getAllValues());

        List<Object[]> allQueryParams = objectsArgumentCaptor.getAllValues();

        assertEquals(2, allQueryParams.size());

        Object[] queryParameters1 = {
            PAGE_SIZE, 50,
            PAGE_TOKEN, null
        };
        Object[] queryParameters2 = {
            PAGE_SIZE, 50,
            PAGE_TOKEN, "t1"
        };

        assertArrayEquals(queryParameters1, allQueryParams.get(0));
        assertArrayEquals(queryParameters2, allQueryParams.get(1));
    }
}

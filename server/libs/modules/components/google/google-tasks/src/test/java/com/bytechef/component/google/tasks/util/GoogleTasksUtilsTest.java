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

package com.bytechef.component.google.tasks.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.LIST_ID;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.MAX_RESULTS;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.NEXT_PAGE_TOKEN;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.PAGE_TOKEN;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class GoogleTasksUtilsTest {

    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(LIST_ID, "abc"));
    private final List<Option<String>> expectedOptions = List.of(
        option("List 1", "list1"), option("List 2", "list2"));
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);
    private final Map<String, Object> responseMap = Map.of(
        "items", List.of(
            Map.of("title", "List 1", "id", "list1"),
            Map.of("title", "List 2", "id", "list2")),
        NEXT_PAGE_TOKEN, "t1");
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @BeforeEach
    void beforeEach(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap)
            .thenReturn(Map.of());
    }

    @Test
    void getListsIdOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        List<Option<String>> result = GoogleTasksUtils.getListsIdOptions(
            mockedParameters, null, null, null, mockedContext);

        assertEquals(expectedOptions, result);

        performCommonAssertions(
            "/users/@me/lists", httpFunctionArgumentCaptor, configurationBuilderArgumentCaptor);
    }

    @Test
    void getTasksIdOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        List<Option<String>> result = GoogleTasksUtils.getTasksIdOptions(
            mockedParameters, null, null, null, mockedContext);

        assertEquals(expectedOptions, result);

        performCommonAssertions(
            "/lists/abc/tasks", httpFunctionArgumentCaptor, configurationBuilderArgumentCaptor);
    }

    private void performCommonAssertions(
        String expectedUrl, ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Http.Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals(expectedUrl, stringArgumentCaptor.getValue());

        List<Object[]> allQueryParams = objectsArgumentCaptor.getAllValues();

        assertEquals(2, allQueryParams.size());

        Object[] queryParameters1 = {
            PAGE_TOKEN, null, MAX_RESULTS, 100
        };
        Object[] queryParameters2 = {
            PAGE_TOKEN, "t1", MAX_RESULTS, 100
        };

        assertArrayEquals(queryParameters1, allQueryParams.get(0));
        assertArrayEquals(queryParameters2, allQueryParams.get(1));
    }
}

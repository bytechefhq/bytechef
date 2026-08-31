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

package com.bytechef.component.browser.use.util;

import static com.bytechef.component.browser.use.constant.BrowserUseConstants.CURSOR;
import static com.bytechef.component.browser.use.constant.BrowserUseConstants.ID;
import static com.bytechef.component.browser.use.constant.BrowserUseConstants.LIMIT;
import static com.bytechef.component.browser.use.constant.BrowserUseConstants.PAGE;
import static com.bytechef.component.browser.use.constant.BrowserUseConstants.PAGE_SIZE;
import static com.bytechef.component.browser.use.constant.BrowserUseConstants.TITLE;
import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
@ExtendWith(MockContextSetupExtension.class)
class BrowserUseUtilsTest {

    private final List<Option<String>> expectedOptions = List.of(
        option("s1", "s1"), option("s2", "s2"));
    private final List<Option<String>> expectedRunOptions = List.of(
        option("t1", "r1"), option("t2", "r2"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);

    @Test
    void testGetSessionIdOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(
                "sessions", List.of(Map.of(ID, "s1", TITLE, "s1"), Map.of(ID, "s2", TITLE, "s2")), "total", 2));

        List<Option<String>> result = BrowserUseUtils.getSessionIdOptions(
            null, null, null, null, mockedContext);

        assertEquals(expectedOptions, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/api/v3/sessions", stringArgumentCaptor.getValue());

        List<Object[]> objectsArgumentCaptorAllValues = objectsArgumentCaptor.getAllValues();

        Object[] objects = {
            PAGE, 1, PAGE_SIZE, 20
        };

        assertEquals(1, objectsArgumentCaptorAllValues.size());
        assertArrayEquals(objects, objectsArgumentCaptorAllValues.getFirst());
    }

    @Test
    void testGetRunIdOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(
                "runs", List.of(Map.of(ID, "r1", TITLE, "t1"), Map.of(ID, "r2", TITLE, "t2")), "hasMore", false));

        List<Option<String>> result = BrowserUseUtils.getRunIdOptions(
            null, null, null, null, mockedContext);

        assertEquals(expectedRunOptions, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/api/v4/runs", stringArgumentCaptor.getValue());

        List<Object[]> objectsArgumentCaptorAllValues = objectsArgumentCaptor.getAllValues();

        Object[] objects = {
            LIMIT, 50, CURSOR, null
        };

        assertEquals(1, objectsArgumentCaptorAllValues.size());
        assertArrayEquals(objects, objectsArgumentCaptorAllValues.getFirst());
    }
}

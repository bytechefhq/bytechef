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

package com.bytechef.component.zenrows.action;

import static com.bytechef.component.zenrows.constant.ZenRowsConstants.CSS_EXTRACTOR;
import static com.bytechef.component.zenrows.constant.ZenRowsConstants.KEY;
import static com.bytechef.component.zenrows.constant.ZenRowsConstants.URL;
import static com.bytechef.component.zenrows.constant.ZenRowsConstants.VALUE;
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
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Context.Json;
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
class ZenRowsScrapeUrlWithCssSelectorActionTest {

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Json, Executor>> jsonFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final Json mockedJson = mock(Json.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(URL, "mockUrl", CSS_EXTRACTOR, List.of(Map.of(KEY, "key1", VALUE, "value1"))));
    private final ArgumentCaptor<Object> objectArgumentCaptor = forClass(Object.class);
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        String stringResponse = "scrapedUrl";

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(stringResponse);

        when(mockedContext.json(jsonFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Json, Executor> value = jsonFunctionArgumentCaptor.getValue();

                return value.apply(mockedJson);
            });
        when(mockedJson.write(objectArgumentCaptor.capture()))
            .thenReturn("json");

        String result = ZenRowsScrapeUrlWithCssSelectorAction.perform(
            mockedParameters, mockedParameters, mockedContext);

        assertEquals(stringResponse, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("", stringArgumentCaptor.getValue());
        assertNotNull(jsonFunctionArgumentCaptor.getValue());
        assertEquals(Map.of("key1", "value1"), objectArgumentCaptor.getValue());

        Object[] expectedQueryParameters = {
            URL, "mockUrl", CSS_EXTRACTOR, "json"
        };

        assertArrayEquals(expectedQueryParameters, objectsArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.TEXT, configuration.getResponseType());
    }
}

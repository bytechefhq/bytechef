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

package com.bytechef.component.brave.action;

import static com.bytechef.component.brave.constant.BraveConstants.COUNT;
import static com.bytechef.component.brave.constant.BraveConstants.COUNTRY;
import static com.bytechef.component.brave.constant.BraveConstants.FRESHNESS;
import static com.bytechef.component.brave.constant.BraveConstants.OFFSET;
import static com.bytechef.component.brave.constant.BraveConstants.OPERATORS;
import static com.bytechef.component.brave.constant.BraveConstants.Q;
import static com.bytechef.component.brave.constant.BraveConstants.RESULT_FILTER;
import static com.bytechef.component.brave.constant.BraveConstants.SAFESEARCH;
import static com.bytechef.component.brave.constant.BraveConstants.SEARCH_LANG;
import static com.bytechef.component.brave.constant.BraveConstants.SUMMARY;
import static com.bytechef.component.definition.Authorization.API_TOKEN;
import static com.bytechef.component.definition.Context.ContextFunction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Marko Krišković
 */
@ExtendWith(MockContextSetupExtension.class)
class BraveWebSearchActionTest {

    private final Parameters mockedConnectionParameters = MockParametersFactory.create(
        Map.of(API_TOKEN, "test-api-token"));
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(Q, "test search", COUNT, 10, SAFESEARCH, "moderate"));
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<Map<String, List<String>>> headersArgumentCaptor = forClass(Map.class);
    private final ArgumentCaptor<Object[]> queryArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(headersArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(Map.of("query", Map.of("original", "test search")));

        Object result = BraveWebSearchAction.perform(mockedParameters, mockedConnectionParameters, mockedContext);

        assertEquals(Map.of("query", Map.of("original", "test search")), result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Http.Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/web/search", stringArgumentCaptor.getValue());
        assertEquals(
            Map.of(
                "Accept", List.of("application/json"),
                "Accept-Encoding", List.of("gzip"),
                "X-Subscription-Token", List.of("test-api-token")),
            headersArgumentCaptor.getValue());
        assertEquals(
            Arrays.asList(
                Q, "test search",
                COUNT, 10,
                OFFSET, null,
                SAFESEARCH, "moderate",
                FRESHNESS, null,
                RESULT_FILTER, null,
                SUMMARY, null,
                OPERATORS, null,
                COUNTRY, null,
                SEARCH_LANG, null),
            Arrays.asList(queryArgumentCaptor.getValue()));
    }
}

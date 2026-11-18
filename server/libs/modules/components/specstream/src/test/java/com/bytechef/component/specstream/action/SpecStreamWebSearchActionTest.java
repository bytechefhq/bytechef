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

package com.bytechef.component.specstream.action;

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.COUNTRY_CODE;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.IMAGE_SEARCH;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.LANGUAGE_CODE;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.NEWS_SEARCH;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.NUMBER_OF_SOURCES;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.RESULT_PAGE;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.RETURN_PAGE_PASSAGES;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.SAFE_SEARCH_LEVEL;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.SEARCH_QUERY;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.SEARCH_TIER;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.USE_HTTP_POST;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
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
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Tarek Etman
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class SpecStreamWebSearchActionTest {

    private final Parameters mockedConnectionParameters = MockParametersFactory.create(
        Map.of(TOKEN, "test-api-token"));
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(SEARCH_QUERY, "Who is the CEO of Stripe", SEARCH_TIER, "fast", NUMBER_OF_SOURCES, 5));
    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<Object[]> queryArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerformWithGet(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> expectedBody = Map.of(
            "data", Map.of("kind", "fact", "query", "Who is the CEO of Stripe"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(expectedBody);

        Object result = SpecStreamWebSearchAction.perform(mockedParameters, mockedConnectionParameters, mockedContext);

        assertEquals(expectedBody, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/web-search", stringArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());

        Object[] expectedQueryParameters = {
            SEARCH_QUERY, "Who is the CEO of Stripe",
            SEARCH_TIER, "fast",
            NUMBER_OF_SOURCES, 5,
            RESULT_PAGE, null,
            RETURN_PAGE_PASSAGES, null,
            NEWS_SEARCH, null,
            COUNTRY_CODE, null,
            LANGUAGE_CODE, null,
            SAFE_SEARCH_LEVEL, null
        };

        assertArrayEquals(expectedQueryParameters, queryArgumentCaptor.getValue());
    }

    @Test
    void testPerformWithPost(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedPostParameters = MockParametersFactory.create(
            Map.of(
                SEARCH_QUERY, "Stripe logo", SEARCH_TIER, "fast", IMAGE_SEARCH, true, USE_HTTP_POST, true));

        Map<String, Object> expectedBody = Map.of("data", Map.of("kind", "images", "query", "Stripe logo"));

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(expectedBody);

        Object result = SpecStreamWebSearchAction.perform(
            mockedPostParameters, mockedConnectionParameters, mockedContext);

        assertEquals(expectedBody, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/web-search", stringArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());

        Body body = bodyArgumentCaptor.getValue();

        assertEquals(BodyContentType.JSON, body.getContentType());

        Map<String, Object> expectedContent = Map.of(
            SEARCH_QUERY, "Stripe logo", SEARCH_TIER, "fast", IMAGE_SEARCH, true);

        assertEquals(expectedContent, body.getContent());
    }

    @Test
    void testPerformWithImageSearchUsesPost(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Parameters mockedImageSearchParameters = MockParametersFactory.create(
            Map.of(SEARCH_QUERY, "Stripe logo", IMAGE_SEARCH, true, USE_HTTP_POST, false));

        Map<String, Object> expectedBody = Map.of("data", Map.of("images", true, "query", "Stripe logo"));

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(expectedBody);

        Object result = SpecStreamWebSearchAction.perform(
            mockedImageSearchParameters, mockedConnectionParameters, mockedContext);

        assertEquals(expectedBody, result);
        assertEquals("/web-search", stringArgumentCaptor.getValue());

        Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of(SEARCH_QUERY, "Stripe logo", IMAGE_SEARCH, true), body.getContent());
    }
}

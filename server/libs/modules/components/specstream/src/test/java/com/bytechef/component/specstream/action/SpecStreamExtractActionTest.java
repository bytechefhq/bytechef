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
import static com.bytechef.component.specstream.constant.SpecStreamConstants.ADDITIONAL_RESULT_FIELDS;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.PAGE_URLS;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.TEXT_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Tarek Etman
 */
@ExtendWith(MockContextSetupExtension.class)
class SpecStreamExtractActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final Parameters mockedConnectionParameters = MockParametersFactory.create(
        Map.of(TOKEN, "test-api-token"));
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            PAGE_URLS, List.of("https://en.wikipedia.org/wiki/Narbonne"),
            TEXT_FORMAT, "markdown",
            ADDITIONAL_RESULT_FIELDS, List.of("links")));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> expectedBody = Map.of(
            "data", Map.of("results", List.of(Map.of("url", "https://en.wikipedia.org/wiki/Narbonne"))));

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(expectedBody);

        Object result = SpecStreamExtractAction.perform(mockedParameters, mockedConnectionParameters, mockedContext);

        assertEquals(expectedBody, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/extract", stringArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals(
            Body.of(
                PAGE_URLS, List.of("https://en.wikipedia.org/wiki/Narbonne"),
                TEXT_FORMAT, "markdown",
                ADDITIONAL_RESULT_FIELDS, List.of("links")),
            bodyArgumentCaptor.getValue());
    }

    @Test
    void testPerformWithoutOptionalParameters(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Parameters minimalParameters = MockParametersFactory.create(
            Map.of(PAGE_URLS, List.of("https://example.com")));
        Map<String, Object> expectedBody = Map.of("data", Map.of("results", List.of()));

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(expectedBody);

        Object result = SpecStreamExtractAction.perform(minimalParameters, mockedConnectionParameters, mockedContext);

        assertEquals(expectedBody, result);
        assertEquals(Body.of(PAGE_URLS, List.of("https://example.com")), bodyArgumentCaptor.getValue());
    }
}

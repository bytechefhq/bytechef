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
import static com.bytechef.component.specstream.constant.SpecStreamConstants.COUNTRY;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.IMAGES;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.LANGUAGE;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.NEWS;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.NUM;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.PAGE;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.PASSAGES;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.Q;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.SAFE;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.SPEED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
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
 */
@ExtendWith(MockContextSetupExtension.class)
class SpecStreamWebSearchActionTest {

    private final Parameters mockedConnectionParameters = MockParametersFactory.create(
        Map.of(TOKEN, "test-api-token"));
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(Q, "Who is the CEO of Stripe", SPEED, "fast", NUM, 5));
    private final ArgumentCaptor<Object[]> queryArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
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
            Q, "Who is the CEO of Stripe",
            SPEED, "fast",
            NUM, 5,
            PAGE, null,
            PASSAGES, null,
            NEWS, null,
            IMAGES, null,
            COUNTRY, null,
            LANGUAGE, null,
            SAFE, null
        };

        assertArrayEquals(expectedQueryParameters, queryArgumentCaptor.getValue());
    }
}

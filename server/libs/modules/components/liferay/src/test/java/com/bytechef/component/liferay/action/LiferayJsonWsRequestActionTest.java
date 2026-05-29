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

package com.bytechef.component.liferay.action;

import static com.bytechef.component.liferay.constant.LiferayConstants.CONTEXT_NAME;
import static com.bytechef.component.liferay.constant.LiferayConstants.ENDPOINT;
import static com.bytechef.component.liferay.constant.LiferayConstants.METHOD;
import static com.bytechef.component.liferay.constant.LiferayConstants.PARAMETERS;
import static com.bytechef.component.liferay.constant.LiferayConstants.SERVICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.liferay.util.LiferayUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Marija Horvat
 */
@ExtendWith(MockContextSetupExtension.class)
class LiferayJsonWsRequestActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(CONTEXT_NAME, "portal", SERVICE, 1, PARAMETERS, Map.of("key", "value")));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final ArgumentCaptor<String> contextNameArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Long> longArgumentCaptor = forClass(Long.class);

    @Test
    void testPerformGet(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, String> responseMap = Map.of(METHOD, "GET", ENDPOINT, "/test");

        try (MockedStatic<LiferayUtils> mockedLiferayUtils = Mockito.mockStatic(LiferayUtils.class)) {

            mockedLiferayUtils
                .when(() -> LiferayUtils.getServiceHttpData(
                    contextArgumentCaptor.capture(), contextNameArgumentCaptor.capture(), longArgumentCaptor.capture()))
                .thenReturn(responseMap);

            when(mockedHttp.get(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(Map.of("success", true));

            Object result = LiferayJsonWsRequestAction.perform(mockedParameters, null, mockedContext);
            assertEquals(Map.of("success", true), result);
            assertNotNull(httpFunctionArgumentCaptor.getValue());

            assertEquals(mockedContext, contextArgumentCaptor.getValue());
            assertEquals("portal", contextNameArgumentCaptor.getValue());
            assertEquals(1, longArgumentCaptor.getValue());

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();

            assertEquals(Http.ResponseType.JSON, configuration.getResponseType());
            assertEquals("/api/jsonws/test", stringArgumentCaptor.getValue());

            Map<String, Object> expectedBody = Map.of("key", "value");

            assertEquals(Body.of(expectedBody, Http.BodyContentType.FORM_DATA), bodyArgumentCaptor.getValue());
        }
    }

    @Test
    void testPerformPost(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, String> responseMap = Map.of(METHOD, "POST", ENDPOINT, "/test");

        try (MockedStatic<LiferayUtils> mockedLiferayUtils = Mockito.mockStatic(LiferayUtils.class)) {

            mockedLiferayUtils
                .when(() -> LiferayUtils.getServiceHttpData(
                    contextArgumentCaptor.capture(), contextNameArgumentCaptor.capture(), longArgumentCaptor.capture()))
                .thenReturn(responseMap);

            when(mockedHttp.post(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(Map.of("success", true));

            Object result = LiferayJsonWsRequestAction.perform(mockedParameters, null, mockedContext);
            assertEquals(Map.of("success", true), result);
            assertNotNull(httpFunctionArgumentCaptor.getValue());

            assertEquals(mockedContext, contextArgumentCaptor.getValue());
            assertEquals("portal", contextNameArgumentCaptor.getValue());
            assertEquals(1, longArgumentCaptor.getValue());

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();

            assertEquals(Http.ResponseType.JSON, configuration.getResponseType());
            assertEquals("/api/jsonws/test", stringArgumentCaptor.getValue());

            Map<String, Object> expectedBody = Map.of("key", "value");

            assertEquals(Body.of(expectedBody, Http.BodyContentType.FORM_DATA), bodyArgumentCaptor.getValue());
        }
    }
}

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

import static com.bytechef.component.definition.ConnectionDefinition.BASE_URI;
import static com.bytechef.component.liferay.constant.LiferayConstants.APPLICATION;
import static com.bytechef.component.liferay.constant.LiferayConstants.BODY;
import static com.bytechef.component.liferay.constant.LiferayConstants.ENDPOINT;
import static com.bytechef.component.liferay.constant.LiferayConstants.HEADER;
import static com.bytechef.component.liferay.constant.LiferayConstants.HIDDEN_PROPERTIES;
import static com.bytechef.component.liferay.constant.LiferayConstants.PATH;
import static com.bytechef.component.liferay.constant.LiferayConstants.PROPERTIES;
import static com.bytechef.component.liferay.constant.LiferayConstants.QUERY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
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
class LiferayHeadlessActionTest {

    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<Map<String, List<String>>> headersArgumentCaptor = forClass(Map.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<Map<String, List<String>>> queryArgumentCaptor = forClass(Map.class);
    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);

    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(
        APPLICATION, "application",
        ENDPOINT, "POST /v1.0/endpoint/{userAccountId}",
        PROPERTIES, Map.of(
            "userAccountId", "12345",
            "headerValue", "header-test",
            "queryValue", "query-test",
            "bodyValue", "body-test",
            HIDDEN_PROPERTIES, Map.of(
                PATH, List.of("userAccountId"),
                HEADER, List.of("headerValue"),
                QUERY, List.of("queryValue"),
                BODY, List.of("bodyValue")))));

    private final Parameters connectionParameters = MockParametersFactory.create(Map.of(
        BASE_URI, "http://localhost:8080"));

    @Test
    void testPerform(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(headersArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(Map.of("success", true));

        Object result = LiferayHeadlessAction.perform(mockedParameters, connectionParameters, mockedContext);

        assertEquals(Map.of("success", true), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(Http.ResponseType.JSON, configuration.getResponseType());
        assertEquals("http://localhost:8080/o/application/v1.0/endpoint/12345", stringArgumentCaptor.getValue());

        Map<String, Object> expectedBody = Map.of("bodyValue", "body-test");

        assertEquals(Body.of(expectedBody, Http.BodyContentType.JSON), bodyArgumentCaptor.getValue());

        Map<String, Object> expectedHeader = Map.of("headerValue", List.of("header-test"));
        assertEquals(expectedHeader, headersArgumentCaptor.getValue());
        Map<String, Object> expectedQuery = Map.of("queryValue", List.of("query-test"));
        assertEquals(expectedQuery, queryArgumentCaptor.getValue());
    }
}

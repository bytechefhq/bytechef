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

package com.bytechef.component.http.client.util;

import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.http.client.constant.HttpClientConstants.BODY;
import static com.bytechef.component.http.client.constant.HttpClientConstants.BODY_CONTENT;
import static com.bytechef.component.http.client.constant.HttpClientConstants.BODY_CONTENT_MIME_TYPE;
import static com.bytechef.component.http.client.constant.HttpClientConstants.BODY_CONTENT_TYPE;
import static com.bytechef.component.http.client.constant.HttpClientConstants.FULL_RESPONSE;
import static com.bytechef.component.http.client.constant.HttpClientConstants.URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.RequestMethod;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class HttpClientActionUtilsTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor =
        forClass(ConfigurationBuilder.class);
    private final ArgumentCaptor<ContextFunction<Http, Executor>> contextFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final ArgumentCaptor<Map<String, List<String>>> mapArgumentCaptor = forClass(Map.class);
    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Http mockedHttp = mock(Http.class);
    private final Response mockedResponse = mock(Response.class);
    private final ArgumentCaptor<Http.RequestMethod> requestMethodArgumentCaptor = forClass(Http.RequestMethod.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testExecuteReturnsResponseBodyWhenFullResponseFalseAndNoBody() throws Exception {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(URI, "http://example.com"));

        Object expectedBody = Map.of("ok", true);

        when(mockedContext.http(contextFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> contextFunctionArgumentCaptor.getValue()
                .apply(mockedHttp));
        when(mockedHttp.exchange(stringArgumentCaptor.capture(), requestMethodArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(mapArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(mapArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody())
            .thenReturn(expectedBody);

        Object result = HttpClientActionUtils.execute(mockedParameters, RequestMethod.GET, mockedContext);

        assertEquals(expectedBody, result);

        ConfigurationBuilder expectedConfiguratioinBuilder = Http.allowUnauthorizedCerts(false)
            .filename(null)
            .followAllRedirects(false)
            .followRedirect(false)
            .proxy(null)
            .responseType(Http.ResponseType.JSON)
            .timeout(Duration.ofMillis(10000));

        ContextFunction<Http, Executor> capturedFunction = contextFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);
        assertEquals(mockedExecutor, capturedFunction.apply(mockedHttp));
        assertEquals("http://example.com", stringArgumentCaptor.getValue());
        assertEquals(RequestMethod.GET, requestMethodArgumentCaptor.getValue());
        assertEquals(expectedConfiguratioinBuilder, configurationBuilderArgumentCaptor.getValue());
        assertEquals(List.of(Map.of(), Map.of()), mapArgumentCaptor.getAllValues());
        assertNull(bodyArgumentCaptor.getValue());
    }

    @Test
    void testExecuteReturnsFullResponseWhenFlagTrue() throws Exception {
        Parameters mockedParameters =
            MockParametersFactory.create(Map.of(FULL_RESPONSE, true, URI, "http://example.com"));

        when(mockedContext.http(contextFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> contextFunctionArgumentCaptor.getValue()
                .apply(mockedHttp));
        when(mockedHttp.exchange(stringArgumentCaptor.capture(), requestMethodArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(mapArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(mapArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        Object result = HttpClientActionUtils.execute(mockedParameters, RequestMethod.GET, mockedContext);

        assertEquals(mockedResponse, result);

        ConfigurationBuilder expectedConfiguratioinBuilder = Http.allowUnauthorizedCerts(false)
            .filename(null)
            .followAllRedirects(false)
            .followRedirect(false)
            .proxy(null)
            .responseType(Http.ResponseType.JSON)
            .timeout(Duration.ofMillis(10000));

        ContextFunction<Http, Executor> capturedFunction = contextFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);
        assertEquals(mockedExecutor, capturedFunction.apply(mockedHttp));
        assertEquals("http://example.com", stringArgumentCaptor.getValue());
        assertEquals(RequestMethod.GET, requestMethodArgumentCaptor.getValue());
        assertEquals(expectedConfiguratioinBuilder, configurationBuilderArgumentCaptor.getValue());
        assertEquals(List.of(Map.of(), Map.of()), mapArgumentCaptor.getAllValues());
        assertNull(bodyArgumentCaptor.getValue());
    }

    @Test
    void testExecuteWithRawBodyBuildsCorrectHttpBody() throws Exception {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                URI, "http://example.com",
                BODY,
                Map.of(
                    BODY_CONTENT_TYPE, BodyContentType.RAW.name(),
                    BODY_CONTENT, "hello",
                    BODY_CONTENT_MIME_TYPE, "text/plain")));

        when(mockedContext.http(contextFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> contextFunctionArgumentCaptor.getValue()
                .apply(mockedHttp));
        when(mockedHttp.exchange(stringArgumentCaptor.capture(), requestMethodArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(mapArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(mapArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        Object expectedBody = Map.of("ok", true);
        when(mockedResponse.getBody())
            .thenReturn(expectedBody);

        Object result = HttpClientActionUtils.execute(mockedParameters, RequestMethod.POST, mockedContext);

        assertEquals(expectedBody, result);

        ConfigurationBuilder expectedConfiguratioinBuilder = Http.allowUnauthorizedCerts(false)
            .filename(null)
            .followAllRedirects(false)
            .followRedirect(false)
            .proxy(null)
            .responseType(Http.ResponseType.JSON)
            .timeout(Duration.ofMillis(10000));

        ContextFunction<Http, Executor> capturedFunction = contextFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);
        assertEquals(mockedExecutor, capturedFunction.apply(mockedHttp));
        assertEquals("http://example.com", stringArgumentCaptor.getValue());
        assertEquals(RequestMethod.POST, requestMethodArgumentCaptor.getValue());
        assertEquals(expectedConfiguratioinBuilder, configurationBuilderArgumentCaptor.getValue());
        assertEquals(List.of(Map.of(), Map.of()), mapArgumentCaptor.getAllValues());
        assertEquals(Body.of("hello", "text/plain"), bodyArgumentCaptor.getValue());
    }

    @Test
    void testToArrayMergesLists() {
        List<Property> list1 = List.of(string("a"));
        List<Property> list2 = List.of(integer("b"));

        Property[] merged = HttpClientActionUtils.toArray(list1, list2);

        assertEquals(2, merged.length);
        assertEquals("a", merged[0].getName());
        assertEquals("b", merged[1].getName());
    }
}

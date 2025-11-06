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

package com.bytechef.platform.component.util;

import static com.bytechef.commons.util.MapUtils.setObjectMapper;
import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.ProcessErrorResponseFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OutputDefinition;
import com.bytechef.component.definition.Property;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.exception.ExecutionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class OpenApiClientUtilsTest {

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Http.Executor>> contextFunctionArgumentCaptor =
        ArgumentCaptor.forClass(ContextFunction.class);
    private final ArgumentCaptor<Http.Configuration.ConfigurationBuilder> configurationBuilderArgumentCaptor =
        forClass(Http.Configuration.ConfigurationBuilder.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http mockedHttp = mock(Http.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<Http.RequestMethod> requestMethodArgumentCaptor = forClass(Http.RequestMethod.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<Map> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);

    ModifiableStringProperty idProperty = string("id")
        .metadata(Map.of("type", PropertyType.PATH));

    @BeforeAll
    static void initMapUtilsObjectMapper() {
        setObjectMapper(new ObjectMapper());
    }

    @SuppressWarnings("unchecked")
    @BeforeEach
    void beforeEach() {
        when(mockedContext.http(contextFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> contextFunctionArgumentCaptor.getValue()
                .apply(mockedHttp));
        when(mockedHttp.exchange(stringArgumentCaptor.capture(), requestMethodArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(mapArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(mapArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testExecuteBuildsRequestAndReturnsBody() throws Exception {
        Object expectedBody = Map.of("ok", true);

        when(mockedResponse.getStatusCode())
            .thenReturn(200);
        when(mockedResponse.getBody())
            .thenReturn(expectedBody);

        ModifiableStringProperty qProperty = string("q")
            .metadata(Map.of("type", PropertyType.QUERY));
        ModifiableStringProperty xTokenProperty = string("X-Token")
            .metadata(Map.of("type", PropertyType.HEADER));
        ModifiableStringProperty nameProperty = string("name")
            .metadata(Map.of("type", PropertyType.BODY));

        List<Property> properties = List.of(idProperty, qProperty, xTokenProperty, nameProperty);

        Map<String, Object> inputParameters = Map.of(
            "id", "1", "q", "hello world/ok", "X-Token", "abc123", "name", "John");

        Map<String, Object> metadata = Map.of(
            "responseType", Http.ResponseType.JSON, "path", "/users/{id}",
            "method", Http.RequestMethod.POST, "bodyContentType", Http.BodyContentType.JSON);

        Object result = OpenApiClientUtils.execute(inputParameters, properties, null, metadata, null, mockedContext);

        assertEquals(expectedBody, result);
        assertHttpContextAndResponseType();
        assertEquals("/users/1", stringArgumentCaptor.getValue());
        assertEquals(Http.RequestMethod.POST, requestMethodArgumentCaptor.getValue());
        assertEquals(
            List.of(Map.of("X-Token", List.of("abc123")), Map.of("q", List.of("hello+world%2Fok"))),
            mapArgumentCaptor.getAllValues());
        assertEquals(
            Http.Body.of(Map.of("name", "John"), Http.BodyContentType.JSON), bodyArgumentCaptor.getValue());
    }

    @Test
    void testExecuteThrowsProviderException() throws Exception {
        when(mockedResponse.getStatusCode())
            .thenReturn(404);
        when(mockedResponse.getBody())
            .thenReturn("Not Found");

        List<Property> properties = List.of(idProperty);
        Map<String, Object> inputParameters = Map.of("id", "1");
        Map<String, Object> metadata = Map.of(
            "path", "/users/{id}", "method", Http.RequestMethod.GET, "responseType", Http.ResponseType.JSON);

        ProviderException providerException = assertThrows(
            ProviderException.class,
            () -> OpenApiClientUtils.execute(inputParameters, properties, null, metadata, null, mockedContext));

        assertHttpContextAndResponseType();
        assertEquals(404, providerException.getStatusCode());
        assertEquals("Not Found", providerException.getMessage());
        assertEquals("/users/1", stringArgumentCaptor.getValue());
        assertEquals(Http.RequestMethod.GET, requestMethodArgumentCaptor.getValue());
        assertEquals(List.of(Map.of(), Map.of()), mapArgumentCaptor.getAllValues());
        assertNull(bodyArgumentCaptor.getValue());
    }

    @Test
    void testExecuteThrowsExecutionException() throws Exception {
        when(mockedResponse.getStatusCode())
            .thenReturn(400);
        when(mockedResponse.getBody())
            .thenReturn(Map.of("error", "bad"));

        ProcessErrorResponseFunction processErrorResponseFunction =
            (status, body, context) -> new ProviderException(status, String.valueOf(((Map<?, ?>) body).get("error")));

        ModifiableStringProperty idProp = string("id").metadata(Map.of("type", PropertyType.PATH));
        List<Property> properties = List.of(idProp);
        Map<String, Object> inputProperties = Map.of("id", "1");
        Map<String, Object> metadata = Map.of(
            "path", "/users/{id}", "method", Http.RequestMethod.GET, "responseType", Http.ResponseType.JSON);

        ExecutionException executionException = assertThrows(
            ExecutionException.class,
            () -> OpenApiClientUtils.execute(
                inputProperties, properties, null, metadata, processErrorResponseFunction, mockedContext));

        assertInstanceOf(ProviderException.class, executionException.getCause());
        assertEquals(105, executionException.getErrorKey());
        assertEquals("com.bytechef.component.exception.ProviderException: bad", executionException.getMessage());
        assertEquals("/users/1", stringArgumentCaptor.getValue());
        assertEquals(Http.RequestMethod.GET, requestMethodArgumentCaptor.getValue());
        assertEquals(List.of(Map.of(), Map.of()), mapArgumentCaptor.getAllValues());
        assertNull(bodyArgumentCaptor.getValue());

        assertHttpContextAndResponseType();
    }

    @Test
    void testExecuteWithOutputDefinition() throws Exception {
        Object expectedBody = Map.of("ok", true);

        when(mockedResponse.getStatusCode())
            .thenReturn(200);
        when(mockedResponse.getBody())
            .thenReturn(expectedBody);

        ModifiableStringProperty nameProperty = string("name")
            .metadata(Map.of("type", PropertyType.BODY));

        List<Property> properties = List.of(idProperty, nameProperty);
        Map<String, Object> inputParameters = Map.of("id", "1");
        Map<String, Object> metadata = Map.of(
            "responseType", Http.ResponseType.JSON, "path", "/users/{id}", "method", Http.RequestMethod.GET);

        OutputDefinition outputDefinition = OutputDefinition.of(
            object()
                .properties(string("id"))
                .metadata(Map.of("responseType", Http.ResponseType.JSON)));

        Object result = OpenApiClientUtils.execute(
            inputParameters, properties, outputDefinition, metadata, null, mockedContext);

        assertEquals(expectedBody, result);
        assertEquals("/users/1", stringArgumentCaptor.getValue());
        assertEquals(Http.RequestMethod.GET, requestMethodArgumentCaptor.getValue());

        assertHttpContextAndResponseType();
    }

    private void assertHttpContextAndResponseType() throws Exception {
        ContextFunction<Http, Http.Executor> capturedFunction = contextFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);
        assertEquals(mockedExecutor, capturedFunction.apply(mockedHttp));

        Http.Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
    }
}

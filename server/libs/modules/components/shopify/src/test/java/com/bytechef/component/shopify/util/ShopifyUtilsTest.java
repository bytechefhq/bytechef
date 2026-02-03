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

package com.bytechef.component.shopify.util;

import static com.bytechef.component.shopify.constant.ShopifyConstants.QUERY;
import static com.bytechef.component.shopify.constant.ShopifyConstants.VARIABLES;
import static com.bytechef.component.shopify.util.ShopifyUtils.sendGraphQlQuery;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Context.Http.ResponseType.Type;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Monika Domiter
 * @author Nikolina Spehar
 */
class ShopifyUtilsTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor =
        forClass(ConfigurationBuilder.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final Http mockedHttp = mock(Http.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testSendGraphQlQuery() {
        String mockedQuery = "testQuery";
        Map<String, Object> mockedVariables = Map.of();
        Map<String, Object> mockedObject = Map.of("data", Map.of());

        when(mockedContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Http, Executor> value = httpFunctionArgumentCaptor.getValue();

                return value.apply(mockedHttp);
            });
        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedObject);

        Object result =
            assertDoesNotThrow(() -> sendGraphQlQuery(mockedQuery, mockedContext, mockedVariables));

        assertEquals(Map.of(), result);

        ContextFunction<Http, Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();
        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();
        ResponseType responseType = configuration.getResponseType();

        Map<String, Object> expectedBody = Map.of(
            QUERY, mockedQuery,
            VARIABLES, mockedVariables);

        Body body = bodyArgumentCaptor.getValue();

        assertEquals(expectedBody, body.getContent());

        assertEquals(Type.JSON, responseType.getType());
        assertEquals("/2025-10/graphql.json", stringArgumentCaptor.getValue());
    }

    @Test
    void testSendGraphQlQueryThrowsProviderException() {
        when(mockedContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Http, Executor> value = httpFunctionArgumentCaptor.getValue();
                return value.apply(mockedHttp);
            });
        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("errors", List.of(Map.of("message", "Top-level error"))));

        ProviderException ex = assertThrows(
            ProviderException.class,
            () -> sendGraphQlQuery("queryWithErrors", mockedContext, Map.of()));

        assertEquals("Top-level error", ex.getMessage());
    }

    @Test
    void testExecuteGraphQlOperationReturnsContent() {
        String query = "mutation { doSomething }";
        Map<String, Object> variables = Map.of("key", "value");
        String dataKey = "operationResult";

        Map<String, Object> expectedContent = Map.of("id", 123, "status", "OK");

        try (MockedStatic<ShopifyUtils> utils = Mockito.mockStatic(ShopifyUtils.class, Mockito.CALLS_REAL_METHODS)) {
            utils.when(() -> sendGraphQlQuery(query, mockedContext, variables))
                .thenReturn(Map.of(dataKey, expectedContent));

            Object result = ShopifyUtils.executeGraphQlOperation(query, mockedContext, variables, dataKey);

            assertEquals(expectedContent, result);
        }
    }

    @Test
    void testExecuteGraphQlOperationThrowsOnUserErrors() {
        String query = "mutation { doSomething }";
        Map<String, Object> variables = Map.of();
        String dataKey = "operationResult";

        Map<String, Object> contentWithErrors = Map.of(
            "userErrors", List.of(Map.of("message", "User-level error")));

        try (MockedStatic<ShopifyUtils> utils = Mockito.mockStatic(ShopifyUtils.class, Mockito.CALLS_REAL_METHODS)) {
            utils.when(() -> sendGraphQlQuery(query, mockedContext, variables))
                .thenReturn(Map.of(dataKey, contentWithErrors));

            ProviderException ex = assertThrows(
                ProviderException.class,
                () -> ShopifyUtils.executeGraphQlOperation(query, mockedContext, variables, dataKey));

            assertEquals("User-level error", ex.getMessage());
        }
    }
}

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
import static com.bytechef.component.shopify.util.ShopifyUtils.checkForUserError;
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

/**
 * @author Monika Domiter
 * @author Nikolina Spehar
 */
class ShopifyUtilsTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor =
        forClass(Http.Configuration.ConfigurationBuilder.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final Http mockedHttp = mock(Http.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testCheckForUserErrorProviderException() {
        Map<String, Object> mockedContent = Map.of(
            "userErrors", List.of(
                Map.of("message", "Error occurred", "field", "orderId")));

        ProviderException exception = assertThrows(ProviderException.class, () -> checkForUserError(mockedContent));

        assertEquals("Error occurred", exception.getMessage());
    }

    @Test
    void testCheckForUserErrorNoUserError() {
        Map<String, Object> mockedContent = Map.of(
            "data", "some data",
            "status", "success");

        assertDoesNotThrow(() -> checkForUserError(mockedContent));
    }

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

        Map<String, Object> result = sendGraphQlQuery(mockedQuery, mockedContext, mockedVariables);

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
}

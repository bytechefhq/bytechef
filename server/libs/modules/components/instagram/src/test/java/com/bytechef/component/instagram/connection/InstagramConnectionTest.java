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

package com.bytechef.component.instagram.connection;

import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.instagram.connection.InstagramConnection.LONG_LIVED_TOKEN_URL;
import static com.bytechef.component.instagram.connection.InstagramConnection.TOKEN_URL;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class InstagramConnectionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final Parameters connectionParameters = MockParametersFactory.create(
        Map.of(CLIENT_ID, "app-id", CLIENT_SECRET, "app-secret"));
    private final ArgumentCaptor<String> getUrlArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<String> postUrlArgumentCaptor = forClass(String.class);

    @Test
    void testAuthorizationCallbackExchangesForLongLivedToken(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.post(postUrlArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedHttp.get(getUrlArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of("access_token", "short-token", "user_id", 123),
                Map.of("access_token", "long-token", "token_type", "bearer", "expires_in", 5183944));

        AuthorizationCallbackResponse response = InstagramConnection.authorizationCallback(
            connectionParameters, "code-1", "https://example.com/callback", null, mockedContext);

        assertEquals(new AuthorizationCallbackResponse("long-token", null, 5183944L), response);
        assertEquals(TOKEN_URL, postUrlArgumentCaptor.getValue());
        assertEquals(LONG_LIVED_TOKEN_URL, getUrlArgumentCaptor.getValue());
        assertEquals(
            Body.of(
                Map.of(
                    "client_id", "app-id", "client_secret", "app-secret", "code", "code-1",
                    "grant_type", "authorization_code", "redirect_uri", "https://example.com/callback"),
                BodyContentType.FORM_URL_ENCODED),
            bodyArgumentCaptor.getValue());

        Object[] expectedQueryParameters = {
            "grant_type", "ig_exchange_token", "client_secret", "app-secret", "access_token", "short-token"
        };

        assertArrayEquals(expectedQueryParameters, objectsArgumentCaptor.getValue());
    }

    @Test
    void testAuthorizationCallbackFailsWithoutAccessToken(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.post(anyString()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(any(Body.class)))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("error_message", "Invalid code"));

        assertThrows(
            ProviderException.class,
            () -> InstagramConnection.authorizationCallback(
                connectionParameters, "code-1", "https://example.com/callback", null, mockedContext));
    }
}

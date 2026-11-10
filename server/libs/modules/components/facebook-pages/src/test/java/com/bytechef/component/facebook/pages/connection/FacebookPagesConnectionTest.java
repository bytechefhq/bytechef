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

package com.bytechef.component.facebook.pages.connection;

import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class FacebookPagesConnectionTest {

    private final Parameters connectionParameters = MockParametersFactory.create(
        Map.of(CLIENT_ID, "appId", CLIENT_SECRET, "appSecret"));
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = ArgumentCaptor.forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testAuthorizationCallbackExchangesForLongLivedToken(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of("access_token", "shortToken", "expires_in", 3600),
                Map.of("access_token", "longToken", "expires_in", 5_184_000));

        AuthorizationCallbackResponse authorizationCallbackResponse = FacebookPagesConnection.authorizationCallback(
            connectionParameters, "code", "https://app.bytechef.io/callback", null, mockedContext);

        Map<String, ?> result = authorizationCallbackResponse.result();

        assertEquals("longToken", result.get("access_token"));
        assertEquals(5_184_000L, result.get("expires_in"));
        assertEquals(
            List.of(FacebookPagesConnection.TOKEN_URL, FacebookPagesConnection.TOKEN_URL),
            stringArgumentCaptor.getAllValues());

        List<Object[]> queryParameters = objectsArgumentCaptor.getAllValues();

        assertArrayEquals(
            new Object[] {
                "client_id", "appId", "client_secret", "appSecret", "redirect_uri", "https://app.bytechef.io/callback",
                "code", "code"
            },
            queryParameters.get(0));
        assertArrayEquals(
            new Object[] {
                "grant_type", "fb_exchange_token", "client_id", "appId", "client_secret", "appSecret",
                "fb_exchange_token", "shortToken"
            },
            queryParameters.get(1));
    }

    @Test
    void testAuthorizationCallbackFailsWithoutAccessToken(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.get(any(String.class)))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(any(Object[].class)))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getStatusCode())
            .thenReturn(400);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("error", Map.of("message", "Invalid verification code format.")));

        assertThrows(
            ProviderException.class,
            () -> FacebookPagesConnection.authorizationCallback(
                connectionParameters, "code", "https://app.bytechef.io/callback", null, mockedContext));
    }

    @Test
    void testAuthorizationCallbackIsRegistered() {
        List<? extends Authorization> authorizations =
            FacebookPagesConnection.CONNECTION_DEFINITION.getAuthorizations();

        Authorization authorization = authorizations.getFirst();

        assertTrue(authorization.getAuthorizationCallback()
            .isPresent());
    }
}

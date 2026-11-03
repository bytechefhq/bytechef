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

package com.bytechef.component.viber.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class ViberUtilsTest {

    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testCheckStatusReturnsResponseOnZeroStatus() {
        Map<String, Object> response = Map.of("status", 0, "status_message", "ok");

        assertEquals(response, ViberUtils.checkStatus(response));
    }

    @Test
    void testCheckStatusThrowsWithViberMessage() {
        ProviderException providerException = assertThrows(
            ProviderException.class,
            () -> ViberUtils.checkStatus(Map.of("status", 6, "status_message", "receiverNotSubscribed")));

        assertEquals("Viber rejected the request: receiverNotSubscribed (6)", providerException.getMessage());
    }

    @Test
    void testGetAccountInfoPostsToAccountInfoEndpoint(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> responseBody = Map.of("status", 0, "name", "Bot");

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(any(Http.Body.class)))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseBody);

        assertEquals(responseBody, ViberUtils.getAccountInfo(mockedContext));

        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/get_account_info", stringArgumentCaptor.getValue());
        assertEquals(ResponseType.JSON, configurationBuilderArgumentCaptor.getValue()
            .build()
            .getResponseType());
    }

    @Test
    void testResolveSenderNameFallsBackToBotName(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(any(Http.Body.class)))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("status", 0, "name", "Acme Support"));

        assertEquals("Acme Support", ViberUtils.resolveSenderName(" ", mockedContext));
    }

    @Test
    void testResolveSenderNameTruncatesTo28Characters(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        String botName = "abcdefghijklmnopqrstuvwxyz0123456789ABCD";

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(any(Http.Body.class)))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("status", 0, "name", botName));

        String result = ViberUtils.resolveSenderName(null, mockedContext);

        assertEquals(28, result.length());
        assertEquals(botName.substring(0, 28), result);
    }

    @Test
    void testResolveSenderNameFailsWhenViberReturnsNoBotName(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(any(Http.Body.class)))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("status", 0), Map.of("status", 0, "name", " "), Map.of("status", 0, "name", 7));

        for (int attempt = 0; attempt < 3; attempt++) {
            ProviderException providerException = assertThrows(
                ProviderException.class, () -> ViberUtils.resolveSenderName(null, mockedContext));

            assertEquals(
                "Viber did not return a bot name; set a sender name explicitly.", providerException.getMessage());
        }
    }

    @Test
    void testResolveSenderNameUsesProvidedName(Context mockedContext) {
        assertEquals("Support", ViberUtils.resolveSenderName("Support", mockedContext));
    }
}

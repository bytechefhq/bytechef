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

package com.bytechef.component.slack.util;

import static com.bytechef.component.slack.constant.SlackConstants.CHANNEL;
import static com.bytechef.component.slack.constant.SlackConstants.ERROR;
import static com.bytechef.component.slack.constant.SlackConstants.OK;
import static com.bytechef.component.slack.constant.SlackConstants.POST_AT;
import static com.bytechef.component.slack.constant.SlackConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
class SlackSendMessageUtilsTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testSendMessageNormal(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("ok", true));

        Object result = SlackSendMessageUtils.sendMessage(
            "general", "hello", null, null, mockedContext);

        assertEquals(Map.of(OK, true), result);

        ContextFunction<Http, Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Http.Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/chat.postMessage", stringArgumentCaptor.getValue());
        assertEquals(
            Http.Body.of(Map.of(CHANNEL, "general", TEXT, "hello"), Http.BodyContentType.JSON),
            bodyArgumentCaptor.getValue());
    }

    @Test
    void testSendMessageScheduled(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("ok", true));

        try (MockedStatic<SlackUtils> slackUtilsMockedStatic = mockStatic(SlackUtils.class)) {
            slackUtilsMockedStatic.when(() -> SlackUtils.getSlackTimeZone(contextArgumentCaptor.capture()))
                .thenReturn("Europe/Zagreb");

            slackUtilsMockedStatic.when(() -> SlackUtils.getUserId(contextArgumentCaptor.capture()))
                .thenReturn("userId");

            LocalDateTime mockedLocalDateTime = LocalDateTime.of(
                1970, 1, 1, 0, 0, 0);

            Object result = SlackSendMessageUtils.sendMessage(
                "general", "hello", mockedLocalDateTime, null, mockedContext);

            assertEquals(Map.of(OK, true), result);

            ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

            assertNotNull(capturedFunction);

            Http.Configuration.ConfigurationBuilder configurationBuilder =
                configurationBuilderArgumentCaptor.getValue();
            Http.Configuration configuration = configurationBuilder.build();
            Http.ResponseType responseType = configuration.getResponseType();

            assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
            assertEquals("/chat.scheduleMessage", stringArgumentCaptor.getValue());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
            assertEquals(
                Http.Body.of(Map.of(CHANNEL, "general", TEXT, "hello", POST_AT, -3600L), Http.BodyContentType.JSON),
                bodyArgumentCaptor.getValue());
        }
    }

    @Test
    void testSendMessageError(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(OK, false, ERROR, "some_error"));

        ProviderException providerException = assertThrows(
            ProviderException.class,
            () -> SlackSendMessageUtils.sendMessage(
                "general", "hello", null, null, mockedContext));

        assertEquals("some_error", providerException.getMessage());

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Http.Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/chat.postMessage", stringArgumentCaptor.getValue());
        assertEquals(
            Http.Body.of(Map.of(CHANNEL, "general", TEXT, "hello"), Http.BodyContentType.JSON),
            bodyArgumentCaptor.getValue());
    }
}

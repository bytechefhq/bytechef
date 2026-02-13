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

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.slack.constant.SlackConstants.ID;
import static com.bytechef.component.slack.constant.SlackConstants.NAME;
import static com.bytechef.component.slack.constant.SlackConstants.OK;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
class SlackUtilsTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<Http.Configuration.ConfigurationBuilder> configurationBuilderArgumentCaptor =
        forClass(Http.Configuration.ConfigurationBuilder.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http mockedHttp = mock(Http.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @BeforeEach
    void beforeEach() {
        when(mockedActionContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Http, Http.Executor> value = httpFunctionArgumentCaptor.getValue();

                return value.apply(mockedHttp);
            });
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testGetChannelIdOptions() {
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of(
                    OK, true,
                    "channels", List.of(Map.of(NAME, "A", ID, "10")),
                    "response_metadata", Map.of("next_cursor", "abc")))
            .thenReturn(
                Map.of(
                    OK, true,
                    "channels", List.of(Map.of(NAME, "B", ID, "20")),
                    "response_metadata", Map.of("next_cursor", "")));

        List<Option<String>> result = SlackUtils.getChannelIdOptions(
            mockedParameters, null, null, null, mockedActionContext);

        assertEquals(List.of(option("A", "10"), option("B", "20")), result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Http.Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/conversations.list", stringArgumentCaptor.getValue());

        List<Object[]> allCaptured = objectsArgumentCaptor.getAllValues();

        assertEquals(2, allCaptured.size());

        Object[] expectedFirst = {
            "types", "public_channel,private_channel", "exclude_archived", true, "limit", 1000, "cursor", null
        };

        Object[] expectedSecond = {
            "types", "public_channel,private_channel", "exclude_archived", true, "limit", 1000, "cursor", "abc"
        };

        assertArrayEquals(expectedFirst, allCaptured.get(0));
        assertArrayEquals(expectedSecond, allCaptured.get(1));
    }

    @Test
    void testGetUserIdOptions() {
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of(
                    OK, true,
                    "members", List.of(Map.of(NAME, "A", ID, "10")),
                    "response_metadata", Map.of("next_cursor", "abc")))
            .thenReturn(
                Map.of(
                    OK, true,
                    "members", List.of(Map.of(NAME, "B", ID, "20")),
                    "response_metadata", Map.of("next_cursor", "")));

        List<Option<String>> result = SlackUtils.getUserIdOptions(
            mockedParameters, null, null, null, mockedActionContext);

        assertEquals(List.of(option("A", "10"), option("B", "20")), result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Http.Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/users.list", stringArgumentCaptor.getValue());

        List<Object[]> allCaptured = objectsArgumentCaptor.getAllValues();

        assertEquals(2, allCaptured.size());

        Object[] expectedFirst = {
            "limit", 1000, "cursor", null
        };

        Object[] expectedSecond = {
            "limit", 1000, "cursor", "abc"
        };

        assertArrayEquals(expectedFirst, allCaptured.get(0));
        assertArrayEquals(expectedSecond, allCaptured.get(1));
    }

    @Test
    void testGetSlackTimeZone() {
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of("user", Map.of("tz", "timezone")));

        ArgumentCaptor<ActionContext> actionContextArgumentCaptor = forClass(ActionContext.class);

        try (MockedStatic<SlackUtils> slackUtilsMockedStatic = mockStatic(SlackUtils.class, CALLS_REAL_METHODS)) {
            slackUtilsMockedStatic.when(() -> SlackUtils.getUserId(actionContextArgumentCaptor.capture()))
                .thenReturn("userId");

            String result = SlackUtils.getSlackTimeZone(mockedActionContext);

            assertEquals("timezone", result);

            ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

            assertNotNull(capturedFunction);

            Http.Configuration.ConfigurationBuilder configurationBuilder =
                configurationBuilderArgumentCaptor.getValue();
            Http.Configuration configuration = configurationBuilder.build();
            Http.ResponseType responseType = configuration.getResponseType();

            assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
            assertEquals("/users.info", stringArgumentCaptor.getValue());

            Object[] queryParameters = objectsArgumentCaptor.getValue();

            Object[] expectedQueryParameters = {
                "user", "userId"
            };

            assertArrayEquals(expectedQueryParameters, queryParameters);
            assertEquals(mockedActionContext, actionContextArgumentCaptor.getValue());
        }
    }

    @Test
    void testGetUserId() {
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of("user_id", "userId"));

        String result = SlackUtils.getUserId(mockedActionContext);

        assertEquals("userId", result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Http.Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/auth.test", stringArgumentCaptor.getValue());
    }
}

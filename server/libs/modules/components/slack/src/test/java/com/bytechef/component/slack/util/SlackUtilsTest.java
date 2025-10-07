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
import static com.bytechef.component.slack.constant.SlackConstants.CHANNEL;
import static com.bytechef.component.slack.constant.SlackConstants.ID;
import static com.bytechef.component.slack.constant.SlackConstants.NAME;
import static com.bytechef.component.slack.constant.SlackConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Mario Cvjetojevic
 * @author Monika Domiter
 */
class SlackUtilsTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @BeforeEach
    void beforeEach() {
        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testSendMessage() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("ok", true));

        Object result = SlackUtils.sendMessage("general", "hello", null, mockedActionContext);

        assertEquals(Map.of("ok", true), result);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of(CHANNEL, "general", TEXT, "hello"), body.getContent());
    }

    @Test
    void testSendMessageError() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("ok", false, "error", "some_error"));

        ProviderException providerException = assertThrows(
            ProviderException.class,
            () -> SlackUtils.sendMessage("general", "hello", null, mockedActionContext));

        assertEquals("some_error", providerException.getMessage());
    }

    @Test
    void testGetChannelOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("channels", List.of(Map.of(NAME, "abc", ID, "123"))));

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("abc", "123"));

        assertEquals(
            expectedOptions,
            SlackUtils.getChannelOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));
    }

    @Test
    void testGetUserOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("members", List.of(Map.of(NAME, "abc", ID, "123"))));

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("abc", "123"));

        assertEquals(
            expectedOptions,
            SlackUtils.getUserOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));
    }
}

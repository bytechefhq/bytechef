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

package com.bytechef.component.discord.trigger;

import static com.bytechef.component.discord.constant.DiscordConstants.CHANNEL_ID;
import static com.bytechef.component.discord.constant.DiscordConstants.LAST_MESSAGE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Pamod Madubashana
 */
@ExtendWith(MockContextSetupExtension.class)
class DiscordNewMessageTriggerTest {

    private static final String OLDER_MESSAGE_ID = "999999999999999999";
    private static final String OLDEST_MESSAGE_ID = "111111111111111111";
    private static final String NEWER_MESSAGE_ID = "1000000000000000000";

    private final Parameters inputParameters = MockParametersFactory.create(Map.of(CHANNEL_ID, "channelId"));
    private final ArgumentCaptor<String> queryParameterNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> queryParameterValueArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    @SuppressWarnings("unchecked")
    void testPollForEmptyChannel(
        TriggerContext mockedTriggerContext, Http mockedHttp, Http.Executor mockedExecutor,
        Http.Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(
            queryParameterNameArgumentCaptor.capture(), queryParameterValueArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of());

        PollOutput pollOutput = DiscordNewMessageTrigger.poll(
            inputParameters, null, MockParametersFactory.create(Map.of()), mockedTriggerContext);

        assertEquals(new PollOutput(List.of(), Map.of(), false), pollOutput);

        assertNotNull(httpFunctionArgumentCaptor.getValue());

        Configuration configuration = configurationBuilderArgumentCaptor.getValue()
            .build();

        assertEquals(Http.ResponseType.JSON, configuration.getResponseType());
        assertEquals("/channels/channelId/messages", stringArgumentCaptor.getValue());
        assertEquals(List.of("limit"), queryParameterNameArgumentCaptor.getAllValues());
        assertEquals(List.of("1"), queryParameterValueArgumentCaptor.getAllValues());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPollForEditorEnvironmentOnInitialPoll(
        TriggerContext mockedTriggerContext, Http mockedHttp, Http.Executor mockedExecutor,
        Http.Response mockedResponse) {

        Map<String, String> olderMessage = createMessage(OLDER_MESSAGE_ID);
        Map<String, String> newerMessage = createMessage(NEWER_MESSAGE_ID);

        when(mockedTriggerContext.isEditorEnvironment())
            .thenReturn(true);
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(
            queryParameterNameArgumentCaptor.capture(), queryParameterValueArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(olderMessage, newerMessage));

        PollOutput pollOutput = DiscordNewMessageTrigger.poll(
            inputParameters, null, MockParametersFactory.create(Map.of()), mockedTriggerContext);

        assertEquals(
            new PollOutput(List.of(newerMessage), Map.of(LAST_MESSAGE_ID, NEWER_MESSAGE_ID), false), pollOutput);
        assertEquals(List.of("limit"), queryParameterNameArgumentCaptor.getAllValues());
        assertEquals(List.of("1"), queryParameterValueArgumentCaptor.getAllValues());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPollOnInitialPollOutsideEditorEnvironment(
        TriggerContext mockedTriggerContext, Http mockedHttp, Http.Executor mockedExecutor,
        Http.Response mockedResponse) {

        Map<String, String> olderMessage = createMessage(OLDER_MESSAGE_ID);
        Map<String, String> newerMessage = createMessage(NEWER_MESSAGE_ID);

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(
            queryParameterNameArgumentCaptor.capture(), queryParameterValueArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(olderMessage, newerMessage));

        PollOutput pollOutput = DiscordNewMessageTrigger.poll(
            inputParameters, null, MockParametersFactory.create(Map.of()), mockedTriggerContext);

        assertEquals(new PollOutput(List.of(), Map.of(LAST_MESSAGE_ID, NEWER_MESSAGE_ID), false), pollOutput);
        assertEquals(List.of("limit"), queryParameterNameArgumentCaptor.getAllValues());
        assertEquals(List.of("1"), queryParameterValueArgumentCaptor.getAllValues());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPollWithoutNewMessages(
        TriggerContext mockedTriggerContext, Http mockedHttp, Http.Executor mockedExecutor,
        Http.Response mockedResponse) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(
            queryParameterNameArgumentCaptor.capture(), queryParameterValueArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of());

        PollOutput pollOutput = DiscordNewMessageTrigger.poll(
            inputParameters, null, MockParametersFactory.create(Map.of(LAST_MESSAGE_ID, OLDER_MESSAGE_ID)),
            mockedTriggerContext);

        assertEquals(
            new PollOutput(List.of(), Map.of(LAST_MESSAGE_ID, OLDER_MESSAGE_ID), false), pollOutput);
        assertEquals(List.of("limit", "after"), queryParameterNameArgumentCaptor.getAllValues());
        assertEquals(List.of("100", OLDER_MESSAGE_ID), queryParameterValueArgumentCaptor.getAllValues());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPollWithNewMessages(
        TriggerContext mockedTriggerContext, Http mockedHttp, Http.Executor mockedExecutor,
        Http.Response mockedResponse) {

        Map<String, String> oldestMessage = createMessage(OLDEST_MESSAGE_ID);
        Map<String, String> olderMessage = createMessage(OLDER_MESSAGE_ID);
        Map<String, String> newerMessage = createMessage(NEWER_MESSAGE_ID);

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(
            queryParameterNameArgumentCaptor.capture(), queryParameterValueArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(newerMessage, oldestMessage, olderMessage));

        PollOutput pollOutput = DiscordNewMessageTrigger.poll(
            inputParameters, null, MockParametersFactory.create(Map.of(LAST_MESSAGE_ID, OLDEST_MESSAGE_ID)),
            mockedTriggerContext);

        assertEquals(
            new PollOutput(
                List.of(oldestMessage, olderMessage, newerMessage), Map.of(LAST_MESSAGE_ID, NEWER_MESSAGE_ID), false),
            pollOutput);
        assertEquals(List.of("limit", "after"), queryParameterNameArgumentCaptor.getAllValues());
        assertEquals(List.of("100", OLDEST_MESSAGE_ID), queryParameterValueArgumentCaptor.getAllValues());
    }

    private static Map<String, String> createMessage(String messageId) {
        return Map.of("id", messageId, "content", "content of message " + messageId);
    }
}

/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.slack.action;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.slack.constant.SlackConstants.CHANNEL_ID;
import static com.bytechef.component.slack.constant.SlackConstants.TEXT;
import static com.bytechef.component.slack.properties.SlackInputProperties.AS_USER;
import static com.bytechef.component.slack.properties.SlackInputProperties.ATTACHMENTS;
import static com.bytechef.component.slack.properties.SlackInputProperties.BLOCKS;
import static com.bytechef.component.slack.properties.SlackInputProperties.ICON_EMOJI;
import static com.bytechef.component.slack.properties.SlackInputProperties.ICON_URL;
import static com.bytechef.component.slack.properties.SlackInputProperties.LINK_NAMES;
import static com.bytechef.component.slack.properties.SlackInputProperties.METADATA;
import static com.bytechef.component.slack.properties.SlackInputProperties.MRKDWN;
import static com.bytechef.component.slack.properties.SlackInputProperties.PARSE;
import static com.bytechef.component.slack.properties.SlackInputProperties.REPLY_BROADCAST;
import static com.bytechef.component.slack.properties.SlackInputProperties.THREAD_TS;
import static com.bytechef.component.slack.properties.SlackInputProperties.UNFURL_LINKS;
import static com.bytechef.component.slack.properties.SlackInputProperties.UNFURL_MEDIA;
import static com.bytechef.component.slack.properties.SlackInputProperties.USERNAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.slack.api.bolt.App;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

/**
 * @author Mario Cvjetojevic
 */
public abstract class AbstractSlackActionTest {
    protected static final String SEARCH_TEXT = "12345";
    protected ActionContext mockedContext = mock(ActionContext.class);
    protected Parameters mockedParameters = mock(Parameters.class);
    protected MockedConstruction<App> mockedApp;
    protected MethodsClient mockedMethodsClient = mock(MethodsClient.class);
    protected ArgumentCaptor<ChatPostMessageRequest> chatPostMessageRequestArgumentCaptor =
        ArgumentCaptor.forClass(ChatPostMessageRequest.class);

    @BeforeEach
    public void beforeEach() {
        mockedApp = Mockito.mockConstruction(App.class, (mock, context) -> {
            when(mock.client()).thenReturn(mockedMethodsClient);
        });
    }

    @AfterEach
    public void afterEach() {
        mockedApp.close();
    }

    protected void beforeTestPerform_whenMockedParametersThenReturn(){
        when(mockedParameters.getRequiredString(ACCESS_TOKEN))
            .thenReturn(ACCESS_TOKEN);
        when(mockedParameters.getRequiredString(CHANNEL_ID))
            .thenReturn(CHANNEL_ID);
        when(mockedParameters.getString(ATTACHMENTS))
            .thenReturn(ATTACHMENTS);
        when(mockedParameters.getString(BLOCKS))
            .thenReturn(BLOCKS);
        when(mockedParameters.getString(TEXT))
            .thenReturn(TEXT);
        when(mockedParameters.getBoolean(AS_USER))
            .thenReturn(true);
        when(mockedParameters.getString(ICON_EMOJI))
            .thenReturn(ICON_EMOJI);
        when(mockedParameters.getString(ICON_URL))
            .thenReturn(ICON_URL);
        when(mockedParameters.getBoolean(LINK_NAMES))
            .thenReturn(true);
        when(mockedParameters.getString(METADATA))
            .thenReturn(METADATA);
        when(mockedParameters.getBoolean(MRKDWN))
            .thenReturn(true);
        when(mockedParameters.getString(PARSE))
            .thenReturn(PARSE);
        when(mockedParameters.getBoolean(REPLY_BROADCAST))
            .thenReturn(true);
        when(mockedParameters.getString(THREAD_TS))
            .thenReturn(THREAD_TS);
        when(mockedParameters.getBoolean(UNFURL_LINKS))
            .thenReturn(true);
        when(mockedParameters.getBoolean(UNFURL_MEDIA))
            .thenReturn(true);
        when(mockedParameters.getString(USERNAME))
            .thenReturn(USERNAME);
    }

    protected void afterTestPerform_assertEquals() {
        assertEquals(ACCESS_TOKEN, chatPostMessageRequestArgumentCaptor.getValue()
            .getToken());
        assertEquals(ATTACHMENTS, chatPostMessageRequestArgumentCaptor.getValue()
            .getAttachmentsAsString());
        assertEquals(BLOCKS, chatPostMessageRequestArgumentCaptor.getValue()
            .getBlocksAsString());
        assertEquals(TEXT, chatPostMessageRequestArgumentCaptor.getValue()
            .getText());
        assertEquals(true, chatPostMessageRequestArgumentCaptor.getValue()
            .isAsUser());
        assertEquals(ICON_EMOJI, chatPostMessageRequestArgumentCaptor.getValue()
            .getIconEmoji());
        assertEquals(ICON_URL, chatPostMessageRequestArgumentCaptor.getValue()
            .getIconUrl());
        assertEquals(true, chatPostMessageRequestArgumentCaptor.getValue()
            .isLinkNames());
        assertEquals(METADATA, chatPostMessageRequestArgumentCaptor.getValue()
            .getMetadataAsString());
        assertEquals(true, chatPostMessageRequestArgumentCaptor.getValue()
            .isMrkdwn());
        assertEquals(PARSE, chatPostMessageRequestArgumentCaptor.getValue()
            .getParse());
        assertEquals(true, chatPostMessageRequestArgumentCaptor.getValue()
            .isReplyBroadcast());
        assertEquals(THREAD_TS, chatPostMessageRequestArgumentCaptor.getValue()
            .getThreadTs());
        assertEquals(true, chatPostMessageRequestArgumentCaptor.getValue()
            .isUnfurlLinks());
        assertEquals(true, chatPostMessageRequestArgumentCaptor.getValue()
            .isUnfurlMedia());
        assertEquals(USERNAME, chatPostMessageRequestArgumentCaptor.getValue()
            .getUsername());
    }
}

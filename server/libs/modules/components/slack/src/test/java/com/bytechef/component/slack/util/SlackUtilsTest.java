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

package com.bytechef.component.slack.util;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.slack.api.bolt.App;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.conversations.ConversationsListRequest;
import com.slack.api.methods.request.users.UsersListRequest;
import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.methods.response.users.UsersListResponse;
import com.slack.api.model.Conversation;
import com.slack.api.model.User;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

/**
 * @author Mario Cvjetojevic
 * @author Monika Domiter
 */
class SlackUtilsTest {

    private MockedConstruction<App> mockedApp;
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final MethodsClient mockedMethodsClient = mock(MethodsClient.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final ConversationsListResponse mockedConversationsListResponse = mock(ConversationsListResponse.class);
    private final UsersListResponse usersListResponse = mock(UsersListResponse.class);

    @BeforeEach
    void beforeEach() {
        mockedApp = mockConstruction(App.class, (mock, context) -> when(mock.client()).thenReturn(mockedMethodsClient));
    }

    @AfterEach
    void afterEach() {
        mockedApp.close();
    }

    @Test
    void testGetChannelOptions() throws IOException, SlackApiException {
        Conversation conversation = new Conversation();

        conversation.setName("New channel");
        conversation.setId("id");

        when(mockedParameters.getRequiredString(ACCESS_TOKEN))
            .thenReturn(ACCESS_TOKEN);
        when(mockedMethodsClient.conversationsList(any(ConversationsListRequest.class)))
            .thenReturn(mockedConversationsListResponse);
        when(mockedConversationsListResponse.getChannels())
            .thenReturn(List.of(conversation));

        List<Option<String>> options = SlackUtils.getChannelOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        verify(mockedMethodsClient, times(1))
            .conversationsList(any(ConversationsListRequest.class));
        assertEquals(1, options.size());

        Option<String> option = options.getFirst();

        assertTrue(StringUtils.equals(option.getLabel(), "New channel"));
        assertTrue(StringUtils.equals(option.getValue(), "id"));
    }

    @Test
    void testGetUserOptions() throws IOException, SlackApiException {
        User user = new User();

        user.setName("User name");
        user.setId("id");

        when(mockedMethodsClient.usersList(any(UsersListRequest.class)))
            .thenReturn(usersListResponse);
        when(usersListResponse.getMembers())
            .thenReturn(List.of(user));

        List<Option<String>> options = SlackUtils.getUserOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        verify(mockedMethodsClient, times(1))
            .usersList(any(UsersListRequest.class));
        assertEquals(1, options.size());

        Option<String> option = options.getFirst();

        assertTrue(StringUtils.equals(option.getLabel(), "User name"));
        assertTrue(StringUtils.equals(option.getValue(), "id"));
    }
}

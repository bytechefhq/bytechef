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

import static com.bytechef.component.slack.constant.SlackConstants.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Option;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.users.UsersListRequest;
import com.slack.api.methods.response.users.UsersListResponse;
import com.slack.api.model.User;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

/**
 * @author Mario Cvjetojevic
 */
public final class SlackSendDirectMessageActionTest extends AbstractSlackActionTest {

    @Test
    public void testPerform() throws SlackApiException, IOException {
        beforeTestPerformWhenMockedParametersThenReturn();
        when(mockedParameters.getRequiredString(USER_ID))
            .thenReturn(USER_ID);

        SlackSendDirectMessageAction.perform(mockedParameters, mockedParameters, mockedContext);

        verify(mockedMethodsClient, times(1))
            .chatPostMessage(chatPostMessageRequestArgumentCaptor.capture());
        assertEquals(USER_ID, chatPostMessageRequestArgumentCaptor.getValue()
            .getChannel());
        afterTestPerformAssertEquals();
    }

    @Test
    public void testGetUserOptions() throws IOException, SlackApiException {
        UsersListResponse usersListResponse = mock(UsersListResponse.class);
        List<User> mockedUserList = Arrays.asList(
            mock(User.class),
            mock(User.class),
            mock(User.class),
            mock(User.class));

        when(mockedMethodsClient.usersList(any(UsersListRequest.class)))
            .thenReturn(usersListResponse);
        when(usersListResponse.getMembers())
            .thenReturn(mockedUserList);
        mockedUserList.forEach(user -> when(user.getName())
            .thenReturn("NOT searched text"));

        User user = mockedUserList.getFirst();

        when(user.getName())
            .thenReturn(SEARCH_TEXT + " more text");

        List<Option<String>> options = SlackSendDirectMessageAction.getUserOptions(
            mockedParameters, mockedParameters, Map.of(), SEARCH_TEXT, mockedContext);

        verify(mockedMethodsClient, times(1))
            .usersList(any(UsersListRequest.class));
        assertEquals(1, options.size());

        Option<String> option = options.getFirst();

        assertTrue(StringUtils.equals(option.getLabel(), SEARCH_TEXT + " more text"));
    }
}

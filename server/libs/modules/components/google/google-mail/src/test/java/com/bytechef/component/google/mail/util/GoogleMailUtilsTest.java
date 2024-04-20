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

package com.bytechef.component.google.mail.util;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.ListThreadsResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.Thread;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
class GoogleMailUtilsTest {

    protected MockedStatic<GoogleServices> googleServicesMockedStatic;
    protected ActionContext mockedContext = mock(ActionContext.class);
    protected Gmail mockedGmail = mock(Gmail.class);
    protected Gmail.Users.Labels mockedLabels = mock(Gmail.Users.Labels.class);
    protected Gmail.Users.Labels.List mockedLabelsList = mock(Gmail.Users.Labels.List.class);
    protected Gmail.Users.Messages mockedMesages = mock(Gmail.Users.Messages.class);
    protected Gmail.Users.Messages.List mockedMesagesList = mock(Gmail.Users.Messages.List.class);
    protected Parameters mockedParameters = mock(Parameters.class);
    protected Gmail.Users.Threads mockedThreads = mock(Gmail.Users.Threads.class);
    protected Gmail.Users.Threads.List mockedThreadsList = mock(Gmail.Users.Threads.List.class);
    protected Gmail.Users mockedUsers = mock(Gmail.Users.class);
    private final ArgumentCaptor<String> userIdArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @BeforeEach
    void beforeEach() {
        googleServicesMockedStatic = mockStatic(GoogleServices.class);

        when(mockedParameters.getRequiredString(ACCESS_TOKEN))
            .thenReturn("accessToken");

        googleServicesMockedStatic
            .when(() -> GoogleServices.getMail(mockedParameters))
            .thenReturn(mockedGmail);
    }

    @AfterEach
    public void afterEach() {
        googleServicesMockedStatic.close();
    }

    @Test
    void testGetLabelIdOptions() throws IOException {
        List<Label> labels = List.of(new Label().setName("label1"), new Label().setName("label2"));

        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.labels())
            .thenReturn(mockedLabels);
        when(mockedLabels.list(userIdArgumentCaptor.capture()))
            .thenReturn(mockedLabelsList);
        when(mockedLabelsList.execute())
            .thenReturn(new ListLabelsResponse().setLabels(labels));

        List<Option<String>> result = GoogleMailUtils.getLabelIdOptions(
            mockedParameters, mockedParameters, Map.of(), anyString(), mockedContext);

        assertEquals("me", userIdArgumentCaptor.getValue());

        assertNotNull(result);
        assertEquals(2, result.size());

        Option<String> labelIdOptionFirst = result.getFirst();

        assertEquals("label1", labelIdOptionFirst.getLabel());
        assertEquals("label1", labelIdOptionFirst.getValue());

        Option<String> option = result.get(1);

        assertEquals("label2", option.getLabel());
        assertEquals("label2", option.getValue());
    }

    @Test
    void testGetMessageIdOptions() throws IOException {
        List<Message> messages = List.of(new Message().setId("id1"), new Message().setId("id2"));

        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.messages())
            .thenReturn(mockedMesages);
        when(mockedMesages.list(userIdArgumentCaptor.capture()))
            .thenReturn(mockedMesagesList);
        when(mockedMesagesList.execute())
            .thenReturn(new ListMessagesResponse().setMessages(messages));

        List<Option<String>> messageIdOptions = GoogleMailUtils.getMessageIdOptions(
            mockedParameters, mockedParameters, Map.of(), anyString(), mockedContext);

        assertEquals("me", userIdArgumentCaptor.getValue());

        assertNotNull(messageIdOptions);
        assertEquals(2, messageIdOptions.size());

        Option<String> messageIdOptionsFirst = messageIdOptions.getFirst();

        assertEquals("id1", messageIdOptionsFirst.getLabel());
        assertEquals("id1", messageIdOptionsFirst.getValue());

        Option<String> option = messageIdOptions.get(1);

        assertEquals("id2", option.getLabel());
        assertEquals("id2", option.getValue());
    }

    @Test
    void testGetThreadIdOptions() throws IOException {
        List<Thread> threads = List.of(new Thread().setId("id1"), new Thread().setId("id2"));

        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.threads())
            .thenReturn(mockedThreads);
        when(mockedThreads.list(userIdArgumentCaptor.capture()))
            .thenReturn(mockedThreadsList);
        when(mockedThreadsList.execute())
            .thenReturn(new ListThreadsResponse().setThreads(threads));

        List<Option<String>> threadIdOptions = GoogleMailUtils.getThreadIdOptions(
            mockedParameters, mockedParameters, Map.of(), anyString(), mockedContext);

        assertEquals("me", userIdArgumentCaptor.getValue());

        assertNotNull(threadIdOptions);
        assertEquals(2, threadIdOptions.size());

        Option<String> threadIdOptionsFirst = threadIdOptions.getFirst();

        assertEquals("id1", threadIdOptionsFirst.getLabel());
        assertEquals("id1", threadIdOptionsFirst.getValue());

        Option<String> option = threadIdOptions.get(1);

        assertEquals("id2", option.getLabel());
        assertEquals("id2", option.getValue());
    }
}

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

package com.bytechef.component.google.mail.trigger;

import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.trigger.GoogleMailNewEmailPollingTrigger.LAST_TIME_CHECKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.Gmail.Users;
import com.google.api.services.gmail.Gmail.Users.Messages;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleMailNewEmailPollingTriggerTest {

    private final Gmail mockedGmail = mock(Gmail.class);
    private final Users.Messages.List mockedList = mock(Users.Messages.List.class);
    private final Messages mockedMessages = mock(Messages.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final Users mockedUsers = mock(Users.class);
    private final ArgumentCaptor<String> qArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> userIdArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPoll() throws IOException {
        List<Message> messages = List.of(new Message().setId("abc"));
        LocalDateTime startDate = LocalDateTime.of(2000, 1, 1, 1, 1, 1);

        Parameters parameters = MockParametersFactory.create(Map.of(LAST_TIME_CHECKED, startDate));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getMail(parameters))
                .thenReturn(mockedGmail);
            when(mockedGmail.users())
                .thenReturn(mockedUsers);
            when(mockedUsers.messages())
                .thenReturn(mockedMessages);
            when(mockedMessages.list(userIdArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.setQ(qArgumentCaptor.capture()))
                .thenReturn(mockedList);

            when(mockedList.execute())
                .thenReturn(new ListMessagesResponse().setMessages(messages));

            PollOutput pollOutput =
                GoogleMailNewEmailPollingTrigger.poll(parameters, parameters, parameters, mockedTriggerContext);

            assertEquals(ME, userIdArgumentCaptor.getValue());

            ZonedDateTime zonedDateTime = startDate.atZone(ZoneId.systemDefault());

            assertEquals("is:unread after:" + zonedDateTime.toEpochSecond(), qArgumentCaptor.getValue());

            assertEquals(messages, pollOutput.getRecords());
            assertFalse(pollOutput.pollImmediately());
        }
    }
}

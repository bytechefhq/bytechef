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
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.calendar.Calendar;
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

    private final ArgumentCaptor<Calendar> calendarArgumentCaptor = ArgumentCaptor.forClass(Calendar.class);
    private final Gmail mockedGmail = mock(Gmail.class);
    private final Calendar mockedCalendar = mock(Calendar.class);
    private final Users.Messages.List mockedList = mock(Users.Messages.List.class);
    private final Messages mockedMessages = mock(Messages.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final Users mockedUsers = mock(Users.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPoll() throws IOException {
        String timezone = "Europe/Zagreb";
        List<Message> messages = List.of(new Message().setId("abc"));
        LocalDateTime startDate = LocalDateTime.of(2000, 1, 1, 1, 1, 1);

        Parameters parameters = MockParametersFactory.create(Map.of(LAST_TIME_CHECKED, startDate));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleUtils> googleUtilsMockedStatic = mockStatic(GoogleUtils.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getCalendar(parametersArgumentCaptor.capture()))
                .thenReturn(mockedCalendar);
            googleUtilsMockedStatic
                .when(() -> GoogleUtils.getCalendarTimezone(calendarArgumentCaptor.capture()))
                .thenReturn(timezone);
            googleServicesMockedStatic
                .when(() -> GoogleServices.getMail(parametersArgumentCaptor.capture()))
                .thenReturn(mockedGmail);
            when(mockedGmail.users())
                .thenReturn(mockedUsers);
            when(mockedUsers.messages())
                .thenReturn(mockedMessages);
            when(mockedMessages.list(stringArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.setQ(stringArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.execute())
                .thenReturn(new ListMessagesResponse().setMessages(messages));

            PollOutput pollOutput = GoogleMailNewEmailPollingTrigger.poll(
                parameters, parameters, parameters, mockedTriggerContext);

            assertEquals(messages, pollOutput.records());
            assertFalse(pollOutput.pollImmediately());

            ZoneId zoneId = ZoneId.of(timezone);

            ZonedDateTime zonedDateTime = startDate.atZone(zoneId);

            assertEquals(
                List.of(ME, "is:unread after:" + zonedDateTime.toEpochSecond()), stringArgumentCaptor.getAllValues());
            assertEquals(List.of(parameters, parameters), parametersArgumentCaptor.getAllValues());
            assertEquals(mockedCalendar, calendarArgumentCaptor.getValue());
        }
    }
}

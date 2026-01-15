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

import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.trigger.GoogleMailNewEmailPollingTrigger.LAST_TIME_CHECKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.google.mail.definition.Format;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.Gmail.Users;
import com.google.api.services.gmail.Gmail.Users.Messages;
import com.google.api.services.gmail.Gmail.Users.Messages.Get;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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
    private final ArgumentCaptor<Long> longArgumentCaptor = ArgumentCaptor.forClass(Long.class);
    private final Gmail mockedGmail = mock(Gmail.class);
    private final Calendar mockedCalendar = mock(Calendar.class);
    private final Get mockedGet = mock(Get.class);
    private final Users.Messages.List mockedList = mock(Users.Messages.List.class);
    private final Messages mockedMessages = mock(Messages.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final Users mockedUsers = mock(Users.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<ZoneId> zoneIdArgumentCaptor = ArgumentCaptor.forClass(ZoneId.class);

    @Test
    void testPoll() throws IOException {
        String timezone = "Europe/Zagreb";
        Message message = new Message().setId("abc");
        List<Message> messages = List.of(message);
        LocalDateTime startDate = LocalDateTime.of(2000, 1, 1, 1, 1, 1);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 2, 0, 0, 0);

        Parameters parameters = MockParametersFactory.create(Map.of(LAST_TIME_CHECKED, startDate, FORMAT, Format.FULL));

        try (
            MockedStatic<LocalDateTime> localDateTimeMockedStatic = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS);
            MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleUtils> googleUtilsMockedStatic = mockStatic(GoogleUtils.class)) {

            localDateTimeMockedStatic.when(() -> LocalDateTime.now(zoneIdArgumentCaptor.capture()))
                .thenReturn(endDate);

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
            when(mockedList.setMaxResults(longArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.setPageToken(stringArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.execute())
                .thenReturn(new ListMessagesResponse().setMessages(messages));
            when(mockedMessages.get(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(mockedGet);
            when(mockedGet.setFormat(stringArgumentCaptor.capture()))
                .thenReturn(mockedGet);
            when(mockedGet.execute())
                .thenReturn(message);

            when(mockedTriggerContext.isEditorEnvironment())
                .thenReturn(false);

            localDateTimeMockedStatic.when(() -> LocalDateTime.now(zoneIdArgumentCaptor.capture()))
                .thenReturn(endDate);

            PollOutput pollOutput = GoogleMailNewEmailPollingTrigger.poll(
                parameters, parameters, parameters, mockedTriggerContext);

            assertEquals(new PollOutput(messages, Map.of(LAST_TIME_CHECKED, endDate), false), pollOutput);

            ZoneId zoneId = ZoneId.of(timezone);

            ZonedDateTime zonedDateTime = startDate.atZone(zoneId);

            List<String> strings = new ArrayList<>();

            strings.add(ME);
            strings.add("is:unread after:" + zonedDateTime.toEpochSecond());
            strings.add(null);
            strings.add(ME);
            strings.add("abc");
            strings.add("full");

            assertEquals(strings, stringArgumentCaptor.getAllValues());
            assertEquals(500L, longArgumentCaptor.getValue());
            assertEquals(List.of(parameters, parameters), parametersArgumentCaptor.getAllValues());
            assertEquals(mockedCalendar, calendarArgumentCaptor.getValue());
            assertEquals(zoneId, zoneIdArgumentCaptor.getValue());
        }
    }

    @Test
    void testPollWithNullMessages() throws IOException {
        String timezone = "Europe/Zagreb";
        LocalDateTime startDate = LocalDateTime.of(2000, 1, 1, 1, 1, 1);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 2, 0, 0, 0);

        Parameters parameters = MockParametersFactory.create(Map.of(LAST_TIME_CHECKED, startDate));

        try (
            MockedStatic<LocalDateTime> localDateTimeMockedStatic = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS);
            MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleUtils> googleUtilsMockedStatic = mockStatic(GoogleUtils.class)) {

            localDateTimeMockedStatic.when(() -> LocalDateTime.now(zoneIdArgumentCaptor.capture()))
                .thenReturn(endDate);

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
            when(mockedList.setMaxResults(longArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.setPageToken(stringArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.execute())
                .thenReturn(new ListMessagesResponse().setMessages(null));

            when(mockedTriggerContext.isEditorEnvironment())
                .thenReturn(false);

            localDateTimeMockedStatic.when(() -> LocalDateTime.now(zoneIdArgumentCaptor.capture()))
                .thenReturn(endDate);

            PollOutput pollOutput = GoogleMailNewEmailPollingTrigger.poll(
                parameters, parameters, parameters, mockedTriggerContext);

            assertEquals(new PollOutput(List.of(), Map.of(LAST_TIME_CHECKED, endDate), false), pollOutput);
        }
    }
}

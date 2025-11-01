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

package com.bytechef.component.google.calendar.action;

import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ALL_DAY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ATTACHMENTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ATTENDEES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DESCRIPTION;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.END;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.GUEST_CAN_INVITE_OTHERS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.GUEST_CAN_MODIFY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.GUEST_CAN_SEE_OTHER_GUESTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.LOCATION;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SEND_UPDATES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.START;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SUMMARY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.USE_DEFAULT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils.CustomEvent;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleCalendarCreateEventActionTest {

    private final ArgumentCaptor<Calendar> calendarArgumentCaptor = ArgumentCaptor.forClass(Calendar.class);
    private final ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    private final EventDateTime eventDateTime = new EventDateTime();
    private final Calendar mockedCalendar = mock(Calendar.class);
    private final Calendar.Events mockedEvents = mock(Calendar.Events.class);
    private final CustomEvent mockedCustomEvent = mock(CustomEvent.class);
    private final Event mockedEvent = mock(Event.class);
    private final Calendar.Events.Insert mockedInsert = mock(Calendar.Events.Insert.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() throws IOException {
        String calendarTimezone = "Europe/Zagreb";
        Parameters mockedParameters = getParameters();

        when(mockedCalendar.events())
            .thenReturn(mockedEvents);
        when(mockedEvents.insert(stringArgumentCaptor.capture(), eventArgumentCaptor.capture()))
            .thenReturn(mockedInsert);
        when(mockedInsert.setSendUpdates(stringArgumentCaptor.capture()))
            .thenReturn(mockedInsert);
        when(mockedInsert.execute())
            .thenReturn(mockedEvent);

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleCalendarUtils> googleCalendarUtilsMockedStatic = mockStatic(GoogleCalendarUtils.class);
            MockedStatic<GoogleUtils> googleUtilsMockedStatic = mockStatic(GoogleUtils.class)) {

            googleServicesMockedStatic.when(() -> GoogleServices.getCalendar(parametersArgumentCaptor.capture()))
                .thenReturn(mockedCalendar);
            googleUtilsMockedStatic
                .when(() -> GoogleUtils.getCalendarTimezone(calendarArgumentCaptor.capture()))
                .thenReturn(calendarTimezone);
            googleCalendarUtilsMockedStatic
                .when(() -> GoogleCalendarUtils.createEventDateTime(
                    parametersArgumentCaptor.capture(), stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(eventDateTime);
            googleCalendarUtilsMockedStatic
                .when(() -> GoogleCalendarUtils.createCustomEvent(
                    eventArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(mockedCustomEvent);

            CustomEvent result = GoogleCalendarCreateEventAction.perform(
                mockedParameters, mockedParameters, mock(ActionContext.class));

            assertEquals(mockedCustomEvent, result);
            assertEquals(
                List.of(mockedParameters, mockedParameters, mockedParameters), parametersArgumentCaptor.getAllValues());
            assertEquals(mockedCalendar, calendarArgumentCaptor.getValue());
            assertEquals(
                List.of(END, calendarTimezone, START, calendarTimezone, "calendarId", "sendUpdates", calendarTimezone),
                stringArgumentCaptor.getAllValues());

            Event expectedEvent = new Event()
                .setAttachments(List.of())
                .setAttendees(
                    List.of(
                        new EventAttendee().setEmail("attendee1@mail.com"),
                        new EventAttendee().setEmail("attendee2@mail.com")))
                .setDescription("description")
                .setEnd(eventDateTime)
                .setGuestsCanInviteOthers(true)
                .setGuestsCanModify(true)
                .setGuestsCanSeeOtherGuests(true)
                .setLocation("location")
                .setReminders(
                    new Event.Reminders()
                        .setUseDefault(true)
                        .setOverrides(List.of()))
                .setStart(eventDateTime)
                .setSummary("summary");

            assertEquals(List.of(expectedEvent, mockedEvent), eventArgumentCaptor.getAllValues());
        }
    }

    private static Parameters getParameters() {
        Map<String, Object> parametersMap = new HashMap<>();

        parametersMap.put(CALENDAR_ID, "calendarId");
        parametersMap.put(SEND_UPDATES, "sendUpdates");
        parametersMap.put(SUMMARY, "summary");
        parametersMap.put(DESCRIPTION, "description");
        parametersMap.put(LOCATION, "location");
        parametersMap.put(ATTACHMENTS, List.of());
        parametersMap.put(ATTENDEES, List.of("attendee1@mail.com", "attendee2@mail.com"));
        parametersMap.put(ALL_DAY, false);
        parametersMap.put(GUEST_CAN_INVITE_OTHERS, true);
        parametersMap.put(GUEST_CAN_MODIFY, true);
        parametersMap.put(GUEST_CAN_SEE_OTHER_GUESTS, true);
        parametersMap.put(USE_DEFAULT, true);

        return MockParametersFactory.create(parametersMap);
    }
}

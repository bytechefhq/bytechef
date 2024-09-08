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
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleCalendarCreateEventActionTest {

    private final ArgumentCaptor<String> calendarIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    private final EventDateTime eventDateTime = new EventDateTime();
    private final Calendar mockedCalendar = mock(Calendar.class);
    private final CustomEvent mockedCustomEvent = mock(CustomEvent.class);
    private final Calendar.Events mockedEvents = mock(Calendar.Events.class);
    private final Event mockedEvent = mock(Event.class);
    private final Calendar.Events.Insert mockedInsert = mock(Calendar.Events.Insert.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final ArgumentCaptor<String> sendUpdatesArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() throws IOException {
        when(mockedParameters.getRequiredString(CALENDAR_ID))
            .thenReturn("calendarId");
        when(mockedParameters.getString(SUMMARY))
            .thenReturn("summary");
        when(mockedParameters.getRequiredBoolean(ALL_DAY))
            .thenReturn(false);
        when(mockedParameters.getString(DESCRIPTION))
            .thenReturn("description");
        when(mockedParameters.getString(LOCATION))
            .thenReturn("location");
        when(mockedParameters.getFileEntries(ATTACHMENTS, List.of()))
            .thenReturn(List.of());
        when(mockedParameters.getList(ATTENDEES, String.class, List.of()))
            .thenReturn(List.of("attendee1@mail.com", "attendee2@mail.com"));
        when(mockedParameters.getBoolean(GUEST_CAN_INVITE_OTHERS))
            .thenReturn(true);
        when(mockedParameters.getBoolean(GUEST_CAN_MODIFY))
            .thenReturn(true);
        when(mockedParameters.getBoolean(GUEST_CAN_SEE_OTHER_GUESTS))
            .thenReturn(true);
        when(mockedParameters.getString(SEND_UPDATES))
            .thenReturn("sendUpdates");
        when(mockedParameters.getRequiredBoolean(USE_DEFAULT))
            .thenReturn(true);
        when(mockedParameters.getString(SUMMARY))
            .thenReturn("summary");

        when(mockedCalendar.events())
            .thenReturn(mockedEvents);
        when(mockedEvents.insert(calendarIdArgumentCaptor.capture(), eventArgumentCaptor.capture()))
            .thenReturn(mockedInsert);
        when(mockedInsert.setSendUpdates(sendUpdatesArgumentCaptor.capture()))
            .thenReturn(mockedInsert);
        when(mockedInsert.execute())
            .thenReturn(mockedEvent);

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleCalendarUtils> googleCalendarUtilsMockedStatic = mockStatic(GoogleCalendarUtils.class)) {

            googleServicesMockedStatic.when(() -> GoogleServices.getCalendar(mockedParameters))
                .thenReturn(mockedCalendar);
            googleCalendarUtilsMockedStatic
                .when(() -> GoogleCalendarUtils.createEventDateTime(mockedParameters, START))
                .thenReturn(eventDateTime);
            googleCalendarUtilsMockedStatic
                .when(() -> GoogleCalendarUtils.createEventDateTime(mockedParameters, END))
                .thenReturn(eventDateTime);
            googleCalendarUtilsMockedStatic
                .when(() -> GoogleCalendarUtils.createCustomEvent(mockedEvent))
                    .thenReturn(mockedCustomEvent);

            CustomEvent result = GoogleCalendarCreateEventAction.perform(
                mockedParameters, mockedParameters, mock(ActionContext.class));

            assertEquals(mockedCustomEvent, result);
            assertEquals("calendarId", calendarIdArgumentCaptor.getValue());
            assertEquals("sendUpdates", sendUpdatesArgumentCaptor.getValue());

            testEvent(eventArgumentCaptor.getValue());
        }
    }

    private void testEvent(Event event) {
        List<EventAttendee> eventAttendees = List.of(
            new EventAttendee().setEmail("attendee1@mail.com"), new EventAttendee().setEmail("attendee2@mail.com"));

        Event.Reminders reminders = new Event.Reminders().setUseDefault(true)
            .setOverrides(List.of());

        assertEquals(eventAttendees, event.getAttendees());
        assertEquals("description", event.getDescription());
        assertEquals(eventDateTime, event.getEnd());
        assertEquals(true, event.getGuestsCanInviteOthers());
        assertEquals(true, event.getGuestsCanModify());
        assertEquals(true, event.getGuestsCanSeeOtherGuests());
        assertEquals("location", event.getLocation());
        assertEquals(reminders, event.getReminders());
        assertEquals(eventDateTime, event.getStart());
        assertEquals("summary", event.getSummary());
    }
}
